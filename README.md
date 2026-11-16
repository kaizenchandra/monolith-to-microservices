# Executable Strangler Fig migration

Java 21 / Spring Boot 3.5.16; PostgreSQL 17.6; Kafka 3.9.1. This is a locally verifiable migration laboratory, not a
production certification. Existing workspace examples are unchanged.

## Run

```sh
cd monolith-to-microservices
export JAVA_HOME=$(/usr/libexec/java_home -v 21) # macOS; use your JDK 21 path elsewhere
mvn verify                       # Docker required for PostgreSQL Testcontainers
mkdir -p artifacts
docker compose up -d --build
python3 integration-tests/bootstrap.py
python3 integration-tests/demo.py # fresh dataset required; mutates migration state
python3 integration-tests/failures.py
python3 integration-tests/postflight.py
```

Gateway: http://localhost:28082/api/products/1. Local users `reader`, `admin`, `service`, password `local-only`.
Credentials and cleartext connections are strictly localhost demonstration defaults. Grafana: http://localhost:53000
(`admin` / `local-only`); Prometheus: http://localhost:59090.

```sh
curl -u reader:local-only localhost:28082/api/products/1
python3 migration-tools/migrate.py sync catalog
python3 migration-tools/migrate.py route --mode SHADOW
python3 migration-tools/migrate.py route --mode CANARY --percent 10
python3 migration-tools/migrate.py cutover catalog
python3 migration-tools/migrate.py route --mode MODERN
python3 migration-tools/migrate.py rollback catalog # includes reverse copy of modern writes
python3 migration-tools/migrate.py cutover catalog
python3 migration-tools/migrate.py cutover inventory # pauses checkout
python3 migration-tools/migrate.py checkout          # activates Saga checkout
curl -u admin:local-only -H 'Content-Type: application/json' localhost:28082/api/orders \
  -d '{"id":"example-1","customer_id":1,"product_id":2,"quantity":1}'
curl -u reader:local-only localhost:28082/api/orders/example-1
```

Run the manual journey OR the automated demo on a fresh dataset. To reset **only this demo's disposable data**,
`docker compose down -v`, then `docker compose up -d`. Stop without deleting data with `docker compose stop`.

## Implemented boundaries

`legacy-monolith`: customer/catalog/stock/checkout/payment/shipment/notification/reporting baseline with one shared
database and ACID checkout. After extraction it retains customer, fulfillment, notification, reporting and historic
orders; events update modern order projections. Payments are an authorization simulator; notifications are durable
intents, not email delivery.

`services/catalog-service`: independent model, optimistic versioned product writes, tombstones, ACL replication, outbox.

`services/inventory-service`: independent stock/reservations, no-oversell conditional updates, idempotent
reserve/release, outbox.

`services/order-service`: durable orchestration and manual recovery. `services/payment-service`: idempotent sandbox
authorization/void. `strangler-gateway`: narrow servlet router, stable authenticated-principal cohorts, bounded read
shadowing, read fallback, centralized rollout settings. Spring Cloud Gateway is unnecessary for this intentionally
narrow routing contract.

`platform` shares security/HTTP/observability/outbox infrastructure only. No shared entities or mutable business tables.
`migration-tools` is a standard-library Python operator utility. `contract-tests` holds the consumer product contract
used against legacy and modern APIs. `helm` requires external IAM, Kafka and databases; it does not provision a
production platform.

## Verification and limits

See [verification](docs/verification.md) for actual execution evidence
and [known limitations](docs/known-limitations.md). The E2E script exercises legacy checkout, authorization, shadow
parity, rollout settings, fencing, modern writes, reverse synchronization, concurrent reservations and Saga
compensation. Maven tests use real PostgreSQL via Testcontainers. Benchmark commands are supplied; design targets are
not measured capacity.

Snapshots are full in-memory exports; continuous CDC is evaluated, not implemented. Catalog rollback is executable;
Inventory/checkout rollback after Saga activation is deliberately blocked pending workflow drain. No destructive legacy
table retirement is performed. This demo includes bounded write pauses and frozen legacy-read 503s during transfer. It
makes no zero-downtime claim. The final hardened Docker migration/failure run and historical host-JVM smoke measurements are detailed
in docs/verification.md.

Start
with [runbook](docs/runbook.md), [ownership](docs/data-ownership.md), [failure catalog](docs/failure-scenarios.md), [architecture review](docs/architecture-review.md).

The final hardened run passed all 14 Maven tests, 48 migration assertions, six failure checks and 14 postflight integrity checks.
Evidence is in `artifacts/final-hardened-run/` and [verification](docs/verification.md). The verified Compose environment is left
running in its migrated state; rerunning `demo.py` requires a fresh disposable dataset as described above.

The broker and monitoring ports can be overridden with `KAFKA_HOST_PORT`, `PROMETHEUS_HOST_PORT`, and `GRAFANA_HOST_PORT`.
This run used 59192, 59190 and 53100 respectively because the defaults belonged to other projects. To operate this run:

```sh
export KAFKA_HOST_PORT=59192 PROMETHEUS_HOST_PORT=59190 GRAFANA_HOST_PORT=53100
docker compose ps
```

Gateway remains at port 28082. Bootstrap scripts contain psql backslash commands; keep each command and its argument on one line.
PostgreSQL becomes healthy only after the complete role/database bootstrap marker is present.
