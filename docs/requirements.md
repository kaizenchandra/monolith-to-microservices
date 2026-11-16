# Requirements and acceptance

Functional: stable product lookup with integer cents and legacy IDs; optimistic catalog updates; customers; ACID legacy
checkout; authorization simulator; durable shipment/notification intents; combined sales reporting; independently owned
catalog and inventory; Saga checkout; migration gates and reverse catalog copy.

Invariants: one active writer per capability; every admitted stock decrement is conditional and transactional; same
operation key requires identical immutable intent; money uses integer minor units and checked multiplication; stock is
nonnegative; reservation effects and events commit together; target imports require fencing; order confirmation occurs
only after reservation and authorization. Consumers deduplicate event ID within their projection transaction. No
business writes are shadowed.

NFRs: authenticated APIs, bounded outbound timeouts/shadow queues, low-cardinality metrics, graceful shutdown, durable
state, reproducible schema and data, explicit recovery. The 100k reads/s and 99.99% availability are planning targets
only. Local single-node infrastructure cannot meet them.

Acceptance: `mvn verify` and E2E pass on fresh PostgreSQL; canonical digests match before transfer; no critical
mismatches; no open security defects; cohort rollout requires operator review. Production acceptance additionally
requires workload/authorization contracts, measured SLOs, HA restore drills and operational ownership. Missing evidence
is a failed gate, never implicit approval.
