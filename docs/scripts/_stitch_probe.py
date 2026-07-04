import os, sys, itertools
sys.stdout.reconfigure(encoding="utf-8")
from PIL import Image, ImageDraw
import numpy as np
import cv2
try:
    import pillow_heif; pillow_heif.register_heif_opener()
except Exception:
    pass

RAW = os.path.join("..", "..", "..", "Tour 360")
def fold(p):
    for n in os.listdir(RAW):
        if n.startswith(p) and os.path.isdir(os.path.join(RAW, n)):
            return os.path.join(RAW, n)
HAO, PHAT = fold("H"), fold("P")

def load(folder, num, ext="JPG"):
    return os.path.join(folder, f"IMG_{num}.{ext}")

# candidate consecutive groups per stop (author, number, ext)
GROUPS = {
    "bai-xe":     [(HAO, n, "JPG") for n in [3304, 3305, 3306, 3307]],
    "den":        [(PHAT, n, "HEIC") for n in [3585, 3587, 3588]],
    "bo-tu-lenh": [(HAO, n, "JPG") for n in [3307, 3308, 3309, 3310]],
    "khu-tb":     [(HAO, n, "JPG") for n in [3314, 3315, 3316, 3317, 3318]],
    "khu-uy":     [(HAO, n, "JPG") for n in [3409, 3410, 3411, 3412]],
    "nha-bd":     [(HAO, n, "JPG") for n in [3382, 3383, 3384]],
    "khu-th":     [(HAO, n, "JPG") for n in [3364, 3365, 3366, 3367]],
}

os.makedirs("_cu_chi_work/stitch", exist_ok=True)

def to_mat(path):
    im = Image.open(path).convert("RGB")
    w, h = im.size
    if w > 3000:
        im = im.resize((3000, round(h * 3000 / w)), Image.LANCZOS)
    return cv2.cvtColor(np.array(im), cv2.COLOR_RGB2BGR)

def try_stitch(mats):
    st = cv2.Stitcher_create(cv2.Stitcher_PANORAMA)
    st.setPanoConfidenceThresh(0.2)
    status, pano = st.stitch(mats)
    return status, pano

for name, grp in GROUPS.items():
    paths = [load(a, n, e) for (a, n, e) in grp if os.path.exists(load(a, n, e))]
    mats = [to_mat(p) for p in paths]
    best = None
    # try the full group, then trailing/leading subsets
    combos = [mats] + [mats[i:i+2] for i in range(len(mats)-1)] + [mats[i:i+3] for i in range(len(mats)-2)]
    labels = ["all"] + [f"{i}-{i+1}" for i in range(len(mats)-1)] + [f"{i}-{i+2}" for i in range(len(mats)-2)]
    for lbl, mc in zip(labels, combos):
        if len(mc) < 2:
            continue
        try:
            status, pano = try_stitch(mc)
        except Exception as e:
            print(f"{name:11s} {lbl:5s} EXC {e}")
            continue
        if status != 0:
            print(f"{name:11s} {lbl:5s} fail({status})")
            continue
        h, w = pano.shape[:2]
        ratio = w / h
        print(f"{name:11s} {lbl:5s} OK ratio={ratio:.2f} size={w}x{h}")
        if best is None or ratio > best[0]:
            best = (ratio, lbl, pano)
    if best:
        ratio, lbl, pano = best
        prev = cv2.cvtColor(pano, cv2.COLOR_BGR2RGB)
        Image.fromarray(prev).save(f"_cu_chi_work/stitch/{name}_{lbl}_{ratio:.1f}.jpg", quality=80)
        print(f"  -> BEST {name}: {lbl} ratio={ratio:.2f}")
print("done")
