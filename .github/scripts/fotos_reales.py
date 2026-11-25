#!/usr/bin/env python3
"""Busca en Open Food Facts (productos vendidos en República Dominicana) fotos reales
para los productos del catálogo que hoy usan una ilustración genérica o que son de marca.
Escribe fotos.json con {sku: {"url": ..., "nombre_off": ..., "puntaje": ...}}.
Solo acepta coincidencias con suficientes palabras en común para no poner la foto equivocada."""
import json, re, sys, time, unicodedata, urllib.parse, urllib.request
from pathlib import Path

SRC = Path(sys.argv[1])
OUT = Path(sys.argv[2])
GENERIC = ("themealdb.com", "dummyjson.com", "thecocktaildb.com")
STOP = {"de", "con", "y", "en", "la", "el", "para", "bravo", "lb", "oz", "gr", "g", "ml", "l", "und", "pack", "unidad", "unidades"}

def norm(s):
    s = unicodedata.normalize("NFKD", s.lower())
    s = "".join(c for c in s if not unicodedata.combining(c))
    return [w for w in re.findall(r"[a-z]+", s) if w not in STOP and len(w) > 2]

def search(terms, country=True):
    q = {"search_terms": terms, "json": 1, "page_size": 6, "search_simple": 1, "action": "process",
         "fields": "code,product_name,brands,image_front_url"}
    if country:
        q["tagtype_0"] = "countries"; q["tag_contains_0"] = "contains"; q["tag_0"] = "dominican-republic"
    url = "https://world.openfoodfacts.org/cgi/search.pl?" + urllib.parse.urlencode(q)
    req = urllib.request.Request(url, headers={"User-Agent": "BravoVaDemo/1.3 (github.com/Joypawbs/BravoVA-IA)"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r).get("products", [])

entries = re.findall(r'p\("([^"]+)", "([^"]+)", \d+, "[^"]*", "[^"]*", (true|false), "[^"]*", "[^"]*", "[^"]*", "([^"]+)"\)', SRC.read_text())
results = {}
for sku, name, is_bravo, img in entries:
    generic = any(h in img for h in GENERIC)
    if is_bravo == "true" and not generic:
        continue  # marca propia con foto real: se deja
    want = set(norm(name))
    if len(want) < 2:
        continue
    best = None
    for country in (True, False):
        try:
            prods = search(" ".join(norm(name)), country)
        except Exception as e:
            print("error", sku, e); prods = []
        for p in prods:
            if not p.get("image_front_url"):
                continue
            have = set(norm((p.get("product_name") or "") + " " + (p.get("brands") or "")))
            score = len(want & have) / len(want)
            if score >= 0.67 and (best is None or score > best["puntaje"]):
                best = {"url": p["image_front_url"], "nombre_off": p.get("product_name"), "puntaje": round(score, 2), "rd": country}
        if best:
            break
        time.sleep(0.7)
    if best:
        results[sku] = best
        print(f"{sku} {name} -> {best['nombre_off']} ({best['puntaje']})")
    time.sleep(0.7)

OUT.write_text(json.dumps(results, ensure_ascii=False, indent=1))
print("total", len(results))
