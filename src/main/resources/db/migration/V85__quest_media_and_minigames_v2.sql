-- Nhiệm vụ v2 (Giai đoạn 3) — ảnh và mini game.
-- Spec: BE/docs/superpowers/specs/2026-10-19-quest-tracks-v2-design.md (mục "Giai đoạn 3")
--
-- • Ảnh bìa và ảnh bước của nhiệm vụ Củ Chi: mỗi ảnh khác nhau trong cùng một nhiệm vụ,
--   dùng ảnh thật nằm trong FE/public/images (không phụ thuộc tệp R2 chưa tải lên).
-- • Cấu hình mini game: ảnh thật thay cho SVG giữ chỗ, Đường khói thành lưới rãnh có bếp/hầm khói/miệng thoát,
--   Xếp tầng thêm độ sâu từng tầng, Im lặng thêm dữ kiện có nguồn, Đếm dấu thành đếm lỗ thông hơi.
-- • Id mức chơi giữ nguyên để user_game_results.last_level_id vẫn khớp.
-- Chạy lại an toàn (chỉ UPDATE theo id).

-- ---------------------------------------------------------------------------
-- 1. Ảnh bìa nhiệm vụ Củ Chi
-- ---------------------------------------------------------------------------
UPDATE quests q
SET cover_image = v.cover
FROM (VALUES
    ('33333333-3333-3333-3333-333333333334', '/images/time-portal/era-1968-meeting.jpg'),
    ('33333333-3333-3333-3333-333333333335', '/images/xua-nay/den-ham-nay.webp'),
    ('33333333-3333-3333-3333-333333333336', '/images/time-portal/era-1968-tunnel.jpg'),
    ('33333333-3333-3333-3333-333333333337', '/images/time-portal/site-kitchen.jpg'),
    ('33333333-3333-3333-3333-333333333338', '/images/cu-chi/nap-ham-mo.jpg'),
    ('33333333-3333-3333-3333-333333333339', '/images/time-portal/era-2026-main.png'),
    ('33333333-3333-3333-3333-333333333340', '/images/cu-chi/rung-dia-dao.jpg')
) AS v(id, cover)
WHERE q.id = v.id::uuid;

-- ---------------------------------------------------------------------------
-- 2. Ảnh từng bước (không trùng nhau, không trùng ảnh bìa trong cùng nhiệm vụ)
-- ---------------------------------------------------------------------------
UPDATE quest_steps s
SET preview_image = v.image
FROM (VALUES
    -- 333 Hành trình dưới lòng đất
    ('33333333-3333-3333-3333-333333333301', '/images/time-portal/site-kitchen.jpg'),
    ('33333333-3333-3333-3333-333333333302', '/images/xua-nay/phong-hop-nay.webp'),
    ('33333333-3333-3333-3333-333333333305', '/images/xua-nay/bep-hoang-cam-1968.webp'),
    ('33333333-3333-3333-3333-333333333303', '/images/time-portal/era-1948-main.png'),
    -- 334 Mật mã Sa bàn Tác chiến
    ('33333333-3333-3333-3333-333333333341', '/images/characters/anh-ba.png'),
    ('33333333-3333-3333-3333-333333333342', '/images/time-portal/site-diorama.jpg'),
    ('33333333-3333-3333-3333-333333333343', '/images/time-portal/era-1968-main.png'),
    -- 335 Ánh sáng Đèn Mù U
    ('33333333-3333-3333-3333-333333333351', '/images/characters/chi-nam.png'),
    ('33333333-3333-3333-3333-333333333352', '/images/time-portal/era-1968-surgery.jpg'),
    ('33333333-3333-3333-3333-333333333353', '/images/time-portal/era-1968-crawl.jpg'),
    -- 336 Mật lệnh thử game
    ('33333333-3333-3333-3333-333333333361', '/images/cu-chi/so-do-ba-tang.jpg'),
    ('33333333-3333-3333-3333-333333333362', '/images/minigames/soi-nap-cover.jpg'),
    ('33333333-3333-3333-3333-333333333363', '/images/time-portal/era-1975-mid.png'),
    ('33333333-3333-3333-3333-333333333364', '/images/minigames/dem-lo-1.jpg'),
    -- 337 Bếp không khói
    ('33333333-3333-3333-3333-333333333371', '/images/xua-nay/bep-hoang-cam-nay.webp'),
    ('33333333-3333-3333-3333-333333333372', '/images/xua-nay/bep-hoang-cam-1968.webp'),
    -- 338 Nắp hầm và cạm bẫy
    ('33333333-3333-3333-3333-333333333381', '/images/time-portal/site-hatch.jpg'),
    ('33333333-3333-3333-3333-333333333382', '/images/minigames/soi-nap-cover.jpg'),
    ('33333333-3333-3333-3333-333333333383', '/images/cu-chi/du-kich-chien-dau.jpg'),
    -- 339 Ba tầng lòng đất
    ('33333333-3333-3333-3333-333333333391', '/images/cu-chi/duong-ham.jpg'),
    ('33333333-3333-3333-3333-333333333392', '/images/cu-chi/so-do-ba-tang.jpg'),
    -- 340 Theo dấu địa đạo (6 trạm)
    ('33333333-3333-3333-3333-333333340001', '/images/cu-chi/tram-st01-phong-hop.jpg'),
    ('33333333-3333-3333-3333-333333340002', '/images/time-portal/site-kitchen.jpg'),
    ('33333333-3333-3333-3333-333333340003', '/images/cu-chi/tram-st03-gieng.jpg'),
    ('33333333-3333-3333-3333-333333340004', '/images/cu-chi/tram-st04-quan-y.jpg'),
    ('33333333-3333-3333-3333-333333340005', '/images/cu-chi/tram-st05-xuong-vu-khi.jpg'),
    ('33333333-3333-3333-3333-333333340006', '/images/cu-chi/tram-st06-lo-chau-mai.jpg')
) AS v(id, image)
WHERE s.id = v.id::uuid;

-- ---------------------------------------------------------------------------
-- 3. Mini game
-- ---------------------------------------------------------------------------
-- 3a. Soi nắp (Củ Chi + bản trong "Mật lệnh thử game"): ảnh thật nắp hầm ngụy trang dưới lá.
--     Toạ độ là % theo ảnh 960×640 trong FE/public/images/minigames.
UPDATE quest_minigames
SET config = $json${
  "facts": [
    {"url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490",
     "text": "Chỗ lên xuống giữa các tầng có nắp hầm bí mật, cửa lên được ngụy trang."}
  ],
  "levels": [
    {"id": "lid-1",
     "prompt": "Dưới lớp lá có một nắp hầm đang hé mở. Tìm và chạm vào nó trước khi hết 10 giây.",
     "imageUrl": "/images/minigames/soi-nap-1.jpg",
     "revealImageUrl": "/images/minigames/soi-nap-reveal.jpg",
     "timeLimitMs": 10000,
     "targetX": 49, "targetY": 36.3, "radius": 10},
    {"id": "lid-2",
     "prompt": "Góc nhìn khác, nắp nằm chỗ khác. Chạm hụt 3 lần là bị lộ.",
     "imageUrl": "/images/minigames/soi-nap-2.jpg",
     "revealImageUrl": "/images/minigames/soi-nap-reveal.jpg",
     "timeLimitMs": 10000,
     "targetX": 68, "targetY": 29, "radius": 10},
    {"id": "lid-3",
     "prompt": "Nắp đã khép gần kín, chỉ còn một mép gỗ lộ ra giữa lá khô.",
     "imageUrl": "/images/minigames/soi-nap-3.jpg",
     "revealImageUrl": "/images/minigames/soi-nap-reveal.jpg",
     "timeLimitMs": 10000,
     "targetX": 32, "targetY": 48.3, "radius": 10,
     "fact": {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
              "text": "Khi đóng nắp miệng hầm, kẻ thù đi trên mặt đất khó phát hiện được căn hầm."}}
  ]
}$json$
WHERE id IN ('a1000001-0000-4000-8000-000000000002'::uuid, 'a1000001-0000-4000-8000-000000000012'::uuid);

-- 3b. Đường khói: lưới rãnh nhìn mặt cắt. Ô có "rotation" là đoạn rãnh cần xoay (đáp án, FE không thấy).
--     Hướng: straight 0 = Tây–Đông, 1 = Bắc–Nam; elbow 0 = Bắc–Đông, 1 = Đông–Nam, 2 = Nam–Tây, 3 = Tây–Bắc;
--     tee 0 = Tây–Bắc–Đông, 1 = Bắc–Đông–Nam, 2 = Đông–Nam–Tây, 3 = Nam–Tây–Bắc. "start" là góc ban đầu FE vẽ.
--     Mỗi màn chỉ có một cách nối kín (không rò khói), nên FE tự kiểm tra được và BE chấm lại.
UPDATE quest_minigames
SET config = $json${
  "facts": [
    {"url": "https://dantri.com.vn/khoa-hoc/thiet-ke-dac-biet-cua-bep-hoang-cam-de-triet-khoi-che-mat-quan-thu-20250814144359648.htm",
     "text": "Khói đi qua đường rãnh, tản ra, trên mặt đất còn làn khói mỏng bay là là."}
  ],
  "levels": [
    {"id": "dk-1",
     "prompt": "Xoay các đoạn rãnh để khói từ bếp đi qua hầm chứa khói rồi thoát lên mặt đất.",
     "cols": 4, "rows": 3, "parMs": 20000,
     "cells": [
       {"id": "bep", "kind": "stove", "x": 0, "y": 2, "open": ["E"]},
       {"id": "a", "kind": "straight", "x": 1, "y": 2, "start": 1, "rotation": 0},
       {"id": "h1", "kind": "chamber", "x": 2, "y": 2, "open": ["W", "E"]},
       {"id": "b", "kind": "elbow", "x": 3, "y": 2, "start": 0, "rotation": 3},
       {"id": "c", "kind": "straight", "x": 3, "y": 1, "start": 0, "rotation": 1},
       {"id": "o1", "kind": "outlet", "x": 3, "y": 0, "open": ["S"]}
     ]},
    {"id": "dk-2",
     "prompt": "Khói phải chia ra hai miệng thoát để chỉ còn làn mỏng trên mặt đất.",
     "cols": 5, "rows": 3, "parMs": 30000,
     "cells": [
       {"id": "bep", "kind": "stove", "x": 0, "y": 2, "open": ["E"]},
       {"id": "a", "kind": "elbow", "x": 1, "y": 2, "start": 1, "rotation": 3},
       {"id": "b", "kind": "elbow", "x": 1, "y": 1, "start": 3, "rotation": 1},
       {"id": "h1", "kind": "chamber", "x": 2, "y": 1, "open": ["W", "E"]},
       {"id": "t", "kind": "tee", "x": 3, "y": 1, "start": 2, "rotation": 0},
       {"id": "o1", "kind": "outlet", "x": 3, "y": 0, "open": ["S"]},
       {"id": "d", "kind": "elbow", "x": 4, "y": 1, "start": 1, "rotation": 3},
       {"id": "o2", "kind": "outlet", "x": 4, "y": 0, "open": ["S"]}
     ]},
    {"id": "dk-3",
     "prompt": "Rãnh dài hơn, qua hai hầm khói. Không để khói rò ra chỗ nào.",
     "cols": 5, "rows": 4, "parMs": 45000,
     "cells": [
       {"id": "bep", "kind": "stove", "x": 0, "y": 3, "open": ["E"]},
       {"id": "a", "kind": "straight", "x": 1, "y": 3, "start": 1, "rotation": 0},
       {"id": "b", "kind": "elbow", "x": 2, "y": 3, "start": 0, "rotation": 3},
       {"id": "h1", "kind": "chamber", "x": 2, "y": 2, "open": ["S", "N"]},
       {"id": "t", "kind": "tee", "x": 2, "y": 1, "start": 0, "rotation": 2},
       {"id": "c", "kind": "elbow", "x": 1, "y": 1, "start": 2, "rotation": 0},
       {"id": "o1", "kind": "outlet", "x": 1, "y": 0, "open": ["S"]},
       {"id": "d", "kind": "straight", "x": 3, "y": 1, "start": 1, "rotation": 0},
       {"id": "h2", "kind": "chamber", "x": 4, "y": 1, "open": ["W", "N"]},
       {"id": "o2", "kind": "outlet", "x": 4, "y": 0, "open": ["S"]}
     ]}
  ]
}$json$
WHERE id = 'a1000001-0000-4000-8000-000000000011'::uuid;

-- 3c. Xếp tầng: thêm độ sâu từng tầng (baolaocai.vn: ~3 m, ~5 m, ~8–10 m, có đoạn tới 12 m).
UPDATE quest_minigames
SET config = $json${
  "facts": [
    {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
     "text": "Tầng 1 sâu khoảng 3 m, tầng 2 khoảng 5 m, tầng 3 khoảng 8–10 m, có đoạn tới 12 m. Càng sâu càng chịu được bom đạn lớn."},
    {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
     "text": "Tầng 2 chống được bom cỡ nhỏ."},
    {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
     "text": "Tầng 3 chống được hầu hết các loại bom đạn."}
  ],
  "levels": [
    {"id": "fn-1",
     "prompt": "Đặt mỗi thẻ vào đúng tầng địa đạo. Gợi ý: càng sâu càng chịu được bom đạn lớn.",
     "floors": [
       {"id": "t1", "label": "Tầng 1", "depth": "sâu khoảng 3 m"},
       {"id": "t2", "label": "Tầng 2", "depth": "sâu khoảng 5 m"},
       {"id": "t3", "label": "Tầng 3", "depth": "sâu khoảng 8–10 m"}
     ],
     "items": [
       {"id": "tank", "label": "Chịu được đạn pháo và sức nặng xe tăng", "floorId": "t1"},
       {"id": "small", "label": "Chống được bom cỡ nhỏ", "floorId": "t2"},
       {"id": "most", "label": "Chống được hầu hết bom đạn", "floorId": "t3"}
     ]},
    {"id": "fn-2",
     "prompt": "Thứ tự thẻ đã đổi. Đặt lại cho đúng tầng.",
     "floors": [
       {"id": "t1", "label": "Tầng 1", "depth": "sâu khoảng 3 m"},
       {"id": "t2", "label": "Tầng 2", "depth": "sâu khoảng 5 m"},
       {"id": "t3", "label": "Tầng 3", "depth": "sâu khoảng 8–10 m"}
     ],
     "items": [
       {"id": "most", "label": "Chống được hầu hết bom đạn", "floorId": "t3"},
       {"id": "tank", "label": "Chịu được đạn pháo và sức nặng xe tăng", "floorId": "t1"},
       {"id": "small", "label": "Chống được bom cỡ nhỏ", "floorId": "t2"}
     ],
     "fact": {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
              "text": "Tầng 2 chống được bom cỡ nhỏ."}},
    {"id": "fn-3",
     "prompt": "Lượt cuối, thẻ ngắn hơn. Đừng để sai quá nhiều.",
     "floors": [
       {"id": "t1", "label": "Tầng 1", "depth": "sâu khoảng 3 m"},
       {"id": "t2", "label": "Tầng 2", "depth": "sâu khoảng 5 m"},
       {"id": "t3", "label": "Tầng 3", "depth": "sâu khoảng 8–10 m"}
     ],
     "items": [
       {"id": "small", "label": "Bom cỡ nhỏ", "floorId": "t2"},
       {"id": "most", "label": "Hầu hết bom đạn", "floorId": "t3"},
       {"id": "tank", "label": "Đạn pháo và xe tăng", "floorId": "t1"}
     ],
     "fact": {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
              "text": "Tầng 3 chống được hầu hết các loại bom đạn."}}
  ]
}$json$
WHERE id IN ('a1000001-0000-4000-8000-000000000001'::uuid, 'a1000001-0000-4000-8000-000000000013'::uuid);

-- 3d. Đào gặp nhau: giữ đáp án, thêm khoảng cách hai giếng để FE vẽ (dữ kiện: 6–15 m).
UPDATE quest_minigames
SET config = $json${
  "facts": [
    {"url": "https://baophapluat.vn/doi-quan-xuat-quy-nhap-than-bai-3-thien-la-dia-vong-cho-quan-my-duoi-long-dat-cu-chi-post293888.html",
     "text": "Hai giếng cách nhau khoảng 6–15 m, đào xuống khoảng 3–4 m rồi hai người đào hướng vào nhau."}
  ],
  "levels": [
    {"id": "well-1", "prompt": "Chỉnh hướng đào để gặp đồng đội đang đào từ giếng bên kia.",
     "distanceM": 8, "angleMin": -60, "angleMax": 60, "targetAngle": -28, "tolerance": 8},
    {"id": "well-2", "prompt": "Giếng này lệch hướng khác. Lắng nghe tiếng cuốc để chỉnh.",
     "distanceM": 11, "angleMin": -60, "angleMax": 60, "targetAngle": 16, "tolerance": 8},
    {"id": "well-3", "prompt": "Hai giếng xa nhau hơn. Chỉ có 6 lần đào.",
     "distanceM": 14, "angleMin": -60, "angleMax": 60, "targetAngle": 44, "tolerance": 8}
  ]
}$json$
WHERE id = 'a1000001-0000-4000-8000-000000000003'::uuid;

-- 3e. Đếm dấu → đếm lỗ thông hơi ngụy trang trên nền lá (ảnh trong FE/public/images/minigames).
UPDATE quest_minigames
SET title = 'Đếm lỗ thông hơi',
    config = $json${
  "facts": [
    {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
     "text": "Dọc đường hầm có các lỗ thông hơi được ngụy trang kín đáo, đưa lên mặt đất qua nhiều cửa bí mật."}
  ],
  "levels": [
    {"id": "dem-1", "prompt": "Có bao nhiêu lỗ thông hơi lẫn trong lớp lá? Chạm để đánh dấu từng lỗ rồi chốt số.",
     "imageUrl": "/images/minigames/dem-lo-1.jpg", "answerCount": 4},
    {"id": "dem-2", "prompt": "Khu rừng khác, nhiều lỗ hơn. Đếm kỹ trước khi chốt.",
     "imageUrl": "/images/minigames/dem-lo-2.jpg", "answerCount": 7},
    {"id": "dem-3", "prompt": "Lượt cuối. Có lỗ bị lá che một phần.",
     "imageUrl": "/images/minigames/dem-lo-3.jpg", "answerCount": 5}
  ]
}$json$
WHERE id = 'a1000001-0000-4000-8000-000000000004'::uuid;

UPDATE quest_steps
SET title = 'Chương 4 · Đếm lỗ thông hơi'
WHERE id = '33333333-3333-3333-3333-333333333364'::uuid;

-- 3f. Im lặng: thêm dữ kiện có nguồn, lời dẫn theo tình huống lính tuần tra trên đầu.
UPDATE quest_minigames
SET config = $json${
  "facts": [
    {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
     "text": "Khi đóng nắp miệng hầm, kẻ thù đi trên mặt đất khó phát hiện được căn hầm."}
  ],
  "levels": [
    {"id": "im-1", "holdMs": 3000, "marginRatio": 1.8,
     "prompt": "Lính tuần tra đang đi ngang trên đầu. Giữ im lặng (hoặc giữ nút) cho tới khi tiếng bước chân đi qua."},
    {"id": "im-2", "holdMs": 3000, "marginRatio": 1.5,
     "prompt": "Toán lính dừng lại gần nắp hầm. Lần này phải im hơn nữa.",
     "fact": {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
              "text": "Cán bộ sống trong vùng địch ban ngày náu mình dưới hầm bí mật, ban đêm mới lên khỏi mặt đất."}},
    {"id": "im-3", "holdMs": 4000, "marginRatio": 1.5,
     "prompt": "Cuộc lùng sục kéo dài hơn. Giữ im lâu hơn.",
     "fact": {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
              "text": "Cán bộ sống trong vùng địch ban ngày náu mình dưới hầm bí mật, ban đêm mới lên khỏi mặt đất."}}
  ]
}$json$
WHERE id = 'a1000001-0000-4000-8000-000000000014'::uuid;

-- ---------------------------------------------------------------------------
-- 4. Lời giới thiệu trạm hiện cho người dùng: bỏ ghi chú nội bộ còn sót trong seed.
-- ---------------------------------------------------------------------------
UPDATE station_content_blocks
SET body = btrim(replace(replace(body,
        ' Nội dung chi tiết sẽ được BQL xác thực sau khảo sát Tuần 0.', ''),
        ' (nếu nằm trên tuyến tham quan)', ''))
WHERE body LIKE '%BQL xác thực sau khảo sát Tuần 0%'
   OR body LIKE '%(nếu nằm trên tuyến tham quan)%';

UPDATE story_chapters
SET synopsis = replace(synopsis, ' (nếu trên tuyến)', '')
WHERE synopsis LIKE '%(nếu trên tuyến)%';
