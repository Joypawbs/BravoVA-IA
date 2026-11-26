"""Hojas de contacto para revisar a ojo que cada foto corresponda a su producto.

Para qué: las fotos del catálogo se buscaron automáticamente por nombre y algunas no coinciden
(p. ej. "helado de pistacho" con una foto de almendras). Revisarlas en la app una por una es lento;
estas hojas ponen 30 fotos con su SKU y nombre en una sola imagen.

Uso: python3 hojas_fotos.py <salida> <catalogo.kt> [<catalogo.kt> ...]
"""
import concurrent.futures as cf
import io
import re
import sys
import textwrap
import urllib.request
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

PAT = re.compile(r'p\("([A-Z]+-\d+)",\s*"([^"]*)",\s*\d+,\s*"[^"]*",\s*"([^"]*)".*?"(https://[^"]+)"\)')
TILE, LABEL, COLS, ROWS = 220, 58, 6, 5


def fetch(url):
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "BravoVa-revision-fotos/1.0"})
        with urllib.request.urlopen(req, timeout=25) as r:
            im = Image.open(io.BytesIO(r.read())).convert("RGB")
        im.thumbnail((TILE - 10, TILE - 10))
        return im
    except Exception as e:  # noqa: BLE001
        print("ERROR", url, e)
        return None


def main():
    out = Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
    items = []
    for f in sys.argv[2:]:
        items += PAT.findall(Path(f).read_text(encoding="utf-8"))
    print(len(items), "productos")
    with cf.ThreadPoolExecutor(16) as ex:
        images = list(ex.map(lambda it: fetch(it[3]), items))
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 13)
    except OSError:
        font = ImageFont.load_default()
    per = COLS * ROWS
    broken = []
    for s in range(0, len(items), per):
        sheet = Image.new("RGB", (COLS * TILE, ROWS * (TILE + LABEL)), "white")
        d = ImageDraw.Draw(sheet)
        for k, (it, im) in enumerate(zip(items[s:s + per], images[s:s + per])):
            x, y = (k % COLS) * TILE, (k // COLS) * (TILE + LABEL)
            if im is None:
                broken.append(it[0])
                d.rectangle([x + 5, y + 5, x + TILE - 5, y + TILE - 5], outline="red", width=3)
                d.text((x + 20, y + TILE // 2), "SIN FOTO", fill="red", font=font)
            else:
                sheet.paste(im, (x + (TILE - im.width) // 2, y + (TILE - im.height) // 2))
            label = f"{it[0]} {it[1]} ({it[2]})"
            for i, line in enumerate(textwrap.wrap(label, 30)[:3]):
                d.text((x + 4, y + TILE + i * 17), line, fill="black", font=font)
            d.rectangle([x, y, x + TILE - 1, y + TILE + LABEL - 1], outline="#ccc")
        sheet.save(out / f"hoja_{s // per + 1:02d}.jpg", quality=82)
    (out / "sin_foto.txt").write_text("\n".join(broken), encoding="utf-8")
    print("listo;", len(broken), "fotos rotas")


if __name__ == "__main__":
    main()
