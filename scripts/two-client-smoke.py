#!/usr/bin/env python3
"""Run two real Fabric clients against an already running, isolated localhost server.

First run ./gradlew writeClientTestLaunch. Requires Python 3, JDK 25, a display,
and a local test server on 127.0.0.1:25565 with matching Momentum/Fabric mods.
Never point this at a public server. It assumes default movement settings.
"""
import argparse
import json
import os
from pathlib import Path
import subprocess
import socket
import time

project = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--port', type=int, default=25565)
parser.add_argument('--second-display', help='Optional separate X display for MomentumB to isolate SDL focus')
options = parser.parse_args()
# Wait before starting either heavy graphical process, so a fast client cannot
# attempt its single connection while the dedicated JVM is still starting.
ready_deadline = time.monotonic() + 60
while True:
    try:
        with socket.create_connection(('127.0.0.1', options.port), timeout=1):
            break
    except OSError:
        if time.monotonic() > ready_deadline:
            raise TimeoutError('Local test server did not open its port within 60 seconds')
        time.sleep(0.5)
launch = json.loads((project / 'build/client-test-launch.json').read_text())
root = project / 'build/run/twoClients'
root.mkdir(parents=True, exist_ok=True)
barriers = root / ('barriers-' + str(time.time_ns()))
barriers.mkdir()
processes = []
try:
    for name in ('MomentumA', 'MomentumB'):
        directory = root / name
        directory.mkdir(exist_ok=True)
        log = (root / (name + '.log')).open('w')
        # Loom's @argument file is created only by its JavaExec action; use its
        # exported classpath directly when launching the two independent JVMs.
        jvm = [arg for arg in launch['jvmArgs'] if not arg.startswith('@')]
        command = [launch['java'], '-Xms128M', '-Xmx900M', *jvm,
                   '-Dmomentum.test.remote=127.0.0.1:' + str(options.port),
                   '-Dmomentum.test.barriers=' + str(barriers),
                   '-cp', launch['classpath'], launch['main'],
                   *launch['args'], '--username', name]
        environment = os.environ.copy()
        if name == 'MomentumB' and options.second_display:
            environment['DISPLAY'] = options.second_display
        process = subprocess.Popen(command, cwd=directory, env=environment, stdout=log, stderr=subprocess.STDOUT)
        processes.append((name, process, log))
        print('Started', name, 'PID', process.pid, flush=True)
    deadline = time.monotonic() + 240
    while any(process.poll() is None for _, process, _ in processes):
        if time.monotonic() > deadline:
            raise TimeoutError('Two-client test exceeded four minutes; inspect ' + str(root))
        if any(process.poll() not in (None, 0) for _, process, _ in processes):
            raise RuntimeError('A client failed; inspect ' + str(root))
        time.sleep(0.5)
    for name, process, _ in processes:
        if process.returncode != 0:
            raise RuntimeError(name + ' exited with ' + str(process.returncode))
        if not (barriers / (name + '.done')).exists():
            raise RuntimeError(name + ' did not complete its checks')
    print('PASS: both clients completed jump, dash, and independent sprint checks.', flush=True)
finally:
    for _, process, log in processes:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
        log.close()
