# Measurement protocol

After E2E, `python3 benchmarks/run.py` measures catalog reads for six routing configurations with k6 (4 VUs, 10s/stage).
Results are raw k6 summary JSON/logs and benchmark-results.json. It reverse-migrates Catalog first for a real legacy
read baseline, then restores modern ownership. A single reader principal means canary percentages are configuration
stages, not achieved population proportions. The target database is an exact frozen replica during the read comparison;
benchmark does not measure modern write throughput.

This is a smoke measurement on a shared local engine, not an enterprise sizing result. Compare dedicated hosts, matching
warmups/datasets/connection settings, per-cohort workload, fixed offered arrival rate and longer representative windows
before claiming improvement. Capture offered/achieved rates, p50/p95/p99 (configure
`--summary-trend-stats avg,med,p(95),p(99)`), database QPS/lock waits, pools, JVM CPU/heap/GC, broker backlog and
cutover/rollback wall times. Those additional measurements are unverified unless in artifacts. No 100k-RPS or
availability claim follows from this harness.

The recorded run used host Java 21 JVMs for Legacy/Gateway/Catalog, with tracing disabled, and the same Compose
PostgreSQL/Kafka databases. The shared engine's container startup exceeded health deadlines; this topology is recorded
rather than represented as a successful Docker performance run. Host runner:

```sh
docker compose stop legacy gateway catalog inventory order payment
python3 integration-tests/native.py start
python3 benchmarks/run.py
python3 integration-tests/outbox.py
python3 integration-tests/native.py stop
```

Run only after the full migration demo has initialized its dataset; this runner hosts catalog operations only. Keep
JAVA_HOME set to Java 21. Start PostgreSQL/Kafka first. Do not run containerized and host JVMs on the same ports
concurrently. Use `docker compose up -d --build` to restore the full environment after host processes have stopped.
