# Kiezmarkt — the guiding project

Every hands-on exercise in the two-day training runs against **Kiezmarkt**, a
synthetic neighbourhood classifieds marketplace. One project, both days, four role
groups working the same domain from four sides.

Start with [`domain.md`](domain.md). Then, depending on what you need:
[`bootstrap.md`](bootstrap.md) to build it, [`docs/project/backlog.md`](docs/project/backlog.md) for the
tickets, [`contracts/`](contracts/) for the specification.

## What is in here

| File                                                 | What it holds                                                                                  |
| :--------------------------------------------------- | :--------------------------------------------------------------------------------------------- |
| [`domain.md`](domain.md)                             | Entities, invariants, the four surfaces, and what the domain deliberately leaves open          |
| [`bootstrap.md`](bootstrap.md)                       | Participant-facing. The fixed prompt, the skeleton contract, the self-check                    |
| [`docs/project/backlog.md`](docs/project/backlog.md) | The tickets, by role and by block. Including the block-7 risk task sets                        |
| [`ops.md`](ops.md)                                   | The operational context: what is in production, what is irreversible, what nobody would notice |
| [`contracts/api.yaml`](contracts/api.yaml)           | OpenAPI for the listing service                                                                |
| [`contracts/events.md`](contracts/events.md)         | The published event contracts                                                                  |
| [`contracts/data.md`](contracts/data.md)             | Warehouse sources, models, metrics, and the one metric that is undefined                       |
| [`contracts/ui.md`](contracts/ui.md)                 | Screens, components, states. Renderer-agnostic                                                 |
| [`contracts/acceptance.md`](contracts/acceptance.md) | Acceptance criteria per backlog item, stack-independent                                        |