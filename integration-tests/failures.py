#!/usr/bin/env python3
"""Run after demo.py on its migrated dataset. Operates only this Compose project."""
import json
import pathlib
import subprocess
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / "migration-tools"))
from migrate import *

checks = []
RUN = str(time.time_ns())
CRASH = "crash-recovery-" + RUN
BROKER = "broker-recovery-" + RUN


def docker(*args): return subprocess.check_output(["docker", "compose", *args], text=True)


def sql(database, q): return docker("exec", "-T", "postgres", "psql", "-U", "demo", "-d", database, "-Atc", q).strip()


def until(fn, timeout=180):
    start = time.monotonic()
    while time.monotonic() - start < timeout:
        try:
            if fn(): return
        except Exception:
            pass
        time.sleep(1)
    raise AssertionError("condition timeout")


def check(name, ok): assert ok, name;checks.append(name);print("PASS", name, flush=True)


def committed():
    request = urllib.request.Request("http://localhost:28081/actuator/prometheus", headers={
        "Authorization": "Basic " + base64.b64encode(b"service:local-only").decode()})
    with urllib.request.urlopen(request, timeout=10) as r: lines = r.read().decode().splitlines()
    values = [float(line.split()[-1]) for line in lines if line.startswith("kafka_consumer_coordinator_commit_total{")]
    if not values: raise AssertionError("consumer commit metric unavailable")
    return sum(values)


G = "http://localhost:28082"


def create(body):
    # Same business intent/key after ambiguous response; gateway never retries writes.
    until(lambda: bool(call(G, "/api/orders", body)), 90)


for port in range(28081, 28087):
    until(lambda: call("http://localhost:" + str(port), "/actuator/health")["status"] == "UP", 300)
try:
    docker("stop", "payment")
    create({"id": CRASH, "customer_id": 1, "product_id": 2, "quantity": 1})
    until(lambda: call(G, "/api/orders/" + CRASH)["state"] == "RESERVED", 20)
    docker("restart", "order")
    docker("start", "payment")
    until(lambda: call("http://localhost:28086", "/actuator/health")["status"] == "UP")
    until(lambda: call("http://localhost:28085", "/actuator/health")["status"] == "UP")
    if call(G, "/api/orders/" + CRASH)["state"] == "MANUAL": call("http://localhost:28085",
                                                                  "/internal/admin/recover/" + CRASH + "?state=RESERVED",
                                                                  {})
    until(lambda: call(G, "/api/orders/" + CRASH)["state"] == "CONFIRMED")
    check("saga resumes after dependency outage and worker restart", True)
    check("one reservation after replay",
          sql("inventoryservice", f"select count(*) from reservations where id='{CRASH}'") == "1")
    docker("stop", "kafka")
    create({"id": BROKER, "customer_id": 1, "product_id": 2, "quantity": 1})
    until(lambda: call(G, "/api/orders/" + BROKER)["state"] == "CONFIRMED")
    check("state commits while broker unavailable",
          int(sql("orderservice", "select count(*) from outbox where not published")) > 0)
    docker("start", "kafka")
    until(lambda: sql("orderservice", "select count(*) from outbox where not published") == "0", 120)
    until(lambda: sql("legacymonolith", f"select count(*) from modern_orders where id='{BROKER}'") == "1")
    check("outbox recovers and consumer projects", True)
    before = committed()
    sql("orderservice", f"update outbox set published=false where aggregate_id='{BROKER}'")
    until(lambda: sql("orderservice", "select count(*) from outbox where not published") == "0")
    until(lambda: committed() > before)
    check("duplicate event suppresses duplicate fulfillment",
          sql("legacymonolith", f"select count(*) from shipments where order_id='{BROKER}'") == "1")
    # A fenced transfer interrupted before import can be safely resumed.
    transition("legacy", "catalog", "FROZEN")
    cutover("catalog")
    check("cutover resumes after source delegation fence", state("legacy", "catalog")["state"] == "MODERN")
finally:
    docker("start", "payment", "kafka", "order")
pathlib.Path("artifacts/failure-results.json").write_text(
    json.dumps({"checks": checks, "count": len(checks)}, indent=2))
