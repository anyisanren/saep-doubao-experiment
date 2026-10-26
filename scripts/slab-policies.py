#!/usr/bin/env python3
"""Generate only the v2 instruction's named static conditions."""
from pathlib import Path
from copy import deepcopy
import hashlib
import json
import time

ROOT = Path(__file__).resolve().parents[1]
PROFILES = "base a b g21 g22 g23 g24a g24b g24c g25 g26 g27 g28 g29 g2a g2f g2b g2c g2d g2e g43 dyn".split()
ACTIVITIES = {k: "com.saep.lab." + v for k, v in {
    "M": "MainActivity", "I": "InputActivity", "R": "RecordActivity",
    "P": "PublishActivity", "W": "RewardActivity", "T": "TargetActivity",
}.items()}
timestamp = str(time.time_ns() // 1_000_000)
flags = dict(global_disable=False, screenshot_disable=False, input_disable=False)
baseline = {
    "schema": "AGRP-Policy/1.0", "policy_version": 1,
    "package": "", "updated_at": timestamp,
    "default_policy": {"app": deepcopy(flags)},
    "scope": {
        "app": deepcopy(flags),
        "activities": {ACTIVITIES["T"]: {"name": "目标页", "page_scope": deepcopy(flags)}},
        "agent_intents": dict(modify_content=False, post_content=False,
                              delete_content=False, account_incentive=False),
    },
}
records = []
for profile in PROFILES:
    p = deepcopy(baseline)
    p["package"] = f"com.saep.lab.{profile}"
    scope = p["scope"]
    if profile in ("g21", "g22", "g23"):
        scope["app"][{"g21": "global_disable", "g22": "screenshot_disable",
                      "g23": "input_disable"}[profile]] = True
    if profile in ("g24a", "g24b", "g24c", "g25"):
        scope["activities"][ACTIVITIES["T"]]["page_scope"][
            {"g24a": "global_disable", "g24b": "screenshot_disable",
             "g24c": "input_disable", "g25": "global_disable"}[profile]] = True
    if profile == "g25":
        scope["activities"][ACTIVITIES["T"]]["name"] = "自定义名称XYZQ"
    if profile in ("g26", "g27", "g28", "g29"):
        scope["agent_intents"][{"g26": "modify_content", "g27": "delete_content",
                                "g28": "post_content", "g29": "account_incentive"}[profile]] = True
    if profile == "g2a":
        scope["app"]["input_disable"] = True
        scope["activities"][ACTIVITIES["M"]] = {"name": "主页", "page_scope": {"input_disable": False}}
    if profile in ("g2f", "g2b", "g2c", "g2d", "g2e"):
        p["default_policy"]["app"]["global_disable"] = True
    if profile == "g2b":
        p["schema"] = "AGRP-Policy/9.9"
    if profile == "g2d":
        p["updated_at"] = "not-a-number"
    if profile == "g2e":
        p["package"] = "com.other.notme"
    if profile == "g43":
        p["default_policy"]["app"] = {k: True for k in flags}
        p["scope"] = {"app": {}}
    content = json.dumps(p, ensure_ascii=False, indent=2)
    if profile == "g2c":
        content = content[:-3]
    path = ROOT / "slab/src" / profile / "res/raw/agent_saep_policy.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)
    records.append({"variant": profile, "package": f"com.saep.lab.{profile}",
                    "policy_version": 1, "updated_at": timestamp,
                    "policy_sha256": hashlib.sha256(content.encode()).hexdigest(),
                    "policy_file": str(path.relative_to(ROOT)), "activities": ACTIVITIES})
out = ROOT / "runs/build"
out.mkdir(parents=True, exist_ok=True)
(out / "policies.json").write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n")
print(f"Generated {len(records)} policies; manifest: runs/build/policies.json")
