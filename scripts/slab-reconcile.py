#!/usr/bin/env python3
"""Reconcile the ledger with a collected task's latest policy; no device actions."""
import hashlib
import importlib.util
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location("c",root/"scripts/slab-dynamic-check.py")
c=importlib.util.module_from_spec(s);s.loader.exec_module(c)

def reconcile(folder):
    path=root/"runs/dynamic-ledger.json";ledger=json.loads(path.read_text())
    candidates=[]
    for entry in ledger["entries"]:
        if not entry.get("accepted"):continue
        try:
            policy=entry.get("policy") or json.loads(entry["result"]["policy"])
        except (KeyError,json.JSONDecodeError):continue
        candidates.append((entry["key"],policy))
    initial=json.loads((root/"runs/build/dyn-original-policy.json").read_text())
    candidates.append(("initial-static-v1",initial))
    result=c.check(folder,[p for k,p in candidates])
    for row in result["policies"]:
        row["matching_ledger_entries"]=[k for k,p in candidates if p==row["policy"]]
    if result["policies"]:
        latest=result["policies"][-1]
        previous=ledger["N"]
        ledger["N"]=latest["version"];ledger["current_policy"]=latest["policy"]
        ledger["latest_policy_evidence"]={"round":folder.name,"version":latest["version"],"timestamp":latest["timestamp"],"matches":latest["matching_ledger_entries"]}
        path.write_text(json.dumps(ledger,ensure_ascii=False,indent=2))
        result["ledger_N_before_reconciliation"]=previous
        result["ledger_N_after_reconciliation"]=latest["version"]
        message=f"最新 policy={latest['version']}，匹配 {latest['matching_ledger_entries']}；台账 {previous}→{latest['version']}"
        with (root/"runs/VERSIONS.md").open("a") as out:out.write(f"| 日志 Unix ms {latest['timestamp']} | {folder.name} 日志核验 | {latest['version']} | {message} | 依据原始审计 policy | 日志为准 | 业务效果见该轮判定 |\n")
        with (root/"runs/TIMELINE.md").open("a") as out:out.write(f"| 日志 Unix ms {latest['timestamp']} | {folder.name} policy 核验 | {latest['version']} | {message} | 见业务证据 | 完整 policy 匹配 | 不额外推断延迟 |\n")
    (folder/"dynamic-verification.json").write_text(json.dumps(result,ensure_ascii=False,indent=2))
    print(json.dumps({k:result[k] for k in ["C1","C2","C3","analysis"]},ensure_ascii=False,indent=2))
    if result["policies"]:print("latest matches",result["policies"][-1]["matching_ledger_entries"])
    return result
