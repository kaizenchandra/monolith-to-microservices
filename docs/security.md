# Trust and deployment

Local HTTP Basic users are explicit demo credentials bound to localhost. Reader cannot mutate or administer. Internal
APIs require SERVICE; ownership/routing/import/recovery require ADMIN. Product reads have no private customer fields.
JWT production profile validates signature through issuer discovery, issuer/time and audience, then scope authorities.
`JWT_ISSUER`, `JWT_AUDIENCE`, `SERVICE_TOKEN` must come from secret management. Service tokens must have least scopes
and automated rotation; the sample reads a static token at startup, so expiry/rotation requires redeploy. Do not deploy
default Basic mode publicly.

TLS terminates at ingress, with private service networks and mesh mTLS as a production requirement. Helm references
`<service>-runtime` Secrets: DB_URL, DB_USER, DB_PASSWORD, SPRING_FLYWAY_USER, SPRING_FLYWAY_PASSWORD, JWT_ISSUER,
JWT_AUDIENCE, SERVICE_TOKEN and broker security settings. Per-service PostgreSQL runtime roles get only their database
DML, migrator roles own DDL; Compose provisions isolated runtime/migrator roles and revokes cross-database CONNECT.
Passwords are deliberately shared local-only demo defaults; production secrets must differ. Separate migrator jobs
before immutable application rollout are recommended. Configure Kafka SASL/TLS through Spring's environment properties
and separate producer/consumer ACLs. NetworkPolicy/service mesh/IAM infrastructure are deployment prerequisites.

Production user ownership checks for orders/customers are not implemented: demo roles are store-operator roles, not
shopper accounts. Never expose these APIs as a consumer storefront. A real rollout must derive customer reference from
JWT subject/tenant and enforce row authorization in both legacy and modern paths. Central gateway authorization is
insufficient if callers can access services directly.

Legacy SSO was not supplied. A production session bridge should exchange validated sessions for short-lived
audience-scoped service tokens; never forward cookies blindly or mint identity from caller headers. CSRF is disabled
because this demo has stateless credentials and no session cookies; session bridging would require a CSRF review.
Structured log correlation is bounded and sanitized; audit identity/epoch/import approvals should be sent to an
immutable audit system before production. Gateway does not log bodies or credentials.
