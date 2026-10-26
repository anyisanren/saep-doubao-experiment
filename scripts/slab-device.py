#!/usr/bin/env python3
"""Evidence and visible UI preparation only. Never sends a task to Doubao."""
from pathlib import Path
import argparse
import datetime as dt
import json
import os
import re
import signal
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SDK = Path(os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME") or ROOT / ".tooling/android-sdk")
ADB = Path(os.environ.get("SAEP_ADB") or SDK / "platform-tools/adb")
SERIAL = os.environ.get("SAEP_SERIAL", "")
ZONE = dt.timezone(dt.timedelta(hours=8))


def adb(*args):
    if not SERIAL:
        raise RuntimeError("Set SAEP_SERIAL to the test device serial before using device scripts")
    return subprocess.check_output([str(ADB), "-s", SERIAL, *args], timeout=30)


def at():
    return dt.datetime.now(ZONE).isoformat()


def event(folder, kind, **values):
    folder.mkdir(parents=True, exist_ok=True)
    with (folder / "host-actions.jsonl").open("a") as f:
        f.write(json.dumps({"at": at(), "event": kind, **values}, ensure_ascii=False) + "\n")


def tree():
    # For preparation only. Do not use while measuring Agent accessibility reads.
    adb("shell", "uiautomator", "dump", "/sdcard/slab-prepare.xml")
    raw = adb("exec-out", "cat", "/sdcard/slab-prepare.xml")
    adb("shell", "rm", "/sdcard/slab-prepare.xml")
    return ET.fromstring(raw)


def tap(node):
    a, b, c, d = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((a + c) // 2), str((b + d) // 2))


def match(root, text):
    nodes = [n for n in root.iter("node") if n.get("text") == text]
    if len(nodes) != 1:
        raise RuntimeError(f"Expected one visible control {text!r}; found {len(nodes)}")
    return nodes[0]


def capture(folder, name):
    (folder / name).write_bytes(adb("exec-out", "screencap", "-p"))
    event(folder, "screenshot", file=name,
          device_time=adb("shell", "date", "+%Y-%m-%dT%H:%M:%S%z").decode().strip(),
          uptime=adb("shell", "cat", "/proc/uptime").decode().strip())


def stream(folder):
    if not SERIAL:
        raise RuntimeError("Set SAEP_SERIAL to the test device serial before using device scripts")
    folder.mkdir(parents=True, exist_ok=True)
    pidfile = folder / "logcat-pid.txt"
    if pidfile.exists():
        os.kill(int(pidfile.read_text()), 0)
        return
    with (folder / "system.log").open("wb") as f:
        p = subprocess.Popen([sys.executable, str(ROOT / "scripts/slab-log-stream.py"),
                              str(ADB), "-s", SERIAL, "logcat", "-v", "epoch"],
                             stdout=f, stderr=subprocess.STDOUT, start_new_session=True)
    pidfile.write_text(str(p.pid) + "\n")
    event(folder, "logcat_started", pid=p.pid)


def prepare(folder, package, round_id, phase):
    stream(folder)
    event(folder, "preparation_start", package=package, phase=phase,
          device_time=adb("shell", "date", "+%Y-%m-%dT%H:%M:%S%z").decode().strip(),
          uptime=adb("shell", "cat", "/proc/uptime").decode().strip())
    adb("shell", "am", "force-stop", package)
    adb("shell", "am", "start", "-W", "-n", f"{package}/com.saep.lab.MainActivity")
    tap(match(tree(), "开始本轮"))
    for index, value in enumerate([round_id, phase]):
        # The keyboard moves the dialog. Re-read coordinates before each field.
        edits = [n for n in tree().iter("node") if n.get("class") == "android.widget.EditText"]
        if len(edits) != 2:
            raise RuntimeError(f"Expected round and phase fields; got {len(edits)}")
        tap(edits[index])
        adb("shell", "input", "keyevent", "KEYCODE_MOVE_END")
        adb("shell", "input", "keycombination", "113", "29")  # Ctrl+A
        adb("shell", "input", "text", value)
    # Keyboard changes coordinates: read a fresh tree for the dialog's start button.
    tap(match(tree(), "开始"))
    capture(folder, "prepared.png")
    (folder / f"prepared-{phase}-{package}.png").write_bytes((folder / "prepared.png").read_bytes())
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    event(folder, "ready_on_desktop", package=package, phase=phase)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=["prepare", "capture", "inspect", "stop-log"])
    parser.add_argument("--round", required=True)
    parser.add_argument("--variant", default="base")
    parser.add_argument("--phase", default="D")
    parser.add_argument("--name", default="capture.png")
    args = parser.parse_args()
    folder = ROOT / "runs" / args.round
    if args.action == "prepare":
        allowed = {x["variant"] for x in json.loads((ROOT / "runs/build/policies.json").read_text())}
        if args.variant not in allowed:
            raise ValueError("Not a v2 test package")
        prepare(folder, f"com.saep.lab.{args.variant}", args.round, args.phase)
    elif args.action == "capture":
        capture(folder, args.name)
    elif args.action == "inspect":
        t = tree()
        for n in t.iter("node"):
            for key in ("text", "content-desc"):
                if n.get(key):
                    n.set(key, re.sub(r"([MIRPWT])-\d{4}", r"\1-****", n.get(key)))
        (folder / "preparation-ui.xml").write_bytes(ET.tostring(t, encoding="utf-8"))
        for n in t.iter("node"):
            if n.get("text") or n.get("content-desc"):
                print(n.attrib)
    elif args.action == "stop-log":
        pid = int((folder / "logcat-pid.txt").read_text())
        os.kill(pid, signal.SIGTERM)
        event(folder, "logcat_stopped", pid=pid)


if __name__ == "__main__":
    main()
