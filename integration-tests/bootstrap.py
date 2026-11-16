#!/usr/bin/env python3
"""Assert complete cold-start provisioning before exercising business migration."""
import json
import pathlib
import subprocess

DATABASES = ["legacymonolith", "stranglergateway", "catalogservice",
             "inventoryservice", "orderservice", "paymentservice"]


def verify():
    values = ",".join("('" + name + "')" for name in DATABASES)
    query = f"""
    WITH services(name) AS (VALUES {values})
    SELECT json_build_object(
      'bootstrap_version', (SELECT version FROM public.demo_bootstrap_complete),
      'services', (SELECT json_agg(json_build_object(
        'database', s.name,
        'owner', pg_get_userbyid(d.datdba),
        'runtime_superuser', r.rolsuper,
        'migrator_superuser', m.rolsuper,
        'runtime_can_connect', has_database_privilege(r.oid, d.oid, 'CONNECT'),
        'cross_database_connections', (SELECT count(*) FROM services other
          WHERE other.name <> s.name AND
            has_database_privilege(r.oid, other.name, 'CONNECT'))
      ) ORDER BY s.name)
      FROM services s JOIN pg_database d ON d.datname = s.name
      JOIN pg_roles r ON r.rolname = s.name || '_app'
      JOIN pg_roles m ON m.rolname = s.name || '_migrator'));
    """
    raw = subprocess.check_output(
        ["docker", "compose", "exec", "-T", "postgres", "psql", "-v", "ON_ERROR_STOP=1",
         "-U", "demo", "-d", "postgres", "-Atc", query], text=True)
    report = json.loads(raw)
    assert report["bootstrap_version"] == 1, report
    assert len(report["services"]) == len(DATABASES), report
    for row in report["services"]:
        assert row["owner"] == row["database"] + "_migrator", row
        assert row["runtime_can_connect"] is True, row
        assert row["runtime_superuser"] is False, row
        assert row["migrator_superuser"] is False, row
        assert row["cross_database_connections"] == 0, row
    report_path = pathlib.Path("artifacts/final-hardened-run/bootstrap-results.json")
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, indent=2))
    print("PASS complete bootstrap and database role isolation for six services")


if __name__ == "__main__":
    verify()
