#!/usr/bin/env python3
"""Destructive to DEMO dataset. Requires fresh Compose volumes, records actual assertions."""
import concurrent.futures
import json
import pathlib
import sys
import time
import urllib.error

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / "migration-tools"))
from migrate import *

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / "contract-tests"))
from verify import product as verify_product

checks = []


def check(name, condition):
    assert condition, name
    checks.append(name);
    print("PASS", name)


def expect(status, fn):
    try:
        fn()
    except urllib.error.HTTPError as e:
        return e.code == status
    return False


G = "http://localhost:28082"
for service, port in [("legacy", 8081), ("gateway", 8082), ("catalog", 8083), ("inventory", 8084), ("order", 8085),
                      ("payment", 8086)]:
    for attempt in range(300):
        try:
            call("http://localhost:" + str(port + 20000), "/actuator/health");break
        except Exception:
            time.sleep(1)
    else:
        raise RuntimeError(service + " unavailable")
check("reader cannot write", expect(403, lambda: call(G, "/api/products", {}, user="reader")))
check("reader cannot administer",
      expect(403, lambda: call("legacy", "/internal/admin/routing", {"mode": "MODERN", "percent": 100}, user="reader")))
check("missing product contract", expect(404, lambda: call(G, "/api/products/999")))
p = verify_product(call(G, "/api/products/1"));
check("legacy characterization cents", p["price_cents"] == 12999)
order = {"id": "baseline-1", "customer_id": 1, "product_id": 1, "quantity": 1}
o = call(G, "/api/orders", order);
check("legacy checkout", o["state"] == "CONFIRMED")
check("legacy duplicate", call(G, "/api/orders", order) == o)
sync("catalog");
reconcile("legacy", "catalog", "catalog")
call("legacy", "/internal/admin/routing", {"mode": "SHADOW", "percent": 0})
for _ in range(15): check("shadow preserves response", call(G, "/api/products/1") == p)
time.sleep(2)
metrics = urllib.request.Request(G + "/actuator/prometheus",
                                 headers={"Authorization": "Basic " + base64.b64encode(b"service:local-only").decode()})
with urllib.request.urlopen(metrics) as r: check("shadow parity recorded",
                                                 'strangler_shadow_total{result="match"' in r.read().decode())
for percent in [1, 5, 10, 25, 50, 100]:
    call("legacy", "/internal/admin/routing", {"mode": "CANARY", "percent": percent});
    check("canary " + str(percent), call(G, "/api/products/1") == p)
old_owner = state("legacy", "catalog")
transition("legacy", "catalog", "FROZEN")
check("frozen legacy reads never serve stale data", expect(503, lambda: call("legacy", "/api/products/1")))
check("frozen source rejects catalog writes", expect(409, lambda: call("legacy", "/api/products", p)))
check("replica rejects competing catalog writes", expect(409, lambda: call("catalog", "/api/products", p)))
check("stale ownership epoch rejected", expect(409, lambda: call("legacy", "/internal/admin/ownership/catalog",
                                                               {"expected": "FROZEN", "next": "LEGACY",
                                                                "epoch": old_owner["epoch"]})))
transition("legacy", "catalog", "LEGACY")
cutover("catalog")
p["name"] = "Updated Keyboard";
changed = verify_product(call(G, "/api/products", p));
check("modern write version", changed["version"] == 2)
check("stale write rejected", expect(409, lambda: call(G, "/api/products", p)))
rollback("catalog");
check("reverse sync retains modern write", call(G, "/api/products/1") == changed)
cutover("catalog");
cutover("inventory");
checkout()
check("baseline id retained across cutover", call(G, "/api/orders", order) == o)


# Nine units remain. Twenty parallel distinct reservations cannot oversell.
def reserve(i):
    try:
        call("http://localhost:28084", "/internal/reserve",
             {"id": "race-" + str(i), "product_id": 1, "quantity": 1});return True
    except urllib.error.HTTPError as e:
        if e.code == 409: return False
        raise


with concurrent.futures.ThreadPoolExecutor(max_workers=12) as ex: results = list(ex.map(reserve, range(20)))
check("last unit no overselling", sum(results) == 9)
stock = snapshot("inventory", "inventory");
check("stock nonnegative", stock["stock"][0]["available"] == 0)
for i, ok in enumerate(results):
    if ok: call("http://localhost:28084", "/internal/release", {"id": "race-" + str(i), "product_id": 1, "quantity": 1})
req = {"id": "saga-1", "customer_id": 1, "product_id": 2, "quantity": 2}
call(G, "/api/orders", req);
call(G, "/api/orders", req)
for _ in range(30):
    row = call(G, "/api/orders/saga-1")
    if row["state"] == "CONFIRMED": break
    time.sleep(1)
check("durable saga confirmed", row["state"] == "CONFIRMED")
check("conflicting duplicate rejected", expect(409, lambda: call(G, "/api/orders", dict(req, quantity=3))))
req2 = dict(req, id="saga-no-stock", product_id=1, quantity=1000);
call(G, "/api/orders", req2)
for _ in range(30):
    r = call(G, "/api/orders/saga-no-stock")
    if r["state"] == "CANCELLED": break
    time.sleep(1)
check("saga compensates insufficient inventory", r["state"] == "CANCELLED")

# Exercise the final monetary and rollback supplier policies through real HTTP/services.
check("missing product price rejected", expect(400, lambda: call(G, "/api/products",
                                                               {"id": 1, "name": "Missing price", "version": 2,
                                                                "deleted": False})))
check("missing payment amount rejected", expect(400, lambda: call("http://localhost:28086", "/internal/authorize",
                                                                 {"id": "missing-amount"})))
cable = call(G, "/api/products/2")
free_product = call(G, "/api/products", dict(cable, price_cents=0))
call(G, "/api/orders", dict(req, id="free-order", quantity=1))
for _ in range(30):
    free_order = call(G, "/api/orders/free-order")
    if free_order["state"] == "CONFIRMED": break
    time.sleep(1)
check("zero-value saga confirmed", free_order["state"] == "CONFIRMED" and free_order["amount_cents"] == 0)
zero_auth = {"id": "free-order", "amount_cents": 0}
first_auth = call("http://localhost:28086", "/internal/authorize", zero_auth)
check("zero authorization idempotent", call("http://localhost:28086", "/internal/authorize", zero_auth) == first_auth)
check("conflicting payment amount rejected", expect(409, lambda: call("http://localhost:28086", "/internal/authorize",
                                                                      dict(zero_auth, amount_cents=1))))
call(G, "/api/products", dict(free_product, price_cents=cable["price_cents"]))

rollback("catalog")
legacy_product = call(G, "/api/products/2")
repriced = call(G, "/api/products", dict(legacy_product, price_cents=cable["price_cents"] + 100))
check("frozen modern replica remains distinct during rollback",
      call("catalog", "/api/products/2")["price_cents"] == cable["price_cents"])
call(G, "/api/orders", dict(req, id="rollback-priced-order", quantity=1))
for _ in range(30):
    rollback_order = call(G, "/api/orders/rollback-priced-order")
    if rollback_order["state"] == "CONFIRMED": break
    time.sleep(1)
check("modern checkout uses authoritative legacy price after rollback",
      rollback_order["state"] == "CONFIRMED" and rollback_order["amount_cents"] == repriced["price_cents"])
call(G, "/api/products", dict(repriced, price_cents=cable["price_cents"]))
cutover("catalog")
pathlib.Path("artifacts/e2e-results.json").write_text(
    json.dumps({"checks": checks, "count": len(checks), "time": time.time()}, indent=2))
