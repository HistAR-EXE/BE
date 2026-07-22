#!/usr/bin/env python3
"""
Cu Chi Tour 360 pipeline — manifest-first, exports JPG + SQL.

Usage:
    python cu_chi_tour_pipeline.py assets [--source "E:/Tour 360/Hảo"]
    python cu_chi_tour_pipeline.py sql
    python cu_chi_tour_pipeline.py manifest  # write manifest from scan (heuristic fill)
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from pathlib import Path

from PIL import Image

try:
    import pillow_heif

    pillow_heif.register_heif_opener()
except Exception:
    pass

sys.stdout.reconfigure(encoding="utf-8")

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
MANIFEST_PATH = os.path.join(REPO, "docs", "cu-chi-tour-manifest.json")
FE_PANO_ROOT = Path(REPO) / "FE" / "public" / "media" / "cu-chi" / "panoramas"
SQL_OUT = os.path.join(REPO, "BE", "docs", "database", "2026-07-10_cu_chi_multi_panoramas.sql")
CU_CHI = "11111111-1111-1111-1111-111111111111"

EXPORT_W = 4096
EXPORT_H = 2048
JPEG_QUALITY = 78
MAX_BYTES_WARN = 2_000_000

# Reuse image processing from legacy script
sys.path.insert(0, HERE)
import cu_chi_assets as assets  # noqa: E402

PANO360_DIR_NAMES = ("ảnh 360", "anh 360")
STATIC_DIR_NAMES = ("ảnh tĩnh", "anh tinh")


def q(s: str) -> str:
    return s.replace("'", "''")


def load_manifest() -> dict:
    with open(MANIFEST_PATH, encoding="utf-8") as f:
        return json.load(f)


def resolve_source_root(manifest: dict, override: str | None) -> Path:
    raw = override or manifest.get("sourceRoot") or os.path.join(REPO, "Tour 360", "Hảo")
    return Path(raw)


def _norm_name(s: str) -> str:
    import unicodedata

    s = unicodedata.normalize("NFD", s.lower())
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    s = re.sub(r"[^a-z0-9]+", "", s)
    return s


def find_file_by_basename(root: Path, rel_hint: str) -> Path | None:
    """Find file under root matching basename (case-insensitive, accent-tolerant)."""
    hint = Path(rel_hint)
    name = hint.name.lower()
    norm_hint = _norm_name(hint.name)
    direct = root / rel_hint
    if direct.is_file():
        return direct
    for p in root.rglob("*"):
        if not p.is_file():
            continue
        if p.name.lower() == name:
            return p
        if norm_hint and _norm_name(p.name) == norm_hint:
            return p
    # fuzzy: basename contains key tokens (e.g. t5, street view)
    tokens = [t for t in re.split(r"[\s_\-./]+", _norm_name(hint.stem)) if len(t) >= 2]
    if len(tokens) >= 2:
        best: Path | None = None
        best_score = 0
        for p in root.rglob("*"):
            if not p.is_file():
                continue
            pn = _norm_name(p.name)
            score = sum(1 for t in tokens if t in pn)
            if score > best_score and score >= max(2, len(tokens) - 1):
                best_score = score
                best = p
        if best:
            return best
    return None


def open_strip(path: Path) -> Image.Image:
    return Image.open(path).convert("RGB")


def ratio_of(path: Path) -> float:
    with Image.open(path) as im:
        w, h = im.size
        return w / h if h else 1.0


def is_full_equirect(strip: Image.Image) -> bool:
    """Google Maps / Street View exports are already 2:1 equirectangular."""
    w, h = strip.size
    if h == 0:
        return False
    ratio = w / h
    return 1.95 <= ratio <= 2.05


def passthrough_equirect(strip: Image.Image) -> Image.Image:
    """Keep native 360° imagery — only resize later in save_web_jpg."""
    return strip.convert("RGB")


def process_scene(scene: dict, root: Path) -> Image.Image:
    mode = scene.get("mode", "single")
    files = scene.get("sourceFiles") or []
    if not files:
        raise FileNotFoundError(f"scene {scene.get('slug')} has no sourceFiles")

    paths = []
    for rel in files:
        p = find_file_by_basename(root, rel)
        if not p:
            raise FileNotFoundError(f"missing source: {rel}")
        paths.append(p)

    force_partial = scene.get("forcePartial", False)
    force_full = scene.get("forceFull", False)

    stitch_post = scene.get("stitchPost") or {}

    if mode == "stitch" and len(paths) >= 2:
        # stitch_panos expects (author, name) via src() — inline open instead
        import cv2
        import numpy as np

        mats = []
        for p in paths:
            im = open_strip(p)
            w, h = im.size
            if w > 3000:
                im = im.resize((3000, round(h * 3000 / w)), Image.LANCZOS)
            mats.append(cv2.cvtColor(np.array(im), cv2.COLOR_RGB2BGR))
        if stitch_post.get("equalizeExposure", False):
            mats = assets.equalize_exposure_strips(mats)
        st = cv2.Stitcher_create(cv2.Stitcher_PANORAMA)
        st.setPanoConfidenceThresh(0.2)
        status, pano = st.stitch(mats)
        if status != 0:
            # fallback: horizontal concat + equirect
            strips = [open_strip(p) for p in paths]
            total_w = sum(s.width for s in strips)
            max_h = max(s.height for s in strips)
            canvas = Image.new("RGB", (total_w, max_h))
            x = 0
            for s in strips:
                canvas.paste(s, (x, 0))
                x += s.width
            pano_rgb = np.asarray(canvas)
            pano_rgb = assets.apply_stitch_post_wide(pano_rgb, stitch_post)
            eq = assets.make_equirect_wide(Image.fromarray(pano_rgb))
            return assets.apply_stitch_post_equirect(eq, stitch_post)
        pano = assets._crop_black(cv2.cvtColor(pano, cv2.COLOR_BGR2RGB))
        pano = assets.apply_stitch_post_wide(pano, stitch_post)
        eq = assets.make_equirect_wide(Image.fromarray(pano))
        return assets.apply_stitch_post_equirect(eq, stitch_post)

    strip = open_strip(paths[0])
    if mode == "equirect" or is_full_equirect(strip):
        return passthrough_equirect(strip)

    r = strip.width / strip.height
    if force_full or (not force_partial and r <= 2.2):
        return assets.make_equirect(strip)
    if r > 2.5:
        return assets.equirect_from(strip)
    return assets.make_equirect(strip)


def save_web_jpg(img: Image.Image, out_path: Path) -> None:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    resized = img
    if img.size != (EXPORT_W, EXPORT_H):
        resized = img.resize((EXPORT_W, EXPORT_H), Image.LANCZOS)
    resized.save(
        out_path,
        "JPEG",
        quality=JPEG_QUALITY,
        optimize=True,
        progressive=True,
        subsampling=2,
    )
    size = out_path.stat().st_size
    if size > MAX_BYTES_WARN:
        print(f"  WARN {out_path.name}: {size // 1024}KB > {MAX_BYTES_WARN // 1024}KB")


def cmd_assets(args: argparse.Namespace) -> None:
    manifest = load_manifest()
    root = resolve_source_root(manifest, args.source)
    if not root.is_dir():
        print(f"ERROR: source root not found: {root}")
        sys.exit(1)

    for area in manifest.get("areas", []):
        slug_area = area["areaSlug"]
        for scene in area.get("scenes", []):
            slug = scene["slug"]
            rel_url = f"/media/cu-chi/panoramas/{slug_area}/{slug}.jpg"
            out = FE_PANO_ROOT / slug_area / f"{slug}.jpg"
            try:
                eq = process_scene(scene, root)
                save_web_jpg(eq, out)
                print(f"OK  {rel_url} <- {scene.get('mode')} {scene.get('sourceFiles')}")
            except Exception as ex:
                print(f"SKIP {slug}: {ex}")
                scene.setdefault("needsReview", True)

    print("done assets")


def cmd_sql(_args: argparse.Namespace) -> None:
    manifest = load_manifest()
    lines: list[str] = []
    lines.append("SET client_encoding TO 'UTF8';")
    lines.append("-- Cu Chi multi-scene tour — generated from docs/cu-chi-tour-manifest.json")
    lines.append("-- Route: 1 Bãi xe → 2 Đền → 3 Khu trưng bày → 4 Bộ Tư lệnh → 5 Khu ủy → 6 Nhà BD → 7 Tái hiện")
    lines.append("")
    lines.append("-- Ensure tour360 columns exist before INSERT (Render seed runs this before Flyway V15)")
    lines.append("ALTER TABLE panoramas ADD COLUMN IF NOT EXISTS area_slug VARCHAR(64);")
    lines.append("ALTER TABLE panoramas ADD COLUMN IF NOT EXISTS sort_order INT DEFAULT 0;")
    lines.append("ALTER TABLE panoramas ADD COLUMN IF NOT EXISTS default_yaw DOUBLE PRECISION DEFAULT 0;")
    lines.append("ALTER TABLE panoramas ADD COLUMN IF NOT EXISTS default_pitch DOUBLE PRECISION DEFAULT 0;")
    lines.append("ALTER TABLE hotspots ADD COLUMN IF NOT EXISTS marker_style VARCHAR(10);")
    lines.append("")
    lines.append("-- Audit user discoveries (run manually on production before deploy)")
    lines.append("-- SELECT discovery_key, COUNT(*) FROM user_discoveries")
    lines.append("-- WHERE discovery_key LIKE 'scene:22222222-2222-2222-2222-%' GROUP BY discovery_key;")
    lines.append("")

    all_ids: list[str] = []
    pano_area: dict[str, str] = {}
    for area in manifest["areas"]:
        for scene in area["scenes"]:
            pid = scene["panoramaId"]
            all_ids.append(pid)
            pano_area[pid] = area["areaSlug"]

    for area in manifest["areas"]:
        for scene in area["scenes"]:
            pid = scene["panoramaId"]
            img = f"/media/cu-chi/panoramas/{area['areaSlug']}/{scene['slug']}.jpg"
            dy = scene.get("defaultYaw", 0)
            dp = scene.get("defaultPitch", 0)
            so = scene.get("sortOrder", 1)
            lines.append(
                "INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) "
                f"VALUES ('{pid}', '{CU_CHI}', '{img}', '{q(scene['title'])}', "
                f"'{area['areaSlug']}', {so}, {dy}, {dp}) "
                "ON CONFLICT (id) DO UPDATE SET "
                "image_url = EXCLUDED.image_url, title = EXCLUDED.title, "
                "area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, "
                "default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;"
            )

    lines.append("")
    id_list = ", ".join(f"'{i}'" for i in all_ids)
    lines.append(f"DELETE FROM hotspots WHERE type = 'scene' AND panorama_id IN ({id_list});")

    # Build forward + backward links from manifest
    link_map: dict[str, list[dict]] = {}
    forward_pairs: set[tuple[str, str]] = set()
    for link in manifest.get("links", []):
        link_map.setdefault(link["from"], []).append(link)
        forward_pairs.add((link["from"], link["to"]))

    for link in manifest.get("links", []):
        fr, to = link["from"], link["to"]
        style = link.get("markerStyle", "far")
        yaw = link.get("yaw", 0.0)
        pitch = link.get("pitch", -0.55 if style == "far" else -0.42)
        label = link.get("label", f"→ {to}")
        lines.append(
            "INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) "
            f"SELECT '{fr}', {yaw}, {pitch}, 'scene', '{to}', '{q(label)}', '{style}' "
            f"WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '{fr}' "
            f"AND content_ref = '{to}' AND type = 'scene');"
        )

    # backward links: yaw + PI (skip when explicit reverse forward exists)
    import math

    for link in manifest.get("links", []):
        fr, to = link["from"], link["to"]
        if (to, fr) in forward_pairs:
            continue
        style = link.get("markerStyle", "far")
        fwd_yaw = link.get("yaw", 0.0)
        back_yaw_val = fwd_yaw + math.pi
        pitch = link.get("pitch", -0.55 if style == "far" else -0.42)
        lines.append(
            "INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) "
            f"SELECT '{to}', {back_yaw_val}, {pitch}, 'scene', '{fr}', '← Quay lại', '{style}' "
            f"WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '{to}' "
            f"AND content_ref = '{fr}' AND type = 'scene');"
        )

    with open(SQL_OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print(f"Wrote {SQL_OUT}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Cu Chi Tour 360 pipeline")
    sub = parser.add_subparsers(dest="cmd", required=True)
    p_assets = sub.add_parser("assets", help="Process images from manifest")
    p_assets.add_argument("--source", help="Override E:/Tour 360/Hảo path")
    sub.add_parser("sql", help="Generate SQL from manifest")
    args = parser.parse_args()
    if args.cmd == "assets":
        cmd_assets(args)
    elif args.cmd == "sql":
        cmd_sql(args)


if __name__ == "__main__":
    main()
