# Illustrative capacity, not benchmark results

Assume 100k catalog reads/s, 1k orders/s, 10M customers, 1KB product response, 2KB event, 95% read hit rate at an
approved cache/CDN, 500 uncached read RPS/pod measured separately, 60% utilization target. Instances = ceil(5000/(500*
0.6)) = 17; add failure-domain capacity, minimum 26 with loss of one of three equal zones. Without caching: ceil
(100000/300)=334 pods. Redis is not implemented because no cache staleness/ownership protocol has been justified or
benchmarked.

Connections: 17 pods * 8 = 136 Catalog DB connections, plus migrations/admin/replication reserve 20 = 156. Budget
against DB max connections; pool proxies and query profiling before scaling pods. At 334 pods the 2672 connections
require architectural change, not a larger HPA alone.

Cache memory: illustrative 1M hot objects * (1KB + 0.2KB overhead) * 2 replication / 0.75 utilization = 3.2GB. Customer
rows: 10M * 1KB * 2 index factor ~20GB before MVCC/WAL/backups. Migration duplicates live datasets: original + target +
snapshot staging + WAL/event retention; budget >=3x live catalog storage plus measured retained log volume.

Kafka: 4 events/order * 1000/s * 2KB = 8MB/s ingress; RF3 ~24MB/s replicated writes, 691GB/day raw payload and
2.07TB/day replicated before compression. Provision partitions using measured per-partition sustainable rate and
consumer parallelism, not service count. Local one-broker Kafka is a correctness lab only.

Backfill: 10M records / 5000 rows/s = 2000s (~33.3min), ~10GB at 1KB each; minimum 5MB/s payload network plus overhead.
At 1000 source changes/s, target apply 3000/s, 2M backlog drains in 1000s; lag grows unbounded when apply <= incoming.
Full-copy fenced pause in this demo would be unacceptable for that scale, motivating snapshot+CDC.

Network responses: 100k/s * 1KB ~100MB/s (~0.8Gb/s), duplicated eligible shadow traffic approximately doubles read
payload at 100% shadow, plus TLS/protocol overhead. Bounded sampling and load budgets required.

Cost model (hypothetical rates, not a quote): 26 pods * 0.5 vCPU * $0.04/vCPU-hour * 730h = $379.60/month CPU, 26GiB * $
0.005/GiB-hour *730 = $94.90 memory; DB/Kafka HA, IOPS, storage, backups, egress, observability and operations
additional. Price inputs must be verified for chosen provider/region before budgeting.

99.99% monthly availability permits ~4.38min unavailability in 30.44 days. Catalog write pause must be measured and
budgeted independently of read availability. No local run proves this SLO.
