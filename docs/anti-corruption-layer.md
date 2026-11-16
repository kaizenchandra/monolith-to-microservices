# Contract and ACL

Legacy `products` is not a shared Java entity. The representative legacy export still uses row maps. Catalog replication
binds only its independent Product fields; reconciliation blocks an unexpected source/target schema mismatch. Before
expanding legacy product columns, introduce explicit legacy API/export projections so new database fields cannot leak
into the v1 response. Catalog's `Product` record owns ID/name/cents/version/tombstone semantics. Catalog's replication
adapter accepts typed records and uses version-conditional upsert; migrations use full fenced imports to preserve
deletions and exact state. Null names and negative money are rejected by API/schema, rather than silently inventing
values. Legacy IDs remain stable bigint references; no UUID remapping collision is introduced. HTTP 404 is absent
product, 409 is stale version/fence/conflicting identity, 400 is invalid request, 503 is downstream unavailable.

The external product contract is checked in contract-tests/product-v1.json and verified against legacy and modern
responses during E2E. This is a hand-maintained consumer contract, not generated Pact verification. Pact/Spring Cloud
Contract would add independent consumer/provider publishing and mixed-version CI at scale; they are not dependencies
here. Additional event fields are tolerated but schemaVersion must remain 1 for the existing consumer. Mixed-version and
error-shape expansion needs a separate compatibility matrix before production deployment.

Ordering resolves product prices through the ownership-aware legacy Catalog facade throughout the rollback window. A
direct read from the frozen Catalog replica after rollback would otherwise return stale prices. Retain this ACL/control
dependency until Catalog rollback is permanently retired; only then switch the supplier port directly to Catalog.
