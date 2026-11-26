#!/usr/bin/env python3
"""Busca fotos candidatas para los productos del catálogo que no tienen foto (img = null).

Fuentes abiertas: Open Food Facts (productos empacados) y Wikimedia Commons (frescos,
genéricos). Por cada producto guarda hasta 4 candidatas y arma hojas de contacto para
revisarlas a ojo; ninguna foto entra a la app sin esa revisión.
Uso: buscar_fotos.py SALIDA catalogo1.kt [catalogo2.kt ...]
"""
import io, json, re, sys, time, unicodedata, urllib.error, urllib.parse, urllib.request
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(sys.argv[1]); OUT.mkdir(parents=True, exist_ok=True)
UA = {"User-Agent": "BravoVaDemo/2.3 (github.com/Joypawbs/BravoVA-IA; demo app)"}
STOP = {"de", "con", "y", "en", "la", "el", "los", "las", "para", "bravo", "lb", "oz", "gr", "g", "ml", "l",
        "und", "pack", "unidad", "unidades", "litros", "litro", "kg", "x", "segun", "peso", "gal", "galon"}
LINE = re.compile(r'p\("([A-Z]{3}-\d+)",\s*"([^"]+)",\s*\d+,\s*"([^"]+)".*,\s*null\)')

def norm(s):
    s = unicodedata.normalize("NFKD", s.lower())
    s = "".join(c for c in s if not unicodedata.combining(c))
    return [w for w in re.findall(r"[a-z]+", s) if w not in STOP and len(w) > 2]

def get_json(url, tries=4):
    """GET con reintentos: los servicios abiertos responden 429/503 cuando se les pide rápido."""
    for k in range(tries):
        try:
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.loads(r.read().decode("utf-8", "replace"))
        except urllib.error.HTTPError as e:
            if e.code in (429, 502, 503, 504) and k < tries - 1:
                time.sleep(4 * (k + 1)); continue
            raise

def off(name):
    terms = " ".join(norm(name)[:4])
    products = []
    try:
        q = {"q": terms, "page_size": 8, "fields": "product_name,brands,image_front_url"}
        data = get_json("https://search.openfoodfacts.org/search?" + urllib.parse.urlencode(q))
        products = data.get("hits", [])
        for p in products:
            if isinstance(p.get("brands"), list): p["brands"] = " ".join(p["brands"])
    except Exception as e:
        print("OFF (nuevo) falló", name, e)
    if not products:
        q = {"search_terms": terms, "json": 1, "page_size": 8, "search_simple": 1, "action": "process",
             "fields": "product_name,brands,image_front_url"}
        try:
            products = get_json("https://world.openfoodfacts.org/cgi/search.pl?" + urllib.parse.urlencode(q)).get("products", [])
        except Exception as e:
            print("OFF falló", name, e); return []
    want = set(norm(name)); out = []
    for p in products:
        url = p.get("image_front_url")
        if not url: continue
        have = set(norm((p.get("product_name") or "") + " " + (p.get("brands") or "")))
        score = len(want & have)
        if score >= min(2, len(want)):
            out.append((score, url.replace(".full.jpg", ".400.jpg"), "OFF: " + (p.get("product_name") or "")[:40]))
    out.sort(key=lambda t: -t[0])
    return [(u, l) for _, u, l in out[:2]]

def commons(name):
    terms = " ".join(norm(name)[:2])
    q = {"action": "query", "format": "json", "generator": "search", "gsrnamespace": 6, "gsrsearch": terms + " filetype:bitmap",
         "gsrlimit": 6, "prop": "imageinfo", "iiprop": "url|mime", "iiurlwidth": 400}
    try:
        data = get_json("https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode(q))
    except Exception as e:
        print("Commons falló", name, e); return []
    pages = sorted(data.get("query", {}).get("pages", {}).values(), key=lambda p: p.get("index", 99))
    out = []
    for p in pages:
        ii = (p.get("imageinfo") or [{}])[0]
        if ii.get("mime") not in ("image/jpeg", "image/png"): continue
        out.append((ii.get("thumburl") or ii.get("url"), "Commons: " + p.get("title", "")[5:45]))
    return out[:3]

def wikipedia(name):
    """Imagen principal del artículo de Wikipedia en español (ideal para frescos y genéricos)."""
    terms = " ".join(norm(name)[:2])
    q = {"action": "query", "format": "json", "generator": "search", "gsrsearch": terms, "gsrlimit": 2,
         "prop": "pageimages", "piprop": "thumbnail", "pithumbsize": 400}
    try:
        data = get_json("https://es.wikipedia.org/w/api.php?" + urllib.parse.urlencode(q))
    except Exception as e:
        print("Wikipedia falló", name, e); return []
    pages = sorted(data.get("query", {}).get("pages", {}).values(), key=lambda p: p.get("index", 99))
    return [(p["thumbnail"]["source"], "Wikipedia: " + p.get("title", "")[:40]) for p in pages if p.get("thumbnail")]

def thumb(url):
    try:
        req = urllib.request.Request(url, headers=UA)
        with urllib.request.urlopen(req, timeout=30) as r:
            im = Image.open(io.BytesIO(r.read())).convert("RGB")
        im.thumbnail((150, 150)); return im
    except Exception as e:
        print("descarga falló", url, e); return None

items = []
for f in sys.argv[2:]:
    for line in Path(f).read_text(encoding="utf-8").splitlines():
        m = LINE.search(line)
        if m: items.append(m.groups())
print(len(items), "productos sin foto")

cands = {}
for i, (sku, name, cat) in enumerate(items):
    c = off(name)
    if len(c) < 2: c += wikipedia(name)
    if len(c) < 3: c += commons(name)
    time.sleep(1.2)
    cands[sku] = {"name": name, "cat": cat, "c": [{"url": u, "src": l} for u, l in c[:4]]}
    if i % 20 == 0: print(i, sku, name, len(c))
(OUT / "candidatas.json").write_text(json.dumps(cands, ensure_ascii=False, indent=1), encoding="utf-8")

# Hojas: 12 productos por hoja; columna de nombre + 4 candidatas numeradas 1..4
font = ImageFont.load_default()
ROW, W_LABEL, CELL = 165, 230, 160
skus = list(cands)
for sheet in range(0, len(skus), 12):
    chunk = skus[sheet:sheet + 12]
    img = Image.new("RGB", (W_LABEL + CELL * 4, ROW * len(chunk)), "white")
    d = ImageDraw.Draw(img)
    for r, sku in enumerate(chunk):
        y = r * ROW
        info = cands[sku]
        words, lines, cur = info["name"].split(), [], ""
        for w in words:
            if len(cur) + len(w) > 28: lines.append(cur); cur = w
            else: cur = (cur + " " + w).strip()
        lines.append(cur)
        d.text((6, y + 6), sku, fill="red", font=font)
        for k, l in enumerate(lines[:6]): d.text((6, y + 22 + k * 13), l, fill="black", font=font)
        for k, cnd in enumerate(info["c"]):
            t = thumb(cnd["url"]); time.sleep(0.15)
            x = W_LABEL + k * CELL
            if t: img.paste(t, (x + 5, y + 10))
            d.rectangle([x + 2, y + 2, x + 18, y + 16], fill="yellow"); d.text((x + 6, y + 3), str(k + 1), fill="black", font=font)
        d.line([0, y + ROW - 1, img.width, y + ROW - 1], fill="#999")
    img.save(OUT / f"hoja_{sheet // 12 + 1:02d}.jpg", quality=72)
print("listo")
