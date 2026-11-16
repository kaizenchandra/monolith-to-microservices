#!/usr/bin/env python3
"""Cross-service integrity and exact Kafka-consumer progress after the full suites."""
import json
import pathlib
import subprocess
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / "migration-tools"))
from migrate import call, snapshot, state

TOPIC = "commerce.events.v1"
checks = []


def check(name, condition):
    assert condition, name
    checks.append(name)
    print("PASS", name, flush=True)


def dc(*args):
    return subprocess.check_output(["docker", "compose", *args], text=True, timeout=30)


def rows(database, table):
    # Table names are fixed test-owned identifiers, not caller input.
    return json.loads(dc("exec", "-T", "postgres", "psql", "-v", "ON_ERROR_STOP=1",
                         "-U", "demo", "-d", database, "-Atc",
                         f"select coalesce(jsonb_agg(to_jsonb(t)), '[]'::jsonb) from {table} t"))


def kafka(script, *args):
    return dc("exec", "-T", "kafka", "/opt/kafka/bin/" + script,
              "--bootstrap-server", "kafka:19092", *args)


# Wait for all producers, then capture a fixed broker boundary, not a generic metric delta.
for _ in range(60):
    if all(all(row["published"] for row in rows(db, "outbox"))
           for db in ["catalogservice", "inventoryservice", "orderservice", "paymentservice"]):
        break
    time.sleep(1)
else:
    raise AssertionError("outbox backlog remains")
check("all service outboxes drained", True)
ends = {}
for line in kafka("kafka-get-offsets.sh", "--topic", TOPIC, "--time", "-1").splitlines():
    fields = line.split(":")
    if len(fields) == 3 and fields[0] == TOPIC:
        ends[int(fields[1])] = int(fields[2])
assert ends, "no broker offsets returned"
committed = {}
for _ in range(30):
    raw = kafka("kafka-consumer-groups.sh", "--describe", "--group", "legacy-projections-v1", "--offsets")
    committed = {}
    for line in raw.splitlines():
        fields = line.split()
        if len(fields) >= 6 and fields[0] == "legacy-projections-v1" and fields[1] == TOPIC and fields[3].isdigit():
            committed[int(fields[2])] = int(fields[3])
    if all(committed.get(partition, -1) >= end for partition, end in ends.items()):
        break
    time.sleep(1)
else:
    raise AssertionError(f"consumer not caught up: broker={ends}, committed={committed}")
check("consumer committed through every captured broker partition including replay", True)

orders = rows("orderservice", "checkout")
confirmed = {row["id"]: row for row in orders if row["state"] == "CONFIRMED"}
check("all checkout workflows terminal", all(row["state"] in {"CONFIRMED", "CANCELLED"} for row in orders))
auths = {row["id"]: row for row in rows("paymentservice", "authorizations")}
check("one matching authorization per confirmed order",
      set(auths) == set(confirmed) and all(auths[key]["state"] == "AUTHORIZED" and
      auths[key]["amount_cents"] == order["amount_cents"] for key, order in confirmed.items()))
projections = {row["id"]: row for row in rows("legacymonolith", "modern_orders")}
check("modern financial projections match orders exactly",
      set(projections) == set(confirmed) and all(projections[key]["amount_cents"] == order["amount_cents"]
                                               for key, order in confirmed.items()))
events = rows("orderservice", "outbox")
inbox = rows("legacymonolith", "inbox")
check("each confirmed event applied once in inbox",
      len(events) == len(confirmed) and {row["id"] for row in events} == {row["event_id"] for row in inbox})
historic = rows("legacymonolith", "orders")
expected_ids = set(confirmed) | {row["id"] for row in historic}
for table in ["shipments", "notifications"]:
    effects = rows("legacymonolith", table)
    check("exactly one " + table + " intent per confirmed or historical order",
          len(effects) == len(expected_ids) and {row["order_id"] for row in effects} == expected_ids)
expected_total = sum(row["amount_cents"] for row in historic) + sum(row["amount_cents"] for row in confirmed.values())
report = call("http://localhost:28082", "/api/report")
check("report count and monetary total reconcile", report["orders"] == len(expected_ids) and report["amount_cents"] == expected_total)

legacy_inventory = snapshot("legacy", "inventory")
modern_inventory = snapshot("inventory", "inventory")
def conserved(data):
    total = {row["product_id"]: row["available"] for row in data["stock"]}
    for row in data["reservations"]:
        if row["state"] == "RESERVED": total[row["product_id"]] += row["quantity"]
    return total
check("inventory quantity conserved across ownership transfer", conserved(legacy_inventory) == conserved(modern_inventory))
reserved = {row["id"]: row for row in modern_inventory["reservations"] if row["state"] == "RESERVED"}
check("no orphan or duplicate active reservations",
      set(reserved) == set(confirmed) and all(reserved[key]["product_id"] == order["product_id"] and
      reserved[key]["quantity"] == order["quantity"] for key, order in confirmed.items()))
check("catalog reconciliation remains exact after recovery", snapshot("legacy", "catalog") == snapshot("catalog", "catalog"))
check("only modern capabilities authoritative",
      all(state("legacy", cap)["state"] == "MODERN" for cap in ["catalog", "inventory", "checkout"]) and
      all(state(cap, cap)["state"] == "MODERN" for cap in ["catalog", "inventory"]))
call("legacy", "/internal/admin/routing", {"mode": "MODERN", "percent": 100})
check("new-service-only catalog routing preserves contract", call("http://localhost:28082", "/api/products/1") == call("catalog", "/api/products/1"))
report_path = pathlib.Path("artifacts/final-hardened-run/postflight-results.json")
report_path.parent.mkdir(parents=True, exist_ok=True)
report_path.write_text(json.dumps({
    "checks": checks, "count": len(checks), "broker_end_offsets": ends, "consumer_committed_offsets": committed,
    "modern_confirmed_orders": len(confirmed), "historical_orders": len(historic),
    "amount_cents_total": expected_total, "inventory_conservation": conserved(modern_inventory)}, indent=2))
