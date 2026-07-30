# 011 — Transactional email: welcome, booking confirmation, password reset

**Status:** Implemented (2026-07-30) — 53/53 `./gradlew test` green, all three messages verified end-to-end against a live API + headless browser
**Provider:** Gmail SMTP (`smtp.gmail.com:587`, STARTTLS) via `spring-boot-starter-mail`
**Depends on:** [001](./001-user-accounts.md) (accounts), [002](./002-booking-and-group-matching.md) (bookings), [007](./007-customer-web.md) (customer pages), [009](./009-pricing.md) (fare shown in the receipt)

## 1. Problem / Goal

The product had **no outbound channel at all**. Three consequences, in the order they hurt:

1. A traveller who forgot their password was permanently locked out — the only recovery was the
   owner editing the database by hand. 001 deferred email flows, so this was by design, and the
   bill came due.
2. Booking produced no receipt. The one artefact of a ₩140,000–840,000 commitment lived only
   inside the web app, and travellers book from an airport lounge on a phone they may not reopen.
3. Nothing confirmed a signup, so a typo'd address failed silently and forever.

Goal: one mail provider wired once, with the three messages that carry their own weight.
Verification emails and marketing stay out (§3).

## 2. Current state

Before this plan: no `spring-boot-starter-mail`, no SMTP config, no `MAIL_*` secret anywhere.
The backlog in [README](./README.md) listed "email notifications" with no spec.

Adjacent conventions that decided the shape:

| Input | Effect on this design |
|---|---|
| `conventions/17-sms-verification` — `SmsSender` interface + one implementation | Copied wholesale as `EmailSender` + `JavaMailEmailSender` (§4.2) |
| `conventions/19-configuration` — secrets from env only, `@ConfigurationProperties` for the rest | `spring.mail.*` from env, `app.mail.*` as `MailProperties` |
| `conventions/18-deployment` — secrets travel base64 through SSH, validated before deploy | `MAIL_USERNAME`/`MAIL_PASSWORD` follow the `DB_PASSWORD` path exactly |
| 001 §3 "Out: email flows (verification, password reset)" | Password reset lands here; **verification stays out** |

Owner supplied a Gmail account and a 16-character app password, already stored as the
`MAIL_USERNAME` / `MAIL_PASSWORD` repository secrets (2026-07-29).

## 3. Scope

**In:** the mail provider (dependency, config, transport abstraction, async dispatch, failure
policy); **welcome on signup**; **booking confirmation** with route/date/party/flight/fare;
**password reset** — `email_verification` table, `POST /v1/auth/password/forgot`,
`POST /v1/auth/password/reset`, and the `/forgot-password` + `/reset-password` customer pages.

**Out (deliberately):** email *verification* of new addresses (would gate signup behind an inbox
round-trip — the product's whole promise is booking in minutes from an arrivals hall);
driver-assigned notices (no trigger exists until the ops workflow plan — the admin assigns a
driver today with no lifecycle event to hang mail on); admin/ops alerts; any marketing or digest
mail; SMS/push (17/16 exist as conventions, no product need yet); per-user notification
preferences (three transactional messages, all of them consequences of the user's own action —
a preferences table would be ceremony); bounce/complaint handling and a delivery log
(Gmail's own dashboard is enough at this volume; revisit with a real provider, §6);
i18n of email copy (site is English-only, tracked in the backlog).

## 4. Design

### 4.1 Failure policy — the decision everything else follows

**Mail is never allowed to fail the user's action.** A booking that succeeded must return 201
even if Gmail is unreachable. So: every send is `@Async` on a dedicated pool (`mail-`, 2–4
threads, `CallerRunsPolicy`), and `MailService` catches and logs every transport exception
rather than rethrowing. Callers get no success signal — by construction, since the action that
triggered the mail has already committed.

The inverse matters too: **mail must not go out for work that then rolls back.** Sends are
therefore deferred to after commit via `AfterCommitExecutor` (`TransactionSynchronization`;
runs immediately when no transaction is active). Without it, a booking rejected after the
`save` — or any later failure in the same transaction — still emails "your pickup is booked"
for a booking that does not exist. This was a real defect caught in testing, not a hypothetical.

Rejected: a `mail_outbox` table with a retry scheduler (correct for high volume, unjustified
ceremony for three messages at this traffic — the log is the audit trail for now, and a lost
welcome email costs nothing); sending inline on the request thread (an SMTP stall would become
a booking timeout); `@TransactionalEventListener(AFTER_COMMIT)` events (same guarantee, one more
indirection and a class per message type for no gain at three call sites).

### 4.2 Transport (Strategy, per convention 17)

```
AuthFacade / BookingFacade
  → AfterCommitExecutor.execute(…)        // defer past commit
      → MailService (@Async, swallows failures)
          → MailTemplates                 // HTML bodies
          → EmailSender                   // interface
              ├── JavaMailEmailSender     // @ConditionalOnProperty app.mail.enabled=true
              └── LoggingEmailSender      // default, matchIfMissing=true
```

`LoggingEmailSender` is the default so **local runs and the test suite can never reach a real
SMTP server** — a wrong profile logs instead of emailing strangers. `app.mail.enabled` is
`false` in `loc` (override with `MAIL_ENABLED=true`), `true` in `dev`/`pro`.

Bodies are plain Java string templates in `MailTemplates`, not Thymeleaf: mail HTML must survive
clients that discard stylesheets, so it is inline-styled and simple, and four messages don't
justify a second view engine in the dependency graph. All interpolated user input (name, flight
number, locations) is HTML-escaped.

### 4.3 Password reset

Stored in **`email_verification`** — convention 17's `sms_verification` applied to the email
channel, one table for every email flow rather than one table per flow (owner's call,
2026-07-30). Columns: `id`, nullable `user_id` FK, `contact`, `purpose`, `code_hash CHAR(64)`
UNIQUE, `status`, `verified_at`, `used_at`, `expires_at`, timestamps.

- `EmailPurpose` carries its own validity: `PASSWORD_RESET`/`VERIFY_ACCOUNT`/`JOIN` 60 min,
  `DELETE_ACCOUNT` 30. **Only `PASSWORD_RESET` is wired today**; the rest are the vocabulary for
  flows that will reuse the table.
- `EmailStatus` keeps 17's `PENDING → VERIFIED → USED` vocabulary, but a link-based flow is one
  step — possession of the link *is* the proof — so reset goes `PENDING → USED` and never sets
  `VERIFIED`. `isRedeemable()` therefore accepts `PENDING` only.
- **Purpose is checked on redemption**, so a code minted for one flow can't be spent on another
  (a future "verify your address" link must not double as a password reset). Two tests pin this.
- Retiring outstanding codes is scoped to `(user, purpose)`: issuing a reset code must not
  invalidate a live code from a different flow.
- `user_id` is nullable so a future flow can verify an address before an account exists;
  `contact` records the address as it was at issue time.
- The emailed code is 32 random bytes, URL-safe base64 (43 chars), from `SecureRandom`.
- **Only its SHA-256 is stored.** A leaked table yields no usable reset links.
- Single-use (`used_at`), one live code per user per purpose.
- `POST /password/forgot` **always returns 204**, whether or not the address is registered —
  the endpoint must not answer "does this person have an account?".
- `POST /password/reset` returns one error for unknown / expired / already-used tokens
  (`USR_BR_003`), for the same reason.

The no-oracle rule dictated an implementation detail worth recording: `forgotPassword` looks the
user up with `userService.getNullableActiveByEmail` (convention 07's `getNullableByXxx` shape),
**not** `getActiveByEmail` in a `try/catch`. A thrown `ApiException` inside the transaction marks
it rollback-only, so even a caught exception fails the commit — turning the unknown-address case
into a 500 and re-creating the very oracle the 204 exists to remove. Caught in testing;
regression-tested by a `@Transactional(NOT_SUPPORTED)` test, since a test-managed transaction
hides it.

Rejected: a purpose-specific `password_reset_token` table (built first, replaced on the owner's
call — one table per email flow would multiply near-identical schemas as verification flows
arrive); building 17's `sms_verification` and putting emails in it (the name would lie, `pin`
would hold a 43-char hash, and `VERIFIED` would never be reached — there is also no SMS provider
wired in this project, so there was nothing to consolidate *into*); a channel-generic
`verification` table covering SMS too (deferred: no SMS need today, and a `channel` column that
only ever holds one value is speculation — if SMS lands, it gets its own table per 17 or the two
merge then); JWT reset tokens (can't be revoked or made single-use without a table anyway);
emailing a short numeric code (fine for SMS, worse than a link in email); reusing the
`/v1/users/me/password` endpoint (that one requires a session and the current password — the
whole point here is that the user has neither).

### 4.4 Triggers and content

| Message | Trigger | Contents |
|---|---|---|
| Welcome | `AuthFacade.signup` | First name, what the product does, CTA → `/book` |
| Booking confirmation | `BookingFacade.create` | Route, travel date, party size, flight no (if given), **fare per person**, pay-cash-on-arrival note, CTA → `/trips` |
| Password reset | `AuthFacade.forgotPassword` | One-hour single-use link → `/reset-password?token=…`, plus "ignore this if it wasn't you" |

The fare is resolved with the same ladder rule as the site's calculator (highest tier at or below
the party size, per 009) — a receipt that disagreed with the booking page would be worse than no
fare at all. Verified equal to `priceFor()` in `lib/api.ts` for parties 1–6.

`BookingMail` is a flat record snapshotted **inside** the caller's transaction: the send runs on
another thread after commit, where lazy associations would otherwise fail.

### 4.5 Config and links

Email links need the **public site root**, not the API host: `app.mail.web-base-url`
(`WEB_BASE_URL`, defaulting to `https://pickupdrop.vercel.app`). `deploy-api.yml` passes
`MAIL_USERNAME`/`MAIL_PASSWORD` base64-encoded like every other secret, fails the deploy early
if either is absent (dev/pro resolve `spring.mail.*` from them, so a missing one crash-loops the
container), and **strips whitespace from the password** — Google displays app passwords in
four-character groups and SMTP wants the 16 characters unspaced. It warns when the length isn't 16.

### 4.6 Convention compliance, and the one open question

| Convention | How this plan complies |
|---|---|
| 03 entity | `EmailVerification` extends `BaseTimeEntity`, `char(36)` UUID PK assigned in the constructor, `@Enumerated(STRING)` for `purpose`/`status`, mutation through named methods (`updateStatus`, mirroring 17's `updateSmsVerificationStatus`) — no setters |
| 06 repository | Three files (`Repository` / `JpaRepository` / `RepositoryImpl`), no `QueryRepository` — matching `user`, `route`, `pricetier`, which likewise have no dynamic search. Bulk delete is `@Modifying(flushAutomatically, clearAutomatically) @Query` with `@Param`, the same idiom as `PriceTierJpaRepository` |
| 07 service/facade | Flow lives in `AuthFacade` (owns `@Transactional`, returns DTO/void); `EmailVerificationService` depends on its repository only and returns entities. Optional lookup named `getNullableActiveByEmail` per the naming table. Mail classes are `@Component` — they are infrastructure, not domain services with a repository (as `SolapiSmsSender` is in 17) |
| 09 exception | `PASSWORD_RESET_TOKEN_INVALID` = `USR_BR_003`, continuing the `USR_BR_*` sequence, message written for the end user |
| 17 sms-verification | Followed structurally for the email channel: `email_verification` mirrors `sms_verification` (contact + purpose + status + expiry, hashed code), `EmailPurpose`/`EmailStatus` mirror `SmsPurpose`/`SmsStatus`, and `EmailSender` mirrors `SmsSender` (interface + one live implementation, swap = add a class) |
| 19 configuration | Secrets from env only (`spring.mail.*`); non-secret shape in `MailProperties` (`@ConfigurationProperties("app.mail")`); per-profile enable flag |
| 20 flyway | `V20260730005223__santos_email_verification.sql` — KST timestamp, author prefix, snake_case description, standard header comment |
| 23 test | Integration tests via `IntegrationTestBase` (Testcontainers MySQL), `@MockitoBean` for the transport |

**One table per channel, not per flow (resolved 2026-07-30, owner).** The first cut of this plan
added a `password_reset_token` table. Replaced: `email_verification` holds every email flow,
keyed by `purpose`, exactly as 17's `sms_verification` holds every SMS flow. A per-flow table
would have meant a new near-identical schema for each verification feature that follows.

Note for whoever adds SMS: 17's table is the sibling of this one, not its replacement — an SMS
PIN is 4 plaintext digits with a genuine three-state flow (prove you hold the number, *then*
spend it), while an emailed link is a hashed 32-byte code where possession is the proof. If a
`channel` column ever looks better than two tables, merge then; a channel column with one value
today would be speculation.

## 5. Security & edge cases

- [x] No account enumeration: `/password/forgot` is 204 for any syntactically valid address;
      one error code for all bad-token cases.
- [x] Codes stored hashed, single-use, expiry per purpose, one live code per user per purpose;
      a code cannot be spent on a purpose other than the one it was issued for.
- [x] Reset does not log the user in — it redirects to `/login?reset=1`; possession of an inbox
      link shouldn't mint a session directly.
- [x] Credentials only ever from the environment; nothing mail-related committed.
- [x] Mail failure cannot fail signup, booking or reset-request.
- [x] Rollback cannot emit mail (§4.1).
- [x] User input in email bodies is HTML-escaped.
- [x] SMTP connect/read/write timeouts (5s) so a hung connection can't pin a mail thread.
- [x] Tests and `loc` cannot send real email (`LoggingEmailSender` is the default).
- [ ] **Known gap:** Gmail sending limits (~500/day) and no bounce handling — acceptable at
      current volume, revisit per §6.

## 6. Migration & rollout

One Flyway migration, `V20260730005223__santos_email_verification.sql` — a new table, no
change to existing rows, nothing to backfill. (It supersedes an unreleased
`password_reset_token` migration from the first cut; that file was deleted rather than
superseded-by-a-second-migration because it had only ever run against a local DB.) Rollout order matters only in that the API must be
deployed before the frontend links to `/forgot-password`; both ship together here.

Deploy needs `MAIL_USERNAME` + `MAIL_PASSWORD` present (owner set them 2026-07-29) and
optionally the `WEB_BASE_URL` repo variable. Rollback is redeploying the previous image: the new
table simply goes unused, and no other feature reads it.

When volume or deliverability justifies it, swap Gmail for a transactional provider
(SES/Postmark/Resend): only `JavaMailEmailSender` changes if it keeps SMTP, or one new
`EmailSender` implementation if the provider is API-based. That is the entire point of §4.2.

## 7. Acceptance

- [x] Signing up sends a welcome email; signup still succeeds when SMTP is down.
- [x] Creating a booking sends a receipt whose fare matches the site's calculator for the same
      party size; a rejected booking (past date) sends nothing.
- [x] `/password/forgot` returns 204 for both a registered and an unregistered address, and
      emails a link only in the first case.
- [x] The emailed link sets a new password; the new password logs in and the old one no longer does.
- [x] Replaying a used token, and using a token superseded by a newer one, both fail `USR_BR_003`.
- [x] `/reset-password` with no token explains itself instead of showing a dead form.
- [x] Mismatched confirmation shows "Those passwords don't match." rather than failing silently.
- [x] Local runs and the test suite send no real email.
- [x] Full suite green (53 tests).

## 8. Test plan

`PasswordResetControllerTest` (11 tests, `EmailSender` replaced with `@MockitoBean` so nothing
leaves the JVM): the 204-for-both-addresses no-oracle rule, the emailed body carrying a
`/reset-password?token=` link, no send for an unknown address, token redemption changing the
password, replay rejection, supersede rejection, unknown-token rejection, short-password validation, and
two that pin the shared table's isolation: a code issued for `VERIFY_ACCOUNT` is refused by the
reset endpoint, and issuing one purpose's code leaves another purpose's live code intact.

Two tests deliberately run `@Transactional(propagation = NOT_SUPPORTED)`, because the behaviour
under test only exists at commit time: the rollback-only 500 of §4.3 passes inside a test
transaction, and after-commit dispatch never fires in one. `rolledBackRequestSendsNoMail` asserts
the opposite guarantee from inside a rolled-back transaction.

Manually verified against a live API (`loc`, mail logged rather than sent) plus headless Chromium
on the real pages: signup → welcome; booking → receipt with correct fare; forgot → link → reset →
login with the new password → old password rejected. Gmail credentials were validated by an SMTP
`STARTTLS` + `AUTH` handshake that sends no message.
