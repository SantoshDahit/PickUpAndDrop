# 005 — Driver accounts: DRIVER role, self-service profile, my rides

**Status:** Implemented (2026-07-26) — 27/27 tests green + live pass (Kim Cheolsu login, profile patch, rides with Minji & Jonas)
**Depends on:** [003](./003-driver-management.md) (driver roster + assignment)

## 1. Problem / Goal

Drivers exist only as roster rows the admin manages (003); the admin relays every pickup detail by phone. Give drivers a login so they can keep their own profile current and see the rides assigned to them — who lands when, how many people, which flight, how to reach the passenger.

User stories:

- As the admin I create a login for a roster driver (email + initial password) — drivers do **not** self-signup; the roster stays admin-curated.
- As a driver I log in, see my profile, and update the fields that are mine to change (phone, vehicle model, plate).
- As a driver I see my upcoming assigned rides: route, landing day, party details (first names, party size, flight no, contact), and total seats needed.
- As a driver I cannot see anything else — not other drivers, not unassigned bookings, not admin screens.

## 2. Current state

- `Role` enum: USER | ADMIN. Auth issues JWT with `ROLE_{role}`.
- `driver` table (003) has no link to `user`. Assignment lives on `travel_group.driver_id` / `booking.driver_id`.
- Passenger `contact` is admin/driver-facing data by design (002 §4.5) but no driver can read it yet.

## 3. Scope

**In:** `DRIVER` role, admin-created driver logins linked 1:1 to roster rows, `/v1/drivers/me` profile GET/PATCH, `/v1/drivers/me/rides` (upcoming assigned rides with passenger details), route rule for `/v1/drivers/**`.

**Out:** driver self-signup, password reset flows, ride status updates by the driver ("picked up", "completed" — belongs with the ops workflow plan), notifications, earnings/settlement, a driver web app (they can consume the API from the future portal; endpoints first).

## 4. Design

### 4.1 Account linkage — the decision

`driver.user_id CHAR(36) NULL UNIQUE → user.id`. One roster row, at most one login. Rejected: making Driver a subtype of User (roster rows exist before/without logins — Kim Cheolsu drives fine unlinked); separate driver-credentials table (there is exactly one auth system, `user`).

Admin creates the login: `POST /v1/admin/drivers/{id}/account {email, password}` → creates a `user` row with `role = DRIVER`, links it. Refused if the driver already has one (`DRIVER_ACCOUNT_EXISTS`) or the email is taken. Deactivating logins reuses 001 semantics (soft-deleted user can't log in) — the roster row itself stays.

### 4.2 What a driver may edit

Phone, vehicle model, plate — the facts only they know day-to-day. **Not** editable: name/license (identity, admin verifies), seats (capacity feeds assignment validation — admin-only), status (roster state is dispatch's call).

### 4.3 My rides

One list, soonest first, travel date ≥ today, from both assignment paths:

- groups where `travel_group.driver_id = my driver` → all active member bookings rolled up (first names, party sizes, flights, contacts, agreed/spread dates, total seats)
- individual bookings where `booking.driver_id = my driver`

Drivers see passenger **first name, party size, travel date, flight no, contact** — contact is the point (calling a lost passenger); email and intro stay private to the group.

### 4.4 API

| Endpoint | Who | Behaviour |
|---|---|---|
| `POST /v1/admin/drivers/{id}/account` | ADMIN | Create + link login (201). `DRIVER_ACCOUNT_EXISTS` / `USER_DUPLICATE_EMAIL` |
| `GET /v1/drivers/me` | DRIVER | My roster profile |
| `PATCH /v1/drivers/me` | DRIVER | phone / vehicle / plateNo only |
| `GET /v1/drivers/me/rides` | DRIVER | Upcoming rides as §4.3 |

`SecurityConfig`: `/v1/drivers/**` → `hasRole("DRIVER")`. New ErrorCodes: `DRIVER_ACCOUNT_EXISTS(DRV_BR_004)`, `DRIVER_PROFILE_NOT_LINKED(DRV_NF_002)` (a DRIVER-role token whose driver row vanished).

### 4.5 Layers

Migration `ALTER TABLE driver ADD user_id ... UNIQUE`; `Role.DRIVER`; `Driver.linkAccount(User)` + `updateOwnProfile(...)`; `DriverDto.AccountPostRequest / MePatchRequest / RideResponse (+PassengerResponse)`; repo `findByUserId`, group/booking `findAllByDriverId...`; `DriverFacade.createAccount / getMyProfile / updateMyProfile / getMyRides`; `DriverPortalController`.

## 5. Security & edge cases

- [ ] `/v1/drivers/me*` resolves the driver **from the token's user id**, never from a parameter (IDOR-proof by construction); USER/ADMIN tokens get 403 by route rule.
- [ ] Creating an account for a soft-deleted driver → 404; duplicate link → `DRV_BR_004`; email uniqueness races handled like signup (UNIQUE + exception mapping).
- [ ] Rides list never includes cancelled bookings, past dates, or unassigned work; an unassigned driver sees an empty list, not an error.
- [ ] Unlinking is not a feature (delete the login user via admin instead — future admin-users work); a dangling DRIVER token whose roster row was soft-deleted gets `DRV_NF_002`.
- [ ] Driver PATCH cannot touch seats/status/license/name even if posted (DTO simply has no such fields).

## 6. Migration & rollout

Additive migration only. Existing drivers stay unlinked and unaffected; the admin links them one by one. No config change beyond the route rule.

## 7. Acceptance criteria

- [ ] Admin creates a login for Kim Cheolsu → Kim logs in via `/v1/auth/login`, `role: DRIVER`.
- [ ] `GET /v1/drivers/me` shows his roster row; PATCH updates phone; seats/status unchanged even if posted.
- [ ] With his group assignment, `GET /v1/drivers/me/rides` lists the ride with Minji's and Jonas's first names, party sizes and contacts, and 3 total seats.
- [ ] A USER token gets 403 on `/v1/drivers/me`; a DRIVER token gets 403 on `/v1/admin/**` and 200 on `/v1/users/me` is **not** required (driver logins are drivers, not travellers — they may still use shared endpoints like logout-free JWT expiry).
- [ ] Second account for the same driver → `DRV_BR_004`; taken email → `USR_BR_001`.
- [ ] All existing tests stay green; migration applies once.

## 8. Test plan

`DriverPortalControllerTest` (Testcontainers): account creation + duplicate/link guards, DRIVER login, profile GET/PATCH field-guard, rides list with grouped + individual assignments and passenger contact visibility, role gates both directions. `TestDataHelper.createDriverAccount(driver)`.
