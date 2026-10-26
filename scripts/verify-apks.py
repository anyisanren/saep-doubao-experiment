#!/usr/bin/env python3
"""Verify the frozen handoff APK bytes and embedded policy bytes, without a device."""
import hashlib
import json
from pathlib import Path
import zipfile


def main():
    root = Path(__file__).resolve().parents[1]
    archive = root / "apks"
    manifest = json.loads((archive / "manifest.json").read_text())
    indexed = {entry["file"] for entry in manifest["apks"]}
    actual = {str(path.relative_to(archive)) for path in archive.rglob("*.apk")}
    if actual != indexed:
        raise SystemExit(f"APK inventory differs: missing={indexed - actual}, extra={actual - indexed}")
    checksums = []
    for entry in manifest["apks"]:
        path = archive / entry["file"]
        raw = path.read_bytes()
        digest = hashlib.sha256(raw).hexdigest()
        if len(raw) != entry["size_bytes"] or digest != entry["sha256"]:
            raise SystemExit(f"APK checksum/size mismatch: {path}")
        with zipfile.ZipFile(path) as package:
            policy = package.read(entry["policy_resource"])
        if hashlib.sha256(policy).hexdigest() != entry["policy_sha256"]:
            raise SystemExit(f"Embedded policy mismatch: {path}")
        if (archive / entry["policy_file"]).read_bytes() != policy:
            raise SystemExit(f"Extracted policy differs from APK: {path}")
        checksums.append(f"{digest}  {entry['file']}\n")
    if (archive / "SHA256SUMS.txt").read_text() != "".join(checksums):
        raise SystemExit("SHA256SUMS.txt does not match manifest.json")
    print(f"Verified {len(indexed)} frozen APKs and their embedded policies.")


if __name__ == "__main__":
    main()
