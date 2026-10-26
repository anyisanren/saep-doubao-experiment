#!/usr/bin/env python3
"""P0-5: install the named APKs and verify their installed manifests/resources."""
from pathlib import Path
import datetime as dt
import hashlib
import json
import os
import re
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "runs/p0"
SDK = Path(os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME") or ROOT / ".tooling/android-sdk")
ADB = Path(os.environ.get("SAEP_ADB") or SDK / "platform-tools/adb")
AAPT = SDK / "build-tools/34.0.0/aapt"
SERIAL = os.environ.get("SAEP_SERIAL", "")
if not SERIAL:
    raise RuntimeError("Set SAEP_SERIAL to the test device serial before installing APKs")
profiles = json.loads((ROOT / "runs/build/policies.json").read_text())
records = []


def execute(args):
    r = subprocess.run([str(x) for x in args], capture_output=True, text=True, timeout=60)
    commands.append({"at": dt.datetime.now(dt.timezone(dt.timedelta(hours=8))).isoformat(),
                     "args": [str(x) for x in args], "exit_code": r.returncode,
                     "stdout": r.stdout, "stderr": r.stderr})
    if r.returncode:
        raise RuntimeError(f"{args}: {r.stdout} {r.stderr}")
    return r.stdout


(OUT / "installed").mkdir(parents=True, exist_ok=True)
(ROOT / "runs/build/apks").mkdir(parents=True, exist_ok=True)
for info in profiles:
    commands = []
    name, pkg = info["variant"], info["package"]
    record = {"variant": name, "package": pkg, "commands": commands}
    records.append(record)
    try:
        built = ROOT / f"slab/build/outputs/apk/{name}/release/slab-{name}-release.apk"
        assert built.exists(), str(built)
        installed = OUT / "installed" / f"{name}.apk"
        # Resume tool setup without reinstalling APKs already pulled from this device.
        previously_pulled = installed.exists() and installed.read_bytes() == built.read_bytes()
        if not previously_pulled:
            execute([ADB, "-s", SERIAL, "install", "-r", built])
        remote = execute([ADB, "-s", SERIAL, "shell", "pm", "path", pkg]).strip()
        assert remote.startswith("package:") and "\n" not in remote, remote
        execute([ADB, "-s", SERIAL, "pull", remote.removeprefix("package:"), installed])
        assert hashlib.sha256(installed.read_bytes()).digest() == hashlib.sha256(built.read_bytes()).digest()
        manifest = execute([AAPT, "dump", "xmltree", installed, "AndroidManifest.xml"])
        badging = execute([AAPT, "dump", "badging", installed])
        (OUT / "installed" / f"{name}-manifest.txt").write_text(manifest)
        assert f'package="{pkg}"' in manifest
        assert "E: queries" not in manifest
        assert "android.permission.QUERY_ALL_PACKAGES" not in manifest
        target = int(re.search(r"targetSdkVersion:'(\d+)'", badging).group(1))
        assert target >= 30
        activities = re.findall(r'E: activity .*?\n\s*A: android:name[^=]*="([^"]+)"', manifest)
        assert set(activities) == set(info["activities"].values()), activities
        resources = execute([AAPT, "dump", "--values", "resources", installed])
        resource_path = re.search(
            r"resource 0x[0-9a-f]+ [^\n]*:raw/agent_saep_policy: t=[^\n]*\n\s*\(string8\) \"([^\"]+)\"",
            resources,
        ).group(1)
        with zipfile.ZipFile(installed) as z:
            policy = z.read(resource_path)
            digest = hashlib.sha256(policy).hexdigest()
            assert digest == info["policy_sha256"]
            page_keys = re.findall(r'"(com\.saep\.lab\.[^"]+Activity)"\s*:', policy.decode())
            assert all(key in activities for key in page_keys)
            dex = z.read("classes.dex")
            assert all(("L" + activity.replace(".", "/") + ";").encode() in dex for activity in activities)
            current_code = hashlib.sha256(dex).hexdigest()
        record.update({"verified": True, "target_sdk": target, "activities": activities,
                       "policy_activity_keys": page_keys, "policy_sha256": digest,
                       "policy_version": info["policy_version"],
                       "apk_sha256": hashlib.sha256(installed.read_bytes()).hexdigest(),
                       "dex_sha256": current_code, "extra_visibility_declarations": False,
                       "label": re.search(r"application-label:'([^']+)'", badging).group(1)})
        shutil.copy2(built, ROOT / "runs/build/apks" / ("dyn-static-v1.apk" if name == "dyn" else f"{name}.apk"))
    except Exception as e:
        record.update({"verified": False, "error": str(e)})
        raise
    finally:
        (OUT / "installed-verification.json").write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n")
    print(f"{name}: installed APK verified, target SDK {target}, six classes, policy SHA-256 matches", flush=True)
