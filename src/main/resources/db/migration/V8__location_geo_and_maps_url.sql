-- V8: Địa chỉ + Google Maps URL + tọa độ GPS chuẩn cho 10 di tích

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS formatted_address TEXT,
    ADD COLUMN IF NOT EXISTS google_maps_url TEXT;

-- Địa đạo Củ Chi
UPDATE locations SET
    latitude = 11.141591,
    longitude = 106.4615963,
    formatted_address = 'Khu di tích lịch sử Địa đạo Củ Chi, Phú Mỹ Hưng, Củ Chi, TP. Hồ Chí Minh',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=11.141591,106.4615963'
WHERE id = '11111111-1111-1111-1111-111111111111';

-- Bến Nhà Rồng
UPDATE locations SET
    latitude = 10.766813,
    longitude = 106.706726,
    formatted_address = '1 Nguyễn Tất Thành, phường 12, quận 4, TP. Hồ Chí Minh',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=10.766813,106.706726'
WHERE id = '22222222-2222-2222-2222-222222222201';

-- Chùa Thiên Mụ
UPDATE locations SET
    latitude = 16.453453,
    longitude = 107.558618,
    formatted_address = 'Kim Long, phường Hương Long, TP. Huế, Thừa Thiên Huế',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=16.453453,107.558618'
WHERE id = '22222222-2222-2222-2222-222222222202';

-- Cố Đô Hoa Lư
UPDATE locations SET
    latitude = 20.281923,
    longitude = 105.919373,
    formatted_address = 'xã Trường Yên, huyện Hoa Lư, tỉnh Ninh Bình',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=20.281923,105.919373'
WHERE id = '22222222-2222-2222-2222-222222222203';

-- Hoàng Thành Thăng Long
UPDATE locations SET
    latitude = 21.036578,
    longitude = 105.817288,
    formatted_address = '19C phố Hoàng Diệu, phường Điện Biên, quận Ba Đình, Hà Nội',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=21.036578,105.817288'
WHERE id = '22222222-2222-2222-2222-222222222204';

-- Phố Cổ Hội An
UPDATE locations SET
    latitude = 15.877157,
    longitude = 108.32934,
    formatted_address = 'phường Minh An, TP. Hội An, tỉnh Quảng Nam',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=15.877157,108.32934'
WHERE id = '22222222-2222-2222-2222-222222222205';

-- Thành Nhà Hồ
UPDATE locations SET
    latitude = 20.079722,
    longitude = 105.604167,
    formatted_address = 'xã Vĩnh Lộc, huyện Vĩnh Lộc, tỉnh Thanh Hóa',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=20.079722,105.604167'
WHERE id = '22222222-2222-2222-2222-222222222206';

-- Văn Miếu Quốc Tử Giám
UPDATE locations SET
    latitude = 21.027954,
    longitude = 105.835754,
    formatted_address = '58 phố Quốc Tử Giám, phường Văn Miếu - Quốc Tử Giám, quận Đống Đa, Hà Nội',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=21.027954,105.835754'
WHERE id = '22222222-2222-2222-2222-222222222207';

-- Đại Nội Huế
UPDATE locations SET
    latitude = 16.469617,
    longitude = 107.579412,
    formatted_address = 'phố Hùng Vương, phường Thuận Thành, TP. Huế, Thừa Thiên Huế',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=16.469617,107.579412'
WHERE id = '22222222-2222-2222-2222-222222222208';

-- Đền Hùng Vương
UPDATE locations SET
    latitude = 21.353353,
    longitude = 105.314722,
    formatted_address = 'Khu di tích Đền Hùng, xã Hy Cường, huyện Lâm Thao, tỉnh Phú Thọ',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=21.353353,105.314722'
WHERE id = '22222222-2222-2222-2222-222222222209';
