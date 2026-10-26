#!/usr/bin/env python3
"""Collect one task whose ended state was already visually confirmed."""
import argparse
import contextlib
import importlib.util
import json
import os
from pathlib import Path
import signal
import time

root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("evidence", root / "scripts/slab-evidence.py")
e = importlib.util.module_from_spec(spec)
spec.loader.exec_module(e)
p = argparse.ArgumentParser()
p.add_argument("--round", required=True)
p.add_argument("--variant", required=True)
args = p.parse_args()
folder = root / "runs" / args.round
pkg = "com.saep.lab." + args.variant
meta = json.loads((folder / "round.json").read_text())
if meta.get("status") != "task_dispatched":
    raise RuntimeError("Expected a dispatched, uncollected task")
e.end(folder)
with (folder / "response-summary.json").open("w") as out:
    with contextlib.redirect_stdout(out):
        e.response(folder)
e.export_request(folder, pkg)
# Allow the one requested export's UI and file write to finish; never re-query.
time.sleep(1)
with (folder / "pull-summary.json").open("w") as out:
    with contextlib.redirect_stdout(out):
        e.export_pull(folder, pkg, "D")
e.analyze(folder, pkg, "D")
pid = int((folder / "logcat-pid.txt").read_text())
try:
    os.kill(pid, signal.SIGTERM)
    e.d.event(folder, "logcat_stopped", pid=pid)
except ProcessLookupError:
    e.d.event(folder, "logcat_already_exited", pid=pid)
for row in json.loads((folder / f"task-audit_{pkg}.json").read_text()):
    if row.get("agent_decision"):
        decision = json.loads(row["agent_decision"])
        decision.pop("policy", None)
        print(json.dumps(decision, ensure_ascii=False))
