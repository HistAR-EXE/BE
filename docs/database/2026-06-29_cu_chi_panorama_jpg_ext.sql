-- Đổi đuôi .png → .jpg (file Google export thực chất là JPEG)

UPDATE panoramas SET image_url = REPLACE(image_url, '.png', '.jpg')
WHERE location_id = '11111111-1111-1111-1111-111111111111'
  AND image_url LIKE '%.png';
