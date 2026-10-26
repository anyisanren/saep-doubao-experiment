#!/usr/bin/env python3
"""Execute the protocol's explicitly specified non-Agent submission rounds."""
import argparse
import copy
import importlib.util
import json
import os
from pathlib import Path
import signal
import time

root = Path(__file__).resolve().parents[1]
def module(name, filename):
    spec = importlib.util.spec_from_file_location(name, root / "scripts" / filename)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value
x = module("submit", "slab-submit.py")
e = module("evidence", "slab-evidence.py")
d = x.d

def ledger():
    return json.loads((root/"runs/dynamic-ledger.json").read_text())

def policy_for(version, path=None, all_allow=False):
    p = (json.loads((root/"slab/src/dyn/res/raw/agent_saep_policy.json").read_text())
         if all_allow else copy.deepcopy(ledger()["current_policy"]))
    p["policy_version"] = version
    p["updated_at"] = str(int(d.adb("shell","date","+%s%3N").decode()))
    if path:
        if path[0] == "activity":
            key = "com.saep.lab."+path[1]+"Activity"
            names = {"Input":"输入页","Publish":"发布页","Record":"记录页","Reward":"签到页","Target":"目标页","Main":"主页"}
            item = p["scope"].setdefault("activities",{}).setdefault(key, {"name":names[path[1]],"page_scope":{}})
            item["page_scope"][path[2]] = True
        else:
            p["scope"]["agent_intents"][path[1]] = True
    return p

def begin(round_id):
    f=root/"runs"/round_id
    f.mkdir(exist_ok=True)
    assert not (f/"round.json").exists(), "Round already begun"
    (f/"round.json").write_text(json.dumps({"round":round_id,"package":"com.saep.lab.dyn",
        "variant":"dyn","phase":"S","status":"submissions_preparing","doubao_task_sent":False},ensure_ascii=False,indent=2))
    d.prepare(f,"com.saep.lab.dyn",round_id,"S")
    (f/f"r{round_id}_d1.png").write_bytes((f/"prepared.png").read_bytes())
    d.adb("shell","am","start","-W","-n","com.saep.lab.dyn/com.saep.lab.MainActivity")
    return f

def finish(f):
    e.export_request(f,"com.saep.lab.dyn")
    time.sleep(1)
    e.export_pull(f,"com.saep.lab.dyn","S")
    d.capture(f,f"r{f.name}_d2.png")
    try:
        os.kill(int((f/"logcat-pid.txt").read_text()),signal.SIGTERM)
    except ProcessLookupError:
        d.event(f,"logcat_already_exited")
    d.event(f,"logcat_stopped")

if __name__=="__main__":
    ap=argparse.ArgumentParser()
    ap.add_argument("--round",choices=["R3-5","R3-6","R3-7","R3-8"],required=True)
    args=ap.parse_args()
    f=begin(args.round)
    n=ledger()["N"]
    if args.round=="R3-5":
        for item,version in [("equal",n),("lower",n-1)]:
            x.submit(f,item,policy_for(version,("activity","Reward","input_disable")),version,"当前内容 + 签到页禁输入")
    elif args.round=="R3-6":
        x.submit(f,"relax",policy_for(n+1,all_allow=True),n+1,"全放行")
    elif args.round=="R3-7":
        raw=json.dumps(policy_for(n+1,("activity","Record","input_disable")),ensure_ascii=True,separators=(",",":"))
        (f/"legal-before-truncation.txt").write_text(raw)
        x.submit(f,"malformed",raw[:-3],n+1,"当前内容 + 记录页禁输入，合法 JSON 末尾删 3 字符")
    else:
        additions=[("activity","Record","input_disable"),("activity","Reward","screenshot_disable"),
                   ("activity","Target","screenshot_disable"),("intent","modify_content"),("intent","delete_content")]
        results=[]
        last_rejected=None
        for i,addition in enumerate(additions,1):
            version=ledger()["N"]+1
            result=x.submit(f,f"burst-{i}",policy_for(version,addition),version,str(addition))
            results.append(result)
            if result.get("return_value")!=1:
                last_rejected=addition
        rate_in_log = "E_RATE_LIMITED" in (f/"system.log").read_text(errors="replace")
        if any("E_RATE_LIMITED" in json.dumps(r) for r in results) or rate_in_log:
            (f/"rate-wait.json").write_text(json.dumps({"at":d.at(),"seconds":60,"last_rejected":last_rejected},ensure_ascii=False))
            print("Rate limit observed; protocol's single 60-second wait begins.",flush=True)
            time.sleep(60)
            assert last_rejected is not None, "Rate code only in logs with no rejected result; inspect before the single retry"
            version=ledger()["N"]+1
            x.submit(f,"rate-retry",policy_for(version,last_rejected),version,str(last_rejected))
    finish(f)
