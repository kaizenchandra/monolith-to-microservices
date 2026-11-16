# Ordered delivery gates

1. Characterize the newly created baseline: HTTP contracts, local transaction, authorization, constraints.
2. Introduce gateway at LEGACY, compare direct versus routed product responses.
3. Deploy fenced Catalog, snapshot legacy, validate exact fields and digest.
4. SHADOW: exact canonical JSON, bounded queue; inspect mismatch/error/dropped metrics.
5. CANARY 1/5/10/25/50/100: operator gates, stable principal cohort. A single demo reader cannot prove percentage
   distribution across customers.
6. Fence Catalog, final copy/reconcile, activate modern writer, enable delegation. Keep reverse synchronization
   executable.
7. Fence checkout before Inventory. Transfer stock and reservation identities atomically; test last-unit race. Keep
   checkout unavailable until dependencies are ready.
8. Activate Saga checkout. Existing legacy IDs stay on legacy; new IDs are durably orchestrated. Consume confirmed
   events into fulfillment/notification/reporting.
9. Validate outage recovery, security and restore drills; only then consider permanent retirement.

Each phase has files and executable checks in the project. Production phase acceptance is conditional on evidence in
verification.md; remaining checks are not declared complete. Catalog retirement and Inventory rollback are deliberately
deferred.
