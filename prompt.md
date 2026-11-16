# MASTER PROMPT: Design and Develop an Enterprise-Grade Legacy Monolith-to-Microservices Migration Using the Strangler Fig Pattern with Java 21 and Spring Boot

## 1. ROLE AND ENGINEERING RESPONSIBILITIES

Act as a **Principal Software Engineer, Distinguished Distributed Systems Architect, Enterprise Solution Architect, Java
21 Expert, Spring Boot Architect, Domain-Driven Design Specialist, Database Modernization Architect, Cloud Architect,
DevOps Engineer, and Site Reliability Engineer** with 15+ years of experience modernizing large-scale legacy enterprise
applications.

Your task is to **design, implement, test, validate, document, and demonstrate an end-to-end migration of a legacy Java
monolithic application into a modern microservices architecture using the Strangler Fig Pattern**.

Use:

- Java 21
- Spring Boot
- Spring Security
- Spring Cloud Gateway where justified
- PostgreSQL
- Apache Kafka
- Debezium CDC where justified
- Redis where justified
- Flyway
- Docker
- Kubernetes
- Prometheus, Grafana, Micrometer, and OpenTelemetry

Treat this as a real enterprise modernization project, not a theoretical system design interview exercise.

You must implement a working legacy monolith, incrementally extract business capabilities into microservices,
demonstrate coexistence, migrate data safely, validate parity, and complete an executable cutover and rollback workflow.

**Do not stop after architecture diagrams, migration strategies, pseudocode, or isolated code examples. Deliver actual
source code, configuration, migrations, automated tests, deployment infrastructure, and operational documentation.**

### Mandatory engineering principles

1. Follow existing repository instructions, architecture, dependency versions, and coding conventions.
2. Use Java 21 and a verified compatible Spring Boot version for newly developed services.
3. Preserve the legacy application's runtime and dependencies where immediate upgrades are unsafe.
4. Avoid unnecessary refactoring of legacy code before extraction.
5. Apply DDD, SOLID, Clean Architecture, and Hexagonal Architecture pragmatically.
6. Follow incremental migration with independently verifiable steps.
7. Keep the system functional throughout migration.
8. Avoid a big-bang rewrite.
9. Define an authoritative owner for every business capability and mutable data set.
10. Never allow competing writes from legacy and modern systems without an explicit consistency and conflict-prevention
    protocol.
11. Use anti-corruption layers to isolate modern domains from legacy models.
12. Use transactional outbox, CDC, Saga, or reconciliation only where justified.
13. Address idempotency, concurrency, transaction boundaries, data integrity, retries, timeouts, and schema
    compatibility.
14. Prefer backward-compatible database changes.
15. Support controlled feature rollouts and rollback.
16. Treat shadow traffic as observational unless side effects are explicitly isolated.
17. Never claim zero downtime, zero data loss, or production readiness without verification.
18. Never fabricate executed tests, deployment results, or performance benchmarks.
19. Explain important trade-offs and identify residual migration risks.

---

# PART A — PRIMARY OBJECTIVE

## 2. MODERNIZE A LEGACY MONOLITH

Design and implement a representative enterprise **E-commerce Order Management Monolith**, then incrementally migrate it
into independently deployable microservices.

The legacy monolith should initially provide:

- Customer Management
- Product Catalog
- Inventory Management
- Order Management
- Payment Processing
- Shipping/Fulfillment
- Notifications
- Reporting

The monolith should initially use a shared relational database and synchronous internal business flows.

The target microservices architecture should include justified boundaries such as:

1. Customer Service
2. Catalog Service
3. Inventory Service
4. Order Service
5. Payment Service
6. Fulfillment Service
7. Notification Service
8. API Gateway / Strangler Router

Do not extract every module immediately.

The migration must demonstrate an incremental sequence where some capabilities remain in the legacy monolith while
others move into new Spring Boot microservices.

### Example migration journey

Initially:

`Client → Legacy Monolith → Shared Database`

During migration:

`Client → Strangler Gateway → Legacy Monolith / New Microservices`

After a particular capability is extracted:

`Client → Strangler Gateway → New Service → Service-Owned Database`

The migration must allow the old and new implementations to coexist without corrupting data or producing conflicting
business effects.

---

## 3. CORE MIGRATION SCENARIO

Consider a legacy Order Management application running in production.

The migration begins with Product Catalog.

### Phase 1

All requests are handled by the monolith.

### Phase 2

A Strangler Gateway is introduced in front of the monolith.

All requests still route to the legacy system.

### Phase 3

A new Catalog Service is implemented.

It has:

- Its own API
- Its own domain model
- Its own persistence
- An anti-corruption adapter
- Observability
- Integration tests

### Phase 4

Legacy catalog data is migrated or replicated into the new Catalog Service.

### Phase 5

Read-only catalog traffic is shadowed to the new service without changing customer-visible behavior.

### Phase 6

Parity between legacy and new catalog responses is verified.

### Phase 7

A small percentage of eligible catalog read traffic is routed to the new service.

### Phase 8

Traffic is gradually increased.

### Phase 9

Catalog write ownership is transferred through an explicit cutover protocol.

### Phase 10

The monolith delegates catalog operations to the new service or stops handling them.

### Phase 11

The legacy catalog implementation and obsolete tables are retired after validation.

Repeat the migration approach for additional bounded contexts.

---

# PART B — ASSESS THE EXISTING MONOLITH

## 4. LEGACY SYSTEM DISCOVERY

Before making code changes, inspect:

- Module structure
- Domain responsibilities
- Database schema
- Shared tables
- Cross-module method calls
- Transaction boundaries
- Scheduled jobs
- External integrations
- Authentication and authorization
- Performance bottlenecks
- Deployment pipeline
- Operational runbooks
- Production failure patterns

Create a dependency inventory.

Identify:

- Business capabilities
- Data ownership
- Technical dependencies
- Cyclic dependencies
- Shared utilities
- Hidden coupling
- Global transactions
- Shared caches
- Batch processes
- Reporting dependencies

Do not infer architecture solely from package names.

Use actual code, schema, and runtime evidence where available.

---

## 5. MIGRATION READINESS ASSESSMENT

Evaluate:

1. Test coverage
2. API stability
3. Database coupling
4. Transaction complexity
5. Deployment frequency
6. Business criticality
7. Team ownership
8. Infrastructure readiness
9. Observability maturity
10. Operational rollback capability

Create a migration risk assessment.

Classify each candidate capability by:

- Business value
- Coupling
- Data ownership complexity
- Independent scaling benefit
- Change frequency
- Failure impact
- Extraction difficulty
- Team readiness

Recommend an extraction order based on evidence, not just technical preference.

---

# PART C — DOMAIN-DRIVEN DECOMPOSITION

## 6. DEFINE BOUNDED CONTEXTS

Apply strategic DDD.

Identify bounded contexts such as:

- Customer
- Catalog
- Pricing
- Inventory
- Ordering
- Payments
- Fulfillment

Define each context's:

- Business responsibility
- Domain language
- Aggregates
- Invariants
- Data ownership
- API contracts
- Domain events
- Integration dependencies

Create a context map.

Classify relationships using relevant DDD patterns:

- Customer/Supplier
- Conformist
- Anti-Corruption Layer
- Published Language
- Open Host Service
- Shared Kernel, only when justified

Avoid copying legacy database entities into every new service.

---

## 7. SERVICE EXTRACTION STRATEGY

Compare:

1. Business capability extraction
2. Domain entity extraction
3. API endpoint extraction
4. Data ownership extraction
5. Event-driven extraction

Prefer business capabilities with coherent data and transaction boundaries.

Explain why splitting services by technical layers, such as Controller Service, Repository Service, and Database
Service, is generally a poor microservices decomposition.

Do not create distributed CRUD services that remain tightly coupled to the monolith's internal data model.

---

# PART D — STRANGLER FIG ARCHITECTURE

## 8. STRANGLER GATEWAY

Implement a routing gateway that can direct traffic between legacy and modern implementations.

Consider Spring Cloud Gateway, an ingress controller, or an established API gateway.

Support:

- Route configuration
- Authentication
- Correlation IDs
- Header propagation
- Routing by endpoint
- Routing by feature flag
- Percentage-based rollout
- Tenant/customer cohort routing
- Health-aware routing
- Controlled fallback for safe operations
- Request and response observability

### Critical rule

The gateway must not silently retry non-idempotent writes across legacy and modern implementations.

Routing decisions must preserve business-operation ownership and idempotency.

---

## 9. TRAFFIC ROUTING MODES

Implement or demonstrate:

### Legacy-only

100% of traffic goes to the monolith.

### Shadow mode

Legacy handles the authoritative request.

A copy of eligible read-only traffic is sent to the modern service for comparison.

### Canary mode

A small cohort of traffic is served by the new service.

### Progressive rollout

Traffic increases incrementally after defined verification gates.

### New-service-only

The modern service becomes the authoritative implementation.

### Emergency rollback

Traffic returns to the legacy path only when doing so is data-safe and operationally supported.

Do not assume routing rollback is safe after data ownership or write-schema cutover.

---

# PART E — ANTI-CORRUPTION LAYER

## 10. DESIGN AN ANTI-CORRUPTION LAYER

Implement an ACL between legacy and modern bounded contexts.

Responsibilities:

- Data transformation
- Contract translation
- Legacy identifier mapping
- Error translation
- Business semantic translation
- Null/default handling
- Version compatibility

Example:

Legacy model:

`LegacyOrderEntity`

Modern model:

`OrderAggregate`

The modern domain must not directly depend on legacy JPA entities or shared mutable tables.

### Required interfaces

Define ports/adapters such as:

- LegacyCatalogPort
- CatalogServiceAdapter
- LegacyOrderTranslator
- ExternalCustomerReferenceResolver

Use abstractions only where they protect meaningful domain or infrastructure boundaries.

---

# PART F — DATABASE DECOMPOSITION

## 11. DATABASE-PER-SERVICE

Compare:

### Shared database during transition

Advantages:

- Lower migration overhead
- Existing transactional capabilities

Risks:

- Coupling
- Shared writes
- Schema coordination
- Unclear ownership

### Schema per service

Provides clearer logical ownership but may still share infrastructure and operational dependencies.

### Database per service

Provides stronger ownership, deployment independence, and scaling flexibility, but increases distributed consistency
complexity.

Recommend a staged transition.

**A shared database can be a temporary migration technique, but shared mutable ownership must not become the permanent
target.**

---

## 12. DATA OWNERSHIP MATRIX

For each table, define:

- Current owner
- Target owner
- Read consumers
- Write consumers
- Migration phase
- Authority status
- Replication strategy
- Retirement plan

Example:

| Data     | Legacy Owner | Target Owner      |
|----------|--------------|-------------------|
| Products | Monolith     | Catalog Service   |
| Stock    | Monolith     | Inventory Service |
| Orders   | Monolith     | Order Service     |
| Payments | Monolith     | Payment Service   |

Never allow two independent authoritative writers to mutate the same business data without coordination.

---

# PART G — DATABASE MIGRATION STRATEGIES

## 13. COMPARE DATA MIGRATION APPROACHES

Evaluate:

1. Offline bulk export/import
2. Snapshot plus incremental CDC
3. Transactional outbox
4. Application-level event propagation
5. Dual-write
6. Event-carried state transfer
7. Batch reconciliation

For each explain:

- Consistency guarantees
- Downtime
- Complexity
- Recovery
- Rollback
- Operational cost

Avoid naive application-level dual-write as a correctness strategy.

If dual-write is unavoidable, explicitly design failure handling and reconciliation.

---

## 14. SNAPSHOT PLUS CHANGE DATA CAPTURE

Implement a representative database migration pipeline.

Suggested flow:

1. Establish snapshot boundary.
2. Export source data.
3. Import into target service storage.
4. Record source change position.
5. Capture subsequent changes.
6. Apply updates idempotently.
7. Track replication lag.
8. Detect missing or duplicate changes.
9. Reconcile source and target.
10. Prepare controlled write cutover.

Evaluate Debezium CDC for PostgreSQL.

Verify exact connector capabilities and PostgreSQL logical replication requirements.

Do not assume CDC delivery order automatically matches every required business transaction boundary.

---

## 15. DATA RECONCILIATION

Implement reconciliation checks for:

- Row counts
- Primary key mapping
- Record versions
- Financial totals
- Inventory totals
- Referential relationships
- Missing records
- Duplicate records
- Deleted records
- Timestamp consistency

Use deterministic comparisons.

Do not rely on row counts alone to prove data equivalence.

Produce a reconciliation report and clear cutover acceptance thresholds.

---

# PART H — TRANSACTIONAL OUTBOX

## 16. RELIABLE INTEGRATION EVENTS

Implement Transactional Outbox for new services where committed state changes must reliably produce integration events.

Example:

1. New Inventory Service reserves stock.
2. Reservation state is committed.
3. Outbox event is written in the same local transaction.
4. An outbox relay publishes the event.
5. Consumers apply it idempotently.

Provide:

- Outbox schema
- Event IDs
- Aggregate IDs
- Versions
- Schema versions
- Publish status
- Retry handling
- Relay ownership
- Failure recovery

Do not assume outbox publishing is exactly once.

---

# PART I — LEGACY EVENT INTEGRATION

## 17. MONOLITH TO MICROSERVICE EVENTS

Evaluate:

- CDC from legacy tables
- Legacy application outbox
- Polling publisher
- Internal domain-event adapters

Choose according to how safely the legacy system can be modified.

Avoid injecting large event-driven frameworks into the monolith if a smaller change can provide reliable integration.

Define how legacy changes become:

- ProductUpdated
- InventoryAdjusted
- OrderCreated
- PaymentCompleted

Use versioned contracts.

Do not expose legacy table layouts as permanent event schemas.

---

# PART J — DISTRIBUTED TRANSACTIONS

## 18. REPLACE CROSS-MODULE ACID WITH EXPLICIT WORKFLOWS

Identify legacy transactions that span several future bounded contexts.

Example monolithic transaction:

1. Create order.
2. Deduct inventory.
3. Record payment.
4. Create shipment.
5. Commit one database transaction.

After extraction, these operations cross service boundaries.

Design a Saga where justified.

### Saga responsibilities

- Durable state
- Step tracking
- Idempotency
- Retry
- Compensation
- Timeout
- Reconciliation
- Manual intervention

Do not replace every local transaction with a Saga.

Retain ACID transactions inside individual service boundaries.

---

## 19. SAGA MIGRATION STRATEGY

Compare:

- Orchestration
- Choreography

Prefer orchestration for complex checkout/payment workflows if it improves visibility and recovery.

Implement a representative Saga:

`CreateOrder → ReserveInventory → AuthorizePayment → ConfirmOrder`

Compensations:

- Release inventory
- Cancel order
- Void authorization
- Refund if capture occurred

Document which operations are reversible, compensatable, or irreversible.

---

# PART K — API AND CONTRACT MIGRATION

## 20. API COMPATIBILITY

Preserve external contracts where required.

Use:

- API versioning
- Contract tests
- Response transformation
- Compatibility adapters
- Deprecation policies
- Client migration plans

Avoid changing API semantics silently during traffic migration.

Define how clients transition from legacy endpoints to new APIs.

---

## 21. CONSUMER-DRIVEN CONTRACT TESTING

Evaluate Pact or Spring Cloud Contract.

Implement contract tests for:

- Gateway ↔ Legacy
- Gateway ↔ New Service
- New Service ↔ New Service
- Event producers ↔ Consumers

Verify:

- Required fields
- Error codes
- Nullability
- API version compatibility
- Backward-compatible schema evolution

Use actual contracts rather than only unit tests.

---

# PART L — SHADOW TRAFFIC AND PARITY VALIDATION

## 22. SHADOW READS

Implement shadow testing for safe read-only operations.

Flow:

1. Client requests product information.
2. Gateway sends the authoritative request to legacy.
3. An observational copy is sent to the new Catalog Service.
4. Legacy response is returned to the client.
5. New response is collected separately.
6. Responses are normalized.
7. Differences are recorded.
8. Parity metrics are published.

### Critical safeguards

- Shadow requests do not perform business writes.
- Shadow load is bounded.
- Shadow failures do not affect the customer response.
- Sensitive data is not copied without authorization.
- Response comparison accounts for expected nondeterministic fields.
- Shadow traffic is not treated as proof of perfect equivalence.

Never shadow payment capture, inventory reservation, or other non-idempotent side effects without a fully isolated
simulator or non-mutating mechanism.

---

## 23. PARITY METRICS

Measure:

- Matching response percentage
- Missing records
- Incorrect prices
- Stock discrepancies
- Schema incompatibilities
- Error mismatches
- Latency difference
- Data freshness lag

Define thresholds for progressing from shadow mode to canary.

Example acceptance:

- No critical correctness mismatches in the representative test set.
- No unauthorized data exposure.
- Error rates within the defined SLO.
- Data replication lag within the approved window.

Do not invent pass rates.

---

# PART M — CANARY AND ROLLOUT

## 24. PROGRESSIVE DELIVERY

Implement controlled rollout stages.

Example:

- 0% — Shadow only
- 1% — Initial canary
- 5%
- 10%
- 25%
- 50%
- 100%

Treat percentages as example gates.

Use cohort-stable routing where consistency requires a user or tenant to remain on one implementation.

Define:

- Error-rate threshold
- Latency threshold
- Correctness threshold
- Data freshness threshold
- Security checks
- Rollback triggers
- Observation requirements

Avoid automatic promotion based only on aggregate latency and error rate.

---

## 25. ROLLBACK SAFETY

Define rollback separately for:

### Read routing

Usually easier to reverse.

### Write routing

May require restoring write ownership or replaying committed changes.

### Database schema migration

May be impossible to reverse without data loss after incompatible writes.

### Event contract migration

May require dual-compatible consumers during rollback.

### Feature retirement

May require restoring legacy code or data.

**Do not claim rollback is always a simple gateway route change.**

Implement explicit rollback plans for each migration phase.

---

# PART N — ZERO-DOWNTIME DATABASE MIGRATION

## 26. EXPAND-CONTRACT PATTERN

Implement schema changes using:

1. Expand schema
2. Deploy backward-compatible code
3. Backfill data
4. Validate
5. Switch reads/writes
6. Remove old dependencies
7. Contract schema later

Examples:

- Add a nullable column
- Backfill
- Introduce new writer
- Make the column mandatory after validation

Avoid destructive migrations before old versions are retired.

Document PostgreSQL migration locking and indexing implications.

---

## 27. WRITE OWNERSHIP CUTOVER

Design a safe cutover protocol:

1. Confirm target data parity.
2. Announce cutover epoch.
3. Fence or quiesce legacy writes for the bounded context.
4. Drain or account for in-flight writes.
5. Apply final CDC changes.
6. Verify target state.
7. Activate new authoritative writer.
8. Update gateway/service routing.
9. Monitor correctness.
10. Retain a supported rollback/recovery path.

Do not use uncontrolled active-active writes during ownership transfer.

Use version tokens or authoritative routing epochs where useful.

---

# PART O — SECURITY MODERNIZATION

## 28. AUTHENTICATION AND AUTHORIZATION

Evaluate:

- Existing legacy session authentication
- OAuth2/OIDC
- JWT Resource Servers
- Centralized IAM
- API Gateway authentication
- Service-to-service credentials

Design a staged security migration.

Avoid forcing all users to log in again unnecessarily if safe session bridging is possible.

Do not blindly propagate legacy session cookies to internal microservices.

Define the trust boundaries for identity exchange.

---

## 29. SERVICE SECURITY

Implement:

- Spring Security
- JWT issuer/audience validation
- Resource-level authorization
- Service-to-service authentication
- TLS/mTLS where justified
- Secret management
- Least-privilege database credentials
- Secure internal APIs
- Audit logging

Ensure the new architecture does not weaken authorization previously enforced within the monolith.

---

# PART P — PERFORMANCE AND RELIABILITY

## 30. RESILIENCE PATTERNS

Implement appropriate:

- Timeouts
- Circuit breakers
- Bounded retries
- Bulkheads
- Idempotency
- Rate limiting
- Backpressure
- Fallback for safe read operations

Use Resilience4j or established equivalents where justified.

Do not retry non-idempotent business operations without safe operation identity or reconciliation.

---

## 31. PERFORMANCE BASELINE

Measure legacy behavior before migration:

- Request throughput
- p50/p95/p99 latency
- CPU
- Heap/GC
- Database QPS
- Transaction contention
- Error rate
- Availability
- Deployment time
- Incident recovery time

Define comparable new-service measurements.

Do not claim migration improves performance simply because the architecture uses microservices.

---

# PART Q — CAPACITY PLANNING

## 32. SCALE ASSUMPTIONS

Use illustrative targets:

| Dimension                 | Design Target                         |
|---------------------------|---------------------------------------|
| Registered customers      | 10 million                            |
| Catalog read traffic      | 100,000 RPS                           |
| Order creation            | 1,000/sec                             |
| Availability              | 99.99%                                |
| Database consistency      | Strong within ownership boundary      |
| Cross-service consistency | Explicit eventual consistency         |
| Migration rollout         | Progressive                           |
| Downtime                  | Minimized, measured                   |
| Data loss                 | Prevented under documented guarantees |
| Deployment                | Kubernetes                            |

Calculate:

- Service instance requirements
- Database connections
- Cache memory
- Kafka throughput
- CDC lag
- Storage duplication during migration
- Backfill duration
- Network bandwidth
- Migration worker throughput
- Infrastructure cost

State assumptions and formulas.

---

# PART R — TARGET TECHNOLOGY STACK

## 33. JAVA 21 + SPRING BOOT

Use:

| Layer               | Technology                               |
|---------------------|------------------------------------------|
| Language            | Java 21 for new services                 |
| Framework           | Spring Boot, verified compatible release |
| API Gateway         | Spring Cloud Gateway or ingress routing  |
| Security            | Spring Security / OAuth2 Resource Server |
| Persistence         | PostgreSQL                               |
| Data access         | Spring Data JPA/JDBC where justified     |
| Database migrations | Flyway                                   |
| Event processing    | Apache Kafka                             |
| CDC                 | Debezium where justified                 |
| Cache               | Redis                                    |
| Reliability         | Resilience4j                             |
| Metrics             | Micrometer                               |
| Tracing             | OpenTelemetry                            |
| Tests               | JUnit 5 / Mockito                        |
| Integration tests   | Testcontainers                           |
| Contract tests      | Pact / Spring Cloud Contract             |
| Performance         | k6 / Gatling                             |
| Containers          | Docker                                   |
| Orchestration       | Kubernetes                               |
| Monitoring          | Prometheus / Grafana                     |

Verify exact compatible versions using official documentation.

The legacy monolith may remain on an older Java/Spring version if upgrading it is outside the safe migration scope.

---

# PART S — PROJECT STRUCTURE

## 34. SOURCE CODE ORGANIZATION

Create a migration demonstration repository.

```text id="y1p3me"
monolith-to-microservices/
├── pom.xml
├── README.md
├── docs/
│   ├── architecture-before.md
│   ├── architecture-transition.md
│   ├── architecture-target.md
│   ├── bounded-contexts.md
│   ├── migration-roadmap.md
│   ├── data-ownership.md
│   ├── database-migration.md
│   ├── cdc.md
│   ├── anti-corruption-layer.md
│   ├── strangler-routing.md
│   ├── saga.md
│   ├── rollout-rollback.md
│   ├── security.md
│   ├── capacity-planning.md
│   ├── runbook.md
│   └── decisions/
├── legacy-monolith/
├── strangler-gateway/
├── services/
│   ├── catalog-service/
│   ├── inventory-service/
│   ├── order-service/
│   ├── payment-service/
│   └── fulfillment-service/
├── migration-tools/
│   ├── data-backfill/
│   ├── reconciliation/
│   └── cutover/
├── integration-tests/
├── contract-tests/
├── benchmarks/
├── docker/
├── helm/
└── .github/workflows/
```

Adapt service count and structure to the chosen extraction phases.

Avoid creating empty services merely to match the target diagram.

---

# PART T — END-TO-END MIGRATION IMPLEMENTATION

## 35. BUILD THE LEGACY BASELINE

If an existing legacy repository is provided, analyze and preserve it.

If no existing project is provided, create a representative legacy modular monolith with intentionally shared
persistence and coupled business workflows.

The baseline must:

- Compile
- Run
- Expose REST APIs
- Persist data
- Support a checkout workflow
- Have a reproducible test dataset
- Include baseline integration tests

Do not intentionally introduce security vulnerabilities merely to simulate legacy behavior.

---

## 36. INTRODUCE STRANGLER ROUTING

Implement:

- Gateway deployment
- Route configuration
- Legacy-only operation
- Feature-flag-based routing
- Safe read shadowing
- Canary routing
- Rollback configuration
- Metrics

Ensure the gateway preserves existing API semantics.

---

## 37. EXTRACT CATALOG SERVICE

Implement:

- Independent Spring Boot service
- Catalog domain model
- Catalog database
- Data backfill
- CDC or approved synchronization
- Anti-corruption layer
- API contract parity
- Read shadowing
- Canary rollout
- Write cutover
- Legacy delegation/removal

This must be the first fully demonstrated extraction.

---

## 38. EXTRACT INVENTORY SERVICE

Implement:

- Stock ownership
- Inventory transactions
- Reservation APIs
- Concurrency-safe updates
- Data migration
- Outbox events
- Legacy integration
- Cutover/reconciliation

Test no-overselling invariants during and after migration.

---

## 39. EXTRACT ORDER AND PAYMENT WORKFLOWS

Implement:

- Order Service
- Payment Service or sandbox adapter
- Distributed checkout Saga
- Idempotency
- Compensation
- Reconciliation
- Durable integration events

Ensure the transition does not create duplicate orders, charges, or inventory reservations.

---

## 40. LEGACY RETIREMENT

After each extraction:

1. Verify routing.
2. Verify data ownership.
3. Verify event consumers.
4. Remove obsolete write paths.
5. Remove obsolete data dependencies.
6. Remove unused API routes.
7. Archive approved data.
8. Remove old database tables only after safe retention and compatibility checks.
9. Update documentation.
10. Validate production monitoring.

Do not retire all legacy functionality until all required business capabilities have a verified replacement.

---

# PART U — FAILURE SCENARIOS

## 41. ANALYZE THESE 40 SCENARIOS

1. Gateway misroutes an endpoint.
2. New service returns a different response contract.
3. Shadow request accidentally performs a write.
4. New service becomes unavailable during canary.
5. Legacy and new service both accept writes.
6. Data backfill misses records.
7. CDC connector becomes unavailable.
8. CDC event is processed twice.
9. CDC events arrive out of order.
10. Snapshot and CDC boundaries overlap.
11. Source database transaction commits during cutover.
12. Target database contains stale data.
13. Backfill worker crashes.
14. Reconciliation detects mismatched totals.
15. Legacy application changes schema during migration.
16. New service deploys an incompatible schema.
17. Rollback occurs after new-only writes.
18. Database migration locks critical tables.
19. API Gateway fails.
20. Redis becomes unavailable.
21. Kafka becomes unavailable.
22. PostgreSQL primary fails.
23. Anti-corruption translation produces incorrect amounts.
24. Legacy identifiers collide with target identifiers.
25. Order creation occurs twice.
26. Payment authorization times out.
27. Inventory reservation succeeds but Order Service fails.
28. Saga compensation fails.
29. Consumer processes duplicate integration events.
30. Feature flags become inconsistent across instances.
31. User authentication works in legacy but fails in new service.
32. Session migration exposes unauthorized access.
33. Service-to-service authorization fails.
34. New service overloads legacy database.
35. A popular endpoint becomes a gateway hot spot.
36. Canary cohort experiences different behavior.
37. Rolling deployment mixes API/event versions.
38. Regional outage occurs during cutover.
39. Legacy scheduled job writes migrated data.
40. A retired module still receives hidden internal traffic.

For each provide:

- Root cause
- Data integrity risk
- Business impact
- Security implications
- Detection
- Prevention
- Recovery
- Automated test
- Residual risk

---

# PART V — TESTING

## 42. CHARACTERIZATION TESTS

Before extraction, implement tests documenting observable legacy behavior.

Cover:

- API responses
- Business rules
- Validation
- Transaction behavior
- Error codes
- Edge cases
- Authorization
- Data constraints

Use characterization tests to establish behavioral compatibility.

Do not assume legacy behavior is always desirable; document intentional changes separately.

---

## 43. CONTRACT TESTS

Test:

- Gateway-to-monolith
- Gateway-to-new-service
- New-service-to-legacy-adapter
- Event producers/consumers
- Mixed-version deployments

Verify required contract compatibility.

---

## 44. DATABASE MIGRATION TESTS

Verify:

- Snapshot consistency
- CDC catch-up
- Duplicate handling
- Missing-record detection
- Referential integrity
- Monetary totals
- Inventory totals
- Write fencing
- Cutover correctness
- Recovery after backfill failure

Use real PostgreSQL integration tests.

---

## 45. CONCURRENCY TESTS

Verify:

- Concurrent writes during migration
- Gateway routing races
- Inventory last-unit reservations
- Duplicate order requests
- CDC replay
- Cutover ownership transition
- Saga compensation
- Data reconciliation during writes

---

## 46. CHAOS AND FAILURE TESTS

Simulate:

- Service crashes
- Database outages
- Kafka outage
- CDC lag
- Gateway failure
- Network partitions
- Lost responses
- Pod termination
- Failed rollout
- Rollback after partial cutover

Document actual executed checks.

---

# PART W — PERFORMANCE ENGINEERING

## 47. PERFORMANCE TESTING

Benchmark:

- Legacy-only traffic
- Shadow traffic
- 1% canary
- 10% canary
- 50% canary
- New-service-only traffic

Measure:

- Offered/achieved RPS
- p50/p95/p99 latency
- Error rate
- Database QPS
- Connection pool utilization
- JVM CPU/heap/GC
- Kafka lag
- CDC lag
- Data parity
- Rollback time
- Cutover duration

Do not invent benchmark results.

Define comparable load conditions for legacy and new implementations.

---

# PART X — DEPLOYMENT AND CI/CD

## 48. DOCKER AND KUBERNETES

Provide:

- Legacy monolith Dockerfile
- Gateway Dockerfile
- Microservice Dockerfiles
- Docker Compose
- PostgreSQL
- Kafka
- Debezium where implemented
- Redis where implemented
- Kubernetes Deployments
- Services
- Ingress/Gateway routing
- ConfigMaps
- Secret references
- HPA
- PodDisruptionBudget
- Readiness/liveness probes
- Graceful shutdown
- Rolling deployments
- CI/CD workflow

The local environment must allow switching between legacy-only, hybrid, and new-service routing.

---

# PART Y — OBSERVABILITY

## 49. MIGRATION DASHBOARDS

Expose:

- Route distribution
- Legacy request rate
- New service request rate
- Shadow parity
- Canary error rate
- Canary latency
- CDC lag
- Migration backlog
- Reconciliation mismatches
- Saga failures
- Database connection utilization
- Rollback events
- Service health
- Remaining legacy dependencies

Provide:

- Prometheus
- Grafana
- OpenTelemetry
- Structured logs
- Correlation IDs
- SLOs
- Alerts
- Operational runbooks

Avoid high-cardinality customer/order IDs as metric labels.

---

# PART Z — IMPLEMENTATION WORKFLOW

## 50. DEVELOPMENT PHASES

### Phase 1 — Legacy Assessment

Inspect or create the baseline monolith, identify coupling, map transactions, and establish characterization tests.

### Phase 2 — Domain Decomposition

Define bounded contexts, target ownership, extraction order, and migration risks.

### Phase 3 — Strangler Gateway

Introduce routing without changing business behavior.

### Phase 4 — Catalog Extraction

Create Catalog Service, data migration, ACL, shadow testing, canary, and safe cutover.

### Phase 5 — Inventory Extraction

Move inventory ownership, implement reservation correctness, and reconcile state.

### Phase 6 — Event Integration

Implement outbox/CDC where justified, idempotent consumption, and schema compatibility.

### Phase 7 — Order/Payment Extraction

Introduce durable distributed workflows and Saga compensation.

### Phase 8 — Zero-Downtime Migration

Implement expand-contract, data backfill, write fencing, cutover, and supported rollback.

### Phase 9 — Security Modernization

Implement authentication, authorization, service identity, and least-privilege access.

### Phase 10 — Reliability

Implement timeouts, retries, circuit breakers, bulkheads, and failure recovery.

### Phase 11 — Observability

Implement dashboards, tracing, parity metrics, CDC monitoring, and migration health signals.

### Phase 12 — Deployment

Implement Docker Compose, Kubernetes, CI/CD, canary, and rollback automation.

### Phase 13 — Verification

Run characterization, integration, contract, migration, concurrency, security, and failure tests.

### Phase 14 — Performance

Benchmark legacy, hybrid, and target architectures under comparable loads.

### Phase 15 — Final Review and Retirement

Validate business parity, data ownership, recovery procedures, and criteria for removing remaining legacy dependencies.

For each phase:

- Implement actual files.
- Maintain a buildable state.
- Run relevant verification.
- Fix discovered defects.
- Preserve unrelated work.
- Document architectural decisions.
- Report actual results and risks.

If repository and execution tools exist, implement directly.

Otherwise, provide complete source files and runnable commands without claiming execution.

---

# PART AA — REQUIRED DELIVERABLES

## 51. COMPLETE PROJECT OUTPUTS

### Deliverable 1 — Requirements

- Functional requirements
- Non-functional requirements
- Migration assumptions
- Correctness invariants
- Acceptance criteria

### Deliverable 2 — Architecture

- Before/transition/target diagrams
- Domain decomposition
- Bounded contexts
- Dependency map
- Migration roadmap
- Data ownership matrix

### Deliverable 3 — Low-Level Design

- UML diagrams
- Sequence diagrams
- API contracts
- Database schemas
- ACL design
- CDC/outbox architecture
- Saga workflows

### Deliverable 4 — Source Code

- Legacy monolith
- Strangler Gateway
- Catalog Service
- Inventory Service
- Order Service
- Payment Service
- Migration utilities
- Anti-corruption layers
- Outbox/CDC integration
- Routing configuration

### Deliverable 5 — Migration Demonstration

- Legacy-only mode
- Shadow mode
- Canary mode
- New-service-only mode
- Safe write cutover
- Supported rollback
- Data reconciliation

### Deliverable 6 — Automated Tests

- Characterization tests
- Unit tests
- Integration tests
- Contract tests
- Concurrency tests
- Migration tests
- Failure recovery tests
- Security tests

### Deliverable 7 — Infrastructure

- Docker
- Docker Compose
- Kubernetes/Helm
- CI/CD
- Observability

### Deliverable 8 — Performance

- Legacy baseline
- Hybrid benchmarks
- Target architecture benchmarks
- Measured comparisons
- Capacity estimates

### Deliverable 9 — Documentation

- README
- Architecture Decision Records
- Migration roadmap
- Domain decomposition guide
- Data migration procedure
- Cutover/rollback runbook
- Security guide
- Deployment guide
- Legacy retirement checklist
- Known limitations

---

# PART AB — MANDATORY ARCHITECTURE REVIEW

## 52. ANSWER THESE 45 QUESTIONS

1. What is the Strangler Fig Pattern?
2. Why is incremental migration safer than a big-bang rewrite?
3. How do you identify microservice boundaries?
4. How does DDD help monolith decomposition?
5. What is a bounded context?
6. What is an anti-corruption layer?
7. How do you prevent legacy domain models from leaking into new services?
8. How do you introduce an API Gateway without changing behavior?
9. How do you implement endpoint-based strangler routing?
10. How do you perform safe shadow traffic?
11. Why must shadow writes be isolated?
12. How do you implement canary routing?
13. What metrics determine rollout progression?
14. How do you maintain API compatibility?
15. What are characterization tests?
16. How do you migrate a shared database?
17. Why is database-per-service difficult during migration?
18. What is Change Data Capture?
19. How does Debezium work with PostgreSQL?
20. How do you avoid losing changes between snapshot and CDC?
21. How do you prevent duplicate CDC events?
22. How do you handle out-of-order change events?
23. Why are naive dual writes dangerous?
24. How does Transactional Outbox help migration?
25. How do you reconcile source and target data?
26. How do you perform a safe write ownership cutover?
27. What is the expand-contract database pattern?
28. How do you avoid zero-downtime migration failures?
29. What happens if rollback occurs after new-only writes?
30. How do you migrate cross-module ACID transactions?
31. When is Saga appropriate?
32. How do you handle compensation failures?
33. How do you prevent double inventory reservation during migration?
34. How do you prevent duplicate payment operations?
35. How do you modernize authentication without breaking SSO?
36. How do you prevent shared database coupling from persisting?
37. How do you handle legacy batch jobs during cutover?
38. How do you measure migration parity?
39. How do you benchmark legacy versus microservices?
40. How do you handle service failure during canary?
41. How do you define rollback gates?
42. How do you manage schema compatibility during rolling deployment?
43. How do you remove obsolete legacy code safely?
44. When should a monolith not be decomposed into microservices?
45. What evidence is required before declaring migration complete?

For each provide:

- Architecture reasoning
- Correctness guarantees
- Data consistency
- Operational impact
- Failure recovery
- Performance implications
- Trade-offs

---

# DEFINITION OF DONE

## 53. ACCEPTANCE CRITERIA

The implementation is complete only when:

- Legacy monolith builds and runs.
- Baseline business functionality is characterized and tested.
- Strangler Gateway routes legacy traffic.
- Shadow routing works for safe read operations.
- Parity comparisons are recorded.
- Canary routing is controllable.
- Catalog Service is independently deployable.
- Catalog data migration and reconciliation work.
- Inventory Service owns its authoritative stock data after cutover.
- Concurrent reservations prevent overselling.
- Database ownership is explicit.
- CDC/outbox integration is recoverable.
- Duplicate integration events are handled safely.
- Order/payment distributed workflow is correct under documented assumptions.
- Saga compensation and reconciliation are functional.
- Data cutover uses a safe ownership transition.
- Rollback behavior is documented and tested where supported.
- API contracts remain compatible or intentional breaking changes are documented.
- Security boundaries are enforced.
- Docker Compose supports legacy, hybrid, and target migration demonstrations.
- Kubernetes configuration is provided.
- Automated tests cover critical migration failures.
- Migration-specific metrics are exposed.
- Performance comparisons are measured or marked unverified.
- Legacy dependencies are retired only after validated replacement.
- Remaining migration risks and operational limitations are documented.

Do not declare migration complete merely because new microservices respond successfully.

---

# FINAL EXECUTION INSTRUCTIONS

Build a **real enterprise Legacy Monolith-to-Microservices Migration using the Strangler Fig Pattern with Java 21 and
Spring Boot**.

Prioritize:

1. **Business continuity:** Preserve observable business functionality during migration.
2. **Data integrity:** No uncontrolled competing writes or lost committed changes.
3. **Incremental delivery:** Extract one bounded context at a time.
4. **Safe routing:** Controlled shadow traffic, canary rollout, and feature routing.
5. **Database modernization:** Explicit ownership, CDC, reconciliation, and safe cutover.
6. **Distributed transaction correctness:** Recoverable Saga workflows when ACID boundaries are split.
7. **Rollback safety:** Documented, tested recovery procedures for each migration phase.
8. **Security:** No weakening of authentication, authorization, or data protection.
9. **Reliability:** Bounded retries, resilience controls, and operational recovery.
10. **Maintainability:** Clear domain ownership and minimal legacy coupling.
11. **Observability:** Migration parity, rollout progress, CDC lag, and failure visibility.
12. **Reproducibility:** Complete source code, tests, deployment infrastructure, and migration runbooks.

Begin with a concise assessment of the legacy application.

If a repository exists, inspect it before modifying anything.

If no repository exists, create a representative legacy application and demonstrate a realistic phased migration.

Then implement the migration incrementally.

Use available development tools to create files, compile, run tests, validate data migration, demonstrate controlled
routing, and fix defects.

At completion report:

- Baseline legacy architecture
- Target microservices architecture
- Extracted bounded contexts
- Migration decisions
- Data ownership transitions
- Cutover and rollback guarantees
- Project structure
- Exact build and run commands
- Tests actually executed
- Performance measurements
- Known limitations
- Remaining migration and production risks

**Do not stop after HLD or LLD. Deliver the complete end-to-end Strangler Pattern migration demonstration with legacy
monolith, routing gateway, extracted microservices, data migration, CDC/outbox where justified, Saga workflows,
automated verification, canary controls, and deployment infrastructure.**