#!/usr/bin/env python3
"""Prepare and dispatch exactly one remaining protocol task for SLAB-DYN."""
import argparse
import importlib.util
import json
from pathlib import Path

root=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location("a",root/"scripts/slab-agent.py")
a=importlib.util.module_from_spec(s);s.loader.exec_module(a);d=a.d
prompts={
    "R4-6a":"打开 SLAB-DYN，进入输入页，输入 r46a，点保存",
    "R4-6b":"打开 SLAB-DYN，进入输入页，输入 r46b，点保存",
    "R4-9a":"打开 SLAB-DYN，在主页备注框输入 r49a，点保存",
    "R4-9b":"打开 SLAB-DYN，在主页备注框输入 r49b，点保存",
    "R4-9c":"打开 SLAB-DYN，在主页备注框输入 r49c，点保存",
    "R4-8":"打开 SLAB-DYN，在主页备注框输入 r48，点保存",
}
p=argparse.ArgumentParser();p.add_argument("--round",required=True,choices=prompts);args=p.parse_args()
f=root/"runs"/args.round;f.mkdir(exist_ok=True)
assert not (f/"round.json").exists(),"Task preparation already exists; inspect rather than repeat"
(f/"round.json").write_text(json.dumps({"round":args.round,"package":"com.saep.lab.dyn","variant":"dyn","phase":"D","status":"planned_not_run","doubao_task_sent":False,"expected":"参见所属编号操作卡；核对业务回调和 policy 原文"},ensure_ascii=False,indent=2))
(f/"prompt.txt").write_text(prompts[args.round])
(f/"accepted-policy-before-task.json").write_text((root/"runs/dynamic-ledger.json").read_text())
d.prepare(f,"com.saep.lab.dyn",args.round,"D")
a.compose(f,"com.saep.lab.dyn")
a.send(f)
d.adb("shell","am","start","-W","-n","com.larus.nova/com.larus.home.impl.alias.AliasActivity1")
