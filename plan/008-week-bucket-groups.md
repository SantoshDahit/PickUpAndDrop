# 008 — Landing-week groups: book first, then choose your group

**Status:** Implemented (2026-07-26) — 41/41 backend tests green + 9/9 two-traveller browser checks
**Supersedes:** the rolling 7-day auto-matching of [002](./002-booking-and-group-matching.md) §4.1
**Depends on:** 002/006/007

## 1. Problem / Goal (owner direction)

Auto-matching put travellers into a group at booking time, silently. The owner wants:

1. **Book first, group second** — a user reaches groups only through their trip.
2. **Explicit choice** — after booking we *suggest* groups and the user selects (or starts) one.
3. **Hard chat boundaries** — a group's chat is only for its members, and membership itself is
   bounded by a calendar landing week; you can't sit in a Sep 1–7 chat while landing Sep 20.
4. **Date changes re-suggest** — moving your arrival into another week moves you out of the group
   and offers the right one.
5. **Fixed week buckets** — divide each month into **5 weeks** (months have 29–31 days):
   W1 = 1–7, W2 = 8–14, W3 = 15–21, W4 = 22–28, W5 = 29–end (1–3 days).

## 2. The bucket — decision

`week_bucket = "YYYY-MM-W#"` with `# = min(4, (dayOfMonth-1)/7) + 1`. Deterministic, needs no
lookup table, and the label writes itself ("Landing week Sep 8–14").

Rejected: ISO weeks (Mon–Sun) — they straddle month boundaries and don't match how travellers
talk ("mid-September"); rolling ±7-day window (the old model) — non-deterministic group
boundaries made "which chat am I in and why" unexplainable, and drift let members 13 days apart
share a van chat. The W5 stub (29–31) is accepted: those 2–3 days form small buckets, and the
admin can always publish an official ride bridging demand.

## 3. Rules

1. Every group carries a `week_bucket`, set at creation (organic: from the founding booking's
   date; published rides: from the target date).
2. **A booking may only join a group when `bucket(booking.travelDate) == group.week_bucket`**
   (and same route, OPEN, seats fit). This replaces the span rule everywhere — including
   published-ride joins at booking time.
3. **Booking creation no longer auto-assigns a group** (`matchPref` stays as recorded intent).
   Exception: an explicit `groupId` at booking time (the /book published-ride join) still works,
   bucket-validated.
4. **Suggestions**: `GET /v1/bookings/{id}/group-suggestions` (booking owner only) → the
   bucket's date range + candidate groups on that route+bucket with seats left: member count,
   seats left, current date span, official-ride flag. **No personal data**; names appear only
   after joining. The owner-only + same-bucket scoping is the privacy boundary that lets organic
   groups be listed here where 002 forbade a global browse.
5. **Select**: `PUT /v1/bookings/{id}/group {groupId}` joins (switching groups is allowed —
   it's a selection); `{groupId: null}` **creates** the bucket group and joins it — that's
   "where groups are created" now. `DELETE /v1/groups/{id}/members/me` (leave) unchanged.
6. **Date change**: `PATCH /v1/bookings/{id}` with a date in a different bucket than the current
   group **detaches the booking** (membership must stay inside the boundary) and the UI
   immediately routes the user to the new week's suggestions. Same-bucket changes are free.
7. Published rides (006) unchanged in spirit: browsable pre-booking, join at booking time or
   from suggestions; they surface in suggestions flagged "official". The old "organic matching
   seeds into published rides" dies with auto-matching.
8. Chat/members/leave/driver assignment: unchanged — membership was already the gate; the
   bucket now bounds membership itself.

## 4. Changes

**Backend** — migration (`travel_group.week_bucket` + backfill from target/founding dates);
`WeekBucket` domain util (`of/startOf/endOf`); GroupMatcher's span logic removed; facade:
`suggestGroups`, `selectGroup`, bucket-validated `create(groupId)` and cross-bucket detach in
`updateTravelDate`; `TravelGroupDto.SuggestionsResponse`; `GroupView` gains `weekStart/weekEnd`.
Error `GRP_BR_002` message becomes "Your landing day falls outside this group's landing week."

**Next.js app** — `/trips`: ungrouped active bookings get "Choose your travel group" →
new page `/trips/[id]/group` (design language kept): week label, suggestion cards
("3 travellers · 3 seats free · landing Sep 8–14", official rides marked), "Start a new group";
date change from the group page redirects there with an explainer when it crossed weeks;
group page shows its landing week.

**Admin console** — no structural change (publishing already sets a target date → bucket).

## 5. Acceptance criteria

- [ ] Booking (no groupId) creates an ungrouped trip; `/trips` shows "Choose your travel group".
- [ ] Suggestions list only same-route groups of the booking's landing week; a group founded for
      Sep 3 never appears for a Sep 20 booking; capacity-full groups drop out.
- [ ] "Start a new group" creates + joins; a second same-week booker sees that group suggested
      and joins it; chat works; a third user in another week sees an empty list + start option.
- [ ] Published ride join (from /book or suggestions) works only when the chosen landing day is
      inside the ride's week; outside → `GRP_BR_002`.
- [ ] Date change within the week: nothing happens to membership. Across weeks: booking leaves
      the group, group page access turns 404, user lands on the new week's suggestions.
- [ ] Switching between two same-week groups works; seats free up behind you.
- [ ] Suggestion endpoint is owner-only (someone else's bookingId → 404) and shows no names.
- [ ] W5 correctness: Jan 29–31 forms its own bucket; Feb 1 does not join it.
- [ ] Full suite green.

## 6. Test plan

Rewrite matching tests: `GroupSelectionControllerTest` (suggest/select/create/switch/boundaries/
owner-gate/W5 edge), booking-creation tests updated (no auto-group), published-ride tests updated
(bucket join), helper `TestDataHelper.groupableDate()` avoids bucket-edge flakiness. Headless
two-traveller pass on :3000 covering the select-and-chat journey and the date-change re-suggest.
