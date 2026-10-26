#!/usr/bin/env python3
"""Archive an explicit evidence-backed result for a non-Agent or derived round."""
import argparse
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser()
p.add_argument("--round", required=True)
args = p.parse_args()
folder = root / "runs" / args.round
result = json.loads((folder / "result.json").read_text())
required = ["condition", "expected", "behavior", "layer", "evidence", "verdict", "notes", "C1", "C2", "C3"]
assert all(k in result for k in required)
matrix = root / "runs/MATRIX.md"
text = matrix.read_text()
assert f"| {args.round} |" not in text
row = "| " + " | ".join(str(x).replace("|", "/").replace("\n", " ") for x in [
    args.round, *[result[k] for k in required[:7]]]) + " |"
matrix.write_text(text.replace("\n## 全局偏差", "\n" + row + "\n\n## 全局偏差"))
(folder / "REPORT.md").write_text(
    f"# {args.round}\n\n判定：{result['verdict']}\n\n"
    f"实测：{result['behavior']}\n\n拦截层：{result['layer']}\n\n"
    f"C1：{result['C1']}。C2：{result['C2']}。C3：{result['C3']}。\n\n"
    f"证据：{result['evidence']}\n\n备注：{result['notes']}\n")
(folder / "verification.json").write_text(json.dumps(result, ensure_ascii=False, indent=2))
mp = folder / "round.json"
meta = json.loads(mp.read_text()) if mp.exists() else {"round": args.round}
meta.update(status="complete", **{k: result[k] for k in ("verdict", "C1", "C2", "C3")})
mp.write_text(json.dumps(meta, ensure_ascii=False, indent=2))
sp = root / "runs/state.json"
state = json.loads(sp.read_text())
if args.round not in state["completed"]:
    state["completed"].append(args.round)
state.update(active_round=args.round, phase="complete", pending="next ordered operation card")
sp.write_text(json.dumps(state, ensure_ascii=False, indent=2))
print(row)
