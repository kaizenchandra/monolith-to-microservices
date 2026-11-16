# APIs and transactions

```mermaid
classDiagram
 class LegacyApi { +checkout(Checkout) Object +product(id) Object }
 class CatalogApi { +get(id) Product +put(Product) Product +replicate(List) void }
 class InventoryApi { +reserve(Request) Map +release(Request) Map }
 class OrderApi { +create(Checkout) Map }
 class SagaWorker { +tick() void +step(row) void }
 class PaymentApi { +authorize(Authorization) Map +cancel(Authorization) Map }
 LegacyApi --> CatalogApi : ACL HTTP
 LegacyApi --> OrderApi : delegate
 SagaWorker --> InventoryApi : idempotent reserve/release
 SagaWorker --> PaymentApi : authorize/void
```

```mermaid
sequenceDiagram
 participant Op as Operator
 participant L as Legacy
 participant C as Catalog
 Op->>L: fence expected LEGACY epoch
 L->>L: exclusive owner lock drains transactions
 Op->>L: repeatable-read export
 Op->>C: transactional import while FROZEN
 Op->>C: export and canonical digest
 Op->>C: activate MODERN with epoch CAS
 Op->>L: activate delegation MODERN
```

GET /api/products/{id}: v1 exact product JSON. POST /api/products: expected `version`; updates existing IDs only,
deletion represented by tombstone. POST /api/orders: caller-supplied operation ID, customer/product/quantity; legacy
CONFIRMED or modern NEW and polling GET /api/orders/{id}. Modern order eventual states are an intentional compatibility
difference and must be client-approved. No new-product creation endpoint is claimed.

Internal POST reserve/release: `{id,product_id,quantity}`; authorize/void: `{id,amount_cents}`. Ownership CAS:
`{expected,next,epoch}`; invalid stale transitions 409. Role enforcement applies independently at every service. Exact
SQL is under each module's Flyway resources. Local transaction boundaries are annotated in controller beans; Saga worker
and relay use explicit TransactionTemplate with row locks.
