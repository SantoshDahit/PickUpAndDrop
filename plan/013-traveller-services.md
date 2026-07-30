# 013 — Traveller services: SIM card requests, and a services shelf after login

**Status:** Implemented (2026-07-31) — 82/82 `./gradlew test` green (17 new), verified end-to-end in both apps via headless Chromium
**Depends on:** [001](./001-user-accounts.md) (accounts), [007](./007-customer-web.md) (customer pages), [006](./006-admin-rides-and-console.md) (admin console)

## 1. Problem / Goal

A traveller landing in Korea needs more than the ride. The first thing most of them want is a
**working phone** — a local SIM before they leave the terminal. Today there is nowhere to ask for
one: the product knows how to move people and nothing else, so these requests arrive by
KakaoTalk and live in someone's messages.

Goal: a place, visible once you are logged in, where a traveller can **request a SIM card** and
see what else the company offers, and where the operator sees those requests as a work queue
instead of a chat backlog.

## 2. Current state

Nothing exists. The logged-in nav is *My trips · Account · Book a pickup* (007), and every
customer table hangs off `booking`. There is no concept of a service that isn't a ride.

The nearest precedent is `booking` itself: a customer-owned row with a status the admin moves
through, listed for the owner at `/trips` and for the operator in the console. This plan copies
that shape rather than inventing one.

## 3. Scope

**In:** one `service_request` table covering any requestable service, keyed by type; the SIM card
request end to end (traveller creates, sees status, cancels; admin lists, progresses status,
adds an internal note); a `/services` page after login; a **Services** page in the admin console;
an informational card for the bank balance facility.

**Out (deliberately):** payment or pricing of services (the fare model in 009 is per-ride; a
service price list needs its own plan, and SIM prices change with the carrier's promos —
today the team quotes on confirmation); attaching a SIM request to a booking (owner's call
2026-07-31 — travellers ask for SIMs whether or not they have booked, and forcing a booking
first would lose requests. The arrival details are collected instead); passport/ID upload for
SIM registration (needs S3, blocked exactly as avatars are — convention 21; the team collects
documents offline for now); email on status change (the provider exists since 011 and this is a
natural second use — deliberately deferred so this plan stays one thing); stock/inventory of SIMs;
delivery tracking beyond a status.

**Explicitly out — bank balance:** the app **shows this facility as information only** (owner,
2026-07-31). It stores no amount, makes no arrangement, and produces no document or attestation
about anyone's funds. A traveller reads what the service is and contacts the team. Nothing in
this plan should ever grow into the app asserting that a person holds money they do not: that
would be a misrepresentation to whoever reads the resulting document. If this facility later
needs to be transactional, it needs its own plan and a hard look at what is actually being
arranged.

## 4. Design

### 4.1 One table for every service, not one per service

`service_request` carries `type` (`SIM_CARD` today) rather than a `sim_card_request` table. The
next facility that becomes requestable reuses the schema, the endpoints, the admin queue and the
status vocabulary; only the type-specific fields and copy differ. This is the same call made for
`email_verification` in [011](./011-transactional-email.md) §4.3 — one table per *channel or
domain*, keyed by purpose, instead of a near-identical schema per flow.

The type-specific fields are deliberately generic and nullable, because SIM is not the last
service: `arrivalDate`, `airport`, `detail` (the plan the traveller picked), `deliverTo`,
`contact`, `notes`. A service that needs a genuinely different shape gets its own columns then,
not now.

Rejected: a `sim_card_request` table (multiplies schemas per service, and the admin queue would
be one page per type); a JSON blob for type-specific fields (unqueryable, and the admin console
needs to sort a SIM queue by arrival date).

### 4.2 Status is the workflow

`REQUESTED → CONFIRMED → DELIVERED`, plus `CANCELLED` from either open state. The traveller
creates a request (`REQUESTED`) and may cancel while it is not yet delivered; the operator moves
it forward and may also cancel with a note. Terminal states are `DELIVERED` and `CANCELLED` —
a delivered SIM cannot be cancelled, and a cancelled request is not resurrected (the traveller
asks again, which is one click and leaves an honest record).

Ownership: a traveller reads and cancels **only their own** requests, enforced in the facade the
same way bookings are (404, never 403 — do not confirm someone else's request exists).

### 4.3 Endpoints

| Method | Path | Who | Purpose |
|---|---|---|---|
| `POST` | `/v1/service-requests` | user | Create (201) |
| `GET` | `/v1/service-requests/me` | user | My requests, newest first |
| `DELETE` | `/v1/service-requests/{id}` | user | Cancel my own (204) |
| `GET` | `/v1/admin/service-requests` | admin | Queue, with traveller identity + contact |
| `PATCH` | `/v1/admin/service-requests/{id}` | admin | Status and/or internal note |

Admin routes sit under `/v1/admin/**`, inheriting the existing ADMIN rule. As in
[012](./012-admin-chat-moderation.md) §4.3 the admin response is a **separate type** carrying the
traveller's name, email and phone — the customer response never contains another person's details.

### 4.4 The services shelf

`/services` after login: the SIM request form, the traveller's own requests with status, and an
informational card per additional facility. The nav gains **Services** for signed-in travellers.

The bank balance card is prose and a "talk to the team" pointer — no amount field, no form that
implies the company will arrange funds (§3).

## 5. Security & edge cases

- [x] A traveller cannot read or cancel another traveller's request (404).
- [x] Cancelling an already-`DELIVERED` request is refused.
- [x] Cancelling an already-`CANCELLED` request is refused (no silent double-cancel).
- [x] Admin status transitions are validated, not free-form string assignment.
- [x] The customer response carries no other person's identity; the admin one is a separate type.
- [x] Every admin endpoint refuses a USER token.
- [x] Free-text fields are length-bounded (notes 1000, the rest short) and trimmed.
- [x] `arrivalDate` must be sane — not in the past, not more than a year out, like bookings.
- [x] The bank balance card stores nothing and submits nothing.

## 6. Migration & rollout

One additive migration creating `service_request`; nothing to backfill and no existing table
touched. The API ships first (the customer page 404s on the endpoint until it does), then the
Next.js app and the console, in any order.

## 7. Acceptance

- [x] A signed-in traveller sees **Services** in the nav and can request a SIM with arrival date,
      airport, plan and delivery details.
- [x] The request appears in their list as `REQUESTED`, and they can cancel it.
- [x] The operator sees it in the console queue with the traveller's name and contact, and can
      move it to `CONFIRMED` then `DELIVERED`.
- [x] A delivered request can no longer be cancelled by either side.
- [x] Another traveller cannot see or cancel it.
- [x] The bank balance card is visible, explains the facility, and has no amount field.
- [x] Full suite green.

## 8. Test plan

`ServiceRequestControllerTest`: create returns 201 in `REQUESTED`; `/me` lists only mine;
a second traveller gets 404 on cancel; cancel works while open and is refused once `DELIVERED`
or already `CANCELLED`; invalid arrival dates are rejected; oversized notes are rejected.

`AdminServiceRequestControllerTest`: the queue carries traveller identity; each status transition
is applied; an illegal transition is refused; a USER token is refused on every admin route.

Manual: the `/services` page in a browser — request, see it listed, cancel — with the console
open alongside to watch the queue and move the status.
