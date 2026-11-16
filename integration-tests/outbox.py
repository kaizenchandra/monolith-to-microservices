#!/usr/bin/env python3
"""Catalog broker-outage + real OrderConfirmed replay contract on host-JVM topology."""
import base64
import json
import pathlib
import subprocess
import sys
import time
import urllib.request

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / 'migration-tools'))
from migrate import call

checks = []


def dc(*args, input=None): return subprocess.check_output(['docker', 'compose', *args], input=input, text=True)


def sql(db, q): return dc('exec', '-T', 'postgres', 'psql', '-U', 'demo', '-d', db, '-Atc', q).strip()


def until(fn, seconds=120):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        try:
            if fn(): return
        except Exception:
            pass
        time.sleep(1)
    raise AssertionError('condition timed out')


def commit_count():
    req = urllib.request.Request('http://localhost:28081/actuator/prometheus',
                                 headers={'Authorization': 'Basic ' + base64.b64encode(b'service:local-only').decode()})
    with urllib.request.urlopen(req, timeout=10) as r: lines = r.read().decode().splitlines()
    values = [float(line.split()[-1]) for line in lines if line.startswith('kafka_consumer_coordinator_commit_total{')]
    if not values: raise AssertionError('consumer commit metric absent')
    return sum(values)


def check(name, value): assert value, name;checks.append(name);print('PASS', name, flush=True)


G = 'http://localhost:28082'
original = call(G, '/api/products/1')
try:
    dc('stop', 'kafka')
    updated = call(G, '/api/products', dict(original, name=original['name'] + ' [outbox probe]'))
    check('catalog commits state and durable event during broker outage',
          updated['version'] == original['version'] + 1 and int(
              sql('catalogservice', 'select count(*) from outbox where not published')) > 0)
finally:
    dc('start', 'kafka')
until(lambda: sql('catalogservice', 'select count(*) from outbox where not published') == '0')
check('outbox relay drains after broker restoration', True)
# Replay an actual producer envelope, with identical event ID and operation ID.
payload = sql('orderservice', "select payload from outbox where aggregate_id='saga-1' limit 1")
assert json.loads(payload)['type'] == 'OrderConfirmed'


def publish(): dc('exec', '-T', 'kafka', '/opt/kafka/bin/kafka-console-producer.sh', '--bootstrap-server',
                  'kafka:19092', '--topic', 'commerce.events.v1', input=payload + '\n')


publish()
until(lambda: sql('legacymonolith', "select count(*) from shipments where order_id='saga-1'") == '1')
until(lambda: commit_count() > 0)
before = commit_count();
publish();
until(lambda: commit_count() > before)
check('duplicate event consumed and offset committed', True)
check('duplicate OrderConfirmed produces one shipment',
      sql('legacymonolith', "select count(*) from shipments where order_id='saga-1'") == '1')
current = call(G, '/api/products/1');
call(G, '/api/products', dict(current, name=original['name']))
pathlib.Path('artifacts/outbox-results.json').write_text(
    json.dumps({'checks': checks, 'count': len(checks), 'topology': 'host JVMs + Compose PostgreSQL/Kafka'}, indent=2))
