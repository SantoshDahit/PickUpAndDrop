# 009 — Realistic pricing: cost analysis and fare tables

**Status:** Implemented (2026-07-26) — **Revised same day (owner):** value-based single table, see §0
**Displayed fares:** ₩150,000 solo · ₩140,000/person for 2+ (all routes)

## 0. Revision — owner's pricing directive (2026-07-26)

The cost analysis below stays as the **internal business reference**; customer pricing is
decoupled from it. Whether a party rides bus, train, or hired car is an operational choice,
not a price input. Owner's formula: **₩150,000 per person, minus ₩10,000 × n off the total
from 2 people** — which resolves to a flat **₩140,000/person for any party of 2–6**:

| Party | 1 | 2 | 3 | 4 | 5 | 6 |
|---|---|---|---|---|---|---|
| **₩/person** | 150,000 | 140,000 | 140,000 | 140,000 | 140,000 | 140,000 |
| Total | 150,000 | 280,000 | 420,000 | 560,000 | 700,000 | 840,000 |

Worked example (owner): 3 people → ₩420,000; hire the ₩200,000 car; the rest is profit.
Profit vs the internal cost model: 2 pax +124k (Seoul) / +40k (intercity); 3 pax +252k/+140k;
4 pax +290k/+240k; 6 pax +570k/+520k. The one thin case — solo outside Seoul — is handled
operationally (ticket-and-boarding assistance rather than full-day escort), owner-confirmed
not loss-making. The earlier two-zone discount ladder is superseded; per-route tuning stays
available via the admin Routes page.

Implementation note: tiers are stored as two rows per route (size 1 = 150,000; size 2 =
140,000 applying to 2+), which the site renders naturally as "1 person / 2 people or more".
**Depends on:** price tiers (008-era `price_tier` table), routes seed, admin Routes page (tier PATCH)

## 1. Cost inputs (owner, 2026-07-26)

1. A pickup consumes a **greeter's full day** ("one person has to take a day off") — a solo
   pickup can never sell below **₩150,000 including their ticket**.
2. **Seoul metro**: individuals can be escorted by bus/train; **parties over 3 need a hired
   taxi/van ≈ ₩150,000**.
3. **Outside Seoul**: intercity bus ≈ **₩40,000 per person** (average), or a hired car ≈
   **₩200,000** (average).

## 2. Cost model

| Parameter | Value | Basis |
|---|---|---|
| Greeter day (labour) `L` | ₩120,000 | Backed out of the ₩150,000 solo floor: L + 2 transit tickets ≈ ₩144k |
| Seoul transit ticket | ₩12,000/rider | AREX express / limousine bus, airport→city |
| Seoul van (parties ≥ 4) | ₩150,000 | Owner input |
| Intercity bus ticket | ₩40,000/rider | Owner input (nationwide average) |
| Hired car outside Seoul (≥ 4) | ₩200,000 | Owner input (average) |

Assumptions: the greeter **accompanies** the party door-to-door (that is the product), so
transit legs cost `(party + 1)` tickets; hired vehicles come with a driver so no extra ticket;
grouped travellers on one booking-week ride share one greeter and one vehicle. Two pricing
zones for now — **Zone A: Seoul metro** (Seoul, Incheon City), **Zone B: everywhere else** —
using the owner's national averages; per-route tuning happens any time through the admin
Routes page (tier PATCH), e.g. Busan will likely need a Zone B premium later.

Cost per party: Zone A = `L + 12k×(n+1)` for n ≤ 3, `L + 150k` for n ≥ 4.
Zone B = `L + 40k×(n+1)` for n ≤ 3, `L + 200k` for n ≥ 4.

## 3. Fare tables (per person, cash on arrival)

### Zone A — Seoul metro (Seoul, Incheon City)

| Party | Mode | Cost (total) | **Price/person** | Total | Margin |
|---|---|---|---|---|---|
| 1 | transit escort | 144,000 | **₩150,000** | 150,000 | +6k (4%) |
| 2 | transit escort | 156,000 | **₩90,000** | 180,000 | +24k (13%) |
| 3 | transit escort | 168,000 | **₩75,000** | 225,000 | +57k (25%) |
| 4 | private van | 270,000 | **₩72,000** | 288,000 | +18k (6%) |
| 5 | private van | 270,000 | **₩62,000** | 310,000 | +40k (13%) |
| 6 | private van | 270,000 | **₩55,000** | 330,000 | +60k (18%) |

### Zone B — rest of Korea (Daejeon, Suwon, Busan, …)

| Party | Mode | Cost (total) | **Price/person** | Total | Margin |
|---|---|---|---|---|---|
| 1 | bus escort | 200,000 | **₩210,000** | 210,000 | +10k (5%) |
| 2 | bus escort | 240,000 | **₩130,000** | 260,000 | +20k (8%) |
| 3 | bus escort | 280,000 | **₩105,000** | 315,000 | +35k (11%) |
| 4 | hired car | 320,000 | **₩95,000** | 380,000 | +60k (16%) |
| 5 | hired car | 320,000 | **₩80,000** | 400,000 | +80k (20%) |
| 6 | hired car | 320,000 | **₩70,000** | 420,000 | +100k (24%) |

Properties, by design: per-person price falls monotonically with party size (the product's
"cheaper together" promise stays true); **solo margins are deliberately at the owner's floor**
(≈4–5%) so the economics themselves push travellers toward grouping, where margins reach
13–25%; the 3→4 step buys a private vehicle, which is why the per-person drop flattens there.

Rejected: pricing solo below ₩150,000 to look competitive (sells the greeter's day at a loss);
one nationwide table (Seoul transit reality is 3× cheaper than intercity); distance-per-km
formula (false precision — the admin tunes per route with real quotes instead).

## 4. Rollout

Migration replaces all `price_tier` rows with the two-zone tables (Zone A: Seoul, Incheon City;
Zone B: the rest). Every price display on the website reads `/v1/routes` tiers at request time —
home fare calculator, featured fare table, route cards ("from ₩…"), booking fare panel, trip
ticket stubs — so no frontend copy changes are required beyond verification. Old prototype
fares (₩12,500–40,000) disappear everywhere at once.

## 5. Acceptance

- [ ] `/v1/routes`: Seoul & Incheon City carry Zone A tiers; all other routes Zone B.
- [ ] Home calculator: Seoul solo shows ₩150,000; 6 people ₩55,000/pp; "bring more people"
      nudge still appears and is truthful.
- [ ] Booking fare panel and trip stubs show the same numbers.
- [ ] Admin can still PATCH a single route's tiers without touching others.
- [ ] Full test suite green.
