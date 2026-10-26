#!/usr/bin/env python3
"""Persist logcat and resume from the last seen device timestamp after transport EOF."""
import signal
import subprocess
import sys
import time
import re

command=sys.argv[1:]
last="1"
child=None
stop=False
def terminate(signum, frame):
    global stop
    stop=True
    if child and child.poll() is None:
        child.terminate()
signal.signal(signal.SIGTERM,terminate)
while not stop:
    child=subprocess.Popen(command+["-T",last],stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    for raw in child.stdout:
        sys.stdout.buffer.write(raw)
        sys.stdout.buffer.flush()
        match=re.match(rb"\s*(\d+\.\d+)\s",raw)
        if match:
            last=match.group(1).decode()
    child.wait()
    if not stop:
        print(f"# SLAB logcat EOF; resuming from {last}",flush=True)
        time.sleep(.2)
