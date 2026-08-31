---
kunde: adevinta
stand: 2026-08-31
status: Entwurf — Leitprojekt für das AI-Enablement-Training
zweck: Event-Kontrakte des Listing-Service im Leitprojekt Kiezmarkt
updated: 2026-08-31
---

# Kiezmarkt — listing service events

The listing service publishes one Kafka topic: `savedsearch.matched`. The
notification service consumes it. Everything else the service tells the rest of
Kiezmarkt it tells synchronously, over HTTP, or not at all.

Kafka is the house broker, chosen over SQS/SNS on purpose: the consumer here needs
replay and per-key ordering, and it reads from the log rather than polling the
service for what changed.

The domain model behind these payloads is `docs/project/domain.md`; the
synchronous surface is `docs/project/contracts/api.yaml`. Where any of the three
disagree, `domain.md` wins.

## What does not go through Kafka today

Listing lifecycle changes are not published. There is no topic for a listing being
created, updated, published, paused, expired or deleted, and nothing subscribes to
one.

When a listing changes status, the service writes the change and calls the
notification service over HTTP, synchronously, inside the same transaction that
writes the status change. Three other teams have asked for the same information.
Each one currently means another outbound call from the same place in the code.

The service does keep its own status history in Postgres. Every accepted transition
writes a row there, and that table is what the warehouse export reads
(`contracts/data.md`, `raw_listing_events`). It is a table inside the service, not
an interface anybody else reads.

Changing any of this is open work and is not specified here.

## Envelope

Every message carries the same five envelope fields before its payload fields.
These are transport metadata, not domain fields — they exist in no entity in
`domain.md` and no consumer should persist them as business data.

| Field | Type | Notes |
| :--- | :--- | :--- |
| `eventId` | string | Unique per message. The idempotency key for consumers |
| `eventType` | string | Equals the topic name |
| `schemaVersion` | integer | Starts at `1`. Bumped only on a breaking change |
| `occurredAt` | string, RFC 3339 UTC | When the service accepted the change, not when it produced the message |
| `producer` | string | Service name. Always `listing-service` today |

`eventId` is what makes redelivery safe. Kafka gives at-least-once delivery, so
a consumer will see the same `eventId` twice; deduplicating on it is the
consumer's job, not the producer's.

## Topics

| Topic | Key | Partitions | `cleanup.policy` | Retention |
| :--- | :--- | :--- | :--- | :--- |
| `savedsearch.matched` | `savedSearchId` | 24 | `delete` | 7 days |

Naming is `<aggregate>.<what>.<past-tense verb>`. A topic name is a promise
about what already happened, never a command.

## `savedsearch.matched`

Emitted when a listing becomes published and matches a stored saved search whose
`notify` is `true`. A saved search with `notify: false` never produces a message
on this topic, no matter how many listings match it.

The trigger is a transition into `published`, so a listing that is paused and
published again can match the same saved search twice. Suppressing the repeat is a
consumer decision, not a producer one.

**Key:** `savedSearchId`. The consumer of this topic is the notification service,
and the thing it must not reorder or duplicate is the sequence of notifications
for one saved search. Keying by `listingId` would spread one search's matches
across all partitions and make per-search throttling impossible; keying by
`sellerId` would serialize an account with many searches behind one partition.

**Ordering:** per `savedSearchId`. Matches for different saved searches are
unordered relative to each other, including two searches owned by the same
account. See *Deliberately unresolved* below.

**Retention:** `cleanup.policy=delete`, 7 days. A notification that is a week
late is worse than no notification, so there is nothing to replay beyond that.

**Fan-out:** one published listing matching `n` notifying saved searches produces
`n` messages, one per search, spread across partitions by `savedSearchId`.

```json
{
  "eventId": "ev_01k4m6h4d9",
  "eventType": "savedsearch.matched",
  "schemaVersion": 1,
  "occurredAt": "2026-08-31T09:31:09Z",
  "producer": "listing-service",
  "savedSearchId": "ss_4b1e07",
  "sellerId": "sel_0142",
  "listingId": "lst_c40a19",
  "matchedAt": "2026-08-31T09:31:09Z"
}
```

`sellerId` here is the *owner of the saved search*, not the seller of the
listing. The matched listing's own `sellerId` is not carried; a consumer that
needs it reads `GET /listings/{id}`.

The event says a match happened. It does not say a notification was sent and it
carries no `channel` — the choice between `push` and `email` belongs to the
`Notification` entity and to the notification service that owns it. Writing
`lastMatchedAt` back onto the saved search is the listing service's own job and
happens before the message is produced.

## No PII in payloads

No event payload may carry `Seller.email`, and none may carry any other personal
datum about an account: no phone number, no street address, no client IP, no
device identifier. A seller appears as `sellerId` and nothing else. Where a
consumer needs a human-readable name it resolves `displayName` itself against the
listing service. This is invariant 6 in `domain.md`, and it holds for the
warehouse for the same reason it holds here.

This is checkable, and the checks are cheap:

- Every registered payload schema sets `additionalProperties: false`. A field
  cannot reach a consumer without appearing in a schema first, which puts every new
  field in front of a reviewer.
- A CI check reads the registered schemas and fails on any property name
  matching `email`, `phone`, `address`, `ip`, `birth`, or on any schema that
  relaxes `additionalProperties`.
- A producer-side contract test asserts that the key set of a produced message
  equals the key set of its schema, so a payload cannot grow a field the schema
  does not know.
- The warehouse export rejects records with unknown columns rather than widening
  its table.

The limit of those checks is that they are structural. `title`, `description`,
`label` and `query` are user-authored free text, and a payload that carries any of
them carries whatever the user typed, phone number included. No structural check
catches that. `savedsearch.matched` sidesteps it by carrying identifiers only;
anything that does carry free text inherits the problem, and redacting free text is
not solved here.

## Deliberately unresolved

Two questions are open. They are exercise material for the backend planning
block and are recorded here as open, not as decided.

**Ordering across a partition change.** The per-`savedSearchId` guarantee above
holds only while the key stays `savedSearchId` *and* the partition count of the
topic never changes. Adding partitions rehashes keys, so messages for one saved
search can land in a different partition than its earlier ones and their relative
order is no longer guaranteed across the change. The same holds for any second
topic: two topics have independent partition assignment, so a consumer joining
both sees no order between them. What guarantee is actually required, whether it
should be enforced by the key, by a sequence number in the envelope, or not at all,
is not decided.

**Topic ownership after publication.** A topic is produced by the listing service,
but once another team consumes it the schema is a shared interface. Who may add a
field, who may remove one, who sets the compatibility mode in the schema registry,
who is called when a consumer breaks, and whether a consumer can veto a producer's
change — none of that is settled for `savedsearch.matched`, and nothing settles it
for anything published later. Nor is what happens when the consuming team wants a
field the producing team does not have a reason to emit.
