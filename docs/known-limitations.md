# Explicit residual risks

This is an executable representative system, not an enterprise deployment certification. Payment is a simulator; no
capture/refund, real carrier or email provider. Customer/resource authorization is operator-scoped and must be extended
for shoppers. All service-to-service demo calls use a shared technical account; production needs per-service scopes and
rotation. Administrative APIs trust privileged operator sequencing; no distributed coordinator prevents two
administrators activating independent databases out of protocol.

Full snapshot copies are memory-bound and pause writes. Debezium is evaluated only. No Redis, no Cloud Gateway, no
external feature-flag vendor, no autonomous rollout promotion, no native circuit breaker/rate limiting. Gateway depends
on legacy control DB; shadow/fallback latency/queue behavior needs load evidence. Product comparison excludes errors;
shadow 404/error comparison and detailed latency-delta metrics need expansion.

Checkout activation changes synchronous response semantics to pending durable Saga and exposes operational fields;
clients must poll state. Operation IDs are globally unique through the gateway facade; direct service writers must be
isolated. Legacy historical order keys are retained, not migrated. Customer validity is checked remotely before commit;
deletion/concurrent customer changes need a lifecycle protocol. Catalog price is snapshotted, not globally transactional
with inventory/payment.

Saga uses DB locks during network requests, limited retries with no exponential backoff, and operator recovery after
ambiguity. It lacks reservation expiry, customer cancellation, capture/refund, and production settlement reconciliation.
Event projections eventually update fulfillment/reporting; default consumer error recovery is not a durable quarantine.
Event contracts beyond v1 and mixed-version deployment matrix need further tests.

Helm resources are templates requiring external databases, IAM/TLS/secrets/network policy and an image registry. Local
service roles are isolated by database; local passwords remain public demo defaults. Production PostgreSQL HA, Kafka
replication/retention, backup restore, regional fencing, privacy review and secret rotation are not demonstrated. No
source tables were destroyed. Capacity calculations are assumptions. Performance, chaos coverage and actual evidence are
recorded in verification.md; supplied scripts alone are not evidence of execution.

Catalog reads through the legacy facade return 503 while ownership is FROZEN. This explicitly prevents stale-source
reads during reverse synchronization; the transfer includes a bounded read interruption on that path as well as a write
pause. Direct modern snapshot reads remain independently gated.

The final hardened Docker replay now passes the expanded migration suite, complete repository failure suite and
postflight cross-service integrity audit; see verification.md. This verifies the scenarios exercised by those scripts,
not all 40 failure-catalog scenarios or production chaos coverage. Historical performance smoke measurements used host
JVMs and were not rerun against this revision; they are not Docker or production capacity benchmarks.

Legacy row-map product responses/export use `select *`; future column expansion needs an explicit compatibility
projection before DDL. Existing v1 fixtures cover the current five-field schema only.
