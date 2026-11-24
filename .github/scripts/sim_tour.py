"""Recorre la app BravoVa en el simulador de iOS y guarda capturas.

Usa idb para leer el árbol de accesibilidad y tocar elementos por su texto.
Si un texto no aparece, toca por coordenadas relativas (mismo diseño que Android).
"""
import json, os, shutil, subprocess, sys, time

OUT = sys.argv[1]
UDID = sys.argv[2]
os.makedirs(OUT, exist_ok=True)
log = open(os.path.join(OUT, "tour.log"), "w")

def run(*a, check=False):
    r = subprocess.run(list(a), capture_output=True, text=True)
    log.write(f"$ {' '.join(a)}\n{r.stdout[-2000:]}{r.stderr[-2000:]}\n"); log.flush()
    return r

def shot(name):
    run("xcrun", "simctl", "io", UDID, "screenshot", os.path.join(OUT, f"{name}.png"))
    print("captura", name, flush=True)

def tree():
    r = run("idb", "ui", "describe-all", "--udid", UDID, "--json")
    try:
        return json.loads(r.stdout)
    except Exception:
        return []

def screen_size():
    for e in tree():
        f = e.get("frame") or {}
        if f.get("width", 0) > 300 and f.get("height", 0) > 600:
            return f["width"], f["height"]
    return 402, 874

HAS_IDB = shutil.which("idb") is not None

def tap_xy(x, y):
    if not HAS_IDB:
        log.write("sin idb: no se puede tocar\n"); return
    run("idb", "ui", "tap", "--udid", UDID, str(int(x)), str(int(y)))

def tap_text(*texts, fallback=None):
    els = tree()
    for t in texts:
        for e in els:
            label = " ".join(str(e.get(k) or "") for k in ("AXLabel", "AXValue", "title"))
            if t.lower() in label.lower():
                f = e["frame"]
                tap_xy(f["x"] + f["width"] / 2, f["y"] + f["height"] / 2)
                log.write(f"tap '{t}' -> {f}\n")
                return True
    if fallback:
        W, H = screen_size()
        tap_xy(W * fallback[0], H * fallback[1])
        log.write(f"tap fallback {fallback} for {texts}\n")
        return True
    log.write(f"NO ENCONTRADO {texts}\n")
    return False

def wait(s):
    time.sleep(s)

wait(3); shot("01_splash")
wait(6); shot("02_inicio_sesion")
json.dump(tree(), open(os.path.join(OUT, "a11y_auth.json"), "w"), indent=1)
tap_text("Continuar como Invitado", "Invitado", fallback=(0.5, 0.86))
wait(4); shot("03_home")
json.dump(tree(), open(os.path.join(OUT, "a11y_home.json"), "w"), indent=1)
run("idb", "ui", "swipe", "--udid", UDID, "200", "650", "200", "250")
wait(2); shot("04_home_scroll")
DOCK_Y = 0.94
for i, (label, x, name) in enumerate([("Catálogos", 0.32, "05_catalogo"), ("Tarjeta", 0.14, "06_bravisima"),
                                       ("Buscar", 0.68, "07_brav_ia"), ("Perfil", 0.86, "08_mi_cuenta"),
                                       ("Home", 0.5, "09_home_final")]):
    tap_text(label, fallback=(x, DOCK_Y))
    wait(3); shot(name)
run("xcrun", "simctl", "ui", UDID, "appearance", "light")
wait(3); shot("10_home_modo_claro")
