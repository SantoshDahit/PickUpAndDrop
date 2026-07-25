# 13. OAuth 소셜 로그인 컨벤션

## 1. 개요

OAuth 소셜 로그인은 **Strategy + Factory 패턴**으로 설계되어 있습니다.
새 OAuth Provider 추가 시 `OAuthApiClient` 구현체 하나만 작성하면 자동으로 등록됩니다.

**지원 Provider:**
- Google
- Kakao
- Naver
- Apple

**패키지 위치:** `security/oauth/` 하위

---

## 2. 디렉토리 구조

```
security/oauth/
├── client/
│   ├── OAuthApiClient.java           # 공통 인터페이스 (Strategy)
│   ├── OAuthApiClientFactory.java    # Factory (Provider 선택)
│   ├── GoogleOAuthClient.java        # Google 구현
│   ├── KakaoOAuthClient.java         # Kakao 구현
│   ├── NaverOAuthClient.java         # Naver 구현
│   └── AppleOAuthClient.java         # Apple 구현
├── service/
│   ├── OAuthService.java             # 핵심 비즈니스 로직 (코드 → 토큰 → 사용자 정보)
│   ├── OAuthCodeService.java         # 1회용 코드 관리 (웹 플로우)
│   ├── OAuthStateService.java        # State 파라미터 (플랫폼 감지)
│   └── OAuthRedirectService.java     # 리다이렉트 URL 빌더
├── dto/
│   ├── OAuthDto.java                 # TokenResponse, UserInfo
│   ├── OAuthCodeDto.java             # 1회용 코드 데이터
│   └── OAuthStateDto.java            # State 데이터 (platform, csrf, timestamp)
└── config/
    ├── OAuthProperties.java          # Provider별 설정 (clientId, clientSecret 등)
    └── OAuthCallbackProperties.java  # 콜백 리다이렉트 URL 설정
```

---

## 3. 핵심 디자인 패턴

### Strategy 패턴 - OAuthApiClient 인터페이스

모든 OAuth Provider가 구현하는 공통 인터페이스입니다.

```java
public interface OAuthApiClient {
    OAuthProvider getProvider();
    OAuthDto.TokenResponse exchangeToken(String authorizationCode);
    OAuthDto.UserInfo getUserInfo(String accessToken);
}
```

| 메서드 | 역할 |
|--------|------|
| `getProvider()` | 이 Client가 담당하는 Provider 반환 |
| `exchangeToken()` | Authorization Code → Access Token 교환 |
| `getUserInfo()` | Access Token → 사용자 정보 조회 |

### Factory 패턴 - OAuthApiClientFactory

Spring이 모든 `OAuthApiClient` 구현체를 `List`로 자동 주입합니다.
`getClient(provider)`로 해당 Provider의 Client를 조회합니다.

```java
@Component
@RequiredArgsConstructor
public class OAuthApiClientFactory {
    private final List<OAuthApiClient> oAuthApiClients;

    public OAuthApiClient getClient(OAuthProvider provider) {
        return oAuthApiClients.stream()
                .filter(client -> client.getProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED));
    }
}
```

> **핵심:** `@Component`로 등록된 `OAuthApiClient` 구현체는 자동으로 Factory에 수집됩니다.
> 새 Provider를 추가해도 Factory 코드를 수정할 필요가 없습니다.

### OAuthService - 핵심 흐름

Factory에서 Client를 조회한 뒤 3단계로 사용자 정보를 가져옵니다.

```java
@Service
@RequiredArgsConstructor
public class OAuthService {
    private final OAuthApiClientFactory oAuthApiClientFactory;

    public OAuthDto.UserInfo getUserInfo(String authorizationCode, OAuthProvider provider) {
        // 1. Provider에 맞는 Client 조회
        OAuthApiClient client = oAuthApiClientFactory.getClient(provider);

        // 2. Authorization Code → Access Token 교환
        OAuthDto.TokenResponse tokenResponse = client.exchangeToken(authorizationCode);

        // 3. Access Token으로 사용자 정보 조회
        OAuthDto.UserInfo userInfo = client.getUserInfo(tokenResponse.accessToken());

        return userInfo;
    }
}
```

---

## 4. 이중 플로우 (모바일 + 웹)

### 플로우 1: 모바일 직접 로그인

앱에서 OAuth Provider 인증을 완료한 뒤, Authorization Code(또는 Apple의 identityToken)를 직접 서버에 전달합니다.

```
앱 → OAuth Provider 인증 → Authorization Code 획득
앱 → POST /v1/auth/oauth { code, provider }
서버 → OAuthService.getUserInfo(code, provider)
서버 → Account 조회 또는 생성
서버 → JWT 토큰 발급
앱 ← { accessToken, refreshToken, account }
```

**요청 DTO:**
```java
public record OAuthLoginRequest(
    @NotBlank String code,       // Authorization Code 또는 identityToken (Apple)
    @NotNull OAuthProvider provider  // GOOGLE, KAKAO, NAVER, APPLE
) {}
```

### 플로우 2: 웹 콜백 기반 로그인

웹에서 OAuth Provider로 리다이렉트 후, 콜백으로 돌아와 1회용 코드를 발급받습니다.

```
웹 → OAuth Provider 인증 페이지로 이동 (state=web)
Provider → GET /v1/auth/oauth/callback/{provider}?code=...&state=web
서버 → OAuthService.getUserInfo(code, provider)
서버 → Account 조회 또는 생성
서버 → 1회용 코드 생성 (UUID, 5분 만료)
서버 → 302 Redirect → 웹 URL 또는 앱 Deep Link (code 포함)
클라이언트 → POST /v1/auth/oauth/code { code: "1회용코드" }
서버 → 코드 검증 및 소비 (1회용)
서버 → JWT 토큰 발급
클라이언트 ← { accessToken, refreshToken, account }
```

**1회용 코드 교환 DTO:**
```java
public record OAuthTokenRequest(
    @NotBlank String code  // 1회용 코드 (UUID)
) {}
```

### 1회용 코드 관리 (OAuthCodeService)

- **저장소:** `ConcurrentHashMap` (인메모리)
- **코드 형식:** UUID (하이픈 제거, 32자)
- **만료 시간:** 5분 (`EXPIRATION_MILLIS = 5 * 60 * 1000L`)
- **사용 방식:** 1회 사용 후 즉시 삭제 (`codeStore.remove(code)`)
- **정리:** `@Scheduled(fixedRate = 60000)` - 1분마다 만료 코드 정리

```java
public record OAuthCodeDto(
    String code,            // UUID (하이픈 제거)
    String accountId,       // 내부 계정 ID
    OAuthProvider provider,
    OAuthPlatform platform, // APP 또는 WEB
    long createdAt
) {
    public static final long EXPIRATION_MILLIS = 5 * 60 * 1000L;

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > EXPIRATION_MILLIS;
    }
}
```

### 플랫폼 감지 (OAuthStateService)

OAuth `state` 파라미터로 APP/WEB을 구분합니다.

```java
@Service
public class OAuthStateService {
    public OAuthPlatform validateAndGetPlatform(String state) {
        return OAuthPlatform.fromValue(state);  // null이면 WEB 기본값
    }
}
```

```java
public enum OAuthPlatform {
    APP("app"),
    WEB("web");

    public static OAuthPlatform fromValue(String value) {
        // null → WEB, "app" → APP, 그 외 → WEB (기본값)
    }
}
```

### 리다이렉트 URL 빌더 (OAuthRedirectService)

| 플랫폼 | 성공 리다이렉트 | 에러 리다이렉트 |
|--------|----------------|----------------|
| APP | `{appscheme}:///oauth/callback/{code}` | `{appscheme}:///oauth/callback?error=...&errorMessage=...` |
| WEB | `https://example.com/#/oauth/callback?code={code}` | `https://example.com/#/oauth/callback?error=...&errorMessage=...` |

> **Hash-based routing 지원:** `baseUrl`에 `#`이 포함되면 `#` 뒤에 쿼리 파라미터를 추가합니다.
> SPA 프레임워크의 hash routing과 호환됩니다.

---

## 5. 공통 DTO

### OAuthDto.TokenResponse

OAuth 표준 토큰 응답을 매핑하는 공통 record입니다.

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") Long expiresIn,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("scope") String scope
) {}
```

> `@JsonIgnoreProperties(ignoreUnknown = true)`: Provider마다 추가 필드(예: Apple의 `id_token`)가 있으므로 무시합니다.

### OAuthDto.UserInfo

각 Provider의 사용자 정보를 공통 형태로 변환한 record입니다.

```java
public record UserInfo(
    String providerId,       // Provider 고유 사용자 ID
    String email,            // nullable (카카오 선택, Apple 미제공 가능)
    String name,             // nullable (Apple 최초 로그인만 제공)
    OAuthProvider provider   // GOOGLE, KAKAO, NAVER, APPLE
) {}
```

### OAuthStateDto

State 파라미터의 구조화된 데이터입니다.

```java
public record OAuthStateDto(
    OAuthPlatform platform,  // APP 또는 WEB
    String csrf,             // CSRF 토큰 (UUID)
    long timestamp,          // 생성 시간
    String redirectPath      // 로그인 후 이동할 경로 (선택적)
) {
    public boolean isApp() { return platform == OAuthPlatform.APP; }
    public boolean isWeb() { return platform == OAuthPlatform.WEB; }
}
```

---

## 6. Provider별 구현 가이드

### 비교 표

| | Google | Kakao | Naver | Apple |
|---|---|---|---|---|
| Token URL | `oauth2.googleapis.com/token` | `kauth.kakao.com/oauth/token` | `nid.naver.com/oauth2.0/token` | `appleid.apple.com/auth/token` |
| UserInfo URL | `googleapis.com/oauth2/v2/userinfo` | `kapi.kakao.com/v2/user/me` | `openapi.naver.com/v1/nid/me` | JWT에서 직접 추출 |
| ID 타입 | String (`id`) | Long → String 변환 (`id`) | String (nested `response.id`) | JWT subject |
| Token 교환 시 redirect_uri | 필수 | 필수 | 불필요 | 필수 (웹) |
| 특이사항 | 표준 OAuth 2.0 | email 선택적, 중첩 구조 | 응답이 `response` 안에 중첩 | JWT client_secret, JWKS 검증 |

### Google (가장 표준적 - 새 Provider 참고 기준)

표준 OAuth 2.0 Authorization Code Grant입니다.

```java
@Component
public class GoogleOAuthClient implements OAuthApiClient {
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    // exchangeToken: form body (code, client_id, client_secret, redirect_uri, grant_type=authorization_code)
    // getUserInfo: Bearer 토큰으로 GET 요청

    // 내부 DTO
    private static class GoogleUserResponse {
        private String id;
        private String email;
        private String name;
        private String picture;
        @JsonProperty("verified_email")
        private Boolean verifiedEmail;
    }
}
```

### Kakao

중첩 구조의 사용자 정보를 파싱합니다.

```java
// 내부 DTO (중첩 구조)
private static class KakaoUserResponse {
    private Long id;                           // Long → String 변환 필요
    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;         // email은 여기서
    private Properties properties;             // nickname은 여기서

    private static class KakaoAccount {
        private String email;                  // 선택 동의 항목 (nullable)
    }
    private static class Properties {
        private String nickname;
    }
}
```

> **주의:** `id`가 `Long` 타입이므로 `String.valueOf()`로 변환하여 `UserInfo.providerId`에 저장합니다.

### Naver

응답이 `response` 필드 안에 한 번 더 감싸져 있습니다.

```java
// 내부 DTO (중첩 구조)
private static class NaverUserResponse {
    private String resultcode;
    private String message;
    private NaverUserInfo response;            // 실제 사용자 정보

    private static class NaverUserInfo {
        private String id;
        private String email;
        private String name;
    }
}
```

> **주의:** Token 교환 시 `redirect_uri`를 보내지 않습니다. Naver는 이를 요구하지 않습니다.

### Apple (가장 복잡)

Apple은 다른 Provider와 크게 다른 구조를 가집니다.

**모바일/웹 자동 감지:**
```java
private boolean isJwtToken(String token) {
    String[] parts = token.split("\\.");
    return parts.length == 3;  // JWT는 header.payload.signature 3파트
}
```

- **모바일:** `identityToken` (JWT)이 직접 전달됨 → Token 교환 없이 JWT 검증만 수행
- **웹:** Authorization Code 전달됨 → 표준 Token 교환 후 `id_token` JWT 검증

**client_secret 동적 생성:**
```
Apple은 고정 client_secret을 사용하지 않습니다.
ES256 알고리즘으로 서명된 JWT를 client_secret으로 사용합니다.

Claims: iss(teamId), sub(serviceId), aud(https://appleid.apple.com), iat, exp(10분)
Header: alg=ES256, kid(keyId)
서명: Apple Developer 포털에서 다운로드한 p8 개인키 사용
```

**사용자 정보 추출:**
```
Apple은 별도 UserInfo API가 없습니다.
identityToken JWT의 claims에서 직접 추출합니다:
  - subject → providerId
  - email claim → email
  - name → 최초 로그인 시에만 제공 (이후 null)
```

**JWKS 검증:**
```
Apple의 공개키를 사용하여 JWT 서명을 검증합니다.
  - JWKS URL: https://appleid.apple.com/auth/keys
  - 캐시: 24시간 TTL (volatile + double-check locking)
  - audience: clientId(Bundle ID, 모바일용) 또는 serviceId(웹용) 이중 검증
```

---

## 7. 에러 처리

### OAuth 전용 ErrorCode 체계

| ErrorCode | HTTP | 코드 | 메시지 |
|---|---|---|---|
| `OAUTH_PROVIDER_NOT_SUPPORTED` | 400 | `OAUTH_001` | 지원하지 않는 OAuth Provider입니다. |
| `OAUTH_TOKEN_EXCHANGE_FAILED` | 401 | `OAUTH_002` | OAuth 인증 코드 교환에 실패했습니다. |
| `OAUTH_USER_INFO_FAILED` | 401 | `OAUTH_003` | OAuth 사용자 정보 조회에 실패했습니다. |
| `OAUTH_PROVIDER_SERVER_ERROR` | 503 | `OAUTH_004` | OAuth Provider 서버 오류입니다. 잠시 후 다시 시도해주세요. |
| `OAUTH_CONNECTION_TIMEOUT` | 504 | `OAUTH_005` | OAuth Provider 서버 응답 시간이 초과되었습니다. |
| `OAUTH_STATE_INVALID` | 400 | `OAUTH_006` | 잘못된 OAuth state 값입니다. |
| `OAUTH_STATE_EXPIRED` | 400 | `OAUTH_007` | OAuth 인증 요청이 만료되었습니다. 다시 시도해주세요. |
| `OAUTH_CALLBACK_ERROR` | 400 | `OAUTH_008` | OAuth 인증에 실패했습니다. |
| `OAUTH_CODE_INVALID` | 400 | `OAUTH_009` | 유효하지 않은 인증 코드입니다. |
| `OAUTH_CODE_EXPIRED` | 400 | `OAUTH_010` | 인증 코드가 만료되었습니다. |
| `OAUTH_TOKEN_VALIDATION_FAILED` | 401 | `OAUTH_011` | OAuth 토큰 검증에 실패했습니다. |
| `OAUTH_TOKEN_EXPIRED` | 401 | `OAUTH_012` | OAuth 토큰이 만료되었습니다. |

### 공통 에러 처리 패턴

모든 OAuthApiClient 구현체에서 동일한 예외 매핑을 사용합니다.

```java
try {
    // RestTemplate 호출
} catch (HttpClientErrorException e) {
    // 4xx → OAUTH_TOKEN_EXCHANGE_FAILED 또는 OAUTH_USER_INFO_FAILED
} catch (HttpServerErrorException e) {
    // 5xx → OAUTH_PROVIDER_SERVER_ERROR
} catch (ResourceAccessException e) {
    // 타임아웃 → OAUTH_CONNECTION_TIMEOUT
}
```

### 콜백 에러 처리 (AuthFacade)

콜백 플로우에서는 예외가 발생해도 에러 페이지로 리다이렉트합니다 (HTTP 500 대신).

```java
try {
    // OAuth 처리
    return oAuthRedirectService.buildSuccessRedirectUrl(platform, oauthCode);
} catch (ApiException e) {
    return oAuthRedirectService.buildErrorRedirectUrl(platform, e.getErrorCode().getErrorCode(), e.getErrorCode().getMessage());
} catch (Exception e) {
    return oAuthRedirectService.buildErrorRedirectUrl(platform, ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), ...);
}
```

---

## 8. 설정 (application.yml)

### Provider별 설정 구조

```yaml
oauth:
  google:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
    redirect-uri: ${SERVER_URL}/v1/auth/oauth/callback/google
  kakao:
    client-id: ${KAKAO_CLIENT_ID}
    client-secret: ${KAKAO_CLIENT_SECRET}
    redirect-uri: ${SERVER_URL}/v1/auth/oauth/callback/kakao
  naver:
    client-id: ${NAVER_CLIENT_ID}
    client-secret: ${NAVER_CLIENT_SECRET}
    # redirect-uri 불필요 (Naver는 Token 교환 시 redirect_uri 미전송)
  apple:
    client-id: ${APPLE_BUNDLE_ID}         # iOS Bundle ID (모바일 audience 검증용)
    team-id: ${APPLE_TEAM_ID}             # Apple Developer Team ID
    key-id: ${APPLE_KEY_ID}               # Apple Private Key ID
    private-key: ${APPLE_PRIVATE_KEY}     # p8 파일 내용 (PEM 형식)
    service-id: ${APPLE_SERVICE_ID}       # Services ID (웹 audience 검증용)
    redirect-uri: ${SERVER_URL}/v1/auth/oauth/callback/apple
  callback:
    app-scheme: {appscheme}                # 앱 딥링크 스킴
    app-callback-path: /oauth/callback    # 앱 콜백 경로
    web-redirect-url: ${WEB_URL}/#/oauth/callback  # 웹 리다이렉트 URL
```

### Properties 클래스

**OAuthProperties** (`@ConfigurationProperties(prefix = "oauth")`):

```java
public class OAuthProperties {
    private Provider google = new Provider();
    private Provider kakao = new Provider();
    private Provider naver = new Provider();
    private AppleProvider apple = new AppleProvider();

    public static class Provider {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
    }

    public static class AppleProvider {
        private String clientId;     // Bundle ID (모바일)
        private String teamId;
        private String keyId;
        private String privateKey;   // PEM 형식
        private String serviceId;    // 웹용
        private String redirectUri;
    }
}
```

**OAuthCallbackProperties** (`@ConfigurationProperties(prefix = "oauth.callback")`):

```java
public class OAuthCallbackProperties {
    private String appScheme = "{appscheme}";
    private String appCallbackPath = "/oauth/callback";
    private String webRedirectUrl;

    public String getAppRedirectUrl() {
        return appScheme + "://" + appCallbackPath;
    }
}
```

---

## 9. Entity 연동 패턴

OAuth 모듈은 Account Entity의 구체적인 구조에 의존하지 않습니다.
OAuth가 Entity에 요구하는 것은 **`provider`와 `providerId` 2개 필드**뿐입니다.

### Entity에 추가할 OAuth 필드

```java
@Enumerated(EnumType.STRING)
@Column(name = "provider")
private OAuthProvider provider;     // GOOGLE, KAKAO, NAVER, APPLE

@Column(name = "provider_id")
private String providerId;          // Provider별 고유 사용자 ID
```

### OAuth 전용 생성자

```java
/**
 * OAuth 로그인용 생성자
 * loginId는 provider_providerId 형식으로 생성, password는 빈 문자열
 */
public Account(Role role, OAuthProvider provider, String providerId) {
    this.id = UUID.randomUUID().toString();
    this.loginId = provider.name() + "_" + providerId;  // 예: "GOOGLE_12345"
    this.password = "";  // OAuth는 비밀번호 없음
    this.role = role;
    this.provider = provider;
    this.providerId = providerId;
    this.lastLoginAt = LocalDateTime.now();
}
```

> **loginId 규칙:** `{PROVIDER}_{providerId}` 형식으로 생성하여 일반 로그인과 중복을 방지합니다.

### Repository 조회 메서드

```java
Optional<Account> findByProviderIdAndProvider(String providerId, OAuthProvider provider);
```

### Facade에서 조회 또는 생성 패턴

```java
private Account getOrCreateOAuthAccount(OAuthDto.UserInfo userInfo) {
    // 1. provider + providerId로 기존 계정 조회
    Optional<Account> account = accountService
            .getNullAbleByProviderIdAndProvider(userInfo.providerId(), userInfo.provider());

    // 2. 없으면 신규 생성
    return account.orElseGet(
            () -> accountService.save(
                    new Account(Role.USER, userInfo.provider(), userInfo.providerId())
            )
    );
}
```

---

## 10. 새 Provider 추가 체크리스트

### 필수 작업

1. **`OAuthProvider` enum에 새 Provider 추가**
   ```java
   public enum OAuthProvider {
       GOOGLE, KAKAO, NAVER, APPLE, NEW_PROVIDER
   }
   ```

2. **DB migration: `oauth_provider` ENUM에 값 추가 (Flyway)**
   ```sql
   ALTER TABLE account MODIFY COLUMN provider ENUM('GOOGLE','KAKAO','NAVER','APPLE','NEW_PROVIDER');
   ```

3. **`OAuthApiClient` 구현체 작성 (`@Component`)**
   ```java
   @Component
   @RequiredArgsConstructor
   public class NewProviderOAuthClient implements OAuthApiClient {
       private final RestTemplate restTemplate;
       private final OAuthProperties oAuthProperties;

       @Override
       public OAuthProvider getProvider() { return OAuthProvider.NEW_PROVIDER; }

       @Override
       public OAuthDto.TokenResponse exchangeToken(String authorizationCode) { ... }

       @Override
       public OAuthDto.UserInfo getUserInfo(String accessToken) { ... }
   }
   ```

4. **`OAuthProperties`에 Provider 설정 추가**
   ```java
   private Provider newProvider = new Provider();
   ```

5. **`application-*.yml`에 환경변수 추가**
   ```yaml
   oauth:
     new-provider:
       client-id: ${NEW_PROVIDER_CLIENT_ID}
       client-secret: ${NEW_PROVIDER_CLIENT_SECRET}
       redirect-uri: ${SERVER_URL}/v1/auth/oauth/callback/new_provider
   ```

### 선택 작업

6. **ErrorCode에 Provider별 에러 추가** (Provider 고유 에러가 있는 경우)
7. **Provider별 내부 DTO 정의** (응답 구조가 다를 경우 `private static class` 내부 클래스로)

### 수정 불필요한 파일

- `OAuthApiClientFactory.java` - `List<OAuthApiClient>` 자동 주입으로 자동 등록
- `OAuthService.java` - Factory를 통해 Client를 조회하므로 변경 불필요
- `AuthController.java` - Provider를 PathVariable/RequestBody로 받으므로 변경 불필요
- `AuthFacade.java` - OAuthService를 통해 처리하므로 변경 불필요
