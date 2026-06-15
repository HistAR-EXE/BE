-- Fix typo: Đền Hùng Vướng → Đền Hùng Vương
UPDATE locations
SET name = 'Đền Hùng Vương'
WHERE id = '22222222-2222-2222-2222-222222222209'
   OR name ILIKE '%Hùng Vướng%';
