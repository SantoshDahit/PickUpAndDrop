# Feature Plans

Design docs for **LandGreet** (landgreet.com) — airport pickups anywhere in Korea. Working title was "Pickup & Drop"; renamed 2026-07-26 (see 000). One file per feature, numbered in rough build order. A plan is written **before** the code and updated when reality disagrees with it — the plan is the contract, the code is the implementation.

## Index

| # | Feature | Status |
|---|---------|--------|
| [000](./000-stack-migration.md) | Stack decision: Spring Boot + Thymeleaf, migration off Next.js | Accepted |
| [001](./001-user-accounts.md) | User accounts — signup/login, profile CRUD (avatar deferred) | Implemented (REST) |
| [002](./002-booking-and-group-matching.md) | Booking with 7-day group matching + group chat | Implemented (REST) |
| [003](./003-driver-management.md) | Taxi drivers — roster, admin CRUD, ride assignment | Implemented (REST) |
| [004](./004-admin-web.md) | Admin web app — separate Vite+React SPA | Implemented |

Backlog (no spec yet, add as `004+` when picked up): pricing/fare tiers + settlement (introduces the `ride` entity, see 003 §4.1), booking lifecycle/ops workflow, landing page + fare calculator, email notifications, driver portal (driver accounts + DRIVER role), avatars & driver photos (blocked on S3, convention 21), i18n (KO/EN).

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

## Conventions

The authoritative codebase conventions live in **`springboot/conventions/`** (26 documents +
code templates) — read them before implementing any plan. Plan 000 records the adoption
decision and the agreed deviations (English error messages, single User entity, avatars
deferred pending S3).
