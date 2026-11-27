#!/usr/bin/env python3
"""Busca fotos candidatas para los productos sin foto, con consultas escritas a mano.

Lee .github/fotos/consultas.tsv (sku, consulta de marca para Open Facts, consulta genérica en
inglés) y busca en:
  - Open Food / Beauty / Products / Pet Food Facts (según el producto), sin filtrar por país;
  - Openverse (fotos con licencia abierta de Flickr, Wikimedia y otros);
  - Wikimedia Commons.
Guarda hasta 4 candidatas por producto en candidatas.json y arma hojas de contacto para elegir
a ojo: ninguna foto entra a la app sin esa revisión.
Uso: fotos_faltantes.py SALIDA consultas.tsv
"""
import io, json, sys, time, urllib.parse, urllib.request
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(sys.argv[1]); OUT.mkdir(parents=True, exist_ok=True)
UA = {"User-Agent": "BravoVaDemo/2.6 (github.com/Joypawbs/BravoVA-IA; demo app)"}
FAILS = {}

def get_json(url):
    svc = urllib.parse.urlparse(url).netloc
    if FAILS.get(svc, 0) >= 8:
        raise RuntimeError(svc + " desactivado por fallos seguidos")
    for k in range(3):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=20) as r:
                data = json.loads(r.read().decode("utf-8", "replace"))
            FAILS[svc] = 0
            return data
        except Exception as e:
            if k < 2 and getattr(e, "code", 503) in (429, 500, 502, 503, 504):
                time.sleep(4 * (k + 1)); continue
            FAILS[svc] = FAILS.get(svc, 0) + 1
            raise

def words(s):
    return {w for w in "".join(c if c.isalnum() else " " for c in s.lower()).split() if len(w) > 2}

def open_facts(host, query):
    q = {"search_terms": query, "json": 1, "page_size": 10, "search_simple": 1, "action": "process",
         "fields": "product_name,brands,image_front_url"}
    try:
        products = get_json(f"https://{host}/cgi/search.pl?" + urllib.parse.urlencode(q)).get("products", [])
    except Exception as e:
        print("  ", host, "falló:", e); return []
    want, out = words(query), []
    for p in products:
        url = p.get("image_front_url")
        if not url: continue
        brands = p.get("brands") or ""
        if isinstance(brands, list): brands = " ".join(brands)
        have = words((p.get("product_name") or "") + " " + brands)
        score = len(want & have)
        if score >= min(2, len(want)):
            out.append((score, url.replace(".full.jpg", ".400.jpg"), host.split(".")[1][4:] + ": " + (p.get("product_name") or "")[:34]))
    out.sort(key=lambda t: -t[0])
    return [(u, l) for _, u, l in out[:2]]

def openverse(query):
    q = {"q": query, "page_size": 6, "mature": "false"}
    try:
        res = get_json("https://api.openverse.org/v1/images/?" + urllib.parse.urlencode(q)).get("results", [])
    except Exception as e:
        print("   openverse falló:", e); return []
    return [(r.get("thumbnail") or r.get("url"), "Openverse: " + (r.get("title") or "")[:30]) for r in res if r.get("url")][:3]

def commons(query):
    q = {"action": "query", "format": "json", "generator": "search", "gsrnamespace": 6,
         "gsrsearch": query + " filetype:bitmap", "gsrlimit": 6, "prop": "imageinfo", "iiprop": "url|mime", "iiurlwidth": 400}
    try:
        data = get_json("https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode(q))
    except Exception as e:
        print("   commons falló:", e); return []
    pages = sorted(data.get("query", {}).get("pages", {}).values(), key=lambda p: p.get("index", 99))
    out = []
    for p in pages:
        ii = (p.get("imageinfo") or [{}])[0]
        if ii.get("mime") in ("image/jpeg", "image/png"):
            out.append((ii.get("thumburl") or ii.get("url"), "Commons: " + p.get("title", "")[5:36]))
    return out[:3]

HOSTS_FOOD = ["world.openfoodfacts.org", "world.openbeautyfacts.org", "world.openproductsfacts.org"]
HOSTS_PET = ["world.openpetfoodfacts.org", "world.openproductsfacts.org"]

rows = [l.split("\t") for l in Path(sys.argv[2]).read_text(encoding="utf-8").splitlines()[1:] if l.strip()]
print(len(rows), "productos")
cands = {}
for i, row in enumerate(rows):
    sku, off_q, generic_q = (row + ["", ""])[:3]
    c = []
    if off_q:
        for host in (HOSTS_PET if sku.startswith("ARC") else HOSTS_FOOD):
            c += open_facts(host, off_q)
            if len(c) >= 2: break
            time.sleep(0.4)
    c += openverse(generic_q or off_q)
    if len(c) < 4: c += commons(generic_q or off_q)
    seen, uniq = set(), []
    for u, l in c:
        if u and u not in seen: seen.add(u); uniq.append((u, l))
    cands[sku] = {"q": generic_q or off_q, "c": [{"url": u, "src": l} for u, l in uniq[:4]]}
    print(i, sku, len(uniq), flush=True)
    time.sleep(0.5)
(OUT / "candidatas.json").write_text(json.dumps(cands, ensure_ascii=False, indent=1), encoding="utf-8")

def thumb(url):
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=15) as r:
            im = Image.open(io.BytesIO(r.read())).convert("RGB")
        im.thumbnail((150, 150)); return im
    except Exception as e:
        print("descarga falló", url, e); return None

# Hojas: 12 productos por hoja; columna con sku + consulta y 4 candidatas numeradas 1..4
font = ImageFont.load_default()
ROW, W_LABEL, CELL = 165, 200, 160
skus = list(cands)
for sheet in range(0, len(skus), 12):
    chunk = skus[sheet:sheet + 12]
    img = Image.new("RGB", (W_LABEL + CELL * 4, ROW * len(chunk)), "white")
    d = ImageDraw.Draw(img)
    for r, sku in enumerate(chunk):
        y = r * ROW
        d.text((6, y + 6), sku, fill="red", font=font)
        words_q, lines, cur = cands[sku]["q"].split(), [], ""
        for w in words_q:
            if len(cur) + len(w) > 24: lines.append(cur); cur = w
            else: cur = (cur + " " + w).strip()
        lines.append(cur)
        for k, l in enumerate(lines[:5]): d.text((6, y + 22 + k * 13), l, fill="black", font=font)
        for k, cnd in enumerate(cands[sku]["c"]):
            t = thumb(cnd["url"]); time.sleep(0.1)
            x = W_LABEL + k * CELL
            if t: img.paste(t, (x + 5, y + 10))
            d.rectangle([x + 2, y + 2, x + 18, y + 16], fill="yellow"); d.text((x + 6, y + 3), str(k + 1), fill="black", font=font)
        d.line([0, y + ROW - 1, img.width, y + ROW - 1], fill="#999")
    img.save(OUT / f"hoja_{sheet // 12 + 1:02d}.jpg", quality=75)
print("listo")
