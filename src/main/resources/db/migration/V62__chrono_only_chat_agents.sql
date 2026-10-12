-- Keep historical characters and their conversations intact, but expose only
-- Chrono as the technical chat agent for new conversations.
ALTER TABLE characters
    ADD COLUMN IF NOT EXISTS chat_enabled BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE characters
SET chat_enabled = FALSE
WHERE name <> 'Chrono — Trợ lý TimeLens'
   OR era IS DISTINCT FROM 'TimeLens';

CREATE UNIQUE INDEX IF NOT EXISTS ux_characters_enabled_chat_agent_per_location
    ON characters (location_id)
    WHERE chat_enabled;

INSERT INTO characters (location_id, name, era, persona_prompt, portrait_url, chat_enabled)
SELECT
    locations.id,
    'Chrono — Trợ lý TimeLens',
    'TimeLens',
    'Bạn là Chrono, Trợ lý TimeLens. Chỉ trả lời bằng thông tin có trong tư liệu và nguồn được cung cấp. Phân biệt rõ fact đã kiểm chứng với điều chưa đủ dữ liệu, không tự tạo nguồn, không hướng dẫn hành vi nguy hiểm, và không tiết lộ dữ liệu hay lịch sử của người dùng khác.',
    '/media/mascot/mascot-5.png',
    TRUE
FROM locations
ON CONFLICT (location_id) WHERE chat_enabled
DO UPDATE SET
    name = EXCLUDED.name,
    era = EXCLUDED.era,
    persona_prompt = EXCLUDED.persona_prompt,
    portrait_url = EXCLUDED.portrait_url,
    chat_enabled = TRUE;

-- Suggested prompts are part of the chat surface too.  Preserve the records,
-- but make the canonical Chrono key and wording available to every client.
DELETE FROM station_chat_prompts legacy
USING station_chat_prompts canonical
WHERE legacy.persona <> 'chrono'
  AND canonical.persona = 'chrono'
  AND canonical.site_code = legacy.site_code
  AND canonical.station_code = legacy.station_code
  AND canonical.sort_order = legacy.sort_order;

UPDATE station_chat_prompts
SET persona = 'chrono',
    question_text = replace(question_text, 'Chị Năm', 'Chrono')
WHERE persona <> 'chrono';
