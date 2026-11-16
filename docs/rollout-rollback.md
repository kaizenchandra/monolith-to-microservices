# Recovery guarantees

Catalog transfer: source LEGACY -> FROZEN; final export/import + exact reconcile; target FROZEN -> MODERN; source
FROZEN -> MODERN (delegation). Rerun cutover after interruption; when target is already MODERN it only completes source
delegation. An import/reconcile failure leaves both writers fenced. Never reopen source by guesswork.

Catalog rollback after new writes: route reads through legacy; fence source delegation; fence target/drain in-flight
writes; reverse-copy the entire modern state including versions/tombstones; reconcile; activate source LEGACY. Target
remains FROZEN. The automated demo verifies a new authoritative write survives this reverse transfer. A crashed rollback
must be resumed with inspection before any writer is enabled. Route-only rollback is insufficient.

Inventory: pause checkout first, fence legacy inventory, copy stock plus reservations, activate modern. Then explicitly
activate Saga checkout only if Catalog and Inventory are modern. Existing historical order IDs stay legacy. Inventory
rollback after Saga activation is blocked: drain/resolve every active workflow, copy reservations and stock, reconcile
payment authorization ownership and legacy key semantics, then review activation. There is no claim this unsupported
rollback is safe.

Schema rollback: V1/V2 are additive. Incompatible future schemas require forward repair or tested restore/replay, not
traffic changes. Event rollback: retain v1 compatibility and inbox retention past replay horizon. Retirement rollback:
restore archived code/data only with compatible readers and re-established ownership. Regional outage: stop transfers
until quorum/authority evidence is available; this single-region demo has no regional failover protocol.

Catalog reads through the legacy facade return 503 while ownership is FROZEN. This explicitly prevents stale-source
reads during reverse synchronization; the transfer includes a bounded read interruption on that path as well as a write
pause. Direct modern snapshot reads remain independently gated.

Ordering resolves product prices through the ownership-aware legacy Catalog facade throughout the rollback window. A
direct read from the frozen Catalog replica after rollback would otherwise return stale prices. Retain this ACL/control
dependency until Catalog rollback is permanently retired; only then switch the supplier port directly to Catalog.
