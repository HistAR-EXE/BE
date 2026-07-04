import os, sys
import numpy as np, cv2
from PIL import Image
try:
    import pillow_heif; pillow_heif.register_heif_opener()
except Exception: pass
sys.stdout.reconfigure(encoding="utf-8")
RAW=os.path.join('..','..','..','Tour 360'); OUT='_cu_chi_work'
def folder(p):
    for n in os.listdir(RAW):
        if n.startswith(p) and os.path.isdir(os.path.join(RAW,n)): return os.path.join(RAW,n)
PHAT=folder('P')

def load(n, target_w=3600):
    im=Image.open(os.path.join(PHAT,f'IMG_{n}.HEIC')).convert('RGB')
    w,h=im.size
    if w>target_w: im=im.resize((target_w, round(h*target_w/w)), Image.LANCZOS)
    return cv2.cvtColor(np.array(im), cv2.COLOR_RGB2BGR)

for combo in [[3585,3588],[3588,3585],[3585,3586,3587,3588],[3587,3588]]:
    imgs=[load(n) for n in combo]
    st=cv2.Stitcher_create(cv2.Stitcher_PANORAMA)
    st.setPanoConfidenceThresh(0.3)
    status, pano = st.stitch(imgs)
    if status==0:
        h,w=pano.shape[:2]
        print("OK", combo, "->", w,'x',h, 'ratio', round(w/h,2))
        cv2.imwrite(os.path.join(OUT,f'stitch_{"_".join(map(str,combo))}.jpg'), pano)
    else:
        print("FAIL", combo, "status", status)
