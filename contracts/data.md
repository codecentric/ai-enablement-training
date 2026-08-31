---
kunde: adevinta
stand: 2026-08-31
status: Entwurf — Leitprojekt für das AI-Enablement-Training
zweck: Warehouse-Kontrakt Kiezmarkt für die Rollengruppe Data
updated: 2026-08-31
---

# Kiezmarkt — the warehouse contract

Scope: the **Marketplace report** surface from `docs/project/domain.md`. Python on
Databricks, transformations in dbt. Everything below has to agree with `domain.md`
field for field; where the two disagree, `domain.md` wins and the disagreement is a
finding, not something to smooth over.

Naming: source and warehouse columns are `snake_case`. The domain document is written
in the service's `camelCase`, so `priceCents` in `domain.md` is `price_cents` here.
The mapping is mechanical and there is no renaming beyond it.

Project layout:

```
models/
  staging/    stg_*      views, one per source, cleaning only
  intermediate/ int_*    reshaping, no business metrics
  marts/      mart_*     published, consumed outside the project
```

## Sources

Four tables land in the `kiezmarkt_raw` schema. They are written by the listing
service's export job, not by dbt. dbt reads them through `models/staging/sources.yml`
and never writes to them.

### `raw_listing_events`

Append-only event stream, **exported from the listing service's status-history
table**. It is not consumed from a Kafka topic: there is no listing event topic
(`contracts/events.md`). The export job reads the history rows the service writes on
every accepted transition and lands them here. **Delivery is at-least-once**: the
same `event_id` can appear more than once, and `occurred_at` and `received_at` can
fall in different weeks. Both facts are load-bearing for everything below.

| Column | Type | Notes |
| :--- | :--- | :--- |
| `event_id` | `STRING` | Idempotency key. Not unique in the landed table |
| `event_type` | `STRING` | `listing.created`, `listing.status.changed`, `listing.updated`, `listing.deleted`. These are row types in the service's status history, not topic names, and they follow the same naming shape by convention only. There is no per-status type: every transition arrives as `listing.status.changed` |
| `listing_id` | `STRING` | → `raw_listings.id` |
| `seller_id` | `STRING` | → `raw_sellers.id` |
| `occurred_at` | `TIMESTAMP` | UTC. When the transition happened in the service |
| `received_at` | `TIMESTAMP` | UTC. When the export job wrote the row. Partition column |
| `from_status` | `STRING` | Set only on `listing.status.changed`. Null on every other type |
| `to_status` | `STRING` | Set only on `listing.status.changed`. Null on every other type |
| `price_cents` | `BIGINT` | Price as of the event. **Nullable in the data** |
| `currency` | `STRING` | ISO 4217 |
| `category_id` | `STRING` | Category as of the event |
| `postcode` | `STRING` | 5 digits |
| `ingest_date` | `DATE` | Partition column, derived from `received_at` |

### `raw_listings`

Daily full snapshot of the listing table. Mirrors the `Listing` entity.

| Column | Type | Notes |
| :--- | :--- | :--- |
| `id` | `STRING` | |
| `seller_id` | `STRING` | |
| `title` | `STRING` | 3–80 characters |
| `description` | `STRING` | Up to 4000 characters |
| `category_id` | `STRING` | Leaf category, current form `elektronik.audio` |
| `price_cents` | `BIGINT` | Declared non-null upstream. **Arrives null.** See *Hazards* |
| `currency` | `STRING` | Always `EUR` today |
| `status` | `STRING` | `draft`, `published`, `paused`, `expired`, `deleted` |
| `postcode` | `STRING` | 5 digits, zero-padded, string not integer |
| `created_at` | `TIMESTAMP` | UTC |
| `published_at` | `TIMESTAMP` | Null while `draft` |
| `image_ids` | `ARRAY<STRING>` | 0–20, ordered |
| `snapshot_date` | `DATE` | Partition column |

### `raw_sellers`

Daily full snapshot of the seller table, **with `email` removed at the service
boundary**. Invariant 6 in `domain.md`: seller email never appears in an event
payload, a search response, or the warehouse. There is no column for it here, no
hashed variant, and no join that could reconstruct it. A pipeline that needs to
contact a seller is out of scope for this warehouse.

| Column | Type | Notes |
| :--- | :--- | :--- |
| `id` | `STRING` | |
| `display_name` | `STRING` | Public display name |
| `created_at` | `TIMESTAMP` | UTC |
| `is_commercial` | `BOOLEAN` | |
| `snapshot_date` | `DATE` | Partition column |

### `raw_saved_searches`

Daily full snapshot. The `filters` object from `domain.md` lands as a struct.

| Column | Type | Notes |
| :--- | :--- | :--- |
| `id` | `STRING` | |
| `seller_id` | `STRING` | The owner |
| `label` | `STRING` | User-supplied |
| `query` | `STRING` | Free text |
| `filters` | `STRUCT<category_id: STRING, price_min_cents: BIGINT, price_max_cents: BIGINT, postcode: STRING, radius_km: INT>` | Any member may be null |
| `notify` | `BOOLEAN` | |
| `last_matched_at` | `TIMESTAMP` | Null if never matched |
| `snapshot_date` | `DATE` | Partition column |

### No category source

There is no `raw_categories`. The category tree is not exported. Consequences that
the warehouse lives with today:

- `category_id` cannot be validated against a list of known leaves, so a typo or a
  retired id passes through into the mart silently.
- The `legacy_id` question from `domain.md` (which categories are still referenced by
  their pre-2024 numeric id) cannot be answered from the warehouse at all.
- Reports group by `category_id`, never by a display name, because there is nowhere
  to get one.

## Staging models

One per source. Cleaning and typing only, no business logic, no joins. All four are
materialised as views except `stg_listing_events`.

Every staging model renames the source's bare `id` to an entity-qualified key, so no
downstream model ever joins on a column called `id`: `raw_listings.id` becomes
`listing_id`, `raw_sellers.id` becomes `seller_id`, `raw_saved_searches.id` becomes
`saved_search_id`. `raw_listing_events.event_id` is already qualified and is left
alone.

### `stg_listing_events`

`materialized='incremental'`, `incremental_strategy='merge'`, `unique_key='event_id'`,
`partition_by='ingest_date'`.

- Deduplicates on `event_id`, keeping the row with the greatest `received_at`. This is
  the only place at-least-once delivery is handled.
- Filters `event_type` to the four known values; unknown types are dropped, uncounted
  and unlogged.
- Casts `occurred_at` and `received_at` to UTC timestamps.
- Adds `event_week`, the Monday of the ISO week of `occurred_at` (see the
  `listing_week` doc block).
- Incremental filter on `received_at`, not `occurred_at`, so late events are picked up
  on the run after they land.

### `stg_listings`

- One row per `listing_id` from the latest `snapshot_date`.
- `postcode` trimmed and left-padded to 5 characters. Rows whose `postcode` is not 5
  digits after padding are kept, not rejected, and carry `region_code = NULL`.
- `region_code`: the first two characters of `postcode`. **`domain.md` has no region
  entity.** This derivation exists only in the warehouse, was never agreed with the
  service, and is what every "per region" number in the report is built on. See
  *Underspecified*.
- `price_cents` passed through unchanged, nulls included. Nothing is coalesced to `0`
  here, because `0` and `null` are not the same thing and the difference is not
  settled (`domain.md`, *Where the domain is deliberately silent*).
- `is_priced`: boolean, `price_cents > 0`.
- `status` lower-cased and trimmed.

### `stg_sellers`

- One row per `seller_id` from the latest `snapshot_date`.
- Passes `is_commercial` through. Adds no activity flag of any kind. See
  *The metric that does not exist*.

### `stg_saved_searches`

- One row per `saved_search_id` from the latest `snapshot_date`.
- Flattens `filters` into `filter_category_id`, `filter_price_min_cents`,
  `filter_price_max_cents`, `filter_postcode`, `filter_radius_km`.
- Adds `has_price_filter`, `has_geo_filter` as booleans.
- Does not validate that `filter_price_min_cents <= filter_price_max_cents`.

## Intermediate

### `int_listing_status_history`

One row per `listing_id` per status interval, built from `stg_listing_events`:
`listing_id`, `status`, `valid_from`, `valid_to` (null while current). Used to answer
"was this listing published at any point in week W" without scanning events per week.
`deleted` is terminal, so an interval with `status = 'deleted'` never has a successor.

## Marts

| Model | Grain | Materialisation |
| :--- | :--- | :--- |
| `mart_weekly_marketplace` | `listing_week` × `category_id` × `region_code` | incremental, merge |
| `dim_seller` | one row per `seller_id` | table |
| `mart_saved_search_activity` | `listing_week` × `filter_category_id` | table |

`dim_seller` carries `seller_id`, `display_name`, `created_at`, `is_commercial`,
`first_listing_published_at`, `listings_published_total`. It carries **no** activity
flag and no `is_active` column.

### `mart_weekly_marketplace`

The weekly aggregate. It has been running for three years and its output has been
published into the same review meeting the whole time.

**Grain: one row per (`listing_week`, `category_id`, `region_code`).** No other key
combination is unique. A week with no listings in a given category and region produces
no row; absence means zero, and consumers have to know that.

| Column | Type | Definition |
| :--- | :--- | :--- |
| `listing_week` | `DATE` | Monday of the ISO week, UTC. See doc block |
| `category_id` | `STRING` | Leaf category id, unvalidated |
| `region_code` | `STRING` | First two digits of `postcode`, warehouse-derived |
| `active_listings` | `BIGINT` | Count of `active_listing` in the week |
| `new_listings` | `BIGINT` | Count of `new_listing` in the week |
| `listings_priced` | `BIGINT` | Active listings with `price_cents > 0` |
| `listings_zero_price` | `BIGINT` | Active listings with `price_cents = 0` |
| `listings_null_price` | `BIGINT` | Active listings with `price_cents IS NULL` |
| `price_cents_sum` | `BIGINT` | Sum over `listings_priced` only |
| `price_cents_p50` | `BIGINT` | Discrete median over `listings_priced`, lower of the two middle values. Integer cents |
| `distinct_sellers` | `BIGINT` | Distinct `seller_id` among the active listings. **This is not "active sellers".** See below |
| `dbt_updated_at` | `TIMESTAMP` | Run timestamp |

Two properties the model is supposed to hold, neither of them tested:

1. `active_listings = listings_priced + listings_zero_price + listings_null_price`.
2. `price_cents_sum` and `price_cents_p50` are integers. Invariant 3 in `domain.md`:
   money is an integer number of cents, at every layer, and that includes an
   aggregate. A `percentile()` or `avg()` over cents returns a double and breaks it.
   There is no average column for exactly this reason, and no test that would catch
   one being added.

Incremental configuration:

```
{{ config(
    materialized  = 'incremental',
    incremental_strategy = 'merge',
    unique_key    = ['listing_week', 'category_id', 'region_code'],
    partition_by  = 'listing_week'
) }}
```

Lookback: on an incremental run the model rebuilds `listing_week >= date_trunc('week',
current_date() - interval 14 days)`, so the current week and the two before it are
recomputed on every run. That is the mechanism by which the previous week gets its
late events.

## Metric definitions

These are the only metric definitions that exist. They live in
`models/marts/_marketplace__docs.md` and are attached to the columns above via
`description: "{{ doc('...') }}"`.

```
{% docs listing_week %}

The ISO-8601 week, keyed by its Monday as a `DATE`. The week runs from Monday
00:00:00.000 UTC to Sunday 23:59:59.999 UTC.

The boundary is UTC, not `Europe/Berlin`. A listing published on Monday 00:30
Berlin time in summer falls into the previous week. This has been the behaviour
since the first run and is not being changed: three years of published numbers
use this boundary and changing it would move counts in every historical week.

{% enddocs %}
```

```
{% docs active_listing %}

A listing counts as active in a `listing_week` if it was in status `published` at
any point during that week.

Evaluated against `int_listing_status_history`: the listing is active in week W if
it has a status interval with `status = 'published'` that overlaps W by at least one
instant. A listing published on Sunday at 23:00 and paused on Monday at 01:00 is
active in both weeks.

This is an overlap definition, not an end-of-week snapshot. It is therefore not
additive across weeks: the sum of `active_listings` over four weeks is not the number
of distinct listings active in that month, and no model in this project computes that
number.

Price does not enter the definition. A listing with `price_cents IS NULL` or
`price_cents = 0` is active if its status says so.

{% enddocs %}
```

```
{% docs new_listing %}

A listing counts as new in a `listing_week` if its `published_at` falls inside that
week.

`published_at`, not `created_at`. A listing drafted in January and published in March
is new in March. Per invariant 2 in `domain.md`, `published_at` is set exactly once,
on the first transition to `published`, so a listing is new in exactly one week and
never in two, even if it is later paused and published again.

A listing in status `draft` has `published_at IS NULL` and is never new.

Every `new_listing` in week W is also an `active_listing` in week W. The reverse does
not hold.

{% enddocs %}
```

## The metric that does not exist

**There is no definition of `active_seller`.** Not in this project, not upstream.

Stated plainly, because this is the thing that gets discovered halfway through an
analysis and not before:

- There is no `active_seller` doc block, no `is_active` column on `dim_seller`, no
  `mart_weekly_sellers`, and no macro. Grepping the repository for `active_seller`
  returns nothing.
- `mart_weekly_marketplace.distinct_sellers` is the closest thing that exists, and it
  is not the same measure. It counts sellers who have at least one active listing in
  the week, in one category and one region. Summing it across categories or regions
  double-counts any seller listing in more than one. It was named `distinct_sellers`
  rather than `active_sellers` deliberately, and that naming is the only surviving
  trace of the distinction.
- **Trust & Safety publish their own `active sellers` figure into the same weekly
  review**, computed from moderation and login activity on their side. Their number
  and any number derived from `distinct_sellers` will not match and are not supposed
  to. Nothing in either pipeline says so, and the two numbers sit in the same deck.
- The person who defined the listing-side metric left in March. There is no design
  doc, no ticket, and no commit message that records the intent. The people in the
  review meeting have been reading the number for three years.

This gap is not resolved here and must not be resolved by inference from the code.
Whatever the code computes today is what the code computes today; it is not evidence
of what the metric was meant to be. Establishing a definition is a conversation with
the people who consume the number, and that conversation is the exercise.

## Seed data

Every bootstrap produces the same seed set, matching `domain.md`'s sample data, so
groups can compare results.

| Table | Rows | Notes |
| :--- | :--- | :--- |
| `raw_listings` | 120 | Across 8 leaf categories in 3 top-level ones |
| `raw_sellers` | 40 | 6 with `is_commercial = true` |
| `raw_saved_searches` | 12 | 5 with `notify = true` |
| `raw_listing_events` | 312 physical rows, 303 distinct `event_id` | 9 deliberate duplicates |

Listing status distribution across the 120:

| `status` | Rows | `published_at` |
| :--- | :--- | :--- |
| `draft` | 9 | null |
| `published` | 86 | set |
| `paused` | 12 | set |
| `expired` | 10 | set |
| `deleted` | 3 | set |

So 9 rows have `published_at IS NULL` and 111 have it set, consistent with invariant 2.

### The price rows

These are the rows that break a naive aggregate, and they are the reason the seed set
is fixed rather than random.

- **7 listings with `price_cents: null`.** Spread across 7 of the 8 leaf categories,
  one each, deliberately not clustered, so a per-category check on a single category
  can miss them entirely. Statuses: 5 `published`, 1 `paused`, 1 `expired`. Five of
  the seven are therefore active in a normal week and land in
  `listings_null_price`.
- **3 listings with `price_cents: 0`.** All three `published`, in 3 different
  categories, one of which also holds one of the null rows. Whether these mean free or
  mean unpriced is not decided (`domain.md`); the warehouse counts them separately
  from both priced and null rows and takes no position.

What goes wrong without them: `sum(price_cents)` over all active listings silently
skips 7 rows while `count(*)` counts them, so any average computed as sum over count
is wrong by a factor nobody notices. `min(price_cents)` returns `0` and reads as a
one-cent bargain. A `coalesce(price_cents, 0)` merges the two cases and destroys the
distinction the mart is carrying on purpose.

### Saved search filter coverage

Of the 12 rows: 9 set `filter_category_id`, 7 set at least one of the price bounds,
5 set `filter_postcode` together with `filter_radius_km`, 2 set all four, 1 sets none
and is free text only. One row has `filter_price_min_cents > filter_price_max_cents`
and matches nothing; nothing flags it.

### Event seed

Generated from the listing states: 120 `listing.created`, 136 `listing.status.changed`
(111 to `published`, 12 to `paused`, 10 to `expired`, 3 to `deleted`), 47
`listing.updated` and 3 `listing.deleted`, for 306 distinct events. Nine of them are
written twice, giving 315 physical rows. Six events have `occurred_at` in the week
before their `received_at`.

## Hazards

**Late-arriving events are normal, not exceptional.** The export job delivers a
non-trivial share of each week's events after that week has closed. The previous week
is therefore already re-run as a matter of course, which is what the 14-day lookback
is for. Any change that narrows the lookback silently drops late rows.

**Re-runs must be idempotent, and one case is not.** The merge on
(`listing_week`, `category_id`, `region_code`) updates rows that are present in the new
result and inserts rows that are new. It does **not** delete rows that were present in
a previous run and are absent from the re-run. When the last listing in a category and
region gets deleted, the previous run's row survives with its old counts and the number
never goes to zero. No test catches this.

**Three years of published numbers must stay comparable.** The mart's output is read
in a recurring review, week over week, against its own history. A change to
`active_listing`, to `listing_week`'s boundary, or to `region_code`'s derivation
rewrites history on the next full refresh, and the meeting sees a step change with no
explanation. Any such change needs both a backfill and a note in the meeting, and
neither is automated.

**At-least-once delivery.** Deduplication happens in exactly one place,
`stg_listing_events`. Any model reading `raw_listing_events` directly double-counts.

**Timezone.** UTC week boundaries against a German-market product. Known, deliberate,
frozen for comparability. It is wrong for the business and right for the history.

**Unvalidated category ids.** No category source, so `category_id` is whatever the
snapshot says. A retired id produces a new row in the mart rather than an error.

**Price semantics.** `0` versus `null` is undecided upstream. The warehouse keeps them
in separate columns and pushes the decision to the reader. Any model that coalesces
them has taken a position the domain has not taken.

## Tests

Honest inventory. The suite is thin and known to be thin.

### What exists

| Model | Column | Test |
| :--- | :--- | :--- |
| `stg_listings` | `listing_id` | `unique`, `not_null` |
| `stg_listings` | `status` | `accepted_values` (the five from `domain.md`) |
| `stg_sellers` | `seller_id` | `unique`, `not_null` |
| `mart_weekly_marketplace` | `listing_week` | `not_null` |

Four models, seven test instances. That is the whole suite. `dbt build` is green.

### What does not exist

- **No uniqueness test on the mart grain.** Nothing asserts that
  (`listing_week`, `category_id`, `region_code`) is unique. A fan-out in a join would
  inflate every count and the run would stay green.
- **No test on `price_cents`.** A `not_null` test here would fail on the 7 seed rows,
  which is precisely the signal that should have been recorded and was not. There is
  no `dbt_utils.accepted_range` for `price_cents >= 0` either.
- **No relationship test** from `stg_listings.seller_id` to `stg_sellers.seller_id`,
  so an orphaned listing joins to nothing and disappears from any inner-joined mart.
- **No test that the count components sum to `active_listings`.**
- **No test that `price_cents_sum` and `price_cents_p50` are integers.** The invariant
  most likely to be violated is the one with no sensor on it.
- **No `dbt source freshness`.** Nothing notices if the export job stops. A stale
  snapshot produces a plausible-looking week of flat numbers.
- **No uniqueness test on `stg_listing_events.event_id`** after deduplication, so a
  regression in the dedup logic surfaces as counts that are slightly too high.
- **No idempotency test.** Nothing runs the model twice over the same window and
  compares.
- **No regression test against published history.** No stored snapshot of last week's
  output, so a definition change is invisible until someone in the review notices the
  shape of a line.
- **No test on `region_code`,** which is the one column with no upstream definition at
  all.

Block 5.1 of the training audits exactly this list. The gap is the material; do not
close it ahead of the session.

## Underspecified

Carried out of `domain.md`, or discovered while writing this contract:

- **Region is not a domain concept.** `domain.md` has `postcode` and nothing above it.
  `region_code` is invented in `stg_listings` from the first two digits and is not
  agreed with anyone. Every "per region" number depends on it.
- **`active_seller` has no definition anywhere.** Central, see above.
- **`0` versus `null` price.** Undecided in `domain.md`, kept separate here.
- **No category source table**, so no leaf validation and no `legacy_id` answer.
- **No definition of what a "month" or "quarter" is** on top of `listing_week`, though
  both appear in the review. Weeks do not tile months.
- **The seed set is described in two places.** `contracts/api.yaml` fixes the fixture
  examples served by the API; this document fixes the row counts and distributions in
  the warehouse. They agree on the counts `domain.md` states (120 listings, 40 sellers,
  12 saved searches, 7 null prices, 3 zero prices). The status distribution and the
  event counts are defined **here only** and have no counterpart in `api.yaml`; if that
  file ever states them too, the two have to be reconciled rather than allowed to
  drift.
- **The API exposes a seller as `sellerId` only** and defines a `SellerPublic` shape
  without `createdAt`. `dim_seller.created_at` therefore comes from the warehouse
  snapshot and has no serving-side equivalent, which is worth knowing before anyone
  proposes computing seller tenure in the application instead.
