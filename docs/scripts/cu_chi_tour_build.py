"""
Cu Chi Tour 360 - offline media tooling.

Reads the raw trip folder (Tour 360/), extracts GPS + dimensions from the
geotagged iPhone panoramas (Hao) and HEIC photos (Phat), clusters them into
representative 360 stops along the walking route, and prepares web-ready assets.

This is OFFLINE tooling only. It does not touch BE/FE app code; it writes
processed images into FE/public/media/cu-chi/** and a manifest into docs/.

Sub-commands:
    probe     -> extract GPS + size for every photo, write cu-chi-360-manifest.json
    cluster   -> group geotagged panos into ~N stops ordered along the route
    sheets    -> build small contact-sheet thumbnails per stop for visual review

Usage:
    python cu_chi_tour_build.py probe
    python cu_chi_tour_build.py cluster --stops 10
    python cu_chi_tour_build.py sheets
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from dataclasses import dataclass, asdict

from PIL import Image, ExifTags

try:
    import pillow_heif

    pillow_heif.register_heif_opener()
    HEIC_OK = True
except Exception:  # pragma: no cover
    HEIC_OK = False

sys.stdout.reconfigure(encoding="utf-8")

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------
HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
RAW_ROOT = os.path.join(REPO, "Tour 360")
DOCS_DIR = os.path.join(REPO, "docs")
MANIFEST = os.path.join(DOCS_DIR, "cu-chi-360-manifest.json")
WORK_DIR = os.path.join(HERE, "_cu_chi_work")

GPS_TAG = {v: k for k, v in ExifTags.TAGS.items()}.get("GPSInfo")


def raw_subdir(prefix: str) -> str | None:
    if not os.path.isdir(RAW_ROOT):
        return None
    for name in os.listdir(RAW_ROOT):
        if name.startswith(prefix) and os.path.isdir(os.path.join(RAW_ROOT, name)):
            return os.path.join(RAW_ROOT, name)
    return None


# ---------------------------------------------------------------------------
# EXIF / GPS
# ---------------------------------------------------------------------------
def read_gps(img: Image.Image):
    try:
        exif = img.getexif()
        if not exif:
            return None
        g = exif.get_ifd(GPS_TAG)
        if not g or 2 not in g or 4 not in g:
            return None

        def dms(v):
            return float(v[0]) + float(v[1]) / 60 + float(v[2]) / 3600

        lat = dms(g[2])
        lon = dms(g[4])
        if g.get(1) == "S":
            lat = -lat
        if g.get(3) == "W":
            lon = -lon
        return round(lat, 6), round(lon, 6)
    except Exception:
        return None


@dataclass
class Photo:
    rel: str          # relative path under RAW_ROOT
    author: str       # Hao / Phat / Vy
    w: int
    h: int
    ratio: float
    lat: float | None
    lon: float | None


def iter_photos():
    authors = {
        "Hao": raw_subdir("H"),
        "Phat": raw_subdir("P"),
        "Vy": raw_subdir("V"),
    }
    exts = (".jpg", ".jpeg", ".heic", ".png")
    for author, d in authors.items():
        if not d:
            continue
        for f in sorted(os.listdir(d)):
            if not f.lower().endswith(exts):
                continue
            if f.lower().endswith(".heic") and not HEIC_OK:
                continue
            p = os.path.join(d, f)
            try:
                img = Image.open(p)
                w, h = img.size
                gps = read_gps(img)
            except Exception as e:
                print("  ! skip", f, str(e)[:40])
                continue
            yield Photo(
                rel=os.path.relpath(p, RAW_ROOT),
                author=author,
                w=w,
                h=h,
                ratio=round(w / max(h, 1), 3),
                lat=gps[0] if gps else None,
                lon=gps[1] if gps else None,
            )


def cmd_probe(_args):
    os.makedirs(DOCS_DIR, exist_ok=True)
    photos = list(iter_photos())
    geo = [p for p in photos if p.lat is not None]
    print(f"photos: {len(photos)} | geotagged: {len(geo)} | HEIC_OK={HEIC_OK}")
    by_author = {}
    for p in photos:
        by_author.setdefault(p.author, {"n": 0, "geo": 0})
        by_author[p.author]["n"] += 1
        if p.lat is not None:
            by_author[p.author]["geo"] += 1
    for a, s in by_author.items():
        print(f"  {a}: {s['n']} photos, {s['geo']} geotagged")
    if geo:
        lats = [p.lat for p in geo]
        lons = [p.lon for p in geo]
        print(f"  bbox lat {min(lats):.6f}..{max(lats):.6f} lon {min(lons):.6f}..{max(lons):.6f}")
    with open(MANIFEST, "w", encoding="utf-8") as fh:
        json.dump([asdict(p) for p in photos], fh, ensure_ascii=False, indent=2)
    print("wrote", MANIFEST)


# ---------------------------------------------------------------------------
# Clustering + route ordering
# ---------------------------------------------------------------------------
def load_manifest():
    with open(MANIFEST, encoding="utf-8") as fh:
        return [Photo(**d) for d in json.load(fh)]


def haversine_m(a, b):
    from math import radians, sin, cos, asin, sqrt

    lat1, lon1, lat2, lon2 = map(radians, [a[0], a[1], b[0], b[1]])
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    h = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
    return 2 * 6371000 * asin(sqrt(h))


def kmeans(points, k, iters=60):
    """Tiny lat/lon k-means (points = list of (lat,lon)). Deterministic seed."""
    import random

    random.seed(7)
    # seed: spread initial centroids by farthest-point sampling
    centroids = [points[0]]
    while len(centroids) < k:
        d = [min(haversine_m(p, c) for c in centroids) for p in points]
        centroids.append(points[d.index(max(d))])
    assign = [0] * len(points)
    for _ in range(iters):
        for i, p in enumerate(points):
            dists = [haversine_m(p, c) for c in centroids]
            assign[i] = dists.index(min(dists))
        new = []
        for j in range(k):
            members = [points[i] for i in range(len(points)) if assign[i] == j]
            if members:
                new.append(
                    (sum(m[0] for m in members) / len(members),
                     sum(m[1] for m in members) / len(members))
                )
            else:
                new.append(centroids[j])
        if new == centroids:
            break
        centroids = new
    return centroids, assign


def order_route(centroids):
    """Greedy nearest-neighbour starting from the north-most point."""
    remaining = list(range(len(centroids)))
    start = max(remaining, key=lambda i: centroids[i][0])  # north-most lat
    order = [start]
    remaining.remove(start)
    while remaining:
        last = centroids[order[-1]]
        nxt = min(remaining, key=lambda i: haversine_m(last, centroids[i]))
        order.append(nxt)
        remaining.remove(nxt)
    return order


def cmd_cluster(args):
    photos = load_manifest()
    # 360 stops are defined by the wide iPhone panoramas (Hao) only.
    panos = [p for p in photos if p.author == "Hao" and p.lat is not None]
    # Phat = normal geotagged photos -> supplementary, attached to nearest stop.
    supp = [p for p in photos if p.author == "Phat" and p.lat is not None]
    pts = [(p.lat, p.lon) for p in panos]
    k = args.stops
    centroids, assign = kmeans(pts, k)
    order = order_route(centroids)
    rank = {cid: i for i, cid in enumerate(order)}

    stops = []
    for cid in order:
        members = [panos[i] for i in range(len(panos)) if assign[i] == cid]
        # widest ratio = closest to full pano, pick as hero candidate
        members.sort(key=lambda p: -p.ratio)
        c = centroids[cid]
        nearby_phat = sorted(
            (p for p in supp),
            key=lambda p: haversine_m((c[0], c[1]), (p.lat, p.lon)),
        )
        # attach a Phat photo to a stop only if it is reasonably close (<60m)
        phat_here = [
            p.rel for p in nearby_phat
            if haversine_m((c[0], c[1]), (p.lat, p.lon)) < 60
        ]
        stops.append(
            {
                "routeOrder": rank[cid] + 1,
                "lat": round(c[0], 6),
                "lon": round(c[1], 6),
                "count": len(members),
                "hero": members[0].rel if members else None,
                "hero_ratio": members[0].ratio if members else None,
                "members": [m.rel for m in members],
                "supp_phat": phat_here,
            }
        )

    out = os.path.join(DOCS_DIR, "cu-chi-360-stops.json")
    with open(out, "w", encoding="utf-8") as fh:
        json.dump(stops, fh, ensure_ascii=False, indent=2)
    print(f"{k} stops along route (panos=Hao, supp=Phat):")
    for s in stops:
        print(
            f"  #{s['routeOrder']:2d} ({s['lat']:.5f},{s['lon']:.5f}) "
            f"panos={s['count']:3d} phat={len(s['supp_phat']):2d} "
            f"hero={s['hero']} r={s['hero_ratio']}"
        )
    print("wrote", out)


# ---------------------------------------------------------------------------
# Contact sheets per stop (visual review)
# ---------------------------------------------------------------------------
def cmd_sheets(_args):
    stops = json.load(open(os.path.join(DOCS_DIR, "cu-chi-360-stops.json"), encoding="utf-8"))
    os.makedirs(WORK_DIR, exist_ok=True)
    for s in stops:
        members = s["members"]
        cols = min(4, len(members))
        rows = (len(members) + cols - 1) // cols
        cw, ch = 480, 160
        sheet = Image.new("RGB", (cols * cw, rows * ch), (20, 20, 26))
        for idx, rel in enumerate(members):
            p = os.path.join(RAW_ROOT, rel)
            try:
                im = Image.open(p).convert("RGB")
            except Exception:
                continue
            im.thumbnail((cw, ch))
            x = (idx % cols) * cw
            y = (idx // cols) * ch
            sheet.paste(im, (x, y))
        name = f"stop-{s['routeOrder']:02d}.jpg"
        sheet.save(os.path.join(WORK_DIR, name), quality=80)
        print("sheet", name, f"({len(members)} imgs)")
    print("contact sheets in", WORK_DIR)


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("probe")
    c = sub.add_parser("cluster")
    c.add_argument("--stops", type=int, default=10)
    sub.add_parser("sheets")
    args = ap.parse_args()
    {"probe": cmd_probe, "cluster": cmd_cluster, "sheets": cmd_sheets}[args.cmd](args)


if __name__ == "__main__":
    main()
