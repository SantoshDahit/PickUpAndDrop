# 네이밍 규칙

## 클래스 네이밍

### 패턴표

| 유형 | 패턴 | 예시 |
|------|------|------|
| Entity | `{Domain}` | `Account`, `User`, `Hospital` |
| DTO | `{Domain}Dto` | `AccountDto`, `UserDto` |
| Repository Interface | `{Domain}Repository` | `AccountRepository` |
| JPA Repository | `{Domain}JpaRepository` | `AccountJpaRepository` |
| Query Repository | `{Domain}QueryRepository` | `AccountQueryRepository` |
| Repository Impl | `{Domain}RepositoryImpl` | `AccountRepositoryImpl` |
| Service | `{Domain}Service` | `AccountService` |
| Facade | `{Domain}Facade` | `AccountFacade` |
| Controller | `{Domain}Controller` | `AccountController` |
| Mapper | `{Domain}Mapper` | `AccountMapper` |
| Enum | `{의미있는 이름}` | `Role`, `Gender`, `DiabetesType` |
| Exception | `{Name}Exception` | `ApiException` |

### 복합 도메인
```java
// 두 도메인 간 관계
UserHospital          // Entity
UserHospitalDto       // DTO
UserHospitalService   // Service

// 하위 개념
UserInsulin           // 사용자의 인슐린 기록
UserMeal              // 사용자의 식사 기록
UserExercise          // 사용자의 운동 기록
```

---

## DTO Inner Class 네이밍

| 클래스명 | 용도 | 타입 |
|----------|------|------|
| `PostRequest` | 생성 요청 | `record` |
| `PatchRequest` | 수정 요청 | `record` |
| `Response` | 상세 응답 | `class` |
| `SummaryResponse` | 목록 응답 | `class` |
| `SearchRequest` | 검색 조건 | `record` |

### 특수 목적 DTO
```java
// 비밀번호 전용
PasswordPatchRequest

// 복합 요청 (다른 DTO 포함)
HospitalPatchRequest  // Account + Hospital 수정

// 통계/집계
UserCountResponse
GlucoseStatisticsResponse
```

---

## 메서드 네이밍

### Service 메서드

| 접두사 | 용도 | 반환 | 예외 |
|--------|------|------|------|
| `getById` | ID로 필수 조회 | Entity | Not Found |
| `getNullableById` | ID로 선택 조회 | Entity/null | - |
| `getByXxx` | 조건 필수 조회 | Entity | Not Found |
| `getNullableByXxx` | 조건 선택 조회 | Entity/null | - |
| `search` | 검색 (페이징) | Page | - |
| `create` | Entity 생성 + 저장 | Entity | - |
| `save` | 이미 생성된 Entity 저장 | Entity | - |
| `delete` | 삭제 | void | - |
| `updateXxx` | 특정 필드 수정 | Entity/void | - |

```java
// 예시
public Account getById(String id);
public Account getNullableByLoginId(String loginId);
public Page<Account> search(SearchRequest request, Pageable pageable);
public User create(Account account, String name, LocalDate birthDate, Gender gender, String contact, DiabetesType diabetesType);
public Account save(Account account);
public void delete(Account account);
public Account updatePassword(Account account, String password);
```

### Facade 메서드

| 접두사 | 용도 | 반환 |
|--------|------|------|
| `getById` | ID로 조회 | DTO |
| `search` | 검색 (페이징) | Page<DTO> |
| `create` | 생성 | DTO |
| `update` | 수정 | DTO |
| `updateXxx` | 특정 필드 수정 | DTO |
| `delete` | 삭제 | void |

```java
// 예시
public AccountDto.Response getById(String id);
public Page<AccountDto.SummaryResponse> search(SearchRequest request, Pageable pageable);
public AccountDto.Response create(AccountDto.PostRequest request);
public AccountDto.Response updatePassword(String id, PasswordPatchRequest request);
public void delete(String id);
```

### Controller 메서드

| 메서드명 | HTTP | 용도 |
|----------|------|------|
| `search` | GET | 목록 검색 |
| `getById` | GET | 단건 조회 |
| `getByXxx` | GET | 조건 조회 |
| `create` | POST | 생성 |
| `update` | PATCH/PUT | 수정 |
| `updateXxx` | PATCH | 특정 필드 수정 |
| `delete` | DELETE | 삭제 |

```java
// 예시
@GetMapping("/search")
public Page<SummaryResponse> search(...);

@GetMapping("/{accountId}")
public Response getById(@PathVariable String accountId);

@PostMapping
public Response create(@RequestBody PostRequest request);

@PatchMapping("/{accountId}")
public Response update(@PathVariable String accountId, @RequestBody PatchRequest request);

@DeleteMapping("/{accountId}")
public void delete(@PathVariable String accountId);
```

### Entity 메서드

| 접두사 | 용도 | 반환 |
|--------|------|------|
| `update` | 수정 | void |
| `updateXxx` | 특정 필드 수정 | void |
| `softDelete` | 논리 삭제 | void |
| `addXxx` | 컬렉션 추가 | void |
| `removeXxx` | 컬렉션 제거 | void |
| `clearXxx` | 컬렉션 초기화 | void |

```java
// 예시
public void update(String name, LocalDate birthDate);
public void updatePassword(String password);
public void updateLoginId(String loginId);
public void softDelete();
public void addImage(HospitalImage image);
public void clearImageList();
```

---

## 변수 네이밍

### 기본 규칙
- **camelCase** 사용
- 의미 있는 이름 사용
- 약어 지양 (id, dto 제외)

### 접미사 규칙

| 접미사 | 용도 | 예시 |
|--------|------|------|
| `List` | 리스트 컬렉션 | `userList`, `roleList` |
| `Map` | 맵 컬렉션 | `userMap`, `cacheMap` |
| `Count` | 개수 | `totalCount`, `newCount` |
| `At` | 시간 | `createdAt`, `deletedAt` |
| `By` | 생성/수정자 | `createdBy`, `updatedBy` |
| `Id` | 식별자 | `accountId`, `userId` |

### 범위 검색 변수

| 접두사 | 용도 | 예시 |
|--------|------|------|
| `min` | 최소값 | `minCreatedAt`, `minAge` |
| `max` | 최대값 | `maxCreatedAt`, `maxAge` |

```java
public record SearchRequest(
    String loginId,
    List<Role> roleList,          // 목록 → List 접미사
    LocalDateTime minCreatedAt,    // 범위 시작 → min 접두사
    LocalDateTime maxCreatedAt     // 범위 끝 → max 접두사
) {}
```

---

## 패키지 네이밍

```
com.example
├── annotation      # 커스텀 어노테이션
├── aop             # AOP
├── common          # 공통 클래스
├── config          # 설정
├── controller      # Controller
├── domain          # 도메인 로직
├── dto             # DTO
├── entity          # Entity
│   └── base       # Base Entity
├── enums           # Enum
├── exception       # 예외
├── factory         # Factory
├── mapper          # Mapper
├── repository      # Repository
├── scheduler       # 스케줄러
├── security        # Security
├── service         # Service, Facade
├── sms             # SMS
├── util            # 유틸리티
└── validation      # Validation
```

---

## 데이터베이스 네이밍

### 테이블명
- **snake_case** 사용
- 단수형 사용

```java
@Table(name = "account")
@Table(name = "user")
@Table(name = "user_hospital")
@Table(name = "user_insulin")
```

### 컬럼명
- **snake_case** 사용

```java
@Column(name = "login_id")
@Column(name = "created_at")
@Column(name = "last_login_at")
@Column(name = "provider_id")
```

### 외래키
```
{참조테이블}_id
```
```java
@JoinColumn(name = "user_id")
@JoinColumn(name = "account_id")
@JoinColumn(name = "hospital_id")
```

> **도메인 작성자/수정자 FK는 별도 컬럼(`creator_id`, `author_id` 등)을 만들지 않고 `BaseCreateEntity` / `BaseFullEntity`의 `created_by` / `updated_by`를 그대로 사용한다. FK 제약(`FOREIGN KEY (created_by) REFERENCES user(id)`)은 해당 도메인 Flyway 마이그레이션에서 추가한다.**

---

## ErrorCode 네이밍

### 패턴

| 유형 | 패턴 | 예시 |
|------|------|------|
| Not Found | `{DOMAIN}_IS_NOT_FOUND` | `ACCOUNT_IS_NOT_FOUND` |
| 중복 | `{DOMAIN}_DUPLICATE_{FIELD}` | `USER_DUPLICATE_LOGIN_ID` |
| 유효성 | `{FIELD}_IS_INVALID` | `CONTACT_IS_INVALID` |
| 권한 | `{ACTION}_INVALID` | `AUTHENTICATION_INVALID` |
| 만료 | `{DOMAIN}_{FIELD}_EXPIRED` | `SMS_CODE_EXPIRED` |
| 실패 | `{ACTION}_FAIL` | `SMS_SEND_FAIL` |

---

## URL 네이밍

### 규칙
- **kebab-case** 사용 (camelCase X)
- 복수형 사용
- 소문자만 사용

```
/v1/accounts           # O
/v1/account            # X (단수형)
/v1/userHospitals      # X (camelCase)
/v1/user-hospitals     # O (kebab-case)
```

---

## 상수 네이밍

### Enum 값
- **UPPER_SNAKE_CASE** 사용

```java
public enum Role {
    USER,
    HOSPITAL,
    ADMIN
}

public enum DiabetesType {
    TYPE_1,
    TYPE_2,
    GESTATIONAL
}
```

### static final 상수
```java
public static final String DEFAULT_SORT_FIELD = "createdAt";
public static final int MAX_PAGE_SIZE = 100;
```

---

## 약어 규칙

### 허용되는 약어
| 약어 | 의미 |
|------|------|
| `id` | identifier |
| `dto` | Data Transfer Object |
| `jpa` | Java Persistence API |
| `api` | Application Programming Interface |
| `url` | Uniform Resource Locator |
| `sms` | Short Message Service |
| `fcm` | Firebase Cloud Messaging |
| `oauth` | Open Authorization |
| `jwt` | JSON Web Token |

### 약어 사용 예시
```java
// 클래스명에서는 대문자로
AccountDto
AccountJpaRepository
SmsService
FcmService
OAuthProvider

// 변수명에서는 camelCase
String accountId;
String jwtToken;
String smsCode;
```
