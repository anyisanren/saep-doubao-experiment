#!/usr/bin/env python3
"""Read collected evidence; compare policy objects with provided accepted policies."""
import json
from pathlib import Path
import re

def check(folder, accepted):
    pkg = "com.saep.lab.dyn"
    export = json.loads((folder / f"export_D_{pkg}.json").read_text())
    analysis = json.loads((folder / f"analysis_{pkg}.json").read_text())
    audit = json.loads((folder / f"task-audit_{pkg}.json").read_text())
    policies = []
    for row in sorted(audit, key=lambda r:int(r["timestamp"])):
        if row.get("agent_decision"):
            dec = json.loads(row["agent_decision"])
            if dec.get("policy"):
                policy = json.loads(dec["policy"])
                policies.append({"timestamp": int(row["timestamp"]), "activity": row.get("target_activity"),
                    "action": dec.get("action_type"), "version":policy["policy_version"],
                    "matches_accepted": policy in accepted, "policy": policy})
    ui = json.loads((folder / "response-ui-text.json").read_text())
    replies = [x["content-desc"] for x in ui if not x.get("resource-id") and x.get("text") and x.get("content-desc")]
    (folder / "doubao-response.txt").write_text("\n\n".join(replies))
    stamps = [float(m.group(1))*1000 for line in (folder/"system.log").read_text(errors="replace").splitlines()
              if (m:=re.match(r"\s*(\d+\.\d+)\s",line))]
    checks = {
        "round_package":export["round"]==folder.name and export["package"]==pkg,
        "main_foreground_correct":any(x["event"]=="foreground" and x["package"]==pkg and x["data"]["page"]=="M" for x in export["observations"]),
        "screenshots":len(list(folder.glob(f"r{folder.name}_d*.png")))>=2,
        "system_log_covers_task":bool(stamps) and stamps[0]<=analysis["task_start_device_ms"] and stamps[-1]>=analysis["task_end_device_ms"]-2000,
        "wall_and_uptime":bool(export["wall_time_ms"] and export["uptime_ms"]),
        "reply":bool(replies),
    }
    result={"C1":f"{len(policies)} 条 policy；版本 {sorted(set(p['version'] for p in policies))}；"+
             ("均匹配对应已接受内容" if policies and all(p["matches_accepted"] for p in policies) else "需核对原文"),
        "C2":"缺失（同全局偏差 G01）",
        "C3":"齐全" if all(checks.values()) else "不齐全："+str(checks),
        "checks":checks,"policies":policies,"analysis":analysis,"replies":replies}
    (folder/"dynamic-verification.json").write_text(json.dumps(result,ensure_ascii=False,indent=2))
    return result
