# Copy subset ảnh Củ Chi → FE/public/media/cu-chi (dev static)
$ErrorActionPreference = "Stop"
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$dest = Join-Path $root "FE\public\media\cu-chi"
New-Item -ItemType Directory -Force -Path "$dest\scenes", "$dest\artifacts", "$dest\map" | Out-Null

$anhDd = Get-ChildItem $root -Directory | Where-Object {
  $_.Name -notin @('AI', 'BE', 'FE', 'docs') -and $_.Name -notlike '*GD*'
} | Select-Object -First 1

$anhGd = Get-ChildItem $root -Directory | Where-Object { $_.Name -like '*GD*' } | Select-Object -First 1

if ($anhDd) {
  $hero = Get-ChildItem $anhDd.FullName -Recurse -Filter 'images.jpg' | Select-Object -First 1
  if ($hero) { Copy-Item $hero.FullName "$dest\map\hero.jpg" -Force }

  $benDuoc = Get-ChildItem $anhDd.FullName -Directory | Select-Object -First 1
  if ($benDuoc) {
    $map = @{
      '1.png' = 'artifacts\trung-bay-vu-khi.png'
      'dsc-0001-14049075581474.jpg' = 'scenes\cua-ham-2026.jpg'
      '8-1.png' = 'scenes\bep-hoang-cam-1968.png'
      'dsc-8499.jpg' = 'scenes\bep-hoang-cam-2026.jpg'
      '10.png' = 'scenes\phong-hop-1968.png'
      'dsc-8523.jpg' = 'scenes\phong-hop-2026.jpg'
      '12.png' = 'scenes\gieng-1968.png'
      'dsc-4794.jpg' = 'scenes\gieng-2026.jpg'
      '5-14046990411011.png' = 'scenes\thong-gio-1968.png'
      'dsc-0063-14118068932602.jpg' = 'scenes\thong-gio-2026.jpg'
      '6-14046990474996.png' = 'scenes\cua-ham-1968.png'
    }
    foreach ($k in $map.Keys) {
      $src = Join-Path $benDuoc.FullName $k
      if (Test-Path $src) { Copy-Item $src (Join-Path $dest $map[$k]) -Force }
    }
  }
}

if ($anhGd) {
  $wm = Get-ChildItem $anhGd.FullName -Recurse -Filter 'TIMELENS 512px.png' -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($wm) { Copy-Item $wm.FullName "$dest\brand-watermark.png" -Force }
  $khung = Get-ChildItem $anhGd.FullName -Recurse -Filter 'KHUNG 1.png' -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($khung) { Copy-Item $khung.FullName "$dest\frame-khung-1.png" -Force }
}

Write-Host "Copied $((Get-ChildItem $dest -Recurse -File).Count) files to $dest"
