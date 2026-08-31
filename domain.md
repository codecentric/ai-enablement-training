---
kunde: adevinta
stand: 2026-08-31
status: Entwurf — Leitprojekt für das AI-Enablement-Training
zweck: Domänenmodell des Leitprojekts Kiezmarkt
updated: 2026-08-31
---

# Kiezmarkt — the domain

Kiezmarkt is a neighbourhood classifieds marketplace. People list things they no
longer need, other people find them through search, save the searches they care
about, and get told when something new matches.

It is synthetic. No Kleinanzeigen service is named, reproduced or implied. The
domain is close enough to daily work that nobody has to translate, and far enough
that nobody has to defend how it is really done.

## The four surfaces

Each role group works on one surface. The surfaces share the same domain objects,
so a decision made on one is visible from the others.

| Surface | Role group | What lives here |
| :--- | :--- | :--- |
| **Listing service** | Backend | Listings, their lifecycle, status events, the search API |
| **Search & filter** | Frontend | Result list, filter panel, listing card, saved searches on web |
| **Saved searches** | Mobile | The saved searches screen, offline behaviour, match notifications |
| **Marketplace report** | Data | Weekly aggregates over listings and sellers |

## Entities

### Listing

The centre of the domain. Everything else points at it.

| Field | Type | Notes |
| :--- | :--- | :--- |
| `id` | string | Opaque, server-assigned |
| `sellerId` | string | → Seller |
| `title` | string | 3–80 characters |
| `description` | string | Up to 4000 characters |
| `categoryId` | string | → Category. See the legacy id note below |
| `priceCents` | integer | **Declared non-null. The feed disagrees — see Invariants** |
| `currency` | string | ISO 4217. Always `EUR` today |
| `status` | enum | `draft` · `published` · `paused` · `expired` · `deleted` |
| `postcode` | string | 5 digits, German format |
| `createdAt` | timestamp | UTC |
| `publishedAt` | timestamp \| null | Null while `draft` |
| `imageIds` | string[] | 0–20, ordered, first one is the cover |

### Seller

| Field | Type | Notes |
| :--- | :--- | :--- |
| `id` | string | |
| `displayName` | string | Shown publicly |
| `email` | string | **PII.** Never leaves the service, never enters the warehouse |
| `createdAt` | timestamp | |
| `isCommercial` | boolean | Commercial sellers have different listing limits |

A listing carries `sellerId` and nothing else about the seller. Any surface that
shows a name resolves it through `GET /sellers/{id}`, which serves the public
projection only: `id`, `displayName`, `isCommercial`. There is no path to `email`.

### Category

A two-level tree. Leaf categories carry listings; top-level ones do not.

| Field | Type | Notes |
| :--- | :--- | :--- |
| `id` | string | Current form: `elektronik.audio` |
| `legacyId` | integer | The pre-2024 numeric id. **Still referenced in places** |
| `parentId` | string \| null | Null for top level |
| `name` | string | Display name |

### SavedSearch

| Field | Type | Notes |
| :--- | :--- | :--- |
| `id` | string | |
| `sellerId` | string | The owner. Yes, buyers are sellers too — one account type |
| `label` | string | User-supplied, renameable |
| `query` | string | Free text |
| `filters` | object | `categoryId`, `priceMinCents`, `priceMaxCents`, `postcode`, `radiusKm` |
| `notify` | boolean | Whether new matches trigger a notification |
| `lastMatchedAt` | timestamp \| null | |

### Notification

Emitted when a new listing matches a saved search with `notify: true`.

| Field | Type | Notes |
| :--- | :--- | :--- |
| `id` | string | |
| `savedSearchId` | string | |
| `listingId` | string | |
| `channel` | enum | `push` · `email` |
| `sentAt` | timestamp | |

### Listing status transitions

`status` moves only through the status endpoint, never through a patch. These are the
legal moves. Everything else is rejected, including a transition to the status the
listing already holds.

| From | To |
| :--- | :--- |
| `draft` | `published`, `deleted` |
| `published` | `paused`, `expired`, `deleted` |
| `paused` | `published`, `expired`, `deleted` |
| `expired` | `published`, `deleted` |
| `deleted` | nothing |

A `draft` never expires: expiry is a consequence of having been published.
Re-publishing from `paused` or `expired` leaves `publishedAt` untouched.

## Invariants

These hold. Code may rely on them.

1. A listing is visible in search only while `status = published`.
2. `publishedAt` is set exactly once, on the first transition to `published`.
3. **Money is always an integer number of cents.** No floats, anywhere, at any
   layer. This is the single most-violated rule in the codebase and it is written
   down in exactly one place.
4. A category with children never carries listings directly.
5. `deleted` is terminal. Nothing transitions out of it.
6. Seller email never appears in an event payload, a search response, or the
   warehouse.

### One derivation that is not in the domain

The weekly marketplace report is grained *per region*, and there is no region entity
here. `region_code` is the first two digits of `postcode`, derived inside the
warehouse in `stg_listings`. It exists nowhere in the service and nobody outside the
data models has agreed to it. It is recorded here so it is a known shortcut rather
than an orphan invention. See `contracts/data.md`.

## Where the domain is deliberately silent

These are not oversights. They are the material for discovery, planning and example
mapping, and every one of them has bitten somebody in a block.

- **`priceCents` is typed non-null and the import feed delivers `null`** for
  listings posted without a price ("Zu verschenken"). The type is wrong. Fixing the
  type is out of scope for the training; handling the value is not.
- **What "active seller" means** is defined nowhere. Not in the code, not in a
  schema, not in a comment. The person who defined the listing-side metric left.
- **Whether a price of `0` means free or means unpriced.** Both readings exist in
  the current data.
- **Which categories are still referenced by `legacyId`.** Some are. Nobody has
  counted.
- **How stale a cached saved-search list may be** before showing it is worse than
  showing an error.
- **Whether the search API guarantees ordering** across pages when listings change
  status mid-pagination.

## Sample data

Every bootstrap produces the same seed set, so groups can compare results:

- 120 listings across 8 leaf categories in 3 top-level ones
- 40 sellers, 6 of them commercial
- **7 listings with `priceCents: null`**, spread across categories, not clustered
- 3 listings with `priceCents: 0`
- 12 saved searches, 5 with `notify: true`
- 4 categories that still carry a `legacyId` reference somewhere in the code

The exact fixtures are defined in `contracts/api.yaml` (`examples`) and
`contracts/data.md` (seed tables).
