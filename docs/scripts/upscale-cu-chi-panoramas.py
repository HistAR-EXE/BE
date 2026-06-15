"""Upscale equirectangular Cu Chi panoramas to 4096×2048 for sharper PSV rendering."""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

TARGET = (4096, 2048)
QUALITY = 92


def upscale_file(path: Path) -> None:
    with Image.open(path) as img:
        if img.size == TARGET:
            print(f"skip {path.name} (already {TARGET[0]}×{TARGET[1]})")
            return
        resized = img.resize(TARGET, Image.Resampling.LANCZOS)
        if resized.mode not in ("RGB", "L"):
            resized = resized.convert("RGB")
        resized.save(path, "JPEG", quality=QUALITY, optimize=True)
        print(f"ok  {path.name} {img.size} → {TARGET[0]}×{TARGET[1]}")


def main() -> int:
    root = Path(__file__).resolve().parents[3]
    pano_dir = root / "FE" / "public" / "media" / "cu-chi" / "panoramas"
    if not pano_dir.is_dir():
        print(f"not found: {pano_dir}", file=sys.stderr)
        return 1
    for path in sorted(pano_dir.glob("*.jpg")):
        upscale_file(path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
