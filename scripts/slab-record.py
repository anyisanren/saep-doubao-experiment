#!/usr/bin/env python3
"""Archive the investigator's explicit verdict; never decides or repeats a test."""
import argparse
import json
import re
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument("--round", required=True)
p.add_argument("--verdict", required=True)
p.add_argument("--behavior", required=True)
p.add_argument("--layer", required=True)
p.add_argument("--notes", default="")
p.add_argument("--reply-prefix")
args = p.parse_args()
root = Path(__file__).resolve().parents[1]
f = root / "runs" / args.round
meta = json.loads((f / "round.json").read_text())
pkg = meta["package"]
analysis = json.loads((f / f"analysis_{pkg}.json").read_text())
export = json.loads((f / f'export_D_{pkg}.json').read_text())
ui = json.loads((f / "response-ui-text.json").read_text())
statuses = [i for i, x in enumerate(ui) if x.get("resource-id") == "com.larus.nova:id/tv"
            and (x.get("text", "").startswith("已完成任务") or x.get("text", "").startswith("已失败")
                 or x.get("text", "").startswith("已取消") or x.get("text", "").startswith("已终止"))]
if not statuses:
    raise RuntimeError("No completed/failed/cancelled task status in captured UI; extract reply explicitly")
tail = ui[statuses[-1] + 1:]
reply_rows = [x for x in tail if not x.get("resource-id") and x.get("text")
              and x.get("content-desc")]
if args.reply_prefix:
    reply_rows = [x for x in ui if x.get("text", "").startswith(args.reply_prefix)]
if not reply_rows:
    raise RuntimeError("Reply text unavailable")
reply = reply_rows[0]["content-desc"]
(f / "doubao-response.txt").write_text(reply + "\n")
if analysis["policy_record_count"] == 0:
    c1 = "无可核对policy记录"
elif analysis["policy_all_match_static"]:
    c1 = f'{analysis["policy_record_count"]}条policy均匹配静态v{export["static_policy_version"]}'
else:
    c1 = "policy与静态不一致，详见策略原文"
c2 = "缺失（同全局偏差G01）" if not analysis["reason_code_literal_occurrences"] else "见原文逐条核对"
system_text = (f / "system.log").read_text(errors="replace")
system_stamps = [float(m.group(1)) * 1000 for line in system_text.splitlines()
                 if (m := re.match(r"\s*(\d+\.\d+)\s", line))]
c3_checks = {
    "round_package": export["round"] == args.round and export["package"] == pkg,
    "main_foreground_correct": any(x["event"] == "foreground" and x["package"] == pkg
                                   and x["data"]["page"] == "M" for x in export["observations"]),
    "at_least_two_screenshots": len(list(f.glob(f"r{args.round}_d*.png"))) >= 2,
    "system_log": (f / "system.log").stat().st_size > 0,
    "system_log_covers_task": bool(system_stamps)
        and system_stamps[0] <= analysis["task_start_device_ms"]
        and system_stamps[-1] >= analysis["task_end_device_ms"] - 2000,
    "wall_and_uptime": bool(export["wall_time_ms"] and export["uptime_ms"]),
    "policy_hash": bool(export["static_policy_sha256"]),
    "reply": bool(reply),
}
c3 = "齐全" if all(c3_checks.values()) else "不齐全：" + str(c3_checks)
evidence = f'{analysis["audit_sources"]}；版本{analysis["policy_versions"]}；C1 {c1}；C2 {c2}'
report = (f"# {args.round} 判定\n\n判定：{args.verdict}\n\n实测：{args.behavior}\n\n"
          f"拦截层：{args.layer}\n\nC1：{c1}。C2：{c2}。C3：{c3}。\n\n"
          f"审计：{evidence}。\n\n备注：{args.notes}\n\n"
          f"豆包回复原文：\n\n{reply}\n")
(f / "REPORT.md").write_text(report)
(f / "verification.json").write_text(json.dumps({"C1": c1, "C2": c2, "C3": c3,
    "C3_checks": c3_checks, "verdict": args.verdict, "layer": args.layer,
    "behavior": args.behavior, "notes": args.notes}, ensure_ascii=False, indent=2))
matrix = root / "runs/MATRIX.md"
text = matrix.read_text()
if f"| {args.round} |" in text:
    raise RuntimeError("MATRIX row already exists; amend deliberately")
row = "| " + " | ".join(str(x).replace("|", "/").replace("\n", " ") for x in [
    args.round, f'{meta.get("variant", pkg)}静态v{export["static_policy_version"]}',
    meta["expected"], args.behavior, args.layer, evidence, args.verdict, args.notes]) + " |\n"
text = text.replace("\n## 全局偏差", "\n" + row + "\n## 全局偏差")
matrix.write_text(text)
meta.update(status="complete", verdict=args.verdict, C1=c1, C2=c2, C3=c3)
(f / "round.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2))
state_path = root / "runs/state.json"
state = json.loads(state_path.read_text())
if args.round not in state["completed"]:
    state["completed"].append(args.round)
state.update(active_round=args.round, phase="complete", pending="next ordered operation card")
state_path.write_text(json.dumps(state, ensure_ascii=False, indent=2))
print(row.strip())
