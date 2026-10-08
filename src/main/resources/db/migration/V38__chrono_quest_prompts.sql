UPDATE quest_steps
SET title = 'Chương 1 · Hỏi Chrono về các tầng',
    objective = 'Nghe Chrono kể cấu trúc 3 tầng và độ sâu tầng sâu nhất.',
    description = 'Tầng sâu nhất sâu 8–10 m, tối đa 12 m.',
    hint = 'Gửi câu hỏi đã soạn. Chrono kể theo tư liệu.',
    action_label = 'Hỏi Chrono',
    chat_prompt = 'Chrono ơi, địa đạo Củ Chi có mấy tầng, tầng sâu nhất sâu bao nhiêu?'
WHERE unlock_key = 'dialogue:anh-ba'
  AND quest_id = '33333333-3333-3333-3333-333333333334';

UPDATE quest_steps
SET title = 'Chương 1 · Hỏi Chrono về bếp Hoàng Cầm',
    objective = 'Nghe Chrono kể cách Bếp Hoàng Cầm giấu khói.',
    description = 'Khói đi theo rãnh, lọc qua đất ẩm, thoát lên chỉ còn hơi như sương sớm.',
    hint = 'Gửi câu hỏi đã soạn cho Chrono.',
    action_label = 'Hỏi Chrono',
    chat_prompt = 'Chrono ơi, Bếp Hoàng Cầm dẫn khói theo rãnh và lọc qua đất ẩm như thế nào?'
WHERE unlock_key = 'dialogue:chi-nam'
  AND quest_id = '33333333-3333-3333-3333-333333333335';
