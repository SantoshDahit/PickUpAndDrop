# Soft Delete 컨벤션

## 개요

데이터를 물리적으로 삭제하지 않고, `deletedAt` 필드에 삭제 시각을 기록하여 논리적으로 삭제합니다.

---

## Soft Delete 방식

### deletedAt 필드

```java
@Column(name = "deleted_at")
private LocalDateTime deletedAt;

public void softDelete() {
    this.deletedAt = LocalDateTime.now();
}
```

| 상태 | `deletedAt` 값 |
|------|---------------|
| 활성 데이터 | `null` |
| 삭제된 데이터 | `2024-01-15T10:30:00` (삭제 시각) |

### Soft Delete 지원 Base Entity

| Base Entity | deletedAt | softDelete() |
|------------|:---------:|:------------:|
| `BaseTimeEntity` | - | - |
| `BaseCreateEntity` | - | - |
| `BaseAuditEntity` | - | - |
| **`BaseFullTimeEntity`** | **O** | **O** |
| **`BaseFullEntity`** | **O** | **O** |

---

## 삭제 흐름

### Service에서 삭제

```java
@Service
public class AccountService {

    @Transactional
    public void delete(Account account) {
        account.softDelete();           // deletedAt = now()
        accountRepository.save(account); // DB에 반영
    }
}
```

### Facade에서 호출

```java
@Component
public class AccountFacade {

    @Transactional
    public void delete(String id, AccountDto.DeleteRequest request) {
        Account account = accountService.getById(id);

        // 비즈니스 검증...
        smsVerificationService.validateAndMarkAsUsed(...);

        accountService.delete(account);  // Soft Delete
    }
}
```

---

## 필터링 방식: QueryDSL 수동 필터링

### 핵심 원칙

이 프로젝트에서는 **전역 필터(@Where, @SQLRestriction)를 사용하지 않습니다.** QueryDSL에서 매번 수동으로 `deletedAt.isNull()` 조건을 추가합니다.

### QueryRepository에서 필터링

```java
@Repository
@RequiredArgsConstructor
public class AccountQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Account> search(AccountDto.SearchRequest searchRequest, Pageable pageable) {

        List<Account> content = queryFactory.selectFrom(account)
            .where(
                eqLoginIdIfExits(searchRequest.loginId()),
                eqRoleListInIfExits(searchRequest.roleList()),
                betweenCreatedAt(searchRequest.minCreatedAt(), searchRequest.maxCreatedAt()),
                account.deletedAt.isNull()  // ⚠️ Soft Delete 필터 필수
            )
            .offset(pageable.getOffset())
            .orderBy(QueryDslUtil.getOrderSpecifiers(pageable.getSort(), account))
            .limit(pageable.getPageSize())
            .fetch();

        Long totalCount = Optional.ofNullable(
                queryFactory.select(Wildcard.count)
                    .from(account)
                    .where(
                        eqLoginIdIfExits(searchRequest.loginId()),
                        eqRoleListInIfExits(searchRequest.roleList()),
                        betweenCreatedAt(searchRequest.minCreatedAt(), searchRequest.maxCreatedAt()),
                        account.deletedAt.isNull()  // ⚠️ count 쿼리에도 동일하게 적용
                    )
                    .fetchOne()
            ).orElse(0L);

        return new PageImpl<>(content, pageable, totalCount);
    }

    // 특정 조건 조회에서도 Soft Delete 필터 적용
    public Optional<Account> findByUserContact(String userContact) {
        return Optional.ofNullable(
            queryFactory.selectFrom(account)
                .where(
                    account.deletedAt.isNull(),      // ⚠️ Soft Delete 필터
                    account.role.eq(Role.USER),
                    account.user.contact.eq(userContact)
                ).fetchFirst()
        );
    }
}
```

---

## 주의사항

### 1. JpaRepository는 삭제된 데이터도 반환

```java
// ⚠️ 주의: 삭제된 데이터도 반환됨!
public interface AccountJpaRepository extends JpaRepository<Account, String> {
    Optional<Account> findByLoginId(String loginId);  // deletedAt 체크 안 함
}
```

JpaRepository의 기본 메서드(`findById`, `findAll`, `findByXxx`)는 **Soft Delete를 고려하지 않습니다.**

### 해결 방법

**방법 1**: QueryRepository를 통해 조회 (권장)
```java
// QueryRepository에서 deletedAt.isNull() 조건 포함
public Optional<Account> findByUserContact(String userContact) {
    return Optional.ofNullable(
        queryFactory.selectFrom(account)
            .where(
                account.deletedAt.isNull(),  // 삭제되지 않은 데이터만
                account.user.contact.eq(userContact)
            ).fetchFirst()
    );
}
```

**방법 2**: Service에서 deletedAt 체크
```java
// JpaRepository로 조회 후 Service에서 검증
public Account getById(String id) {
    Account account = accountRepository.findById(id)
        .orElseThrow(() -> new ApiException(ErrorCode.ACCOUNT_IS_NOT_FOUND));

    if (account.getDeletedAt() != null) {
        throw new ApiException(ErrorCode.ACCOUNT_IS_NOT_FOUND);
    }
    return account;
}
```

### 2. 새 QueryRepository 작성 시 `.deletedAt.isNull()` 누락 주의

```java
// ❌ 잘못된 예: deletedAt 필터 누락
queryFactory.selectFrom(account)
    .where(
        eqLoginIdIfExits(loginId)
        // deletedAt.isNull() 빠짐!
    )
    .fetch();

// ✅ 올바른 예: deletedAt 필터 포함
queryFactory.selectFrom(account)
    .where(
        eqLoginIdIfExits(loginId),
        account.deletedAt.isNull()  // 반드시 포함
    )
    .fetch();
```

### 3. content 쿼리와 count 쿼리의 조건 일치

```java
// content 쿼리
.where(
    eqLoginIdIfExits(loginId),
    account.deletedAt.isNull()   // 조건 A
)

// count 쿼리 - 반드시 동일한 조건!
.where(
    eqLoginIdIfExits(loginId),
    account.deletedAt.isNull()   // 조건 A (동일)
)
```

---

## 전역 필터를 사용하지 않는 이유

| 방식 | 장점 | 단점 |
|------|------|------|
| `@Where` / `@SQLRestriction` (전역) | 자동 적용, 누락 없음 | 삭제된 데이터 조회 불가, 유연성 부족 |
| **QueryDSL 수동 필터 (현재 방식)** | **유연함, 삭제 데이터 조회 가능** | **누락 위험** |

현재 프로젝트에서는 **관리자 화면에서 삭제된 데이터를 조회**하거나, **삭제된 계정의 복구** 등이 필요할 수 있어 수동 필터링 방식을 채택했습니다.

---

## 현재 Soft Delete 사용 Entity

| Entity | Base Entity |
|--------|------------|
| `Account` | `BaseFullTimeEntity` |
| `User` | `BaseFullTimeEntity` |
| `UserGlucose` | `BaseFullTimeEntity` |
| `UserHospital` | `BaseFullTimeEntity` |
| `UserGuardian` | `BaseFullTimeEntity` |
| `Notice` | `BaseFullEntity` |
| `Inquiry` | `BaseFullEntity` |
| `Guide` | `BaseFullEntity` |
| 기타 다수 | 도메인에 따라 다름 |

---

## PK 공유 1:1 엔티티 쌍 (예: `Account ↔ User`)

### 정의

`@MapsId` 등으로 동일 PK를 공유하는 1:1 엔티티 쌍. 현재 프로젝트에서는 `Account ↔ User`가 해당된다. 두 엔티티 모두 `BaseFullTimeEntity`를 상속해 각자 `deletedAt`을 보유한다.

### 원칙

두 row는 같은 PK로 항상 함께 살고 함께 죽는다. 한쪽만 `softDelete()` 하면 `deletedAt` 값이 어긋나 조회·통계가 inconsistency를 일으킨다.

### 규칙

회원 탈퇴 등 쌍을 죽이는 작업은 **Facade의 단일 트랜잭션 안에서 두 엔티티 모두 `softDelete()` 호출**한다.

```java
@Component
@RequiredArgsConstructor
public class AccountFacade {

    private final AccountService accountService;

    @Transactional
    public void withdraw(String accountId) {
        Account account = accountService.getById(accountId);
        User user = account.getUser();

        user.softDelete();
        account.softDelete();
    }
}
```

### 2차 확장 안내

소셜 로그인 3종 도입으로 1 user : N accounts로 확장되면 의미가 갈라진다:

- 단일 `account.softDelete()` → "소셜 연결 해제" (다른 provider 계정은 계속 살아 있음)
- 모든 account + user 동반 `softDelete()` → "회원 탈퇴"

이때부터는 호출 위치를 별도 Facade 메서드로 분리해 의미를 명시한다.

---

## 새 도메인 추가 시 체크리스트

- [ ] Entity가 `BaseFullTimeEntity` 또는 `BaseFullEntity`를 상속하는지 확인
- [ ] QueryRepository의 모든 `where` 절에 `.deletedAt.isNull()` 포함
- [ ] content 쿼리와 count 쿼리의 조건이 동일한지 확인
- [ ] Service의 `delete()` 메서드가 `entity.softDelete()` + `save()` 패턴인지 확인
- [ ] JpaRepository 메서드를 직접 사용하는 곳이 있다면 Soft Delete 고려 여부 확인
- [ ] PK 공유 1:1 엔티티 쌍은 한 Facade 메서드에서 두 엔티티 모두 `softDelete()` 호출했는지 확인
