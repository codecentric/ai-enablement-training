---
kunde: adevinta
stand: 2026-08-31
status: Entwurf — Leitprojekt für das AI-Enablement-Training
zweck: Interface-Kontrakt Kiezmarkt für die Rollengruppen Frontend und Mobile
updated: 2026-08-31
---

# Kiezmarkt — the interface contract

Scope: the **Search & filter** surface (web) and the **Saved searches** surface
(mobile) from `docs/project/domain.md`, plus the screens both share.

This document describes screens, components, states and behaviour. It names no
framework, no view library and no layout system. Two groups build from it in parallel
on different stacks and their results have to be comparable, so anything that is a
rendering technique belongs in the group's own code and not here.

Two words used precisely throughout:

- **Screen** is a destination a user can be on. On web it has a URL; on mobile it is a
  navigation destination.
- **Component** is a reusable piece that appears on more than one screen and has its
  own states.

Field names are the domain's: `priceCents`, `imageIds`, `publishedAt`. Where the
platform naming differs, the mapping happens in the client and the contract stays in
domain terms.

The transport is fixed by `contracts/api.yaml`. Where this document describes what the
client does with a response, that file describes the response. On any disagreement
about a field, a status code or a parameter, `api.yaml` wins and this document is
wrong.

## Inventory

| Screen | Web | Mobile |
| :--- | :--- | :--- |
| Search results | yes | yes |
| Listing detail | yes | yes |
| Saved searches | yes | yes, primary surface |

| Component | Appears on |
| :--- | :--- |
| `ListingCard` | Search results |
| `FilterPanel` | Search results |
| `PriceLabel` | `ListingCard`, Listing detail |
| `ResultCountHeader` | Search results |
| `LoadMoreControl` | Search results |
| `SavedSearchRow` | Saved searches |
| `StalenessNotice` | Saved searches, Search results (offline) |

`StalenessNotice` **does not exist in the design system.** See *Saved searches*.

## Screen: Search results

The list of listings matching a free-text query and a filter set. Only listings with
`status = published` ever appear here (invariant 1).

Composition, top to bottom: the query input, `ResultCountHeader`, `FilterPanel`
(web: persistent beside the list; mobile: a sheet opened from a control in the
header), the result list of `ListingCard`, and the pagination affordance.

### States

| State | Required behaviour |
| :--- | :--- |
| Initial loading | Placeholder cards, one per expected result up to the page size. The list area does not change height when real results replace them. No spinner over an empty screen |
| Loaded | Results rendered. `ResultCountHeader` states how many results are loaded so far and how many filters are active. **It cannot state a total**: `ListingPage` in `api.yaml` carries `items` and `nextCursor` and no count. Wording that implies a total ("24 of 312") is not implementable and must not be designed |
| Loading more | Existing results stay interactive and stay in place. The appending indicator sits below the last card, never over it |
| Empty | No results for this query and filter set. Must name which filters are currently applied and offer a single control that clears all of them. Must not read as an error |
| Error | Retry control, and the query and filter state survive the retry. Partial results already on screen are kept and marked as incomplete rather than discarded. Errors are RFC 9457 problem documents; the client branches on `type` and never on `detail`, which is free text |
| Offline, mobile | The last successfully loaded page is shown from cache with a `StalenessNotice`. Filters and pagination are disabled while offline. New queries are refused with an explanation, not with a silent no-op |
| Offline, web | Treated as the error state. No result cache on web |
| End of results | `nextCursor` came back `null`. Stated explicitly. An infinite list that simply stops scrolling is not an end state |

### Pagination

Pagination is **cursor-based**, not page-numbered. The client sends `limit` and, from
the second page on, the `nextCursor` returned by the previous page. `nextCursor` is
`null` on the last page, and that is the only end-of-results signal.

Both platforms request `limit=20`, the API default.

- **The cursor is opaque and short-lived.** `api.yaml` states it must not be parsed,
  constructed, or stored beyond the life of the result set it belongs to. It therefore
  **never goes in the URL** and never into persistent storage. See *URL reflection*.
- **Web:** a forward-only sequence. The next page is fetched by an explicit control.
  Because there is no page number, "go to page 4" does not exist and must not be
  designed.
- **Mobile: infinite scroll.** The next page is requested when 6 items remain below the
  viewport. There is also a `LoadMoreControl` that is reachable by keyboard and by
  assistive technology, because an intersection-triggered load is not operable without
  scrolling.
- An expired or malformed cursor returns `400` with problem type `invalid-cursor`. The
  client discards the cursor and restarts the result set from the current filter state.
  It does not show a generic error for this case.

**Ordering across pages is not guaranteed.** `domain.md` records this as an open
question: when listings change status mid-pagination the backend makes no promise
about stable ordering. The client therefore must:

- de-duplicate by `Listing.id` when appending a page, keeping the first occurrence;
- tolerate a page that returns fewer than `limit` items without treating it as the
  end, since only `nextCursor: null` means the end;
- never key list items by index.

A listing that transitions out of `published` between two page loads simply does not
arrive. The list does not need to detect this and must not leave a gap.

## Component: `FilterPanel`

The filters are exactly the members of `SavedSearch.filters` in `domain.md`, and no
others. Adding a filter here without adding it there breaks saved searches. They map
one-to-one onto the query parameters of `GET /listings` in `api.yaml`. The free-text
query is separate from the filters and is capped at 200 characters.

| Filter | Domain field | Control | Rules |
| :--- | :--- | :--- | :--- |
| Category | `categoryId` | Single select over the two-level tree | Both levels are selectable: a top-level id widens the search to all of its leaves, a leaf id matches exactly. This is a search-only affordance. A top-level category still carries no listings of its own (invariant 4) |
| Price from | `priceMinCents` | Numeric input, euros | Entered in euros, held as integer cents. Inclusive bound. See *Money* |
| Price to | `priceMaxCents` | Numeric input, euros | Inclusive bound. A listing priced at exactly the bound is included |
| Postcode | `postcode` | Text input | Exactly 5 digits. Leading zeros preserved, so this is a string and never a number |
| Radius | `radiusKm` | Discrete steps: 2, 5, 10, 20, 50, 100 | Requires a postcode. Sent alone it is a `400` with problem type `radius-without-postcode`, so the control stays disabled until a valid postcode is entered and that error never reaches a user |

### Money in the filter inputs

The user types euros; the domain stores cents (invariant 3). The conversion is integer
arithmetic on the parsed digits, not a multiplication of a parsed decimal by 100. A
float round-trip turns `19.99` into `1998` cents often enough to matter, and the
resulting filter silently excludes listings priced at exactly the bound.

### URL reflection, web only

**Filter state is part of the URL.** Every applied filter appears in the query string,
and the URL is the single source of truth for the filter state: the panel renders from
the URL, not from a separate in-memory copy.

This is a hard requirement and it is where the frontend planning exercise goes wrong,
so it is spelled out:

- A pasted URL reconstructs the exact same result set and the exact same panel state,
  with no filter silently defaulted.
- The browser back button steps back through filter changes, one change at a time.
  Applying a filter is a history entry; typing in a text field is not, so a text filter
  is committed on blur or submit, not on every keystroke.
- Clearing a filter removes its parameter rather than writing an empty value, so two
  URLs describing the same search are string-identical and cache and share cleanly.
- Parameter names and value encodings are fixed by this contract and are not derived
  from internal state names, because they are public and appear in shared links.
- An unknown or malformed parameter is ignored and dropped from the URL rather than
  causing an error screen.
- **The pagination cursor is not part of the URL.** It is opaque and not durable past
  the result set, so a shared or bookmarked link reconstructs the filters and starts
  from the first page. A link that carried a cursor would resolve to `invalid-cursor`
  for whoever opened it later. Position within a result set is session state, held in
  the history entry, not in the address.

Mobile has no URL. The equivalent requirement is that filter state survives process
death and restoration: coming back to the app shows the same filters, not a reset
panel.

### States

| State | Required behaviour |
| :--- | :--- |
| Loading | The category tree is fetched. The other four controls are usable while it loads |
| Category tree error | The panel stays usable with the category control disabled and an explanation. The whole panel does not fail |
| Applying | Controls disabled, previous results dimmed but still on screen |
| Invalid | `priceMinCents > priceMaxCents`, or a postcode that is not 5 digits. Stated at the field, apply blocked. Never sent to the backend to be rejected there |
| Empty result preview | When the applied set yields zero results, the panel keeps the values so the user can widen rather than re-enter |

## Component: `ListingCard`

The unit of the result list. It shows, and shows only:

| Element | Source | Rules |
| :--- | :--- | :--- |
| Cover image | First entry of `imageIds` | `imageIds` may be empty. Fixed aspect ratio, reserved before load, so the list never reflows |
| Title | `Listing.title` | Maximum two lines, truncated with an ellipsis. Never wrapped mid-word |
| Price | `Listing.priceCents`, `Listing.currency` | Rendered by `PriceLabel`. See below |
| Postcode | `Listing.postcode` | Rendered as five digits, no locality name; there is no locality in the domain |
| Age | `Listing.publishedAt` | Relative, coarse: "today", "yesterday", "3 days ago", "2 weeks ago". Never a raw timestamp |

Not on the card: seller name, seller commercial flag, description, image count,
category. Not by preference: the `Listing` schema in `api.yaml` exposes a seller as
`sellerId` and nothing else, and no endpoint in that contract returns a seller. There
is no display name to render, so the card is listing-only.

The whole card is one activation target leading to Listing detail. There are no nested
controls inside it.

### States

| State | Required behaviour |
| :--- | :--- |
| Placeholder | Same outer dimensions as a loaded card. Image area, two title lines, one price line |
| Loaded | As above |
| Image missing (`imageIds` empty) | A neutral placeholder graphic in the image area. Not a blank box and not a collapsed layout |
| Image failed to load | Identical to image missing. Never a broken-image indicator, never a retry loop |
| Title at maximum length | 80 characters is the domain maximum and must render without pushing the price line out of the card |

## Component: `PriceLabel`

**This is the acceptance surface for the test-driven-development block.** The strings
below are the contract. They are not examples and they are not suggestions.

### Format

- Locale `de-DE`: `.` groups thousands, `,` separates the decimals.
- Always exactly two decimal places.
- The currency symbol follows the amount, separated by a **non-breaking space**
  (`U+00A0`), so the amount and symbol never break across lines.
- The symbol is derived from `Listing.currency`. It is `EUR` for every listing today
  and it is still read from the field, never hard-coded.
- The string is built from the integer: euros are `priceCents / 100` in integer
  division, cents are `priceCents % 100` zero-padded to two digits. **No floating-point
  arithmetic at any point.** Invariant 3 in `domain.md` is the most-violated rule in the
  codebase and this is where it gets violated.

### The three cases

In the tables below `\u00A0` stands for the non-breaking space, written out so the
expected strings can be copied into a test without an invisible character surviving or
not surviving the copy.

| `priceCents` | Display string |
| :--- | :--- |
| any integer greater than `0` | the formatted amount, e.g. `25,00\u00A0€` |
| `0` | `0,00\u00A0€` |
| `null` | `Zu verschenken` |

Required cases in full, to be asserted directly:

| `priceCents` | Display string |
| :--- | :--- |
| `1` | `0,01\u00A0€` |
| `5` | `0,05\u00A0€` |
| `99` | `0,99\u00A0€` |
| `100` | `1,00\u00A0€` |
| `2500` | `25,00\u00A0€` |
| `129900` | `1.299,00\u00A0€` |
| `1000000` | `10.000,00\u00A0€` |
| `0` | `0,00\u00A0€` |
| `null` | `Zu verschenken` |

### Why `0` and `null` differ

`priceCents` is declared non-null in the domain and **the import feed delivers `null`
anyway**, for listings posted without a price. The type is wrong; fixing the type is
out of scope and handling the value is not. A client that trusts the declared type
crashes, renders an empty price line, or prints `undefined` into the card, and all
three have happened.

`0` renders literally as a price of zero. It does **not** render as `Zu verschenken`.
`domain.md` records that whether `0` means free or means unpriced is undecided and that
both readings exist in the current data, so the display refuses to interpret it: it
shows what the field says and takes no position.

The result is an inconsistency visible to any user who sees both cards: a listing with
no price reads as a giveaway and a listing priced at zero reads as costing nothing. The
inconsistency is known, it is in the contract on purpose, and it is not resolved here.
It resolves when the product decides what `0` means, and when it does, this table
changes and every test that asserts against it changes with it. That is the point of
pinning the strings.

### Out of contract

- Negative `priceCents` does not exist in the domain and its rendering is undefined.
  A client must not crash on it. What it should show is an open item.
- A `currency` other than `EUR` does not exist today. The symbol lookup must not throw
  on an unknown code; falling back to the raw ISO code is acceptable.

## Screen: Saved searches

Primary surface for mobile, also present on web. A list of the user's `SavedSearch`
entries with rename, delete and the notify toggle.

The list is returned whole: `api.yaml` gives it no pagination, and `domain.md` sets no
upper bound on saved searches per account. The seed set has 12. A client that assumes
the list is always small has no contract backing that assumption, so the list is
virtualised or windowed rather than rendered in one pass.

### `SavedSearchRow`

| Element | Source | Rules |
| :--- | :--- | :--- |
| Label | `SavedSearch.label` | User-supplied, renameable. Truncated to one line |
| Summary | `query` and `filters` | A human-readable rendering of the search, e.g. "Verstärker, Elektronik/Audio, up to 200,00 €, 10245 within 10 km". Prices in the summary use `PriceLabel` and any member of `filters` may be null |
| Notify toggle | `SavedSearch.notify` | Two states, no third |
| Last match | `SavedSearch.lastMatchedAt` | Relative, e.g. "Last match 3 days ago". When `null`: "No matches yet". Never blank |

### Actions

| Action | Required behaviour |
| :--- | :--- |
| Rename | Edited in place. Empty is rejected. The domain sets no length limit on `label`, so the limit is a client decision and is currently unspecified. See *Underspecified* |
| Delete | Confirmed before it happens. Deletion is not undoable in this contract |
| Notify toggle | Applied optimistically. On failure the toggle returns to its previous position and the failure is stated. It never silently stays in the position the user chose |

### States

| State | Required behaviour |
| :--- | :--- |
| Loading | Placeholder rows |
| Loaded | As above |
| Empty | No saved searches yet. Explains what a saved search does and links to Search results to create one. Distinct from the error state |
| Error | Retry. If a cached list exists, it is shown behind the error rather than replaced by it |
| Offline, cached list available | The cached list is shown, marked stale, with its age. See below |
| Offline, no cache | An offline state, not an error state. Different wording, no retry control while offline |
| Stale | See below |

### Offline and staleness

The list is cached so it can be opened without a connection. That produces two problems
this contract does not solve.

**What must be communicated.** A cached list carries the age of the data, stated in
concrete terms ("Updated 2 hours ago"), not as a vague marker. A toggle changed while
the list is stale is still acting on data the user can see is old, so the toggle stays
enabled and the mutation is what gets blocked, not the display.

**Mutations while offline are refused, not queued.** Rename, delete and the notify
toggle require a connection. The attempt is refused with an explanation and the control
returns to its previous state. Queueing and replaying them is out of scope. This is
recorded here as a decision; it has not been agreed with product.

**How stale is too stale is not decided.** `domain.md` lists this explicitly as a
question the domain leaves open: at what age is showing a cached saved-search list
worse than showing an error. There is no threshold in this contract because there is no
threshold anywhere. A team building this screen has to pick one and has to be able to
say why.

**The design system has no component for stale or offline data.** There is no
`StalenessNotice`, no offline banner, no established pattern for "this is old". Design
has not been asked for one, and there is no ticket. Anything a group builds here is
invented on the spot and will not match the rest of the product. That is a real
constraint of the exercise, not an omission from this document.

## Screen: Listing detail

Reached from a `ListingCard`, and on web also by direct link.

| Element | Source | Rules |
| :--- | :--- | :--- |
| Image gallery | `imageIds` | 0 to 20 images in the given order, the first is the cover. Swipeable or paged; the current position is indicated |
| Title | `Listing.title` | Full, not truncated |
| Price | `priceCents`, `currency` | The same `PriceLabel` as the card, identical strings for identical inputs |
| Description | `Listing.description` | Up to 4000 characters. Collapsed past a threshold with an expand control. Rendered as plain text |
| Category | `Listing.categoryId` | Two-level breadcrumb |
| Postcode | `Listing.postcode` | Five digits |
| Published | `Listing.publishedAt` | Relative, same vocabulary as the card |
| Seller | `Listing.sellerId` | **Nothing renderable today.** `api.yaml` defines a `SellerPublic` shape (`id`, `displayName`, `isCommercial`) but no endpoint returns it, and that shape has no `createdAt`, so neither a display name, a commercial marker nor a "member since" line is available. The screen ships without a seller block. **`Seller.email` never appears anywhere** (invariant 6) |

### States

| State | Required behaviour |
| :--- | :--- |
| Loading | Skeleton with the image area at its final aspect ratio |
| Loaded | As above |
| Not found (`404`, `not-found`) | No such listing, or the caller may not see it. A "not available" state with a route back to search. Not an error and not a retry |
| Gone (`410`, `listing-deleted`) | The listing exists and is `deleted`, which is terminal (invariant 5). Distinct wording from not found: it was here and it is gone. Also not a retry |
| Error | Retry, distinct from both of the above |
| Offline, mobile | Shown from cache if the listing was viewed before, marked stale. Otherwise the offline state |
| Gallery image failed | The failed image is skipped in the gallery, the position indicator adjusts. One bad image does not fail the gallery |
| No images | The gallery is not rendered at all. Not an empty frame |

`GET /listings/{id}` returns a listing in **any** status except `deleted`, so a
`draft`, `paused` or `expired` listing reached by direct link renders normally. That is
deliberate in `api.yaml` (a seller reads their own draft there) and it means the screen
must handle a listing with `publishedAt: null`, which is every draft: the "Published"
row is omitted, not rendered as an empty value.

What is **not** decided is whether a non-owner following a shared link to a `paused` or
`expired` listing should see it. The transport allows it, the product has not said, and
the screen currently shows it. See *Underspecified*.

## Accessibility contract

Applies to both platforms. Requirements only; the mechanism is the platform's.

### Price

A price is announced as spoken words, not as the display string read literally. The
display string exists for the eye and contains a grouping dot, a decimal comma and a
symbol, all of which assistive technology renders inconsistently. `PriceLabel`
therefore carries a separate accessible text.

| `priceCents` | Announced as |
| :--- | :--- |
| `2500` | `25 Euro` |
| `129900` | `1299 Euro` |
| `1999` | `19 Euro 99 Cent` |
| `0` | `0 Euro` |
| `null` | `Zu verschenken` |

Whole amounts drop the cents. A `null` price is announced as the same phrase the eye
sees. **A `null` price must never produce an empty announcement, a skipped element,
the word "null", "undefined", "NaN", a lone currency symbol, or a bare number.** An
empty announcement is the specific failure mode here: the element is present, focusable
and silent, and a screen-reader user cannot tell a free listing from a broken card.

### `ListingCard`

The card is a single stop in the reading and focus order, with one accessible label
composed in this order: title, price announcement, postcode, age.

`Vintage Verstärker, 25 Euro, 10245, published 3 days ago`

The cover image is decorative and carries an empty alternative text, because the title
already names the item; a second announcement of the same thing doubles every card. No
element inside the card is separately focusable.

### Lists and filters

- The result count is announced when it changes, politely, without moving focus.
- Every filter control has a persistent label, not a placeholder used as a label.
- The number of applied filters is announced when it changes.
- A validation failure is associated with its field and announced when it appears.
- After a page is appended, the number of new results is announced as a count. Items
  are not announced one by one.
- Infinite scroll always has the keyboard-operable `LoadMoreControl` alongside it.
- Staleness and offline state are announced when they are entered, not only shown.

### Baseline

Focus is always visible. Interactive targets are at least 44 by 44. Text contrast is at
least 4.5 to 1, and state is never carried by colour alone; the notify toggle in
particular has a text state, not only a tint.

## Underspecified

Carried out of `domain.md`, or found while writing this contract:

- **`0` versus `null` price** is undecided in the domain. The display strings above are
  pinned so tests can exist; the meaning is not resolved.
- **Cache staleness threshold** for the saved-search list is undecided in the domain and
  undecided here.
- **Search ordering across pages** carries no guarantee. The client compensates by
  de-duplicating; that is a mitigation, not a fix.
- **Detail visibility for non-`published` listings** is decided at the transport layer
  and not at the product layer: `GET /listings/{id}` serves any status but `deleted`,
  and nobody has said whether a non-owner following a shared link should see a `paused`
  or `expired` listing.
- **No length limit on `SavedSearch.label`** in the domain, so rename has no defined
  maximum.
- **No locality name in the domain**, only `postcode`, so nothing above "10245" can be
  shown. Whether that is acceptable in the UI has not been asked.
- **No endpoint returns a seller.** `api.yaml` defines `SellerPublic` and serves it
  nowhere, so neither the card nor the detail screen can show a display name or a
  commercial marker. `SellerPublic` also omits `createdAt`, so "member since" is not
  expressible even once an endpoint exists.
- **No result total.** `ListingPage` has no count, so no screen can state how many
  results a search has.
- **Negative `priceCents`** rendering is undefined.
