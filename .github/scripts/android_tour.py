#!/usr/bin/env python3
"""Recorrido automático de BravoVa en el emulador Android.

Busca los botones por su texto (o descripción de accesibilidad) con
`uiautomator dump`, los toca con `adb shell input tap` y guarda capturas.
Uso: android_tour.py <carpeta_de_capturas>
"""
import re
import subprocess
import sys
import time
from pathlib import Path

PACKAGE = "com.aistudio.bravova.rdshop"
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else "android-out")
OUT.mkdir(parents=True, exist_ok=True)
counter = 0


def adb(*args, capture=False):
    result = subprocess.run(["adb", *args], capture_output=True, text=True)
    return result.stdout if capture else None


def dismiss_system_dialogs():
    """Cierra avisos del sistema (p. ej. "Pixel Launcher isn't responding") que tapan la app."""
    for _ in range(3):
        current = nodes()
        if not any("isn't responding" in t or "no responde" in t for t, _, _, _ in current):
            return
        for text, desc, x, y in current:
            if text in ("Wait", "Esperar"):
                adb("shell", "input", "tap", str(x), str(y))
                print("[tour] cerré un aviso del sistema", flush=True)
                time.sleep(2)
                break


def shot(name):
    dismiss_system_dialogs()
    global counter
    counter += 1
    path = OUT / f"{counter:02d}_{name}.png"
    with open(path, "wb") as fh:
        fh.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True).stdout)
    print(f"[tour] captura {path.name}", flush=True)


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml", capture=True) or ""
    found = []
    for m in re.finditer(r'<node [^>]*>', xml):
        node = m.group(0)
        text = re.search(r'text="([^"]*)"', node).group(1)
        desc = re.search(r'content-desc="([^"]*)"', node).group(1)
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            found.append((text, desc, (x1 + x2) // 2, (y1 + y2) // 2))
    return found


def tap(labels, prefix=False, wait=2.0):
    """Toca el primer elemento cuyo texto o descripción coincide con alguna etiqueta."""
    dismiss_system_dialogs()
    for _ in range(3):
        for text, desc, x, y in nodes():
            for label in labels:
                hit = (text.startswith(label) or desc.startswith(label)) if prefix else label in (text, desc)
                if hit:
                    adb("shell", "input", "tap", str(x), str(y))
                    print(f"[tour] tap '{label}' en ({x},{y})", flush=True)
                    time.sleep(wait)
                    return True
        time.sleep(2)
    print(f"[tour] no encontré {labels}", flush=True)
    return False


def swipe_up():
    adb("shell", "input", "swipe", "540", "1700", "540", "700", "350")
    time.sleep(1.5)


def main():
    adb("shell", "cmd", "uimode", "night", "no")
    time.sleep(25)  # deja que el sistema termine de arrancar
    dismiss_system_dialogs()
    adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
    time.sleep(4); shot("splash")
    time.sleep(8); shot("inicio_sesion")
    tap(["Continuar como Invitado", "Invitado"], prefix=True, wait=5)
    shot("home")
    if tap(["Café Soluble"], prefix=True, wait=3):
        shot("detalle_producto")
        adb("shell", "input", "keyevent", "4"); time.sleep(2)
    swipe_up(); shot("home_secciones")
    tap(["Agregar "], prefix=True, wait=1)
    tap(["Agregar "], prefix=True, wait=1)
    shot("home_con_carrito")

    for label, name in [("Catálogo", "catalogo"), ("Tarjeta", "tarjeta_bravisima"),
                        ("Brav IA", "brav_ia"), ("Perfil", "mi_cuenta")]:
        tap([label], wait=3)
        shot(name)

    tap(["Mis pedidos"], wait=3); shot("mis_pedidos")
    adb("shell", "input", "keyevent", "4"); time.sleep(2)
    tap(["Inicio"], wait=3)
    tap(["Ver carrito"], wait=3); shot("carrito")
    adb("shell", "input", "keyevent", "4"); time.sleep(2)

    # Modo oscuro del sistema: la app debe seguirlo sola.
    adb("shell", "cmd", "uimode", "night", "yes"); time.sleep(4)
    tap(["Inicio"], wait=3); shot("home_oscuro")
    tap(["Catálogo"], wait=3); shot("catalogo_oscuro")
    adb("shell", "cmd", "uimode", "night", "no")

    (OUT / "ui_final.txt").write_text("\n".join(f"{t} | {d} | {x},{y}" for t, d, x, y in nodes()))


if __name__ == "__main__":
    main()
