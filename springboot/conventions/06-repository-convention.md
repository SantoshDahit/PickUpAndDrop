# Repository 컨벤션

## 3계층 Repository 패턴

Repository는 4개의 파일로 구성됩니다.

```
repository/
├── UserInsulinRepository.java       # 인터페이스 (추상화)
├── UserInsulinJpaRepository.java    # Spring Data JPA
├── UserInsulinQueryRepository.java  # QueryDSL
└── UserInsulinRepositoryImpl.java   # 구현체 (조합)
```

---

## 1. Repository 인터페이스

Service에서 사용하는 인터페이스입니다. 구현 방식(JPA/QueryDSL)을 숨깁니다.

```java
package com.example.repository;

import com.example.dto.UserInsulinDto;
import com.example.entity.UserInsulin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserInsulinRepository {

    // 기본 CRUD
    Optional<UserInsulin> findById(String id);
    UserInsulin save(UserInsulin userInsulin);
    void delete(UserInsulin userInsulin);

    // 검색
    Page<UserInsulin> search(UserInsulinDto.SearchRequest searchRequest, Pageable pageable);
}
```

---

## 2. JpaRepository

Spring Data JPA를 상속하여 기본 CRUD를 제공합니다.

```java
package com.example.repository;

import com.example.entity.UserInsulin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInsulinJpaRepository extends JpaRepository<UserInsulin, String> {
    // 필요한 경우 쿼리 메서드 추가
    // Optional<UserInsulin> findByUserId(String userId);
}
```

### JpaRepository 쿼리 메서드 예시
```java
public interface AccountJpaRepository extends JpaRepository<Account, String> {
    Optional<Account> findByLoginId(String loginId);
    Optional<Account> findByLoginIdAndRole(String loginId, Role role);
    Optional<Account> findByProviderIdAndProvider(String providerId, OAuthProvider provider);
}
```

---

## 3. QueryRepository (QueryDSL)

복잡한 동적 쿼리를 담당합니다.

```java
package com.example.repository;

import com.example.dto.UserInsulinDto;
import com.example.entity.UserInsulin;
import com.example.enums.InjectionTiming;
import com.example.common.QueryDslUtil;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.example.entity.QUserInsulin.userInsulin;

@Repository
@RequiredArgsConstructor
public class UserInsulinQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<UserInsulin> search(UserInsulinDto.SearchRequest searchRequest, Pageable pageable) {

        // 1. 데이터 조회
        List<UserInsulin> content = queryFactory.selectFrom(userInsulin)
            .where(
                eqUserIdIfExists(searchRequest.userId()),
                inInjectionTimingIfExists(searchRequest.injectionTimingList()),
                injectedAtBetween(searchRequest.minInjectedAt(), searchRequest.maxInjectedAt())
            )
            .offset(pageable.getOffset())
            .orderBy(QueryDslUtil.getOrderSpecifiers(pageable.getSort(), userInsulin))
            .limit(pageable.getPageSize())
            .fetch();

        // 2. 전체 카운트 조회
        Long totalCount = Optional.ofNullable(
            queryFactory.select(Wildcard.count)
                .from(userInsulin)
                .where(
                    eqUserIdIfExists(searchRequest.userId()),
                    inInjectionTimingIfExists(searchRequest.injectionTimingList()),
                    injectedAtBetween(searchRequest.minInjectedAt(), searchRequest.maxInjectedAt())
                )
                .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(content, pageable, totalCount);
    }

    // === 동적 조건 메서드 ===

    private BooleanExpression eqUserIdIfExists(String userId) {
        if (Objects.isNull(userId)) {
            return null;  // null 반환 시 조건에서 제외
        }
        return userInsulin.user.id.eq(userId);
    }

    private BooleanExpression inInjectionTimingIfExists(List<InjectionTiming> injectionTimingList) {
        if (Objects.isNull(injectionTimingList) || injectionTimingList.isEmpty()) {
            return null;
        }
        return userInsulin.injectionTiming.in(injectionTimingList);
    }

    private BooleanExpression injectedAtBetween(LocalDateTime minInjectedAt, LocalDateTime maxInjectedAt) {
        if (minInjectedAt != null && maxInjectedAt != null) {
            return userInsulin.injectedAt.between(minInjectedAt, maxInjectedAt);
        } else if (minInjectedAt != null) {
            return userInsulin.injectedAt.goe(minInjectedAt);  // >=
        } else if (maxInjectedAt != null) {
            return userInsulin.injectedAt.loe(maxInjectedAt);  // <=
        }
        return null;
    }
}
```

---

## 4. RepositoryImpl (구현체)

JpaRepository와 QueryRepository를 조합합니다.

```java
package com.example.repository;

import com.example.dto.UserInsulinDto;
import com.example.entity.UserInsulin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserInsulinRepositoryImpl implements UserInsulinRepository {

    private final UserInsulinQueryRepository userInsulinQueryRepository;
    private final UserInsulinJpaRepository userInsulinJpaRepository;

    @Override
    public Page<UserInsulin> search(UserInsulinDto.SearchRequest searchRequest, Pageable pageable) {
        return userInsulinQueryRepository.search(searchRequest, pageable);
    }

    @Override
    public Optional<UserInsulin> findById(String id) {
        return userInsulinJpaRepository.findById(id);
    }

    @Override
    public UserInsulin save(UserInsulin userInsulin) {
        return userInsulinJpaRepository.save(userInsulin);
    }

    @Override
    public void delete(UserInsulin userInsulin) {
        userInsulinJpaRepository.delete(userInsulin);
    }
}
```

---

## 동적 조건 메서드 네이밍 규칙

| 패턴 | 용도 | 반환 |
|------|------|------|
| `eqXxxIfExists` | 단일 값 동등 비교 | `entity.field.eq(value)` |
| `inXxxIfExists` | 목록 포함 여부 | `entity.field.in(list)` |
| `xxxBetween` | 범위 조건 | `between`, `goe`, `loe` |
| `likeXxxIfExists` | 문자열 부분 검색 | `entity.field.contains(value)` |

### 예시
```java
// 단일 값 동등 비교
private BooleanExpression eqUserIdIfExists(String userId) {
    if (Objects.isNull(userId)) {
        return null;
    }
    return userInsulin.user.id.eq(userId);
}

// 목록 포함 여부 (IN)
private BooleanExpression inRoleListIfExists(List<Role> roleList) {
    if (Objects.isNull(roleList) || roleList.isEmpty()) {
        return null;
    }
    return account.role.in(roleList);
}

// 범위 조건
private BooleanExpression createdAtBetween(LocalDateTime minCreatedAt, LocalDateTime maxCreatedAt) {
    if (minCreatedAt != null && maxCreatedAt != null) {
        return account.createdAt.between(minCreatedAt, maxCreatedAt);
    } else if (minCreatedAt != null) {
        return account.createdAt.goe(minCreatedAt);
    } else if (maxCreatedAt != null) {
        return account.createdAt.loe(maxCreatedAt);
    }
    return null;
}

// 문자열 부분 검색 (LIKE)
private BooleanExpression likeLoginIdIfExists(String loginId) {
    if (Objects.isNull(loginId) || loginId.isBlank()) {
        return null;
    }
    return account.loginId.contains(loginId);
}
```

---

## Page 조회 패턴

### 기본 구조
```java
public Page<Entity> search(SearchRequest searchRequest, Pageable pageable) {

    // 1. 데이터 조회 (offset, limit, order)
    List<Entity> content = queryFactory.selectFrom(qEntity)
        .where(/* 조건들 */)
        .offset(pageable.getOffset())
        .orderBy(QueryDslUtil.getOrderSpecifiers(pageable.getSort(), qEntity))
        .limit(pageable.getPageSize())
        .fetch();

    // 2. 전체 카운트 조회
    Long totalCount = Optional.ofNullable(
        queryFactory.select(Wildcard.count)
            .from(qEntity)
            .where(/* 동일한 조건들 */)
            .fetchOne()
    ).orElse(0L);

    // 3. PageImpl 반환
    return new PageImpl<>(content, pageable, totalCount);
}
```

### 정렬 처리
```java
// QueryDslUtil.getOrderSpecifiers를 사용하여 Sort → OrderSpecifier 변환
.orderBy(QueryDslUtil.getOrderSpecifiers(pageable.getSort(), qEntity))

// 기본 정렬이 필요한 경우
.orderBy(qEntity.createdAt.desc())
```

---

## QClass 사용

QueryDSL Q클래스는 static import로 사용합니다.

```java
import static com.example.entity.QUserInsulin.userInsulin;
import static com.example.entity.QAccount.account;
import static com.example.entity.QUser.user;
```

---

## Join 예시

```java
public Page<User> searchWithHospital(SearchRequest searchRequest, Pageable pageable) {
    List<User> content = queryFactory.selectFrom(user)
        .leftJoin(user.account, account).fetchJoin()  // fetchJoin으로 N+1 방지
        .where(
            eqNameIfExists(searchRequest.name()),
            account.deletedAt.isNull()  // Soft Delete 고려
        )
        .offset(pageable.getOffset())
        .orderBy(QueryDslUtil.getOrderSpecifiers(pageable.getSort(), user))
        .limit(pageable.getPageSize())
        .fetch();

    // ... 카운트 조회
}
```

---

## Repository 인터페이스 메서드 명명 규칙

| 메서드명 | 용도 | 반환 타입 |
|----------|------|----------|
| `findById` | ID로 단건 조회 | `Optional<Entity>` |
| `findByXxx` | 조건으로 단건 조회 | `Optional<Entity>` |
| `findAll` | 전체 조회 | `List<Entity>` |
| `search` | 조건 검색 (페이징) | `Page<Entity>` |
| `save` | 저장/수정 | `Entity` |
| `delete` | 삭제 | `void` |
