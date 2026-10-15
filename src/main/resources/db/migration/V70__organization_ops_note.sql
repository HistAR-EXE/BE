ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS ops_note TEXT;

UPDATE organizations
SET ops_note = 'Đối tác chiến lược: gói ưu tiên bổ sung tài khoản hướng dẫn viên khảo sát đợt tiếp theo.'
WHERE slug = 'cu-chi-demo'
  AND (ops_note IS NULL OR BTRIM(ops_note) = '');

UPDATE organizations
SET ops_note = 'Tổ chức demo STANDARD — dùng cho kiểm thử roster và analytics admin.'
WHERE slug = 'demo-standard-org'
  AND (ops_note IS NULL OR BTRIM(ops_note) = '');
