# Target and deliberately retained monolith

Catalog, Inventory, Ordering and Payment have deployable implementations. Customer, Fulfillment and Notification remain
coherent legacy modules until justified extraction; Reporting consumes a published event projection. There are no empty
services standing in for extraction.

```mermaid
flowchart TB
 API --> Catalog
 API --> Ordering
 Ordering --> Inventory
 Ordering --> Payments
 Ordering -. OrderConfirmed .-> Fulfillment
 Ordering -. OrderConfirmed .-> Notifications
 Ordering --> CustomerReference
 Catalog --> CatalogDatabase
 Inventory --> InventoryDatabase
 Ordering --> OrderingDatabase
 Payments --> PaymentDatabase
```

Production target separates resource authorization, customer identity, read caching/CDN, HA databases and multi-broker
Kafka. These are future integrations, not deployed claims.
