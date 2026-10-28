-- V84 seeded five preview images with .webp paths, while R2 holds .jpeg files.
-- Update only those exact obsolete paths; preserve all quest progress and media.
UPDATE quest_steps
SET preview_image = replace(preview_image, '/cover.webp', '/cover.jpeg')
WHERE preview_image IN (
    '/media/heritage/hoang-thanh-thang-long/cover.webp',
    '/media/heritage/pho-co-hoi-an/cover.webp',
    '/media/heritage/thanh-nha-ho/cover.webp',
    '/media/heritage/van-mieu-quoc-tu-giam/cover.webp',
    '/media/heritage/dai-noi-hue/cover.webp'
);
