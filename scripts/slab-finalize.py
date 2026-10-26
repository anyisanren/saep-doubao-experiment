#!/usr/bin/env python3
"""Close the existing evidence archive without starting any device test."""
import collections
import datetime as dt
import hashlib
import json
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
runs = root / "runs"
zone = dt.timezone(dt.timedelta(hours=8))
now = dt.datetime.now(zone).isoformat()
def wall(ms):
    return dt.datetime.fromtimestamp(int(ms)/1000, zone).isoformat()
def save(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n")

# Resolve forward-looking notes using the evidence already collected later.
updates = {
    "R3-6": {"notes": "后续 R3-7 的 5 条 policy 已完整匹配本次接受的全放行 v3，且 d37 保存成功；未重复提交"},
    "R3-8": {"notes": "仅说明本次约25秒间隔的 UI 提交节奏下未出现频控；最后接受 v8 已由后续 R4-6a 的 5 条完整 policy 核验，未单独验证五项新增限制的业务效果"},
}
matrix = (runs / "MATRIX.md").read_text()
for round_id, changes in updates.items():
    folder = runs / round_id
    value = json.loads((folder / "result.json").read_text())
    value.update(changes)
    save(folder / "result.json", value)
    save(folder / "verification.json", value)
    fields = ["condition", "expected", "behavior", "layer", "evidence", "verdict", "notes"]
    row = "| " + " | ".join([round_id] + [str(value[k]).replace("|", "/").replace("\n", " ") for k in fields]) + " |"
    matrix = re.sub(r"^\| " + re.escape(round_id) + r" \|.*$", lambda _: row, matrix, flags=re.M)
    (folder / "REPORT.md").write_text(
        f"# {round_id}\n\n判定：{value['verdict']}\n\n实测：{value['behavior']}\n\n"
        f"拦截层：{value['layer']}\n\nC1：{value['C1']}。C2：{value['C2']}。C3：{value['C3']}。\n\n"
        f"证据：{value['evidence']}\n\n备注：{value['notes']}\n")

rows = [[x.strip() for x in line.split("|")[1:-1]] for line in matrix.splitlines()
        if line.startswith("| R") or line.startswith("| 1")]
expected = ["R1A-4", *[f"1A-{i}" for i in range(1,4)], *[f"1B-{i}" for i in range(1,4)],
            *[f"R2-{i}" for i in range(1,16) if i != 4], "R2-4a", "R2-4b", "R2-4c", "R2-11c",
            *[f"R3-{i}" for i in range(1,9)], *[f"R4-{i}" for i in range(1,10)]]
assert len(rows) == len(expected) == 42
assert all(len(row) == 8 for row in rows)
assert collections.Counter(row[0] for row in rows) == collections.Counter(expected)
counts = collections.Counter(row[6].split("（")[0] for row in rows)
assert set(counts) <= {"符合", "不符", "文档未定义", "无法验证"}
for row in rows:
    folder = "R1A" if row[0].startswith("1A-") else "R1B" if row[0].startswith("1B-") else row[0]
    assert (runs / folder / "REPORT.md").is_file(), folder
    if row[6].startswith("无法验证"):
        assert "（" in row[6], row[0]

intro = (f"共 42 个编号项，已全部判定并归档：符合 {counts['符合']}，不符 {counts['不符']}，"
         f"文档未定义 {counts['文档未定义']}，无法验证 {counts['无法验证']}。条件未触发的分支已注明。")
matrix = re.sub(r"\n\n共 42 个编号项[^\n]*", "", matrix)
matrix = matrix.replace("唯一依据：runs/p0/instruction.txt。P0 见 runs/p0/REPORT.md。",
                        "唯一依据：runs/p0/instruction.txt。P0 见 runs/p0/REPORT.md。\n\n" + intro)
(runs / "MATRIX.md").write_text(matrix)

ledger = json.loads((runs / "dynamic-ledger.json").read_text())
assert ledger["N"] == 1 and ledger["latest_policy_evidence"]["round"] == "R4-8"
assert len({e["key"] for e in ledger["entries"]}) == len(ledger["entries"])
for name, text_intro in [
    ("VERSIONS.md", "已完成至 R4-8。最终 N=1，6 条任务 policy 完整匹配初始静态 v1，r48 保存成功。历史接受返回与运行时核验分别保留；未获得运行时证据的提交不推断已生效。"),
    ("TIMELINE.md", "第三组与 R4-6 → R4-9 → R4-8 已完成。R3-2 超出10秒条件；R3-7 中断后仅补取历史证据；R4-9 的计划内重启另有 boot_id 和开机时长证据。")
]:
    path = runs / name
    text = path.read_text()
    head, rest = text.split("\n\n", 1)
    _, table = rest.split("\n\n", 1)
    text = head + "\n\n" + text_intro + "\n\n" + table
    text = re.sub(r"\| (?:日志 )?Unix ms (\d+) \|", lambda m: "| " + wall(m[1]) + " |", text)
    text = text.replace("\n\n|", "\n|")
    # Preserve a blank line between the introductory paragraph and the table.
    text = text.replace(text_intro + "\n|", text_intro + "\n\n|")
    path.write_text(text)

final_observations = []
for round_id in ["R4-6a", "R4-6b", "R4-9a", "R4-9c", "R4-8"]:
    folder = runs / round_id
    verification = json.loads((folder / "dynamic-verification.json").read_text())
    assert verification["C3"] == "齐全" and all(verification["checks"].values())
    assert verification["policies"] and all(p["matches_accepted"] for p in verification["policies"])
    export = json.loads((folder / "export_D_com.saep.lab.dyn.json").read_text())
    analysis = verification["analysis"]
    observations = [x for x in export["observations"] if x["phase"] == "D"
                    and analysis["task_start_device_ms"] <= x["wall_time_ms"] <= analysis["task_end_device_ms"]]
    business = [x for x in observations if x["event"] == "business_success"]
    times = [x["wall_time_ms"] for x in business]
    takeover_events = [json.loads(x) for x in (folder / "host-actions.jsonl").read_text().splitlines()
                       if '"file": "takeover-before-cancel.png"' in x]
    final_observations.append({"round": round_id, "first_policy_ms": verification["policies"][0]["timestamp"],
                               "business_times_ms": times, "takeover_observed_at": takeover_events[0]["at"] if takeover_events else None})
    # Keep the full raw UI and make the current task's reply easy to find.
    replies = verification["replies"]
    marker = {"R4-6a":"r46a", "R4-6b":"r46b", "R4-9a":"r49a", "R4-9c":"r49c", "R4-8":"r48"}[round_id]
    current = [r for r in replies if marker in r and not r.strip().startswith("打开 ")]
    (folder / "doubao-current-response.txt").write_text("\n\n".join(current))
    assert current, round_id

with (runs / "TIMELINE.md").open("a") as out:
    for x in final_observations:
        v = json.loads((runs / x["round"] / "dynamic-verification.json").read_text())
        version = v["policies"][0]["version"]
        observed = wall(x["business_times_ms"][0]) if x["business_times_ms"] else x["takeover_observed_at"]
        basis = "App 保存回调 + 完整 policy" if x["business_times_ms"] else "接管截图 + 备注空/保存0 + 完整 policy"
        out.write(f"| {wall(x['first_policy_ms'])} | {x['round']} 最终证据索引 | {version} | 首条任务 policy | 本轮行为证据 {observed} | {basis} | 记录观察时刻，不等于精确传播延迟 |\n")

# Restore the build input to its original baseline. This does not install an APK.
original = runs / "build/dyn-original-policy.json"
source = root / "slab/src/dyn/res/raw/agent_saep_policy.json"
source_before = hashlib.sha256(source.read_bytes()).hexdigest()
source.write_bytes(original.read_bytes())
frozen = runs / "build/apks/dyn-static-v1.apk"
frozen_sha = hashlib.sha256(frozen.read_bytes()).hexdigest()
assert frozen_sha == "3823146e7f20eb7179e352adea75cdc34a5542dc4bd896fd68c6ef1b3b328b30"
assert hashlib.sha256(source.read_bytes()).hexdigest() == "29d0b8c355397ad3a7c0fcaa2020df35508bf7638abf15af419faa020de07d08"

state_path = runs / "state.json"
state = json.loads(state_path.read_text())
state.pop("next_after_H", None)
state.update(active_round=None, phase="complete", pending=None, completed_at=now,
             matrix_items=42, verdict_counts=dict(counts), final_policy_version=1)
save(state_path, state)
resume_path = runs / "resume-state.json"
history = runs / "history"
history.mkdir(exist_ok=True)
backup = history / "resume-state-before-completion.json"
if not backup.exists():
    backup.write_bytes(resume_path.read_bytes())
save(resume_path, {"status":"complete", "no_active_task":True, "pending":[], "current_N":1,
                  "completed_at":now, "supersedes":"history/resume-state-before-completion.json"})
report = {"at":now, "matrix_count":42, "expected_ids_match":True, "no_duplicate_rows":True,
          "verdict_counts":dict(counts), "final_N":1, "remaining_conditions":[],
          "recent_C3_all_present":True, "frozen_apk_sha256":frozen_sha,
          "source_restoration":{"before_sha256":source_before,"after_sha256":hashlib.sha256(source.read_bytes()).hexdigest(),"new_build_or_install":False},
          "recent_evidence_index":final_observations}
save(runs / "final-integrity.json", report)
print(json.dumps(report, ensure_ascii=False, indent=2))
