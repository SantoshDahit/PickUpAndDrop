# RESTful API 설계 컨벤션

> 이 문서는 RESTful API 설계 원칙을 정의합니다.
> URL 패턴, HTTP 메서드 매핑 등 구현 수준의 규칙은 [08-controller-convention.md](./08-controller-convention.md)을 참고합니다.

---

## 핵심 원칙

### 1. 리소스 중심 설계

API는 **동사(행위)가 아닌 명사(리소스)** 로 설계한다.

```
# O — 리소스 중심
GET    /v1/members
POST   /v1/members
PATCH  /v1/members/{memberId}
DELETE /v1/members/{memberId}

# X — 행위 중심
POST   /v1/createMember
POST   /v1/updateMember
POST   /v1/deleteMember
```

### 2. HTTP 메서드로 행위를 표현

| 행위 | HTTP 메서드 | 멱등성 | 안전성 |
|------|------------|--------|--------|
| 조회 | GET | O | O |
| 생성 | POST | X | X |
| 전체 수정 | PUT | O | X |
| 부분 수정 | PATCH | X | X |
| 삭제 | DELETE | O | X |

- **GET**: 서버 상태를 변경하지 않는다. 조회 외 용도로 사용 금지.
- **POST**: 새 리소스를 생성하거나, 다른 메서드로 표현하기 어려운 작업에 사용.
- **PUT vs PATCH**: 전체 교체는 PUT, 일부 필드 수정은 PATCH. 일반적으로 PATCH를 사용한다.
- **DELETE**: 리소스를 삭제(soft delete 포함). 응답 본문 없이 204를 반환한다.

### 3. 적절한 HTTP 상태 코드 사용

| 상태 코드 | 용도 | 사용 시점 |
|-----------|------|----------|
| 200 OK | 성공 (조회, 수정) | GET, PATCH, PUT 성공 |
| 201 Created | 리소스 생성 성공 | POST 성공 |
| 204 No Content | 응답 본문 없음 | DELETE 성공 |
| 400 Bad Request | 잘못된 요청 | Validation 실패, 비즈니스 규칙 위반 |
| 401 Unauthorized | 인증 실패 | 토큰 없음/만료 |
| 403 Forbidden | 인가 실패 | 권한 없음 |
| 404 Not Found | 리소스 없음 | 존재하지 않는 ID 조회 |
| 500 Internal Server Error | 서버 오류 | 예상치 못한 에러 |

### 4. 리소스 관계 표현

계층 관계가 있는 리소스는 URL 경로로 표현한다.

```
# 중개소의 회원 목록
GET /v1/real-estate-agencies/{agencyId}/members

# 회원의 승인 상태 변경 (하위 속성)
PATCH /v1/members/{memberId}/approval
```

**깊이 제한**: URL 경로의 깊이는 최대 3단계까지만 허용한다.

```
# O
/v1/members/{memberId}/approval

# X — 너무 깊음
/v1/agencies/{agencyId}/members/{memberId}/approval/history
```

---

## 리소스 설계 규칙

### 컬렉션 vs 단건

| 패턴 | 의미 | 예시 |
|------|------|------|
| `/v1/{resources}` | 컬렉션 (목록) | `GET /v1/members` |
| `/v1/{resources}/{id}` | 단건 (특정 리소스) | `GET /v1/members/1` |
| `/v1/{resources}/search` | 검색 (쿼리 기반) | `GET /v1/members/search?approvalStatus=PENDING` |
| `/v1/{resources}/me` | 현재 인증된 사용자 | `GET /v1/members/me` |

### 검색과 필터링

- **검색 조건**: 쿼리 파라미터로 전달한다.
- **페이징**: `page`, `size`, `sort` 파라미터를 사용한다 (Spring Pageable).
- **검색 엔드포인트**: 조건이 복잡한 경우 `/search`를 사용한다.

```
# 단순 필터링 — 컬렉션 엔드포인트에 쿼리 파라미터
GET /v1/members?role=OWNER

# 복합 검색 — /search 엔드포인트
GET /v1/members/search?approvalStatus=PENDING&page=0&size=20&sort=createdAt,desc
```

---

## 요청/응답 설계 규칙

### 요청 (Request)

- **Content-Type**: JSON (`application/json`)을 기본으로 사용한다.
- **Body 없는 메서드**: GET, DELETE는 Request Body를 사용하지 않는다.
- **필드 네이밍**: camelCase를 사용한다.

### 응답 (Response)

- **단건 조회**: JSON 객체를 직접 반환한다 (래핑하지 않음).
- **목록 조회**: `Page` 객체를 반환한다 (`content` + `page` 구조).
- **생성**: 생성된 리소스를 반환한다 (201).
- **삭제**: 응답 본문 없이 204를 반환한다.

```java
// 단건 — 객체 직접 반환
@GetMapping("/{memberId}")
public MemberDto.Response getById(@PathVariable String memberId) { ... }

// 목록 — Page 반환
@GetMapping("/search")
public Page<MemberDto.SummaryResponse> search(..., Pageable pageable) { ... }

// 생성 — 201 + 생성된 리소스
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public MemberDto.Response create(@RequestBody @Valid MemberDto.PostRequest request) { ... }

// 삭제 — 204 + 본문 없음
@DeleteMapping("/{memberId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable String memberId) { ... }
```

### 에러 응답

프로젝트의 표준 에러 응답 형식을 따른다. ([09-exception-convention.md](./09-exception-convention.md) 참고)

```json
{
  "message": "에러 메시지",
  "errorCode": "ERR_CODE_001",
  "timestamp": "2026-04-03 10:30:45"
}
```

---

## Stateless 원칙

- 서버는 클라이언트의 상태를 저장하지 않는다.
- 모든 요청은 인증 정보(JWT)를 포함하여 독립적으로 처리 가능해야 한다.
- 세션을 사용하지 않는다 (`SessionCreationPolicy.STATELESS`).

---

## API 버전 관리

- URL 경로에 버전을 명시한다: `/v1/`, `/v2/`
- 기존 API의 하위 호환성이 깨지는 변경이 필요한 경우 새 버전을 추가한다.
- 같은 버전 내에서는 하위 호환성을 유지한다 (필드 추가는 허용, 삭제/변경은 불가).
