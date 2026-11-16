# ADR 001 — Explicit capability ownership and bounded pauses

Accepted: preserve source facade and historical order identity; first extract Catalog, then Inventory and coordinated
checkout/payment. Source/target ownership rows and epochs guard business writers and administrative transitions.
Full-copy fenced transfer is chosen for verifiable small data. Production snapshot+CDC is a later workload-driven
choice. No independent active-active writes, no write fallback, no implicit Inventory rollback after Saga activation.

Consequences: real write unavailability during copy; control depends on monolith database; tables retained for recovery.
A small servlet router is preferable to an unused Cloud dependency. Technical platform sharing reduces duplication
without business entities. Local superuser/Basic defaults are isolated demo choices, never production guarantees.

Version evidence: [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
confirms Boot 3.5.16 supports Java 21 (range 17–25). Dependency versions otherwise follow its BOM. JDK 21 is explicitly
used for builds, avoiding the machine's Maven default JDK 27.
