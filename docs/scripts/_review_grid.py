import os, sys, math
from PIL import Image, ImageDraw, ImageFont
try:
    import pillow_heif; pillow_heif.register_heif_opener()
except Exception: pass
sys.stdout.reconfigure(encoding="utf-8")

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
RAW = os.path.join(REPO, "Tour 360")
OUT = os.path.join(HERE, "_cu_chi_work")
os.makedirs(OUT, exist_ok=True)

def folder(p):
    for n in os.listdir(RAW):
        if n.startswith(p) and os.path.isdir(os.path.join(RAW, n)):
            return os.path.join(RAW, n)

HAO, PHAT = folder("H"), folder("P")

def grid(files, base, cols, cell=320, tag="hao"):
    try: font = ImageFont.truetype("arial.ttf", 22)
    except Exception: font = ImageFont.load_default()
    for pg in range(0, len(files), cols*cols):
        chunk = files[pg:pg+cols*cols]
        rows = math.ceil(len(chunk)/cols)
        W, H = cols*cell, rows*(cell+26)
        sheet = Image.new("RGB", (W, H), (20,20,28))
        d = ImageDraw.Draw(sheet)
        for i,(path,name) in enumerate(chunk):
            try:
                im = Image.open(path).convert("RGB")
            except Exception as e:
                continue
            im.thumbnail((cell, cell), Image.LANCZOS)
            x = (i%cols)*cell; y=(i//cols)*(cell+26)
            sheet.paste(im, (x + (cell-im.width)//2, y+26))
            d.text((x+4, y+2), name, fill=(255,220,120), font=font)
        o = os.path.join(OUT, f"{base}_{pg//(cols*cols)+1}.jpg")
        sheet.save(o, quality=80)
        print("wrote", o, len(chunk), "imgs")

# Hao panos in numeric order
hao = sorted([f for f in os.listdir(HAO) if f.lower().endswith(".jpg") and "Copy" not in f])
grid([(os.path.join(HAO,f), f.replace("IMG_","").replace(".JPG","")) for f in hao], "hao_grid", 6, 300)

# Phat HEIC in order (intro board etc.)
phat = sorted([f for f in os.listdir(PHAT) if f.lower().endswith(".heic")])
grid([(os.path.join(PHAT,f), f.replace("IMG_","").replace(".HEIC","")) for f in phat], "phat_grid", 6, 300)

# Map original + rotations
mp = [f for f in os.listdir(RAW) if f.lower().endswith(".jpg")][0]
m = Image.open(os.path.join(RAW, mp)).convert("RGB")
print("map size", m.size)
m.copy().resize((m.width//3, m.height//3)).save(os.path.join(OUT,"map_orig.jpg"), quality=85)
m.rotate(-90, expand=True).resize((m.height//3, m.width//3)).save(os.path.join(OUT,"map_rotCW.jpg"), quality=85)
m.rotate(90, expand=True).resize((m.height//3, m.width//3)).save(os.path.join(OUT,"map_rotCCW.jpg"), quality=85)
print("done")
