-- CP3 Week 1: Dữ kiện Củ Chi rút gọn từ tài liệu nội bộ (token-efficient, dùng cho Gemini/Ollama)
-- Nguồn: "Chiến dịch Cedar Falls (1967).pdf", "tim-hieu-ve-dia-dao-cu-chi-lich-su-vi-tri-va-vai-tro-quan-trong.pdf"

ALTER TABLE locations ADD COLUMN IF NOT EXISTS knowledge_context TEXT;

UPDATE locations
SET
  sources = 'Ban Quản lý Di tích Củ Chi; UNESCO Di sản Thế giới; Chiến dịch Cedar Falls (1967); Tìm hiểu Địa đạo Củ Chi (ĐH Sư phạm TP.HCM)',
  knowledge_context = $kc$[VI TRI] Huyện Củ Chi, TP.HCM. Hai khu mở cửa: Địa đạo Bến Dược (xã Phú Mỹ Hưng), Địa đạo Bến Đình (xã Nhuận Đức). Cách trung tâm TP khoảng 70km về Tây Bắc. Giáp Bình Dương, Long An, Tây Ninh, Hóc Môn.
[KICH THUOC] Hệ thống sâu 3-10m, tổng chiều dài khoảng 250km. Ba tầng: tầng 1 cách mặt đất ~3m (chống đạn pháo, xe tăng); tầng 2 ~5m (chống bom cỡ nhỏ); tầng 3 ~8-10m (an toàn nhất). Đường "xương sống" tỏa nhánh dài/ngắn, có nhánh tới sông Sài Gòn.
[LICH SU] 1948: đào tại Tân Phú Trung, Phước Vĩnh An (kháng Pháp). 1961-1965: phát triển mạnh chống Mỹ. 1965: hoàn thiện xương sống 6 xã phía Bắc. Công sức hơn 20 năm nhiều thế hệ.
[CONG TRINH] Hầm hội họp; hầm giải phẫu/quân y; kho lương thực-vũ khí; Bếp Hoàng Cầm (nấu ẩn khói: rãnh thoát khói, lá/cây che, giảm khói lộ); lỗ thông hơi và ổ chiến đấu (ngụy trang ụ mối); giếng ngầm (ví dụ sâu 15m); hầm chông.
[DAO HAM] Dụng cụ: cuốc, xẻng, bần tre. Kỹ thuật: giếng kính ~0,6m sâu 3m làm mốc, hai đội đào gặp nhau; nữ dân phi tang đất (hố bom ngập nước, sông, ụ mối, ruộng). Mùa khô đất cứng ~6m/ngày; mùa mưa nhanh hơn.
[CHIEN DICH CRIMP 1966] Mỹ huy động Sư đoàn bộ binh 1 càn quét Củ Chi; địa đạo giúp quân và dân chống trả, làm thất bại âm mưu.
[CHIEN DICH CEDAR FALLS 8-26/01/1967] Mỹ tấn công vùng Tam giác sắt (Bến Súc-Củ Chi-Bến Cát), ~30.000 quân, B-52, napalm, hóa học; bao vây, di dời dân, đốt phá Bến Súc; đội nữ du kích Củ Chi hy sinh anh dũng; hệ thống địa đạo vẫn đứng vững.
[QUY TAC TRA LOI] Chi dựa DỮ KIỆN trên. Khong bia so lieu, ngay thang, nhan vat ngoai tai lieu. Cau ngoai pham vi (ve may bay, chinh tri hien dai, dia danh khac...) tu choi lich su.$kc$
WHERE id = '11111111-1111-1111-1111-111111111111';
