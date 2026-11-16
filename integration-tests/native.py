#!/usr/bin/env python3
"""Host JVM catalog-only test topology; PostgreSQL/Kafka remain in Compose."""
import json
import os
import pathlib
import shutil
import signal
import subprocess
import sys
import time
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
APPS = [('legacy', 'legacy-monolith', 'legacymonolith', 28081),
        ('gateway', 'strangler-gateway', 'stranglergateway', 28082),
        ('catalog', 'services/catalog-service', 'catalogservice', 28083)]
FILE = ROOT / 'artifacts/native-pids.json'


def stop():
    if not FILE.exists(): return
    rows = json.loads(FILE.read_text())
    for row in rows:
        try:
            args = subprocess.check_output(['ps', '-p', str(row['pid']), '-o', 'args='], text=True)
            if row['jar'] in args: os.kill(row['pid'], signal.SIGTERM)
        except (ProcessLookupError, subprocess.CalledProcessError):
            pass
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        alive = []
        for row in rows:
            try:
                args = subprocess.check_output(['ps', '-p', str(row['pid']), '-o', 'args='], text=True,
                                               stderr=subprocess.DEVNULL)
                if row['jar'] in args: alive.append(row['pid'])
            except subprocess.CalledProcessError:
                pass
        if not alive: break
        time.sleep(.25)
    else:
        raise RuntimeError('Host JVM shutdown incomplete; inspect native logs')
    FILE.unlink()


if sys.argv[1] == 'stop': stop();sys.exit(0)
stop();
rows = []
for name, mod, db, port in APPS:
    env = os.environ.copy();
    env.update(PORT=str(port), SERVER_ADDRESS='127.0.0.1', DB_URL='jdbc:postgresql://localhost:55439/' + db,
               DB_USER=db + '_app', DB_PASSWORD='local-only', SPRING_FLYWAY_USER=db + '_migrator',
               SPRING_FLYWAY_PASSWORD='local-only', KAFKA_SERVERS='localhost:' + os.getenv('KAFKA_HOST_PORT', '59092'),
               OUTBOX_ENABLED='true' if name == 'catalog' else 'false', MANAGEMENT_TRACING_ENABLED='false')
    for n, _, _, rp in APPS: env[n.upper() + '_URL'] = 'http://localhost:' + str(rp)
    jar = str(ROOT / mod / 'target' / ((mod.split('/')[-1]) + '-1.0.0.jar'))
    java = str(pathlib.Path(os.environ['JAVA_HOME']) / 'bin/java') if 'JAVA_HOME' in os.environ else shutil.which(
        'java')
    with open(ROOT / 'artifacts' / ('native-' + name + '.log'), 'w') as out:
        process = subprocess.Popen([java, '-Xmx256m', '-Xss256k', '-XX:ActiveProcessorCount=2', '-jar', jar], env=env,
                                   stdin=subprocess.DEVNULL, stdout=out, stderr=subprocess.STDOUT,
                                   start_new_session=True)
    rows.append({'pid': process.pid, 'jar': jar})
FILE.write_text(json.dumps(rows))
for _, _, _, port in APPS:
    started = time.monotonic()
    while time.monotonic() - started < 180:
        try:
            with urllib.request.urlopen('http://localhost:' + str(port) + '/actuator/health', timeout=2) as r:
                if r.status == 200: break
        except Exception:
            time.sleep(1)
    else:
        raise RuntimeError('native port not healthy: ' + str(port))
print('Host Java catalog topology ready', flush=True)
