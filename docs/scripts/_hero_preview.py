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
def folder(p):
    for n in os.listdir(RAW):
        if n.startswith(p) and os.path.isdir(os.path.join(RAW, n)): return os.path.join(RAW, n)
HAO, PHAT = folder("H"), folder("P")
# candidate heroes (Hao) per stop
cands = ["IMG_3302.JPG","IMG_3303.JPG","IMG_3308.JPG","IMG_3309.JPG",
         "IMG_3314.JPG","IMG_3316.JPG","IMG_3382.JPG","IMG_3394.JPG",
         "IMG_3396.JPG","IMG_3360.JPG","IMG_3365.JPG","IMG_3410.JPG",
         "IMG_3429.JPG","IMG_3347.JPG"]
try: font = ImageFont.truetype("arial.ttf", 28)
except Exception: font = ImageFont.load_default()
cols=2; cell=900
rows=math.ceil(len(cands)/cols)
sheet=Image.new("RGB",(cols*cell, rows*(cell//2+34)),(18,18,26))
d=ImageDraw.Draw(sheet)
for i,name in enumerate(cands):
    im=Image.open(os.path.join(HAO,name)).convert("RGB")
    im.thumbnail((cell, cell//2), Image.LANCZOS)
    x=(i%cols)*cell; y=(i//cols)*(cell//2+34)
    sheet.paste(im,(x,y+34))
    d.text((x+6,y+4),name.replace("IMG_","").replace(".JPG",""),fill=(255,220,120),font=font)
sheet.save(os.path.join(OUT,"heroes.jpg"),quality=84)
print("wrote heroes.jpg")
# Phat intro board
for n in ["IMG_3577.HEIC"]:
    Image.open(os.path.join(PHAT,n)).convert("RGB").resize((1100,825)).save(os.path.join(OUT,"intro_board.jpg"),quality=85)
print("wrote intro_board.jpg")
