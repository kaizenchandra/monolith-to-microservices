# Mandatory architecture review

Answers apply to this implementation and distinguish design guarantees from missing production evidence.

## 1. What is Strangler Fig?

Route capabilities incrementally from an existing system to independently owned replacements while keeping a
compatibility facade. One writer per dataset is the key invariant, not the router itself.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 2. Why incremental over big bang?

Smaller exposure and independently verifiable gates limit business blast radius. Coexistence costs more temporarily;
rollback needs state compatibility.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 3. Identify service boundaries?

Follow business invariants, transaction calls, schema ownership and change patterns. Catalog is simpler than splitting
checkout stock/payment atomicity.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 4. How DDD helps?

Separate language and responsibility before selecting deployment units. It reveals where eventual consistency is
acceptable and where local ACID belongs.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 5. Bounded context?

A boundary where terms, models and invariants have one meaning and owner. Product price and inventory quantity use
product reference without sharing mutable entities.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 6. ACL?

A translation boundary protecting a new model from legacy representation. Catalog maps explicit fields and cents, with
typed validation and version semantics.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 7. Prevent model leakage?

Use independent records/contracts, no shared legacy JPA or cross-database repositories. Shared platform code is
technical infrastructure only.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 8. Introduce gateway unchanged?

Begin LEGACY-only and characterize headers/status/body/security against direct provider. The extra hop adds
latency/failure, so measure rather than presume transparency.

Operational/recovery/performance trade-off: flags select eligible reads only; write authority remains independent. Stop
promotion on correctness/security evidence gaps, restore safe reads or run reverse migration. Shadow traffic and an
extra routing hop consume capacity; measure each phase. See strangler-routing.md.

## 9. Endpoint routing?

Allowlist method/path and stable authenticated cohorts. Write facade consults ownership; endpoint flags cannot select a
competing writer.

Operational/recovery/performance trade-off: flags select eligible reads only; write authority remains independent. Stop
promotion on correctness/security evidence gaps, restore safe reads or run reverse migration. Shadow traffic and an
extra routing hop consume capacity; measure each phase. See strangler-routing.md.

## 10. Safe shadows?

Copy eligible pure reads into a bounded executor and compare canonical JSON asynchronously. Queue rejection and errors
are recorded, never customer failures or silent matches.

Operational/recovery/performance trade-off: flags select eligible reads only; write authority remains independent. Stop
promotion on correctness/security evidence gaps, restore safe reads or run reverse migration. Shadow traffic and an
extra routing hop consume capacity; measure each phase. See strangler-routing.md.

## 11. Why isolate shadow writes?

A copied mutation can charge twice or consume stock. This implementation never shadows mutations; simulators would need
isolated effects and state.

Operational/recovery/performance trade-off: flags select eligible reads only; write authority remains independent. Stop
promotion on correctness/security evidence gaps, restore safe reads or run reverse migration. Shadow traffic and an
extra routing hop consume capacity; measure each phase. See strangler-routing.md.

## 12. Canary implementation?

Hash authenticated principal modulo 100 against central percentage. Stable cohort avoids bouncing implementations but
one principal cannot demonstrate full distribution.

Operational/recovery/performance trade-off: flags select eligible reads only; write authority remains independent. Stop
promotion on correctness/security evidence gaps, restore safe reads or run reverse migration. Shadow traffic and an
extra routing hop consume capacity; measure each phase. See strangler-routing.md.

## 13. Progression metrics?

Parity/security, freshness, cohort errors, tail latency and sufficient observation. No automatic latency-only promotion;
unknown evidence blocks advancement.

Operational/recovery/performance trade-off: flags select eligible reads only; write authority remains independent. Stop
promotion on correctness/security evidence gaps, restore safe reads or run reverse migration. Shadow traffic and an
extra routing hop consume capacity; measure each phase. See strangler-routing.md.

## 14. API compatibility?

Retain cents/IDs/status behavior with explicit provider/consumer fixtures. New checkout pending state is intentional and
must be client-approved; product v1 remains stable.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 15. Characterization tests?

Tests capture observed business behavior before replacement, including errors and authorization. They preserve a
baseline without declaring every historical behavior desirable.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 16. Migrate shared DB?

Map owners, separate schemas/databases, copy coherently, fence source, reconcile then activate. Physical separation
without removing shared writes does not achieve independence.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 17. Database-per-service difficulty?

Cross-context joins and transactions become remote contracts and explicit workflows. Local ACID remains; new distributed
failure/consistency costs must be justified.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 18. CDC?

Capture committed source changes for downstream application without app-level dual-write. At-least-once delivery
requires idempotent apply and verified snapshot boundary.

Operational/recovery/performance trade-off: retain durable positions/versions and a single authority; on ambiguity keep
writers fenced and reconcile before reopening. Replication, backfill and validation consume source I/O and extend the
recovery window. See database-migration.md and rollout-rollback.md.

## 19. Debezium/PostgreSQL?

Connector snapshots a source and streams pgoutput WAL changes from retained offsets/slots. Requires logical replication
configuration and WAL capacity; evaluated here, not executed.

Operational/recovery/performance trade-off: retain durable positions/versions and a single authority; on ambiguity keep
writers fenced and reconcile before reopening. Replication, backfill and validation consume source I/O and extend the
recovery window. See database-migration.md and rollout-rollback.md.

## 20. Avoid snapshot/CDC gaps?

Use one consistent connector snapshot/offset lifecycle and retain changes until applied. Demo instead fences source
before final full copy, accepting a measured write pause.

Operational/recovery/performance trade-off: retain durable positions/versions and a single authority; on ambiguity keep
writers fenced and reconcile before reopening. Replication, backfill and validation consume source I/O and extend the
recovery window. See database-migration.md and rollout-rollback.md.

## 21. Duplicate CDC events?

Deduplicate identity and enforce record-version conditional updates. Dedup state must survive restart and outlive replay
retention.

Operational/recovery/performance trade-off: retain durable positions/versions and a single authority; on ambiguity keep
writers fenced and reconcile before reopening. Replication, backfill and validation consume source I/O and extend the
recovery window. See database-migration.md and rollout-rollback.md.

## 22. Out-of-order events?

Use aggregate versions and reject older state; group transactional changes where required. Kafka partition order alone
cannot prove cross-table business transaction atomicity.

Operational/recovery/performance trade-off: retain durable positions/versions and a single authority; on ambiguity keep
writers fenced and reconcile before reopening. Replication, backfill and validation consume source I/O and extend the
recovery window. See database-migration.md and rollout-rollback.md.

## 23. Dual writes dangerous?

Two independent commits have a partial-success window with ambiguous outcomes. Outbox or CDC plus
ownership/reconciliation avoids pretending there is distributed ACID.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 24. Transactional Outbox?

Commit state and event record in one local transaction, publish later. Broker ack/DB commit gap permits duplicates;
inbox suppresses repeated effects.

Operational/recovery/performance trade-off: retain durable positions/versions and a single authority; on ambiguity keep
writers fenced and reconcile before reopening. Replication, backfill and validation consume source I/O and extend the
recovery window. See database-migration.md and rollout-rollback.md.

## 25. Reconcile?

Compare ordered keys, complete fields, versions and tombstones using deterministic digests plus sums. Counts alone miss
wrong values; comparisons during live writes need snapshot semantics.

Operational/recovery/performance trade-off: retain durable positions/versions and a single authority; on ambiguity keep
writers fenced and reconcile before reopening. Replication, backfill and validation consume source I/O and extend the
recovery window. See database-migration.md and rollout-rollback.md.

## 26. Safe write transfer?

Fence/drain source, final synchronize, exact reconcile, activate target, enable delegation. Epoch CAS prevents stale
admin commands; privileged protocol bypass remains a risk.

Operational/recovery/performance trade-off: retain durable positions/versions and a single authority; on ambiguity keep
writers fenced and reconcile before reopening. Replication, backfill and validation consume source I/O and extend the
recovery window. See database-migration.md and rollout-rollback.md.

## 27. Expand-contract?

Add compatible schema, backfill and validate, switch clients, remove later. Destructive contraction waits for all old
versions and rollback windows.

Operational/recovery/performance trade-off: retain durable positions/versions and a single authority; on ambiguity keep
writers fenced and reconcile before reopening. Replication, backfill and validation consume source I/O and extend the
recovery window. See database-migration.md and rollout-rollback.md.

## 28. Avoid downtime migration failure?

Measure locks/backfill load, enforce lock timeout, preserve compatibility and rehearse failures. No generic
zero-downtime guarantee; this lab intentionally pauses writes.

Operational/recovery/performance trade-off: retain durable positions/versions and a single authority; on ambiguity keep
writers fenced and reconcile before reopening. Replication, backfill and validation consume source I/O and extend the
recovery window. See database-migration.md and rollout-rollback.md.

## 29. Rollback after modern writes?

Reverse-copy authoritative data under fences before restoring source ownership. Catalog supports it; Inventory/checkout
requires drain/reconciliation and is blocked here.

Operational/recovery/performance trade-off: retain durable positions/versions and a single authority; on ambiguity keep
writers fenced and reconcile before reopening. Replication, backfill and validation consume source I/O and extend the
recovery window. See database-migration.md and rollout-rollback.md.

## 30. Migrate global ACID?

Identify local invariant groups; split workflow into durable steps with compensation. Avoid turning a local transaction
into network calls while claiming atomicity.

Operational/recovery/performance trade-off: local state commits atomically; remote effects are eventually reconciled by
stable keys and durable retries. Manual states require intervention. Network round trips and row-lock occupancy cap
throughput; sandbox guarantees do not extend to real payment providers. See saga.md.

## 31. When Saga?

When one business workflow spans independently owned stores and can tolerate explicit intermediate states. A Saga is
unnecessary for a single Catalog update; it cannot undo irreversible effects magically.

Operational/recovery/performance trade-off: local state commits atomically; remote effects are eventually reconciled by
stable keys and durable retries. Manual states require intervention. Network round trips and row-lock occupancy cap
throughput; sandbox guarantees do not extend to real payment providers. See saga.md.

## 32. Compensation failure?

Keep durable COMPENSATING/MANUAL state, retry idempotently, expose operator recovery. Never mark cancellation complete
before void/release evidence.

Operational/recovery/performance trade-off: local state commits atomically; remote effects are eventually reconciled by
stable keys and durable retries. Manual states require intervention. Network round trips and row-lock occupancy cap
throughput; sandbox guarantees do not extend to real payment providers. See saga.md.

## 33. Prevent double reservations?

Stable operation key, same-intent validation, row locking and atomic stock condition. At ownership transfer copy
reservation identities as well as quantities.

Operational/recovery/performance trade-off: local state commits atomically; remote effects are eventually reconciled by
stable keys and durable retries. Manual states require intervention. Network round trips and row-lock occupancy cap
throughput; sandbox guarantees do not extend to real payment providers. See saga.md.

## 34. Prevent duplicate payments?

Use provider key scoped to one intent and amount; retry ambiguous outcomes with the same key. Simulator
authorization/void is implemented; real capture/settlement needs provider reconciliation.

Operational/recovery/performance trade-off: local state commits atomically; remote effects are eventually reconciled by
stable keys and durable retries. Manual states require intervention. Network round trips and row-lock occupancy cap
throughput; sandbox guarantees do not extend to real payment providers. See saga.md.

## 35. Modernize SSO?

Bridge validated legacy session to short-lived audience-scoped token where appropriate. No blind cookie/header trust;
actual issuer/session integration is absent from supplied legacy environment.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 36. End shared DB coupling?

Revoke legacy writes/grants, remove cross-schema SQL and track remaining dependencies explicitly. Local databases share
infrastructure but not business tables; production least privilege is prerequisite.

Operational/recovery/performance trade-off: retain durable positions/versions and a single authority; on ambiguity keep
writers fenced and reconcile before reopening. Replication, backfill and validation consume source I/O and extend the
recovery window. See database-migration.md and rollout-rollback.md.

## 37. Legacy jobs at cutover?

Inventory every writer including scripts/jobs; fence or revoke each before final boundary. Application owner locks
cannot protect against an uncoordinated privileged SQL script.

Operational/recovery/performance trade-off: retain durable positions/versions and a single authority; on ambiguity keep
writers fenced and reconcile before reopening. Replication, backfill and validation consume source I/O and extend the
recovery window. See database-migration.md and rollout-rollback.md.

## 38. Measure parity?

Normalize documented nondeterminism, compare records/contracts/errors/authorization and freshness. Product v1 has no
nondeterministic fields; shadow coverage is finite, not universal proof.

Operational/recovery/performance trade-off: flags select eligible reads only; write authority remains independent. Stop
promotion on correctness/security evidence gaps, restore safe reads or run reverse migration. Shadow traffic and an
extra routing hop consume capacity; measure each phase. See strangler-routing.md.

## 39. Benchmark fairly?

Same dataset, warmup, offered concurrency, auth, hardware and monitoring for every routing stage. Measure
offered/achieved RPS and tail/errors alongside resource/lag impact; no invented comparisons.

Operational/recovery/performance trade-off: flags select eligible reads only; write authority remains independent. Stop
promotion on correctness/security evidence gaps, restore safe reads or run reverse migration. Shadow traffic and an
extra routing hop consume capacity; measure each phase. See strangler-routing.md.

## 40. Canary failure?

Before ownership transfer safe read fallback/route reduction may help. After transfer restore authoritative service or
reverse migrate; stale-table fallback is unsafe.

Operational/recovery/performance trade-off: flags select eligible reads only; write authority remains independent. Stop
promotion on correctness/security evidence gaps, restore safe reads or run reverse migration. Shadow traffic and an
extra routing hop consume capacity; measure each phase. See strangler-routing.md.

## 41. Rollback gates?

Classify read/write/schema/event/retirement changes and required state compatibility. Recovery availability and
reverse-copy evidence matter more than a switch percentage.

Operational/recovery/performance trade-off: flags select eligible reads only; write authority remains independent. Stop
promotion on correctness/security evidence gaps, restore safe reads or run reverse migration. Shadow traffic and an
extra routing hop consume capacity; measure each phase. See strangler-routing.md.

## 42. Rolling schema compatibility?

Use additive fields, tolerant v1 consumers and expand-contract database evolution. Production requires actual
mixed-version contract execution; current tests cover v1.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 43. Remove legacy safely?

Observe external/internal/job traffic and DB grants, archive, wait retention, remove paths, then tables. This demo
deliberately retains facade/tables until evidence supports removal.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 44. When keep monolith?

Keep it when cohesive local invariants, small teams or operational costs outweigh independent scaling/change benefits.
Microservices add network failures, data duplication, deployment and on-call costs.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

## 45. Evidence for complete migration?

Passed behavioral/security/contracts, reconciled ownership, recovery/restore drills, measured SLOs, no hidden
dependencies and approved retention. New services returning 200 is insufficient; known limitations remain explicit.

Operational/recovery/performance trade-off: preserve local ownership/ACID and explicit contract compatibility; failure
requires stopping exposure, repairing or restoring with reconciliation. Independence improves deployment options but
adds network/storage/on-call costs; measured evidence is required before claiming speed or availability. See runbook.md
and known-limitations.md.

