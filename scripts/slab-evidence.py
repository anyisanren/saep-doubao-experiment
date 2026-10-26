#!/usr/bin/env python3
"""Collect an already completed round, or inspect passive observations.

No task dispatch, policy submission, or retry is performed here.
"""
import argparse
import collections
import importlib.util
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("slab_device", ROOT / "scripts/slab-device.py")
d = importlib.util.module_from_spec(spec)
spec.loader.exec_module(d)


def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n")


def passive(folder):
    rows = []
    for line in (folder / "system.log").read_text(errors="replace").splitlines():
        if " I SLAB " not in line or '{"round"' not in line:
            continue
        try:
            row = json.loads(line[line.index('{"round"'):])
        except ValueError:
            continue
        if row.get("round") == folder.name and row["event"] in ("foreground", "business_success"):
            rows.append(row)
    print(json.dumps(rows[-12:], ensure_ascii=False, indent=2))
    current = d.adb("shell", "dumpsys", "activity", "activities").decode(errors="replace")
    print("\n".join(x.strip() for x in current.splitlines()
                    if "ResumedActivity" in x or "mResumedActivity" in x))


def end(folder):
    stamp = d.adb("shell", "date", "+%s%3N").decode().strip()
    d.event(folder, "task_end_observed", device_epoch_ms=stamp)
    meta = json.loads((folder / "round.json").read_text())
    meta.update(status="task_end_observed", task_end_device_ms=int(stamp),
                task_end_observed_at=d.at())
    write(folder / "round.json", meta)
    d.capture(folder, f"r{folder.name}_d{len(list(folder.glob('r*_d*.png'))) + 1}.png")


def response(folder):
    # Only after task end: uiautomator can affect accessibility counters.
    state = d.tree()
    (folder / "response-ui.xml").write_bytes(ET.tostring(state, encoding="utf-8"))
    rows = [n.attrib for n in state.iter("node") if n.get("text") or n.get("content-desc")]
    write(folder / "response-ui-text.json", rows)
    print(json.dumps([{"text": x["text"], "description": x.get("content-desc"),
                       "id": x.get("resource-id")} for x in rows], ensure_ascii=False, indent=2))


def export_request(folder, package):
    d.adb("shell", "am", "start", "-W", "-n", f"{package}/com.saep.lab.MainActivity",
          "-f", "0x04000000")
    d.tap(d.match(d.tree(), "导出"))
    d.event(folder, "export_requested", package=package)


def export_pull(folder, package, phase):
    files = d.adb("shell", "ls", "-1",
                  f"/sdcard/Download/SLAB/{folder.name}_{phase}_{package}_*.json").decode().splitlines()
    if len(files) != 1:
        raise RuntimeError(f"Expected exactly one export, got {files}")
    raw = d.adb("exec-out", "cat", files[0])
    obj = json.loads(raw)
    assert obj["round"] == folder.name and obj["package"] == package and obj["phase"] == phase
    name = f"export_{phase}_{package}.json"
    (folder / name).write_bytes(raw)
    d.event(folder, "export_pulled", remote=files[0], local=name)
    print(json.dumps({"business": obj["business"], "counters": obj["counters"],
                      "audit_count": obj["audit"].get("count")}, ensure_ascii=False, indent=2))


def analyze(folder, package, phase):
    obj = json.loads((folder / f"export_{phase}_{package}.json").read_text())
    meta = json.loads((folder / "round.json").read_text())
    actions = [json.loads(x) for x in (folder / "host-actions.jsonl").read_text().splitlines()]
    dispatch = next(x for x in actions if x["event"] == "task_dispatched")
    # Device timestamp is to seconds in older dispatch records.
    import datetime
    start = int(datetime.datetime.strptime(dispatch["device_time"], "%Y-%m-%dT%H:%M:%S%z").timestamp() * 1000)
    end_ms = meta["task_end_device_ms"]
    obs = [x for x in obj["observations"] if x["phase"] == phase and start <= x["wall_time_ms"] <= end_ms]
    # Agent audit can be reported after the visible task ends. Preserve the
    # round's post-dispatch records through export; event statistics stay within
    # the measured task window. Earlier trials in reused packages are excluded.
    audit = [x for x in obj["audit"].get("records", [])
             if start <= int(x.get("timestamp", 0)) <= obj["wall_time_ms"]]
    try:
        expected = json.loads(obj["static_policy_raw"])
    except ValueError:
        expected = None
    policies = []
    for row in audit:
        decision = row.get("agent_decision")
        if isinstance(decision, str):
            try:
                decision = json.loads(decision)
            except ValueError:
                continue
        if isinstance(decision, dict) and "policy" in decision:
            raw = decision["policy"]
            try:
                policy = json.loads(raw) if isinstance(raw, str) else raw
            except ValueError:
                policy = None
            policies.append({"timestamp": row["timestamp"], "raw": raw, "parsed": policy,
                             "matches_static": policy == expected if expected is not None else None})
    counters = {}
    for row in obs:
        if row["event"] == "accessibility_counter":
            q = row["data"]
            key = f'{q["page"]}.{q["view"]}.{q["counter"]}'
            counters[key] = counters.get(key, 0) + 1
    summary = {
        "task_start_device_ms": start, "task_end_device_ms": end_ms,
        "audit_cutoff_device_ms": obj["wall_time_ms"],
        "audit_reported_after_task_end": sum(int(x.get("timestamp", 0)) > end_ms for x in audit),
        "business": obj["business"],
        "event_counts": dict(collections.Counter(x["event"] for x in obs)),
        "audit_sources": dict(collections.Counter(x.get("log_source") for x in audit)),
        "audit_action_types": dict(collections.Counter(x.get("action_type") for x in audit)),
        "policy_record_count": len(policies),
        "policy_versions": sorted({x["parsed"].get("policy_version") for x in policies if isinstance(x["parsed"], dict)}, key=str),
        "policy_all_match_static": all(x["matches_static"] for x in policies) if policies else None,
        "task_counter_events": counters,
        "foreground_packages": sorted({x["package"] for x in obs if x["event"] == "foreground"}),
        "reason_code_literal_occurrences": json.dumps(audit).count("reason_code"),
    }
    write(folder / f"analysis_{package}.json", summary)
    write(folder / f"policy-evidence_{package}.json", policies)
    write(folder / f"task-audit_{package}.json", audit)
    (folder / f"task-observations_{package}.jsonl").write_text(
        "".join(json.dumps(x, ensure_ascii=False) + "\n" for x in obs))
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("action", choices=["status", "end", "response", "export", "pull", "analyze"])
    p.add_argument("--round", required=True)
    p.add_argument("--variant", default="base")
    p.add_argument("--phase", default="D")
    args = p.parse_args()
    folder = ROOT / "runs" / args.round
    pkg = f"com.saep.lab.{args.variant}"
    if args.action == "status":
        passive(folder)
    elif args.action == "end":
        end(folder)
    elif args.action == "response":
        response(folder)
    elif args.action == "export":
        export_request(folder, pkg)
    elif args.action == "pull":
        export_pull(folder, pkg, args.phase)
    elif args.action == "analyze":
        analyze(folder, pkg, args.phase)
