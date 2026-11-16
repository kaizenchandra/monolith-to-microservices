#!/usr/bin/env python3
"""Small reproducible catalog comparison. Run after demo.py; no capacity claim."""
import json
import pathlib
import subprocess
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / "migration-tools"))
from migrate import *

rollback("catalog")
rows = []
try:
    for mode, percent in [("LEGACY", 0), ("SHADOW", 0), ("CANARY", 1), ("CANARY", 10), ("CANARY", 50), ("MODERN", 100)]:
        label = mode.lower() + str(percent)
        call("legacy", "/internal/admin/routing", {"mode": mode, "percent": percent})
        file = "artifacts/k6-" + label + ".json"
        with open("artifacts/k6-" + label + ".log", "w") as out:
            subprocess.run(
                ["k6", "run", "--vus", "4", "--duration", "10s", "--summary-export", file, "benchmarks/routing.js"],
                stdout=out, stderr=subprocess.STDOUT, check=True)
        m = json.loads(pathlib.Path(file).read_text())["metrics"]
        rows.append({"mode": label, "rps": m["http_reqs"]["rate"], "requests": m["http_reqs"]["count"],
                     "p50_ms": m["http_req_duration"]["med"], "p95_ms": m["http_req_duration"]["p(95)"],
                     "p99_ms": m["http_req_duration"]["p(99)"], "errors": m["http_req_failed"]["value"]})
        pathlib.Path("artifacts/benchmark-results.json").write_text(json.dumps(rows, indent=2))
finally:
    cutover("catalog");
    call("legacy", "/internal/admin/routing", {"mode": "MODERN", "percent": 100})
