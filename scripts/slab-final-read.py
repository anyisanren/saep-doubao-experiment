#!/usr/bin/env python3
"""The single policy read after R4-8's task and export; no extra audit query."""
import importlib.util
import json
import os
from pathlib import Path
import signal
import time
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
s = importlib.util.spec_from_file_location("u", root / "scripts/slab-tools-ui.py")
u = importlib.util.module_from_spec(s)
s.loader.exec_module(u)
d = u.d
f = root / "runs/R4-8"
assert (f / "export_D_com.saep.lab.dyn.json").exists()
assert not (f / "policy-read-attempt.json").exists(), "Policy read already attempted"
tail = f / "policy-read-tail"
d.stream(tail)
# Close only the previous export-result dialog, if it is still present.
state = d.tree()
if any(n.get("text") in ("导出完成", "导出结果") for n in state.iter("node")):
    d.tap(d.match(state, "关闭"))
node = u.locate("策略读取", "down")
d.capture(f, "policy-read-before.png")
(f / "policy-read-attempt.json").write_text(json.dumps({"at": d.at(), "count": 1}, indent=2))
d.tap(node)
state = d.tree()
assert any(n.get("text") == "策略读取结果" for n in state.iter("node"))
(f / "policy-read-ui.xml").write_bytes(ET.tostring(state, encoding="utf-8"))
d.capture(f, "policy-read-result.png")
message = next(n.get("text") for n in state.iter("node") if n.get("resource-id") == "android:id/message")
(f / "policy-read-ui-text.txt").write_text(message)
time.sleep(.5)
rows = []
for line in (tail / "system.log").read_text(errors="replace").splitlines():
    if '"event":"policy_read"' not in line or '{"round"' not in line:
        continue
    try:
        row = json.loads(line[line.index('{"round"'):])
    except ValueError:
        continue
    if row.get("round") == "R4-8":
        rows.append(row)
unique = {row["wall_time_ms"]: row for row in rows}
assert len(unique) == 1, f"Expected one policy-read event, got {len(unique)}; do not repeat"
result = next(iter(unique.values()))
(f / "policy-read.json").write_text(json.dumps(result, ensure_ascii=False, indent=2))
d.tap(d.match(state, "关闭"))
pid = int((tail / "logcat-pid.txt").read_text())
os.kill(pid, signal.SIGTERM)
d.event(tail, "logcat_stopped", pid=pid)
print(json.dumps(result, ensure_ascii=False, indent=2))
