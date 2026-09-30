-- B5: pgvector RAG (verified sources -> chunks -> cosine search), station chat chips, offline FAQ, quality log, semantic cache.
--
-- HOST NOTE: CREATE EXTENSION vector needs the pgvector package installed on the Postgres host
-- (Supabase/Neon/Railway pgvector images: available; Render managed Postgres: enable it in the dashboard or
-- use a pgvector-capable instance). If this statement fails on a host without pgvector, install/enable the
-- extension first, then re-run Flyway. The app keeps working with rag.enabled=false (default) as long as the
-- tables exist; embeddings are nullable and search always filters `embedding IS NOT NULL`.
CREATE EXTENSION IF NOT EXISTS vector;

-- ---------------------------------------------------------------------------
-- Verified sources (only rows with verified_at NOT NULL are retrievable by the RAG service)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rag_sources (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title        VARCHAR(255) NOT NULL,
    license      VARCHAR(128),
    verified_by  VARCHAR(128),
    verified_at  TIMESTAMPTZ,
    era          VARCHAR(64),
    station_code VARCHAR(32),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rag_sources_station ON rag_sources (station_code);

CREATE TABLE IF NOT EXISTS rag_chunks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id    UUID NOT NULL REFERENCES rag_sources (id) ON DELETE CASCADE,
    station_code VARCHAR(32),
    era          VARCHAR(64),
    content      TEXT NOT NULL,
    embedding    vector(768),
    chunk_index  INT  NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_rag_chunks_source  ON rag_chunks (source_id);
CREATE INDEX IF NOT EXISTS idx_rag_chunks_station ON rag_chunks (station_code);
-- Pilot corpus is tiny (hundreds of chunks): exact scan is fast and exact. Add an HNSW index when it grows:
--   CREATE INDEX idx_rag_chunks_embedding ON rag_chunks USING hnsw (embedding vector_cosine_ops);

-- ---------------------------------------------------------------------------
-- Suggested question chips per station / persona
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS station_chat_prompts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_code     VARCHAR(64)  NOT NULL DEFAULT 'cu-chi',
    station_code  VARCHAR(32)  NOT NULL,
    persona       VARCHAR(64)  NOT NULL DEFAULT 'chi-nam',
    chip_label    VARCHAR(80)  NOT NULL,
    question_text VARCHAR(500) NOT NULL,
    sort_order    INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_station_chat_prompts UNIQUE (site_code, station_code, persona, sort_order)
);

CREATE INDEX IF NOT EXISTS idx_station_chat_prompts_lookup
    ON station_chat_prompts (site_code, station_code, persona, sort_order);

-- ---------------------------------------------------------------------------
-- Quality log (no raw question text: only a hash) and semantic cache
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_quality_log (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_hash VARCHAR(64) NOT NULL,
    station_code  VARCHAR(32),
    chunks_used   INT         NOT NULL DEFAULT 0,
    had_citation  BOOLEAN     NOT NULL DEFAULT FALSE,
    latency_ms    INT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_quality_log_created ON chat_quality_log (created_at);

CREATE TABLE IF NOT EXISTS semantic_cache (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    embedding    vector(768),
    station_code VARCHAR(32),
    persona      VARCHAR(64) NOT NULL,
    reply_text   TEXT        NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_semantic_cache_expires ON semantic_cache (expires_at);

-- ---------------------------------------------------------------------------
-- Offline FAQ (shipped in the full offline pack as faq_offline.json)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS faq_offline (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_code    VARCHAR(64) NOT NULL DEFAULT 'cu-chi',
    station_code VARCHAR(32),
    question     VARCHAR(500) NOT NULL,
    answer       TEXT         NOT NULL,
    source_refs  TEXT
);

CREATE INDEX IF NOT EXISTS idx_faq_offline_site_station ON faq_offline (site_code, station_code);

-- ---------------------------------------------------------------------------
-- Seed: 3 chips per station ST01-ST06 for persona chi-nam (Cu Chi themed)
-- ---------------------------------------------------------------------------
INSERT INTO station_chat_prompts (id, site_code, station_code, persona, chip_label, question_text, sort_order) VALUES
(md5('chip:cu-chi:ST01:chi-nam:1')::uuid, 'cu-chi', 'ST01', 'chi-nam', 'Bến Dược là gì?',            'Chị Năm ơi, Bến Dược là nơi như thế nào và vì sao nổi tiếng?', 1),
(md5('chip:cu-chi:ST01:chi-nam:2')::uuid, 'cu-chi', 'ST01', 'chi-nam', 'Nhận nhiệm vụ đầu tiên',     'Khi mới đến Bến Dược, người chiến sĩ thường nhận nhiệm vụ gì?', 2),
(md5('chip:cu-chi:ST01:chi-nam:3')::uuid, 'cu-chi', 'ST01', 'chi-nam', 'Cuộc sống nơi đây',         'Cuộc sống hằng ngày của bà con và du kích ở vùng đất thép này ra sao?', 3),

(md5('chip:cu-chi:ST02:chi-nam:1')::uuid, 'cu-chi', 'ST02', 'chi-nam', 'Địa đạo có mấy tầng?',      'Địa đạo Củ Chi có mấy tầng và sâu bao nhiêu hả Chị Năm?', 1),
(md5('chip:cu-chi:ST02:chi-nam:2')::uuid, 'cu-chi', 'ST02', 'chi-nam', 'Vì sao lối xuống nhỏ thế?', 'Vì sao miệng hầm và lối đi trong địa đạo lại nhỏ và hẹp như vậy?', 2),
(md5('chip:cu-chi:ST02:chi-nam:3')::uuid, 'cu-chi', 'ST02', 'chi-nam', 'Sống dưới lòng đất',      'Người ta sinh hoạt, ăn ở và liên lạc thế nào khi sống dưới địa đạo?', 3),

(md5('chip:cu-chi:ST03:chi-nam:1')::uuid, 'cu-chi', 'ST03', 'chi-nam', 'Bếp Hoàng Cầm là gì?',      'Bếp Hoàng Cầm là loại bếp gì và vì sao lại đặc biệt?', 1),
(md5('chip:cu-chi:ST03:chi-nam:2')::uuid, 'cu-chi', 'ST03', 'chi-nam', 'Nấu ăn không bị lộ khói',   'Làm sao để nấu ăn dưới địa đạo mà khói không bị phát hiện?', 2),
(md5('chip:cu-chi:ST03:chi-nam:3')::uuid, 'cu-chi', 'ST03', 'chi-nam', 'Bữa cơm của du kích',       'Bữa cơm của các chiến sĩ thời đó thường có những món gì?', 3),

(md5('chip:cu-chi:ST04:chi-nam:1')::uuid, 'cu-chi', 'ST04', 'chi-nam', 'Giếng nước để làm gì?',     'Giếng nước ở đây có vai trò gì đối với người sống trong địa đạo?', 1),
(md5('chip:cu-chi:ST04:chi-nam:2')::uuid, 'cu-chi', 'ST04', 'chi-nam', 'Lấy nước có nguy hiểm không?', 'Việc lấy nước ở đây có nguy hiểm gì và phải giữ bí mật ra sao?', 2),
(md5('chip:cu-chi:ST04:chi-nam:3')::uuid, 'cu-chi', 'ST04', 'chi-nam', 'Nước quý đến mức nào?',    'Vì sao mỗi giọt nước lại quý đến vậy trong những năm chiến tranh?', 3),

(md5('chip:cu-chi:ST05:chi-nam:1')::uuid, 'cu-chi', 'ST05', 'chi-nam', 'Phòng họp dùng làm gì?',    'Phòng họp dưới địa đạo dùng để làm gì hả Chị Năm?', 1),
(md5('chip:cu-chi:ST05:chi-nam:2')::uuid, 'cu-chi', 'ST05', 'chi-nam', 'Họp dưới ánh đèn dầu',      'Các cuộc họp chỉ huy diễn ra thế nào trong điều kiện thiếu sáng và chật hẹp?', 2),
(md5('chip:cu-chi:ST05:chi-nam:3')::uuid, 'cu-chi', 'ST05', 'chi-nam', 'Quyết định quan trọng',     'Những quyết định quan trọng nào từng được bàn bạc ở những nơi như thế này?', 3),

(md5('chip:cu-chi:ST06:chi-nam:1')::uuid, 'cu-chi', 'ST06', 'chi-nam', 'Tóm tắt hành trình',        'Chị Năm tóm tắt giúp em hành trình một ngày dưới lòng đất nhé!', 1),
(md5('chip:cu-chi:ST06:chi-nam:2')::uuid, 'cu-chi', 'ST06', 'chi-nam', 'Ý nghĩa của Củ Chi',        'Địa đạo Củ Chi có ý nghĩa gì với lịch sử dân tộc?', 2),
(md5('chip:cu-chi:ST06:chi-nam:3')::uuid, 'cu-chi', 'ST06', 'chi-nam', 'Nên đi tiếp đâu?',          'Sau chuyến tham quan này em nên tìm hiểu thêm điều gì về Củ Chi?', 3)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Seed: offline FAQ. PLACEHOLDER content (tone + structure); every answer must be re-verified against approved
-- sources (see docs/RAG_PILOT_GATE.md) before the public pilot.
-- ---------------------------------------------------------------------------
INSERT INTO faq_offline (id, site_code, station_code, question, answer, source_refs) VALUES
(md5('faq:cu-chi:global:1')::uuid, 'cu-chi', NULL, 'Địa đạo Củ Chi là gì?',
 'Địa đạo Củ Chi là hệ thống đường hầm ngầm ở vùng Củ Chi (TP. Hồ Chí Minh), được đào và mở rộng trong các cuộc kháng chiến để trú ẩn, liên lạc, sinh hoạt và chiến đấu.',
 'placeholder:cu-chi-overview (chờ xác minh)'),
(md5('faq:cu-chi:global:2')::uuid, 'cu-chi', NULL, 'Hệ thống địa đạo dài bao nhiêu?',
 'Các tài liệu thường nêu tổng chiều dài khoảng 250 km. Con số có thể khác nhau tùy nguồn và thời điểm thống kê nên cần đối chiếu với tài liệu chính thức của Ban quản lý di tích.',
 'placeholder:cu-chi-overview (chờ xác minh)'),
(md5('faq:cu-chi:global:3')::uuid, 'cu-chi', NULL, 'Địa đạo có mấy tầng?',
 'Nhiều đoạn địa đạo được đào thành nhiều tầng ở các độ sâu khác nhau (thường được mô tả khoảng 3 m, 6 m và 8–10 m) để tránh bom đạn và tăng khả năng ẩn náu.',
 'placeholder:cu-chi-overview (chờ xác minh)'),
(md5('faq:cu-chi:global:4')::uuid, 'cu-chi', NULL, 'Nên chuẩn bị gì khi vào địa đạo?',
 'Mang giày thoải mái, nước uống, đèn pin hoặc điện thoại đã sạc pin. Lối đi hẹp, thấp, nóng và ẩm; nếu sợ không gian kín bạn có thể chọn đoạn ngắn hoặc đi ra ở các lối thoát.',
 'placeholder:visitor-tips (chờ xác minh)'),
(md5('faq:cu-chi:global:5')::uuid, 'cu-chi', NULL, 'Mất sóng trong địa đạo thì dùng ứng dụng thế nào?',
 'Hãy vào mục "Chuẩn bị hành trang" và tải gói Lite/Full khi còn Wi-Fi. Ảnh, bản đồ và các câu hỏi thường gặp này sẽ dùng được offline; chat AI cần có kết nối.',
 'placeholder:app-offline-guide'),

(md5('faq:cu-chi:ST01:1')::uuid, 'cu-chi', 'ST01', 'Bến Dược ở đâu?',
 'Khu di tích Địa đạo Bến Dược thuộc huyện Củ Chi, TP. Hồ Chí Minh, là một trong hai khu di tích địa đạo được đón khách tham quan (cùng với Bến Đình).',
 'placeholder:ben-duoc (chờ xác minh)'),
(md5('faq:cu-chi:ST01:2')::uuid, 'cu-chi', 'ST01', 'Ở Bến Dược có thể xem những gì?',
 'Du khách có thể xem các đoạn địa đạo, hầm sinh hoạt, hầm chông, khu tái hiện đời sống và chiến đấu của quân dân Củ Chi.',
 'placeholder:ben-duoc (chờ xác minh)'),

(md5('faq:cu-chi:ST02:1')::uuid, 'cu-chi', 'ST02', 'Vì sao miệng hầm lại nhỏ như vậy?',
 'Miệng hầm được làm nhỏ và ngụy trang kỹ để khó bị phát hiện từ trên mặt đất; kích thước nhỏ cũng giúp người trong hầm dễ phòng thủ.',
 'placeholder:dia-dao (chờ xác minh)'),
(md5('faq:cu-chi:ST02:2')::uuid, 'cu-chi', 'ST02', 'Bên trong địa đạo có những khu vực nào?',
 'Bên cạnh đường hầm chính còn có các khu nghỉ ngơi, hội họp, bếp, hầm cứu thương và giếng nước, được nối với nhau bằng các nhánh hầm.',
 'placeholder:dia-dao (chờ xác minh)'),

(md5('faq:cu-chi:ST03:1')::uuid, 'cu-chi', 'ST03', 'Bếp Hoàng Cầm là gì?',
 'Bếp Hoàng Cầm là kiểu bếp nấu ăn hạn chế khói: khói được dẫn qua hệ thống rãnh dài rồi thoát ra ngoài, giúp đơn vị hạn chế bị phát hiện. Bếp mang tên người sáng tạo ra nó.',
 'placeholder:bep-hoang-cam (chờ xác minh)'),
(md5('faq:cu-chi:ST03:2')::uuid, 'cu-chi', 'ST03', 'Vì sao phải nấu ăn kín đáo như vậy?',
 'Khói bếp là dấu hiệu dễ bị máy bay và lực lượng đối phương phát hiện, nên việc giảm khói và chọn thời điểm nấu phù hợp là yếu tố sống còn.',
 'placeholder:bep-hoang-cam (chờ xác minh)'),

(md5('faq:cu-chi:ST04:1')::uuid, 'cu-chi', 'ST04', 'Giếng nước có vai trò gì?',
 'Giếng là nguồn nước sinh hoạt cho người sống và chiến đấu trong khu vực địa đạo, vì vậy vị trí giếng thường được giữ bí mật và bảo vệ cẩn thận.',
 'placeholder:gieng-nuoc (chờ xác minh)'),
(md5('faq:cu-chi:ST04:2')::uuid, 'cu-chi', 'ST04', 'Vì sao nước lại quý ở đây?',
 'Vùng đất cao, khô vào mùa nắng nên việc có nước sạch khó khăn; lấy nước lại dễ bị lộ vị trí nên mỗi lần lấy nước đều phải tính toán thận trọng.',
 'placeholder:gieng-nuoc (chờ xác minh)'),

(md5('faq:cu-chi:ST05:1')::uuid, 'cu-chi', 'ST05', 'Phòng họp dưới địa đạo dùng làm gì?',
 'Đây là nơi cán bộ, chỉ huy bàn kế hoạch tác chiến, phân công nhiệm vụ và thông báo tình hình cho các đơn vị.',
 'placeholder:phong-hop (chờ xác minh)'),
(md5('faq:cu-chi:ST05:2')::uuid, 'cu-chi', 'ST05', 'Ánh sáng và không khí dưới hầm thế nào?',
 'Hầm sử dụng đèn dầu và các lỗ thông hơi được ngụy trang để lấy không khí; điều kiện vẫn rất chật chội, thiếu sáng và ngột ngạt.',
 'placeholder:phong-hop (chờ xác minh)'),

(md5('faq:cu-chi:ST06:1')::uuid, 'cu-chi', 'ST06', 'Toàn bộ hành trình kéo dài bao lâu?',
 'Tùy nhịp tham quan, cả hành trình sáu trạm thường mất khoảng 1–2 giờ. Đây là ước tính cho trải nghiệm trong app, không phải số liệu chính thức của khu di tích.',
 'placeholder:app-tour-estimate')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Seed: 2 placeholder sources + chunks WITHOUT embeddings (embedding NULL).
-- verified_at is NULL on purpose: the search query only returns verified sources, so placeholders are never served.
-- scripts/rag-ingest-approved.ps1 replaces them (same title) with approved markdown + embeddings.
-- ---------------------------------------------------------------------------
INSERT INTO rag_sources (id, title, license, verified_by, verified_at, era, station_code) VALUES
('a5000001-0000-4000-8000-000000000001', 'Tổng quan Địa đạo Củ Chi (mẫu)', 'internal-draft', NULL, NULL, '1946-1975', NULL),
('a5000001-0000-4000-8000-000000000002', 'Bếp Hoàng Cầm (mẫu)',            'internal-draft', NULL, NULL, '1946-1975', 'ST03')
ON CONFLICT (id) DO NOTHING;

INSERT INTO rag_chunks (id, source_id, station_code, era, content, chunk_index) VALUES
(md5('chunk:a5000001-1:0')::uuid, 'a5000001-0000-4000-8000-000000000001', NULL, '1946-1975',
 'Địa đạo Củ Chi là hệ thống đường hầm ngầm ở vùng Củ Chi, được đào và mở rộng trong các cuộc kháng chiến để trú ẩn, liên lạc và chiến đấu.', 0),
(md5('chunk:a5000001-1:1')::uuid, 'a5000001-0000-4000-8000-000000000001', NULL, '1946-1975',
 'Nhiều đoạn địa đạo có cấu trúc nhiều tầng, kèm theo các khu sinh hoạt, hội họp, bếp, hầm cứu thương và giếng nước.', 1),
(md5('chunk:a5000001-2:0')::uuid, 'a5000001-0000-4000-8000-000000000002', 'ST03', '1946-1975',
 'Bếp Hoàng Cầm là kiểu bếp nấu ăn hạn chế khói, khói được dẫn qua rãnh dài để tản ra nhằm giảm nguy cơ bị phát hiện.', 0),
(md5('chunk:a5000001-2:1')::uuid, 'a5000001-0000-4000-8000-000000000002', 'ST03', '1946-1975',
 'Việc nấu ăn thường được tính toán về thời điểm và vị trí để hạn chế dấu hiệu khói bếp lộ ra mặt đất.', 1)
ON CONFLICT (id) DO NOTHING;
