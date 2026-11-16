# Coexistence

```mermaid
flowchart LR
 C[Client] --> G[Strangler router]
 G -->|authoritative legacy reads and all write facade| L[Monolith]
 G -->|shadow or cohort GET only| Cat[Catalog]
 L -->|after ownership switch| Cat
 L -->|checkout delegation| O[Orders]
 O -->|ownership-aware Catalog and customer reads| L
 O --> I[Inventory]
 O --> P[Payment simulator]
 L --> LD[(Legacy DB)]
 Cat --> CD[(Catalog DB)]
 I --> ID[(Inventory DB)]
 O --> OD[(Order DB)]
 P --> PD[(Payment DB)]
 O -->|outbox| K[Kafka]
 I -->|outbox| K
 K -->|deduplicated confirmed orders| L
```

Service-owned databases share one PostgreSQL process locally for cost; they are separate databases, not shared tables.
Local service runtime roles have database-scoped DML and separate Flyway migrator roles; the bootstrap superuser is
retained only for operator inspection. Production must use separate owner/migrator/runtime credentials with no
cross-database grants and restricted networks.

Ownership row shared locks cover business transactions; exclusive ownership updates drain them. Export uses
repeatable-read plus ownership share lock. Final transfer fences the source before copy and exact digest comparison,
then activates the target and only then enables delegation. Failures preserve the fence. Gateway flags affect eligible
reads only; they never determine write authority.
