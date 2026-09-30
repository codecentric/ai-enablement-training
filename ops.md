# Kiezmarkt — operations

The page an on-call engineer reads before their first shift. It exists because a risk
decision needs facts: what is in production, what a mistake costs, and how long it
takes anybody to notice. `domain.md` says what the system is. This says what happens
when it breaks.

Synthetic, like the rest of the project. Nothing here describes a real service.

## What runs in production

| Surface | Stack | Shape |
| :--- | :--- | :--- |
| **Listing service** | Java, Spring, Kubernetes, Postgres, Kafka | ~2.4M reads/day, ~11k writes/day (create, edit, status change) |
| **Search index** | Search cluster, fed from the listing service and a nightly reconciliation | ~1.9M queries/day |
| **Web front end** | TypeScript, React + Astro, plus a legacy server-side renderer on the results page | ~60% of sessions |
| **iOS app** | Swift, SwiftUI, own release train | ~40% of sessions across both apps |
| **Android app** | Kotlin, Compose, own release train | see above |
| **Notification service** | Fan-out job every 15 minutes | ~40k pushes and ~6k emails on a normal day |
| **Warehouse** | Databricks, `dbt`, nightly at 02:15 UTC | ~40 models, 3 of them read outside the data group |
| **Weekly report** | Generated from the warehouse, Monday 06:00 CET, circulated as a snapshot | Read by category management and above |

Traffic peaks Sunday between 18:00 and 21:00 CET, roughly three times the weekday mean,
with a smaller weekday peak between 20:00 and 22:00. New listings are weekend-heavy.
January is the annual high: post-holiday clear-outs push volume to about 1.6 times
December. The notification fan-out spikes whenever a bulk import lands, which is not on
a schedule anybody controls.

## Deploy and rollback

| Surface | Path to production | Time | Rollback |
| :--- | :--- | :--- | :--- |
| Listing service | Merge to `main`, CI, canary at 10% for 15 minutes, then full | ~25 min | Redeploy the previous image, ~6 min |
| Postgres schema | Forward-only migrations, expand and contract | With the deploy | **None.** A dropped column comes back as a restore, not a redeploy |
| Search index | Mapping or analyzer changes need a reindex from the listing store | ~4 h full reindex | **None.** You reindex again |
| Web front end | Merge to `main`, build, atomic swap | ~9 min | Redeploy the previous build, ~5 min. CDN assets are cached up to 24 h, so a bad asset outlives the rollback |
| iOS app | Fortnightly train, store review 1–3 days, phased release over 7 days | 4–10 days | Pause the phased release. Installed builds stay installed |
| Android app | Fortnightly train, staged rollout 1 / 10 / 50 / 100% over 5 days | 1–6 days | Halt the rollout. Installed builds stay installed |
| `dbt` models | Merge to `main`, picked up by the next scheduled run | Up to 24 h | Revert and re-run. Numbers already read stay read |
| Weekly report | Generated Monday 06:00 CET, snapshot circulated | — | A correction is a second email, not a replacement |

Server-side feature flags cover the listing service, the search API and the web front
end, and flip in seconds. The apps read remote config at cold start: a flag reaches
roughly 85% of active devices within 24 hours, the rest whenever the user next opens
the app. Anything behind a build-time switch is not reachable by flag at all.

**Mobile is the asymmetry that matters.** Every other surface can be back to the
previous behaviour inside ten minutes. A mobile release cannot. Halting a rollout stops
new installs; it does not touch the devices that already have the build, and the fix
ships on the next train. Roughly 12% of active devices run a build older than 90 days,
so a client-side decision made today is still being executed by somebody in November.
"We can revert it" holds for the listing service and the web front end. It does not hold
for either app, for a schema migration, for a reindex, or for a number somebody has
already read.

## What cannot be taken back

- **A sent notification or email.** There is no unsend. A wrongly triggered fan-out is
  visible to every recipient before anyone has read the alert.
- **An event another team has consumed.** Once `savedsearch.matched` is read by a
  downstream consumer, a correction is a compensating event, and only if the consumer
  handles one. We do not control their code or their timelines.
- **A deleted listing.** `deleted` is terminal (`domain.md`, invariant 5). Nothing
  transitions out of it. Recovery is a database restore.
- **A number in a published report.** The weekly report is compared quarter over
  quarter. A number that was wrong and is corrected leaves a step in a three-year
  series that has to be explained every time the series is shown.
- **A credential that has been in a repository.** Rotation is the only remedy;
  deleting the commit is not one.

## Data classification

| Data | Class | Rule |
| :--- | :--- | :--- |
| `seller.email` | PII | Never in an event payload, a search response, or the warehouse (`domain.md`, invariant 6). It stops at the service boundary |
| `title`, `description` | User-generated, untrusted | Users put phone numbers, addresses and anything else in there. It reaches the search index, both apps and any model context that touches a listing. Treat it as untrusted input, not as content |
| `postcode` | Quasi-identifier | Five digits plus one listing narrows a person. Fine inside the product; in the warehouse only aggregated, and never joined onto a seller identity in an export |
| `savedSearch.query`, `savedSearch.filters` | Behavioural | What a person is looking for. Not published, not in the report, not in an event payload |
| `sellerId`, `listingId`, `savedSearchId` | Opaque | Not sensitive on their own |
| `displayName`, `isCommercial` | Public | Shown publicly by design |
| Report aggregates | Not confidential, externally quoted | Category management makes investment decisions on them. Not sensitive, but wrong is expensive |
| Warehouse and CI credentials | Secret | Never in a prompt, a transcript, a fixture or a commit. See the incident of 2026-05-06 |

## Blast radius

Contained to one surface, where you can name everyone who reads the output:

- Web rendering, styling and copy on the results page.
- UI inside one app. Contained, but see the mobile asymmetry: contained is not
  reversible.
- A private intermediate `dbt` model with no exposure.
- Test code, fixtures and tooling.

Reaches other teams, where you cannot:

- **The notification service.** It is coupled to us twice: it consumes
  `savedsearch.matched`, and it receives every listing status change over the
  synchronous HTTP call inside our write transaction. A personalisation team, trust &
  safety and a partner-feed team have all asked for listing status changes as well.
  None of those release timelines are ours.
- **The search API.** Consumed by the web front end, both apps, and an internal support
  tool. Two of those four cannot be rolled back this afternoon.
- **Warehouse tables.** Read by category management, trust & safety and finance. Trust
  & safety publish their own `active sellers` figure from the same tables, and the two
  numbers appear in the same review meeting.
- **The weekly report.** Quoted in decisions, compared across quarters.

## Detection

**Alerted within minutes.** 5xx rate and p99 latency on the listing service and the
search API; Kafka consumer lag; canary health during a deploy; search cluster
availability; notification *send failures*; crash-free rate per app version; a failed
nightly `dbt` run.

**Noticed within a day, by someone who looks.** Search index freshness drift; the
image-processing backlog; a filter returning fewer results than it should, but only if
the drop is large enough to move session metrics; a rise in support tickets.

**Noticed weekly, or at the next comparison.** Report numbers. Most `dbt` tests, which
are configured to `warn` rather than `error`, so a failing test is a line in a log
nobody opens. Growth's dashboards, read once a week. Anything on the seller side.

**Not noticed at all.** This is the list the risk conversation should start from:

- A price rendered wrongly for the listings that have no price. No alert, no test, no
  ticket, because a user who sees `0,00 €` concludes the item is free.
- A notification that is never sent. We alert on sends that fail, not on sends that
  never happened.
- An analytics event that stopped firing on one component while the page keeps working.
  The dashboard keeps drawing a line.
- A `legacyId` lookup that silently falls back to a default category instead of failing.
- A change in result ordering across pages. The search API does not guarantee ordering
  when listings change status mid-pagination, so nothing can assert on it.

An error nobody can detect is a different risk from a loud one, and it is usually the
worse of the two. A loud failure gets rolled back in six minutes.

## Past incidents

### 2026-02-11 — status writes failed with the notification service

The notification service returned 5xx for 47 minutes. The listing service calls it over
HTTP inside the transaction that writes the status change, so the write failed with it.
About 2,300 status transitions failed; users pausing or deleting a listing saw a generic
error and retried. The 5xx alert on the *listing service* fired in four minutes; nothing
pointed at notifications. Mitigation was a flag that skipped the call, which then
dropped notifications for the rest of the window, and nobody can say how many. The
synchronous call is still in the write path. `KA-4417` exists because of this.

### 2025-11-24 — a published number moved without the market moving

A filter in the staging model for `active listings` was changed so that listings in
`paused` were excluded; they had been counted before. The change was defensible and
nobody wrote it down. The following Monday the report showed active listings per
category down 9% week over week. Category management escalated it as a supply problem
and spent three days on it before someone found the commit. The quarter-over-quarter
series now has a step in it. Nothing failed: every `dbt` test passed, because none of
them assert on a value.

### 2026-05-06 — a warehouse token reached a repository through a shared session

Session transcripts were exported and committed so a skill could be improved from real
examples. One transcript contained a warehouse token that had been pasted into a prompt
during debugging. Secret scanning ran on a branch pattern the file did not match. The
token sat in the repository for six days and was pushed to one fork before it was
found. Rotated, no evidence of use. Nobody did anything on purpose, and the review that
would have caught it was a review of a skill, not of a credential.
