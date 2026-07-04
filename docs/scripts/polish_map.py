"""Rotate the paper map to landscape, crop the orange frame + fine print,
polish (denoise/contrast/saturation + soft vignette), export the FE webp.

Usage: python polish_map.py [preview|final]
"""
import os, sys
from PIL import Image, ImageEnhance, ImageFilter, ImageDraw
sys.stdout.reconfigure(encoding="utf-8")

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
RAW = os.path.join(REPO, "Tour 360")
WORK = os.path.join(HERE, "_cu_chi_work")
OUT = os.path.join(REPO, "FE", "public", "media", "cu-chi", "map", "so-do-ben-duoc.webp")

# crop fractions (of rotated landscape image): left, top, right, bottom
CROP = (0.048, 0.052, 0.030, 0.082)


def load_rotated() -> Image.Image:
    name = [f for f in os.listdir(RAW) if f.lower().endswith(".jpg")][0]
    im = Image.open(os.path.join(RAW, name)).convert("RGB")
    return im.rotate(90, expand=True)  # CCW -> labels become horizontal


def vignette(im: Image.Image, strength: float = 0.22) -> Image.Image:
    w, h = im.size
    mask = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(mask)
    m = int(min(w, h) * 0.06)
    d.rectangle([m, m, w - m, h - m], fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(min(w, h) * 0.05))
    dark = ImageEnhance.Brightness(im).enhance(1 - strength)
    return Image.composite(im, dark, mask)


def polish() -> Image.Image:
    im = load_rotated()
    w, h = im.size
    l, t, r, b = CROP
    im = im.crop((int(w * l), int(h * t), int(w * (1 - r)), int(h * (1 - b))))
    im = im.filter(ImageFilter.MedianFilter(3))  # soften paper grain/fold
    im = im.filter(ImageFilter.UnsharpMask(radius=2, percent=70, threshold=2))
    im = ImageEnhance.Color(im).enhance(1.12)
    im = ImageEnhance.Contrast(im).enhance(1.05)
    im = ImageEnhance.Brightness(im).enhance(1.02)
    im = vignette(im, 0.20)
    return im


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else "preview"
    im = polish()
    print("output size", im.size)
    if mode == "final":
        os.makedirs(os.path.dirname(OUT), exist_ok=True)
        im.save(OUT, "WEBP", quality=88, method=6)
        print("wrote", OUT)
    im.resize((im.width // 3, im.height // 3)).save(
        os.path.join(WORK, "map_landscape_preview.jpg"), quality=86
    )
    print("wrote preview")


if __name__ == "__main__":
    main()
