---
kunde: adevinta
stand: 2026-08-31
status: Entwurf — Leitprojekt für das AI-Enablement-Training
zweck: Wie eine Gruppe das Kiezmarkt-Skelett im eigenen Stack erzeugt
updated: 2026-08-31
---

# Bootstrap Kiezmarkt in your stack

Kiezmarkt ships as contracts, not as code. Your group generates the skeleton in the
stack you actually work in, so everything you build over the two days sits in an
idiom you recognise.

This happens once, at the start of block 1. It is the first thing you will do with
an agent today, and it is also the first thing you will criticise.

## Before you start

You need: Claude Code working (`/doctor` if anything feels off), an empty directory,
and the contracts in `docs/project/contracts/`.

One person per group runs the bootstrap and shares the result. Do not run it four
times in parallel and then reconcile.

## The prompt

Use this one. Do not improve it yet — the point is that everyone starts from the
same instruction, and the differences you get anyway are the interesting part.

```
Read docs/project/domain.md and everything in docs/project/contracts/.

Build a runnable skeleton of the Kiezmarkt listing service in <YOUR STACK>.

It must:
- implement every endpoint in contracts/api.yaml, returning real data from an
  in-memory or file-backed store, no database server
- model the entities exactly as domain.md defines them, with the same field
  names and the same nullability
- load the seed dataset described in domain.md at startup
- have a test suite that runs with a single command and passes
- have a single command that starts the service

Do not write a README. Do not add a linter, formatter or CI config.
Stop when the test command and the start command both work.
```

Replace `<YOUR STACK>` with what your group works in, in one line. Be specific about
the language and the framework, and say nothing about structure:

| Role group | Example |
| :--- | :--- |
| Backend | `Java 21 with Spring Boot` |
| Frontend | `TypeScript with Astro and React` |
| Mobile | `Kotlin with Jetpack Compose` or `Swift with SwiftUI` |
| Data | `Python with dbt against DuckDB` |

Data groups build against `contracts/data.md` instead of `api.yaml`: the source
tables, the staging models and `mart_weekly_marketplace`, loaded from the seed data.

## What the skeleton must have when you are done

This is the contract of the bootstrap, not a description of the output. Different
stacks will produce very different trees; these things have to be true in all of
them.

- Every endpoint in `api.yaml` answers, with seed data behind it.
- Entity field names match `domain.md`. Not close, the same.
- The seed dataset loads: 120 listings, 40 sellers, 12 saved searches.
- One command runs the tests. One command starts the service.
- Nothing else is configured. No linter, no CI, no formatter.

**Two things are deliberately not specified: where tests live and what they are
called.** Whatever your agent chose, that is your group's starting convention. Leave
it. You will need it in block 3.

## Self-check

Run this before you move on. It is the first sensor you will meet today, and block
5.1 comes back to it.

1. Start the service. `GET /listings` returns published listings from the seed set.
2. `GET /listings?categoryId=<a leaf category>` filters. The count changes.
3. `GET /listings/{id}` for a seeded id returns that listing.
4. `POST /listings` creates a `draft`. It does **not** appear in `GET /listings`.
5. `POST /listings/{id}/status` to `published` makes it appear.
6. The test command runs and passes.
7. `grep` your source for the string `priceCents`. It appears in the model and in
   at least one rendering or serialisation path.

If 1 to 6 pass, you are ready. Do not polish.

## When it does not converge

Timebox it. If the skeleton is not answering after a reasonable stretch, stop
generating and do this instead:

- **Narrow the prompt.** Drop `POST`, `PATCH` and saved searches. `GET /listings`,
  `GET /listings/{id}` and a passing test are enough substrate for every block on
  day 1.
- **Ask your trainer for a neighbouring group's skeleton.** Comparing two is not a
  consolation prize, it is better material than one.
- Blocks 2, 5.1, 6.3, 7 and 9 are design work and run fine on a partial skeleton.
  Only 6.1/6.2 and 8 need code that executes.

## Then, and only then: `/init`

Once the self-check passes, run `/init` on the skeleton and **do not accept the
result**. Read it.

That generated file is the material for the rest of block 1. It will describe what
is in the repository accurately, and it will not contain a single thing you actually
need to tell an agent — because none of that is in the code yet. Four examples, all
of them in `domain.md` and none of them in your skeleton:

- money is always an integer number of cents, never a float
- seller email never leaves the service
- `deleted` is terminal
- `priceCents` is typed non-null and the feed delivers `null` anyway

Finding that gap is the exercise.
