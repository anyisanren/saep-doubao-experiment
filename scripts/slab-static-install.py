#!/usr/bin/env python3
"""Build and cover-install one protocol-specified static all-allow DYN variant."""
import argparse
import datetime as dt
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

root=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location("d",root/"scripts/slab-device.py")
d=importlib.util.module_from_spec(s);s.loader.exec_module(d)
p=argparse.ArgumentParser();p.add_argument("--round",required=True);p.add_argument("--version",required=True,type=int);p.add_argument("--code",required=True,type=int);p.add_argument("--tag",required=True)
a=p.parse_args();f=root/"runs"/a.round;f.mkdir(exist_ok=True)
assert not (f/"install-attempt.json").exists()
ledger_path=root/"runs/dynamic-ledger.json";ledger=json.loads(ledger_path.read_text())
(f/"prior-ledger.json").write_text(json.dumps(ledger,ensure_ascii=False,indent=2))
policy=json.loads((root/"runs/build/dyn-original-policy.json").read_text())
policy["policy_version"]=a.version;policy["updated_at"]=str(int(d.adb("shell","date","+%s%3N").decode()))
raw=json.dumps(policy,ensure_ascii=False,indent=2)+"\n"
(f/"installed-static-policy.json").write_text(raw)
(root/"slab/src/dyn/res/raw/agent_saep_policy.json").write_text(raw)
env=os.environ.copy()
env.setdefault("JAVA_HOME",str(root/".tooling/amazon-corretto-17.jdk/Contents/Home"))
env.setdefault("ANDROID_SDK_ROOT",str(d.SDK))
bundled_gradle=root/".tooling/gradle-8.9/bin/gradle"
gradle=bundled_gradle if bundled_gradle.exists() else root/"gradlew"
with (f/"build.log").open("w") as out:
    subprocess.run([str(gradle),"--no-daemon",":slab:assembleDynRelease",f"-PslabVersionCode={a.code}"],cwd=root,env=env,stdout=out,stderr=subprocess.STDOUT,check=True)
apk=root/"runs/build/apks"/f"dyn-{a.tag}.apk";assert not apk.exists()
shutil.copyfile(root/"slab/build/outputs/apk/dyn/release/slab-dyn-release.apk",apk)
policy_sha=hashlib.sha256(raw.encode()).hexdigest()
with zipfile.ZipFile(apk) as z:
    matching=[n for n in z.namelist() if n.endswith(".json") and hashlib.sha256(z.read(n)).hexdigest()==policy_sha]
assert len(matching)==1,"APK must contain the exact static policy"
d.stream(f)
before=d.adb("shell","dumpsys","package","com.saep.lab.dyn").decode();(f/"package-before.txt").write_text(before)
at_ms=int(d.adb("shell","date","+%s%3N").decode())
info={"at":d.at(),"at_ms":at_ms,"policy_version":a.version,"version_code":a.code,"policy_sha256":policy_sha,"apk_sha256":hashlib.sha256(apk.read_bytes()).hexdigest(),"apk":str(apk),"resource_path":matching[0]}
(f/"install-attempt.json").write_text(json.dumps(info,ensure_ascii=False,indent=2))
output=d.adb("install","-r",str(apk)).decode();(f/"install-output.txt").write_text(output);assert "Success" in output
(f/"package-after.txt").write_bytes(d.adb("shell","dumpsys","package","com.saep.lab.dyn"))
entry={"key":a.round+"/install","kind":"static_install","round":a.round,"at":info["at"],"version":a.version,"summary":"static all allow; preserve data","accepted":True,"policy":policy,**info}
ledger["entries"].append(entry);ledger["N"]=a.version;ledger["current_policy"]=policy;ledger_path.write_text(json.dumps(ledger,ensure_ascii=False,indent=2))
with (root/"runs/VERSIONS.md").open("a") as out:out.write(f"| {info['at']} | {a.round} 静态覆盖安装 | {a.version} | 全放行；SHA-256 {policy_sha} | adb install -r: Success；versionCode {a.code} | 安装接受；生效待 policy 核验 | 待任务日志 |\n")
with (root/"runs/TIMELINE.md").open("a") as out:out.write(f"| {info['at']} | {a.round} 静态覆盖安装 | {a.version} | Success，保留数据 | 待核验 | 待任务 policy | — |\n")
print(json.dumps(info,ensure_ascii=False),flush=True)
