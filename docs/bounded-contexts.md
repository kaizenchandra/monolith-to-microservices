# DDD context map and readiness

| Context         | Language / aggregate / invariant               | Ownership / contract / dependency                                    |
|-----------------|------------------------------------------------|----------------------------------------------------------------------|
| Customer        | Customer, existing reference                   | legacy customers; Orders validates reference through HTTP            |
| Catalog/Pricing | Product, cents, version; nonnegative price     | Catalog owns products after cutover; published v1 product contract   |
| Inventory       | Stock, Reservation; never negative             | Inventory owns stock/reservations; Open Host Service reserve/release |
| Ordering        | Checkout intent, Saga; one identity per intent | Orders owns checkout; customer/catalog are suppliers                 |
| Payments        | Authorization; stable amount for key           | Payment simulator owns authorizations; explicit void                 |
| Fulfillment     | Shipment intent                                | legacy event projection, Customer/Supplier                           |
| Notification    | Notification intent                            | legacy event projection; delivery provider not included              |
| Reporting       | Sales projection                               | legacy modern_orders inbox projection plus historical orders         |

The ACL converts whitelisted legacy snapshot fields into Catalog's independent Product record, retaining integer cents
and explicit tombstones. No JPA types cross boundaries. Infrastructure sharing is not a DDD Shared Kernel. Published
Language is versioned JSON, consumers tolerate extra event fields but reject unsupported schema versions.

| Candidate                | Value / coupling / data complexity                    | Scale / change / failure                              | Difficulty / readiness               | Order                  |
|--------------------------|-------------------------------------------------------|-------------------------------------------------------|--------------------------------------|------------------------|
| Catalog                  | high / low / one aggregate                            | high reads / frequent / browse impact                 | low / demo tests available           | 1                      |
| Inventory                | high / high checkout coupling / reservation ownership | burst writes / medium / overselling                   | high / concurrency evidence required | 2                      |
| Ordering/Payment         | high / high / distributed workflow                    | business critical / frequent / money and stuck orders | high / sandbox first                 | 3 together after stock |
| Customer                 | uncertain / identity coupling / PII                   | unknown / unknown / authorization                     | high / real IAM unknown              | later                  |
| Fulfillment/Notification | medium / external effects / dedup required            | unknown / medium / duplicate business effects         | medium / provider unknown            | retain                 |

API stability is characterized for product v1; legacy/new order responses intentionally differ in pending Saga state and
extra operational fields. Team readiness, deployment frequency, production coverage, recovery maturity remain unknown.
Do not score invented production readiness.

Capability extraction aligns responsibility with transactions. Entity-only extraction often creates distributed joins;
endpoint-only extraction leaves shared writes; data ownership extraction is mandatory but insufficient without business
boundaries; event-driven extraction decouples effects but adds lag. Controller/repository/database services split every
request across layers while retaining a single release/transaction dependency and add network failure without autonomy.
