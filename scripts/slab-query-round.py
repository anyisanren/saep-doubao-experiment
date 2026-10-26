#!/usr/bin/env python3
"""R4-1's three specified UI queries, each exactly once."""
import importlib.util
import json
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("device", root / "scripts/slab-device.py")
d = importlib.util.module_from_spec(spec)
spec.loader.exec_module(d)
folder = root / "runs/R4-1"
assert not (folder / "queries-started.json").exists(), "Do not repeat queries"

def field(state, hint):
    return next(n for n in state.iter("node") if n.get("content-desc") == hint
                and n.get("class") == "android.widget.EditText")

def fill(hint, value):
    d.tap(field(d.tree(), hint))
    d.adb("shell", "input", "keycombination", "113", "29")
    d.adb("shell", "input", "text", str(value))
    d.adb("shell", "input", "keyevent", "KEYCODE_BACK")
    assert field(d.tree(), hint).get("text") == str(value)

now = int(d.adb("shell", "date", "+%s%3N"))
base = json.loads((root / "runs/R2-15/export_com.saep.lab.base.json").read_text())
meta = json.loads((root / "runs/R2-15/round.json").read_text())
windows = [
    {"name": "future", "start": now + 3600000, "end": now + 7200000},
    {"name": "inverted", "start": now, "end": now - 3600000},
    {"name": "normal_R2-15", "start": meta["preparation"]["wall_time_ms"],
     "end": base["wall_time_ms"]},
]
(folder / "queries-started.json").write_text(json.dumps(
    {"at": d.at(), "reference_device_ms": now, "windows": windows}, indent=2))
for i, window in enumerate(windows):
    fill("查询开始时间（Unix 毫秒）", window["start"])
    fill("查询结束时间（Unix 毫秒）", window["end"])
    d.capture(folder, f"rR4-1_d{i*2+1}.png")
    d.event(folder, "audit_query_tap", **window)
    d.tap(d.match(d.tree(), "查询审计日志"))
    state = d.tree()
    assert any(n.get("text") == "查询结果" for n in state.iter("node"))
    (folder / f"query-{window['name']}-ui.xml").write_bytes(ET.tostring(state, encoding="utf-8"))
    d.capture(folder, f"rR4-1_d{i*2+2}.png")
    paths = d.adb("shell", "ls", "-1", "/sdcard/Download/SLAB/R4-1_query_*.json").decode().splitlines()
    assert len(paths) == i + 1, paths
    raw = d.adb("exec-out", "cat", sorted(paths)[-1])
    result = json.loads(raw)
    assert result["start_time"] == window["start"] and result["end_time"] == window["end"]
    (folder / f"query-{window['name']}.json").write_bytes(raw)
    print(json.dumps({k: v for k, v in result.items() if k != "records"}, ensure_ascii=False), flush=True)
    d.tap(d.match(state, "关闭"))
