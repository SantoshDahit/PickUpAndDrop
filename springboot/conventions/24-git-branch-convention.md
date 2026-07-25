# 24. Git 브랜치 명명 컨벤션

백엔드 기능 단위 작업 브랜치를 어떻게 명명하고 push할지 정의한다.

---

## 핵심 원칙

- **한글 이름만 사용**: 기능/도메인 이름을 한글로 그대로 사용한다.
- **prefix 일체 금지**: `feature/`, `feature-`, `backend/feature-` 등 모든 prefix 형태 금지.
- **로컬·원격 동일**: 로컬에서 만든 브랜치명을 그대로 push한다 (PR 직전에 영문으로 rename하지 않는다).
- **PR target**: 항상 `feature` (절대 `main` 아님). 자세한 흐름은 [18-deployment-convention.md](./18-deployment-convention.md) 참고.

---

## 권장 형식

```
{기능명-한글}
```

기능명은 단어를 `-`(하이픈)으로 연결한다.

### 예시

| 작업 | 브랜치명 |
|------|----------|
| 투표 이미지 업로드 | `투표-이미지` |
| 신뢰도 도메인 1차 | `신뢰도` |
| 리워드 출금 흐름 | `리워드-출금` |
| 마이그레이션 단독 작업 | `마이그-vote-tag` |
| 깃 컨벤션 정정 | `깃-컨벤션-정정` |

---

## 작업 흐름

1. `feature` 최신 상태 fetch
2. 새 브랜치 생성 (한글 자유롭게, prefix 없이)
   ```bash
   git checkout feature
   git pull
   git checkout -b 투표-이미지
   ```
3. 작업하며 commit 누적
4. 작업 완료 시 push
   ```bash
   git push -u origin 투표-이미지
   ```
5. GitHub에서 PR 생성 (base = `feature`)
6. 리뷰·CI(Flyway 가드 등) 통과 후 머지
7. 머지된 작업 브랜치는 삭제

---

## 금지 사항

- **`feature` 직접 push 금지** — 모든 변경은 PR로만 들어간다.
- **prefix 사용 금지** — `feature/XXX`, `feature-XXX`, `backend/feature-XXX` 등 모든 prefix 형태 일체 금지. 한글 이름만.
- 영문/한글 혼용 금지 — 한 브랜치 내에서 한글로 통일.
- 의미 없는 브랜치명 금지 (`test`, `tmp`, `wip` 단독 등).

---

## git이 한글 ref를 지원하는가?

지원한다. UTF-8 인코딩이 표준이며 macOS / Linux / Windows(최신) 모두 정상 동작 확인. GitHub UI·CLI도 한글 브랜치명을 정상 표시한다.

---

## 기존 영문·prefix 브랜치는 어떻게 하나?

이미 생성·push된 영문 또는 prefix 브랜치(예: `backend/feature-vote-image`, `backend/feature-trust`)는 historical record로 그대로 유지한다. **신규 작업부터** 한글-only 명명 적용.

---

## 참조

- 배포 흐름 전반: [18-deployment-convention.md](./18-deployment-convention.md)
- Flyway 마이그레이션 V번호 규칙 (브랜치 분리와 함께 고려): [20-flyway-convention.md](./20-flyway-convention.md)
