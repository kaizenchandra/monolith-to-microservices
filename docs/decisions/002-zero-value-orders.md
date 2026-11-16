# ADR 002 — Preserve zero-priced catalog semantics

Catalog and legacy checkout accept zero-priced items. Orders/Payment initially required positive totals, which would
reject an accepted legacy case. Additive V2 migrations relax these checks to nonnegative totals; the sandbox records a
zero-value authorization with normal intent identity and outbox semantics. Negative totals remain rejected. A real
payment adapter must skip provider authorization for a free order. Deploy compatible Payment code/schema before exposing
zero-total Orders during a rolling upgrade. The new checks take table locks; apply during the approved migration window
with lock-timeout safeguards. PostgreSQL schema and payment idempotency tests cover this behavior.

Strict record deserialization distinguishes explicitly provided zero from an omitted/null monetary field. Missing
product prices and authorization amounts are rejected before mutation. After zero-value authorizations exist, keep a
compatible Payment binary during rollback; the old positive-only validator cannot safely resume those intents.
