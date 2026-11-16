# Failure catalog: 40 scenarios

Test labels distinguish executed E2E/JUnit from supplied scripts and pending drills. Every scenario also carries a
security implication: integrity-sensitive mutations require authenticated service identity; incidents must preserve
audit evidence and avoid exposing payloads/secrets. Specific identity failures are expanded below.

## 1. Gateway misroutes endpoint

- Root cause: overbroad route rule.
- Data/business risk: wrong read or business effect.
- Security: authorization/identity boundary may be bypassed; revoke affected trust and audit access.
- Detection: route-labelled rates and contract probe.
- Prevention: allowlist method/path, writes use owner facade.
- Recovery: restore persisted flag and reconcile requests.
- Automated test: E2E path/role assertions; consult verification.md for actual execution.
- Residual risk: routing config coverage remains finite.

## 2. Different response contract

- Root cause: uncoordinated provider schema.
- Data/business risk: incorrect price/client breakage.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: consumer contract and shadow mismatch.
- Prevention: versioned cents/ID contract.
- Recovery: stop canary, repair adapter.
- Automated test: E2E product-v1 verification; consult verification.md for actual execution.
- Residual risk: unseen client assumptions.

## 3. Shadow performs write

- Root cause: unsafe method mirrored.
- Data/business risk: duplicate charge/reservation.
- Security: authorization/identity boundary may be bypassed; revoke affected trust and audit access.
- Detection: write audit and mutation counters.
- Prevention: GET product allowlist only.
- Recovery: disable shadow, reconcile affected operations.
- Automated test: review Router eligible predicate; write isolation test pending; consult verification.md for actual
  execution.
- Residual risk: GET semantics must remain pure.

## 4. Canary service unavailable

- Root cause: process or network loss.
- Data/business risk: read unavailability.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: route errors and health probes.
- Prevention: bounded timeout, safe read fallback.
- Recovery: restore service or revert reads before transfer.
- Automated test: outage script; consult verification.md for actual execution.
- Residual risk: post-transfer fallback still depends on modern authority.

## 5. Two authoritative writers

- Root cause: operator bypasses fence protocol.
- Data/business risk: divergent committed values.
- Security: authorization/identity boundary may be bypassed; revoke affected trust and audit access.
- Detection: compare ownership epochs on both DBs.
- Prevention: serialized admin, source fence before activate.
- Recovery: freeze both, identify and reconcile commits.
- Automated test: E2E ownership gates; hostile admin test pending; consult verification.md for actual execution.
- Residual risk: privileged operator can break protocol.

## 6. Backfill misses records

- Root cause: pagination/export bug.
- Data/business risk: missing catalog/stock.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: canonical key and field digest.
- Prevention: ordered full snapshot with tombstones.
- Recovery: reimport while target fenced.
- Automated test: reconciliation equality assertions; consult verification.md for actual execution.
- Residual risk: large snapshots untested.

## 7. CDC connector unavailable

- Root cause: connector/slot failure.
- Data/business risk: stale target, retained WAL disk.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: LSN/apply lag and slot WAL bytes.
- Prevention: durable offsets, disk alerts.
- Recovery: restart or controlled resnapshot.
- Automated test: not implemented; production CDC test required; consult verification.md for actual execution.
- Residual risk: CDC is evaluated only.

## 8. CDC duplicate

- Root cause: at-least-once delivery.
- Data/business risk: duplicate effects.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: duplicate inbox/version counts.
- Prevention: event identity or record version.
- Recovery: ignore duplicate atomically.
- Automated test: Catalog replay JUnit; consult verification.md for actual execution.
- Residual risk: no real CDC transport tested.

## 9. CDC out of order

- Root cause: parallel consumer ordering.
- Data/business risk: old data replaces new.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: per-record version and reconciliation.
- Prevention: conditional newer-version apply.
- Recovery: replay ordered state or repair.
- Automated test: Catalog out-of-order JUnit; consult verification.md for actual execution.
- Residual risk: multi-table transaction grouping untested.

## 10. Snapshot overlap

- Root cause: wrong source position boundary.
- Data/business risk: duplicate/missing changes.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: position continuity and digest.
- Prevention: one connector snapshot-offset lifecycle or fenced final copy.
- Recovery: fence and reconcile/resnapshot.
- Automated test: E2E final snapshot transfer; consult verification.md for actual execution.
- Residual risk: production CDC boundary not exercised.

## 11. Commit during cutover

- Root cause: in-flight source request.
- Data/business risk: lost acknowledged update.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: owner lock wait and final digest.
- Prevention: shared lock spans write, exclusive fence drains.
- Recovery: wait drain then final copy.
- Automated test: fencing tests; interleaving test pending; consult verification.md for actual execution.
- Residual risk: ad hoc DB writers bypass application locks.

## 12. Stale target

- Root cause: replication lag/precopy drift.
- Data/business risk: stale prices/stock.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: versions and exact hashes.
- Prevention: final source fence and recopy.
- Recovery: keep target fenced, repair.
- Automated test: E2E reconciliation; consult verification.md for actual execution.
- Residual risk: live canary requires freshness budget.

## 13. Backfill crash

- Root cause: worker termination.
- Data/business risk: partial target data.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: import transaction status.
- Prevention: transactional import, no target writes.
- Recovery: rerun while fenced.
- Automated test: failure script crash gate; consult verification.md for actual execution.
- Residual risk: full snapshot memory use.

## 14. Totals mismatch

- Root cause: translation or omission.
- Data/business risk: financial/inventory corruption.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: per-record diff plus sums.
- Prevention: integer cents, full record comparisons.
- Recovery: block transfer and diagnose.
- Automated test: reconciliation report; consult verification.md for actual execution.
- Residual risk: provider settlement absent.

## 15. Legacy schema changes

- Root cause: uncoordinated DDL.
- Data/business risk: ACL/CDC parse errors.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: migration CI and connector alerts.
- Prevention: expand-contract, explicit export fields.
- Recovery: hold rollout, restore compatible adapter.
- Automated test: contract fixture; DDL drill pending; consult verification.md for actual execution.
- Residual risk: real legacy versions unknown.

## 16. Incompatible modern schema

- Root cause: destructive Flyway.
- Data/business risk: old pods fail/data loss.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: migration smoke tests and errors.
- Prevention: additive migrations, compatibility window.
- Recovery: forward repair/restore, no blind down migration.
- Automated test: PostgreSQL Flyway tests; consult verification.md for actual execution.
- Residual risk: DDL locking under load unmeasured.

## 17. Rollback after new writes

- Root cause: route-only rollback assumption.
- Data/business risk: lost modern commits.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: ownership/digest audit.
- Prevention: reverse synchronization while both fenced.
- Recovery: Catalog reverse transfer; block Inventory rollback.
- Automated test: E2E changes survive Catalog rollback; consult verification.md for actual execution.
- Residual risk: reverse copy needs modern DB available.

## 18. Migration lock

- Root cause: large ALTER or long transaction.
- Data/business risk: write outage.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: pg_locks and query latency.
- Prevention: lock_timeout, concurrent index, small backfills.
- Recovery: cancel DDL, retry in approved window.
- Automated test: load locking drill pending; consult verification.md for actual execution.
- Residual risk: production DDL timings unknown.

## 19. Gateway fails

- Root cause: pod/node failure.
- Data/business risk: entrypoint unavailable.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: health/SLO alerts.
- Prevention: multiple replicas, PDB, ingress.
- Recovery: replace pod, keep owner state.
- Automated test: container restart drill pending; consult verification.md for actual execution.
- Residual risk: single local gateway.

## 20. Redis unavailable

- Root cause: cache outage.
- Data/business risk: load amplification/stale reads.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: cache errors and DB load.
- Prevention: Redis omitted until justified.
- Recovery: bypass cache only within capacity budget.
- Automated test: not applicable to implementation; consult verification.md for actual execution.
- Residual risk: future cache consistency protocol needed.

## 21. Kafka unavailable

- Root cause: broker/storage/network loss.
- Data/business risk: delayed fulfillment/reporting.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: publish failure and outbox age.
- Prevention: local outbox, bounded relay.
- Recovery: restore broker then reconcile drain.
- Automated test: outage script; consult verification.md for actual execution.
- Residual risk: single-broker demo storage limits.

## 22. PostgreSQL primary fails

- Root cause: node/disk failure.
- Data/business risk: unavailable or lost data.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: DB health/WAL/backup alerts.
- Prevention: HA and tested backups in production.
- Recovery: fence, restore, reconcile external outcomes.
- Automated test: database outage drill pending; consult verification.md for actual execution.
- Residual risk: local no HA/backup guarantee.

## 23. ACL amount wrong

- Root cause: units/rounding mistake.
- Data/business risk: wrong payment amount.
- Security: authorization/identity boundary may be bypassed; revoke affected trust and audit access.
- Detection: exact cents parity and totals.
- Prevention: minor-unit integers and checked multiply.
- Recovery: stop writes, reconcile affected orders.
- Automated test: product contract and E2E cents; consult verification.md for actual execution.
- Residual risk: multi-currency not supported.

## 24. Identifier collision

- Root cause: new mapping namespace.
- Data/business risk: wrong entity/idempotency conflict.
- Security: authorization/identity boundary may be bypassed; revoke affected trust and audit access.
- Detection: PK and intent mismatch errors.
- Prevention: retain legacy product IDs, global operation IDs.
- Recovery: quarantine conflicting records.
- Automated test: conflicting duplicate E2E; consult verification.md for actual execution.
- Residual risk: external ID governance needed.

## 25. Order twice

- Root cause: retry/lost response.
- Data/business risk: duplicate fulfillment/payment.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: unique intent and event IDs.
- Prevention: PK intent key, existing legacy ID check.
- Recovery: return original operation.
- Automated test: E2E duplicate before/after transfer; consult verification.md for actual execution.
- Residual risk: direct service access must be isolated.

## 26. Payment timeout

- Root cause: effect commits but response lost.
- Data/business risk: unknown authorization.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: Saga attempts/MANUAL and provider lookup.
- Prevention: same idempotency key, no blind compensation.
- Recovery: retry same key then manual reconciliation.
- Automated test: timeout drill pending; consult verification.md for actual execution.
- Residual risk: provider simulator only.

## 27. Reserve then Order crash

- Root cause: remote/local commit gap.
- Data/business risk: orphan reservation.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: Saga step and reservation reconciliation.
- Prevention: stable key, replay durable step.
- Recovery: resume worker same ID or manual recovery.
- Automated test: recovery script; consult verification.md for actual execution.
- Residual risk: no reservation expiry.

## 28. Compensation fails

- Root cause: dependency outage.
- Data/business risk: stock locked/authorization outstanding.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: COMPENSATING/MANUAL alert.
- Prevention: durable compensation, idempotent void/release.
- Recovery: restore dependency then resume compensation.
- Automated test: E2E insufficient stock compensation; consult verification.md for actual execution.
- Residual risk: manual escalation has no automated ticket.

## 29. Duplicate consumer event

- Root cause: relay ack/commit gap.
- Data/business risk: duplicate shipment/notification.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: inbox key and projection uniqueness.
- Prevention: inbox and effect same DB transaction.
- Recovery: replay safely.
- Automated test: outage/replay script; consult verification.md for actual execution.
- Residual risk: default consumer quarantine requires hardening.

## 30. Inconsistent flags

- Root cause: instance-local config drift.
- Data/business risk: unstable cohort behavior.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: central routing row and route distribution.
- Prevention: read same persisted config each request.
- Recovery: restore one authoritative config.
- Automated test: E2E stages; consult verification.md for actual execution.
- Residual risk: DB lookup becomes control dependency.

## 31. Legacy auth works/new fails

- Root cause: issuer/audience/scope mismatch.
- Data/business risk: denied legitimate clients.
- Security: authorization/identity boundary may be bypassed; revoke affected trust and audit access.
- Detection: 401/403 by route.
- Prevention: issuer+audience validation, contract probes.
- Recovery: pause canary, fix trust configuration.
- Automated test: Basic role E2E; real OIDC pending; consult verification.md for actual execution.
- Residual risk: real SSO not supplied.

## 32. Session exposes access

- Root cause: forged header/cookie trust.
- Data/business risk: unauthorized customer data.
- Security: authorization/identity boundary may be bypassed; revoke affected trust and audit access.
- Detection: security audit and negative authorization tests.
- Prevention: validated token exchange, no cookie propagation.
- Recovery: revoke tokens and repair bridge.
- Automated test: no session bridge implemented; consult verification.md for actual execution.
- Residual risk: shopper row authorization pending.

## 33. Service auth fails

- Root cause: expired/mis-scoped token.
- Data/business risk: Saga/delegation failures.
- Security: authorization/identity boundary may be bypassed; revoke affected trust and audit access.
- Detection: 401/403 dependency errors.
- Prevention: per-service scopes and rotation.
- Recovery: rotate token, resume same operation.
- Automated test: production JWT drill pending; consult verification.md for actual execution.
- Residual risk: static token snapshot at startup.

## 34. Modern overloads legacy DB

- Root cause: unbounded backfill/shadow.
- Data/business risk: source contention.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: pool/DB QPS and latency.
- Prevention: bounded queues, snapshot windows, load budget.
- Recovery: reduce shadow/pause copy.
- Automated test: no production load proof; consult verification.md for actual execution.
- Residual risk: full export remains expensive.

## 35. Gateway hot spot

- Root cause: all reads synchronous flags/threads.
- Data/business risk: latency and exhaustion.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: HTTP latency/CPU/queue.
- Prevention: horizontal scaling, bounded work.
- Recovery: reduce shadow, scale gateway within DB budget.
- Automated test: benchmark harness; consult verification.md for actual execution.
- Residual risk: no rate limiter/circuit breaker.

## 36. Cohort differs

- Root cause: data/authorization semantics.
- Data/business risk: subset customers harmed.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: cohort-specific parity/security probes.
- Prevention: stable principal hash, semantic gates.
- Recovery: revert reads and repair.
- Automated test: canary stages E2E; consult verification.md for actual execution.
- Residual risk: one principal does not prove distribution.

## 37. Mixed versions

- Root cause: incompatible API/events during rolling.
- Data/business risk: consumer parse failures.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: version-specific contract alerts.
- Prevention: additive schemas, dual-compatible consumer.
- Recovery: stop rollout, keep v1 compatibility.
- Automated test: v1 consumer tests only; consult verification.md for actual execution.
- Residual risk: mixed-version matrix pending.

## 38. Regional outage cutover

- Root cause: loss of authority evidence.
- Data/business risk: split brain/data loss.
- Security: retain authenticated operator/service boundaries during recovery; do not expose incident data.
- Detection: quorum/region control alerts.
- Prevention: single authority, stop on ambiguity.
- Recovery: freeze until evidence restored.
- Automated test: not implemented; no regional failover; consult verification.md for actual execution.
- Residual risk: regional HA absent.

## 39. Legacy job writes moved data

- Root cause: hidden batch bypass.
- Data/business risk: divergence.
- Security: authorization/identity boundary may be bypassed; revoke affected trust and audit access.
- Detection: database audit and stale source versions.
- Prevention: inventory all writers, revoke legacy grants.
- Recovery: disable job, reconcile before resume.
- Automated test: no mutating batch in demo; consult verification.md for actual execution.
- Residual risk: real batch discovery required.

## 40. Hidden retired traffic

- Root cause: untracked internal dependency.
- Data/business risk: failed calls or stale writes.
- Security: authorization/identity boundary may be bypassed; revoke affected trust and audit access.
- Detection: dependency traces and DB access audit.
- Prevention: observe compatibility window before removal.
- Recovery: restore compatible facade/data access safely.
- Automated test: no destructive retirement executed; consult verification.md for actual execution.
- Residual risk: real retirement evidence unavailable.

