-- Test-runtime-only fixtures for live loopback RAG acceptance. Never packaged in production.
INSERT INTO locations (
    id, name, description, latitude, longitude, city, site_code, sources,
    knowledge_context, created_at
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Địa đạo Củ Chi',
    'Disposable H2 fixture for live RAG acceptance.',
    11.1417, 106.4625, 'TP.HCM', 'cu-chi', NULL, NULL, CURRENT_TIMESTAMP
);

INSERT INTO characters (
    id, location_id, name, era, chat_enabled, persona_prompt, portrait_url
) VALUES (
    'a1000001-0000-4000-8000-00000000c001',
    '11111111-1111-1111-1111-111111111111',
    'Chrono — Trợ lý TimeLens',
    'TimeLens',
    TRUE,
    'Chỉ trả lời từ tư liệu đã truy xuất; không tự tạo nguồn.',
    '/media/mascot/mascot-5.png'
);
