# Kiezmarkt — the backlog

Everything the hands-on blocks run on. Read `domain.md` first: entities, invariants,
and the list of things the domain deliberately leaves open.

Four role groups work in parallel and never mix: **Backend**, **Frontend**, **Mobile**
(iOS and Android together), **Data**. Groups are formed by role. Nobody in a group is
assumed to work with anybody else in it, and none of the four surfaces belongs to
anybody's real team. Kiezmarkt is the shared codebase for two days.

Where a ticket is silent, that is deliberate. The silence is the exercise. Nothing in
this file resolves an ambiguity that `domain.md` left open, and neither should a
participant without writing down that they decided it.

## Inventory

| Block | Backend | Frontend | Mobile | Data |
| :--- | :--- | :--- | :--- | :--- |
| **2** — planning | `KA-4417` | `KA-2986` | `KA-3771` | `KA-1502` |
| **6** — test-driven development | `KA-4523` | `KA-3042` | `KA-3818` | `KA-1560` |
| **6.3** — loop engineering (paper) | `KA-4488` | `KA-3110` | `KA-3902` | `KA-1611` |
| **7** — risk classification | 8 tasks | 8 tasks | 8 tasks | 8 tasks |

The four block 2 tickets each carry a **Proposed approach** quoted from a colleague. It
is plausible, confidently phrased, and either wrong or much smaller than the real
problem. An agent will not object to it unaided. That is the point of the block.

---

## Backend

Surface: the listing service. Listings, their lifecycle, status events, the search API.

### KA-4417 · Publish listing status changes as events

**Block 2 — planning.**

> Plan this against Kiezmarkt. Where the ticket is silent, that is deliberate: decide,
> and write the assumption down where the next person will find it.

#### Context

When a listing changes status — published, paused, expired, deleted — the listing
service calls the notification service over HTTP, synchronously, inside the same
transaction that writes the status change.

Two things have made this hurt:

- Three other teams have asked for the same information in the last quarter. Right now
  each one means another outbound call from us.
- The notification service was down twice this year. Both times, status changes failed
  for our users, because the call sits inside the transaction.

#### What we want

Other teams subscribe to listing status changes instead of us calling them.

#### Proposed approach

> "Replace the HTTP call with a Kafka publish. It's the same place in the code — one
> line changes."
>
> — API guild thread

#### What we know

- Kafka is the standard for async here, ahead of SQS/SNS.
- A consumer must never see a status change that was later rolled back.
- The existing HTTP call has to keep working until the consuming teams have migrated.
  We do not control their timelines.
- Someone asked in the thread whether we need ordering guarantees per listing. Nobody
  has answered yet.

#### Not decided

Topic naming, event schema, and who owns it afterwards.

### KA-4523 · Serve a display-ready price on the listing API

**Block 6 — test-driven development.** Small enough to finish in the block.

**Behaviour to state before any code exists.** The listing and search endpoints return
a price that a client can render without inventing rules of its own. Today they return
`priceCents` and each of the three clients decides for itself.

**What is known.**

- `priceCents` is declared non-null. The import feed delivers `null` for listings posted
  without a price; 7 of the 120 seed listings are in that state.
- 3 seed listings carry `priceCents: 0`.
- Money is an integer number of cents at every layer (`domain.md`, invariant 3). No
  floats, not even in a response field.
- Web, iOS and Android all render this, and they do not agree today.

**Open question — do not let it be answered silently.** Whether `0` means free or means
unpriced, and what the API says when there is no price at all. Both readings exist in
the current data. Park it in example mapping; an agent will pick one and sound certain.

**Scope.** One response field, one mapper, the tests around them.

### KA-4488 · Replace numeric category references in the listing service

Categories carry a current string `id` (`elektronik.audio`) and a pre-2024 numeric
`legacyId`. The numeric form is still referenced in the code. Which categories are
still referenced that way has never been counted (`domain.md`). Replace the numeric
references with the current string form.

**Where they sit.** DTO mappers, the category resolver, two Flyway seed scripts,
feature-flag configuration, and hard-coded integers in the test suite.

**Sensor candidates.** `mvn -pl listing-service test`, compilation, a grep count that
has to go to zero.

**What would make the sensor lie.** A bare integer is not self-describing. The same
literal appears as a price in cents and as an HTTP timeout, so a grep-based count
overshoots, and a count that is wrong at the start is a stop criterion that never fires.

**Boundary the agent must not cross.** The Flyway migration files and the OpenAPI
contract under `contracts/`.

---

## Frontend

Surface: search and filter. Result list, filter panel, listing card, saved searches on
web.

### KA-2986 · Move the search filter panel off the legacy renderer

**Block 2 — planning.**

> Plan this against Kiezmarkt. Where the ticket is silent, that is deliberate: decide,
> and write the assumption down where the next person will find it.

#### Context

The search filter panel is still rendered by the legacy server-side templates. It is
one of the highest-traffic components we have.

Design has moved on: the new filter controls exist as design-system components, and the
design system cannot be used from the legacy renderer. Every design change to the panel
currently has to be hand-rebuilt in old markup, and the last two were shipped weeks late
because of it.

#### What we want

The filter panel uses design-system components and can be changed without touching the
legacy renderer.

#### Proposed approach

> "Port it 1:1 into a React island in the Astro page. Same markup, same behaviour,
> design-system components instead of the old CSS. It's a contained component — nobody
> else depends on it."
>
> — frontend guild sync

#### What we know

- Design-system components are available through the design system MCP and the Figma
  skill.
- Analytics events are currently bound to the existing markup. Growth reads those
  dashboards weekly.
- The panel is not the only thing the legacy renderer draws on that page. The results
  list stays where it is for now.
- Current filter state is reflected in the URL. Nobody is sure what depends on that.

#### Not decided

Whether this is the first of several islands on that page or a one-off.

### KA-3042 · Price on the listing card

**Block 6 — test-driven development.** Small enough to finish in the block.

**Behaviour to state before any code exists.** The listing card shows a price for every
listing in the seed set, including the 7 with no price and the 3 at zero, and the result
list does not render an empty cell for any of them.

**What is known.**

- The card is used in the result list and in saved searches on web.
- `currency` is always `EUR`; formatting is German locale, `1.234,00 €`.
- The price filter currently drops listings with no price out of the result set. Nobody
  decided that; it is what the query does.
- Money stays integer cents through the component (`domain.md`, invariant 3). A
  formatter that divides by 100 into a float is the most common way this rule is broken.

**Open question — do not let it be answered silently.** Whether `0` means free or
unpriced, and whether a listing with no price should be inside or outside a
`priceMaxCents` filter. The card answer and the filter answer have to be the same one.

**Scope.** One component, one formatter, the snapshot tests.

### KA-3110 · Replace numeric category references in the front end

Same migration as `KA-4488`, seen from the other side: numeric `legacyId` values are
still used where category ids are built, read and reported. Replace them with the
current string form. Which ones are still in use has never been counted.

**Where they sit.** Category link builders, filter query parameters, analytics event
payloads, a JSON fixture, and the legacy renderer's templates.

**Sensor candidates.** `tsc --noEmit`, the unit suite, the snapshot suite.

**What would make the sensor lie.** The legacy renderer's templates are not
typechecked. `tsc` goes green with every template untouched, so a loop that stops on a
green typecheck stops halfway and reports success.

**Boundary the agent must not cross.** The analytics event *names*. Growth's dashboards
key on them and a rename is invisible until the following week.

## Mobile

Surface: saved searches. The saved searches screen, offline behaviour, match
notifications. iOS and Android work as one group on two codebases.

### KA-3771 · Saved searches should survive a lost connection

**Block 2 — planning.**

> Plan this against Kiezmarkt. Where the ticket is silent, that is deliberate: decide,
> and write the assumption down where the next person will find it.

#### Context

The saved searches screen shows a full-screen error state whenever the request fails.
On a commute — underground, tunnels, bad reception — that is most of the time. It is our
second most common support topic this quarter, and the complaint is always the same:
*"I know what I saved. Why can't the app show me?"*

#### What we want

Users can open their saved searches without a connection.

#### Proposed approach

> "Cache the last successful response and show it when the request fails. Both platforms
> already have a local store, so this should be small."
>
> — ticket refinement

#### What we know

- iOS and Android ship on independent release trains. A shared cutover date is not
  realistic.
- The design system has no component for indicating stale or offline data. Design would
  have to make one, and they have not been asked yet.
- Users can rename and delete saved searches from this screen.
- Support has not been told anything about this yet.

#### Not decided

How old cached data may be before it is worse than an error state.

### KA-3818 · Price in the saved-search match cell

**Block 6 — test-driven development.** Small enough to finish in the block.

**Behaviour to state before any code exists.** iOS and Android render the same string
for the same listing, for every listing in the seed set. Today they do not.

**What is known.**

- iOS formats through `NumberFormatter`, Android through `NumberFormat`. The two already
  produce different output for `priceCents: 0`.
- The same cell appears in the match list and in the notification preview, and the push
  payload carries a pre-rendered price string from the server. That is a third
  rendering, and it ships on a different release cycle from either app.
- 7 of the 120 seed listings have no price, 3 have `0`.
- Money stays integer cents up to the formatter (`domain.md`, invariant 3).

**Open question — do not let it be answered silently.** Whether `0` is free or unpriced,
and what "no price" looks like: a dash, a word, or an empty cell. The second one is a
copy decision nobody has made, and an agent will make it.

**Scope.** One cell, one formatter per platform, snapshot tests on both.

### KA-3902 · Replace numeric category references in both apps

Same migration as `KA-4488`, across two codebases and two languages. Numeric `legacyId`
values are still used where categories are parsed, filtered and reported. Which ones are
still in use has never been counted.

**Where they sit.** Deep-link parsing, saved-search filters held in the local store,
analytics payloads, and test fixtures in both projects.

**Sensor candidates.** The unit suites, compilation on both platforms, a snapshot suite.
Not a device farm; nothing a human reads.

**What would make the sensor lie.** The local store on a user's device already holds
numeric ids written by an older build. The code migration is not the whole migration and
no compiler will say so. A green suite on both platforms means the source is clean, not
that the installed population is.

**Boundary the agent must not cross.** The persisted store's schema version. Changing it
without a read path for the old form breaks every device that upgrades, and no rollback
reaches them.

## Data

Surface: the marketplace report. Weekly aggregates over listings and sellers.

### KA-1502 · Add active sellers to the weekly marketplace report

**Block 2 — planning.**

> Plan this against Kiezmarkt. Where the ticket is silent, that is deliberate: decide,
> and write the assumption down where the next person will find it.

#### Context

The weekly marketplace report covers listings: how many are active, per category, per
region. Category management has asked repeatedly for the seller side of the same
picture — they are making decisions about category investment and can currently only see
supply, not who is producing it.

#### What we want

The weekly report shows active sellers per category alongside active listings.

#### Proposed approach

> "Add a column to the existing weekly aggregate: count distinct `seller_id` where the
> seller had at least one listing action in the last 7 days."
>
> — analytics request

#### What we know

- The report has been running weekly for three years. Category management compares
  quarter over quarter.
- Trust & Safety already publish something called *active sellers* in their own
  dashboard. The two numbers will be visible in the same review meeting.
- Event data arrives late often enough that we already re-run the previous week.
- The person who defined the original listing-side metric left in March.

#### Not decided

Whether "listing action" includes edits and price changes, or only new listings.

### KA-1560 · Price columns in the weekly report

**Block 6 — test-driven development.** Small enough to finish in the block.

**Behaviour to state before any code exists.** The weekly report shows a median price
per category, and the number is defined for categories that contain listings without a
price.

**What is known.**

- 7 of the 120 seed listings have no price and 3 have `priceCents: 0`, spread across
  categories rather than clustered.
- A `0` counted as a price moves the median. A `null` excluded changes the denominator.
  The two choices are independent and both have to be made.
- The report has run for three years and is compared quarter over quarter. Precedent for
  changing a definition quietly: 2025-11-24 in `ops.md`.
- `dbt` tests are configured to `warn`, not `error`. A green run is not evidence.
- The per-category listing count already exists and is untouched by this story. It must
  not end up disagreeing with the price column about which listings are in the category.

**Open question — do not let it be answered silently.** Whether `0` is a price.

**Scope.** One model, one column, the tests that assert on a value rather than a shape.

### KA-1611 · Replace numeric category references in the warehouse

Same migration as `KA-4488`, in `dbt`. Numeric `legacyId` values are still used in
category logic. Which ones are still in use has never been counted.

**Where they sit.** `case when` blocks in the staging models, a seed CSV mapping, three
exposures, and hard-coded ids in the SQL behind two dashboards that do not live in this
repository.

**Sensor candidates.** `dbt build --select state:modified+`, row-count parity against
the previous run, a per-category comparison against last week.

**What would make the sensor lie.** Two things at once. The tests `warn` instead of
failing, so `dbt build` exits green over a broken model until they are promoted to
`error`; and the two dashboards are outside the repository, so no sensor that runs here
can see them at all.

**Boundary the agent must not cross.** Anything the weekly report reads while the
previous week is still being re-run for late-arriving events.

### Block 7 — risk classification

**The rows are not in risk order.** Facts come from `ops.md`.

| # | Task | Facts that bear on it |
| :--- | :--- | :--- |
| D1 | Add column descriptions and `dbt` docs to the listings model | Documentation only. Nothing downstream reads it |
| D2 | Change whether listings with no price are excluded from the average-price aggregate | Moves a published number. Precedent: 2025-11-24. Nothing fails, and it surfaces at the next quarterly comparison |
| D3 | Join `seller.email` into a warehouse table for a marketing export | Email never enters the warehouse (`domain.md`, invariant 6) |
| D4 | Backfill last quarter after a fix to late-arriving-event handling | Routine and repeatable. It also moves numbers that have already been compared quarter over quarter |
| D5 | Add `active sellers` to the report using a definition the agent proposes | The definition is not in the code and cannot be recovered from it. Trust & Safety publish a differently-defined number into the same meeting |
| D6 | Add `not_null` and `unique` tests to a staging model | Additive. They `warn` rather than fail, so they change what a green run means without changing what it does |
| D7 | Rename a private intermediate model with no exposure | Lineage inside the group. Nothing outside reads it |
| D8 | Add a column to the weekly report table that nothing reads yet | Schema change on a published table. Unused today, and a column that exists gets used |

*Trainer note:* D2 and D4 are the intended disagreements. Both are ordinary data work,
both are technically reversible, and in both cases the thing that cannot be reversed is
a number somebody has already quoted.
