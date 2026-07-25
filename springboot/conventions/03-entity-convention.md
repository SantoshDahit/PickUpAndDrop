# Entity 컨벤션

## Base Entity 종류

프로젝트에서 5가지 Base Entity를 제공합니다. 요구사항에 맞는 것을 선택하여 상속합니다.

### 1. BaseTimeEntity
**용도**: 기본 시간 정보만 필요한 경우

| 필드 | 타입 | 설명 |
|------|------|------|
| `createdAt` | `LocalDateTime` | 생성 시간 (자동) |
| `updatedAt` | `LocalDateTime` | 수정 시간 (자동) |

```java
@Getter
@MappedSuperclass
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

---

### 2. BaseAuditEntity
**용도**: 생성자 정보 + 시간 정보가 필요한 경우

| 필드 | 타입 | 설명 |
|------|------|------|
| `createdAt` | `LocalDateTime` | 생성 시간 (자동) |
| `createdBy` | `String` | 생성자 (자동, Security Context) |
| `updatedAt` | `LocalDateTime` | 수정 시간 (자동) |

```java
@Getter
@MappedSuperclass
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

---

### 3. BaseCreateEntity
**용도**: 생성 정보만 필요하고, 수정 시간이 불필요한 경우 (로그성 데이터)

| 필드 | 타입 | 설명 |
|------|------|------|
| `createdAt` | `LocalDateTime` | 생성 시간 (자동) |
| `createdBy` | `String` | 생성자 (자동) |

```java
@Getter
@MappedSuperclass
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseCreateEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
}
```

---

### 4. BaseFullTimeEntity (가장 많이 사용)
**용도**: Soft Delete가 필요한 일반 Entity

| 필드 | 타입 | 설명 |
|------|------|------|
| `createdAt` | `LocalDateTime` | 생성 시간 (자동) |
| `updatedAt` | `LocalDateTime` | 수정 시간 (자동) |
| `deletedAt` | `LocalDateTime` | 삭제 시간 (Soft Delete) |

```java
@Getter
@MappedSuperclass
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseFullTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
```

---

### 5. BaseFullEntity
**용도**: 생성자 정보 + Soft Delete가 모두 필요한 경우

| 필드 | 타입 | 설명 |
|------|------|------|
| `createdAt` | `LocalDateTime` | 생성 시간 (자동) |
| `createdBy` | `String` | 생성자 (자동) |
| `updatedAt` | `LocalDateTime` | 수정 시간 (자동) |
| `deletedAt` | `LocalDateTime` | 삭제 시간 (Soft Delete) |

```java
@Getter
@MappedSuperclass
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseFullEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
```

---

## Base Entity 선택 가이드

| 요구사항 | 선택 |
|----------|------|
| 기본 시간만 필요 | `BaseTimeEntity` |
| 생성자 추적 필요 | `BaseAuditEntity` |
| 로그/이력 데이터 (수정 없음) | `BaseCreateEntity` |
| Soft Delete 필요 (일반적인 경우) | `BaseFullTimeEntity` |
| Soft Delete + 생성자 추적 | `BaseFullEntity` |

---

## UUID ID 생성 전략

모든 Entity의 ID는 **UUID 문자열**을 사용합니다.

```java
@Id
@Column(updatable = false, nullable = false)
private String id;

// 생성자에서 UUID 할당
public Account(String loginId, String password, Role role) {
    this.id = UUID.randomUUID().toString();
    this.loginId = loginId;
    this.password = password;
    this.role = role;
}
```

### UUID 사용 이유
1. **분산 환경**: DB 시퀀스 의존 없이 ID 생성
2. **보안**: 예측 불가능한 ID
3. **마이그레이션**: 데이터 이관 시 ID 충돌 방지

---

## Entity 클래스 규칙

### 1. 기본 어노테이션
```java
@Entity
@Table(name = "account")  // 테이블명 명시 (snake_case)
@Getter
@NoArgsConstructor        // JPA 요구사항 (protected 권장)
public class Account extends BaseFullTimeEntity {
```

### 2. NoArgsConstructor 접근 제한
```java
// 권장: Entity에서는 access = PROTECTED 생략 가능 (JPA 프록시 요구)
@NoArgsConstructor
public class Account extends BaseFullTimeEntity {

// Base Entity에서는 반드시 PROTECTED
@NoArgsConstructor(access = PROTECTED)
public abstract class BaseFullTimeEntity {
```

### 3. 생성자 패턴
```java
// 필수 필드만 받는 생성자
public Account(String loginId, String password, Role role) {
    this.id = UUID.randomUUID().toString();  // UUID 생성
    this.loginId = loginId;
    this.password = password;
    this.role = role;
    this.lastLoginAt = LocalDateTime.now();  // 기본값 설정
}

// 용도별 생성자 분리 (OAuth 로그인용)
public Account(Role role, OAuthProvider provider, String providerId) {
    this.id = UUID.randomUUID().toString();
    this.loginId = provider.name() + "_" + providerId;
    this.password = "";
    this.role = role;
    this.provider = provider;
    this.providerId = providerId;
}
```

---

## update 메서드 패턴

### 1. 단일 필드 업데이트
```java
public void updateLastLoginAt() {
    this.lastLoginAt = LocalDateTime.now();
}

public void updatePassword(String password) {
    if (StringUtils.isBlank(password)) {
        return;  // null/빈값 방어
    }
    this.password = password;
}
```

### 2. 연관관계 업데이트
```java
public void updateUser(User user) {
    this.user = user;
}

public void updateHospital(Hospital hospital) {
    this.hospital = hospital;
}
```

### 3. 다중 필드 업데이트 (선택적)
```java
public void update(String name, LocalDate birthDate, Gender gender,
                   DiabetesType diabetesType, BigDecimal height, BigDecimal weight) {
    if (name != null) this.name = name;
    if (birthDate != null) this.birthDate = birthDate;
    if (gender != null) this.gender = gender;
    if (diabetesType != null) this.diabetesType = diabetesType;
    if (height != null) this.height = height;
    if (weight != null) this.weight = weight;
}
```

---

## 연관관계 매핑

### 1. @OneToOne
```java
@OneToOne(mappedBy = "account")
private User user;
```

### 2. @ManyToOne
```java
@ManyToOne(fetch = FetchType.LAZY)  // LAZY 기본 사용
@JoinColumn(name = "user_id")
private User user;
```

### 3. @OneToMany
```java
@OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true)
private List<HospitalImage> imageList = new ArrayList<>();

// 편의 메서드
public void addImage(HospitalImage image) {
    this.imageList.add(image);
}

public void clearImageList() {
    this.imageList.clear();
}
```

---

## Enum 매핑

```java
@Enumerated(EnumType.STRING)  // STRING 타입 사용 (ORDINAL 금지)
@Column(name = "role", nullable = false)
private Role role;

@Enumerated(EnumType.STRING)
@Column(name = "provider")
private OAuthProvider provider;
```

---

## 예제: Account Entity

```java
@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor
public class Account extends BaseFullTimeEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "login_id", unique = true, nullable = false)
    private String loginId;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private OAuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    @OneToOne(mappedBy = "account")
    private User user;

    @OneToOne(mappedBy = "account")
    private Hospital hospital;

    // 일반 로그인용 생성자
    public Account(String loginId, String password, Role role) {
        this.id = UUID.randomUUID().toString();
        this.loginId = loginId;
        this.password = password;
        this.role = role;
        this.lastLoginAt = LocalDateTime.now();
    }

    // OAuth 로그인용 생성자
    public Account(Role role, OAuthProvider provider, String providerId) {
        this.id = UUID.randomUUID().toString();
        this.loginId = provider.name() + "_" + providerId;
        this.password = "";
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updatePassword(String password) {
        if (StringUtils.isBlank(password)) {
            return;
        }
        this.password = password;
    }

    public void updateLoginId(String loginId) {
        if (StringUtils.isBlank(loginId)) {
            return;
        }
        this.loginId = loginId;
    }

    public void updateUser(User user) {
        this.user = user;
    }

    public void updateHospital(Hospital hospital) {
        this.hospital = hospital;
    }
}
```
