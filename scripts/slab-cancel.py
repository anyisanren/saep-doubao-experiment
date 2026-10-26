#!/usr/bin/env python3
"""Cancel an observed takeover task; header coordinates must come from its screenshot."""
import argparse
import importlib.util
import json
from pathlib import Path
import subprocess
import time
root=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location("d",root/"scripts/slab-device.py")
d=importlib.util.module_from_spec(s);s.loader.exec_module(d)
p=argparse.ArgumentParser();p.add_argument("--round",required=True);p.add_argument("--x",required=True);p.add_argument("--y",required=True)
a=p.parse_args();f=root/"runs"/a.round
def shot(name):
    d.capture(f,name)
    return json.loads(subprocess.check_output([str(root/".tooling/slab-ocr"),str(f/name)]))
d.capture(f,"takeover-before-cancel.png")
d.adb("shell","input","tap",a.x,a.y)
time.sleep(.7)
rows=shot("task-details.png")
assert any("任务详情" in r["text"] for r in rows)
buttons=[r for r in rows if "停止任务" in r["text"] and r["y"]>2400]
assert len(buttons)==1, "No visible stop button; no further action"
d.adb("shell","input","tap",str(round(buttons[0]["x"])),str(round(buttons[0]["y"])))
d.event(f,"stop_task_tapped")
for _ in range(20):
    time.sleep(1)
    rows=shot("task-terminated.png")
    if any("任务已终止" in r["text"] for r in rows):
        d.event(f,"task_termination_visually_confirmed")
        d.adb("shell","input","keyevent","KEYCODE_BACK")
        subprocess.run(["python3",str(root/"scripts/slab-collect.py"),"--round",a.round,"--variant","dyn"],check=True)
        break
else:
    raise RuntimeError("Cancellation pending; no collection or repeated cancel")
