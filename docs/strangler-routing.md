# Routing and rollout

GET `/api/products/{numericId}` alone is eligible for modern/shadow routing. All writes use the legacy facade, which
consults ownership inside the transaction and delegates only after activation. Unknown endpoints fail closed; no
client-controlled destination URL is accepted. Existing authenticated principal hash determines cohort [0,99], stable
across JVMs/restarts. Tenant routing needs authenticated tenant claims; arbitrary tenant headers are intentionally
ignored.

LEGACY returns monolith; SHADOW returns legacy result and asynchronously compares with Catalog; CANARY selects principal
cohorts; MODERN routes product reads to Catalog. The shadow executor has 2 workers and queue 32, rejection records
dropped work. Shadow exceptions do not change the customer response. Comparison is canonical parsed JSON with no
nondeterministic fields in product v1. Shadow is not a completeness proof and must not carry PII without review.

Outbound connect limit is 2s; nested budgets are gateway 20s, legacy facade POST 15s and GET 3s, downstream services 5s.
Budgets decrease along the call chain; end-to-end client deadline should exceed the gateway limit. Safe read fallback
only handles modern server/network failures; it goes to the monolith, which delegates if modern owns data. Thus fallback
cannot secretly serve stale legacy stock/catalog after transfer. No write retry or cross-writer fallback exists. Central
DB-backed flags avoid independent instance configuration, but add a control-path dependency; fail closed on unavailable
control DB. Circuit-breaker/rate-limiter and cached signed config are future hardening, not implemented claims.

Promotion policy: zero critical parity/security mismatches; no dropped comparisons accepted as matches; p99 within a
measured agreed SLO; error budget acceptable by cohort; freshness within approved window; representative authorization
contract coverage. Example production observation: at least 30 minutes and sufficient samples per stage, including peak
load. Manual gates; no latency-only auto-promotion. Read percentage settings are exercised, not production traffic
proportions measured.
