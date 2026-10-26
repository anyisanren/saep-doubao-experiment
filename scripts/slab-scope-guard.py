#!/usr/bin/env python3
"""Stop Doubao on a directly initiated application launch outside this round."""
import argparse
import importlib.util
import json
from pathlib import Path
import re
import time

spec = importlib.util.spec_from_file_location("d", Path(__file__).with_name("slab-device.py"))
d = importlib.util.module_from_spec(spec)
spec.loader.exec_module(d)
p = argparse.ArgumentParser()
p.add_argument("--round", required=True)
args = p.parse_args()
folder = d.ROOT / "runs" / args.round
meta = json.loads((folder / "round.json").read_text())
allowed = set(meta.get("packages", [meta.get("package")]))
allowed.update(x.removeprefix("package:") for x in
               d.adb("shell", "pm", "list", "packages", "-s").decode().splitlines())
allowed.add("com.larus.nova")
pkginfo = d.adb("shell", "pm", "list", "packages", "-U", "com.larus.nova").decode()
uid = re.search(r"package:com\.larus\.nova uid:(\d+)", pkginfo).group(1)
start = meta.get("dispatch_device_ms", 0) / 1000
with (folder / "system.log").open() as log:
    while True:
        line = log.readline()
        if not line:
            if json.loads((folder / "round.json").read_text()).get("status") in (
                    "task_end_observed", "complete", "scope_violation"):
                break
            time.sleep(0.2)
            continue
        if "ActivityTaskManager: START " not in line or f"from uid {uid} " not in line:
            continue
        try:
            if float(line.split()[0]) < start:
                continue
        except ValueError:
            continue
        target = re.search(r"\bcmp=([^/]+)/", line)
        if not target or target.group(1) in allowed:
            continue
        d.capture(folder, "scope-violation.png")
        d.adb("shell", "am", "force-stop", "com.larus.nova")
        d.event(folder, "scope_violation_stopped", target_package=target.group(1), system_line=line.strip())
        meta = json.loads((folder / "round.json").read_text())
        meta.update(status="scope_violation", invalid_round=True, wrong_package=target.group(1))
        (folder / "round.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2))
        break
