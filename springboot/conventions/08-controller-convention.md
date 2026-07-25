# Controller 컨벤션

## 기본 구조

```java
@RestController
@RequestMapping("/v1/accounts")  // 버전 + 리소스 복수형
@RequiredArgsConstructor
public class AccountController {

    private final AccountFacade accountFacade;  // Facade만 의존

    /**
     * 계정 리스트 조회 (검색) #9
     */
    @GetMapping("/search")
    public Page<AccountDto.SummaryResponse> search(
            @ModelAttribute AccountDto.SearchRequest searchRequest,
            Pageable pageable) {
        return accountFacade.search(searchRequest, pageable);
    }

    /**
     * 계정 조회 #7
     */
    @GetMapping("/{accountId}")
    public AccountDto.Response getById(@PathVariable String accountId) {
        return accountFacade.getById(accountId);
    }
}
```

---

## URL 설계 규칙

### 1. 기본 패턴
```
/v{version}/{resources}
/v{version}/{resources}/{id}
/v{version}/{resources}/{id}/{sub-resources}
```

### 2. 규칙

| 규칙 | 예시 |
|------|------|
| 버전 접두사 | `/v1/`, `/v2/` |
| 리소스 복수형 | `/accounts`, `/users` |
| kebab-case | `/user-hospitals` (camelCase X) |
| 동사 사용 금지 | `/accounts` (O), `/getAccounts` (X) |
| 계층 관계 | `/accounts/{id}/users` |

### 3. URL 예시
```
GET     /v1/accounts              # 목록 조회
GET     /v1/accounts/search       # 검색 (쿼리 파라미터)
GET     /v1/accounts/{id}         # 단건 조회
POST    /v1/accounts              # 생성
PATCH   /v1/accounts/{id}         # 수정
DELETE  /v1/accounts/{id}         # 삭제

GET     /v1/accounts/me           # 현재 사용자 정보
PATCH   /v1/accounts/{id}/password  # 비밀번호 변경
```

---

## HTTP 메서드 매핑

| 메서드 | 용도 | 어노테이션 | 응답 코드 |
|--------|------|-----------|----------|
| GET | 조회 | `@GetMapping` | 200 OK |
| POST | 생성 | `@PostMapping` | 200 OK / 201 Created |
| PATCH | 부분 수정 | `@PatchMapping` | 200 OK |
| PUT | 전체 수정 | `@PutMapping` | 200 OK |
| DELETE | 삭제 | `@DeleteMapping` | 204 No Content |

### 삭제 응답 예시
```java
@DeleteMapping("/{accountId}")
@ResponseStatus(HttpStatus.NO_CONTENT)  // 204 반환
public void delete(@PathVariable String accountId) {
    accountFacade.delete(accountId);
}
```

---

## 파라미터 바인딩

### 1. @PathVariable
URL 경로의 변수를 바인딩합니다.

```java
@GetMapping("/{accountId}")
public AccountDto.Response getById(@PathVariable String accountId) {
    return accountFacade.getById(accountId);
}

// 여러 PathVariable
@GetMapping("/{accountId}/users/{userId}")
public UserDto.Response getUser(
        @PathVariable String accountId,
        @PathVariable String userId) {
    return accountFacade.getUser(accountId, userId);
}
```

### 2. @RequestBody
JSON Body를 DTO로 바인딩합니다.

```java
@PostMapping
public AccountDto.Response create(@RequestBody @Valid AccountDto.PostRequest request) {
    return accountFacade.create(request);
}

@PatchMapping("/{accountId}")
public AccountDto.Response update(
        @PathVariable String accountId,
        @RequestBody @Valid AccountDto.PatchRequest request) {
    return accountFacade.update(accountId, request);
}
```

### 3. @ModelAttribute
쿼리 파라미터를 DTO로 바인딩합니다 (GET 검색에 사용).

```java
@GetMapping("/search")
public Page<AccountDto.SummaryResponse> search(
        @ModelAttribute AccountDto.SearchRequest searchRequest,
        Pageable pageable) {
    return accountFacade.search(searchRequest, pageable);
}
```

### 4. @Valid
DTO의 Validation을 수행합니다.

```java
@PostMapping
public AccountDto.Response create(@RequestBody @Valid AccountDto.PostRequest request) {
    // @NotBlank, @NotNull 등 검증 후 실행
}
```

---

## Pageable 파라미터

Spring Data의 `Pageable`을 사용하여 페이징을 처리합니다.

### 요청 예시
```
GET /v1/accounts/search?page=0&size=20&sort=createdAt,desc
```

### 파라미터

| 파라미터 | 설명 | 기본값 |
|----------|------|--------|
| `page` | 페이지 번호 (0부터 시작) | 0 |
| `size` | 페이지 크기 | 20 |
| `sort` | 정렬 (필드명,방향) | - |

### Controller 예시
```java
@GetMapping("/search")
public Page<AccountDto.SummaryResponse> search(
        @ModelAttribute AccountDto.SearchRequest searchRequest,
        Pageable pageable) {  // 자동 바인딩
    return accountFacade.search(searchRequest, pageable);
}
```

---

## API 문서화 주석

### 형식
```java
/**
 * {기능 설명} #{Notion API 명세서 ID}
 */
```

- `#` 뒤의 번호는 **Notion API 명세서**의 자동 부여 ID를 사용한다.
- API를 추가할 때 반드시 Notion API 명세서에 먼저 등록하고, 부여된 ID를 Controller 주석에 기입한다.
- Notion 등록 시 **분류** 필드를 반드시 설정한다. (예: 인증, 개공, 소공, 보조원, 중개업소, SMS 인증, 파일, 매물 찾기)

### 예시
```java
/**
 * 계정 리스트 조회 (검색) #9
 */
@GetMapping("/search")
public Page<AccountDto.SummaryResponse> search(...) {}

/**
 * 계정 조회 #7
 */
@GetMapping("/{accountId}")
public AccountDto.Response getById(...) {}

/**
 * 비밀 번호 변경 #27
 */
@PatchMapping("/{accountId}/password")
public AccountDto.Response changePassword(...) {}

/**
 * 회원 탈퇴 #23
 */
@DeleteMapping("/{accountId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(...) {}
```

---

## 전체 Controller 예시

```java
package com.example.controller;

import com.example.dto.AccountDto;
import com.example.service.AccountFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountFacade accountFacade;

    /**
     * 계정 리스트 조회 (검색) #9
     */
    @GetMapping("/search")
    public Page<AccountDto.SummaryResponse> search(
            @ModelAttribute AccountDto.SearchRequest searchRequest,
            Pageable pageable) {
        return accountFacade.search(searchRequest, pageable);
    }

    /**
     * 계정 조회 #7
     */
    @GetMapping("/{accountId}")
    public AccountDto.Response getById(@PathVariable String accountId) {
        return accountFacade.getById(accountId);
    }

    /**
     * 내 정보 조회 #10
     */
    @GetMapping("/me")
    public AccountDto.Response getMyInfo() {
        return accountFacade.getMyInfo();
    }

    /**
     * 비밀 번호 변경 #27
     */
    @PatchMapping("/{accountId}/password")
    public AccountDto.Response changePassword(
            @PathVariable String accountId,
            @RequestBody @Valid AccountDto.PasswordPatchRequest passwordPatchRequest) {
        return accountFacade.updatePassword(accountId, passwordPatchRequest);
    }

    /**
     * 계정 수정 (병원) #12
     */
    @PatchMapping("/{accountId}/hospital")
    public AccountDto.Response updateHospital(
            @PathVariable String accountId,
            @Valid @RequestBody AccountDto.HospitalPatchRequest request) {
        return accountFacade.updateHospital(accountId, request);
    }

    /**
     * 회원 탈퇴 #23
     */
    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String accountId) {
        accountFacade.delete(accountId);
    }

    /**
     * 계정 전화번호 조회 (일반) #171
     */
    @GetMapping("/users")
    public AccountDto.Response getUsers(
            @Valid @ModelAttribute AccountDto.UserRequest userRequest) {
        return accountFacade.getByUserContact(userRequest);
    }

    /**
     * 비밀번호 초기화 #24
     */
    @PatchMapping("/password/reset")
    public AccountDto.Response passwordReset(
            @RequestBody @Valid AccountDto.ResetPasswordRequest request) {
        return accountFacade.passwordReset(request);
    }
}
```

---

## 응답 형식

### 단건 조회
```json
{
  "id": "uuid-string",
  "loginId": "user@example.com",
  "role": "USER",
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-02T00:00:00"
}
```

### 목록 조회 (Page)

> **필수 설정**: Application 클래스에 `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` 어노테이션이 필요합니다. 이 설정이 없으면 `pageable`, `sort`, `first`, `last`, `empty` 등 불필요한 필드가 모두 포함됩니다.

```java
// Application.java
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class Application { ... }
```

**VIA_DTO 적용 후 응답 형식:**
```json
{
  "content": [
    { "id": "...", "loginId": "..." },
    { "id": "...", "loginId": "..." }
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### 삭제
```
HTTP 204 No Content
(응답 본문 없음)
```

---

## 어노테이션 정리

| 어노테이션 | 용도 |
|-----------|------|
| `@RestController` | REST API Controller |
| `@RequestMapping` | 기본 URL 매핑 |
| `@GetMapping` | GET 요청 |
| `@PostMapping` | POST 요청 |
| `@PatchMapping` | PATCH 요청 |
| `@PutMapping` | PUT 요청 |
| `@DeleteMapping` | DELETE 요청 |
| `@PathVariable` | URL 경로 변수 |
| `@RequestBody` | JSON Body |
| `@ModelAttribute` | 쿼리 파라미터 → DTO |
| `@Valid` | Validation 수행 |
| `@ResponseStatus` | 응답 상태 코드 지정 |
| `@RequiredArgsConstructor` | 생성자 주입 |
