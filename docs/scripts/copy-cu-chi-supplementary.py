"""Copy Cu Chi assets from user folders → FE/public/media/cu-chi."""
from __future__ import annotations

import os
import shutil
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
DEST = ROOT / "FE" / "public" / "media" / "cu-chi"
MIN_PANO = (1440, 720)  # 720p equirectangular (2:1)
MAX_PANO = (4096, 2048)
JPEG_QUALITY = 92


def find_child_dir(parent: Path, *needles: str) -> Path | None:
    needles_l = [n.lower() for n in needles]
    for entry in parent.iterdir():
        if not entry.is_dir():
            continue
        name = entry.name.lower()
        if all(n in name for n in needles_l):
            return entry
    return None


def ensure_dirs() -> None:
    for sub in ("scenes", "artifacts", "map", "panoramas", "gallery"):
        (DEST / sub).mkdir(parents=True, exist_ok=True)


def copy_file(src: Path, rel_dest: str) -> bool:
    if not src.is_file():
        return False
    out = DEST / rel_dest.replace("\\", "/")
    out.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, out)
    print(f"  copy -> {rel_dest}")
    return True


def normalize_pano(path: Path) -> None:
    with Image.open(path) as img:
        w, h = img.size
        target = (w, h)
        if w < MIN_PANO[0] or h < MIN_PANO[1]:
            scale = max(MIN_PANO[0] / w, MIN_PANO[1] / h)
            target = (min(int(w * scale), MAX_PANO[0]), min(int(h * scale), MAX_PANO[1]))
        if target == (w, h):
            if img.mode not in ("RGB", "L"):
                rgb = img.convert("RGB")
                rgb.save(path, "JPEG", quality=JPEG_QUALITY, optimize=True)
            return
        resized = img.resize(target, Image.Resampling.LANCZOS)
        if resized.mode not in ("RGB", "L"):
            resized = resized.convert("RGB")
        resized.save(path, "JPEG", quality=JPEG_QUALITY, optimize=True)
        print(f"  pano {path.name}: {w}x{h} -> {target[0]}x{target[1]}")


def copy_panoramas(anh_dd: Path) -> None:
    pano_dir = find_child_dir(anh_dd, "ảnh", "360") or find_child_dir(anh_dd, "360")
    if not pano_dir:
        print("skip panoramas — no Ảnh 360 folder")
        return
    mapping = {
        "street view 5": "panoramas/duong-vao.jpg",
        "street view 360": "panoramas/san-le-tuong-niem.jpg",
        "street view 4": "panoramas/den-tuong-niem.jpg",
        "street view 3": "panoramas/trung-bay-vu-khi.jpg",
        "street view 2": "panoramas/xe-thiet-giap.jpg",
    }
    for src in pano_dir.iterdir():
        if not src.is_file():
            continue
        key = src.stem.lower()
        for pattern, dest in mapping.items():
            if pattern in key:
                out = DEST / dest
                shutil.copy2(src, out)
                normalize_pano(out)
                break


def copy_ben_duoc_scenes(anh_dd: Path) -> None:
    ben = find_child_dir(anh_dd, "bến", "dược") or find_child_dir(anh_dd, "ben", "duoc")
    if not ben:
        return
    scene_map = {
        "1.png": "artifacts/trung-bay-vu-khi.png",
        "dsc-0001-14049075581474.jpg": "scenes/cua-ham-2026.jpg",
        "8-1.png": "scenes/bep-hoang-cam-1968.png",
        "dsc-8499.jpg": "scenes/bep-hoang-cam-2026.jpg",
        "10.png": "scenes/phong-hop-1968.png",
        "dsc-8523.jpg": "scenes/phong-hop-2026.jpg",
        "12.png": "scenes/gieng-1968.png",
        "dsc-4794.jpg": "scenes/gieng-2026.jpg",
        "5-14046990411011.png": "scenes/thong-gio-1968.png",
        "dsc-0063-14118068932602.jpg": "scenes/thong-gio-2026.jpg",
        "6-14046990474996.png": "scenes/cua-ham-1968.png",
    }
    for name, rel in scene_map.items():
        copy_file(ben / name, rel)
    hero = ben / "images.jpg"
    if not hero.is_file():
        hero_dir = find_child_dir(anh_dd, "tổng", "thể") or find_child_dir(anh_dd, "tong", "the")
        if hero_dir:
            hero = next(hero_dir.glob("images.jpg"), None)
    if hero and hero.is_file():
        copy_file(hero, "map/hero.jpg")


def copy_supplementary(supp_dir: Path) -> None:
    if not supp_dir.is_dir():
        print("skip supplementary folder")
        return

    artifact_map: list[tuple[str, str]] = [
        ("chế tạo vũ khí.jpg", "artifacts/che-tao-vu-khi.jpg"),
        ("chế tạo vũ khí (2).jpg", "artifacts/che-tao-vu-khi-2.jpg"),
        ("chế tạo vũ khí (3).jpg", "artifacts/che-tao-vu-khi-3.jpg"),
        ("chế tạo vũ khí (4).jpg", "artifacts/che-tao-vu-khi-4.jpg"),
        ("cối_xay_thóc.jpg", "artifacts/coi-xay-thoc.jpg"),
        ("vót chông.jpg", "artifacts/vot-chong.jpg"),
        ("hầm giải phẫu.jpg", "artifacts/ham-giai-phau.jpg"),
        ("một lỗ châu mai", "artifacts/lo-chau-mai.jpg"),
        ("du khách được ăn uống", "gallery/an-uong-duoi-dat.jpg"),
        ("du khách bắn súng", "gallery/ban-sung-the-thao.jpg"),
        ("w_củ_chi_khoai_mì_củ_chi_2", "gallery/khoai-mi-1.jpg"),
        ("w_củ_chi_khoai_mì_củ_chi_3", "gallery/khoai-mi-2.jpg"),
        ("w_củ_chi_khoai_mì_củ_chi_4", "gallery/khoai-mi-3.jpg"),
        ("sự tinh vi trong ngụy trang", "gallery/nguy-trang-ham-1.jpg"),
        ("ảnh phục dựng chiến sĩ", "gallery/chien-si-du-kich.jpg"),
        ("một phần địa đạo", "gallery/dia-dao-tong-quan.jpg"),
        ("lính mỹ đang tấn công", "gallery/linh-my-tan-cong.jpg"),
        ("reconstitution_du_transport", "gallery/cuu-thuong-ham.jpg"),
        ("vị trí địa đạo củ chi trên bản đồ", "map/vi-tri-hcm.webp"),
    ]

    files = {f.name.lower(): f for f in supp_dir.iterdir() if f.is_file()}
    for needle, dest in artifact_map:
        needle_l = needle.lower()
        for name, src in files.items():
            if needle_l in name:
                copy_file(src, dest)
                break

    # ngụy trang (2) (3) as gallery extras
    nguy_extra = sorted(
        f for n, f in files.items() if "ngụy trang" in n and ("(2)" in n or "(3)" in n)
    )
    for i, src in enumerate(nguy_extra[:2], start=2):
        copy_file(src, f"gallery/nguy-trang-ham-{i}.jpg")


def main() -> int:
    ensure_dirs()
    anh_dd = find_child_dir(ROOT, "ảnh", "địa") or find_child_dir(ROOT, "anh", "dia")
    supp = find_child_dir(ROOT, "địa", "củ") or find_child_dir(ROOT, "dia", "cu")

    print("=== Panoramas (native resolution, min 720p) ===")
    if anh_dd:
        copy_panoramas(anh_dd)
        copy_ben_duoc_scenes(anh_dd)

    print("=== Supplementary photos & info ===")
    if supp:
        copy_supplementary(supp)

    count = sum(1 for _ in DEST.rglob("*") if _.is_file())
    print(f"\nDone — {count} files in {DEST}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
