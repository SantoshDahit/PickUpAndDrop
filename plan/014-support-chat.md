# 014 — Contact the team: direct traveller ↔ operator chat

**Status:** Implemented (2026-07-31) — 94/94 `./gradlew test` green (12 new), full round-trip verified in both apps via headless Chromium
**Depends on:** [001](./001-user-accounts.md) (accounts), [007](./007-customer-web.md) (customer pages), [012](./012-admin-chat-moderation.md) (staff-reply pattern), [013](./013-traveller-services.md) (services shelf)

## 1. Problem / Goal

Chat existed only **inside a travel group**, so reaching the company depended on having
companions. A traveller riding individually — the default until they pick a group, and permanently
for anyone who prefers to — had no way to contact anyone at all. 013 shipped a services page whose
copy says "get in touch" and "ask our team", with nothing to click.

Goal: one place a signed-in traveller can message the company and read replies, and an inbox where
the operator sees who is waiting.

## 2. Current state

`group_message` hangs off `travel_group` (002), and 012 added `staff` so operator replies read as
"Pickup & Drop team". Both require a group. `/trips` for an individual rider offers *Choose your
travel group* and *Cancel request* — nothing else.

## 3. Scope

**In:** `support_message` table; a traveller's own thread (read + send); an operator inbox with
unread counts and per-thread reply; a `/contact` page; **Contact** in the signed-in nav, footer and
on each active trip card; the *Message the team* call to action on the highlighted services card;
a **Support** page in the admin console.

**Out (deliberately):** email or push on a new message (the provider exists since 011 and this is
the obvious next use — deliberately deferred so this plan stays one thing; today both sides poll by
opening the page); realtime transport (chat stays pull-based per 002 §4.6); attachments; canned
replies/macros; assignment of a thread to a specific operator (one team, no need yet); per-message
delete or edit (same policy question 012 §3 left open); threads for signed-out visitors (a public
contact form is a different feature with its own spam problem — this needs a login).

## 4. Design

### 4.1 A thread is a traveller, not a row

There is no `support_thread` table. A thread is every `support_message` sharing `user_id` — the
traveller it belongs to — so a traveller has exactly one conversation and the customer endpoint
needs no id at all (`GET /v1/support/messages` is "mine"). A thread table would add a row whose
only content is a foreign key.

`author_id` is separate from `user_id`: a staff reply lands on the **traveller's** thread while
recording which operator wrote it. `staff` is fixed at write time for the same reason as
`group_message.staff` (012 §4.2) — role is mutable and must not relabel history.

Rejected: reusing `group_message` with a nullable group (its rows mean "said to a group"; the
privacy and membership rules differ, and every existing query would need a null guard);
a shared `conversation` + `participant` model (correct for many-to-many chat, unjustified for
one traveller and one team).

### 4.2 Read state

`read_at` per message, with `staff` selecting whose messages a read clears: a traveller opening
their thread clears the team's, an operator opening it clears the traveller's. The inbox counts
only **unread from the traveller**, because that is the number that means "someone is waiting".
This is what 012 §3 deliberately skipped for group chat — there a per-admin cursor was needed and
last-message-time was enough, whereas here the thread has exactly two sides, so a single flag works.

The inbox is one aggregate query (`count`, conditional `sum`, `max(createdAt)` grouped by user),
not a query per thread.

### 4.3 Endpoints

| Method | Path | Who | Purpose |
|---|---|---|---|
| `GET` | `/v1/support/messages` | user | My thread; clears the team's messages as read |
| `POST` | `/v1/support/messages` | user | Send (201) |
| `GET` | `/v1/admin/support` | admin | Inbox: who wrote, totals, unread, last activity |
| `GET` | `/v1/admin/support/{userId}/messages` | admin | One thread; clears theirs as read |
| `POST` | `/v1/admin/support/{userId}/messages` | admin | Reply as the team (201) |

Isolation falls out of the model: the customer route only ever reads the caller's own `userId`,
so there is no id to tamper with. Admin routes inherit the `/v1/admin/**` ADMIN rule.

Deleted accounts drop out of the inbox — same reasoning as the service-request queue (013 §4.2):
nobody left to reply to. Their messages stay in the table.

### 4.4 Where travellers find it

**Contact** in the signed-in nav, the footer account column, and a *Contact the team* button on
every active trip card — that last one is the point, since an individual rider's card is exactly
where the absence was felt. The services card's *Message the team* button lands here too.

Staff replies render as they do in group chat: team name, Official badge, accent-tinted bubble.

## 5. Security & edge cases

- [x] A traveller can only ever read and write their own thread (no id in the customer route).
- [x] A USER token is refused on every admin support route.
- [x] Body validated 1–1000 and trimmed, matching group chat (`MSG_BR_001`).
- [x] A staff reply shows the team name, never the operator's own, and is never "mine" to a traveller.
- [x] Unread counts only the traveller's messages, so the operator's own replies never look like work.
- [x] Soft-deleted accounts leave the inbox.
- [x] An unknown `userId` on the admin route is 404.
- [x] Support requires a login (401 without a token).

## 6. Migration & rollout

One additive migration creating `support_message`. Nothing to backfill.

Note for the next table added: `SupportMessage` extends `BaseCreateEntity`, which maps
**`created_by` as well as `created_at`** (convention 03). The first cut of the migration omitted
it and every support test failed with `Unknown column 'sm1_0.created_by'` — `group_message` carries
the same column for the same reason.

## 7. Acceptance

- [x] A traveller with no booking and no group can open `/contact` and send a message.
- [x] The operator sees it in the console with an unread count, opens it, and replies.
- [x] The reply appears on the traveller's page as the Pickup & Drop team with an Official badge.
- [x] Opening a thread clears the right side's unread count (inbox shows *answered*).
- [x] Threads are isolated between travellers.
- [x] Contact is reachable from the nav, the footer, a trip card, and the services card.
- [x] Full suite green.

## 8. Test plan

`SupportControllerTest`: a traveller with no group can send and read; threads are isolated;
a staff reply reads as the team and is not "mine"; blank and over-long bodies are refused;
support requires a login.

`AdminSupportControllerTest`: the inbox carries identity, totals and unread, and unread clears once
the thread is opened; a reply lands on the traveller's thread and creates none for the admin;
a traveller is refused on all three admin routes; an unknown user is 404; deleted accounts leave
the inbox.

Manual, both apps side by side: traveller sends → inbox shows *1 unread* → operator opens and
replies → inbox shows *answered* → traveller reloads and sees the team reply badged Official.
