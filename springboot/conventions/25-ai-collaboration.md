# 25. AI 어시스턴트 협업 컨벤션

Claude Code 등 AI 어시스턴트를 사용하여 백엔드 작업을 진행할 때 지켜야 하는 규칙을 정의한다.

---

## 핵심 원칙

- **변경의 원격 전파(push, PR 생성)는 사용자가 직접 통제한다.**
- AI는 로컬에서 코드 작성·수정·commit·테스트 실행까지 자율 수행 가능.
- AI는 `git push`, GitHub PR 생성, force-push, 원격 브랜치 삭제 등 **원격에 영향을 미치는 모든 행위에 대해 사용자의 명시적 허락**을 먼저 받는다.

---

## AI에게 허용되는 작업 (사용자 허락 불요)

| 카테고리 | 예시 |
|---------|------|
| 코드 작성·수정 | 파일 편집, 신규 파일 생성, 삭제 |
| 로컬 commit | `git commit`, `git add` |
| 테스트 실행 | `./gradlew test`, `./gradlew bootRun` |
| 로컬 브랜치 조작 | `git checkout -b`, `git branch`, `git reset --soft/--mixed` |
| 정보 조회 | `git log`, `git diff`, `git status`, 코드 검색 |

---

## AI가 사용자 허락을 받아야 하는 작업

| 카테고리 | 예시 |
|---------|------|
| 원격 push | `git push`, `git push --force-with-lease`, `git push --force` |
| PR 생성 | `gh pr create`, GitHub PR 링크 호출, GitHub API로 PR 생성 |
| 원격 브랜치 삭제 | `git push origin --delete {branch}` |
| 강제 reset | `git reset --hard` (push된 이력을 건드릴 때) |
| 머지·머지 취소 | `gh pr merge`, `git revert` (이미 push된 경우) |
| 운영 시스템 변경 | DB 직접 변경, 운영 환경 배포 트리거 |

---

## 허락을 받는 방식

- 사용자가 명시적으로 **"push 해줘"**, **"PR 올려줘"**, **"머지해줘"** 등 행동 동사를 표현했을 때만 진행.
- "공유해줘", "팀에 알려줘" 같은 모호한 표현은 push/PR 의도인지 다시 확인.
- 사용자가 한 번 허락한 동작이라도 **다른 컨텍스트에서는 다시 확인**한다 (예: image PR push 허락이 trust PR push까지 자동 확장되지 않음).

---

## 이미 원격 전파된 작업을 되돌리는 경우

- `--force` 푸시, 원격 브랜치 삭제 등은 destructive 행위.
- AI는 되돌릴 수 있는 옵션들을 제시하고, 사용자가 어느 옵션을 선택할지 받은 후에만 실행한다.

---

## 본 컨벤션의 위치

본 규칙은 인간 ↔ AI 협업 흐름에 한정된다. 팀원 본인이 직접 git을 다룰 때의 규칙은 [24-git-branch-convention.md](./24-git-branch-convention.md) 및 [18-deployment-convention.md](./18-deployment-convention.md)를 따른다.
