# Service / Facade 컨벤션

## 역할 분리

| 구분 | Facade | Service |
|------|--------|---------|
| 책임 | 비즈니스 흐름 조율 | 단일 도메인 CRUD |
| 의존 | 여러 Service, Mapper | Repository만 |
| 트랜잭션 | `@Transactional` 정의 | `@Transactional` 전파 |
| 반환 | DTO | Entity |
| 위치 | `service/` 패키지 | `service/` 패키지 |

---

## Service 규칙

### 1. 기본 구조

```java
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;  // Repository만 의존

    // 조회 메서드
    @Transactional(readOnly = true)
    public Account getById(String id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_IS_NOT_FOUND));
    }

    // 검색 메서드
    @Transactional(readOnly = true)
    public Page<Account> search(AccountDto.SearchRequest searchRequest, Pageable pageable) {
        return accountRepository.search(searchRequest, pageable);
    }

    // 생성 메서드 (파라미터 → Entity 생성 + 저장)
    @Transactional
    public User create(Account account, String name, LocalDate birthDate, Gender gender, String contact, DiabetesType diabetesType) {
        User user = new User(account, name, birthDate, gender, contact, diabetesType);
        account.updateUser(user);
        return userRepository.save(user);
    }

    // 저장 메서드 (이미 생성된 Entity 저장)
    @Transactional
    public Account save(Account account) {
        return accountRepository.save(account);
    }

    // 삭제 메서드 (Soft Delete)
    @Transactional
    public void delete(Account account) {
        account.softDelete();
        accountRepository.save(account);
    }
}
```

### 2. Service 메서드 네이밍

| 메서드명 | 용도 | 반환 | 예외 |
|----------|------|------|------|
| `getById` | ID로 필수 조회 | `Entity` | Not Found 시 예외 |
| `getNullableById` | ID로 선택 조회 | `Entity` or `null` | - |
| `getByXxx` | 조건으로 필수 조회 | `Entity` | Not Found 시 예외 |
| `getNullableByXxx` | 조건으로 선택 조회 | `Entity` or `null` | - |
| `search` | 검색 (페이징) | `Page<Entity>` | - |
| `create` | Entity 생성 + 저장 | `Entity` | - |
| `save` | 이미 생성된 Entity 저장 | `Entity` | - |
| `delete` | 삭제 (Soft Delete) | `void` | - |
| `updateXxx` | 특정 필드 수정 | `Entity` | - |

### 3. 조회 메서드 패턴

```java
// 필수 조회 (Not Found 시 예외)
@Transactional(readOnly = true)
public Account getById(String id) {
    return accountRepository.findById(id)
        .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_IS_NOT_FOUND));
}

// 선택 조회 (Not Found 시 null)
@Transactional(readOnly = true)
public Account getNullableByLoginIdAndRole(String loginId, Role role) {
    return accountRepository.findByLoginIdAndRole(loginId, role).orElse(null);
}

// Optional 반환 (호출자가 처리)
@Transactional(readOnly = true)
public Optional<Account> getNullableByProviderIdAndProvider(String providerId, OAuthProvider provider) {
    return accountRepository.findByProviderIdAndProvider(providerId, provider);
}
```

### 4. 수정 메서드 패턴

```java
// 비밀번호 수정 (인코딩 포함)
@Transactional
public Account updatePassword(Account account, String password) {
    account.updatePassword(passwordEncoder.encode(password));
    return accountRepository.save(account);
}

// 로그인 ID 수정 (중복 검사 포함)
@Transactional
public void updateLoginId(Account account, String newLoginId) {
    // 변경 없으면 스킵
    if (account.getLoginId().equals(newLoginId)) {
        return;
    }

    // 중복 검사
    accountRepository.findByLoginId(newLoginId)
        .ifPresent(existing -> {
            throw new ApiException(ErrorCode.USER_DUPLICATE_LOGIN_ID);
        });

    account.updateLoginId(newLoginId);
    accountRepository.save(account);
}
```

---

## Facade 규칙

### 1. 기본 구조

```java
@Component
@RequiredArgsConstructor
public class AccountFacade {

    // 여러 Service 의존
    private final AccountService accountService;
    private final SmsVerificationService smsVerificationService;
    private final HospitalService hospitalService;

    // Mapper 의존
    private final AccountMapper accountMapper;

    // 조회
    @Transactional(readOnly = true)
    public Page<AccountDto.SummaryResponse> search(AccountDto.SearchRequest searchRequest, Pageable pageable) {
        return accountService.search(searchRequest, pageable)
            .map(accountMapper::toSummaryResponse);  // Entity → DTO 변환
    }

    // 상세 조회
    @Transactional(readOnly = true)
    public AccountDto.Response getById(String id) {
        return accountMapper.toResponse(
            accountService.getById(id)
        );
    }

    // 수정 (여러 Service 조합)
    @Transactional
    public AccountDto.Response updatePassword(String accountId, AccountDto.PasswordPatchRequest request) {
        Account account = accountService.getById(accountId);
        return accountMapper.toResponse(
            accountService.updatePassword(account, request.password())
        );
    }

    // 삭제
    @Transactional
    public void delete(String id) {
        Account account = accountService.getById(id);
        accountService.delete(account);
    }
}
```

### 2. 복잡한 비즈니스 로직 조율

```java
@Transactional
public AccountDto.Response updateHospital(String accountId, AccountDto.HospitalPatchRequest request) {
    Account account = accountService.getById(accountId);

    // 1. 권한 검사
    if (!Role.HOSPITAL.equals(account.getRole())) {
        throw new ApiException(ErrorCode.ACCOUNT_IS_NOT_HOSPITAL);
    }

    // 2. loginId 변경 (값이 있는 경우에만)
    if (StringUtils.hasText(request.loginId())) {
        accountService.updateLoginId(account, request.loginId());
    }

    // 3. password 변경 (값이 있는 경우에만)
    if (StringUtils.hasText(request.password())) {
        accountService.updatePassword(account, request.password());
    }

    // 4. hospital 정보 변경 (null이 아닌 경우에만)
    if (Objects.nonNull(request.hospital())) {
        Hospital hospital = hospitalService.getById(account.getHospital().getId());

        Hospital updatedHospital = hospitalService.update(
            hospital,
            request.hospital().hospitalName(),
            request.hospital().representativeName(),
            // ... 기타 필드
        );

        // 이미지 처리
        if (Objects.nonNull(request.hospital().imageList())) {
            hospital.clearImageList();
            for (HospitalImageDto.PostRequest imageRequest : request.hospital().imageList()) {
                FileAttachment fileAttachment = fileAttachmentService.getById(imageRequest.fileAttachmentId());
                hospital.addImage(new HospitalImage(hospital, fileAttachment));
            }
        }

        account.updateHospital(updatedHospital);
    }

    return accountMapper.toResponse(account);
}
```

### 3. 외부 서비스 연동

```java
@Transactional
public AccountDto.Response passwordReset(AccountDto.ResetPasswordRequest request) {

    Account account = accountService.getByLoginId(request.loginId());
    String contact = account.getUser().getContact();

    // SMS 인증 확인 (외부 서비스)
    smsVerificationService.validateAndMarkAsUsedByPasswordReset(
        request.smsVerificationId(),
        contact
    );

    accountService.updatePassword(account, request.password());

    return accountMapper.toResponse(account);
}
```

---

## 트랜잭션 규칙

### 1. 읽기 전용
```java
@Transactional(readOnly = true)  // 읽기 전용 (성능 최적화)
public AccountDto.Response getById(String id) {
    // 조회만 수행
}
```

### 2. 쓰기
```java
@Transactional  // 쓰기 트랜잭션
public AccountDto.Response updatePassword(String accountId, ...) {
    // 수정 수행
}
```

### 3. 트랜잭션 경계

```
Controller (트랜잭션 없음)
    │
    ▼
Facade (@Transactional 시작)
    │
    ├── Service1 (트랜잭션 전파)
    │
    ├── Service2 (트랜잭션 전파)
    │
    └── Service3 (트랜잭션 전파)
    │
Facade (트랜잭션 종료 - Commit/Rollback)
```

### 4. Service에서의 트랜잭션

Service에서도 `@Transactional`을 선언하지만, Facade에서 호출 시 기존 트랜잭션에 참여합니다.

```java
@Service
public class AccountService {

    @Transactional  // Facade의 트랜잭션에 참여 (REQUIRED 기본값)
    public Account save(Account account) {
        return accountRepository.save(account);
    }
}
```

---

## Mapper 사용

### Mapper 클래스

```java
@Component
public class AccountMapper extends BaseMapper<Account, AccountDto> {

    protected AccountMapper(ModelMapper modelMapper) {
        super(modelMapper, Account.class);
        this.registerDtoMapping(AccountDto.class);
        this.registerDtoMapping(AccountDto.Response.class);
        this.registerDtoMapping(AccountDto.SummaryResponse.class);
    }

    public AccountDto.Response toResponse(Account entity) {
        return super.toDto(entity, AccountDto.Response.class);
    }

    public AccountDto.SummaryResponse toSummaryResponse(Account entity) {
        return super.toDto(entity, AccountDto.SummaryResponse.class);
    }
}
```

### Facade에서 사용

```java
@Component
@RequiredArgsConstructor
public class AccountFacade {

    private final AccountMapper accountMapper;

    @Transactional(readOnly = true)
    public AccountDto.Response getById(String id) {
        Account account = accountService.getById(id);
        return accountMapper.toResponse(account);  // Entity → DTO
    }

    @Transactional(readOnly = true)
    public Page<AccountDto.SummaryResponse> search(...) {
        return accountService.search(searchRequest, pageable)
            .map(accountMapper::toSummaryResponse);  // Page<Entity> → Page<DTO>
    }
}
```

---

## 파일 구조 예시

```
service/
├── AccountService.java       # Account 도메인 Service
├── AccountFacade.java        # Account 비즈니스 로직 조율
├── UserService.java          # User 도메인 Service
├── UserFacade.java           # User 비즈니스 로직 조율
├── HospitalService.java      # Hospital 도메인 Service
├── HospitalFacade.java       # Hospital 비즈니스 로직 조율
└── SmsVerificationService.java  # SMS 인증 Service (외부 연동)
```

---

## @Facade 커스텀 어노테이션

### 정의

`@Component`를 감싼 시맨틱(Semantic) 어노테이션으로, Facade 클래스의 역할을 명시적으로 드러냅니다.

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Facade {
    @AliasFor(
            annotation = Component.class
    )
    String value() default "";
}
```

### 동작 원리

- `@Component`를 메타 어노테이션으로 포함하므로 Spring Bean으로 자동 등록됩니다.
- `@AliasFor(annotation = Component.class)`를 사용하여 `value` 속성이 `@Component`의 Bean 이름으로 전달됩니다.
- 별도의 `@ComponentScan` 설정 없이 기존 `@Component`와 동일하게 동작합니다.

### 사용 예시

```java
@Facade  // @Component 대신 사용
@RequiredArgsConstructor
public class AuthFacade {
    private final AccountService accountService;
    private final JwtTokenProvider jwtTokenProvider;
    // ...
}
```

### 현재 사용 현황

| 어노테이션 | 사용 수 | 예시 |
|-----------|---------|------|
| `@Facade` | 2개 | `AuthFacade`, `UserDexcomTokenFacade` |
| `@Component` | ~48개 | `AccountFacade`, `UserFacade` 등 |

### 컨벤션 권장

**새로운 Facade 클래스를 생성할 때는 `@Facade`를 사용합니다.**

```java
// ✅ 권장: @Facade 사용
@Facade
@RequiredArgsConstructor
public class ProductFacade { ... }

// ⚠️ 기존 방식: 동작은 동일하지만 역할이 덜 드러남
@Component
@RequiredArgsConstructor
public class ProductFacade { ... }
```

> `@Facade`를 사용하면 코드만으로 해당 클래스가 Facade 레이어임을 즉시 파악할 수 있습니다. `@Service`가 Service 레이어를 나타내듯, `@Facade`가 Facade 레이어를 나타냅니다.

---

## 어노테이션 정리

| 어노테이션 | Service | Facade |
|-----------|---------|--------|
| `@Service` | O | - |
| `@Facade` (권장) / `@Component` | - | O |
| `@RequiredArgsConstructor` | O | O |
| `@Transactional(readOnly = true)` | 조회 | 조회 |
| `@Transactional` | 수정 | 수정 |
