# 14. JWT & Security 컨벤션

## 개요

본 프로젝트는 **JWT(JSON Web Token) 기반 Stateless 인증**을 사용합니다.
Spring Security Filter Chain에 커스텀 JWT 필터를 등록하여, 토큰 검증 → 인증 컨텍스트 설정 → 요청 처리 흐름으로 동작합니다.

---

## 디렉토리 구조

```
security/
├── config/
│   ├── SecurityConfig.java          # Security Filter Chain 설정
│   └── SecurityProperties.java      # Whitelist URL 설정
├── dto/
│   └── UserDetail.java              # UserDetails 구현체
├── handler/
│   └── CustomAccessDeniedHandler.java  # 403 → 401 처리
├── jwt/
│   ├── JwtTokenProvider.java        # 토큰 생성/검증/파싱
│   ├── JwtAuthenticationFilter.java # 요청별 토큰 검증 필터
│   └── JwtAuthenticationEntryPoint.java # 401 응답 처리
└── service/
    ├── AuthFacade.java              # 회원가입/로그인
    └── CustomUserDetailsService.java # 사용자 조회

common/
└── AuthConstants.java               # 토큰 만료 시간, PIN 상수

util/
└── AuthorizationUtil.java           # 현재 인증 사용자 조회 유틸
```

---

## JWT 토큰 구조

### 서명 알고리즘

- **HMAC-SHA** (대칭 키)
- Secret Key: `${jwt.token.secret}` 환경변수에서 주입

```java
// JwtTokenProvider.java
private SecretKey getSigningKey() {
    byte[] keyBytes = tokenSecret.getBytes();
    return Keys.hmacShaKeyFor(keyBytes);
}
```

### 토큰 만료 시간

| 토큰 타입 | 만료 시간 | 상수 |
|-----------|----------|------|
| Access Token | 3시간 | `ACCESS_TOKEN_EXPIRY_MILLIS = 3L * 60 * 60 * 1000` |
| Refresh Token | 90일 | `REFRESH_TOKEN_EXPIRY_MILLIS = 90L * 24 * 60 * 60 * 1000` |

```java
// AuthConstants.java
public static final long ACCESS_TOKEN_EXPIRY_MILLIS = 3L * 60 * 60 * 1000;   // 3 hours
public static final long REFRESH_TOKEN_EXPIRY_MILLIS = 90L * 24 * 60 * 60 * 1000; // 90 days
```

### Claims 구조

| Claim | 설명 | 예시 |
|-------|------|------|
| `sub` (subject) | loginId | `"user@example.com"` |
| `accountId` | Account UUID | `"a5019498-4590-4a05-b138-410d77df6257"` |
| `provider` | OAuth Provider (nullable) | `"GOOGLE"`, `"KAKAO"`, `null` |
| `iat` | 발급 시간 | 자동 설정 |
| `exp` | 만료 시간 | 자동 설정 |

### 토큰 생성 메서드

```java
// JwtTokenProvider.java
public String createAccessToken(String accountId, String username, OAuthProvider provider) {
    return createToken(accountId, username, provider, ACCESS_TOKEN_VALID_TIME);
}

public String createRefreshToken(String accountId, String username, OAuthProvider provider) {
    return createToken(accountId, username, provider, REFRESH_TOKEN_VALID_TIME);
}

private String createToken(String accountId, String username, OAuthProvider provider, long validityInMilliseconds) {
    Date now = new Date();
    JwtBuilder jwtBuilder = Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + validityInMilliseconds))
            .signWith(getSigningKey());

    jwtBuilder.claim("accountId", accountId);

    if (provider != null) {
        jwtBuilder.claim("provider", provider.name());
    }

    return jwtBuilder.compact();
}
```

### 토큰 파싱 메서드

| 메서드 | 반환값 | 설명 |
|--------|--------|------|
| `resolveToken(HttpServletRequest)` | `String` | `Authorization: Bearer ...` 헤더에서 토큰 추출 |
| `validateToken(String)` | `void` | 서명 검증 + 만료 확인, 실패 시 `BadCredentialsException` |
| `getUsernameByToken(String)` | `String` | `sub` claim (loginId) 추출 |
| `getProviderByToken(String)` | `OAuthProvider` | `provider` claim 추출 (nullable) |
| `getAuthentication(String)` | `Authentication` | UserDetail 조회 → `UsernamePasswordAuthenticationToken` 생성 |

---

## Security Filter Chain

### SecurityConfig

```java
// SecurityConfig.java
@Bean
public SecurityFilterChain configure(HttpSecurity http) throws Exception {
    return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
            )
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler)
            )
            .addFilterBefore(
                    new JwtAuthenticationFilter(jwtTokenProvider, jwtAuthenticationEntryPoint, securityProperties),
                    UsernamePasswordAuthenticationFilter.class
            )
            .build();
}
```

**주요 설정:**
- **Stateless**: 세션 미사용 (`SessionCreationPolicy.STATELESS`)
- **CSRF 비활성화**: REST API이므로 불필요
- **httpBasic/formLogin 비활성화**: JWT만 사용
- **`anyRequest().permitAll()`**: URL 기반 인가는 Filter에서 처리
- **`GrantedAuthorityDefaults("")`**: `ROLE_` 접두사 제거

### JwtAuthenticationFilter

요청 처리 흐름:

```
요청 수신
  ↓
OPTIONS 요청? → YES → 통과 (CORS Preflight)
  ↓ NO
Whitelist URL 매칭? → YES → 토큰 있으면 인증 시도 (실패해도 통과)
  ↓ NO
토큰 없음? → YES → 401 응답
  ↓ NO
토큰 검증 시도
  ├── 성공 → SecurityContext 설정 → 다음 필터
  └── 실패 → 401 응답
```

```java
// JwtAuthenticationFilter.java - 핵심 로직
@Override
public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
    // 1. OPTIONS 통과 (CORS Preflight)
    if (request.getMethod().equals("OPTIONS")) {
        filterChain.doFilter(servletRequest, servletResponse);
        return;
    }

    // 2. Whitelist 매칭 → 토큰 있으면 인증 시도, 없어도 통과
    List<String> whitelist = securityProperties.getWhitelist();
    if (whitelist.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestURI))) {
        if (!StringUtils.isBlank(token)) {
            try {
                jwtTokenProvider.validateToken(token);
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {}
        }
        filterChain.doFilter(servletRequest, servletResponse);
        return;
    }

    // 3. 토큰 없으면 401
    // 4. 토큰 검증 → 성공 시 SecurityContext 설정, 실패 시 401
}
```

### SecurityProperties

```java
// SecurityProperties.java
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    private List<String> whitelist = new ArrayList<>();
}
```

```yaml
# application-common.yml
security:
  whitelist:
    - /**    # 현재 모든 URL 허용 (개발 단계)
```

> **참고:** `AntPathMatcher`를 사용하여 URL 패턴 매칭합니다. Whitelist에 포함된 URL은 토큰 없이 접근 가능하지만, 토큰이 있으면 인증 정보를 설정합니다.

---

## 인증/인가 예외 처리

### JwtAuthenticationEntryPoint (401 Unauthorized)

인증 실패 시 JSON 형식의 에러 응답을 반환합니다.

```java
// JwtAuthenticationEntryPoint.java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final MappingJackson2HttpMessageConverter messageConverter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        ErrorResponse errorResponse = new ErrorResponse(ErrorCode.JWT_TOKEN_IS_INVALID);
        ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(response);
        messageConverter.write(errorResponse, MediaType.APPLICATION_JSON, outputMessage);
    }
}
```

### CustomAccessDeniedHandler (403 → 401)

접근 거부 시 요청 상세 정보를 로깅하고 JSON 응답을 반환합니다.

```java
// CustomAccessDeniedHandler.java
@Override
public void handle(HttpServletRequest request, HttpServletResponse response,
                   AccessDeniedException accessDeniedException) throws IOException {
    // 요청 상세 로깅 (method, URL, headers)
    log.warn("⛔️ Access Denied: {} {}, headers=[{}]", method, fullUrl, headers);

    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.AUTHENTICATION_INVALID);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
}
```

---

## UserDetail & CustomUserDetailsService

### UserDetail

Spring Security의 `UserDetails` 구현체입니다.

```java
// UserDetail.java
@RequiredArgsConstructor
@Getter
public class UserDetail implements UserDetails {
    private final String userId;      // Account ID (UUID)
    private final String username;    // loginId
    private final String password;
    private final List<? extends GrantedAuthority> authorities;
    private final OAuthProvider provider;

    public UserDetail(Account account, List<? extends GrantedAuthority> authorities) {
        this.userId = account.getId();
        this.username = account.getLoginId();
        this.password = account.getPassword();
        this.authorities = authorities;
        this.provider = account.getProvider();
    }
}
```

### CustomUserDetailsService

일반 로그인과 OAuth 로그인 2가지 방식의 사용자 조회를 제공합니다.

```java
// CustomUserDetailsService.java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountService accountService;

    // 일반 로그인: loginId로 조회
    @Override
    public UserDetail loadUserByUsername(String loginId) {
        Account account = accountService.getByLoginId(loginId);
        return new UserDetail(account, List.of(new SimpleGrantedAuthority(account.getRole().toString())));
    }

    // OAuth 로그인: loginId + provider로 조회
    public UserDetail loadUserByUsername(String email, OAuthProvider oAuthProvider) {
        Account account = accountService.getByLoginIdAndOauthProvider(email, oAuthProvider);
        return new UserDetail(account, List.of(new SimpleGrantedAuthority(account.getRole().toString())));
    }
}
```

---

## 인증 흐름

### 1. 로그인 플로우

```
POST /v1/auth/login
  ↓
AuthFacade.login()
  ├── AccountService.getByLoginIdAndRole() → Account 조회
  ├── passwordEncoder.matches() → 비밀번호 검증 (실패 시 USER_PASSWORD_NOT_MATCH)
  ├── account.updateLastLoginAt() → 마지막 로그인 시간 갱신
  └── JwtTokenProvider.createAccessToken() + createRefreshToken() → 토큰 발급
```

### 2. API 요청 플로우

```
Authorization: Bearer {accessToken}
  ↓
JwtAuthenticationFilter.doFilter()
  ├── resolveToken() → 토큰 추출
  ├── validateToken() → 서명 + 만료 검증
  ├── getAuthentication() → UserDetail 조회 → Authentication 생성
  └── SecurityContextHolder.setAuthentication() → 인증 컨텍스트 설정
  ↓
Controller에서 AuthorizationUtil로 현재 사용자 조회
```

### 3. 회원가입 플로우

```
AuthFacade.create()
  ├── accountService.getNullAbleByLoginIdAndRole() → 중복 확인
  ├── 중복 시 → USER_DUPLICATE_LOGIN_ID 예외
  ├── passwordEncoder.encode() → 비밀번호 암호화
  └── accountRepository.save() → 계정 저장
```

---

## AuthorizationUtil

현재 인증된 사용자 정보를 `SecurityContextHolder`에서 추출하는 유틸리티입니다.

```java
// AuthorizationUtil.java
public class AuthorizationUtil {

    // Account ID 조회 (UUID)
    public static String getId() {
        // SecurityContext → UserDetail → userId
    }

    // LoginId 조회
    public static String getLoginId() {
        // SecurityContext → UserDetail → username
    }

    // 로그인 여부 확인
    public static Boolean isLoggedIn() {
        // SecurityContext.authentication.principal instanceof UserDetail
    }
}
```

| 메서드 | 반환 타입 | 설명 |
|--------|----------|------|
| `getId()` | `String` | Account UUID, 미인증 시 `AUTHENTICATION_INVALID` 예외 |
| `getLoginId()` | `String` | 로그인 ID, 미인증 시 `AUTHENTICATION_INVALID` 예외 |
| `isLoggedIn()` | `Boolean` | 인증 여부 확인, 예외 없이 `true`/`false` |

---

## AuthConstants

```java
// AuthConstants.java
public class AuthConstants {
    // PIN Generation Constants (4-digit verification codes: 1000-9999)
    public static final int PIN_MIN_VALUE = 1000;
    public static final int PIN_RANGE = 9000;
    public static final int PIN_MAX_VALUE = PIN_MIN_VALUE + PIN_RANGE - 1; // 9999

    // JWT Token Expiry Constants
    public static final long ACCESS_TOKEN_EXPIRY_MILLIS = 3L * 60 * 60 * 1000;        // 3 hours
    public static final long REFRESH_TOKEN_EXPIRY_MILLIS = 90L * 24 * 60 * 60 * 1000; // 90 days
}
```

---

## 설정 (application.yml)

```yaml
# JWT 시크릿 키
jwt:
  token:
    secret: ${JWT_TOKEN_SECRET}

# Security Whitelist
security:
  whitelist:
    - /**
```

---

## 에러 코드

| ErrorCode | 설명 |
|-----------|------|
| `JWT_TOKEN_IS_INVALID` | JWT 토큰 검증 실패 (만료, 서명 오류 등) |
| `AUTHENTICATION_INVALID` | 인증 정보 없음 (미로그인 상태에서 인증 필요 API 호출) |
| `AUTHORIZATION_INVALID` | 인가 실패 (권한 부족) |
| `USER_PASSWORD_NOT_MATCH` | 비밀번호 불일치 |
| `USER_DUPLICATE_LOGIN_ID` | 중복 로그인 ID |
