"""
Cu Chi Tour 360 - build web assets + seed SQL + FE pins from real trip photos.

Single source of truth: the SCENES list below (12 stops along the Ben Duoc route,
chosen from the geotagged iPhone panoramas (Hao) + supplementary close-ups from
Phat (HEIC) and Vy (artifacts)).

Outputs (offline, does NOT touch app code):
    assets -> equirectangular hero panos  -> FE/public/media/cu-chi/panoramas/<slug>.jpg
              supplementary gallery photos -> FE/public/media/cu-chi/gallery/<slug>-N.jpg
    sql    -> BE/docs/database/2026-06-25_cu_chi_real_panoramas.sql
    pins   -> prints the cuChiIllustratedMapPins.ts pin array for copy/paste

Usage:
    python cu_chi_assets.py assets
    python cu_chi_assets.py sql
    python cu_chi_assets.py pins
"""

from __future__ import annotations

import argparse
import math
import os
import sys

from PIL import Image, ImageFilter, ImageEnhance

try:
    import pillow_heif

    pillow_heif.register_heif_opener()
except Exception:
    pass

sys.stdout.reconfigure(encoding="utf-8")

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
RAW_ROOT = os.path.join(REPO, "Tour 360")
FE_MEDIA = os.path.join(REPO, "FE", "public", "media", "cu-chi")
PANO_DIR = os.path.join(FE_MEDIA, "panoramas")
GALLERY_DIR = os.path.join(FE_MEDIA, "gallery")
SQL_OUT = os.path.join(REPO, "BE", "docs", "database", "2026-07-10_cu_chi_multi_panoramas.sql")

CU_CHI = "11111111-1111-1111-1111-111111111111"
U = "22222222-2222-2222-2222-22222222222"  # + last hex digit


def raw(prefix: str) -> str:
    for name in os.listdir(RAW_ROOT):
        if name.startswith(prefix) and os.path.isdir(os.path.join(RAW_ROOT, name)):
            return os.path.join(RAW_ROOT, name)
    raise FileNotFoundError(prefix)


HAO = raw("H")
PHAT = raw("P")
VY = raw("V")


def src(author: str, name: str) -> str:
    base = {"hao": HAO, "phat": PHAT, "vy": VY}[author]
    return os.path.join(base, name)


# ---------------------------------------------------------------------------
# 12 scenes along the Ben Duoc walking route (narrative order)
# ---------------------------------------------------------------------------
def S(order, uuid_last, slug, title, desc, hero, xpct, ypct, supp):
    return dict(
        order=order, uuid=U + uuid_last, slug=slug, title=title, desc=desc,
        hero=hero, xpct=xpct, ypct=ypct, supp=supp,
    )


# Lo trinh thuc te cua doan (theo loi ke nguoi dung), khop ten vung tren ban do
# giay da xoay NGANG. Toa do xPct/yPct tinh theo anh landscape so-do-ben-duoc.webp.
SCENES = [
    S(1, "1", "bai-xe-so-1", "Bãi gửi xe gắn máy số 1",
      "Điểm bắt đầu hành trình — bãi gửi xe gắn máy số 1, ngay cạnh bảng giới thiệu tổng quan Khu di tích lịch sử Địa đạo Củ Chi (Bến Dược).",
      ("hao", "IMG_3302.JPG"), 69.0, 21.0,
      [("phat", "IMG_3577.HEIC", "Bảng giới thiệu khu di tích", "Bảng tổng quan các điểm tham quan và dịch vụ vui chơi của Khu di tích Địa đạo Củ Chi."),
       ("vy", "DSCF9639.JPG", "Vòm tre lối vào", "Đường vòm tre rợp bóng dẫn vào khu di tích.")]),

    S(2, "2", "den-ben-duoc", "Đền tưởng niệm Liệt sĩ Bến Dược",
      "Đền Bến Dược thờ các anh hùng liệt sĩ đã hy sinh trên vùng đất Sài Gòn — Chợ Lớn — Gia Định; điểm dừng đầu tiên sau khi vào khu di tích.",
      ("phat", "IMG_3588.HEIC"), 59.0, 32.0,
      [("phat", "IMG_3582.HEIC", "Đền Bến Dược", "Kiến trúc đền tưởng niệm mái cong truyền thống."),
       ("phat", "IMG_3589.HEIC", "Lư hương & bàn thờ", "Lư hương đồng và bàn thờ trong chính điện đền Bến Dược."),
       ("phat", "IMG_3585.HEIC", "Sân hành lễ", "Quảng trường rộng trước cổng đền — nơi tổ chức lễ tưởng niệm.")]),

    S(3, "4", "khu-trung-bay", "Khu trưng bày",
      "Khu trưng bày khí tài ngoài trời: xe tăng, xe thiết giáp M113, lựu pháo 105mm và máy bay thu được sau chiến tranh.",
      ("hao", "IMG_3316.JPG"), 50.0, 43.0,
      [("phat", "IMG_3620.HEIC", "Xe tăng", "Xe tăng trưng bày dưới tán rừng."),
       ("phat", "IMG_3624.HEIC", "Xe thiết giáp M113", "Xe thiết giáp M113 kèm bảng thông tin."),
       ("phat", "IMG_3637.HEIC", "Lựu pháo 105mm", "Dàn pháo trưng bày dưới mái lưới ngụy trang."),
       ("vy", "DSCF9646.JPG", "Máy bay C-130", "Máy bay vận tải C-130 trưng bày ngoài trời.")]),

    S(4, "3", "bo-tu-lenh", "Căn cứ Bộ Tư lệnh Quân khu Sài Gòn - Gia Định",
      "Nhà trưng bày tái hiện căn cứ Bộ Tư lệnh: súng bộ binh, bom đạn thu được, bàn thờ và phòng làm việc của ban chỉ huy.",
      ("hao", "IMG_3308.JPG"), 54.0, 57.0,
      [("phat", "IMG_3595.HEIC", "Kho bom & đạn", "Bom, đạn thu được xếp trong nhà trưng bày căn cứ."),
       ("phat", "IMG_3610.HEIC", "Tủ trưng bày súng", "Bộ sưu tập súng bộ binh trong tủ kính."),
       ("phat", "IMG_3651.HEIC", "Phòng làm việc chỉ huy", "Tái hiện phòng làm việc và bàn thờ trong căn cứ chỉ huy.")]),

    S(5, "5", "khu-uy", "Căn cứ Khu ủy Quân khu Sài Gòn - Gia Định",
      "Theo lối đi bên hông Bộ Tư lệnh, du khách vào căn cứ Khu ủy Quân khu Sài Gòn - Gia Định nằm sâu trong rừng.",
      ("hao", "IMG_3410.JPG"), 20.0, 82.0,
      [("phat", "IMG_3649.HEIC", "Phòng họp Khu ủy", "Tái hiện phòng họp của Khu ủy trong căn cứ."),
       ("phat", "IMG_3650.HEIC", "Gian sinh hoạt căn cứ", "Không gian sinh hoạt và làm việc tái hiện trong rừng.")]),

    S(6, "6", "nha-bieu-dien", "Nhà biểu diễn Sa bàn, Phim 3D",
      "Nhà biểu diễn với sa bàn, phim tư liệu và mô hình 3D mô phỏng hệ thống địa đạo nhiều tầng.",
      ("hao", "IMG_3382.JPG"), 30.0, 35.0,
      [("vy", "DSCF9663.JPG", "Sa bàn thuyết minh", "Hướng dẫn viên thuyết minh trên sa bàn khu địa đạo."),
       ("vy", "DSCF9665.JPG", "Mô hình địa đạo cắt lớp", "Mô hình cắt lớp thể hiện các tầng địa đạo dưới lòng đất.")]),

    S(7, "7", "khu-tai-hien", "Khu tái hiện Vùng Giải phóng",
      "Điểm cuối hành trình — khu tái hiện vùng giải phóng giữa rừng: nhà tranh, bếp Hoàng Cầm, công binh xưởng, hầm và dấu tích hố bom B52.",
      ("hao", "IMG_3365.JPG"), 21.0, 41.0,
      [("vy", "DSCF9689.JPG", "Bếp Hoàng Cầm", "Bếp Hoàng Cầm tản khói để tránh máy bay phát hiện."),
       ("vy", "DSCF9676.JPG", "Hầm giải phẫu", "Hầm giải phẫu cứu chữa thương binh."),
       ("vy", "DSCF9700.JPG", "Công binh xưởng", "Xưởng quân giới chế tạo vũ khí trong lòng đất."),
       ("phat", "IMG_3628.HEIC", "Hố bom B52", "Dấu tích hố bom B52 còn lưu lại trên mặt đất.")]),
]

# panorama cu (uuid_last) toi tao o seed truoc nhung khong con dung trong tuyen 7 diem
ORPHAN_UUID_LAST = ["8", "9", "a", "b", "c"]

# Cac diem co nhieu anh pano chong lan -> GHEP (stitch) thanh 360 day du hon.
# slug -> list (author, filename) theo thu tu ghep.
STITCH = {
    "bai-xe-so-1":   [("hao", "IMG_3304.JPG"), ("hao", "IMG_3305.JPG")],
    "den-ben-duoc":  [("phat", "IMG_3587.HEIC"), ("phat", "IMG_3588.HEIC")],
    "bo-tu-lenh":    [("hao", "IMG_3309.JPG"), ("hao", "IMG_3310.JPG")],
    "khu-trung-bay": [("hao", "IMG_3315.JPG"), ("hao", "IMG_3316.JPG")],
    "khu-uy":        [("hao", "IMG_3409.JPG"), ("hao", "IMG_3410.JPG")],
    "nha-bieu-dien": [("hao", "IMG_3382.JPG"), ("hao", "IMG_3383.JPG")],
    "khu-tai-hien":  [("hao", "IMG_3366.JPG"), ("hao", "IMG_3367.JPG")],
}


# ---------------------------------------------------------------------------
# Image processing
# ---------------------------------------------------------------------------
FULL_W = 5400
FULL_H = 2700


def haov_for(ratio: float) -> int:
    """Horizontal coverage (deg) implied by the pano aspect ratio."""
    if ratio >= 3.3:
        return 360
    if ratio >= 2.8:
        return 330
    if ratio >= 2.2:
        return 280
    return 230


def _trim_black(im: Image.Image) -> Image.Image:
    """Crop the irregular near-black border an iPhone pano leaves on its edges."""
    try:
        import numpy as np
    except Exception:
        return im
    arr = np.asarray(im)
    black = arr.sum(2) <= 26
    h, w = black.shape
    t, b, l, r = 0, h, 0, w
    # peel any edge row/col that still has a meaningful black fraction (corner wedges)
    for _ in range(max(h, w)):
        changed = False
        if t < b and black[t, l:r].mean() > 0.06:
            t += 1; changed = True
        if b > t and black[b - 1, l:r].mean() > 0.06:
            b -= 1; changed = True
        if l < r and black[t:b, l].mean() > 0.06:
            l += 1; changed = True
        if r > l and black[t:b, r - 1].mean() > 0.06:
            r -= 1; changed = True
        if not changed:
            break
    if (b - t) > im.height * 0.45 and (r - l) > im.width * 0.45:
        return im.crop((l, t, r, b))
    return im


def _fill_black_vert(im: Image.Image) -> Image.Image:
    """Replace residual near-black edge wedges by clamping the nearest real pixel
    vertically (sky pulled up, ground pulled down) so no black corners survive."""
    try:
        import numpy as np
    except Exception:
        return im
    arr = np.asarray(im).copy()
    h, w, _ = arr.shape
    black = arr.sum(2) <= 28
    if not black.any():
        return im
    valid = ~black
    cols = np.where(valid.any(0))[0]
    first = valid.argmax(0)
    last = h - 1 - valid[::-1].argmax(0)
    for c in cols:
        t = first[c]
        b = last[c]
        if t > 0:
            arr[:t, c] = arr[t, c]
        if b < h - 1:
            arr[b + 1:, c] = arr[b, c]
    return Image.fromarray(arr)


def _extend_vertical(strip_r: Image.Image, y: int, full_h: int) -> Image.Image:
    """Grow a strip to full sphere height by clamping its top/bottom edge colours
    (sky keeps going up, ground keeps going down) + blur so it reads natural."""
    sw, sh = strip_r.size
    ext = Image.new("RGB", (sw, full_h))
    ext.paste(strip_r, (0, y))
    if y > 0:
        top = strip_r.crop((0, 0, sw, 3)).resize((sw, y), Image.LANCZOS)
        top = top.filter(ImageFilter.GaussianBlur(max(6, y // 12)))
        ext.paste(top, (0, 0))
    below = full_h - (y + sh)
    if below > 0:
        bot = strip_r.crop((0, sh - 3, sw, sh)).resize((sw, below), Image.LANCZOS)
        bot = bot.filter(ImageFilter.GaussianBlur(max(6, below // 12)))
        ext.paste(bot, (0, y + sh))
    return ext


def _extend_horizontal(ext: Image.Image, x: int, full_w: int) -> Image.Image:
    """Fill the gap behind the viewer with ONE continuous band that wraps from the
    strip's right edge, around the back, to its left edge. Because it is a single
    cross-faded + blurred gradient there is no hard seam anywhere behind you."""
    try:
        import numpy as np
    except Exception:
        np = None
    sw, h = ext.size
    canvas = Image.new("RGB", (full_w, h))
    canvas.paste(ext, (x, 0))
    gap = full_w - sw
    if gap <= 0:
        return canvas
    edge = max(2, sw // 90)
    right_col = ext.crop((sw - edge, 0, sw, h)).resize((gap, h), Image.LANCZOS)
    left_col = ext.crop((0, 0, edge, h)).resize((gap, h), Image.LANCZOS)
    if np is not None:
        r = np.asarray(right_col, dtype=np.float32)
        l = np.asarray(left_col, dtype=np.float32)
        t = np.linspace(0.0, 1.0, gap, dtype=np.float32)[None, :, None]
        band = Image.fromarray((r * (1 - t) + l * t).astype("uint8"))
    else:
        band = right_col
    band = band.filter(ImageFilter.GaussianBlur(max(18, gap // 6)))
    band = ImageEnhance.Brightness(band).enhance(0.92)
    len_a = full_w - (x + sw)  # region to the right of the strip
    if len_a > 0:
        canvas.paste(band.crop((0, 0, len_a, h)), (x + sw, 0))
    if x > 0:
        canvas.paste(band.crop((gap - x, 0, gap, h)), (0, 0))
    return canvas


def _feather_into(canvas: Image.Image, strip_r: Image.Image, x: int, y: int) -> None:
    """Alpha-blend the sharp strip back over the fill with feathered edges so the
    transition into the extended fill is invisible."""
    sw, sh = strip_r.size
    mask = Image.new("L", (sw, sh), 255)
    px = mask.load()
    fx = max(10, sw // 22)
    fy = max(10, sh // 10)
    for i in range(fx):
        a = int(255 * i / fx)
        for j in range(sh):
            if a < px[i, j]:
                px[i, j] = a
            if a < px[sw - 1 - i, j]:
                px[sw - 1 - i, j] = a
    for j in range(fy):
        a = int(255 * j / fy)
        for i in range(sw):
            if a < px[i, j]:
                px[i, j] = a
            if a < px[i, sh - 1 - j]:
                px[i, sh - 1 - j] = a
    canvas.paste(strip_r, (x, y), mask)


def equirect_from(strip: Image.Image, haov: int | None = None) -> Image.Image:
    """Map a partial cylindrical pano into a 2:1 equirectangular frame, filling the
    sphere by natural edge-extension (sky up / ground down / hazy sides)."""
    strip = strip.convert("RGB")
    strip = _trim_black(strip)
    strip = _fill_black_vert(strip)
    w, h = strip.size
    ratio = w / h
    if haov is None:
        haov = haov_for(ratio)
    vfov = haov / ratio  # degrees of vertical FOV
    sh = min(FULL_H, round(FULL_H * vfov / 180.0))

    if haov >= 356:
        # Full wrap: stretch content to cover the whole circle (no blurred gap),
        # then cross-fade the back where the strip's two ends meet so the seam
        # melts away instead of leaving a hard line or a blurred band.
        return _full_wrap(strip, sh)

    sw = min(FULL_W, round(FULL_W * haov / 360.0))
    strip_r = strip.resize((sw, sh), Image.LANCZOS)
    x = (FULL_W - sw) // 2
    y = (FULL_H - sh) // 2
    ext = _extend_vertical(strip_r, y, FULL_H)
    canvas = _extend_horizontal(ext, x, FULL_W)
    _feather_into(canvas, strip_r, x, y)
    return canvas


def _full_wrap(strip: Image.Image, sh: int, blend_frac: float = 0.02) -> Image.Image:
    """Place an (already ~360) strip across the full width and feather only a thin
    band at the wrap so the two ends meet invisibly. No fat blurred gap."""
    try:
        import numpy as np
    except Exception:
        np = None
    sh = min(FULL_H, sh)
    strip_r = strip.resize((FULL_W, sh), Image.LANCZOS)
    y = (FULL_H - sh) // 2
    ext = _extend_vertical(strip_r, y, FULL_H)  # width == FULL_W, full height
    if np is None:
        return ext
    a = np.asarray(ext).astype(np.float32)
    b = max(10, round(FULL_W * blend_frac))
    idx = np.arange(b)
    t = ((idx + 1) / b).astype(np.float32)[None, :, None]
    right = a[:, FULL_W - b:FULL_W, :]
    left = a[:, 0:b, :][:, ::-1, :]
    a[:, FULL_W - b:FULL_W, :] = right * (1 - t) + left * t
    return Image.fromarray(a.astype("uint8"))


def make_equirect(strip: Image.Image) -> Image.Image:
    return equirect_from(strip)


def equalize_exposure_strips(mats):
    """CLAHE on L channel to reduce brightness mismatch before stitching."""
    import cv2

    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    out = []
    for mat in mats:
        lab = cv2.cvtColor(mat, cv2.COLOR_BGR2LAB)
        l, a, b = cv2.split(lab)
        l = clahe.apply(l)
        out.append(cv2.cvtColor(cv2.merge([l, a, b]), cv2.COLOR_LAB2BGR))
    return out


def refine_vertical_seam(arr, seam_width: int = 120):
    """Feather-blend the strongest vertical seam near the panorama centre."""
    import numpy as np

    rgb = np.asarray(arr, dtype=np.float32)
    if rgb.ndim != 3 or rgb.shape[1] < seam_width * 2:
        return arr
    h, w, _ = rgb.shape
    gray = rgb.mean(axis=2)
    gx = np.abs(np.diff(gray, axis=1))
    lo, hi = int(w * 0.2), int(w * 0.8)
    if hi <= lo:
        return arr
    seam_x = lo + int(np.argmax(gx[:, lo:hi].mean(axis=0)))
    half = seam_width // 2
    x0, x1 = seam_x - half, seam_x + half
    out = rgb.copy()
    for x in range(max(0, x0), min(w, x1)):
        t = (x - x0) / max(1, (x1 - x0 - 1))
        t = t * t * (3.0 - 2.0 * t)
        src_l = max(0, min(w - 1, seam_x - half + int((1.0 - t) * half)))
        src_r = max(0, min(w - 1, seam_x + int(t * half)))
        out[:, x] = rgb[:, src_l] * (1.0 - t) + rgb[:, src_r] * t
    return out.astype(np.uint8)


def fill_nadir(img: Image.Image, pole_frac: float = 0.07) -> Image.Image:
    """Fill the equirectangular nadir disc using ground-ring content instead of gray blur."""
    import numpy as np

    arr = np.asarray(img.convert("RGB")).copy().astype(np.float32)
    h, w, _ = arr.shape
    pole_h = max(12, int(h * pole_frac))
    nadir_y0 = h - pole_h
    source_row = max(0, nadir_y0 - 4)
    cy = w // 2

    for y in range(nadir_y0, h):
        frac = (y - nadir_y0 + 1) / pole_h
        row = np.zeros((w, 3), dtype=np.float32)
        for x in range(w):
            dist = abs(x - cy) / max(1.0, w / 2.0)
            pull = frac * (1.0 - dist * 0.45)
            sx = int(cy + (x - cy) * (1.0 - pull * 0.35))
            sx = max(0, min(w - 1, sx))
            row[x] = arr[source_row, sx]
        row *= 1.0 - frac * 0.12
        alpha = frac * frac
        arr[y] = arr[y] * (1.0 - alpha) + row * alpha

    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))


def apply_stitch_post_wide(pano_rgb, stitch_post: dict):
    """Optional post-process on stitched wide RGB before equirect mapping."""
    if stitch_post.get("refineSeam", False):
        width = int(stitch_post.get("seamWidth", 120))
        pano_rgb = refine_vertical_seam(pano_rgb, seam_width=width)
    return pano_rgb


def apply_stitch_post_equirect(img: Image.Image, stitch_post: dict) -> Image.Image:
    """Optional post-process on final equirectangular output."""
    if stitch_post.get("fillNadir", False):
        frac = float(stitch_post.get("nadirPoleFrac", 0.07))
        img = fill_nadir(img, pole_frac=frac)
    return img


def _crop_black(arr):
    """Trim irregular black borders left by the stitcher."""
    import numpy as np

    lit = arr.sum(2) > 18
    rmean = lit.mean(1)
    cmean = lit.mean(0)
    rows = np.where(rmean > 0.55)[0]
    cols = np.where(cmean > 0.55)[0]
    if len(rows) and len(cols):
        return arr[rows[0]: rows[-1] + 1, cols[0]: cols[-1] + 1]
    return arr


def stitch_panos(sources) -> Image.Image:
    """Stitch overlapping iPhone panoramas into one wide (~360deg) panorama."""
    import cv2
    import numpy as np

    mats = []
    for au, name in sources:
        im = Image.open(src(au, name)).convert("RGB")
        w, h = im.size
        if w > 3000:
            im = im.resize((3000, round(h * 3000 / w)), Image.LANCZOS)
        mats.append(cv2.cvtColor(np.array(im), cv2.COLOR_RGB2BGR))
    st = cv2.Stitcher_create(cv2.Stitcher_PANORAMA)
    st.setPanoConfidenceThresh(0.2)
    status, pano = st.stitch(mats)
    if status != 0:
        raise RuntimeError(f"stitch failed (status {status})")
    pano = _crop_black(cv2.cvtColor(pano, cv2.COLOR_BGR2RGB))
    return Image.fromarray(pano)


VFOV_EST = 60.0  # assumed vertical FOV of an iPhone pano strip (deg)


def make_equirect_wide(img: Image.Image) -> Image.Image:
    """Map a stitched pano. If it over-covers 360 (a landmark is duplicated at both
    ends) crop off the duplicated arc so it becomes exactly one seamless loop; if it
    under-covers, keep its true angular width and softly fill only the real gap.
    Either way: no doubled landmark, no fat blurred band."""
    img = img.convert("RGB")
    img = _trim_black(img)
    img = _fill_black_vert(img)
    ratio = img.width / img.height
    deg = ratio * VFOV_EST  # estimated horizontal coverage
    if deg > 380:
        # over-360: remove the duplicated extra arc -> exactly one 360 loop
        o = round((deg - 360.0) / deg * img.width)
        img = img.crop((o, 0, img.width, img.height))
        ratio = img.width / img.height
        sh = min(FULL_H, round(FULL_H * (360.0 / ratio) / 180.0))
        return _full_wrap(img, sh, blend_frac=0.015)
    # under 360: place at real coverage, soft-fill only the genuine gap behind
    haov = int(min(350, max(220, round(deg))))
    return equirect_from(img, haov=haov)


def cmd_assets(_a):
    os.makedirs(PANO_DIR, exist_ok=True)
    os.makedirs(GALLERY_DIR, exist_ok=True)
    for sc in SCENES:
        # hero -> equirectangular pano (ghep 360 neu co cau hinh STITCH)
        stitch_src = STITCH.get(sc["slug"])
        if stitch_src:
            wide = stitch_panos(stitch_src)
            eq = make_equirect_wide(wide)
            label = "+".join(n for _a, n in stitch_src)
            print(f"pano  {sc['slug']:20s} <- STITCH {label} {eq.size}")
        else:
            strip = Image.open(src(*sc["hero"]))
            eq = make_equirect(strip)
            print(f"pano  {sc['slug']:20s} <- {sc['hero'][1]:18s} {eq.size}")
        out = os.path.join(PANO_DIR, sc["slug"] + ".jpg")
        eq.save(out, quality=82, optimize=True)
        # supplementary -> gallery
        for i, (au, name, _t, _d) in enumerate(sc["supp"], 1):
            im = Image.open(src(au, name)).convert("RGB")
            im.thumbnail((1600, 1600), Image.LANCZOS)
            gout = os.path.join(GALLERY_DIR, f"{sc['slug']}-{i}.jpg")
            im.save(gout, quality=82, optimize=True)
            print(f"  supp {sc['slug']}-{i} <- {au}/{name} {im.size}")
    print("done assets")


# ---------------------------------------------------------------------------
# Seed SQL
# ---------------------------------------------------------------------------
EXISTING = {"2", "1", "3", "4", "5"}  # uuid_last already in DB


def q(s: str) -> str:
    return s.replace("'", "''")


def cmd_sql(_a):
    L = []
    L.append("SET client_encoding TO 'UTF8';")
    L.append("-- Cu Chi: 12 panorama 360 thuc te tu chuyen di (Ben Duoc) + anh bo sung")
    L.append("-- Sinh tu BE/docs/scripts/cu_chi_assets.py — DATA ONLY (khong doi schema).")
    L.append("-- FE static: /media/cu-chi/panoramas/<slug>.jpg, /media/cu-chi/gallery/<slug>-N.jpg")
    L.append("")
    L.append("-- 0) Don pano thua tu seed truoc (tuyen gio chi con 7 diem) — data only")
    orphan_ids = ", ".join(f"'{U + x}'" for x in ORPHAN_UUID_LAST)
    L.append(f"DELETE FROM hotspots WHERE panorama_id IN ({orphan_ids});")
    L.append(f"DELETE FROM hotspots WHERE type = 'scene' AND content_ref IN ({orphan_ids});")
    L.append(f"DELETE FROM panoramas WHERE id IN ({orphan_ids});")
    L.append("")
    L.append("-- 1) Panorama: cap nhat 5 scene cu + them 2 scene moi (uuid_last 6,7)")
    for sc in SCENES:
        img = f"/media/cu-chi/panoramas/{sc['slug']}.jpg"
        if sc["uuid"][-1] in EXISTING:
            L.append(
                f"UPDATE panoramas SET image_url = '{img}', title = '{q(sc['title'])}' "
                f"WHERE id = '{sc['uuid']}';"
            )
        else:
            L.append(
                "INSERT INTO panoramas (id, location_id, image_url, title) VALUES "
                f"('{sc['uuid']}', '{CU_CHI}', '{img}', '{q(sc['title'])}') "
                "ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title;"
            )
    L.append("")
    L.append("-- 2) Scene-link hotspots noi lo trinh (1<->2<->...<->12)")
    all_ids = ", ".join(f"'{s['uuid']}'" for s in SCENES)
    L.append(
        "-- Lam sach scene-link cu de tuyen 12 diem khong bi roi (chi xoa type='scene', "
        "khong dung info/quest)."
    )
    L.append(f"DELETE FROM hotspots WHERE type = 'scene' AND panorama_id IN ({all_ids});")
    ordered = sorted(SCENES, key=lambda s: s["order"])
    for a, b in zip(ordered, ordered[1:]):
        # forward a -> b
        L.append(
            "INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label) "
            f"SELECT '{a['uuid']}', 1.4, 0.0, 'scene', '{b['uuid']}', '→ {q(b['title'])}' "
            "WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = "
            f"'{a['uuid']}' AND content_ref = '{b['uuid']}' AND type = 'scene');"
        )
        # backward b -> a
        L.append(
            "INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label) "
            f"SELECT '{b['uuid']}', -1.4, 0.0, 'scene', '{a['uuid']}', '← {q(a['title'])}' "
            "WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = "
            f"'{b['uuid']}' AND content_ref = '{a['uuid']}' AND type = 'scene');"
        )
    L.append("")
    L.append("-- 3) Anh bo sung: hotspot_contents + info hotspots dat quanh moi scene")
    L.append(
        "-- Don info 'supp:%' cu tu seed truoc (giu lai quest 'hotspot:kitchen/vent')."
    )
    keep_ids = ", ".join(f"'{s['uuid']}'" for s in SCENES)
    L.append(
        f"DELETE FROM hotspots WHERE type = 'info' AND content_ref LIKE 'supp:%' "
        f"AND panorama_id IN ({keep_ids});"
    )
    for sc in SCENES:
        n = len(sc["supp"])
        for i, (_au, _name, title, desc) in enumerate(sc["supp"], 1):
            ref = f"supp:{sc['slug']}-{i}"
            img = f"/media/cu-chi/gallery/{sc['slug']}-{i}.jpg"
            L.append(
                "INSERT INTO hotspot_contents (content_ref, title, description, image_url) VALUES "
                f"('{ref}', '{q(title)}', '{q(desc)}', '{img}') "
                "ON CONFLICT (content_ref) DO UPDATE SET title = EXCLUDED.title, "
                "description = EXCLUDED.description, image_url = EXCLUDED.image_url;"
            )
            # spread info markers across the front arc
            yaw = round(-1.0 + 2.0 * (i - 0.5) / max(n, 1), 3)
            L.append(
                "INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label) "
                f"SELECT '{sc['uuid']}', {yaw}, -0.05, 'info', '{ref}', '{q(title)}' "
                "WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = "
                f"'{sc['uuid']}' AND content_ref = '{ref}');"
            )
    L.append("")
    with open(SQL_OUT, "w", encoding="utf-8") as fh:
        fh.write("\n".join(L))
    print("wrote", SQL_OUT, f"({len(L)} lines)")


# ---------------------------------------------------------------------------
# FE pins
# ---------------------------------------------------------------------------
def cmd_pins(_a):
    print("// paste into cuChiIllustratedMapPins.ts")
    for sc in sorted(SCENES, key=lambda s: s["order"]):
        print(
            f"  {{ id: '{sc['uuid']}', routeOrder: {sc['order']}, "
            f"xPct: {sc['xpct']}, yPct: {sc['ypct']}, "
            f"label: '{sc['title']}' }},"
        )


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("assets")
    sub.add_parser("sql")
    sub.add_parser("pins")
    a = ap.parse_args()
    {"assets": cmd_assets, "sql": cmd_sql, "pins": cmd_pins}[a.cmd](a)


if __name__ == "__main__":
    main()
