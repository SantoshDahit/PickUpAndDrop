# 15. AOP 동적 응답값 Enrichment 컨벤션

## 개요

엔티티에 존재하지 않는 응답값(비영속 필드)을 **AOP `@AfterReturning`으로 Service 반환 시점에 자동 주입**하는 패턴입니다.
DB에 저장하지 않지만 API 응답에 포함해야 하는 동적 데이터(추천 메시지, 계산된 등급, 외부 연동 링크 등)를 이 패턴으로 처리합니다.

---

## 언제 사용하는가

| 상황 | 예시 |
|------|------|
| 엔티티 필드 조합으로 계산되는 값 | 점수 → 등급, 금액 → 할인율 |
| 외부 데이터를 주입해야 할 때 | 추천 메시지, 관련 링크, 배지 |
| 조건 기반 동적 텍스트 | 상태에 따른 안내 메시지 |
| 모든 조회 API에 일관되게 적용 | Service를 거치면 자동 주입 |

> **핵심**: Controller나 Facade에서 수동으로 주입하지 않고, AOP가 Service 반환값을 자동으로 가로채어 처리합니다.

---

## 아키텍처

```
Service 메서드 반환
  ↓ @AfterReturning (자동)
Aspect → 반환값에서 대상 엔티티 재귀 추출
  ↓
Enricher → 엔티티에 동적 값 주입
  ↓
엔티티.setDynamicField(computedValue)
  ↓
Controller → 이미 주입된 상태로 응답
```

### 구성 요소 (3개 세트)

```
aop/
├── {Entity}Aspect.java          # Service 반환값 가로채기 + 재귀 추출
├── {Entity}Enricher.java        # 동적 값 계산 + 주입
domain/
└── {Domain}DataProvider.java    # 조건별 데이터 관리 (메시지, 링크 등)
```

---

## 1단계: 엔티티에 비영속 필드 추가

```java
@Entity
@Table(name = "order")
public class Order extends BaseEntity {

    @Id
    private String id;

    @Column(name = "amount")
    private int amount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // ─── DB에 없는 동적 응답값 ───
    @Transient
    private String recommendMessage;

    @Transient
    private String grade;

    public void setRecommendMessage(String message) {
        this.recommendMessage = message;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }
}
```

- `@Transient`: JPA가 무시하므로 DB 컬럼 없음
- `setXxx()` 메서드만 제공하면 AOP에서 주입 가능

---

## 2단계: Aspect 클래스 (재귀 추출)

```java
@Aspect
@Component
@RequiredArgsConstructor
public class OrderEnrichAspect {

    private final OrderEnricher enricher;

    @AfterReturning(
        pointcut = "execution(* com.example..service..*(..))",
        returning = "result"
    )
    public void enrich(Object result) {
        Set<Order> orders = extract(result, new HashSet<>(), new IdentityHashMap<>());
        enricher.bulkEnrich(orders);
    }

    private Set<Order> extract(Object source, Set<Order> collected,
                               Map<Object, Boolean> visited) {
        if (source == null || visited.containsKey(source)) return collected;
        visited.put(source, true);

        // 1. 대상 엔티티 발견
        if (source instanceof Order order) {
            collected.add(order);
            return collected;
        }

        // 2. Collection 처리
        if (source instanceof Collection<?> col) {
            for (Object item : col) extract(item, collected, visited);
            return collected;
        }

        // 3. Page 처리
        if (source instanceof Page<?> page) {
            extract(page.getContent(), collected, visited);
            return collected;
        }

        // 4. DTO 등 커스텀 객체 → Reflection으로 필드 탐색
        if (!isJavaInternal(source.getClass())) {
            for (Field field : source.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(source);
                    if (value != null && !Hibernate.isInitialized(value)) {
                        Hibernate.initialize(value);
                    }
                    extract(value, collected, visited);
                } catch (IllegalAccessException ignored) {}
            }
        }

        return collected;
    }

    private boolean isJavaInternal(Class<?> clazz) {
        if (clazz.isPrimitive()) return true;
        Package pkg = clazz.getPackage();
        return pkg != null && pkg.getName().startsWith("java");
    }
}
```

### Pointcut 범위

```java
execution(* com.example..service..*(..))
```

`service` 패키지 하위의 **모든 메서드 반환값**을 대상으로 합니다.
단건 조회, 목록 조회, Page 조회 모두 하나의 Aspect로 처리됩니다.

### 재귀 추출이 필요한 이유

Service 반환값은 다양한 형태로 올 수 있습니다:

| 반환 형태 | 처리 |
|-----------|------|
| `Order` (단건) | `instanceof`로 직접 수집 |
| `List<Order>` | `Collection` → 각 요소 재귀 |
| `Page<Order>` | `getContent()` → Collection 재귀 |
| `OrderDto` (DTO 내 Order 필드) | Reflection으로 필드 탐색 → 재귀 |
| `List<OrderDto>` | Collection → DTO → Reflection → 재귀 |

### 순환 참조 방지

```java
// IdentityHashMap: == 비교 (equals/hashCode 무관)
Map<Object, Boolean> visited = new IdentityHashMap<>();
```

엔티티 간 양방향 연관관계가 있어도 `IdentityHashMap`이 동일 객체 재방문을 방지합니다.

### Hibernate 프록시 처리

```java
if (value != null && !Hibernate.isInitialized(value)) {
    Hibernate.initialize(value);
}
```

Lazy 로딩 프록시는 초기화하지 않으면 실제 타입을 알 수 없으므로, Reflection 탐색 전 강제 초기화합니다.

---

## 3단계: Enricher 클래스 (동적 값 주입)

```java
@Component
@RequiredArgsConstructor
public class OrderEnricher {

    private final OrderGradeProvider gradeProvider;

    public void bulkEnrich(Collection<Order> orders) {
        if (orders == null || orders.isEmpty()) return;

        orders.forEach(order -> {
            // 동적 값 계산 + 주입
            order.setGrade(gradeProvider.getGrade(order.getAmount()));
            order.setRecommendMessage(gradeProvider.getMessage(order.getAmount()));
        });
    }
}
```

- Enricher는 **어떤 동적 값을 주입할지** 결정하는 역할
- 실제 값 계산은 별도 Provider/도메인 클래스에 위임

---

## 4단계: 데이터 Provider (조건별 값 관리)

```java
@Component
public class OrderGradeProvider {

    private final Map<String, List<String>> messages = new HashMap<>();

    public OrderGradeProvider() {
        messages.put("VIP", List.of("VIP 고객님, 특별 할인이 준비되어 있습니다!", "VIP 혜택을 확인해보세요."));
        messages.put("NORMAL", List.of("주문이 처리되었습니다."));
    }

    public String getGrade(int amount) {
        if (amount >= 100_000) return "VIP";
        if (amount >= 50_000) return "GOLD";
        return "NORMAL";
    }

    public String getMessage(int amount) {
        String grade = getGrade(amount);
        List<String> list = messages.getOrDefault(grade, List.of());
        if (list.isEmpty()) return null;
        return list.get(RandomUtils.randomInt(0, list.size() - 1));
    }
}
```

- 조건별로 복수 메시지를 등록하여 랜덤 선택 가능
- 링크, 배지, 추천 데이터 등 어떤 형태든 동일 패턴 적용

---

## 새 동적 응답값 추가 체크리스트

### 1. 엔티티에 `@Transient` 필드 추가

```java
@Transient
private String dynamicField;

public void setDynamicField(String value) {
    this.dynamicField = value;
}
```

### 2. Aspect 생성 (또는 기존 Aspect에 추가)

```
aop/{Entity}EnrichAspect.java
```
- `@AfterReturning` + 재귀 추출 메서드
- 이미 해당 엔티티의 Aspect가 있으면 Enricher에만 로직 추가

### 3. Enricher 생성

```
aop/{Entity}Enricher.java
```
- `bulkEnrich(Collection<T>)` 메서드

### 4. Provider 생성 (필요 시)

```
domain/{Domain}DataProvider.java
```
- 조건별 데이터 관리

---

## 주의사항

| 항목 | 설명 |
|------|------|
| **Aspect 수** | 엔티티당 1개의 Aspect. 동일 엔티티에 여러 동적 값이 필요하면 Enricher에서 한 번에 처리 |
| **성능** | 모든 Service 메서드에 적용되므로 `extract()` 내 `isJavaInternal()` 체크로 불필요한 탐색 차단 |
| **트랜잭션** | `@AfterReturning` 시점은 트랜잭션 내이므로 Hibernate 프록시 초기화 가능 |
| **null 안전** | Enricher에서 `null`/`isEmpty` 체크 필수 |
