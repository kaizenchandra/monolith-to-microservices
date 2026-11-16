-- psql script: backslash commands and their arguments must stay on one line.
create
database legacymonolith;
create
database stranglergateway;
create
database catalogservice;
create
database inventoryservice;
create
database orderservice;
create
database paymentservice;
SELECT 'CREATE ROLE legacymonolith_app LOGIN PASSWORD ''local-only''' WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='legacymonolith_app')\gexec
SELECT 'CREATE ROLE legacymonolith_migrator LOGIN PASSWORD ''local-only''' WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='legacymonolith_migrator')\gexec
ALTER
DATABASE legacymonolith OWNER TO legacymonolith_migrator;
REVOKE CONNECT ON DATABASE legacymonolith FROM PUBLIC;
GRANT
CONNECT
ON DATABASE legacymonolith TO legacymonolith_app, legacymonolith_migrator;
\connect legacymonolith
ALTER
SCHEMA public OWNER TO legacymonolith_migrator;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT
USAGE
ON
SCHEMA
public TO legacymonolith_app;
SELECT format('ALTER TABLE public.%I OWNER TO legacymonolith_migrator', tablename)
FROM pg_tables
WHERE schemaname = 'public'\gexec
GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON ALL TABLES IN SCHEMA public TO legacymonolith_app;
GRANT
USAGE,
SELECT
ON ALL SEQUENCES IN SCHEMA public TO legacymonolith_app;
ALTER
DEFAULT PRIVILEGES FOR ROLE legacymonolith_migrator IN SCHEMA public GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON TABLES TO legacymonolith_app;
ALTER
DEFAULT PRIVILEGES FOR ROLE legacymonolith_migrator IN SCHEMA public GRANT USAGE,
SELECT
ON SEQUENCES TO legacymonolith_app;
\connect postgres
SELECT 'CREATE ROLE stranglergateway_app LOGIN PASSWORD ''local-only''' WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='stranglergateway_app')\gexec
SELECT 'CREATE ROLE stranglergateway_migrator LOGIN PASSWORD ''local-only''' WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='stranglergateway_migrator')\gexec
ALTER
DATABASE stranglergateway OWNER TO stranglergateway_migrator;
REVOKE CONNECT ON DATABASE stranglergateway FROM PUBLIC;
GRANT
CONNECT
ON DATABASE stranglergateway TO stranglergateway_app, stranglergateway_migrator;
\connect stranglergateway
ALTER
SCHEMA public OWNER TO stranglergateway_migrator;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT
USAGE
ON
SCHEMA
public TO stranglergateway_app;
SELECT format('ALTER TABLE public.%I OWNER TO stranglergateway_migrator', tablename)
FROM pg_tables
WHERE schemaname = 'public'\gexec
GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON ALL TABLES IN SCHEMA public TO stranglergateway_app;
GRANT
USAGE,
SELECT
ON ALL SEQUENCES IN SCHEMA public TO stranglergateway_app;
ALTER
DEFAULT PRIVILEGES FOR ROLE stranglergateway_migrator IN SCHEMA public GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON TABLES TO stranglergateway_app;
ALTER
DEFAULT PRIVILEGES FOR ROLE stranglergateway_migrator IN SCHEMA public GRANT USAGE,
SELECT
ON SEQUENCES TO stranglergateway_app;
\connect postgres
SELECT 'CREATE ROLE catalogservice_app LOGIN PASSWORD ''local-only''' WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='catalogservice_app')\gexec
SELECT 'CREATE ROLE catalogservice_migrator LOGIN PASSWORD ''local-only''' WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='catalogservice_migrator')\gexec
ALTER
DATABASE catalogservice OWNER TO catalogservice_migrator;
REVOKE CONNECT ON DATABASE catalogservice FROM PUBLIC;
GRANT
CONNECT
ON DATABASE catalogservice TO catalogservice_app, catalogservice_migrator;
\connect catalogservice
ALTER
SCHEMA public OWNER TO catalogservice_migrator;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT
USAGE
ON
SCHEMA
public TO catalogservice_app;
SELECT format('ALTER TABLE public.%I OWNER TO catalogservice_migrator', tablename)
FROM pg_tables
WHERE schemaname = 'public'\gexec
GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON ALL TABLES IN SCHEMA public TO catalogservice_app;
GRANT
USAGE,
SELECT
ON ALL SEQUENCES IN SCHEMA public TO catalogservice_app;
ALTER
DEFAULT PRIVILEGES FOR ROLE catalogservice_migrator IN SCHEMA public GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON TABLES TO catalogservice_app;
ALTER
DEFAULT PRIVILEGES FOR ROLE catalogservice_migrator IN SCHEMA public GRANT USAGE,
SELECT
ON SEQUENCES TO catalogservice_app;
\connect postgres
SELECT 'CREATE ROLE inventoryservice_app LOGIN PASSWORD ''local-only''' WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='inventoryservice_app')\gexec
SELECT 'CREATE ROLE inventoryservice_migrator LOGIN PASSWORD ''local-only''' WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='inventoryservice_migrator')\gexec
ALTER
DATABASE inventoryservice OWNER TO inventoryservice_migrator;
REVOKE CONNECT ON DATABASE inventoryservice FROM PUBLIC;
GRANT
CONNECT
ON DATABASE inventoryservice TO inventoryservice_app, inventoryservice_migrator;
\connect inventoryservice
ALTER
SCHEMA public OWNER TO inventoryservice_migrator;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT
USAGE
ON
SCHEMA
public TO inventoryservice_app;
SELECT format('ALTER TABLE public.%I OWNER TO inventoryservice_migrator', tablename)
FROM pg_tables
WHERE schemaname = 'public'\gexec
GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON ALL TABLES IN SCHEMA public TO inventoryservice_app;
GRANT
USAGE,
SELECT
ON ALL SEQUENCES IN SCHEMA public TO inventoryservice_app;
ALTER
DEFAULT PRIVILEGES FOR ROLE inventoryservice_migrator IN SCHEMA public GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON TABLES TO inventoryservice_app;
ALTER
DEFAULT PRIVILEGES FOR ROLE inventoryservice_migrator IN SCHEMA public GRANT USAGE,
SELECT
ON SEQUENCES TO inventoryservice_app;
\connect postgres
SELECT 'CREATE ROLE orderservice_app LOGIN PASSWORD ''local-only''' WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='orderservice_app')\gexec
SELECT 'CREATE ROLE orderservice_migrator LOGIN PASSWORD ''local-only''' WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='orderservice_migrator')\gexec
ALTER
DATABASE orderservice OWNER TO orderservice_migrator;
REVOKE CONNECT ON DATABASE orderservice FROM PUBLIC;
GRANT
CONNECT
ON DATABASE orderservice TO orderservice_app, orderservice_migrator;
\connect orderservice
ALTER
SCHEMA public OWNER TO orderservice_migrator;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT
USAGE
ON
SCHEMA
public TO orderservice_app;
SELECT format('ALTER TABLE public.%I OWNER TO orderservice_migrator', tablename)
FROM pg_tables
WHERE schemaname = 'public'\gexec
GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON ALL TABLES IN SCHEMA public TO orderservice_app;
GRANT
USAGE,
SELECT
ON ALL SEQUENCES IN SCHEMA public TO orderservice_app;
ALTER
DEFAULT PRIVILEGES FOR ROLE orderservice_migrator IN SCHEMA public GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON TABLES TO orderservice_app;
ALTER
DEFAULT PRIVILEGES FOR ROLE orderservice_migrator IN SCHEMA public GRANT USAGE,
SELECT
ON SEQUENCES TO orderservice_app;
\connect postgres
SELECT 'CREATE ROLE paymentservice_app LOGIN PASSWORD ''local-only''' WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='paymentservice_app')\gexec
SELECT 'CREATE ROLE paymentservice_migrator LOGIN PASSWORD ''local-only''' WHERE NOT EXISTS(SELECT FROM pg_roles WHERE rolname='paymentservice_migrator')\gexec
ALTER
DATABASE paymentservice OWNER TO paymentservice_migrator;
REVOKE CONNECT ON DATABASE paymentservice FROM PUBLIC;
GRANT
CONNECT
ON DATABASE paymentservice TO paymentservice_app, paymentservice_migrator;
\connect paymentservice
ALTER
SCHEMA public OWNER TO paymentservice_migrator;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT
USAGE
ON
SCHEMA
public TO paymentservice_app;
SELECT format('ALTER TABLE public.%I OWNER TO paymentservice_migrator', tablename)
FROM pg_tables
WHERE schemaname = 'public'\gexec
GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON ALL TABLES IN SCHEMA public TO paymentservice_app;
GRANT
USAGE,
SELECT
ON ALL SEQUENCES IN SCHEMA public TO paymentservice_app;
ALTER
DEFAULT PRIVILEGES FOR ROLE paymentservice_migrator IN SCHEMA public GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON TABLES TO paymentservice_app;
ALTER
DEFAULT PRIVILEGES FOR ROLE paymentservice_migrator IN SCHEMA public GRANT USAGE,
SELECT
ON SEQUENCES TO paymentservice_app;
\connect postgres

-- Readiness must not accept an interrupted, partially provisioned cluster.
CREATE TABLE public.demo_bootstrap_complete(version integer PRIMARY KEY);
INSERT INTO public.demo_bootstrap_complete VALUES (1);
