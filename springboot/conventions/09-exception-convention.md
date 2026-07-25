# 예외 처리 컨벤션

## 예외 처리 구조

```
┌──────────────────────────────────────────────────────────┐
│                        ErrorCode (enum)                   │
│  - HTTP 상태 코드                                          │
│  - 에러 메시지                                             │
│  - 에러 코드 (선택)                                        │
└──────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────┐
│                      ApiException                         │
│  - ErrorCode 포함                                          │
│  - RuntimeException 상속                                   │
└──────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────┐
│                      ErrorResponse                        │
│  - 클라이언트 응답 형식                                     │
│  - message, errorCode, timestamp                          │
└──────────────────────────────────────────────────────────┘
```

---

## ErrorCode Enum

모든 에러 코드를 중앙에서 관리합니다.

### 구조

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다.",
            ""
    ),

    // 401 Unauthorized
    JWT_TOKEN_IS_INVALID(HttpStatus.UNAUTHORIZED,
            "JWT 토큰이 없거나 유효하지 않습니다.",
            ""
    ),

    // 404 Not Found
    ACCOUNT_IS_NOT_FOUND(HttpStatus.NOT_FOUND,
            "계정을 찾을 수 없습니다.",
            ""
    ),

    // 400 Bad Request
    USER_DUPLICATE_LOGIN_ID(HttpStatus.BAD_REQUEST,
            "이미 존재하는 계정 입니다.",
            ""
    ),

    // 403 Forbidden
    AUTHENTICATION_INVALID(HttpStatus.FORBIDDEN,
            "접근 권한이 없습니다.",
            ""
    );

    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
```

### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `httpStatus` | `HttpStatus` | HTTP 응답 상태 코드 |
| `message` | `String` | 사용자에게 표시할 메시지 |
| `errorCode` | `String` | 클라이언트 식별용 코드 (선택) |

---

## ErrorCode 네이밍 규칙

### 1. Not Found (404)
```
{DOMAIN}_IS_NOT_FOUND
```
```java
ACCOUNT_IS_NOT_FOUND(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다.", ""),
USER_IS_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", ""),
HOSPITAL_IS_NOT_FOUND(HttpStatus.NOT_FOUND, "병원을 찾을 수 없습니다.", ""),
USER_INSULIN_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 인슐린 찾을 수 없습니다", ""),
```

### 2. 중복 (400)
```
{DOMAIN}_DUPLICATE_{FIELD}
```
```java
USER_DUPLICATE_LOGIN_ID(HttpStatus.BAD_REQUEST, "이미 존재하는 계정 입니다.", ""),
FAQ_CATEGORY_NAME_IS_DUPLICATED(HttpStatus.BAD_REQUEST, "FAQ 카테고리 이름이 중복됩니다.", ""),
```

### 3. 유효성 검사 실패 (400)
```
{DOMAIN}_IS_INVALID
{FIELD}_IS_INVALID
```
```java
CONTACT_IS_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 전화번호 입니다.", ""),
ROLE_IS_NOT_VALID(HttpStatus.BAD_REQUEST, "존재하지 않는 권한 입니다.", ""),
```

### 4. 인증/인가 (401, 403)
```java
// 401 Unauthorized - 인증 실패
JWT_TOKEN_IS_INVALID(HttpStatus.UNAUTHORIZED, "JWT 토큰이 없거나 유효하지 않습니다.", ""),
AUTHORIZATION_INVALID(HttpStatus.UNAUTHORIZED, "접근 권한이 없습니다.", ""),

// 403 Forbidden - 권한 없음
AUTHENTICATION_INVALID(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", ""),
```

### 5. 비즈니스 규칙 위반 (400)
```java
USER_PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "패스워드가 일치하지 않습니다.", ""),
SMS_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "SMS PIN 이 만료 되었습니다.", ""),
USER_SHARE_CODE_IS_EXPIRED(HttpStatus.BAD_REQUEST, "이미 만료된 연동 코드입니다.", ""),
```

### 6. 외부 서비스 오류 (5xx)
```java
// 503 Service Unavailable
DEXCOM_SERVER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
        "Dexcom 서버 오류입니다. 잠시 후 다시 시도해주세요.",
        "DEXCOM_SERVER_001"
),

// 504 Gateway Timeout
DEXCOM_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT,
        "Dexcom 서버 응답 시간이 초과되었습니다.",
        "DEXCOM_SERVER_002"
),
```

---

## ApiException

비즈니스 예외를 던지는 클래스입니다.

```java
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    // 기본 생성자
    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 커스텀 메시지 생성자
    public ApiException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
```

### 사용 예시

```java
// 기본 사용
public Account getById(String id) {
    return accountRepository.findById(id)
        .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_IS_NOT_FOUND));
}

// 커스텀 메시지
public void validatePermission(Account account, String targetId) {
    if (!account.getId().equals(targetId)) {
        throw new ApiException(ErrorCode.AUTHENTICATION_INVALID,
            "해당 리소스에 대한 접근 권한이 없습니다.");
    }
}
```

---

## ErrorResponse

클라이언트에 반환되는 에러 응답 형식입니다.

```java
@Getter
public class ErrorResponse {
    private final String message;
    private final String errorCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    public ErrorResponse(ErrorCode errorCode) {
        this.message = errorCode.getMessage();
        this.errorCode = errorCode.getErrorCode();
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String message) {
        this.message = message;
        this.errorCode = "";
        this.timestamp = LocalDateTime.now();
    }
}
```

### 응답 예시

```json
{
  "message": "계정을 찾을 수 없습니다.",
  "errorCode": "",
  "timestamp": "2024-01-15 10:30:45"
}
```

---

## HTTP 상태 코드 가이드

| 코드 | 상태 | 용도 |
|------|------|------|
| 200 | OK | 성공 |
| 201 | Created | 생성 성공 |
| 204 | No Content | 삭제 성공 |
| 400 | Bad Request | 잘못된 요청, 유효성 검사 실패, 비즈니스 규칙 위반 |
| 401 | Unauthorized | 인증 실패 (로그인 필요) |
| 403 | Forbidden | 권한 없음 (인증됨, 권한 부족) |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 충돌 (동시성 문제) |
| 500 | Internal Server Error | 서버 내부 오류 |
| 503 | Service Unavailable | 외부 서비스 오류 |
| 504 | Gateway Timeout | 외부 서비스 타임아웃 |

---

## 예외 처리 흐름

### 1. Service/Facade에서 예외 던지기
```java
@Service
public class AccountService {

    public Account getById(String id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_IS_NOT_FOUND));
    }

    public void updateLoginId(Account account, String newLoginId) {
        accountRepository.findByLoginId(newLoginId)
            .ifPresent(existing -> {
                throw new ApiException(ErrorCode.USER_DUPLICATE_LOGIN_ID);
            });
        // ...
    }
}
```

### 2. ExceptionHandler에서 처리 (GlobalExceptionHandler)
```java
@RestControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(new ErrorResponse(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("유효성 검사 실패");
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(message));
    }
}
```

---

## 새 ErrorCode 추가 가이드

1. **적절한 HTTP 상태 코드 선택**
2. **명확한 네이밍 규칙 준수**
3. **사용자 친화적 메시지 작성**
4. **필요시 errorCode 추가** (클라이언트 식별용)

```java
// 예시: 새로운 도메인 추가 시
PRODUCT_IS_NOT_FOUND(HttpStatus.NOT_FOUND,
        "제품을 찾을 수 없습니다.",
        ""
),

PRODUCT_DUPLICATE_NAME(HttpStatus.BAD_REQUEST,
        "이미 존재하는 제품명입니다.",
        ""
),

PRODUCT_STOCK_INSUFFICIENT(HttpStatus.BAD_REQUEST,
        "재고가 부족합니다.",
        "PRODUCT_001"
),
```

---

## ErrorCode 그룹별 예시

### 계정 관련
```java
ACCOUNT_IS_NOT_FOUND(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다.", ""),
ACCOUNT_IS_NOT_HOSPITAL(HttpStatus.BAD_REQUEST, "병원 계정이 아닙니다.", ""),
USER_DUPLICATE_LOGIN_ID(HttpStatus.BAD_REQUEST, "이미 존재하는 계정 입니다.", ""),
USER_PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "패스워드가 일치하지 않습니다.", ""),
```

### 인증 관련
```java
JWT_TOKEN_IS_INVALID(HttpStatus.UNAUTHORIZED, "JWT 토큰이 없거나 유효하지 않습니다.", ""),
AUTHENTICATION_INVALID(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", ""),
AUTHORIZATION_INVALID(HttpStatus.UNAUTHORIZED, "접근 권한이 없습니다.", ""),
```

### SMS 인증 관련
```java
SMS_VERIFICATION_IS_NOT_FOUND(HttpStatus.NOT_FOUND, "SMS 인증을 찾을 수 없습니다.", ""),
SMS_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "SMS PIN 이 만료 되었습니다.", ""),
SMS_PIN_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "SMS PIN 이 검증되지 않았습니다.", ""),
SMS_STATUS_ALREADY_USED(HttpStatus.BAD_REQUEST, "이미 사용된 인증번호입니다.", ""),
```

### 외부 서비스 (Dexcom, OAuth)
```java
DEXCOM_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "Dexcom 인증에 실패했습니다.", "DEXCOM_AUTH_001"),
DEXCOM_SERVER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Dexcom 서버 오류입니다.", "DEXCOM_SERVER_001"),
OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth Provider입니다.", "OAUTH_001"),
OAUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "OAuth 토큰이 만료되었습니다.", "OAUTH_012"),
```

---

## errorCode 규칙

### 형식: `{도메인약어}_{HTTP상태약어}_{순번}`

모든 ErrorCode에는 `errorCode` 필드를 **필수**로 지정합니다.

### 도메인 약어

| 도메인 | 약어 |
|--------|------|
| Common | `CMN` |
| Account | `ACC` |
| Member | `MBR` |
| BrokerageOffice | `BRK` |

> 새 도메인 추가 시 **3글자 약어**를 이 표에 추가합니다.

### HTTP 상태 약어

| HTTP 상태 | 약어 |
|-----------|------|
| 400 Bad Request | `BR` |
| 401 Unauthorized | `UA` |
| 403 Forbidden | `FB` |
| 404 Not Found | `NF` |
| 500 Internal Server Error | `ISE` |
| 503 Service Unavailable | `SU` |
| 504 Gateway Timeout | `GT` |

### 순번

- 같은 도메인 + 같은 HTTP 상태 내에서 `001`부터 순차 증가
- 삭제된 번호는 재사용하지 않음

### 예시

```java
INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
        "서버 내부 오류가 발생했습니다.",
        "CMN_ISE_001"
),

ACCOUNT_IS_NOT_FOUND(HttpStatus.NOT_FOUND,
        "계정을 찾을 수 없습니다.",
        "ACC_NF_001"
),

ACCOUNT_DUPLICATE_LOGIN_ID(HttpStatus.BAD_REQUEST,
        "이미 존재하는 로그인 ID입니다.",
        "ACC_BR_001"
),

MEMBER_IS_NOT_FOUND(HttpStatus.NOT_FOUND,
        "회원을 찾을 수 없습니다.",
        "MBR_NF_001"
),
```
