# 001 — User Accounts: signup/login, profile CRUD, avatar image

**Status:** Implemented (2026-07-26) — all acceptance criteria verified end-to-end (headless-browser pass + `./gradlew test`)
**Revision 2026-07-26:** re-implemented as REST API per `springboot/conventions/` (see 000): `/v1/auth/signup|login` (JWT), `/v1/users/me` GET/PATCH, `/v1/users/me/password`, DELETE `/v1/users/me` (password re-auth, soft delete, email slot freed, active bookings cancelled). Avatars deferred pending S3 (convention 21). Sections below describe the original Thymeleaf implementation and remain the behavioural spec.
**Stack:** Spring Boot + Thymeleaf (see [000](./000-stack-migration.md))
**Depends on:** project skeleton (Gradle project, Flyway, Spring Security wiring) — created as part of this plan since it's the first feature.

## 1. Problem / Goal

First feature on the new stack: complete, boring, reliable user accounts. The frozen Next.js app had signup/login/change-password only; this plan delivers the full CRUD plus profile images (drivers meeting strangers at arrivals benefit from seeing a face):

- **C**reate — signup
- **R**ead — own profile on `/account`; admin user list on `/admin/users`
- **U**pdate — edit name / phone / email; upload / replace / remove an avatar; change password
- **D**elete — self-service account deletion (soft), admin deactivate/reactivate

## 2. Current state

Nothing exists on the Spring side — this plan also bootstraps the skeleton. Behavioural reference is the frozen Next.js app:

| Behaviour to preserve | Reference |
|---|---|
| Signup fields (name, email, phone, password ≥ 6), lowercase-trimmed email, duplicate-email message | `lib/actions.ts` `signup` |
| Login redirect: admin → `/admin`, user → `/trips`; generic "didn't match" error | `lib/actions.ts` `login` |
| Change password: verify current, min length, confirm match | `lib/actions.ts` `changePassword` |
| Seeded admin on empty DB (now `admin@landgreet.com`) | `lib/db.ts` `init()` |
| Users schema baseline | `lib/db.ts` `SCHEMA` |

What the old app **didn't** have, added here: profile editing, avatar, account deletion, admin user management, real schema migrations, sessions that die when an account is deactivated.

## 3. Scope

**In:** project skeleton (Gradle, Boot, Flyway, Security, session-jdbc, Thymeleaf layout fragment, static CSS ported from the old design tokens), `users` table V1 migration + seed, signup/login/logout, profile edit, avatar upload/serve/remove, change password, soft account deletion, admin users page.

**Out:** email flows (verification, password reset), phone verification, login rate limiting/lockout (backlog — note it in the security config as a TODO with the chosen approach: bucket per email+IP), OAuth, roles beyond the `ADMIN` flag, data export. Booking/trips/routes tables come in 002 even though the old schema had them — one feature per plan.

## 4. Design

### 4.1 Schema (Flyway `V1__users.sql`, `V2__seed_admin.sql`)

```sql
-- V1__users.sql
CREATE TABLE users (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  name          TEXT    NOT NULL,
  email         TEXT    NOT NULL UNIQUE,          -- stored lowercase; service normalises
  password_hash TEXT    NOT NULL,
  phone         TEXT,
  is_admin      INTEGER NOT NULL DEFAULT 0,
  avatar_key    TEXT,                             -- opaque storage key, never a path/URL
  created_at    TEXT    NOT NULL DEFAULT (datetime('now')),
  updated_at    TEXT,
  deleted_at    TEXT                              -- soft delete; NULL = active
);
```

`V2` inserts the admin (`admin@landgreet.com` / bcrypt of a bootstrap password) — same convention as the old seed, but the password comes from `app.seed.admin-password` (env-overridable) at first boot via an `ApplicationRunner`, **not** hard-coded in the SQL. Flyway seeds structure; runtime seeds credentials.

Soft delete rationale: future `trip_requests.user_id` will reference users; hard deletion orphans booking history the admin needs for accounting. Deleted users: can't log in, email freed for re-registration by renaming to `deleted:{id}:{original}` at deletion time (keeps UNIQUE honest), name kept for old bookings.

### 4.2 Domain (`com.landgreet.user`)

- `User` entity mapping the table; `isAdmin`/`deletedAt` exposed, password hash never rendered (no `toString` leak).
- `UserRepository extends JpaRepository<User, Long>` + `findByEmailAndDeletedAtIsNull`.
- `UserService` — all mutations, `@Transactional`: `register`, `updateProfile`, `changePassword`, `updateAvatar`, `removeAvatar`, `softDelete`, admin `setActive`/`setAdmin`. Uniqueness race (two signups, same email): rely on the DB UNIQUE constraint, catch `DataIntegrityViolationException`, surface the same "already registered" field error — check-then-insert alone is not enough.

### 4.3 Security config

- `SecurityFilterChain`: permit `/`, `/login`, `/signup`, `/css/**`, `/avatars/**`; `/admin/**` requires `ROLE_ADMIN`; everything else authenticated. Form login at `/login` (custom Thymeleaf page), logout POST `/logout`, success handler routes admins to `/admin`, users to `/trips` (parity with old behaviour).
- `UserDetailsService` loads by email **where `deleted_at IS NULL`** — deactivation kills future logins.
- **Live-session kill on deactivate/delete:** server-side sessions via `spring-session-jdbc` + `SessionRegistry`; `softDelete`/`setActive(false)` expire the user's sessions immediately. This closes the 30-day zombie-JWT hole the old app had, one better than "dead on next request".
- `BCryptPasswordEncoder` (default strength 10, matches old hashes' cost — irrelevant anyway since we re-seed).
- CSRF: default-on; Thymeleaf `th:action` forms get the token automatically. Multipart forms too (token in the form, not the URL).

### 4.4 Avatar storage — the decision

Same trade study as the original plan, same conclusion, Java edition:

| Option | Verdict |
|---|---|
| **A. Files on disk** under `data/uploads/avatars/` | ✅ Zero new services; sits next to `data/app.db`, one directory to persist/back up on the VPS |
| B. BLOB column | ❌ Bloats every user row read; ugly beyond tiny images |
| C. Object storage (R2/S3) | ❌ Third service + creds for an MVP with ~0 users; revisit if we leave the single VPS |

**A, behind an interface** so C is a drop-in later:

```java
public interface ObjectStorage {
  void put(String key, byte[] data, String contentType);
  Optional<StoredObject> get(String key);   // StoredObject(byte[] data, String contentType)
  void delete(String key);
}
@Component class LocalDiskStorage implements ObjectStorage { /* data/uploads/… */ }
```

DB stores an opaque `avatar_key` like `avatars/7f3a….jpg`. Serving: `GET /avatars/{key}` controller streams from storage with `Cache-Control: public, max-age=31536000, immutable` — safe because every upload mints a new random key, so replacement never fights the cache. Static-resource serving from the upload dir is deliberately **not** used: a controller lets us validate the key shape and swap the backend invisibly.

### 4.5 Image processing pipeline

**Thumbnailator** (tiny, battle-tested) — every upload is decoded and re-encoded; we never store bytes the user sent:

1. Spring multipart caps: `max-file-size: 5MB`, `max-request-size: 6MB` (rejection happens before our code runs; a friendly error page maps `MaxUploadSizeExceededException`).
2. `Thumbnails.of(input).size(256, 256).crop(Positions.CENTER).outputFormat("jpg").outputQuality(0.85)` — square crop, EXIF orientation applied, **metadata (GPS etc.) stripped** by re-encode. Output JPEG ~10–20 KB.
   - JPEG, not WebP: Java's ImageIO has no native WebP writer; pulling in `webp-imageio` native binaries for a ~5 KB saving per avatar is not worth the packaging risk. Revisit only if bandwidth ever matters.
3. Decoding failure (`IOException`/null image) *is* our MIME validation — client Content-Type and filename are ignored entirely.
4. Key = `avatars/{UUID}.jpg`. Order of operations: write new object → update `avatar_key` → delete old object. A crash mid-sequence leaves an orphan file, never a broken profile.

### 4.6 Web layer (controllers + templates)

| Route | Controller | View / behaviour |
|---|---|---|
| `GET /signup`, `POST /signup` | `AuthController` | `auth/signup.html`; form-backing `SignupForm` with Bean Validation (`@NotBlank @Size(max=100)` name, `@Email @Size(max=254)`, `@Size(min=6)` password, phone ≤ 30); on success auto-login and redirect `/trips` |
| `GET /login` | `AuthController` | `auth/login.html`; Spring Security handles the POST; `?error` param renders the generic message |
| `GET /account` | `AccountController` | `account/index.html` — three cards: **Profile** (avatar preview + upload/remove, name, phone, email), **Password**, **Danger zone** |
| `POST /account/profile` | `AccountController` | `@Valid ProfileForm`; email uniqueness re-checked; flash success |
| `POST /account/avatar` `POST /account/avatar/delete` | `AccountController` | multipart pipeline of §4.5 / clear + delete object |
| `POST /account/password` | `AccountController` | verify current, min 6, confirm match — same rules as old app |
| `POST /account/delete` | `AccountController` | requires current password in the form (re-auth for destructive ops); soft-delete, expire sessions, redirect `/` |
| `GET /avatars/{key}` | `AvatarController` | §4.4; key must match `^[0-9a-f\-]{36}\.jpg$` else 404 |
| `GET /admin/users` | `AdminUserController` | table: avatar, name, email, phone, joined, admin badge, active state; deactivate/reactivate + promote/demote buttons |
| `POST /admin/users/{id}/active` `POST /admin/users/{id}/admin` | `AdminUserController` | toggles; **self-targeting refused** (can't deactivate or demote yourself — protects the last admin) |

Templates share `fragments/layout.html` (header with avatar-or-initial next to the user's name, footer) — design tokens ported from `app/globals.css`. Avatar `<img>` is plain HTML; no image-optimization framework needed for a pre-sized 256px JPEG.

No avatar at signup — first-run friction for zero benefit; users add it from `/account`.

## 5. Security & edge cases

- [ ] Authorization is route rules **and** service-level checks: admin toggles verify the caller isn't the target; account mutations always operate on the *authenticated* user's id, never an id from the form (IDOR-proof by construction).
- [ ] Upload: container-level size cap, decode-and-re-encode as the only validation, random UUID keys, metadata stripped, client MIME/filename ignored.
- [ ] `/avatars/{key}` regex-validates the key — no traversal into `data/`.
- [ ] Email change to a taken address → field error, same message as signup (race handled by UNIQUE + exception mapping, §4.2).
- [ ] Deactivated/deleted user: sessions expired immediately via `SessionRegistry`; login blocked by the `deleted_at IS NULL` lookup with the same generic error as a bad password (no account-state oracle).
- [ ] Self-demotion / self-deactivation blocked server-side, not just hidden buttons.
- [ ] Account deletion requires password re-entry; frees the email slot (§4.1); avatar object deleted.
- [ ] CSRF on for every state-changing route, including multipart.
- [ ] Bootstrap admin password: env-overridable, warn loudly at startup if the default is active outside the `dev` profile.
- [ ] Concurrent avatar replace: last-write-wins on `avatar_key`; loser's file is orphaned, never user-visible. Acceptable; future janitor task noted.
- [ ] SQLite + JPA gotcha: single-writer database — keep transactions short, set `busy_timeout` in the JDBC URL so concurrent form posts queue instead of failing.

## 6. Migration & rollout

1. This plan creates `springboot/` (skeleton per [000](./000-stack-migration.md) §Stack): `build.gradle.kts` + wrapper, `application.yml` (SQLite datasource, Flyway on, `ddl-auto=none`, multipart caps, session-jdbc), layout fragment, hand-ported static CSS.
2. Fresh boot: Flyway runs V1/V2, runner seeds the admin credential, `data/uploads/` created lazily by `LocalDiskStorage`.
3. The Next.js `data/app.db` is **not** migrated — different schema lineage; users are seed-level (see 000 §Migration). Keep the old file untouched (Flyway history table names don't collide, but use a fresh `data/app-spring.db` during side-by-side development to be safe; rename at cutover).
4. Verify `.gitignore` covers `data/` (it does) and add `springboot/build/`, `springboot/.gradle/`.

## 7. Acceptance criteria

- [ ] `./gradlew bootRun` on a fresh clone boots clean, seeds admin + schema exactly once (restart twice, check `flyway_schema_history`).
- [ ] Sign up → redirected to `/trips` logged in; `/account` shows profile with initial-letter avatar fallback.
- [ ] Upload a 4 MB JPEG portrait → circle-cropped avatar in header and account page; stored file is a 256px JPEG with no EXIF/GPS; old file gone after replacing it.
- [ ] Upload a `.txt` renamed `.jpg` → friendly field error, profile unchanged. 6 MB file → friendly size error.
- [ ] Edit name → header updates immediately. Change email to a taken one → field error; to a fresh one → old email can't log in, new one can.
- [ ] Wrong current password blocks password change and account deletion; correct password deletes: logged out, old creds get the generic error, email is reusable for a new signup.
- [ ] Admin sees users in `/admin/users`; deactivating a user kills that user's **live session** (their next click lands on `/login`); reactivate → login works.
- [ ] Admin's own row offers no deactivate/demote, and hand-crafted POSTs to self-target are refused.
- [ ] `GET /avatars/../app.db` and malformed keys → 404, never file contents.
- [ ] Any state-changing POST without a CSRF token → 403.

## 8. Test plan

Spring gives us a real test harness from day one — use it:

- **Unit:** `UserServiceTest` (Mockito) — email normalisation, soft-delete email renaming, self-target refusal, uniqueness-violation mapping.
- **Web slice:** `@WebMvcTest` + `spring-security-test` — route protection matrix (anon vs user vs admin on every route above), CSRF rejection, `AvatarController` key validation.
- **Integration:** `@SpringBootTest` with a temp-file SQLite — full signup → upload (multipart with a real tiny JPEG fixture) → edit → deactivate flow; Flyway idempotency (context restarts).
- **Manual pass** of the acceptance criteria against `./gradlew bootRun`, driven with the headless-browser smoke scripts.
