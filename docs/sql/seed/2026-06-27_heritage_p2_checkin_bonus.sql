-- P2: heritage onsite check-in bonus XP + per-site "Đã đến nơi" badges + real Then/Now photo_pairs
BEGIN;

ALTER TABLE badges ADD COLUMN IF NOT EXISTS location_id UUID REFERENCES locations(id) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS user_heritage_onsite_bonus (
    user_id     UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    location_id UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    xp_awarded  INT NOT NULL DEFAULT 25,
    awarded_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, location_id)
);

-- Per-site onsite seal badges (condition: heritage_onsite + location_id)
INSERT INTO badges (id, name, description, icon_url, condition_type, condition_value, location_id) VALUES
('e0000001-0000-4000-8000-000000000201', 'Đã đến nơi · Bến Nhà Rồng', 'Check-in tại Bến Nhà Rồng sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222201'),
('e0000001-0000-4000-8000-000000000202', 'Đã đến nơi · Chùa Thiên Mụ', 'Check-in tại Chùa Thiên Mụ sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222202'),
('e0000001-0000-4000-8000-000000000203', 'Đã đến nơi · Cố đô Hoa Lư', 'Check-in tại Cố đô Hoa Lư sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222203'),
('e0000001-0000-4000-8000-000000000204', 'Đã đến nơi · Hoàng Thành', 'Check-in tại Hoàng Thành Thăng Long sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222204'),
('e0000001-0000-4000-8000-000000000205', 'Đã đến nơi · Hội An', 'Check-in tại Phố cổ Hội An sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222205'),
('e0000001-0000-4000-8000-000000000206', 'Đã đến nơi · Thành Nhà Hồ', 'Check-in tại Thành Nhà Hồ sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222206'),
('e0000001-0000-4000-8000-000000000207', 'Đã đến nơi · Văn Miếu', 'Check-in tại Văn Miếu sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222207'),
('e0000001-0000-4000-8000-000000000208', 'Đã đến nơi · Đại Nội Huế', 'Check-in tại Đại Nội Huế sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222208'),
('e0000001-0000-4000-8000-000000000209', 'Đã đến nơi · Đền Hùng', 'Check-in tại Đền Hùng sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222209')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  icon_url = EXCLUDED.icon_url,
  condition_type = EXCLUDED.condition_type,
  condition_value = EXCLUDED.condition_value,
  location_id = EXCLUDED.location_id;

-- Real Then/Now pairs (distinct historical vs present imagery per site)
DELETE FROM photo_pairs
WHERE location_id IN (
  '22222222-2222-2222-2222-222222222201',
  '22222222-2222-2222-2222-222222222202',
  '22222222-2222-2222-2222-222222222203',
  '22222222-2222-2222-2222-222222222204',
  '22222222-2222-2222-2222-222222222205',
  '22222222-2222-2222-2222-222222222206',
  '22222222-2222-2222-2222-222222222207',
  '22222222-2222-2222-2222-222222222208',
  '22222222-2222-2222-2222-222222222209'
);

INSERT INTO photo_pairs (location_id, historical_image, current_image, year, caption, sort_order) VALUES
('22222222-2222-2222-2222-222222222201', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAjLWg45sHfWuxzwOXD11fsIp1wol7YkpA93fnuNpVqzElVB4cq0xhaEVr6i-os7ogOT7f92CtQGQ7gdFjv8LNpC2ytFZGSczSzVq1FaWjLK3eYseKyjkyfL35EuR1sAf_s58Hj5HuOAdp6-tnvOIFvmWCMXYJ2vNRBN1-PyMO5TDmheH9_AppuXQd9WrX4loB21kVTLLyOyd_WbMecxaXFaEqGJKy7FO078n1Yf6D755FJtQpuzmNv-WdgGMqAB_rpzn9DnkzcQjMF', 1911, 'Bến Nhà Rồng — bến cảng xưa và nay', 1),
('22222222-2222-2222-2222-222222222202', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuBO-UNu_GKC60dQ4L1bJxsTQ90dO4MWtIEvZAwYM3Qg171BbOAO5xzCrk6SzxGfQpnA9TRFor3k7Xe-KU8v1RqEl2kW-uteDhKe5lCaSpJ8abetRUKpfJ1CfYG821e_xg6nHyIPler32hZPF58fO1nZKU0YAM1CMSqME3ZF7nzA7jYYJFA8xdc_PV-nvsvvcS9PqP2ptuYh-an4GG1Oi6dONhDfS72A-wmFhhopt-PuSM3BE3NYJJlrVEoPwtOXJMUiIafde493Kzuy', 1844, 'Chùa Thiên Mụ — xưa và nay', 1),
('22222222-2222-2222-2222-222222222203', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuCTOrzxLL7GTvS8QqJbZIcC6-3kdMUNc2qVuGsm6Py8ILXlr4dQA9lBclCQclZ4nR29DUai3kT_EZ0DGPSv5r7TYsAHDO1Gqh7U1ydlAHDdcsabbDsaO8lx4NqQJdybI5MAfxk00ku9guojpAE8Di8n8_LTIo3dXxsS7h16Irxb2xLmSlveMdmvAzdeUKiqxzVT1gj6FQ6ydQpoyUbVlRbqFPcr0Sr2WwZ9wYoYsWftxeYFZLD6mysBhJ4LWDOIl7Nzb3BT65wwOuNQ', 968, 'Cố đô Hoa Lư — xưa và nay', 1),
('22222222-2222-2222-2222-222222222204', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuCtlazSyN49NMSznx4q_qIX0cqxcv62yFHJVbZ30w2DU9rpKRBzc7frzufipWZRmQq5Ov3zFVQB2CwE5crbcFSEMPsOetdhEiemM-YXFa1qhBoourRNMAKIEF7TiPxbU2Q8Rk4J9SDrBGwZC9v9a4_vCBLGGMRNoF3OgGYO2vULBVyKts8t9NXaxmTPDCSUfcw1cAYRXe2qnl7K5nJRHQ6P9nzUpFXOlLwTFvXjRBTjLNQfiWAXKnyNWavouncL-8PVDGEIpgA2lW49', 1010, 'Hoàng Thành Thăng Long — xưa và nay', 1),
('22222222-2222-2222-2222-222222222205', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAqpI-gTLL99nlJth0gz0khOghupmSDWDg5PnGghGIZRtj3zWCluJ4VEf10pvI9-IIoSzs2TqPGwVvhkq28gZ_NH0-o9GOgybv3uEoMS_bqkNdmKVDieQI0XVmr-VbksO6WqB84xzEJTI0ZMYHGStwEikW3FMwm1uOaiqJ-La-WcKTgP-pviKHHPFWXGBjMcm4LvsQpxHzPQw7mXx1GJEAUJtGrjE0Md5CumRsfqpk4vmDXBVYeZkM3T7MUyLC7PeDTW2ebUineVjvB', 1600, 'Phố cổ Hội An — xưa và nay', 1),
('22222222-2222-2222-2222-222222222206', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuBDydkSCyfuTv0QZwuRlZTRSzGdQhQTZwc-NH2mSfkHDV5MVEPaxXcI30FVdAett-CGd9JowEM0X-NuukVppUYjTGQp-xI0XdiZ-S4aZMbCgRJKH8PiKopDUD90uGcIlEGK7uZvspFdLU23zfKBKOgNtKKRMnk7kSBImvp7EOdnahhWYghOeYu-Bmi13XEUY7OprZMEDbEOfP2OOFfjjz_rd3DN9ZqS8lD_sfeZJ2woDaP-9kk7ds1v3cR42ihdcK6CIz4oKVjzx9lo', 1397, 'Thành Nhà Hồ — xưa và nay', 1),
('22222222-2222-2222-2222-222222222207', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAxqMuKQE7yaBYgoARD2p7iFeUE3t_ZLcY6txP8x4mv0uKrSskfSH5ygnZ3INbt63snbS2GSmhdlwbpWacgIQ_8oj0kap8YpsqSE6o73SDS1orZIJVOiWpz-qU7iGV_G9pmnFM_lPaCN0gA2v-_Ec6fNywAnEdJtsw-zYIfgs8jOVWVr2hCLui0KVYJdvXB9qv_PTXiTSl4-GEWwKkrF3AD43ElE_JqaXowOXFNwlA6BEdj2CWhdsGq3O8xLeuBhD14kx2hP9suLd5o', 1070, 'Văn Miếu — xưa và nay', 1),
('22222222-2222-2222-2222-222222222208', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAc8mKVcLTVTDnMzvyQGag3DjLMzJtLi8a8xMlmLzyHao_nUe9-Eaahie-NgxUG3lpPwGksZdwsHz733qOUi107c1JXCnwQWJwlAjPg2bIR3m-_aucswEu33aBeO1v7U_9nEGlA2mmPcOBGb16TyqEqdQniS4Pb8DrvcoG3MoMpFTpkxvVuvPw_buU1rNHdPq4CnFuFX5WZugGfWIXW-l9wd7dUoVwD8QnJRFFhjt4wtuwt0-krlGuqFH_-VCPHyDeqCIItEJA5XhEn', 1802, 'Đại Nội Huế — xưa và nay', 1),
('22222222-2222-2222-2222-222222222209', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuB8IQqPU5BN2MjIDXg5Ch2nDliGa7nxbjtIoZbQai9CKotO_l5f_REDG1IFKo9RgqKck9KD9tK-pzVLXz0Uc0zIO0HtQ1phK8X6I7ewZYrfLVbQFN9MST18bUvlxDY36XEon7LcT0g2McBgHv5QmXcPXtS7zldrHsKhHsfL2tT-MNe8g6bOQFiC4f5Gkn_nSSgsh1UGVMsk63f6vsKaPySPdoT0MVeFraC2f1iLbD2HLM_lwSil7oIDcKRnQecxFTYFiWcBM1FWfLrV', 2879, 'Đền Hùng — xưa và nay', 1);

COMMIT;
