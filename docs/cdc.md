# CDC decision

Debezium is evaluated, not installed. This demo can safely add ownership fencing and expose consistent exports, and its
dataset has two products. Full copy and a measured write pause are easier to verify than an unnecessary distributed CDC
pipeline. Kafka carries committed modern integration events, not legacy row replication.

For a large PostgreSQL source use a tested Debezium connector release with `pgoutput`, wal_level=logical, sufficient
max_replication_slots/max_wal_senders, a replication account, table SELECT grants, a controlled publication and replica
identity suitable for update/delete keys. Initial consistent snapshot and streaming offsets must belong to the same
connector lifecycle. Retain slots and Kafka Connect offsets durably; monitor retained WAL bytes, source LSN vs applied
position, transaction metadata, snapshot status and disk headroom. Never create an unrelated snapshot and start CDC from
a guessed timestamp. Apply tombstones/versions and deduplicate event IDs; do not use LSN alone as per-row unique event
identity. Multi-table atomic business effects require transaction-aware grouping or explicit reconciliation. Connector
restart does not imply business exactly once.

At cutover: fence all legacy mutation paths, wait for in-flight commits, capture final boundary, wait for target apply
through boundary, reconcile, activate the writer, then route. Slot overflow or lost offsets requires controlled
resnapshot while target is nonauthoritative. Source schema DDL needs coordinated compatibility and connector
verification.

Sources: [Debezium PostgreSQL connector](https://debezium.io/documentation/reference/stable/connectors/postgresql.html), [PostgreSQL logical replication configuration](https://www.postgresql.org/docs/17/logical-replication-config.html).
These requirements were checked against official connector documentation; the proposed CDC configuration has not been
executed here.
