# Mapper 컨벤션

## 개요

Entity와 DTO 간 변환을 담당하는 Mapper 클래스입니다.
프로젝트에 포함된 `BaseMapper` 추상 클래스를 상속하며, 내부적으로 `ModelMapper`를 사용합니다.

---

## 의존성

### ModelMapper
```groovy
implementation 'org.modelmapper:modelmapper:3.1.0'
```

> **참고**: `BaseMapper`는 프로젝트의 `common/` 패키지에 직접 포함합니다.
> 외부 라이브러리 의존 없이 사용할 수 있습니다.

---

## ModelMapper 설정

`ModelMapperConfig`에서 Bean으로 등록합니다.

```java
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STANDARD);

        return modelMapper;
    }
}
```

### 설정 상세

| 설정 | 값 | 설명 |
|------|------|------|
| `fieldMatchingEnabled` | `true` | getter/setter 없이 필드 직접 매핑 |
| `fieldAccessLevel` | `PRIVATE` | private 필드도 접근 가능 |
| `matchingStrategy` | `STANDARD` | 이름 기반 표준 매칭 |

> **참고**: `STANDARD` 전략은 필드명이 동일하면 자동 매핑됩니다. Entity와 Response DTO의 필드명을 동일하게 맞추면 별도 매핑 설정 없이 자동 변환됩니다.

---

## BaseMapper 구현

프로젝트의 `common/` 패키지에 아래 클래스를 포함합니다.
모든 도메인 Mapper의 부모 클래스입니다.

```java
package com.example.common;

import org.modelmapper.ModelMapper;

/**
 * Entity ↔ DTO 변환을 위한 기본 Mapper 추상 클래스.
 *
 * @param <E> Entity 타입
 * @param <D> DTO 타입
 */
public abstract class BaseMapper<E, D> {

    protected final ModelMapper modelMapper;
    private final Class<E> entityClass;

    protected BaseMapper(ModelMapper modelMapper, Class<E> entityClass) {
        this.modelMapper = modelMapper;
        this.entityClass = entityClass;
    }

    /**
     * DTO 클래스를 ModelMapper에 TypeMap으로 등록합니다.
     * 생성자에서 사용할 DTO 클래스마다 호출해야 합니다.
     */
    protected <T> void registerDtoMapping(Class<T> dtoClass) {
        if (modelMapper.getTypeMap(entityClass, dtoClass) == null) {
            modelMapper.createTypeMap(entityClass, dtoClass);
        }
    }

    /**
     * Entity를 지정 DTO 클래스로 변환합니다.
     */
    protected <T> T toDto(E entity, Class<T> dtoClass) {
        return modelMapper.map(entity, dtoClass);
    }
}
```

### BaseMapper 핵심 구조

| 요소 | 설명 |
|------|------|
| `modelMapper` | ModelMapper 인스턴스 (Spring Bean 주입) |
| `entityClass` | Entity 클래스 (TypeMap 등록용) |
| `registerDtoMapping()` | Entity → DTO TypeMap 사전 등록 |
| `toDto()` | Entity를 지정 DTO 클래스로 변환 |

> **중요**: `registerDtoMapping()`은 `modelMapper.createTypeMap()`을 호출하여 변환 경로를 사전 등록합니다. 이를 통해 매핑 오류를 애플리케이션 시작 시점에 조기 감지할 수 있습니다.

---

## BaseMapper 상속 패턴

### 기본 Mapper (대다수)

프로젝트의 대부분의 Mapper는 아래 패턴을 따릅니다.

```java
package com.example.mapper;

import com.example.common.BaseMapper;
import com.example.dto.AccountDto;
import com.example.entity.Account;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper extends BaseMapper<Account, AccountDto> {

    protected AccountMapper(ModelMapper modelMapper) {
        super(modelMapper, Account.class);
        this.registerDtoMapping(AccountDto.class);
        this.registerDtoMapping(AccountDto.Response.class);
        this.registerDtoMapping(AccountDto.SummaryResponse.class);
    }

    public AccountDto toDto(Account entity) {
        return super.toDto(entity, AccountDto.class);
    }

    public AccountDto.Response toResponse(Account entity) {
        return super.toDto(entity, AccountDto.Response.class);
    }

    public AccountDto.SummaryResponse toSummaryResponse(Account entity) {
        return super.toDto(entity, AccountDto.SummaryResponse.class);
    }
}
```

### 구조 설명

| 요소 | 설명 |
|------|------|
| `@Component` | Spring Bean 등록 |
| `extends BaseMapper<Entity, Dto>` | 제네릭으로 Entity, Dto 타입 지정 |
| `super(modelMapper, Entity.class)` | 부모 클래스에 ModelMapper와 Entity 클래스 전달 |
| `registerDtoMapping()` | DTO 클래스를 ModelMapper에 TypeMap으로 등록 |
| `toDto()` | Entity → 지정 DTO 클래스로 변환 |

### 생성자 패턴

```java
protected {Domain}Mapper(ModelMapper modelMapper) {
    super(modelMapper, {Domain}.class);
    // 사용할 DTO 클래스를 모두 등록
    this.registerDtoMapping({Domain}Dto.class);
    this.registerDtoMapping({Domain}Dto.Response.class);
    this.registerDtoMapping({Domain}Dto.SummaryResponse.class);
}
```

> **중요**: `registerDtoMapping()`을 호출하지 않으면 TypeMap이 등록되지 않아 매핑 오류를 조기에 감지할 수 없습니다.

### 변환 메서드 패턴

```java
// Entity → 전체 DTO
public {Domain}Dto toDto({Domain} entity) {
    return super.toDto(entity, {Domain}Dto.class);
}

// Entity → 상세 응답 DTO
public {Domain}Dto.Response toResponse({Domain} entity) {
    return super.toDto(entity, {Domain}Dto.Response.class);
}

// Entity → 목록 응답 DTO
public {Domain}Dto.SummaryResponse toSummaryResponse({Domain} entity) {
    return super.toDto(entity, {Domain}Dto.SummaryResponse.class);
}
```

---

## 중첩 Entity 자동 매핑

ModelMapper는 등록된 typeMap을 기반으로 중첩 Entity도 자동 매핑합니다.
**Mapper에서 다른 Mapper를 주입하지 않습니다.** 기본 패턴만으로 처리합니다.

### 자동 매핑 원리

```
Account (Entity)
  └── owner: Owner (Entity)

AccountDto.Response (DTO)
  └── owner: OwnerDto.Response (DTO)

→ OwnerMapper가 Owner → OwnerDto.Response typeMap을 등록했으므로
  AccountMapper에서 별도 처리 없이 자동 매핑됨
```

### 예시: AccountMapper (중첩 Owner/Agent/Assistant 자동 매핑)

```java
@Component
public class AccountMapper extends BaseMapper<Account, AccountDto> {

    protected AccountMapper(ModelMapper modelMapper) {
        super(modelMapper, Account.class);
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

> **핵심**: 중첩 Entity의 Mapper가 typeMap을 등록하면, 부모 Entity의 Mapper에서 별도 설정 없이 자동 변환됩니다.

### DTO에서 @Setter 불필요

ModelMapper 설정 (`fieldMatchingEnabled=true`, `fieldAccessLevel=PRIVATE`)으로 setter 없이 필드에 직접 접근하여 매핑합니다.
DTO의 중첩 객체 필드에 `@Setter`를 붙일 필요가 없습니다.

```java
// ✅ 권장: @Setter 없이 필드만 선언
@Getter
@NoArgsConstructor
public static class Response {
    private String id;
    private OwnerDto.Response owner;           // @Setter 불필요
    private RealEstateAgencyDto.SummaryResponse realEstateAgency;  // @Setter 불필요
}
```

### 엔티티에 없는 필드 skip

DTO에 엔티티에 존재하지 않는 필드가 있는 경우에만 skip을 사용합니다.
이는 ModelMapper의 오매핑을 방지하기 위한 것입니다.

```java
// 엔티티에 없는 필드만 skip (다른 곳에서 enrichment)
modelMapper.typeMap(RealEstateAgency.class, RealEstateAgencyDto.DetailResponse.class)
        .addMappings(mapper -> {
            mapper.skip(RealEstateAgencyDto.DetailResponse::setProfileImageUrl);
            mapper.skip(RealEstateAgencyDto.DetailResponse::setStaffList);
        });
```

---

## Facade에서 Mapper 사용

### 단건 변환

```java
@Transactional(readOnly = true)
public AccountDto.Response getById(String id) {
    Account account = accountService.getById(id);
    return accountMapper.toResponse(account);
}
```

### Page 변환 (메서드 참조)

```java
@Transactional(readOnly = true)
public Page<AccountDto.SummaryResponse> search(AccountDto.SearchRequest searchRequest, Pageable pageable) {
    return accountService.search(searchRequest, pageable)
            .map(accountMapper::toSummaryResponse);  // 메서드 참조 활용
}
```

> **핵심**: `Page.map()`과 메서드 참조(`mapper::toSummaryResponse`)를 조합하면 `Page<Entity>` → `Page<DTO>` 변환을 간결하게 처리할 수 있습니다.

### 생성 후 변환

```java
@Transactional
public AccountDto.Response create(AccountDto.PostRequest request) {
    Account account = new Account(request.loginId(), request.password(), Role.USER);
    Account savedAccount = accountService.save(account);
    return accountMapper.toResponse(savedAccount);
}
```

---

## Mapper 작성 가이드

### 1. 기본 Mapper (연관 Entity 없는 경우)

```
1. BaseMapper<Entity, Dto> 상속
2. 생성자에서 registerDtoMapping() 호출
3. toDto(), toResponse(), toSummaryResponse() 메서드 정의
```

### 2. 새 도메인 추가 시 체크리스트

- [ ] `{Domain}Mapper` 클래스 생성
- [ ] `@Component` 어노테이션 추가
- [ ] `BaseMapper<{Domain}, {Domain}Dto>` 상속
- [ ] 생성자에서 사용할 DTO 클래스 모두 `registerDtoMapping()` 호출
- [ ] 중첩 Entity는 자동 매핑에 맡김 (다른 Mapper 주입 금지)
- [ ] 엔티티에 없는 DTO 필드가 있으면 `typeMap().addMappings(mapper -> mapper.skip(...))` 설정
- [ ] Facade에서 Mapper 주입 확인

---

## 새 프로젝트 적용 가이드

새 프로젝트에서 이 Mapper 패턴을 적용하려면:

1. **ModelMapper 의존성 추가**
   ```groovy
   implementation 'org.modelmapper:modelmapper:3.1.0'
   ```

2. **BaseMapper 클래스 복사**
   - `common/BaseMapper.java`를 프로젝트에 복사 (위 "BaseMapper 구현" 섹션 참조)

3. **ModelMapperConfig 설정**
   - `config/ModelMapperConfig.java` 생성 (위 "ModelMapper 설정" 섹션 참조)

4. **도메인별 Mapper 작성**
   - `templates/MapperTemplate.java` 참조

---

## 참조 파일

| 파일 | 설명 |
|------|------|
| `common/BaseMapper.java` | BaseMapper 추상 클래스 (프로젝트 내 포함) |
| `mapper/AccountMapper.java` | 기본 Mapper 패턴 (중첩 Entity 자동 매핑) |
| `mapper/RealEstateAgencyMapper.java` | 엔티티에 없는 필드 skip 패턴 |
| `config/ModelMapperConfig.java` | ModelMapper Bean 설정 |
