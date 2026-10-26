#!/usr/bin/env python3
"""Passive screenshot/OCR watcher; may approve only an exact app-operation permission."""
import argparse
import importlib.util
import json
from pathlib import Path
import subprocess
import time

root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("device", root / "scripts/slab-device.py")
d = importlib.util.module_from_spec(spec)
spec.loader.exec_module(d)
p = argparse.ArgumentParser()
p.add_argument("--round", required=True)
p.add_argument("--label", default="SLAB-DYN")
p.add_argument("--seconds", type=int, default=40)
args = p.parse_args()
folder = root / "runs" / args.round
approved = (folder / "permission-approved.json").exists()
start = time.monotonic()
previous = None
index = 0
while time.monotonic() - start < args.seconds:
    index += 1
    path = folder / "watch-latest.png"
    path.write_bytes(d.adb("exec-out", "screencap", "-p"))
    rows = json.loads(subprocess.check_output([str(root / ".tooling/slab-ocr"), str(path)]))
    (folder / "watch-latest.json").write_text(json.dumps(rows, ensure_ascii=False, indent=2))
    full = "".join(x["text"] for x in rows).replace(" ", "").upper()
    permission = "没有授予豆包操作" + args.label.upper()
    if not approved and permission in full:
        allow = [x for x in rows if x["text"].strip() == "允许" and x["x"] > 630]
        deny = [x for x in rows if x["text"].strip() == "拒绝" and x["x"] < 630]
        if len(allow) == len(deny) == 1 and abs(allow[0]["y"] - deny[0]["y"]) < 80:
            # A fresh screenshot supplies the exact buttons; only app permission is authorized.
            (folder / "permission-before.png").write_bytes(path.read_bytes())
            d.adb("shell", "input", "tap", str(round(allow[0]["x"])), str(round(allow[0]["y"])))
            info = {"at": d.at(), "label": args.label, "allow": allow[0], "deny": deny[0]}
            (folder / "permission-approved.json").write_text(json.dumps(info, ensure_ascii=False, indent=2))
            d.event(folder, "round_app_permission_tapped", **info)
            approved = True
            print("Exact app-operation permission tapped once.", flush=True)
    relevant = [x["text"] for x in rows if any(s in x["text"] for s in
                ("已完成", "已失败", "任务已终止", "接管", "没有授予", "正在", "SLAB-DYN", "权限"))]
    if relevant != previous:
        print(json.dumps({"at": d.at(), "status_text": relevant}, ensure_ascii=False), flush=True)
        previous = relevant
    time.sleep(1)
print("Screenshot observation finished; inspect watch-latest.png before declaring task end.", flush=True)
