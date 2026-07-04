import os, sys, math
from PIL import Image, ImageDraw, ImageFont
try:
    import pillow_heif; pillow_heif.register_heif_opener()
except Exception: pass
sys.stdout.reconfigure(encoding="utf-8")
HERE=os.path.dirname(os.path.abspath(__file__))
REPO=os.path.abspath(os.path.join(HERE,"..","..",".."))
RAW=os.path.join(REPO,"Tour 360")
OUT=os.path.join(HERE,"_cu_chi_work")
def folder(p):
    for n in os.listdir(RAW):
        if n.startswith(p) and os.path.isdir(os.path.join(RAW,n)): return os.path.join(RAW,n)
HAO,PHAT=folder("H"),folder("P")
try: font=ImageFont.truetype("arial.ttf",26)
except Exception: font=ImageFont.load_default()

def sheet(items, name, cols, cellw, cellh):
    rows=math.ceil(len(items)/cols)
    W,H=cols*cellw, rows*(cellh+30)
    s=Image.new("RGB",(W,H),(18,18,26)); d=ImageDraw.Draw(s)
    for i,(path,lbl) in enumerate(items):
        try: im=Image.open(path).convert("RGB")
        except Exception: continue
        im.thumbnail((cellw,cellh),Image.LANCZOS)
        x=(i%cols)*cellw; y=(i//cols)*(cellh+30)
        s.paste(im,(x+(cellw-im.width)//2,y+30))
        d.text((x+5,y+3),lbl,fill=(255,220,120),font=font)
    s.save(os.path.join(OUT,name),quality=85); print("wrote",name)

# Hao around den plaza
hao=[f"IMG_{n}.JPG" for n in range(3385,3399) if os.path.exists(os.path.join(HAO,f"IMG_{n}.JPG"))]
sheet([(os.path.join(HAO,f),f[4:8]) for f in hao],"den_hao.jpg",4,440,220)
# Phat den + statue
phat=[f"IMG_{n}.HEIC" for n in range(3581,3590) if os.path.exists(os.path.join(PHAT,f"IMG_{n}.HEIC"))]
sheet([(os.path.join(PHAT,f),f[4:8]) for f in phat],"den_phat.jpg",3,460,345)
