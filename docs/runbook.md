# Operator runbook

Use README commands on an isolated fresh dataset. Before any cutover: verify backups/restore, administrator exclusivity,
source jobs/DB roles, no unaccounted writers, healthy target, canonical reconciliation, API/security parity,
lag/freshness approval and business pause window. Do not treat the admin endpoints as safe independently of this
protocol.

`migration-tools/migrate.py` implements the local transfer. Keep `artifacts/reconciliation-*.json` and command
timestamps. Inspect `/internal/ownership/{cap}` on both services after failure. FROZEN is a safe unavailable state;
leave it in place until evidence identifies authority. Resume Catalog cutover if target activation already happened;
never overwrite a live target with stale source export.

Kafka outage: service transactions/outbox continue until storage capacity limits. Monitor oldest unpublished event and
backlog, restore broker, verify relay drains, compare orders to shipments/notifications/modern_orders. Never purge
unpublished events. Consumer exception requires inspecting schema and DLQ/error handler behavior; the default Spring
handler is not a production durable quarantine. Reconcile missed projection rows before trusting reports.

Saga MANUAL: inspect checkout state, reservation state and authorization by same ID. An ambiguous authorization must be
queried/retried with the same key before compensation. Record decision and POST recover with the appropriate existing
state. Failed compensation is a durable incident, not a successful cancellation.

Gateway/control DB outage: calls fail closed. Repair DB/service and restore persisted config; do not redirect writes to
alternate implementations. Modern Catalog outage after transfer cannot safely fall back to stale legacy storage; restore
modern or perform reverse migration with authoritative data accessible.

PostgreSQL loss: stop writes, restore tested backup+WAL, reconcile acknowledged operations and external effects, restore
authority only after review. Local volumes have no replication or backup automation. Do not claim zero data loss after
host failure.

Retirement checklist: no routed/internal/job traffic across representative peak and batch periods; no remaining direct
grants/shared SQL; projections caught up; archive retention/legal sign-off; supported recovery tested; remove code paths
then wait compatibility window; only then contract schema. This demo keeps source tables and delegating facade
intentionally.
