# API 로깅 컨벤션

## 개요

AOP 기반으로 모든 REST API 호출을 자동 로깅합니다. 요청/응답 정보를 DB에 저장하여 운영 모니터링과 디버깅에 활용합니다.

---

## 구성 요소

| 파일 | 역할 |
|------|------|
| `ApiLoggingAspect.java` | AOP Aspect - API 호출 인터셉트 및 로깅 |
| `@NoApiLogging` | 로깅 제외 어노테이션 |
| `ApiCallLog.java` | 로그 저장 Entity |
| `ApiCallLogService.java` | 로그 저장 Service |
| `RequestLoggingConfig.java` | Spring 레벨 요청 로깅 설정 |

---

## ApiLoggingAspect

### 동작 방식

```java
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApiLoggingAspect {

    private final ApiCallLogService apiCallLogService;
    private final ObjectMapper objectMapper;

    private static final int MAX_LENGTH = 1000;

    @Around("within(@org.springframework.web.bind.annotation.RestController *) " +
            "&& !@annotation(com.example.myapp.annotation.NoApiLogging)")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        // ...
    }
}
```

### Pointcut 설명

```java
@Around("within(@org.springframework.web.bind.annotation.RestController *) " +
        "&& !@annotation(com.example.myapp.annotation.NoApiLogging)")
```

| 조건 | 설명 |
|------|------|
| `within(@RestController *)` | 모든 `@RestController` 클래스의 메서드 |
| `!@annotation(NoApiLogging)` | `@NoApiLogging`이 붙은 메서드 제외 |

### 로깅 항목

| 항목 | 설명 | 조건 |
|------|------|------|
| `httpMethod` | HTTP 메서드 (GET, POST 등) | 항상 |
| `endpoint` | 요청 URI | 항상 |
| `queryParams` | 쿼리 파라미터 (JSON) | GET 요청만 |
| `requestBody` | 요청 본문 (JSON) | POST, PUT, PATCH만 |
| `clientIp` | 클라이언트 IP | 항상 |
| `userAgent` | User-Agent 헤더 | 항상 |
| `statusCode` | HTTP 응답 상태 코드 | 항상 |
| `errorMessage` | 에러 메시지 (최대 1000자) | 에러 발생 시 |
| `executionTimeMs` | 실행 시간 (밀리초) | 항상 |

### 에러별 상태 코드 기록

```java
try {
    result = joinPoint.proceed();
} catch (ApiException e) {
    statusCode = e.getErrorCode().getHttpStatus().value();  // 해당 ErrorCode의 HTTP 상태
    errorMessage = e.getMessage();
    throw e;
} catch (MethodArgumentNotValidException |
         HttpMessageNotReadableException |
         MissingServletRequestParameterException |
         MethodArgumentTypeMismatchException |
         NoResourceFoundException e) {
    statusCode = HttpStatus.BAD_REQUEST.value();  // 400
    errorMessage = e.getMessage();
    throw e;
} catch (Exception e) {
    statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();  // 500
    errorMessage = e.getMessage();
    throw e;
}
```

| 예외 타입 | 기록되는 statusCode |
|----------|-------------------|
| `ApiException` | ErrorCode의 httpStatus |
| `MethodArgumentNotValidException` | 400 |
| `HttpMessageNotReadableException` | 400 |
| `MissingServletRequestParameterException` | 400 |
| `MethodArgumentTypeMismatchException` | 400 |
| `NoResourceFoundException` | 400 |
| 기타 `Exception` | 500 |

### requestBody 로깅 조건

```java
private boolean shouldLogRequestBody(String httpMethod) {
    return "POST".equalsIgnoreCase(httpMethod) ||
           "PUT".equalsIgnoreCase(httpMethod) ||
           "PATCH".equalsIgnoreCase(httpMethod);
}
```

- **POST, PUT, PATCH**: requestBody 로깅
- **GET, DELETE**: queryParams만 로깅 (body 없음)

### 민감정보 마스킹

```java
private String maskSensitiveFields(String jsonString) {
    try {
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        if (jsonNode instanceof ObjectNode objectNode) {
            String[] sensitiveFields = {"password"};

            for (String field : sensitiveFields) {
                if (objectNode.has(field)) {
                    String originalValue = objectNode.get(field).asText();
                    String maskedValue = "*".repeat(originalValue.length());
                    objectNode.put(field, maskedValue);
                }
            }
        }
        return objectMapper.writeValueAsString(jsonNode);
    } catch (Exception e) {
        return jsonString;
    }
}
```

- `password` 필드는 `*` 문자로 자동 마스킹
- 예: `{"loginId":"admin","password":"secret123"}` → `{"loginId":"admin","password":"*********"}`
- 새 민감 필드 추가 시: `sensitiveFields` 배열에 필드명 추가

---

## @NoApiLogging 어노테이션

로깅을 제외할 API 메서드에 부착합니다.

### 정의

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoApiLogging {
}
```

### 사용 예시

```java
@RestController
@RequestMapping("/v1/files")
public class FileAttachmentController {

    /**
     * PreSigned URL 생성 #15
     */
    @NoApiLogging  // 파일 업로드 URL은 로깅 불필요
    @PostMapping("/presigned-url")
    public FileDto.PreSignedUrlResponse generatePreSignedUrl(...) {
        // ...
    }
}
```

### 제외 대상 기준

| 상황 | @NoApiLogging 적용 |
|------|-------------------|
| 대용량 요청/응답 (파일 업로드 URL) | O |
| 헬스 체크, 모니터링 API | O |
| 빈번한 호출 (폴링) | O (선택) |
| 일반 CRUD API | X |

---

## ApiCallLog Entity

```java
@Entity
@Table(name = "api_call_log")
@Getter
@NoArgsConstructor
public class ApiCallLog extends BaseCreateEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "http_method", length = 10, nullable = false)
    private String httpMethod;

    @Column(name = "endpoint", length = 500, nullable = false)
    private String endpoint;

    @Column(name = "query_params", columnDefinition = "TEXT")
    private String queryParams;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "client_ip", length = 45, nullable = false)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    public ApiCallLog(String httpMethod, String endpoint, String queryParams,
                      String requestBody, String clientIp, String userAgent,
                      Integer statusCode, String errorMessage, Integer executionTimeMs) {
        this.id = UUID.randomUUID().toString();
        this.httpMethod = httpMethod;
        this.endpoint = endpoint;
        this.queryParams = queryParams;
        this.requestBody = requestBody;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.executionTimeMs = executionTimeMs;
    }
}
```

- `BaseCreateEntity` 상속 → `createdAt`, `createdBy` 자동 기록
- 수정/삭제 없는 로그성 데이터이므로 `BaseCreateEntity` 사용

---

## RequestLoggingConfig

Spring 레벨의 요청 로깅을 추가로 설정합니다. (DEBUG 레벨 로그)

```java
@Configuration
public class RequestLoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setIncludeHeaders(true);
        filter.setMaxPayloadLength(100000);
        filter.setAfterMessagePrefix("📥 Incoming Request: ");
        return filter;
    }
}
```

| 설정 | 값 | 설명 |
|------|-----|------|
| `includeQueryString` | `true` | 쿼리 파라미터 포함 |
| `includePayload` | `true` | 요청 본문 포함 |
| `includeHeaders` | `true` | HTTP 헤더 포함 |
| `maxPayloadLength` | `100000` | 로깅할 최대 페이로드 크기 |

> 이 필터는 Spring의 DEBUG 로깅을 통해 출력됩니다. `ApiLoggingAspect`와는 별개로 동작합니다.

---

## 새 프로젝트 적용 시 체크리스트

- [ ] `ApiLoggingAspect` 클래스 생성
- [ ] `@NoApiLogging` 어노테이션 생성
- [ ] `ApiCallLog` Entity 생성 (`BaseCreateEntity` 상속)
- [ ] `ApiCallLogService` 생성 (save 메서드)
- [ ] `api_call_log` 테이블 DDL 작성 (Flyway 마이그레이션)
- [ ] `RequestLoggingConfig` 설정 (선택)
- [ ] 로깅 제외할 API에 `@NoApiLogging` 부착
