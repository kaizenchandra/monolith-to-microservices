# Signals and evidence

Actuator exposes health/probes and Prometheus to SERVICE identity. Router counters label route/method only; shadow
labels match/mismatch/error/dropped only. No order/customer IDs label metrics. HTTP latency, JVM memory/GC/CPU and
Hikari pool metrics come from Micrometer. Relay failure counter identifies broker trouble. Grafana datasource/dashboard
and local OTLP collector debug exporter are provisioned by Compose. Exported traces are observable in collector logs,
not a durable trace backend.

Operator SQL queries:

```sql
select count(*), min(created_at) from outbox where not published;
select state,count(*) from checkout group by state;
select * from ownership order by capability;
select mode,percent from routing;
```

Run each query against its owning database. No CDC lag metric is invented because CDC is not installed. Reconciliation
JSON includes digest/rows/totals; gateway counters record parity. Saga state/backlog queries supplement dashboards until
a production metrics exporter is added. Metrics alone do not prove financial correctness.

Suggested alerts: critical shadow mismatch >0 over an approved sample; shadow error/dropped >0 blocks promotion; outbox
oldest unpublished >60s; Saga MANUAL >0; pool active near max >5min; HTTP 5xx error budget burn; target DB version or
replication freshness beyond approved gate. Alerts must distinguish before/after ownership rollback options. Local
thresholds are planning guidance, not validated production SLOs.

The configured Boot RestClient.Builder supplies Micrometer client observation/tracing propagation; the custom adapter
preserves it when setting timeouts and service credentials. HTTP histogram buckets are enabled for the Grafana quantile
query. Console logs use Boot Logstash JSON with MDC trace/correlation fields. End-to-end trace export and Kubernetes
alert behavior remain unverified.
