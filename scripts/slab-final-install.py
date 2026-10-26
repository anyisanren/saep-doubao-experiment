#!/usr/bin/env python3
"""R4-8: one uninstall and one reinstall of the frozen initial DYN APK."""
import hashlib
import importlib.util
import json
from pathlib import Path
import zipfile

root = Path(__file__).resolve().parents[1]
s = importlib.util.spec_from_file_location("d", root / "scripts/slab-device.py")
d = importlib.util.module_from_spec(s)
s.loader.exec_module(d)
f = root / "runs/R4-8"
f.mkdir(exist_ok=True)
assert not (f / "uninstall-attempt.json").exists(), "Do not repeat uninstall"
apk = root / "runs/build/apks/dyn-static-v1.apk"
apk_sha = hashlib.sha256(apk.read_bytes()).hexdigest()
assert apk_sha == "3823146e7f20eb7179e352adea75cdc34a5542dc4bd896fd68c6ef1b3b328b30"
raw = (root / "runs/build/dyn-original-policy.json").read_bytes()
policy_sha = hashlib.sha256(raw).hexdigest()
assert policy_sha == "29d0b8c355397ad3a7c0fcaa2020df35508bf7638abf15af419faa020de07d08"
with zipfile.ZipFile(apk) as z:
    assert z.read("res/rm.json") == raw
policy = json.loads(raw)
ledger_path = root / "runs/dynamic-ledger.json"
ledger = json.loads(ledger_path.read_text())
(f / "prior-ledger.json").write_text(json.dumps(ledger, ensure_ascii=False, indent=2))
d.stream(f)
(f / "package-before.txt").write_bytes(d.adb("shell", "dumpsys", "package", "com.saep.lab.dyn"))
uninstall_info = {"at": d.at(), "device_ms": int(d.adb("shell", "date", "+%s%3N"))}
(f / "uninstall-attempt.json").write_text(json.dumps(uninstall_info, indent=2))
output = d.adb("uninstall", "com.saep.lab.dyn").decode()
(f / "uninstall-output.txt").write_text(output)
assert "Success" in output
with (root / "runs/TIMELINE.md").open("a") as out:
    out.write(f"| {uninstall_info['at']} | R4-8 卸载 dyn | — | Success | 待重装任务验证缓存 | 仅卸载本轮测试包 | — |\n")
info = {"at": d.at(), "device_ms": int(d.adb("shell", "date", "+%s%3N")),
        "apk": str(apk), "apk_sha256": apk_sha, "policy_sha256": policy_sha, "version_code": 20}
(f / "install-attempt.json").write_text(json.dumps(info, ensure_ascii=False, indent=2))
output = d.adb("install", str(apk)).decode()
(f / "install-output.txt").write_text(output)
assert "Success" in output
(f / "package-after.txt").write_bytes(d.adb("shell", "dumpsys", "package", "com.saep.lab.dyn"))
(f / "installed-static-policy.json").write_bytes(raw)
entry = {"key": "R4-8/install", "kind": "static_install", "round": "R4-8",
         "version": 1, "accepted": True, "summary": "uninstall then original static v1",
         "policy": policy, **info}
ledger["entries"].append(entry)
ledger.update(N=1, current_policy=policy)
ledger_path.write_text(json.dumps(ledger, ensure_ascii=False, indent=2))
with (root / "runs/VERSIONS.md").open("a") as out:
    out.write(f"| {info['at']} | R4-8 卸载后重装初始 APK | 1 | 全放行；SHA-256 {policy_sha} | uninstall/install 均 Success；code20 | 安装接受；生效待 policy 核验 | 待任务日志 |\n")
with (root / "runs/TIMELINE.md").open("a") as out:
    out.write(f"| {info['at']} | R4-8 重装初始静态 v1 | 1 | Success；冻结 APK 摘要一致 | 待核验 | 待任务业务与 policy | — |\n")
print(json.dumps(info, ensure_ascii=False), flush=True)
