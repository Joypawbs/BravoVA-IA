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
    adb("shell", "rm", "-f", "/sdcard/ui.xml")
    try:
        subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True, timeout=20)
    except subprocess.TimeoutExpired:
        return []
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


def screen_size():
    out = adb("shell", "wm", "size", capture=True) or ""
    m = re.search(r"(\d+)x(\d+)", out)
    return (int(m.group(1)), int(m.group(2))) if m else (1080, 2400)


def tap(labels, prefix=False, wait=2.0, fallback=None):
    """Toca por texto; si uiautomator no ve la app (animaciones sin fin), usa la posición relativa."""
    dismiss_system_dialogs()
    for text, desc, x, y in nodes():
        for label in labels:
            hit = (text.startswith(label) or desc.startswith(label)) if prefix else label in (text, desc)
            if hit:
                adb("shell", "input", "tap", str(x), str(y))
                print(f"[tour] tap '{label}' en ({x},{y})", flush=True)
                time.sleep(wait)
                return True
    if fallback:
        w, h = screen_size()
        x, y = int(w * fallback[0]), int(h * fallback[1])
        adb("shell", "input", "tap", str(x), str(y))
        print(f"[tour] tap por posición {fallback} para {labels}", flush=True)
        time.sleep(wait)
        return True
    print(f"[tour] no encontré {labels}", flush=True)
    return False


def swipe_down():
    adb("shell", "input", "swipe", "540", "800", "540", "1800", "350")
    time.sleep(1.5)


def swipe_up():
    adb("shell", "input", "swipe", "540", "1700", "540", "700", "350")
    time.sleep(1.5)


def main():
    # Registro de errores del sistema por si algo falla durante el recorrido.
    subprocess.Popen(["adb", "logcat", "-v", "time", "*:E"], stdout=open(OUT / "logcat_errores.txt", "w"),
                     stderr=subprocess.DEVNULL)
    # Evita que los avisos "X no responde" del emulador tapen la app.
    adb("shell", "settings", "put", "global", "hide_error_dialogs", "1")
    adb("shell", "cmd", "uimode", "night", "no")
    time.sleep(25)
    dismiss_system_dialogs()
    adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
    time.sleep(4); shot("splash")
    time.sleep(8); shot("inicio_sesion")
    tap(["Continuar como Invitado", "Invitado"], prefix=True, wait=6, fallback=(0.5, 0.77))
    shot("home")
    if tap(["Café Soluble"], prefix=True, wait=3):
        shot("detalle_producto")
        tap(["Volver"], wait=2, fallback=(0.06, 0.06))
    swipe_up(); shot("home_secciones")
    tap(["Agregar "], prefix=True, wait=1)
    tap(["Agregar "], prefix=True, wait=1)
    shot("home_con_carrito")
    # Al bajar se esconden las barras; subir un poco las vuelve a mostrar.
    swipe_down(); swipe_down()

    tabs = [("Catálogo", 0.3, "catalogo"), ("Bravísima", 0.1, "bravisima"),
            ("Brav IA", 0.7, "brav_ia"), ("Perfil", 0.9, "mi_cuenta")]
    for label, x, name in tabs:
        tap([label], wait=3, fallback=(x, 0.955))
        shot(name)

    tap(["Mis pedidos"], wait=3, fallback=(0.4, 0.115)); shot("mis_pedidos")
    tap(["Volver"], wait=2, fallback=(0.06, 0.06))
    tap(["Inicio"], wait=3, fallback=(0.5, 0.955))
    tap(["Ver carrito"], wait=3, fallback=(0.5, 0.865)); shot("carrito")
    for _ in range(4):
        swipe_up()
    shot("carrito_pago_y_entrega")
    swipe_up(); swipe_up(); shot("carrito_diaspora_y_total")
    tap(["Volver"], wait=2, fallback=(0.06, 0.06))

    # Modo oscuro del sistema: la app debe seguirlo sola.
    adb("shell", "cmd", "uimode", "night", "yes"); time.sleep(4)
    tap(["Inicio"], wait=3, fallback=(0.5, 0.955)); shot("home_oscuro")
    tap(["Catálogo"], wait=3, fallback=(0.3, 0.955)); shot("catalogo_oscuro")
    tap(["Perfil"], wait=3, fallback=(0.9, 0.955)); shot("mi_cuenta_oscuro")
    adb("shell", "cmd", "uimode", "night", "no")


if __name__ == "__main__":
    main()
