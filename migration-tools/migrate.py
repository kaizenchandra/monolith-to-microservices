#!/usr/bin/env python3
"""Local demo protocol. Run through one operator; API epochs reject stale transitions.
Full snapshots are intentionally bounded demo data, not a production CDC substitute.
"""
import argparse
import base64
import hashlib
import json
import os
import pathlib
import time
import urllib.error
import urllib.request

URLS = {"legacy": "http://localhost:28081", "catalog": "http://localhost:28083", "inventory": "http://localhost:28084"}


def call(service, path, data=None, user="admin"):
    root = URLS.get(service, service)
    token = base64.b64encode((user + ":" + os.getenv("DEMO_PASSWORD", "local-only")).encode()).decode()
    request = urllib.request.Request(root + path, data=None if data is None else json.dumps(data).encode(),
                                     headers={"Authorization": "Basic " + token, "Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=2 if path.startswith("/actuator/health") else 30) as r:
        b = r.read();
        return json.loads(b) if b else None


def state(service, cap): return call(service, "/internal/ownership/" + cap)


def transition(service, cap, next):
    old = state(service, cap)
    if old["state"] == next: return old
    return call(service, "/internal/admin/ownership/" + cap,
                {"expected": old["state"], "epoch": old["epoch"], "next": next})


def snapshot(service, cap):
    data = call(service, "/internal/export/" + cap);
    data.pop("owner");
    return data


def digest(data): return hashlib.sha256(json.dumps(data, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


def reconcile(a, b, cap):
    left, right = snapshot(a, cap), snapshot(b, cap)
    report = {"capability": cap, "source": a, "target": b, "source_digest": digest(left),
              "target_digest": digest(right), "equal": left == right, "rows": {k: len(v) for k, v in left.items()},
              "time": time.time()}
    if cap == "catalog": report["price_cents_sum"] = sum(r["price_cents"] for r in left["products"])
    if cap == "inventory": report["available_sum"] = sum(r["available"] for r in left["stock"]);report[
        "reserved_sum"] = sum(r["quantity"] for r in left["reservations"] if r["state"] == "RESERVED")
    pathlib.Path("artifacts").mkdir(exist_ok=True)
    pathlib.Path("artifacts/reconciliation-" + cap + ".json").write_text(json.dumps(report, indent=2))
    if not report["equal"]: raise RuntimeError("parity mismatch; leave fenced and investigate")
    print(json.dumps(report));
    return report


def sync(cap):
    if state("legacy", cap)["state"] != "LEGACY": raise RuntimeError("sync requires legacy authority")
    if state(cap, cap)["state"] != "FROZEN": raise RuntimeError("target must be fenced")
    call(cap, "/internal/admin/import/" + cap, snapshot("legacy", cap))


def cutover(cap):
    started = time.monotonic()
    if cap == "inventory": transition("legacy", "checkout", "FROZEN")
    # Crash recovery: never automatically reopen source after target activation.
    if state(cap, cap)["state"] == "MODERN":
        transition("legacy", cap, "MODERN");
        return
    transition("legacy", cap, "FROZEN")
    call(cap, "/internal/admin/import/" + cap, snapshot("legacy", cap))
    reconcile("legacy", cap, cap)
    transition(cap, cap, "MODERN")
    transition("legacy", cap, "MODERN")
    pathlib.Path("artifacts").mkdir(exist_ok=True)
    pathlib.Path("artifacts/cutover-" + cap + ".json").write_text(
        json.dumps({"elapsed_seconds": time.monotonic() - started, "capability": cap}))


def rollback(cap):
    started = time.monotonic()
    if cap == "inventory": raise RuntimeError(
        "inventory rollback after saga start requires draining workflows and reservation reconciliation; intentionally blocked")
    # Suppress modern reads while taking a complete reverse snapshot.
    call("legacy", "/internal/admin/routing", {"mode": "LEGACY", "percent": 0})
    transition("legacy", cap, "FROZEN")
    transition(cap, cap, "FROZEN")
    call("legacy", "/internal/admin/import/" + cap, snapshot(cap, cap))
    reconcile(cap, "legacy", cap)
    transition("legacy", cap, "LEGACY")
    pathlib.Path("artifacts/rollback-" + cap + ".json").write_text(
        json.dumps({"elapsed_seconds": time.monotonic() - started, "capability": cap}))


def checkout():
    for cap in ("catalog", "inventory"):
        if state(cap, cap)["state"] != "MODERN": raise RuntimeError("extract catalog/inventory first")
    transition("legacy", "checkout", "MODERN")


if __name__ == "__main__":
    p = argparse.ArgumentParser();
    p.add_argument("action", choices=["sync", "cutover", "rollback", "reconcile", "checkout", "route"]);
    p.add_argument("capability", nargs="?", default="catalog");
    p.add_argument("--mode", choices=["LEGACY", "SHADOW", "CANARY", "MODERN"], default="LEGACY");
    p.add_argument("--percent", type=int, default=0);
    a = p.parse_args()
    if a.action == "route":
        call("legacy", "/internal/admin/routing", {"mode": a.mode, "percent": a.percent})
    elif a.action == "checkout":
        checkout()
    elif a.action == "reconcile":
        reconcile("legacy", a.capability, a.capability)
    else:
        globals()[a.action](a.capability)
