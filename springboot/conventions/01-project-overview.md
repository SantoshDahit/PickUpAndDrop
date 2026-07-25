# 프로젝트 개요

## 기술 스택

| 구분 | 기술 | 비고 |
|------|------|------|
| Language | Java | - |
| Framework | Spring Boot | - |
| Build Tool | Gradle | - |
| ORM | Spring Data JPA | - |
| Query Builder | QueryDSL | Jakarta 분류자 사용 |
| Object Mapping | ModelMapper | - |
| JWT | jjwt | - |
| Database | MySQL | - |
| Migration | Flyway | - |

---

## 주요 의존성

### Core
```groovy
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-validation'
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```

### 프로젝트 내 공통 클래스

아래 클래스는 프로젝트의 `common/` 패키지에 직접 포함합니다.
(템플릿 파일은 [templates/](./templates/) 참조)

- `BaseMapper`: Entity ↔ DTO 변환을 위한 추상 클래스 ([상세](./05-mapper-convention.md))
- `QueryDslUtil`: QueryDSL 정렬 처리 유틸리티

### QueryDSL
```groovy
implementation "com.querydsl:querydsl-jpa:${querydslVersion}:jakarta"
annotationProcessor(
    "com.querydsl:querydsl-apt:${querydslVersion}:jakarta",
    "jakarta.annotation:jakarta.annotation-api",
    "jakarta.persistence:jakarta.persistence-api"
)
```

### JWT
```groovy
implementation 'io.jsonwebtoken:jjwt-api:${jjwtVersion}'
implementation 'io.jsonwebtoken:jjwt-impl:${jjwtVersion}'
implementation 'io.jsonwebtoken:jjwt-jackson:${jjwtVersion}'
```

### 외부 서비스 (프로젝트에 따라 선택)
```groovy
implementation 'software.amazon.awssdk:s3:${awsVersion}'              // S3 파일 업로드
implementation 'com.google.firebase:firebase-admin:${firebaseVersion}' // FCM 푸시 알림
implementation 'net.nurigo:sdk:${smsVersion}'                          // SMS 발송
```

---

## 패키지 구조

```
src/main/java/com/example/
├── annotation/          # 커스텀 어노테이션
├── aop/                 # AOP 관련 클래스
├── common/              # 공통 클래스
├── config/              # 설정 클래스 (Security, JPA 등)
├── controller/          # REST API Controller
├── domain/              # 도메인 로직 (복잡한 비즈니스 로직)
├── dto/                 # Data Transfer Object
├── entity/              # JPA Entity
│   └── base/           # Base Entity 클래스들
├── enums/               # Enum 타입 정의
├── exception/           # 예외 처리 (ErrorCode, ApiException)
├── factory/             # 팩토리 클래스
├── mapper/              # Entity ↔ DTO 변환 Mapper
├── repository/          # Repository 계층
├── scheduler/           # 스케줄러
├── security/            # Security 관련 (JWT, Filter 등)
├── service/             # Service & Facade 계층
├── sms/                 # SMS 관련 로직
├── util/                # 유틸리티 클래스
└── validation/          # 커스텀 Validation
```

---

## 환경 프로필

| 프로필 | 용도 |
|--------|------|
| `local` | 로컬 개발 환경 |
| `dev` | 개발 서버 환경 |
| `prod` | 운영 서버 환경 |

---

## 주요 설정

### Application 설정

```java
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class Application {

    @PostConstruct
    void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));  // 프로젝트 타임존에 맞게 변경
    }
}
```

| 어노테이션 | 설명 |
|-----------|------|
| `@EnableJpaAuditing` | `createdAt`, `updatedAt` 자동 설정 |
| `@EnableScheduling` | `@Scheduled` 스케줄러 활성화 |
| `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` | Page 응답을 깔끔한 DTO 형식으로 직렬화 |

> **VIA_DTO**: 이 설정이 없으면 Page 응답에 `pageable`, `sort`, `first`, `last`, `empty` 등 불필요한 필드가 모두 포함됩니다. `VIA_DTO` 모드를 사용하면 `content` + `page` 만 포함하는 간결한 응답을 반환합니다.

### 타임존 설정

타임존은 **JVM, Hibernate JDBC, Jackson** 3곳을 모두 동일하게 맞춰야 합니다.
서로 다르면 DB 저장 시각과 API 응답 시각이 어긋나는 문제가 발생합니다.

#### 1. JVM 타임존 (Application 클래스)
```java
@PostConstruct
void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
}
```

#### 2. Hibernate JDBC 타임존 (application-common.yml)
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: Asia/Seoul
```

#### 3. Jackson 직렬화 타임존 (application-common.yml)
```yaml
spring:
  jackson:
    time-zone: Asia/Seoul
    date-format: yyyy-MM-dd HH:mm:ss
```

| 설정 위치 | 영향 범위 |
|----------|----------|
| JVM `TimeZone.setDefault()` | `LocalDateTime.now()`, 스케줄러, 로그 시각 |
| Hibernate `jdbc.time_zone` | DB에 `TIMESTAMP` 저장/조회 시 타임존 변환 |
| Jackson `time-zone` | API JSON 응답의 날짜 직렬화 |

> **주의**: 3곳의 타임존이 일치하지 않으면 DB에 저장된 시각과 API 응답 시각이 달라질 수 있습니다. 프로젝트에 맞는 타임존(예: `Asia/Seoul`, `UTC`, `America/New_York` 등)을 선택한 후 3곳 모두 동일하게 설정하세요.

### QueryDSL Q클래스 생성
```bash
./gradlew compileJava
```
- Q클래스는 `build/generated/sources/annotationProcessor/java/main/` 에 생성됩니다.

---

## 데이터베이스 마이그레이션

Flyway를 사용하여 DB 스키마를 관리합니다.

```
src/main/resources/db/migration/
├── V1__init.sql
├── V2__add_user_table.sql
└── ...
```

---

## 빌드 및 실행

### 빌드
```bash
./gradlew build
```

### 실행
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 테스트
```bash
./gradlew test
```
