# 000 — Stack decision: Spring Boot REST API (conventions-driven)

**Status:** Accepted — **Revised 2026-07-26**: full REST API per `springboot/conventions/`
**Supersedes:** the Next.js 16 implementation at the repo root, and the earlier Thymeleaf server-rendered delivery layer (built, then replaced the same week — git history keeps it)

## Revision 2026-07-26 — team conventions adopted

The team's backend conventions (26 documents + code templates, now at `springboot/conventions/`)
are the **authoritative architecture reference**; this plan defers to them. Decisions taken with the owner:

- **Full REST API** (`/v1/...`, JWT auth, DTO responses). Thymeleaf pages dropped; a separate
  frontend (web/mobile) will consume the API.
- **MySQL** (Docker for local via `springboot/docker-compose.yml`, port **3307** — a native MySQL
  occupies 3306 on the dev machine; Testcontainers for tests).
- 4-layer architecture (Controller → Facade → Service → Repository), Lombok, QueryDSL,
  ModelMapper + BaseMapper, ErrorCode/ApiException, UUID string IDs, Base entities,
  Flyway timestamp naming (`V{YYYYMMDDHHMMSS}__{author}_{desc}.sql`), package-by-layer.
- Git: Korean branch names without prefixes; AI commits locally only — push/PR/merge require
  the owner's explicit go-ahead (conventions 24/25).

**Documented deviations from the conventions** (agreed pragmatics, revisit when relevant):
1. Error messages are **English** (product speaks English; the convention samples are Korean).
   The `errorCode` scheme `{DOM}_{HTTP}_{SEQ}` is followed exactly.
2. **Single `User` entity** instead of the Account/User split — LandGreet has one credential
   set with a small profile; split later if OAuth providers arrive (convention 13).
3. **Avatars deferred**: convention 21 requires S3 presigned upload; no S3 account exists yet.
   The previous local-disk avatar pipeline was removed rather than shipped off-convention.

## Name

**Pickup&Drop** (owner decision, 2026-07-26) — the original working title, restored after a brief rename to LandGreet the same day. Package/artifact name: `com.pickupdrop` / `pickupdrop`. Seed emails rebranded via migration (applied migrations are immutable). The earlier naming exploration below is kept for the record.

**LandGreet** (2026-07-26) — you land, we greet you: the whole value proposition in two plain English words, easy for the target customer (foreign travellers) to say and spell. No brand collisions found. `landgreet.com` was available at decision time; **register it before building further brand equity.** Runners-up, also available at the time: `greetride.com`, `greetport.com`, `sortride.com`, `poolfare.com`, `meetfare.com`. An earlier Korean-rooted candidate ("Majungo", from 마중) was set aside in favour of an English word. Working title "Pickup & Drop" survives only in the repo folder name and the frozen Next.js reference app.

## Decision

Rebuild the product as a single **Spring Boot** application with **Thymeleaf** server-rendered views. One deployable JAR serves both the pages and the backend logic — no separate frontend build, no API layer between our own UI and our own database.

## Stack

| Concern | Choice | Why |
|---|---|---|
| Language / runtime | Java 21 (LTS) | Current LTS, records + pattern matching, supported by every host |
| Framework | Spring Boot 3.x (latest stable; move to 4.x when the ecosystem settles) | Boring, documented, hireable |
| Build | Gradle (Kotlin DSL), wrapper checked in | Already on the dev machine; faster incremental builds than Maven |
| Views | Thymeleaf + fragments/layouts | Server-rendered, natural templating (valid HTML), auto-CSRF with Spring Security |
| Interactivity | Vanilla JS sprinkles; add **htmx** only when a page genuinely needs partial updates (fare calculator) | Avoid a JS build step entirely |
| CSS | Tailwind CLI (standalone binary) compiling `globals.css` → static css, checked into `src/main/resources/static/` at build | Keeps the existing design language without Node in the runtime |
| Security | Spring Security form login, `BCryptPasswordEncoder`, server-side sessions | Replaces the hand-rolled JWT cookie; CSRF on by default |
| Persistence | Spring Data JPA (Hibernate) | Standard repositories, Bean Validation integration |
| Database | **SQLite** via `sqlite-jdbc` + `hibernate-community-dialects`; single file at `data/app.db` | Matches the one-VPS deployment reality; zero ops. Swap to PostgreSQL when concurrent writes actually hurt — Flyway + JPA make that a config change plus a dialect review |
| Migrations | Flyway (`db/migration/V1__…​.sql`) | Versioned schema from day one; the Next.js version's biggest gap |
| Sessions | `spring-session-jdbc` (sessions table in the same SQLite file) | Logins survive app restarts/deploys, no Redis needed |

Rejected: keeping Next.js (the team wants one JVM codebase), REST API + separate SPA (needless for a server-rendered booking site), Turso (no first-class JDBC driver; plain SQLite covers the same need here).

## Migration strategy

1. New Spring project lives in **`springboot/`** at the repo root. The Next.js app stays where it is, frozen, as the behavioural reference ("how does the fare calculator round?", copy for UI text, `app/globals.css` for design tokens).
2. Features are re-specced in `plan/` (001 is already rewritten) and built to **parity checklist** below. Plans reference the old implementation's files for behaviour, the new stack for design.
3. Existing production data: current DB is seed-level (admin user + 2 routes + tiers). We re-seed via Flyway rather than migrate — passwords can't be carried anyway if hash parameters differ, and there are no real users yet. If real bookings exist by cutover, write a one-off CSV export/import; decide at cutover.
4. When parity is reached and deployed, delete the Next.js sources in one commit (`app/`, `components/`, `lib/`, Node config), and `springboot/` contents move to the repo root.

## Parity checklist (what "done" means for the cutover)

- [ ] Public landing page with live fare calculator (routes + group-size tiers)
- [ ] Signup / login / logout, change password (`001`)
- [ ] Profile CRUD + avatar (`001`)
- [ ] Booking flow: request a trip (route, date, flight no, group size, passenger names, contact, notes) with tier pricing snapshot
- [ ] My trips: list + cancel pending
- [ ] Admin: requested bookings (status, driver assignment, costs), routes & price tiers CRUD, drivers CRUD, users (`001`)
- [ ] Seeded admin account + starter routes on first boot
