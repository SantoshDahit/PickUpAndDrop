# 20. Flyway 마이그레이션 컨벤션

## 개요

본 프로젝트는 **Flyway**를 사용하여 DB 스키마 변경을 버전 관리합니다.
모든 DDL/DML 변경은 마이그레이션 파일로 관리하며, `baseline-on-migrate` 모드로 기존 DB에도 안전하게 적용됩니다.

---

## 네이밍 컨벤션

### 파일명 형식

```
V{YYYYMMDDHHMMSS}__{작성자}_{설명}.sql
```

| 구성요소 | 규칙 | 예시 |
|----------|------|------|
| `V{YYYYMMDDHHMMSS}` | 작성 시점의 KST 시각 (14자리) | `V20260518143012` |
| `__` | 더블 언더스코어 (Flyway 필수) | `__` |
| 작성자 | 작성자 식별자 | `kns`, `santos` |
| 설명 | 변경 내용 (snake_case) | `add_vote_tag`, `init_schema` |

### V번호 생성 (KST)

```bash
V$(TZ=Asia/Seoul date +%Y%m%d%H%M%S)__kns_add_vote_tag.sql
```

### 예시

```
V20260518143012__kns_add_vote_tag.sql
V20260518154500__santos_add_reward_log.sql
```

### 기존 마이그레이션 호환

기존 `V1__init_schema.sql` ~ `V6__20260518_kns_add_vote_participation.sql` 파일은 **변경하지 않는다**. Flyway는 V번호를 숫자로 비교하므로 `V6 < V20260518...`로 자연 정렬되어 새 타임스탬프 형식과 호환된다. 새 마이그레이션부터 타임스탬프 형식을 적용한다.

---

## 헤더 주석 템플릿

```sql
-- ============================================
-- Migration V{YYYYMMDDHHMMSS}
-- ============================================
-- 설명: {변경 내용}
-- 작성일: {YYYY-MM-DD HH:MM:SS}
-- ============================================
```

---

## Flyway 설정

```yaml
# application-flyway.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
    placeholders:
      schema: ${DB_SCHEMA}
```

| 설정 | 값 | 설명 |
|------|-----|------|
| `enabled` | `true` | Flyway 활성화 |
| `baseline-on-migrate` | `true` | 기존 DB에 첫 마이그레이션 시 baseline 자동 생성 |
| `baseline-version` | `0` | baseline 기준 버전 (V1부터 적용) |
| `placeholders.schema` | `${DB_SCHEMA}` | 환경변수로 스키마명 주입 |

> `out-of-order`는 설정하지 않는다 (기본값 `false` 유지). CI 가드(`flyway-version-check`)가 V번호 단조 증가를 강제하므로 운영DB에는 항상 오름차순으로 마이그레이션이 들어오며, `out-of-order: false`는 만에 하나 V번호 역전이 발생했을 때 Flyway가 마이그레이션 검증 단계에서 한 번 더 차단해주는 이중 안전망 역할을 한다.

### Placeholder 사용

```sql
-- 마이그레이션 파일에서 스키마명 동적 참조
ALTER DATABASE `${schema}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 동시 작업 규칙

여러 개발자가 동시에 마이그레이션을 작성할 때 발생하는 ① 파일명 충돌과 ② 실행 순서 역전을 방지하기 위한 규칙이다.

### 브랜치 워크플로우

- 모든 백엔드 작업(마이그레이션 유무와 무관)은 한글 이름의 작업 브랜치에서 진행한다 (prefix 없음). 예: `투표`, `리워드`, `마이그-vote-tag`. 자세한 규칙은 [24-git-branch-convention.md](./24-git-branch-convention.md)
- PR target은 항상 `feature` (통합 브랜치)
- `feature` 직접 push는 GitHub branch protection으로 차단된다

### V번호 부여 규칙

- V번호는 **작성 시점의 KST 시각**으로 부여한다 (`V$(TZ=Asia/Seoul date +%Y%m%d%H%M%S)`)
- 작성 시점이 아닌 머지 시점 기준으로 V번호 순서가 보장되어야 하므로, **PR 머지 직전에 `feature`의 최신 V번호를 확인**한다
- 최신 V번호 이하면 파일명을 더 큰 타임스탬프로 rename 후 다시 push

### CI 가드 (`flyway-version-check`)

- PR에 새로 추가된 마이그레이션 파일의 V번호가 `feature`의 최신 V번호보다 작거나 같으면 **CI fail**
- branch protection에서 `Require branches to be up to date before merging`가 켜져 있어 머지 직전 base 최신 상태와 다시 비교 강제됨
- CI fail 시 안내: 파일명을 `V$(TZ=Asia/Seoul date +%Y%m%d%H%M%S)__...`로 rename 후 push

---

## DB 설계 원칙

### 정규화: BCNF 필수

모든 테이블 설계는 **BCNF(Boyce-Codd Normal Form)** 까지 정규화해야 합니다.

| 정규형 | 조건 |
|--------|------|
| **1NF** | 모든 컬럼이 원자값(Atomic Value)을 가질 것 — 반복 그룹, 다중값 컬럼 금지 |
| **2NF** | 1NF + 부분 함수 종속 제거 — 복합키의 일부에만 종속되는 컬럼이 없을 것 |
| **3NF** | 2NF + 이행 함수 종속 제거 — 비키 컬럼이 다른 비키 컬럼에 종속되지 않을 것 |
| **BCNF** | 3NF + 모든 결정자가 후보키일 것 — 후보키가 아닌 컬럼이 다른 컬럼을 결정하지 않을 것 |

### 반정규화(Denormalization)

성능상 명확한 이유가 있을 때만 반정규화를 허용합니다.
반정규화 시 마이그레이션 헤더 주석에 **사유를 반드시 기록**합니다.

```sql
-- ============================================
-- Migration V20260406103045
-- ============================================
-- 설명: order 테이블에 customer_name 반정규화 컬럼 추가
-- 반정규화 사유: 주문 목록 조회 시 customer JOIN 제거로 응답 시간 40% 개선
-- 작성일: 2026-04-06 10:30:45
-- ============================================
```

---

## 마이그레이션 작성 규칙

### 1. 문자셋: UTF8MB4

```sql
ALTER DATABASE `${schema}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. UUID PK (VARCHAR(255))

```sql
id VARCHAR(255) NOT NULL COMMENT '식별자',
PRIMARY KEY (id)
```

- **타입**: `VARCHAR(255)`
- **생성**: 애플리케이션 레벨에서 `UUID.randomUUID().toString()`

### 3. Soft Delete: `deleted_at DATETIME NULL`

```sql
deleted_at DATETIME NULL COMMENT '삭제 일시',
```

### 4. Audit 필드: `created_at`, `updated_at`

```sql
created_at DATETIME DEFAULT (NOW()) NOT NULL,
updated_at DATETIME DEFAULT (NOW()) NOT NULL,
```

- `created_at`: 생성 시 자동 설정 (`DEFAULT NOW()`)
- `updated_at`: 생성 시 자동 설정, 수정 시 JPA가 관리

### 5. ENUM 타입 사용

```sql
role   ENUM('USER', 'HOSPITAL', 'ADMIN') NOT NULL COMMENT '역할',
status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING' NOT NULL,
```

### 6. 외래키 + 인덱스

```sql
CONSTRAINT fk_api_call_log_account
    FOREIGN KEY (created_by) REFERENCES account(id),
INDEX idx_created_by (created_by)
```

### 7. 한국어 COMMENT

```sql
id            VARCHAR(255) NOT NULL COMMENT '식별자',
login_id      VARCHAR(255) NOT NULL COMMENT '로그인 ID',
password      VARCHAR(255) NOT NULL COMMENT '비밀번호',
last_login_at DATETIME     NULL     COMMENT '마지막 로그인 일시',
```

---

## 대표 예시

### 테이블 추가

```sql
-- V20260518143012__kns_add_product.sql
CREATE TABLE product (
    id          VARCHAR(255) PRIMARY KEY,
    name        VARCHAR(100)             NOT NULL COMMENT '상품명',
    status      ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE' NOT NULL COMMENT '상태',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME                 NULL COMMENT '삭제 일시',
    INDEX idx_status (status)
);
```

### ENUM 값 추가

```sql
-- V20260518154500__santos_add_account_role_manager.sql
ALTER TABLE account
MODIFY COLUMN role ENUM('USER', 'ADMIN', 'MANAGER') NOT NULL COMMENT '역할';
```
