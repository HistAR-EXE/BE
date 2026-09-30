-- A3: Củ Chi pilot stations + content blocks.
CREATE TABLE IF NOT EXISTS stations (
    id             UUID PRIMARY KEY,
    site_code      VARCHAR(64)  NOT NULL DEFAULT 'cu-chi',
    code           VARCHAR(32)  NOT NULL,
    name           VARCHAR(255) NOT NULL,
    sort_order     INT          NOT NULL DEFAULT 0,
    lat            DOUBLE PRECISION,
    lng            DOUBLE PRECISION,
    quest_step_key VARCHAR(128),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stations_site_code UNIQUE (site_code, code)
);

CREATE TABLE IF NOT EXISTS station_content_blocks (
    id         UUID PRIMARY KEY,
    station_id UUID         NOT NULL REFERENCES stations (id) ON DELETE CASCADE,
    block_type VARCHAR(16)  NOT NULL,
    title      VARCHAR(255),
    body       TEXT,
    media_url  VARCHAR(1024),
    sort_order INT          NOT NULL DEFAULT 0,
    meta_json  TEXT,
    CONSTRAINT chk_station_block_type CHECK (block_type IN ('TEXT', 'IMAGE', 'AUDIO', 'VIDEO', 'HOTSPOT'))
);

CREATE INDEX IF NOT EXISTS idx_station_content_blocks_station
    ON station_content_blocks (station_id, sort_order);

INSERT INTO stations (id, site_code, code, name, sort_order, quest_step_key, is_active) VALUES
('a3000001-0000-4000-8000-000000000001', 'cu-chi', 'ST01', 'Bến Dược', 1, 'cu-chi:ST01', TRUE),
('a3000001-0000-4000-8000-000000000002', 'cu-chi', 'ST02', 'Địa đạo', 2, 'cu-chi:ST02', TRUE),
('a3000001-0000-4000-8000-000000000003', 'cu-chi', 'ST03', 'Bếp Hoàng Cầm', 3, 'cu-chi:ST03', TRUE),
('a3000001-0000-4000-8000-000000000004', 'cu-chi', 'ST04', 'Giếng nước', 4, 'cu-chi:ST04', TRUE),
('a3000001-0000-4000-8000-000000000005', 'cu-chi', 'ST05', 'Phòng họp', 5, 'cu-chi:ST05', TRUE),
('a3000001-0000-4000-8000-000000000006', 'cu-chi', 'ST06', 'Xuất phát', 6, 'cu-chi:ST06', TRUE)
ON CONFLICT (site_code, code) DO NOTHING;

INSERT INTO station_content_blocks (id, station_id, block_type, title, body, sort_order) VALUES
('a3000002-0000-4000-8000-000000000001', 'a3000001-0000-4000-8000-000000000001', 'TEXT', 'Giới thiệu Bến Dược',
 'Nội dung giới thiệu tạm thời cho trạm Bến Dược. Sẽ được cập nhật bởi đội nội dung.', 1),
('a3000002-0000-4000-8000-000000000002', 'a3000001-0000-4000-8000-000000000002', 'TEXT', 'Giới thiệu Địa đạo',
 'Nội dung giới thiệu tạm thời cho trạm Địa đạo. Sẽ được cập nhật bởi đội nội dung.', 1),
('a3000002-0000-4000-8000-000000000003', 'a3000001-0000-4000-8000-000000000003', 'TEXT', 'Giới thiệu Bếp Hoàng Cầm',
 'Nội dung giới thiệu tạm thời cho trạm Bếp Hoàng Cầm. Sẽ được cập nhật bởi đội nội dung.', 1),
('a3000002-0000-4000-8000-000000000004', 'a3000001-0000-4000-8000-000000000004', 'TEXT', 'Giới thiệu Giếng nước',
 'Nội dung giới thiệu tạm thời cho trạm Giếng nước. Sẽ được cập nhật bởi đội nội dung.', 1),
('a3000002-0000-4000-8000-000000000005', 'a3000001-0000-4000-8000-000000000005', 'TEXT', 'Giới thiệu Phòng họp',
 'Nội dung giới thiệu tạm thời cho trạm Phòng họp. Sẽ được cập nhật bởi đội nội dung.', 1),
('a3000002-0000-4000-8000-000000000006', 'a3000001-0000-4000-8000-000000000006', 'TEXT', 'Giới thiệu Xuất phát',
 'Nội dung giới thiệu tạm thời cho trạm Xuất phát. Sẽ được cập nhật bởi đội nội dung.', 1)
ON CONFLICT (id) DO NOTHING;
