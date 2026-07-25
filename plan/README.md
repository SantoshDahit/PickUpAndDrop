# Feature Plans

Design docs for **LandGreet** (landgreet.com) — airport pickups anywhere in Korea. Working title was "Pickup & Drop"; renamed 2026-07-26 (see 000). One file per feature, numbered in rough build order. A plan is written **before** the code and updated when reality disagrees with it — the plan is the contract, the code is the implementation.

## Index

| # | Feature | Status |
|---|---------|--------|
| [000](./000-stack-migration.md) | Stack decision: Spring Boot + Thymeleaf, migration off Next.js | Accepted |
| [001](./001-user-accounts.md) | User accounts — signup/login, profile CRUD, avatar image | Implemented |

Backlog (no spec yet, add as `002+` when picked up): booking lifecycle & driver assignment, landing page + fare calculator, email notifications (booking confirmed / password reset), payments & settlement tracking, driver portal, i18n (KO/EN).

## How to write a plan

Each plan has these sections — skip a section only if it genuinely doesn't apply:

1. **Problem / Goal** — why we're building this, in user terms.
2. **Current state** — what exists today, with file references. Forces the author to read the code first. While the migration is in flight this includes the frozen Next.js reference implementation.
3. **Scope** — what's in, and explicitly what's out.
4. **Design** — data model, web layer (controllers/views), services, and the key decisions *with the alternatives that were rejected and why*.
5. **Security & edge cases** — the paranoid checklist.
6. **Migration & rollout** — how existing data/deployments survive the change.
7. **Acceptance criteria** — checkable statements; a reviewer should be able to verify each one.
8. **Test plan** — what gets tested and how.

## Conventions for the Spring codebase (read before implementing any plan)

The stack itself is decided in [000](./000-stack-migration.md). Ground rules on top of it:

- **Project layout:** single Gradle project in `springboot/`, base package `com.landgreet`. Package by feature (`user/`, `booking/`, `admin/`), not by layer — `user/` holds `User`, `UserRepository`, `UserService`, `UserController`, `AccountController` together.
- **Web layer:** classic MVC — `@Controller` + Thymeleaf views, POST-redirect-GET everywhere. Flash messages via `RedirectAttributes` (replaces the old `?error=` query-param style). Bean Validation (`@Valid` on form-backing objects) with errors rendered next to fields via `th:errors`.
- **No `@ResponseBody` JSON endpoints for our own pages.** If a page needs partial updates, return a Thymeleaf fragment (htmx-style); a JSON API only appears when an external consumer (mobile app) does.
- **Persistence:** JPA entities kept thin (no business logic), Spring Data repositories, service layer owns transactions (`@Transactional` on services, never controllers). Schema changes go through **Flyway only** — `ddl-auto=none` (SQLite's loose column typing defeats Hibernate's validator), never `update`.
- **Security:** all authorization in `SecurityFilterChain` route rules **plus** method-level checks (`@PreAuthorize` or explicit ownership checks in services) — route rules alone don't protect against IDOR. CSRF stays on; Thymeleaf injects tokens into forms automatically.
- **Templates:** `src/main/resources/templates/`, shared chrome via layout fragments (`fragments/layout.html`). Design tokens and UI text are ported from the frozen Next.js app (`app/globals.css`, page copy) — keep the visual language.
- **Money:** integer KRW (`int` won amounts, no decimals), formatted `₩#,###` via a shared Thymeleaf helper. Same rule the old app followed with `toLocaleString()`.
- **Config:** `application.yml` + env-var overrides for secrets. Never commit secrets; dev defaults may exist but must log a loud warning if used with a production profile.
