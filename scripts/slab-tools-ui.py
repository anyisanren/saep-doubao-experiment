#!/usr/bin/env python3
"""Preparation-only helpers for the visible tools in SLAB's MainActivity."""
import importlib.util
import re
import shlex
import time
from pathlib import Path

root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("device", root / "scripts/slab-device.py")
d = importlib.util.module_from_spec(spec)
spec.loader.exec_module(d)

def bounds(node):
    return list(map(int, re.findall(r"\d+", node.get("bounds"))))

def locate(label, direction="down"):
    for _ in range(10):
        state = d.tree()
        nodes = [n for n in state.iter("node")
                 if n.get("text") == label or n.get("content-desc") == label]
        for n in nodes:
            a, b, c, e = bounds(n)
            if a < c and b < e and 180 < (b + e) / 2 < 2680:
                return n
        if direction == "down":
            d.adb("shell", "input", "swipe", "1150", "2300", "1150", "650", "300")
        else:
            d.adb("shell", "input", "swipe", "1150", "650", "1150", "2300", "300")
    raise RuntimeError(f"Control not visible: {label}; no action performed")

def fill(label, text, direction="down", paste=False):
    node = locate(label, direction)
    d.tap(node)
    d.adb("shell", "input", "keycombination", "113", "29")
    if paste:
        d.adb("shell", "input", "keyevent", "KEYCODE_PASTE")
    else:
        d.adb("shell", shlex.join(["input", "text", str(text)]))
    time.sleep(0.6)
    d.adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(0.6)
    state = d.tree()
    nodes = [n for n in state.iter("node") if n.get("content-desc") == label]
    if len(nodes) != 1 or nodes[0].get("text") != str(text):
        raise RuntimeError(f"Field verification failed: {label}; nothing submitted")
    return nodes[0]
