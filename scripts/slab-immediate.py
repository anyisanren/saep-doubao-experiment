#!/usr/bin/env python3
"""One R3-1 submission followed by the prepared R3-2 task, with device timing."""
import importlib.util
import json
from pathlib import Path
import time
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
def module(name, file):
    spec = importlib.util.spec_from_file_location(name, root / "scripts" / file)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value

a = module("agent", "slab-agent.py")
d = a.d
f1, f2 = root / "runs/R3-1", root / "runs/R3-2"
m1 = json.loads((f1 / "round.json").read_text())
assert not m1.get("submission_attempted"), "Never repeat the submission"
assert not json.loads((f2 / "round.json").read_text()).get("doubao_task_sent")
state = d.tree()
fields = {n.get("content-desc"): n.get("text") for n in state.iter("node")
          if n.get("class") == "android.widget.EditText"}
assert fields["完整动态策略"] == (f1 / "submitted-policy-ascii.txt").read_text()
assert fields["动态版本号"] == "2"
submit = d.match(state, "提交策略")
controls = json.loads((f1 / "main-controls-ocr.json").read_text())
main_xy = {row["text"]: (round(row["x"]), round(row["y"])) for row in controls
           if row["text"] in ("开始本轮", "导出")}
assert len(main_xy) == 2
dialog = json.loads((f1 / "round-dialog-controls.json").read_text())
round_field = ET.Element("node", next(row for row in dialog if row.get("hint") == "轮次号"))
begin_button = ET.Element("node", next(row for row in dialog if row.get("text") == "开始"))
d.stream(f2)
m1.update(submission_attempted=True, status="submission_attempted", at=d.at())
(f1 / "round.json").write_text(json.dumps(m1, ensure_ascii=False, indent=2))
d.event(f1, "submit_tap_once", version=2)
d.tap(submit)

# The App performs exactly one immediate read itself, before displaying this result.
state = d.tree()
message = next(n.get("text") for n in state.iter("node")
               if n.get("resource-id") == "android:id/message")
result = json.loads(message)
assert result["version"] == 2
return_bound = result["immediate_read"]["at_ms"]
(f1 / "submission-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2))
d.capture(f1, "rR3-1_d2.png")
d.tap(d.match(state, "关闭"))

# Preserve the completed R3-1 process data before R3-2's required force-stop.
d.adb("shell", "am", "start", "-W", "-n", "com.saep.lab.dyn/com.saep.lab.MainActivity",
      "-f", "0x04000000")
time.sleep(0.25)
d.adb("shell", "input", "tap", *map(str, main_xy["导出"]))
time.sleep(0.2)
paths = d.adb("shell", "ls", "-1", "/sdcard/Download/SLAB/R3-1_S_com.saep.lab.dyn_*.json").decode().splitlines()
assert len(paths) == 1, "Export unavailable; stop rather than lose submission data"
d.adb("shell", "am", "force-stop", "com.saep.lab.dyn")
d.adb("shell", "am", "start", "-W", "-n", "com.saep.lab.dyn/com.saep.lab.MainActivity")
time.sleep(0.25)
d.adb("shell", "input", "tap", *map(str, main_xy["开始本轮"]))
time.sleep(0.15)
d.tap(round_field)
d.adb("shell", "input", "keycombination", "113", "29")
d.adb("shell", "input", "text", "R3-2")
d.adb("shell", "input", "keyevent", "KEYCODE_BACK")
time.sleep(0.1)
d.tap(begin_button)  # The phase field defaults to D.
time.sleep(0.1)
d.event(f2, "round_initialized_after_submission", phase="D")
d.adb("shell", "input", "keyevent", "KEYCODE_HOME")
d.adb("shell", "input", "keyevent", "KEYCODE_ASSIST")
state = d.tree()
a.send(f2, state=state)  # Verifies the exact retained draft before the single send.
m2 = json.loads((f2 / "round.json").read_text())
timing = {"submission_return_upper_bound_ms": return_bound,
          "dispatch_completed_device_ms": m2["dispatch_completed_device_ms"],
          "elapsed_upper_bound_ms": m2["dispatch_completed_device_ms"] - return_bound}
timing["within_10_seconds"] = timing["elapsed_upper_bound_ms"] <= 10000
(f2 / "immediate-timing.json").write_text(json.dumps(timing, indent=2))
(f1 / "export_S_com.saep.lab.dyn.json").write_bytes(d.adb("exec-out", "cat", paths[0]))
m1.update(status="submission_complete", immediate_task_timing=timing)
(f1 / "round.json").write_text(json.dumps(m1, ensure_ascii=False, indent=2))
print(json.dumps({"result": result, "timing": timing}, ensure_ascii=False), flush=True)
d.adb("shell", "am", "start", "-W", "-n", "com.larus.nova/com.larus.home.impl.alias.AliasActivity1")
