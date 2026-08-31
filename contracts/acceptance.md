---
kunde: adevinta
stand: 2026-08-31
status: Entwurf — Leitprojekt für das AI-Enablement-Training
zweck: Akzeptanzkriterien je Backlog-Item, stack-unabhängig
updated: 2026-08-31
---

# Acceptance criteria

Stack-independent criteria for the items in `backlog.md`. They are written so that a
Java group, a Swift group and a `dbt` group can each turn the same line into a test in
their own idiom without translating anyone else's framework.

## What is deliberately not in here

**The four block 2 planning tickets have no acceptance criteria, and they are not
getting any.** `KA-4417`, `KA-2986`, `KA-3771` and `KA-1502` are the planning exercise.
Setting the definition of done before a prompt runs is the thing being practised, and
handing it over would remove the exercise. If a group asks where the criteria are, the
answer is that writing them is phase 2.

## How the layers fit

Three documents say something about the same behaviour and they do not overlap by
accident:

| Document | Decides |
| :--- | :--- |
| `domain.md` | What is true about the data, and what is open |
| `contracts/ui.md` | The exact strings the web card renders. Display only |
| This file | What must be observably true, per item, and what is still undecided |

`ui.md` pins the rendering of a price and takes no position on what a price of `0`
*means*. That question is open, and it stays open, because it has different answers on
different surfaces: a card that shows `0,00 €` and a filter that drops the listing are
both defensible and they cannot both be right. Pinning the string does not settle the
semantics, and the block 6 tickets are correct to park it.

## Block 6 — test-driven development

Each of these is small enough to finish. Every one has a section of criteria that must
hold and a section of questions that must be answered by the group, in writing, before
the code exists. **An unanswered question is a finding, not a failure.**

### `KA-4523` — display-ready price on the listing API (Backend)

Must hold:

| Given | Then |
| :--- | :--- |
| A listing with `priceCents` greater than `0` | The response carries both the integer cents and a display-ready string, and the string is derived from the integer without floating-point arithmetic at any step |
| A listing with `priceCents: 0` | The response is well-formed and the display field is present and non-empty |
| A listing where the feed delivered no price | The response is well-formed. The endpoint does not error, does not omit the listing, and does not emit `null`, `undefined` or an empty string into a field a client will print |
| Any listing | `currency` is read from the record, never hard-coded |
| The search endpoint | Returns the same display value for a listing as the single-listing endpoint does |

Must be answered before writing code:

- What the API returns for a listing with no price. A field, a sentinel, or an absent
  key are three different contracts for a client.
- Whether `0` is a price or an absence.

Must not happen: a float anywhere in the money path, including intermediate values.

### `KA-3042` — price on the listing card (Frontend)

Must hold:

| Given | Then |
| :--- | :--- |
| Every listing in the seed set, all 120 | The card renders a non-empty price area. No blank cell, no `undefined`, no `NaN` |
| `priceCents` greater than `0` | The string matches the pinned table in `contracts/ui.md` exactly, non-breaking space included |
| A listing with no price | The card renders the agreed no-price treatment, and it is the same one in the result list and in saved searches |
| Any card | An assistive technology announcement exists for the price and is not empty, not `null`, and not the raw number of cents |

Must be answered before writing code:

- Whether a listing with no price is inside or outside a `priceMaxCents` filter. **The
  card answer and the filter answer have to be the same answer**, and today the filter
  silently drops those listings because that is what the query does, not because anyone
  decided it.

### `KA-3818` — price in the saved-search match cell (Mobile)

Must hold:

| Given | Then |
| :--- | :--- |
| The same listing on iOS and on Android | Both render the identical string, character for character |
| `priceCents: 0` | Both render the same thing. They do not today |
| A listing with no price | Both render the agreed treatment, and the cell has a non-zero height |
| The push notification preview | Carries the same string as the cell, or the difference is written down and deliberate |

Must be answered before writing code:

- What "no price" looks like: a dash, a word, or an empty cell. This is a copy
  decision, nobody has made it, and an agent will make it silently.
- Who owns the string when the server pre-renders it for the push payload and ships on
  a third release cycle.

### `KA-1560` — price columns in the weekly report (Data)

Must hold:

| Given | Then |
| :--- | :--- |
| A category containing listings with no price | The median is defined and the model produces a row |
| The same input twice | The same output. The model is idempotent under re-run |
| Any category | The listing count behind the price column agrees with the existing per-category listing count. They cannot disagree about which listings are in the category |
| The three seed listings at `0` and the seven with no price | Their treatment is asserted on by a test that checks a value, not a shape |

Must be answered before writing code:

- Whether `0` is a price. It moves the median if counted.
- Whether a listing with no price leaves the denominator or stays in it.

These are independent choices and both have to be made. A `dbt` run that passes is not
evidence here: the tests are configured to `warn`.

## Block 6.3 — the loop exercise

The four `legacyId` migration items are a **paper exercise**. Nothing runs. What they
need is not a test suite but a *done definition a machine could check*, because that is
the sensor the loop sheet asks for and the line most groups cannot fill.

A usable done definition for all four has the same two halves:

1. **Progress.** No numeric category reference remains outside the one place that is
   allowed to hold the mapping. Countable.
2. **Correctness.** The existing suite is green, and a named boundary was not touched.

Both halves are required. A sensor with only the second reports success on a repository
where nothing has been migrated, because the old code still works. A sensor with only
the first reports success on a repository that has been migrated wrongly.

Each of the four backlog items names a different way that sensor can lie. Finding it is
the exercise.

## The one criterion that applies to everything

No floating-point arithmetic in a money path, at any layer, in any language.
`domain.md` invariant 3. It is written down, nothing enforces it, and it will be broken
within the first hour of anybody generating code against these contracts. That is not a
prediction, it is the reason block 5.1 exists.
