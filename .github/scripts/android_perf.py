#!/usr/bin/env python3
"""Mide fluidez de una APK ya instalada: desplazar, pantallas quietas y arranque en frío.

Uso: android_perf.py ETIQUETA ARCHIVO_SALIDA
Se usa desde el flujo "Comparar rendimiento" para medir la versión de antes y la de ahora
en el mismo emulador y en la misma corrida (A, B, A, B), así la variación entre máquinas no influye.
"""
import re
import subprocess
import sys
import time

PACKAGE = "com.aistudio.bravova.rdshop"
LABEL, OUT = sys.argv[1], sys.argv[2]


def adb(*args):
    return subprocess.run(["adb", *args], capture_output=True, text=True).stdout


def size():
    m = re.search(r"(\d+)x(\d+)", adb("shell", "wm", "size"))
    return (int(m.group(1)), int(m.group(2))) if m else (1080, 2400)


W, H = size()


def tap_text(*labels, wait=2.0):
    adb("shell", "uiautomator", "dump", "/sdcard/u.xml")
    xml = adb("shell", "cat", "/sdcard/u.xml")
    for label in labels:
        for m in re.finditer(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
            if m.group(1).startswith(label):
                x = (int(m.group(2)) + int(m.group(4))) // 2
                y = (int(m.group(3)) + int(m.group(5))) // 2
                adb("shell", "input", "tap", str(x), str(y)); time.sleep(wait)
                return True
    return False


def swipe(up=True):
    y1, y2 = (0.75, 0.30) if up else (0.30, 0.75)
    adb("shell", "input", "swipe", str(W // 2), str(int(H * y1)), str(W // 2), str(int(H * y2)), "300")
    time.sleep(0.8)


def frames(label):
    out = adb("shell", "dumpsys", "gfxinfo", PACKAGE)
    def grab(key):
        for line in out.splitlines():
            if line.strip().startswith(key):
                return line.split(":", 1)[1].strip()
        return "?"
    adb("shell", "dumpsys", "gfxinfo", PACKAGE, "reset")
    return f"{LABEL} | {label}: cuadros={grab('Total frames rendered')} p50={grab('50th percentile')} " \
           f"p90={grab('90th percentile')} p99={grab('99th percentile')}"


rows = []
adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
time.sleep(12)
tap_text("Saltar", wait=3)
tap_text("Continuar como Invitado", "Invitado", wait=3)
time.sleep(15)
adb("shell", "dumpsys", "gfxinfo", PACKAGE, "reset")
for _ in range(4):
    swipe(True)
for _ in range(4):
    swipe(False)
rows.append(frames("inicio: desplazar"))
time.sleep(5); rows.append(frames("inicio: 5 s quieto"))
for tab in ("Catálogo", "Brav IA", "Cuenta"):
    tap_text(tab, wait=3)
    adb("shell", "dumpsys", "gfxinfo", PACKAGE, "reset")
    time.sleep(5); rows.append(frames(f"{tab}: 5 s quieto"))
    if tab == "Catálogo":
        for _ in range(4):
            swipe(True)
        rows.append(frames("catálogo: desplazar"))
starts = []
for _ in range(3):
    adb("shell", "am", "force-stop", PACKAGE); time.sleep(2)
    o = adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/com.example.MainActivity")
    m = re.search(r"TotalTime: (\d+)", o)
    if m:
        starts.append(int(m.group(1)))
    time.sleep(6)
rows.append(f"{LABEL} | arranque en frío: {starts} promedio={sum(starts) // max(1, len(starts))} ms")
mem = re.search(r"TOTAL PSS:\s+(\d+)", adb("shell", "dumpsys", "meminfo", PACKAGE)) or \
      re.search(r"TOTAL\s+(\d+)", adb("shell", "dumpsys", "meminfo", PACKAGE))
rows.append(f"{LABEL} | memoria (PSS): {int(mem.group(1)) // 1024 if mem else '?'} MB")
with open(OUT, "a", encoding="utf-8") as f:
    f.write("\n".join(rows) + "\n")
print("\n".join(rows))
