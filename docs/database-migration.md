# Data migration and expand/contract

| Approach                                         | Consistency and pause                                                                  | Recovery / cost / rollback                                                                            |
|--------------------------------------------------|----------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| Offline bulk copy (implemented final gate)       | coherent snapshot after write fence; bounded write outage                              | rerun while fenced; low complexity, pause proportional to dataset; reverse copy supported for Catalog |
| Snapshot plus CDC                                | consistent snapshot with retained source position and ordered apply; brief final fence | slots/offsets/snapshots need durable lifecycle, WAL disk risk; higher ops cost                        |
| Transactional outbox (implemented modern events) | state/event local atomic; eventual at least once                                       | relay retries, inbox dedup, replay retained events; no cross-service ACID                             |
| Application event after commit                   | gap if crash before publish                                                            | reconciliation required; unsuitable sole correctness path                                             |
| Dual-write                                       | partial commits without protocol                                                       | avoid; would need durable intent, repair and explicit conflict authority                              |
| Event-carried state                              | consumer local view, versioned eventual updates                                        | rebuild/replay; larger messages, data exposure review                                                 |
| Batch reconciliation (implemented)               | point-in-time proof only                                                               | deterministic sorted records, types, versions/tombstones and sums; rerun after repair                 |

Snapshots use repeatable-read, ordered keys and complete tombstones. Full admin import is transactional and requires
FROZEN. Incremental Catalog adapter rejects lower record versions. Normal writers never physically delete product rows.
No cursor/high-water CDC position is claimed for snapshots; the final source fence closes the update window. Live
pre-copy is observational and must be refreshed before the final fence.

Reconciliation SHA256 covers canonical JSON of every field/row and compares complete structures; counts and
financial/stock totals are supplemental. Zero record/version/amount/stock mismatches is the demo acceptance threshold.
Inventory transfer includes reservations and stock in one snapshot. Cross-context order/payment financial auditing in
production additionally requires provider settlement records, absent from this simulator.

Expand-contract example: V2 adds inbox/projection tables without changing V1. Existing APIs retain cents and IDs. For
future currency: add nullable currency, deploy readers defaulting legacy USD only under approved semantics, backfill in
small key ranges, validate null count, deploy mandatory writer, then add NOT VALID check and validate; set NOT NULL
after compatibility proof. CREATE INDEX CONCURRENTLY requires its own nontransactional Flyway migration. ALTER TABLE
takes locks; set lock_timeout, inspect pg_locks/long transactions and abort rather than blocking checkout. Never drop
old columns/tables during rolling compatibility window. Data rollback is restore/replay or reverse-compatible copy, not
automatically a Flyway down migration.

Catalog reads through the legacy facade return 503 while ownership is FROZEN. This explicitly prevents stale-source
reads during reverse synchronization; the transfer includes a bounded read interruption on that path as well as a write
pause. Direct modern snapshot reads remain independently gated.
