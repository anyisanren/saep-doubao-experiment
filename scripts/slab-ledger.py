#!/usr/bin/env python3
"""Persist raw submission results and the accepted-version ledger."""
import datetime as dt
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
zone = dt.timezone(dt.timedelta(hours=8))

def wall(ms):
    return dt.datetime.fromtimestamp(ms / 1000, zone).isoformat()

def record(round_id, item, result, summary):
    path = root / "runs/dynamic-ledger.json"
    ledger = json.loads(path.read_text())
    key = f"{round_id}/{item}"
    assert not any(e["key"] == key for e in ledger["entries"]), key
    accepted = result.get("return_value") == 1 and not result.get("exception_type")
    raw = result["policy"]
    entry = {"key": key, "round": round_id, "item": item,
             "at": wall(result["at_ms"]), "version": result["version"],
             "policy_sha256": hashlib.sha256(raw.encode()).hexdigest(),
             "summary": summary, "accepted": accepted, "result": result,
             "first_effect": None}
    ledger["entries"].append(entry)
    if accepted:
        ledger["N"] = result["version"]
        try:
            ledger["current_policy"] = json.loads(raw)
        except json.JSONDecodeError as exc:
            entry["invalid_json_accepted_return"] = str(exc)
            ledger["unverified_invalid_policy"] = raw
    path.write_text(json.dumps(ledger, ensure_ascii=False, indent=2))
    result_text = ("return_value=" + str(result["return_value"]) if "return_value" in result else
                   result.get("exception_type", "") + ": " + result.get("exception_message", ""))
    read = result["immediate_read"]
    read_text = read.get("exception_message", json.dumps(read, ensure_ascii=False))
    with (root / "runs/VERSIONS.md").open("a") as f:
        f.write(f"| {entry['at']} | {key} | {entry['version']} | {summary}；SHA-256 {entry['policy_sha256']} | "
                f"{result_text}；紧接读取：{read_text} | {'是' if accepted else '否'} | 待任务双证据核验 |\n")
    with (root / "runs/TIMELINE.md").open("a") as f:
        f.write(f"| {entry['at']} | {key} 提交 | {entry['version']} | {result_text}；返回对话框/原始导出；紧接读取：{read_text} | 待核验 | 待任务行为与 policy | 待核验 |\n")
    return entry
