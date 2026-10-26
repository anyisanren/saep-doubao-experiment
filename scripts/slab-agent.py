#!/usr/bin/env python3
"""Authorized task composition/dispatch; no app business actions or task retries."""
from pathlib import Path
import argparse
import importlib.util
import json
import shlex
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("slab_device", ROOT / "scripts/slab-device.py")
d = importlib.util.module_from_spec(spec)
spec.loader.exec_module(d)


def by_id(tree, suffix):
    nodes = [n for n in tree.iter("node") if n.get("resource-id") == "com.larus.nova:id/" + suffix]
    if len(nodes) != 1:
        raise RuntimeError(f"Expected one Doubao {suffix}; found {len(nodes)}")
    return nodes[0]


def compose(folder, package):
    prompt = (folder / "prompt.txt").read_text().strip()
    d.adb("shell", shlex.join(["am", "start", "-W", "-n", f"{package}/com.saep.lab.MainActivity",
                               "-f", "0x04000000", "--es", "operator_prompt", prompt]))
    # Wait for the UI to be idle before leaving; this is preparation, before the task.
    d.tree()
    d.event(folder, "operator_prompt_copied", package=package, prompt=prompt)
    d.adb("shell", "input", "keyevent", "KEYCODE_HOME")
    d.adb("shell", "input", "keyevent", "KEYCODE_ASSIST")
    d.event(folder, "doubao_invoked_from_desktop")
    state = d.tree()
    switch = [n for n in state.iter("node") if n.get("resource-id") == "com.larus.nova:id/action_input"]
    if switch:
        d.tap(switch[0])
        state = d.tree()
    edit = by_id(state, "input_text")
    d.tap(edit)
    d.adb("shell", "input", "keyevent", "KEYCODE_PASTE")
    d.adb("shell", "input", "keyevent", "KEYCODE_BACK")
    state = d.tree()
    actual = by_id(state, "input_text").get("text")
    if actual != prompt:
        d.capture(folder, "composer-mismatch.png")
        raise RuntimeError("Composer differs from the exact task; not sent")
    d.capture(folder, "composer.png")
    (folder / "composer.json").write_text(json.dumps({"at": d.at(), "prompt": actual,
        "send_bounds": by_id(state, "action_send").get("bounds"), "verified": True}, ensure_ascii=False, indent=2) + "\n")
    d.event(folder, "prompt_composed_not_sent", prompt=actual)
    print("Exact prompt composed; not sent.")


def send(folder, state=None):
    meta = json.loads((folder / "round.json").read_text())
    if meta.get("doubao_task_sent"):
        raise RuntimeError("Task has already been dispatched or dispatch was attempted")
    prompt = (folder / "prompt.txt").read_text().strip()
    if state is None:
        state = d.tree()
    if by_id(state, "input_text").get("text") != prompt:
        raise RuntimeError("Composer changed; not sent")
    send_button = by_id(state, "action_send")
    meta.update({"doubao_task_sent": True, "dispatch_started_at": d.at(), "status": "dispatch_attempted",
                 "dispatch_device_ms": int(d.adb("shell", "date", "+%s%3N").decode().strip())})
    (folder / "round.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n")
    with (folder / "scope-guard.log").open("a") as log:
        guard = subprocess.Popen([sys.executable, str(ROOT / "scripts/slab-scope-guard.py"),
                                  "--round", folder.name], stdout=log, stderr=log, start_new_session=True)
    (folder / "scope-guard-pid.txt").write_text(str(guard.pid))
    d.tap(send_button)
    meta["dispatch_completed_device_ms"] = int(d.adb("shell", "date", "+%s%3N").decode().strip())
    d.event(folder, "task_dispatched", prompt=prompt,
            device_time=d.adb("shell", "date", "+%Y-%m-%dT%H:%M:%S%z").decode().strip(),
            uptime=d.adb("shell", "cat", "/proc/uptime").decode().strip())
    d.capture(folder, f"r{folder.name}_d1.png")
    meta["status"] = "task_dispatched"
    (folder / "round.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n")
    print("Task dispatched once; screenshot captured.")


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("action", choices=["compose", "send"])
    p.add_argument("--round", required=True)
    p.add_argument("--variant", default="base")
    args = p.parse_args()
    folder = ROOT / "runs" / args.round
    allowed = {x["variant"] for x in json.loads((ROOT / "runs/build/policies.json").read_text())}
    if args.variant not in allowed:
        raise ValueError("Not a v2 test package")
    if args.action == "compose":
        compose(folder, f"com.saep.lab.{args.variant}")
    else:
        send(folder)
