# 19. 설정 관리 컨벤션

## 개요

본 프로젝트는 **Spring Boot 프로필 기반 설정 관리**를 사용합니다.
프로필을 그룹으로 묶어 공통/환경별/기능별 설정을 분리하고, `@ConfigurationProperties`와 `@Configuration` 빈으로 타입 안전한 설정 바인딩을 수행합니다.

---

## 프로필 구조

### 프로필 그룹

```yaml
# application.yml
spring:
  application:
    name: {project-name}-backend
  profiles:
    default: loc
    group:
      loc: 'common,loc,actuator,flyway'
      dev: 'common,dev,actuator,flyway'
      pro: 'common,pro,actuator,flyway'
```

- **기본 프로필**: `loc` (로컬 개발)
- **그룹 구성**: 각 환경은 `common` + 환경별 + `actuator` + `flyway` 조합

### 설정 파일 매핑

| 파일 | 프로필 | 설명 |
|------|--------|------|
| `application.yml` | (기본) | 프로필 그룹 정의 |
| `application-common.yml` | `common` | 공통 설정 (Timezone, Jackson, CORS 등) |
| `application-loc.yml` | `loc` | 로컬 DB, 로깅 |
| `application-dev.yml` | `dev` | 개발 환경 DB, S3, Slack, OAuth |
| `application-pro.yml` | `pro` | 운영 환경 DB (AWS RDS), S3, OAuth |
| `application-actuator.yml` | `actuator` | Health, Prometheus, Metrics |
| `application-flyway.yml` | `flyway` | DB 마이그레이션 |

---

## 프로필별 설정 차이

### common (공통)

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: Asia/Seoul

  jackson:
    time-zone: Asia/Seoul
    date-format: yyyy-MM-dd HH:mm:ss

app:
  cors:
    allowed-origins:
      - "*"

security:
  whitelist:
    - /**
```

- **Timezone**: `Asia/Seoul` (Hibernate + Jackson 모두)
- **날짜 포맷**: `yyyy-MM-dd HH:mm:ss`
- **CORS**: 모든 도메인 허용 (`*`)
- **Security Whitelist**: 모든 URL 허용 (`/**`)

### dev (개발)

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://data.kayple.com:33060/{project-name}?...
aws:
  s3:
    bucket-name: {project-name}-storage-dev
server:
  port: 20400
```

- **DB 드라이버**: MySQL 표준 (`com.mysql.cj.jdbc.Driver`)
- **포트**: 20400
- **S3 버킷**: `{project-name}-storage-dev`

### pro (운영)

```yaml
spring:
  datasource:
    driver-class-name: software.aws.rds.jdbc.mysql.Driver
    url: jdbc:mysql://${AWS_DB_URL}:33066/{projectname}?...
aws:
  s3:
    bucket-name: {project-name}-storage
server:
  port: 20406
```

- **DB 드라이버**: AWS RDS IAM (`software.aws.rds.jdbc.mysql.Driver`)
- **포트**: 20406
- **S3 버킷**: `{project-name}-storage`

### loc (로컬)

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: ${LOCAL_DB_URL}
    username: ${LOCAL_DB_USERNAME}
    password: ${LOCAL_DB_PASSWORD}
```

- **DB**: 로컬 환경변수로 설정

### actuator (모니터링)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus, metrics
  endpoint:
    health:
      group:
        liveness:
          include: ping
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: [ 0.50, 0.95, 0.99 ]
```

- **노출 엔드포인트**: health, prometheus, metrics
- **Liveness**: `/actuator/health/liveness` (ping)
- **퍼센타일**: P50, P95, P99

### flyway (DB 마이그레이션)

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
    placeholders:
      schema: ${DB_SCHEMA}
```

---

## ConfigurationProperties 패턴

### 사용 규칙

- `@Component` + `@ConfigurationProperties` 또는
- `@Configuration` + `@ConfigurationProperties`
- Lombok `@Getter` / `@Setter`로 바인딩

### 6개 Properties 클래스

#### 1. CorsProperties

```java
// config/CorsProperties.java
@Getter
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {
    @Setter
    private List<String> allowedOrigins;
}
```

#### 2. AwsProperties

```java
// config/AwsProperties.java
@Getter @Setter
@Component
@ConfigurationProperties(prefix = "aws.s3")
public class AwsProperties {
    private String bucketName;
    private String region;
    private Map<String, String> folderPaths;

    public Region getRegionEnum() {
        return Region.of(region);
    }
}
```

#### 3. SecurityProperties

```java
// security/config/SecurityProperties.java
@Setter @Getter
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    private List<String> whitelist = new ArrayList<>();
}
```

#### 4. OAuthProperties

```java
// security/oauth/config/OAuthProperties.java
@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {
    private Provider google = new Provider();
    private Provider kakao = new Provider();
    private Provider naver = new Provider();
    private AppleProvider apple = new AppleProvider();

    @Getter @Setter
    public static class Provider {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
    }

    @Getter @Setter
    public static class AppleProvider {
        private String clientId;    // Bundle ID
        private String teamId;
        private String keyId;
        private String privateKey;  // p8 PEM
        private String serviceId;
        private String redirectUri;
    }
}
```

#### 5. OAuthCallbackProperties

```java
// security/oauth/config/OAuthCallbackProperties.java
@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "oauth.callback")
public class OAuthCallbackProperties {
    private String appScheme = "{projectname}";
    private String appCallbackPath = "/oauth/callback";
    private String webRedirectUrl;

    public String getAppRedirectUrl() {
        return appScheme + "://" + appCallbackPath;
    }
}
```

#### 6. DexcomShareConfig

```java
// dexcom/config/DexcomShareConfig.java
@Configuration
@ConfigurationProperties(prefix = "dexcom.share")
public class DexcomShareConfig {
    private String baseUrl = "https://shareous1.dexcom.com/ShareWebServices/Services";
    private String applicationId = "d89443d2-327c-4a6f-89e5-496bbb0317db";
}
```

---

## @Configuration Bean 클래스

### 10개 Config 클래스

| 클래스 | 역할 | 주요 Bean |
|--------|------|----------|
| `GlobalCorsConfig` | CORS 설정 | `CorsConfigurationSource` |
| `AwsConfig` | AWS S3 클라이언트 | `S3Client`, `S3Presigner` |
| `SecurityConfig` | Spring Security 설정 | `SecurityFilterChain`, `GrantedAuthorityDefaults` |
| `SchedulerConfig` | 스케줄러 스레드 풀 | `ThreadPoolTaskScheduler` (10 threads, `fcm-scheduler-`) |
| `SlackConfig` | Slack 알림 설정 | `SlackUtil` 정적 초기화 |
| `FirebaseConfig` | Firebase 초기화 | `FirebaseApp` |
| `DexcomOAuthConfig` | Dexcom OAuth 설정 | `RestTemplate` |
| `ModelMapperConfig` | ModelMapper 설정 | `ModelMapper` (STANDARD matching) |
| `PasswordEncoderConfig` | 비밀번호 암호화 | `PasswordEncoder` (BCrypt) |
| `RequestLoggingConfig` | 요청 로깅 필터 | `CommonsRequestLoggingFilter` |

### 주요 Config 상세

#### GlobalCorsConfig

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of(
        "X-Faq-Version", "X-Faq-Category-Version",
        "X-Guide-Version", "X-Guide-Category-Version"
    ));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    // ...
}
```

#### SchedulerConfig

```java
@Bean
public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(10);
    scheduler.setThreadNamePrefix("fcm-scheduler-");
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(30);
    return scheduler;
}
```

#### ModelMapperConfig

```java
@Bean
public ModelMapper modelMapper() {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration()
            .setFieldMatchingEnabled(true)
            .setFieldAccessLevel(AccessLevel.PRIVATE)
            .setMatchingStrategy(MatchingStrategies.STANDARD);
    return modelMapper;
}
```

---

## 환경변수 관리 규칙

### 시크릿 (환경변수 필수)

모든 민감 정보는 환경변수로 관리하며, application.yml에 절대 하드코딩하지 않습니다.

- JWT 시크릿: `${JWT_TOKEN_SECRET}`
- DB 비밀번호: `${AWS_DB_PASSWORD}`
- OAuth 클라이언트 시크릿: `${GOOGLE_CLIENT_SECRET}`, `${KAKAO_CLIENT_SECRET}` 등
- SMS API 키: `${SOLAPI_KEY}`, `${SOLAPI_SECRET}`
- Slack 봇 토큰: `${DEV_SLACK_BOT_TOKEN}`

### 하드코딩 허용

- 포트 번호: `20400`, `20406`
- DB 호스트 (dev): `data.kayple.com:33060`
- S3 버킷명: `{project-name}-storage-dev`
- Slack 채널명 (pro): `{project-name}-pro-logs`
- SMS 발신번호: `15517381`
