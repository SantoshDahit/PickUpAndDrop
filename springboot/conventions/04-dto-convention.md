# DTO 컨벤션

## Inner Class 패턴

모든 DTO는 **도메인명 + Dto** 클래스 안에 Inner Class로 정의합니다.

```java
public class AccountDto {

    // 생성 요청
    public record PostRequest() {}

    // 수정 요청
    public record PatchRequest() {}

    // 상세 응답
    @Getter
    public static class Response {}

    // 목록 응답 (요약)
    @Getter
    @NoArgsConstructor
    public static class SummaryResponse {}

    // 검색 조건
    public record SearchRequest() {}
}
```

---

## DTO 종류별 규칙

### 1. PostRequest (생성 요청)
- **Java Record** 사용
- `@NotBlank`, `@NotNull` 등 Validation 어노테이션 적용
- 생성에 필요한 필수 필드만 포함

```java
public record PostRequest(
    @NotBlank String smsVerificationId,
    String accountId,  // Optional
    @NotNull String name,
    @NotNull LocalDate birthDate,
    @NotNull Gender gender,
    @NotBlank String contact,
    @NotNull DiabetesType diabetesType
) {}
```

---

### 2. PatchRequest (수정 요청)
- **Java Record** 사용
- 수정 가능한 필드만 포함
- `null` 허용 (null이면 수정하지 않음)

```java
public record PatchRequest(
    LocalDate birthDate,
    Gender gender,
    DiabetesType diabetesType,
    BigDecimal height,
    BigDecimal weight,
    String name
) {}
```

---

### 3. Response (상세 응답)
- **클래스 + @Getter** 사용
- `@NoArgsConstructor` 선택적 (ModelMapper 사용 시 필요)
- Entity의 모든 필드 포함 (연관 Entity는 해당 DTO로)

```java
@Getter
public static class Response {
    private String id;
    private String loginId;
    private Role role;
    private LocalDateTime lastLoginAt;
    private OAuthProvider provider;
    private String providerId;
    private UserDto.Response user;          // 연관 Entity → 해당 DTO
    private HospitalDto.Response hospital;  // 연관 Entity → 해당 DTO
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
```

---

### 4. SummaryResponse (목록 응답)
- **클래스 + @Getter + @NoArgsConstructor** 사용
- 목록 조회 시 필요한 핵심 필드만 포함
- 연관 Entity는 ID만 포함하거나 최소 정보만 포함

```java
@Getter
@NoArgsConstructor
public static class SummaryResponse {
    private String id;
    private String loginId;
    private Role role;
    private String userId;      // 연관 Entity ID만
    private String hospitalId;  // 연관 Entity ID만

    // Entity로부터 직접 생성하는 생성자 (선택적)
    public SummaryResponse(Account account) {
        this.id = account.getId();
        this.loginId = account.getLoginId();
        this.role = account.getRole();
        this.userId = account.getUser() != null ? account.getUser().getId() : null;
        this.hospitalId = account.getHospital() != null ? account.getHospital().getId() : null;
    }
}
```

---

### 5. SearchRequest (검색 조건)
- **Java Record** 사용
- 검색에 필요한 필드만 포함
- 범위 검색은 `min`, `max` 접두사 사용
- 목록 검색은 `List` 사용

```java
public record SearchRequest(
    String loginId,                    // 단일 조건
    List<Role> roleList,               // 목록 조건 (IN 절)
    LocalDateTime minCreatedAt,        // 범위 조건 (시작)
    LocalDateTime maxCreatedAt         // 범위 조건 (끝)
) {}
```

---

## Validation 어노테이션

### 주요 어노테이션

| 어노테이션 | 용도 | 예시 |
|-----------|------|------|
| `@NotBlank` | 문자열 필수 (공백 불가) | 로그인 ID, 비밀번호 |
| `@NotNull` | null 불가 | Enum, 숫자, 날짜 |
| `@NotEmpty` | 컬렉션 필수 | 리스트 |
| `@Valid` | 중첩 객체 검증 | 내부 DTO |
| `@Size` | 길이 제한 | `@Size(min=2, max=100)` |
| `@Email` | 이메일 형식 | 이메일 필드 |

### 사용 예시
```java
public record PostRequest(
    @NotBlank String smsVerificationId,  // 필수 문자열
    @NotNull LocalDate birthDate,        // 필수 날짜
    @NotNull @Valid UserDto.PostRequest user  // 필수 + 중첩 검증
) {}
```

---

## Record vs Class 선택 기준

| 유형 | 선택 | 이유 |
|------|------|------|
| Request DTO | `record` | 불변성, 간결함 |
| Response DTO | `class` | ModelMapper 호환, 유연성 |
| SearchRequest | `record` | 불변성, 간결함 |
| 복잡한 Response | `class` | 생성자 로직 필요 |

---

## 필드 순서 규칙

### Request DTO
1. 인증/검증 관련 (smsVerificationId)
2. 식별자 (accountId)
3. 필수 필드
4. 선택적 필드

### Response DTO
1. `id`
2. 핵심 비즈니스 필드
3. 연관 Entity DTO
4. 시간 필드 (`createdAt`, `updatedAt`, `deletedAt`)

```java
@Getter
public static class Response {
    // 1. 식별자
    private String id;

    // 2. 핵심 비즈니스 필드
    private String name;
    private LocalDate birthDate;
    private Gender gender;
    private String contact;
    private DiabetesType diabetesType;
    private BigDecimal height;
    private BigDecimal weight;
    private String code;

    // 3. 연관 Entity (해당되는 경우)
    private List<HospitalDto.SummaryResponse> hospitalList;

    // 4. 시간 필드
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 특수 목적 DTO

### 비밀번호 변경 전용
```java
public record PasswordPatchRequest(
    @NotBlank String password
) {}
```

### 복합 요청 (계정 + 병원 정보)
```java
public record HospitalPatchRequest(
    String loginId,
    String password,
    HospitalDto.PatchRequest hospital  // 중첩 DTO
) {}
```

### 통계/집계 응답
```java
@Getter
@NoArgsConstructor
public static class UserCountResponse {
    private Long totalCount;
    private Long newCount;

    public UserCountResponse(Long totalCount, Long newCount) {
        this.totalCount = totalCount;
        this.newCount = newCount;
    }
}
```

---

## DTO 파일 전체 예시

```java
package com.example.dto;

import com.example.enums.DiabetesType;
import com.example.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserDto {

    // 생성 요청
    public record PostRequest(
        @NotBlank String smsVerificationId,
        String accountId,
        @NotNull String name,
        @NotNull LocalDate birthDate,
        @NotNull Gender gender,
        @NotBlank String contact,
        @NotNull DiabetesType diabetesType
    ) {}

    // 수정 요청
    public record PatchRequest(
        LocalDate birthDate,
        Gender gender,
        DiabetesType diabetesType,
        BigDecimal height,
        BigDecimal weight,
        String name
    ) {}

    // 상세 응답
    @Getter
    @NoArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private LocalDate birthDate;
        private Gender gender;
        private String contact;
        private DiabetesType diabetesType;
        private BigDecimal height;
        private BigDecimal weight;
        private String code;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // 목록 응답
    @Getter
    @NoArgsConstructor
    public static class SummaryResponse {
        private String id;
        private String name;
        private LocalDate birthDate;
        private Gender gender;
        private LocalDateTime createdAt;
    }

    // 검색 조건
    public record SearchRequest(
        LocalDateTime minCreatedAt,
        LocalDateTime maxCreatedAt
    ) {}
}
```

---

## Mapper와의 연동

DTO 변환은 Mapper에서 처리합니다.

```java
@Component
public class UserMapper extends BaseMapper<User, UserDto> {

    protected UserMapper(ModelMapper modelMapper) {
        super(modelMapper, User.class);
        this.registerDtoMapping(UserDto.class);
        this.registerDtoMapping(UserDto.Response.class);
        this.registerDtoMapping(UserDto.SummaryResponse.class);
    }

    public UserDto.Response toResponse(User entity) {
        return super.toDto(entity, UserDto.Response.class);
    }

    public UserDto.SummaryResponse toSummaryResponse(User entity) {
        return super.toDto(entity, UserDto.SummaryResponse.class);
    }
}
```
