# 아키텍처

## 4계층 구조

이 프로젝트는 **4계층 아키텍처**를 따릅니다.

```
┌─────────────────────────────────────────────────────────────┐
│                       Controller                             │
│  - HTTP 요청/응답 처리                                        │
│  - 파라미터 바인딩 및 Validation                              │
│  - Facade만 의존                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        Facade                                │
│  - 트랜잭션 경계 정의 (@Transactional)                        │
│  - 여러 Service 조합                                          │
│  - DTO ↔ Entity 변환 (Mapper 사용)                           │
│  - 비즈니스 흐름 조율                                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        Service                               │
│  - 단일 도메인 책임                                           │
│  - Repository만 의존                                          │
│  - Entity 반환                                                │
│  - 기본 CRUD 로직                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       Repository                             │
│  - 데이터 접근 추상화                                         │
│  - JpaRepository + QueryDSL 조합                             │
│  - 3계층 구조 (Interface, Jpa, Query)                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 데이터 흐름

### 조회 요청 흐름
```
Client Request
      │
      ▼
Controller (SearchRequest DTO 바인딩)
      │
      ▼
Facade (@Transactional(readOnly = true))
      │ SearchRequest → Service
      ▼
Service.search(searchRequest, pageable)
      │
      ▼
Repository.search() → Page<Entity>
      │
      ▼
Facade: Entity → Response DTO (Mapper)
      │
      ▼
Controller → Client Response
```

### 생성 요청 흐름
```
Client Request (JSON Body)
      │
      ▼
Controller (@RequestBody PostRequest DTO)
      │
      ▼
Facade (@Transactional)
      │ PostRequest → Entity 생성
      │ Service.save(entity)
      ▼
Service.save()
      │
      ▼
Repository.save() → Entity
      │
      ▼
Facade: Entity → Response DTO (Mapper)
      │
      ▼
Controller → Client Response
```

---

## 각 레이어 책임

### Controller
| 책임 | 설명 |
|------|------|
| HTTP 요청 처리 | `@GetMapping`, `@PostMapping` 등 |
| 파라미터 바인딩 | `@PathVariable`, `@RequestBody`, `@ModelAttribute` |
| Validation | `@Valid` 어노테이션으로 DTO 검증 |
| 응답 상태 코드 | `@ResponseStatus` |

```java
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountFacade accountFacade;  // Facade만 의존

    @GetMapping("/{accountId}")
    public AccountDto.Response getById(@PathVariable String accountId) {
        return accountFacade.getById(accountId);
    }
}
```

### Facade
| 책임 | 설명 |
|------|------|
| 트랜잭션 경계 | `@Transactional` 정의 |
| Service 조합 | 여러 Service 호출 및 조율 |
| DTO 변환 | Entity ↔ DTO 변환 (Mapper 사용) |
| 비즈니스 흐름 | 복잡한 비즈니스 로직 조율 |

```java
@Component
@RequiredArgsConstructor
public class AccountFacade {
    private final AccountService accountService;
    private final SmsVerificationService smsVerificationService;  // 다른 Service
    private final AccountMapper accountMapper;

    @Transactional(readOnly = true)
    public AccountDto.Response getById(String id) {
        return accountMapper.toResponse(
            accountService.getById(id)
        );
    }

    @Transactional
    public AccountDto.Response updatePassword(String accountId, AccountDto.PasswordPatchRequest request) {
        Account account = accountService.getById(accountId);
        return accountMapper.toResponse(
            accountService.updatePassword(account, request.password())
        );
    }
}
```

### Service
| 책임 | 설명 |
|------|------|
| 단일 도메인 | 하나의 Entity/도메인만 담당 |
| Repository 의존 | 해당 도메인의 Repository만 의존 |
| Entity 반환 | DTO가 아닌 Entity 반환 |
| 기본 CRUD | `getById`, `search`, `save`, `delete` |

```java
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;  // Repository만 의존

    @Transactional(readOnly = true)
    public Account getById(String id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_IS_NOT_FOUND));
    }

    @Transactional
    public Account save(Account account) {
        return accountRepository.save(account);
    }
}
```

### Repository
| 책임 | 설명 |
|------|------|
| 데이터 접근 | DB CRUD 작업 |
| 추상화 | Interface로 추상화 |
| 구현 분리 | JPA / QueryDSL 구현 분리 |

```java
// 인터페이스
public interface AccountRepository {
    Optional<Account> findById(String id);
    Account save(Account account);
    Page<Account> search(AccountDto.SearchRequest searchRequest, Pageable pageable);
}

// 구현체
@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {
    private final AccountJpaRepository accountJpaRepository;
    private final AccountQueryRepository accountQueryRepository;
    // ...
}
```

---

## Facade 사용 기준

### Facade가 필요한 경우
1. **여러 Service 조합이 필요한 경우**
   ```java
   // 계정 생성 시 SMS 인증 확인 필요
   smsVerificationService.validateAndMarkAsUsed(smsVerificationId);
   accountService.save(account);
   ```

2. **DTO 변환이 필요한 경우**
   ```java
   return accountMapper.toResponse(account);
   ```

3. **복잡한 비즈니스 로직 조율**
   ```java
   // 병원 계정 수정: Account + Hospital + HospitalImage 처리
   accountService.updateLoginId(account, request.loginId());
   hospitalService.update(hospital, ...);
   ```

### Service에서 직접 처리하는 경우
- 단순 CRUD (단일 도메인)
- 다른 Service 의존 없음
- DTO 변환 필요 없음

---

## 의존성 규칙

```
Controller ──────────▶ Facade
     │                    │
     │                    ▼
     │               Service (여러 개)
     │                    │
     │                    ▼
     │               Repository
     ▼
 절대 직접 의존하지 않음
```

### 금지 사항
- Controller → Service 직접 의존 ❌
- Controller → Repository 직접 의존 ❌
- Service → 다른 Service 직접 의존 ❌ (Facade에서 조합)
- Facade → Repository 직접 의존 ❌ (Service 통해서만)

### 예외
- 간단한 조회 전용 Controller에서는 Facade 없이 Service 직접 사용 가능
  - 단, DTO 변환이 필요 없고, 단일 Service만 사용하는 경우
