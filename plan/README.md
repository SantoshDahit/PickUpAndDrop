# Feature Plans

Design docs for **Pickup&Drop** — airport pickups anywhere in Korea. (Named Pickup&Drop by the owner 2026-07-26, reverting the brief LandGreet rename; see 000.) One file per feature, numbered in rough build order. A plan is written **before** the code and updated when reality disagrees with it — the plan is the contract, the code is the implementation.

## Index

| # | Feature | Status |
|---|---------|--------|
| [000](./000-stack-migration.md) | Stack decision: Spring Boot + Thymeleaf, migration off Next.js | Accepted |
| [001](./001-user-accounts.md) | User accounts — signup/login, profile CRUD (avatar deferred) | Implemented (REST) |
| [002](./002-booking-and-group-matching.md) | Booking with 7-day group matching + group chat | Implemented (REST) |
| [003](./003-driver-management.md) | Taxi drivers — roster, admin CRUD, ride assignment | Implemented (REST) |
| [004](./004-admin-web.md) | Admin web app — separate Vite+React SPA | Implemented |
| [005](./005-driver-accounts.md) | Driver accounts — DRIVER role, self profile, my rides | Implemented (REST) |
| [006](./006-admin-rides-and-console.md) | Admin-published joinable rides + formal console redesign | Implemented |
| [007](./007-customer-web.md) | Customer frontend — the Next.js app ported to the /v1 API | Implemented |
| [008](./008-week-bucket-groups.md) | Landing-week groups — book first, then choose your group | Implemented |
| [009](./009-pricing.md) | Realistic pricing — cost analysis, two-zone fare tables | Implemented |
| [010](./010-deployment.md) | Production deployment — Contabo VPS, Blue-Green, Vercel | Draft |
| [011](./011-transactional-email.md) | Transactional email — welcome, booking receipt, password reset | Implemented |
| [012](./012-admin-chat-moderation.md) | Admin chat — read/reply as staff, add & remove members | Implemented |
| [013](./013-traveller-services.md) | Traveller services — SIM card requests, services shelf | Implemented |
| [014](./014-support-chat.md) | Contact the team — direct traveller ↔ operator chat | Implemented |

Backlog (no spec yet, add as `012+` when picked up): pricing/fare tiers + settlement (introduces the `ride` entity, see 003 §4.1), booking lifecycle/ops workflow, landing page + fare calculator, driver ride-status workflow (picked-up/completed) **plus the driver-assigned email it unblocks (011 §3)**, email verification of new addresses (011 §3), avatars & driver photos (blocked on S3, convention 21), i18n (KO/EN — including email copy).

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
