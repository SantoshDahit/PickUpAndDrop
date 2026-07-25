# 23. 테스트 컨벤션

## 개요

본 프로젝트는 **Testcontainers**를 사용하여 테스트 시 독립된 MySQL Docker 컨테이너를 자동 생성합니다.
로컬 MySQL에 의존하지 않으므로 **Docker만 실행 중이면** 테스트를 바로 수행할 수 있습니다.

---

## 전제 조건

- **Docker Desktop** 또는 Docker Engine이 설치 및 실행 중이어야 합니다.
- 로컬 MySQL 설치는 불필요합니다.

---

## 테스트 인프라 구조

### 핵심 클래스

| 클래스 | 역할 |
|--------|------|
| `IntegrationTestBase` | 모든 통합 테스트의 부모 클래스. `@Transactional`로 테스트 후 롤백 보장 |
| `TestcontainersConfig` | MySQL 8.0 Docker 컨테이너 정의. `@ServiceConnection`으로 datasource 자동 주입 |
| `TestAwsConfig` | AWS S3 Mock 설정 |
| `TestDataHelper` | 테스트 데이터 생성 헬퍼 |
| `TestAuthHelper` | JWT 토큰 생성 헬퍼 |

### 동작 흐름

```
1. 테스트 JVM 시작
2. TestcontainersConfig → MySQL 8.0 Docker 컨테이너 시작 (랜덤 포트)
3. @ServiceConnection → Spring datasource 자동 설정
4. @BeforeAll → Flyway clean + migrate (JVM당 1회)
5. 각 테스트 → @Transactional 내에서 실행 → 종료 후 롤백
6. 테스트 완료 → 컨테이너 종료
```

---

## 테스트 DB 격리

### JVM 간 격리 (병렬 실행)

각 테스트 JVM은 **독립된 MySQL Docker 컨테이너**를 사용합니다.
IDE에서 여러 테스트 클래스를 동시에 실행하거나, Gradle `--parallel` 옵션을 사용해도 DB 충돌이 발생하지 않습니다.

### 테스트 간 격리 (단일 JVM 내)

`IntegrationTestBase`의 `@Transactional`이 각 테스트 메서드를 트랜잭션으로 감싸고, 테스트 종료 시 자동 롤백합니다.

---

## 컨테이너 재사용 (선택)

반복 실행 시 컨테이너 시작 시간(약 5~8초)을 절약하려면 홈 디렉토리에 설정 파일을 추가합니다:

**`~/.testcontainers.properties`**
```properties
testcontainers.reuse.enable=true
```

이 설정을 하면 `./gradlew test`를 반복 실행할 때 기존 컨테이너를 재사용합니다.

---

## 통합 테스트 작성 규칙

### 기본 구조

```java
class SomeControllerTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    @Test
    void 테스트명() throws Exception {
        // given
        Owner owner = dataHelper.createApprovedOwner();
        String token = authHelper.createAccessToken(
                owner.getAccount().getId(),
                owner.getAccount().getLoginId());

        // when & then
        mockMvc.perform(get("/v1/endpoint")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
```

### 규칙

1. **모든 통합 테스트는 `IntegrationTestBase`를 상속**한다.
2. 테스트 데이터는 `TestDataHelper`를 통해 생성한다.
3. 인증이 필요한 요청은 `TestAuthHelper`로 토큰을 생성한다.
4. 테스트 메서드명은 **한글**로 작성한다 (예: `매물_단건조회_성공`).
5. 외부 API(AWS 등)는 Mock으로 대체한다 (`TestAwsConfig`).

---

## 의존성

```groovy
// build.gradle
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:testcontainers-mysql:2.0.4'
testImplementation 'org.springframework.security:spring-security-test'
```

---

## 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests "*.AuthControllerTest"

# 특정 테스트 메서드
./gradlew test --tests "*.AuthControllerTest.로그인_성공"
```
