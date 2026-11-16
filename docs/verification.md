# Final hardened migration verification — 2026-11-15

**Passed:** one complete Maven reactor run, fresh-database bootstrap, the expanded migration journey, the complete repository Docker failure suite, and a cross-service postflight audit. These results supersede the earlier incomplete combined replay.

Run completed at `2026-11-15T16:44:39.078724+00:00`. All application services ran in Docker; no host-JVM fallback was used. Java 21.0.13, Spring Boot 3.5.16, PostgreSQL 17.6, Kafka 3.9.1, Maven 3.10.0 and Docker 29.8.2.

## Actual results

| Check | Result | Observed duration |
|---|---|---|
| `mvn -B verify` — complete reactor | 14 tests, zero failures/errors/skips | Maven reported 1 min 05 s |
| Fresh bootstrap and least-privilege role checks | Six databases and all 12 runtime/migrator roles passed | 0.4 s |
| `integration-tests/demo.py` | 48 assertions passed | 42.7 s |
| `integration-tests/failures.py` | 6 checks passed | 32.9 s |
| `integration-tests/postflight.py` | 14 checks passed | Not timed separately |
| Running image/build identity | All six running jar SHA-256 hashes match the verified Maven output | Not timed separately |

Durations are local observations, not availability or capacity guarantees. Startup was sequential to avoid simultaneous JVM bootstrap contention; all six services were healthy before the migration began. The full stack, including Prometheus/Grafana/OTel, was running during the suites.

## Failures found and repaired

1. **Fresh PostgreSQL initialization was broken.** Formatting had split psql `\connect` commands from their database arguments in `docker/init.sql` and `docker/roles.sql`. Initialization stopped after the legacy roles, and Catalog failed with SQLSTATE `28P01` because its migrator role did not exist. Both scripts now keep the meta-command and argument on one line. A completion marker is written only after all provisioning succeeds; PostgreSQL health requires both TCP readiness and that marker. The repaired initialization was rerun from empty demo volumes.

2. **Default broker/monitoring ports were already occupied.** Compose now accepts `KAFKA_HOST_PORT`, `PROMETHEUS_HOST_PORT`, and `GRAFANA_HOST_PORT`, including the external Kafka advertised listener. This run used 59192/59190/53100. The host-JVM helper honors the broker override too. Other projects were left running.

No Java business-code fixes were needed in this replay. The final rollback-read, ownership-aware pricing, monetary-validation and source-connection-release hardening all passed against the rebuilt application images.

## Migration and hardened regressions exercised

- Characterized legacy checkout and duplicate operation handling; preserved legacy order identity after extraction.
- Verified product v1 contracts, read authorization, shadow comparisons and configured canary stages. Canary settings are not a measured customer-cohort distribution.
- Rejected frozen source/replica writes, returned 503 for frozen legacy reads, and rejected a stale ownership epoch.
- Cut over Catalog, wrote through the modern authority, rejected stale versions and reverse-copied committed modern data during rollback.
- Cut over Inventory and raced 20 distinct reservations against the nine remaining units; exactly nine succeeded, with nonnegative stock and releases verified.
- Completed checkout Saga and insufficient-stock compensation.
- Rejected missing monetary fields, completed a zero-value Saga, verified idempotent zero authorization and rejected a conflicting payment amount.
- Rolled Catalog back while checkout remained modern, changed the legacy price while its modern replica remained stale, and confirmed that modern checkout used the authoritative legacy price. Catalog was then cut over again.

## Complete Docker failure suite

- saga resumes after dependency outage and worker restart.
- one reservation after replay.
- state commits while broker unavailable.
- outbox recovers and consumer projects.
- duplicate event suppresses duplicate fulfillment.
- cutover resumes after source delegation fence.

This is the complete executable repository failure suite, not a claim that all 40 documented failure scenarios or production chaos cases were injected.

## Postflight integrity evidence

- All six checkout workflows were terminal: five modern confirmed orders and one compensated cancellation. One historical order remained in legacy.
- Exactly one matching authorization per modern confirmed order, with identical monetary amounts.
- Order-confirmed event IDs matched the deduplicating inbox; shipment and notification intents existed once per confirmed/historical order.
- The report reconciled to **six orders and 20,594 minor currency units**.
- Active reservations matched confirmed order products/quantities, with no orphan active reservations. Available stock plus active reservations conserved the frozen migration baseline for both products.
- All four producer outboxes drained, and Catalog source/target records still matched exactly at audit time.
- Source facade authority and target Catalog/Inventory authority were MODERN. The audit switched catalog reads to MODERN-only and verified the API response.
- After the replay, the broker end-offset snapshot was `{'0': 38}` and committed consumer offsets were `{'0': 38}`. The audit waited through every captured partition boundary, strengthening the failure suite's generic commit-counter check: the replay was not left pending.

## Reproduce

Use Java 21. Start with fresh **demo-only** volumes; `demo.py` mutates the migration state. The current verified environment is left running in its migrated state for inspection. Remove/reset only this Compose project before replaying from Phase 1.

```sh
cd monolith-to-microservices
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export DOCKER_HOST=$(docker context inspect --format '{{.Endpoints.docker.Host}}')
export KAFKA_HOST_PORT=59192 PROMETHEUS_HOST_PORT=59190 GRAFANA_HOST_PORT=53100
mvn -B verify
# On the disposable demo dataset only, to replay from the beginning:
docker compose down -v
docker compose up -d --build
python3 integration-tests/bootstrap.py
python3 integration-tests/demo.py
python3 integration-tests/failures.py
python3 integration-tests/postflight.py
```

This run started PostgreSQL/Kafka/OTel first, then legacy, Catalog, Inventory, Payment, Order and Gateway one at a time, waiting for `/actuator/health` = UP at each step; Prometheus/Grafana followed. A dedicated clean runner can use normal Compose startup. CI now runs bootstrap and postflight checks around the migration/failure suites.

Gateway: http://localhost:28082/api/products/1. Grafana: http://localhost:53100. Prometheus: http://localhost:59190. All published ports remain loopback-bound. Credentials are the documented local-only demo credentials.

## Evidence files

- [Run summary](../artifacts/final-hardened-run/run-summary.json), [stage exit codes/times](../artifacts/final-hardened-run/suite-results.json).
- [Full Maven reactor log](../artifacts/final-hardened-run/maven-verify.log), [JUnit results](../artifacts/final-hardened-run/junit-results.json).
- [Cold-bootstrap checks](../artifacts/final-hardened-run/bootstrap-results.json), [initial bootstrap failure](../artifacts/final-hardened-run/bootstrap-first-attempt.log).
- [Migration results](../artifacts/final-hardened-run/e2e-results.json), [migration log](../artifacts/final-hardened-run/migration.log).
- [Failure results](../artifacts/final-hardened-run/failure-results.json), [failure log](../artifacts/final-hardened-run/failures.log).
- [Postflight report](../artifacts/final-hardened-run/postflight-results.json), [audit log](../artifacts/final-hardened-run/postflight.log).
- [Image/jar identity](../artifacts/final-hardened-run/image-verification.json), [source hashes](../artifacts/final-hardened-run/source-manifest.json), [startup readiness times](../artifacts/final-hardened-run/startup-results.json).
- [Final container logs](../artifacts/final-hardened-run/compose-final.log), [port overrides](../artifacts/final-hardened-run/compose.env).

## Remaining unverified scope

The prior combined migration/failure replay gate is now closed for these exercised scenarios. Production readiness remains open: Kubernetes rollout, real OIDC/mTLS and token rotation, real payment capture/refund/settlement, HA database/broker recovery, backup restore, regional fencing and the remaining failure-catalog scenarios were not executed. Inventory/checkout reverse migration after Saga activation remains deliberately unsupported. No legacy source tables were retired.

Performance was not rerun in this request. The earlier [host-JVM smoke measurements](../artifacts/benchmark-results.json) remain historical and do not certify this final revision, Docker throughput, target capacity or SLOs. Continuous CDC is still evaluated rather than implemented; transfers use fenced snapshots and bounded interruptions. No zero-downtime, host-failure zero-data-loss, exactly-once delivery or production-readiness claim is made.
