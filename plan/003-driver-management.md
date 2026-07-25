# 003 — Taxi drivers: roster, admin CRUD, ride assignment

**Status:** Implemented (2026-07-26) — 23/23 tests green (Testcontainers) + live curl pass against dev MySQL
**Stack:** REST API per `springboot/conventions/` (see [000](./000-stack-migration.md))
**Depends on:** [001](./001-user-accounts.md) (auth/roles), [002](./002-booking-and-group-matching.md) (bookings, travel groups)

## 1. Problem / Goal

Bookings and groups exist, but nobody drives anyone anywhere. The admin needs a **driver roster** (who drives for us, in what vehicle, how many seats) and a way to **assign a driver to a ride** — a travel group, or an individual booking. Once assigned, travellers must see who is picking them up: at arrivals you're looking for a face, a name, and a plate number.

User stories:

- As the admin I register drivers (name, phone, license, vehicle, seats), edit them, deactivate ones who stop driving, and search the roster.
- As the admin I assign a driver to a travel group (or an individual booking), and can unassign/replace them.
- As a traveller with an assigned driver I see the driver's name, phone, vehicle model, plate, and seat count on my group page / booking.
- Drivers do **not** log in — this is a roster the admin manages. A driver portal is its own future plan.

## 2. Current state

- Spring API has users/auth (001) and bookings/groups/chat (002). No driver concept.
- The frozen Next.js prototype had a `drivers` table (`lib/db.ts`: name, phone, license_no, owns_vehicle, vehicle, seats, active) and `trip_requests.driver_id` — assignment existed but was invisible to travellers. Behaviour to keep: the field set; behaviour to improve: travellers see their assigned driver.
- `plan/000` parity checklist lists "drivers CRUD" and "driver assignment" — this plan delivers both.

## 3. Scope

**In:** `driver` domain (entity → controller per conventions quickstart), admin CRUD + paged QueryDSL search, assignment/unassignment of a driver to a travel group and to an individual booking, driver visibility in traveller-facing group/booking responses, seat-capacity validation at assignment time.

**Out (deliberately):** driver login/portal/app (future plan — would introduce a DRIVER role), driver fees & settlement (belongs with pricing, 004+), automatic driver scheduling/dispatch optimisation, per-day double-booking prevention (vans legitimately do several airport runs a day — revisit with real ops data), driver documents/photo upload (needs S3, same blocker as avatars), notifications to drivers (no channel exists — they're reached by phone/KakaoTalk for now).

## 4. Design

### 4.1 Where does an assignment live? — the decision

A ride unit is **a travel group** for grouped travellers and **the booking itself** for individual riders. Options considered:

| Option | Verdict |
|---|---|
| **A. `driver_id` on `travel_group` + `driver_id` on `booking`** (used only when `group_id` is null) | ✅ Two nullable FKs, zero new concepts, matches how the admin actually thinks ("put driver X on this group") |
| B. A `ride`/`dispatch` entity unifying both | ❌ Right shape *eventually* (fees, settlement, driver schedules hang off it) — but premature now; introduce it in the pricing/settlement plan when it has data to carry |
| C. Individual bookings get an implicit group-of-one so only groups are assignable | ❌ Corrupts 002's clean "null group = individual" semantics and pollutes matching queries |

**Decision: A**, with B explicitly earmarked as the refactor target when settlement arrives. The invariant "a booking's effective driver = its group's driver when grouped, else its own `driver_id`" lives in one place (`Booking.effectiveDriver()`-style accessor via facade assembly, not duplicated logic).

### 4.2 Schema (Flyway `V{timestamp}__santos_add_driver.sql`)

```sql
CREATE TABLE driver (
  id           CHAR(36)     NOT NULL,
  name         VARCHAR(100) NOT NULL,
  phone        VARCHAR(30)  NULL,
  license_no   VARCHAR(50)  NULL,
  owns_vehicle BIT(1)       NOT NULL DEFAULT b'1',
  vehicle      VARCHAR(100) NULL,       -- model, e.g. 'Hyundai Staria'
  plate_no     VARCHAR(20)  NULL,       -- what travellers look for at arrivals
  seats        INT          NOT NULL DEFAULT 4,
  status       VARCHAR(20)  NOT NULL,   -- ACTIVE | INACTIVE
  created_at   DATETIME(6)  NOT NULL,
  updated_at   DATETIME(6)  NOT NULL,
  deleted_at   DATETIME(6)  NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE travel_group ADD COLUMN driver_id CHAR(36) NULL,
  ADD CONSTRAINT fk_travel_group_driver FOREIGN KEY (driver_id) REFERENCES driver (id);
ALTER TABLE booking ADD COLUMN driver_id CHAR(36) NULL,
  ADD CONSTRAINT fk_booking_driver FOREIGN KEY (driver_id) REFERENCES driver (id);
```

Notes: `plate_no` is new vs. the prototype (travellers need it); `status` enum + `deleted_at` follow the soft-delete convention (11) — `INACTIVE` = "not driving these days, keep on file", soft-delete = "remove from roster". `seats` = passenger seats available (not counting driver).

### 4.3 Layers (conventions quickstart, applied)

- `enums/DriverStatus` (`ACTIVE | INACTIVE`).
- `entity/Driver` extends `BaseFullTimeEntity`; UUID ctor; `update(...)` null-safe multi-field; `TravelGroup.assignDriver(Driver)` / `Booking.assignDriver(Driver)` (+ `unassign`).
- `dto/DriverDto`: `PostRequest` (name required; seats 1–10 default 4), `PatchRequest` (all-nullable), `Response`, `SummaryResponse`, `SearchRequest` (name contains, statusList, minSeats). Plus a **traveller-facing `DriverDto.PublicResponse`** — name, phone, vehicle, plateNo, seats only; never license/status/audit fields.
- `mapper/DriverMapper` (BaseMapper; registers Response/SummaryResponse/PublicResponse).
- `repository/driver/` — 4 files; `DriverQueryRepository.search` with dynamic conditions per convention 06.
- `service/DriverService` — `getById` (404 `DRIVER_IS_NOT_FOUND`, non-deleted), `getActiveById` (also rejects INACTIVE for assignment), `save`, `search`.
- `service/DriverFacade` — admin CRUD orchestration **and assignment** (it combines DriverService + TravelGroupService/BookingService, which is exactly what facades are for).

### 4.4 API

| Endpoint | Behaviour |
|---|---|
| `POST /v1/admin/drivers` | Create (201) |
| `GET /v1/admin/drivers/search` | Paged QueryDSL search (name/status/minSeats) |
| `GET /v1/admin/drivers/{id}` | Detail |
| `PATCH /v1/admin/drivers/{id}` | Partial update (null = keep) |
| `PATCH /v1/admin/drivers/{id}/status` | ACTIVE ↔ INACTIVE |
| `DELETE /v1/admin/drivers/{id}` | Soft delete (204). **Refused** (`DRIVER_HAS_UPCOMING_RIDES`) while assigned to any group/booking whose travel date is today or later — unassign first |
| `PUT /v1/admin/groups/{groupId}/driver` | Assign `{driverId}` to a group (idempotent replace) |
| `DELETE /v1/admin/groups/{groupId}/driver` | Unassign (204) |
| `PUT /v1/admin/bookings/{bookingId}/driver` | Assign to an **individual** booking; grouped bookings are refused (`BOOKING_IS_GROUPED` — assign via the group) |
| `DELETE /v1/admin/bookings/{bookingId}/driver` | Unassign (204) |

All under `/v1/admin/**` → existing ADMIN route rule; no new security config.

**Traveller surface (no new endpoints):** `TravelGroupDto.Response` gains `driver: PublicResponse|null`; `BookingDto.Response`/`SummaryResponse` gain the *effective* driver (own for individual, group's when grouped). `GET /v1/bookings/me` therefore answers "who's picking me up?" in one call.

### 4.5 Assignment rules

1. Only `ACTIVE`, non-deleted drivers are assignable (`DRIVER_IS_NOT_ASSIGNABLE`).
2. **Seat check:** sum of the group's active members' `party_size` (or the booking's `party_size`) must fit `driver.seats`, else `DRIVER_SEATS_INSUFFICIENT` (400). Checked again if it's a replace.
3. Assigning to a `CLOSED` group or a cancelled booking is refused.
4. Replacing is a plain `PUT` overwrite — no confirmation workflow (admin tool).
5. A member joining a **full-ish** group after assignment can exceed seats? No: matching (002) caps at `MAX_SEATS = 6`; assignment additionally validates against the *specific* vehicle. A driver with 4 seats on a group that later grows to 5 is possible — group membership changes **do not** auto-unassign; instead the admin search (004 ops work) will surface it. Documented as a known gap, acceptable while one human runs ops.
6. New ErrorCodes (domain `DRV`, plus two `BKG`): `DRIVER_IS_NOT_FOUND(DRV_NF_001)`, `DRIVER_IS_NOT_ASSIGNABLE(DRV_BR_001)`, `DRIVER_SEATS_INSUFFICIENT(DRV_BR_002)`, `DRIVER_HAS_UPCOMING_RIDES(DRV_BR_003)`, `BOOKING_IS_GROUPED(BKG_BR_003)`.

### 4.6 Privacy

- Travellers see the driver's **name, phone, vehicle, plate, seats** — operational necessity (finding the van, calling when lost). License number and roster status are admin-only (`PublicResponse` is a separate DTO, not a filtered view — impossible to leak by accident).
- Drivers see nothing (they have no accounts); the admin relays pickup details by phone. The future driver-portal plan owns that direction.

## 5. Security & edge cases

- [ ] Every endpoint is under `/v1/admin/**` except the enriched traveller responses; assignment facades re-verify entity states, not just route roles.
- [ ] Soft-deleted / INACTIVE drivers: never assignable, never listed in default search (statusList filter can include INACTIVE explicitly; deleted are gone).
- [ ] Deleting a driver with only **past** rides: allowed — history keeps the FK, responses show the name from the (soft-deleted) row.
- [ ] Unassign on group leave/cancel: **not** automatic (group persists; driver stays until admin acts). Group closing (last member leaves) keeps `driver_id` for history.
- [ ] Effective-driver resolution never leaks another rider's booking: it reads only the caller's own bookings / groups they're a member of (002 rules unchanged).
- [ ] `PATCH` with all-null body is a no-op 200, per DTO convention (null = keep).
- [ ] Seat check uses live member data inside the assignment transaction.

## 6. Migration & rollout

Additive only (new table + two nullable FKs); existing rows unaffected; no data migration. Timestamp-named migration per convention 20. No config changes.

## 7. Acceptance criteria

- [ ] Admin creates a driver (Hyundai Staria, 6 seats, plate `12가3456`) → appears in search; PATCH updates phone only; status toggle works; regular user gets 403 on all of it.
- [ ] Assign that driver to a 2-member group → both members' `GET /v1/groups/{id}` and `GET /v1/bookings/me` show name/phone/vehicle/plate; license_no appears nowhere in traveller responses.
- [ ] Assign a 4-seat driver to a group holding 5 seats → 400 `DRV_BR_002`.
- [ ] Assign to an individual booking works; assigning directly to a *grouped* booking → 400 `BKG_BR_003`.
- [ ] Unassign → traveller responses show no driver; replace via second PUT works.
- [ ] INACTIVE and soft-deleted drivers are refused for assignment (`DRV_BR_001`).
- [ ] Deleting a driver assigned to a future-dated group → 400 `DRV_BR_003`; after unassigning, delete succeeds; a driver with only past rides deletes fine and past responses still render.
- [ ] All existing 001/002 tests stay green; migration applies once on the current dev DB.

## 8. Test plan

- **Integration (Testcontainers, extends `IntegrationTestBase`):** `DriverAdminControllerTest` — CRUD + search + role gate + status transitions + delete-guard; `DriverAssignmentControllerTest` — group assign/replace/unassign, individual assign, grouped-booking refusal, seat validation, INACTIVE refusal, traveller visibility (member sees `PublicResponse`, stranger still 404 on group).
- **`TestDataHelper`** gains `createDriver(seats)`.
- **Manual pass** via curl against `bootRun` mirroring the acceptance list.
