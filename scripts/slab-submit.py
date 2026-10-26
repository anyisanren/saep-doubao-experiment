#!/usr/bin/env python3
"""One guarded dynamic submission through SLAB-DYN's visible Main tools."""
import importlib.util
import json
from pathlib import Path
import subprocess
import shlex
import time

root = Path(__file__).resolve().parents[1]
def module(name, filename):
    spec = importlib.util.spec_from_file_location(name, root / "scripts" / filename)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value
u = module("tools_ui", "slab-tools-ui.py")
l = module("ledger", "slab-ledger.py")
d = u.d

def stage(folder, item, policy, version):
    raw = json.dumps(policy, ensure_ascii=True, separators=(",", ":")) if isinstance(policy, dict) else policy
    (folder / f"{item}-policy.txt").write_text(raw)
    # Existing App clipboard preparation entry; no policy API is called here.
    d.adb("shell", shlex.join(["am","start","-W","-n","com.saep.lab.dyn/com.saep.lab.MainActivity",
                              "-f","0x04000000","--es","operator_prompt",raw]))
    d.tree()
    u.fill("完整动态策略", raw, "down", paste=True)
    u.fill("动态版本号", str(version))
    time.sleep(0.6)
    node = u.locate("提交策略")
    d.capture(folder, f"{item}-prepared.png")
    rows = json.loads(subprocess.check_output([str(root / ".tooling/slab-ocr"), str(folder / f"{item}-prepared.png")]))
    visible = [r for r in rows if r["text"] == "提交策略"]
    assert len(visible) == 1, "Submit is not visually visible; no API call"
    a,b,c,e = u.bounds(node)
    assert a <= visible[0]["x"] <= c and b <= visible[0]["y"] <= e
    return node

def submit(folder, item, policy, version, summary):
    marker = folder / f"{item}-attempt.json"
    assert not marker.exists(), "Submission already attempted"
    node = stage(folder, item, policy, version)
    marker.write_text(json.dumps({"at": d.at(), "version": version}, indent=2))
    d.tap(node)
    state = d.tree()
    message = next(n.get("text") for n in state.iter("node")
                   if n.get("resource-id") == "android:id/message")
    result = json.loads(message)
    assert result["version"] == version
    (folder / f"{item}-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2))
    d.capture(folder, f"{item}-result.png")
    d.tap(d.match(state, "关闭"))
    time.sleep(0.3)
    entry = l.record(folder.name, item, result, summary)
    print(json.dumps({"item": item, "version": version, "accepted": entry["accepted"],
        **{k:v for k,v in result.items() if k not in ("policy", "immediate_read")}}, ensure_ascii=False), flush=True)
    return result
