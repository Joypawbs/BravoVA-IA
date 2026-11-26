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
        tm = re.search(r'text="([^"]*)"', node)
        dm = re.search(r'content-desc="([^"]*)"', node)
        text = tm.group(1) if tm else ""
        desc = dm.group(1) if dm else ""
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


def hide_keyboard():
    """Cierra el teclado solo si está abierto (Atrás sin teclado saldría de la pantalla)."""
    state = adb("shell", "dumpsys", "input_method", capture=True) or ""
    if "mInputShown=true" in state or "mIsInputViewShown=true" in state:
        adb("shell", "input", "keyevent", "4"); time.sleep(1)


def swipe_down():
    adb("shell", "input", "swipe", "540", "800", "540", "1800", "350")
    time.sleep(1.5)


def swipe_up():
    adb("shell", "input", "swipe", "540", "1700", "540", "700", "350")
    time.sleep(1.5)


PERF = []


def perf_reset():
    adb("shell", "dumpsys", "gfxinfo", PACKAGE, "reset")


def perf(label):
    """Anota cuántos cuadros salieron con tirón desde el último reinicio (dumpsys gfxinfo)."""
    out = adb("shell", "dumpsys", "gfxinfo", PACKAGE, capture=True) or ""
    def grab(key):
        for line in out.splitlines():
            if line.strip().startswith(key):
                return line.split(":", 1)[1].strip()
        return "?"
    row = f"{label}: cuadros={grab('Total frames rendered')} | con tirón={grab('Janky frames')} | " \
          f"p50={grab('50th percentile')} p90={grab('90th percentile')} p99={grab('99th percentile')}"
    print("[perf]", row)
    PERF.append(row)
    perf_reset()


def cold_starts(n=3):
    """Arranque en frío: tiempo hasta el primer cuadro (am start -W)."""
    times = []
    for _ in range(n):
        adb("shell", "am", "force-stop", PACKAGE); time.sleep(2)
        out = adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/com.example.MainActivity", capture=True) or ""
        for line in out.splitlines():
            if line.startswith("TotalTime"):
                times.append(int(line.split(":")[1]))
        time.sleep(6)
    row = f"arranque en frío (ms): {times} promedio={sum(times) // max(1, len(times))}"
    print("[perf]", row)
    PERF.append(row)


def relaunch():
    """Reinicia la app para seguir desde un estado limpio (sin teclado ni menús abiertos)."""
    adb("shell", "am", "force-stop", PACKAGE)
    time.sleep(1)
    adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
    time.sleep(10)
    tap(["Continuar como Invitado", "Invitado"], prefix=True, wait=3)  # solo si vuelve a pedir sesión
    time.sleep(8)


def main():
    # Registro de errores del sistema por si algo falla durante el recorrido.
    subprocess.Popen(["adb", "logcat", "-v", "time", "*:E"], stdout=open(OUT / "logcat_errores.txt", "w"),
                     stderr=subprocess.DEVNULL)
    # Mensajes de diagnóstico de la app (println → System.out)
    subprocess.Popen(["adb", "logcat", "-v", "time", "-s", "System.out:I"], stdout=open(OUT / "logcat_app.txt", "w"),
                     stderr=subprocess.DEVNULL)
    # Evita que los avisos "X no responde" del emulador tapen la app.
    adb("shell", "settings", "put", "global", "hide_error_dialogs", "1")
    adb("shell", "cmd", "uimode", "night", "no")
    # Con animaciones apagadas la app respeta "reducir movimiento" y no dibuja los efectos de los huevos
    for key in ("animator_duration_scale", "transition_animation_scale", "window_animation_scale"):
        print(key, adb("shell", "settings", "get", "global", key, capture=True))
    time.sleep(25)
    dismiss_system_dialogs()
    adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
    time.sleep(4); shot("splash")
    time.sleep(8); shot("bienvenida_tienda")
    # 2.0: bienvenida de la primera vez (tienda → sucursal → entrar)
    if tap(["Continuar"], wait=2, fallback=(0.5, 0.93)):
        shot("bienvenida_sucursal")
        tap(["Bravo Winston Churchill"], wait=1)
        tap(["Continuar", "Elegir después"], wait=3, fallback=(0.5, 0.93))
    time.sleep(2); shot("inicio_sesion")
    tap(["Continuar como Invitado", "Invitado"], prefix=True, wait=6, fallback=(0.5, 0.77))
    time.sleep(12)  # el emulador es lento: espera a que cargue el catálogo
    shot("home")
    w, h = screen_size()
    for _ in range(5):
        adb("shell", "input", "tap", str(int(w * 0.16)), str(int(h * 0.09)))
    time.sleep(2); shot("huellitas")
    relaunch()

    # Búsqueda: huevo de pascua con efecto y filtros nuevos
    if tap(["Buscar productos"], prefix=True, wait=1.5, fallback=(0.5, 0.145)):
        adb("shell", "input", "text", "spamton"); time.sleep(2.5); shot("efecto_spamton")
        adb("shell", "input", "keyevent", "KEYCODE_MOVE_END")
        for _ in range(8):
            adb("shell", "input", "keyevent", "KEYCODE_DEL")
        adb("shell", "input", "text", "jevil"); time.sleep(1.2); shot("efecto_jevil")
        adb("shell", "input", "keyevent", "KEYCODE_MOVE_END")
        for _ in range(6):
            adb("shell", "input", "keyevent", "KEYCODE_DEL")
        adb("shell", "input", "text", "arroz"); time.sleep(3); shot("busqueda_filtros")
        if tap(["Menor precio"], wait=2):
            shot("busqueda_menor_precio")
    relaunch()
    if tap(["Café Soluble"], prefix=True, wait=3):
        shot("detalle_producto")
        swipe_up(); shot("detalle_precio_por_unidad")
        tap(["Volver"], wait=2, fallback=(0.06, 0.06))
    perf_reset()
    for _ in range(3):
        swipe_up()
    for _ in range(3):
        swipe_down()
    perf("inicio: desplazar arriba y abajo")
    swipe_up(); shot("home_secciones")
    w, h = screen_size()
    adb("shell", "input", "swipe", str(int(w * 0.18)), str(int(h * 0.5)), str(int(w * 0.18)), str(int(h * 0.5)), "1000")
    time.sleep(2); shot("vista_rapida")
    # Cierra la vista rápida tocando fuera (atrás podría cerrar la app si no se abrió)
    adb("shell", "input", "tap", str(int(w * 0.5)), str(int(h * 0.12))); time.sleep(1.5)
    tap(["Agregar "], prefix=True, wait=1)
    tap(["Agregar "], prefix=True, wait=1)
    shot("home_con_carrito")
    # Al bajar se esconden las barras; subir un poco las vuelve a mostrar.
    swipe_down(); swipe_down()

    # 2.0: Inicio, Catálogo, Buscar, Brav IA y Cuenta (Bravísima está dentro de Cuenta)
    tabs = [("Catálogo", 0.3, "catalogo"), ("Brav IA", 0.7, "brav_ia"), ("Cuenta", 0.9, "mi_cuenta")]
    for label, x, name in tabs:
        tap([label], wait=3, fallback=(x, 0.955))
        shot(name)
        # Pantalla quieta 5 s: idealmente casi no se dibujan cuadros (las animaciones de fondo cuestan)
        perf_reset(); time.sleep(5); perf(f"{label}: 5 s sin tocar")
        if label == "Catálogo":
            for _ in range(3):
                swipe_up()
            perf("catálogo: desplazar")
    # Brav IA como invitado: responde en modo local, sin cuenta, con datos del catálogo y el recetario.
    # Enlace directo: más fiable que tocar la pestaña (la barra se esconde al desplazar).
    adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", "bravova://bravia", PACKAGE)
    time.sleep(4)
    for consulta, nombre in (("como hago un sancocho", "brav_ia_receta"), ("necesito arroz y habichuelas", "brav_ia_productos"), ("receta de paella valenciana", "brav_ia_receta_no_verificada")):
        if tap(["Pide una receta"], prefix=True, wait=1.5, fallback=(0.5, 0.9)):
            adb("shell", "input", "text", consulta.replace(" ", "%s")); time.sleep(1)
            adb("shell", "input", "keyevent", "66"); time.sleep(5)
            hide_keyboard()
            shot(nombre)
            if nombre == "brav_ia_receta":
                swipe_down(); shot("brav_ia_receta_ingredientes")
                swipe_down(); shot("brav_ia_receta_pasos")
                swipe_up(); swipe_up(); swipe_up()
    tap(["Cuenta"], wait=3, fallback=(0.9, 0.955))
    if tap(["Tarjeta Bravísima"], wait=3):
        shot("bravisima")
        # Código para la caja a pantalla completa (tocar cualquier parte lo cierra)
        if tap(["BRV-"], prefix=True, wait=2):
            time.sleep(1.5); shot("bravisima_codigo_caja")
            w, h = screen_size()
            adb("shell", "input", "tap", str(int(w * 0.5)), str(int(h * 0.85))); time.sleep(1.5)
        tap(["Volver"], wait=2, fallback=(0.06, 0.06))

    tap(["Mis pedidos"], wait=3, fallback=(0.4, 0.115)); shot("mis_pedidos")
    tap(["Volver"], wait=2, fallback=(0.06, 0.06))
    tap(["Inicio"], wait=3, fallback=(0.5, 0.955))
    tap(["Ver carrito"], wait=3, fallback=(0.5, 0.865)); shot("carrito")
    for _ in range(4):
        swipe_up()
    shot("carrito_pago_y_entrega")
    swipe_up(); swipe_up(); shot("carrito_diaspora_y_total")
    tap(["Volver"], wait=2, fallback=(0.06, 0.06))

    # Atajo del ícono / widget: un enlace bravova:// debe abrir su pantalla
    adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", "bravova://bravisima", PACKAGE)
    time.sleep(4); shot("atajo_bravisima")
    tap(["Volver"], wait=2, fallback=(0.06, 0.06))

    cold_starts()
    (OUT / "rendimiento.txt").write_text("\n".join(PERF) + "\n", encoding="utf-8")

    # Modo oscuro del sistema: la app debe seguirlo sola.
    adb("shell", "cmd", "uimode", "night", "yes"); time.sleep(4)
    tap(["Inicio"], wait=3, fallback=(0.5, 0.955)); shot("home_oscuro")
    tap(["Catálogo"], wait=3, fallback=(0.3, 0.955)); shot("catalogo_oscuro")
    tap(["Cuenta"], wait=3, fallback=(0.9, 0.955)); shot("mi_cuenta_oscuro")
    adb("shell", "cmd", "uimode", "night", "no")


if __name__ == "__main__":
    main()
