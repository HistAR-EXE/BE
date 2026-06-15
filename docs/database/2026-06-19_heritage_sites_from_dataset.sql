-- Heritage sites: FULL content from Dataset docx (locations + parsed artifacts + stories)
BEGIN;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222201', 'Bến Nhà Rồng', '1. Tổng quan & Vị trí
Vị trí: Nằm tại số 01 tòa nhà Nguyễn Tất Thành, Phường 12, Quận 4, TP. Hồ Chí Minh, ngay ngã ba sông Sài Gòn và kênh Bến Nghé.
Tên gọi chính thức hiện nay: Bảo tàng Hồ Chí Minh – Chi nhánh TP. Hồ Chí Minh.
Tầm vóc: Đây là một cụm di tích kiến trúc - lịch sử đặc biệt. Nơi đây không chỉ lưu giữ cấu trúc thương cảng kiểu Pháp bề thế mà còn là "địa chỉ đỏ" gìn giữ hơn 20.000 tài liệu, hiện vật về cuộc đời và sự nghiệp cách mạng của Bác Hồ.
2. Lịch sử hình thành: Từ trụ sở thương cảng đến chứng nhân lịch sử
Xây dựng (1862 - 1863): Sau khi chiếm được Gia Định, thực dân Pháp cho xây dựng tòa nhà này để làm trụ sở cho Hãng vận tải đường biển Hoàng gia Pháp (Messageries Maritimes). Đây là một trong những công trình kiến trúc phương Tây đầu tiên được xây dựng tại Sài Gòn.
Nguồn gốc tên gọi "Nhà Rồng": Tòa nhà được xây theo kiến trúc phương Tây nhưng trên đỉnh mái lại được gắn hai con rồng lớn bằng đất nung tráng men chầu mặt trăng theo mô-típ "Lưỡng long chầu nguyệt" truyền thống của kiến trúc đình chùa Việt Nam. Sự kết hợp độc đáo này khiến người dân Sài Gòn lúc bấy giờ quen gọi tòa nhà là "Nhà Rồng" và bến cảng xung quanh là "Bến Nhà Rồng".
Sự kiện lịch sử vĩ đại (05/06/1911): Trên con tàu buôn Amiral Latouche-Tréville của Pháp đang neo đậu tại Bến Nhà Rồng, người thanh niên Nguyễn Tất Thành với hai bàn tay trắng và tên gọi mới Văn Ba đã chính thức rời Tổ quốc, bắt đầu chuyến hành trình kéo dài 30 năm qua khắp các châu lục để tìm ra con đường giải phóng dân tộc.
3. Kiến trúc giao thoa & Các khu vực trưng bày
Kiến trúc: Tòa nhà Nhà Rồng mang phong cách kiến trúc thuộc địa Pháp với hành lang rộng, các vòm cửa cuốn vững chãi và hệ thống cột trụ uy nghiêm. Tuy nhiên, yếu tố văn hóa Việt (hình tượng rồng trên mái) đã tạo nên nét chấm phá rất riêng, hài hòa giữa hai nền văn hóa Á - Âu.
Không gian bảo tàng: Hiện nay, bảo tàng có nhiều phòng trưng bày cố định và chuyên đề, tái hiện toàn bộ hành trình từ thuở niên thiếu của Bác tại quê nhà Nghệ An, giai đoạn bôn ba tìm đường cứu nước tại Pháp, Anh, Mỹ, Liên Xô, Trung Quốc... cho đến ngày Ngài đọc bản Tuyên ngôn Độc lập và lãnh đạo đất nước.
4. Ứng dụng AR và Chuyển đổi số tương tác tại Bến Nhà Rồng
Để biến những tài liệu giấy và hiện vật đứng yên trở nên sinh động, giàu cảm xúc đối với khách tham quan, Bến Nhà Rồng đã áp dụng nhiều giải pháp công nghệ hiện đại:
"Tái hiện con tàu lịch sử" bằng AR:
Khi đứng tại khu vực bến cảng hướng ra sông Sài Gòn, du khách có thể sử dụng điện thoại thông minh quét mã AR để "triệu hồi" hình ảnh 3D của con tàu buôn cổ Amiral Latouche-Tréville đang neo đậu ngay trên bến sông thật.
Ứng dụng AR sẽ mô phỏng lại khoảnh khắc người thanh niên Văn Ba bước lên mạn tàu, chào tạm biệt bến cảng Sài Gòn trong không gian chuyển động 3D, giúp người xem cảm nhận rõ nét bối cảnh lịch sử của ngày 5/6 năm ấy.
Số hóa bản đồ "Hành trình 30 năm vạn dặm": Bảo tàng trang bị hệ thống màn hình cảm ứng tương tác cỡ lớn, số hóa toàn bộ bản đồ thế giới ghi dấu những nơi Bác đã đi qua. Du khách chỉ cần chạm vào các quốc gia như Pháp, Anh, Nga... bản đồ AR sẽ hiển thị các tư liệu ảnh, bài báo, hiện vật liên quan đến hoạt động của Bác tại quốc gia đó kèm giọng thuyết minh tự động.
Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được quét 3D sắc nét, cho phép người xem phóng to để đọc từng dòng chữ lịch sử.
Bến Nhà Rồng là nơi bắt đầu của một chương lịch sử mới cho dân tộc Việt Nam. Công nghệ AR và số hóa tại đây giống như một cuốn sách lịch sử mở, giúp thế hệ trẻ dễ dàng thấu hiểu hơn về lòng yêu nước, ý chí sắt đá và tầm nhìn vĩ đại của vị lãnh tụ kính yêu ngay tại nơi Người đã cất bước ra đi.', 10.768, 106.707, 'TP.HCM', '/media/heritage/ben-nha-rong/cover.webp')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Bến Nhà Rồng
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000001-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222201', 'Hai con rồng trên mái Nhà Rồng', '/media/heritage/ben-nha-rong/cover.webp', 'Nguồn gốc tên gọi "Nhà Rồng": Tòa nhà được xây theo kiến trúc phương Tây nhưng trên đỉnh mái lại được gắn hai con rồng lớn bằng đất nung tráng men chầu mặt trăng theo mô-típ "Lưỡng long chầu nguyệt" truyền thống của kiến trúc đình chùa Việt Nam. Sự kết hợp độc đáo này khiến người dân Sài Gòn lúc…', 'Nguồn gốc tên gọi "Nhà Rồng": Tòa nhà được xây theo kiến trúc phương Tây nhưng trên đỉnh mái lại được gắn hai con rồng lớn bằng đất nung tráng men chầu mặt trăng theo mô-típ "Lưỡng long chầu nguyệt" truyền thống của kiến trúc đình chùa Việt Nam. Sự kết hợp độc đáo này khiến người dân Sài Gòn lúc bấy giờ quen gọi tòa nhà là "Nhà Rồng" và bến cảng xung quanh là "Bến Nhà Rồng".
Sự kiện lịch sử vĩ đại (05/06/1911): Trên con tàu buôn Amiral Latouche-Tréville của Pháp đang neo đậu tại Bến Nhà Rồng, người thanh niên Nguyễn Tất Thành với hai bàn tay trắng và tên gọi mới Văn Ba đã chính thức rời Tổ quốc, bắt đầu chuyến hành trình kéo dài 30 năm qua khắp các châu lục để tìm ra con đường giải phóng dân tộc.
3. Kiến trúc giao thoa & Các khu vực trưng bày
Kiến trúc: Tòa nhà Nhà Rồng mang phong cách kiến trúc thuộc địa Pháp với hành lang rộng, các vòm cửa cuốn vững chãi và hệ thống cột trụ uy nghiêm. Tuy nhiên, yếu tố văn hóa Việt (hình tượng rồng trên mái) đã tạo nên nét chấm phá rất riêng, hài hòa giữa hai nền văn hóa Á - Âu.
Không gian bảo tàng: Hiện nay, bảo tàng có nhiều phòng trưng bày cố định và chuyên đề, tái hiện toàn bộ hành trình từ thuở niên thiếu của Bác tại quê nhà Nghệ An, giai đoạn bôn ba tìm đường cứu nước tại Pháp, Anh, Mỹ, Liên Xô, Trung Quốc... cho đến ngày Ngài đọc bản Tuyên ngôn Độc lập và lãnh đạo đất nước.
4. Ứng dụng AR và Chuyển đổi số tương tác tại Bến Nhà Rồng
Để biến những tài liệu giấy và hiện vật đứng yên trở nên sinh động, giàu cảm xúc đối với khách tham quan, Bến Nhà Rồng đã áp dụng nhiều giải pháp công nghệ hiện đại:', 'artifact:ben-nha-rong-rong-mai', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000002-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222201', 'Con tàu Amiral Latouche-Tréville', '/media/heritage/ben-nha-rong/cover.webp', 'Sự kiện lịch sử vĩ đại (05/06/1911): Trên con tàu buôn Amiral Latouche-Tréville của Pháp đang neo đậu tại Bến Nhà Rồng, người thanh niên Nguyễn Tất Thành với hai bàn tay trắng và tên gọi mới Văn Ba đã chính thức rời Tổ quốc, bắt đầu chuyến hành trình kéo dài 30 năm qua khắp các châu lục để tìm…', 'Sự kiện lịch sử vĩ đại (05/06/1911): Trên con tàu buôn Amiral Latouche-Tréville của Pháp đang neo đậu tại Bến Nhà Rồng, người thanh niên Nguyễn Tất Thành với hai bàn tay trắng và tên gọi mới Văn Ba đã chính thức rời Tổ quốc, bắt đầu chuyến hành trình kéo dài 30 năm qua khắp các châu lục để tìm ra con đường giải phóng dân tộc.
3. Kiến trúc giao thoa & Các khu vực trưng bày
Kiến trúc: Tòa nhà Nhà Rồng mang phong cách kiến trúc thuộc địa Pháp với hành lang rộng, các vòm cửa cuốn vững chãi và hệ thống cột trụ uy nghiêm. Tuy nhiên, yếu tố văn hóa Việt (hình tượng rồng trên mái) đã tạo nên nét chấm phá rất riêng, hài hòa giữa hai nền văn hóa Á - Âu.
Không gian bảo tàng: Hiện nay, bảo tàng có nhiều phòng trưng bày cố định và chuyên đề, tái hiện toàn bộ hành trình từ thuở niên thiếu của Bác tại quê nhà Nghệ An, giai đoạn bôn ba tìm đường cứu nước tại Pháp, Anh, Mỹ, Liên Xô, Trung Quốc... cho đến ngày Ngài đọc bản Tuyên ngôn Độc lập và lãnh đạo đất nước.
4. Ứng dụng AR và Chuyển đổi số tương tác tại Bến Nhà Rồng
Để biến những tài liệu giấy và hiện vật đứng yên trở nên sinh động, giàu cảm xúc đối với khách tham quan, Bến Nhà Rồng đã áp dụng nhiều giải pháp công nghệ hiện đại:', 'artifact:ben-nha-rong-tau-amiral', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000003-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222201', 'Chiếc vali mây của Bác', '/media/heritage/ben-nha-rong/cover.webp', 'Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được…', 'Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được quét 3D sắc nét, cho phép người xem phóng to để đọc từng dòng chữ lịch sử.
Bến Nhà Rồng là nơi bắt đầu của một chương lịch sử mới cho dân tộc Việt Nam. Công nghệ AR và số hóa tại đây giống như một cuốn sách lịch sử mở, giúp thế hệ trẻ dễ dàng thấu hiểu hơn về lòng yêu nước, ý chí sắt đá và tầm nhìn vĩ đại của vị lãnh tụ kính yêu ngay tại nơi Người đã cất bước ra đi.', 'artifact:ben-nha-rong-vali-may', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000004-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222201', 'Đôi dép cao su', '/media/heritage/ben-nha-rong/cover.webp', 'Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được…', 'Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được quét 3D sắc nét, cho phép người xem phóng to để đọc từng dòng chữ lịch sử.
Bến Nhà Rồng là nơi bắt đầu của một chương lịch sử mới cho dân tộc Việt Nam. Công nghệ AR và số hóa tại đây giống như một cuốn sách lịch sử mở, giúp thế hệ trẻ dễ dàng thấu hiểu hơn về lòng yêu nước, ý chí sắt đá và tầm nhìn vĩ đại của vị lãnh tụ kính yêu ngay tại nơi Người đã cất bước ra đi.', 'artifact:ben-nha-rong-dep-cao-su', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222202', 'Chùa thiên mụ', '1. Tổng quan & Vị trí
Vị trí: Chùa nằm trên đồi Hà Khê, thuộc tả ngạn Sông Hương, cách trung tâm thành phố Huế khoảng 5km về phía Tây.
Kiến trúc cảnh quan: Ngôi chùa soi bóng xuống dòng sông Hương thơ mộng, tạo nên một bức tranh sơn thủy hữu tình, mang đậm nét u tịch, trang nghiêm và cổ kính.
2. Lịch sử hình thành và những truyền thuyết
Năm thành lập: Chùa được chính thức khởi công xây dựng vào năm 1601 (tức năm Tân Sửu) bởi vị chúa Nguyễn đầu tiên – Chúa Tiên Nguyễn Hoàng.
Truyền thuyết về tên gọi "Thiên Mụ":
Theo đại nam nhất thống chí, khi chúa Nguyễn Hoàng vào trấn thủ xứ Thuận Hóa, ông đã đích thân đi dọc bờ sông Hương để xem xét địa hình nhằm chuẩn bị cho việc mở mang cơ nghiệp. Khi đến đồi Hà Khê, ông nghe người dân địa phương kể lại rằng: Đêm đêm thường có một bà lão mặc áo đỏ quần lục xuất hiện trên đồi, nói với mọi người rằng "Rồi đây sẽ có một vị chúa đến lập chùa để tụ khí cho bền long mạch, làm cho nước Nam hùng mạnh".
Nói xong, bà lão biến mất vào đám mây. Cho rằng đó là điềm lành từ trời, Nguyễn Hoàng đã cho dựng chùa trên đồi và đặt tên là Thiên Mụ Tự (Chùa Bà Mụ Trời).
Các mốc thời gian quan trọng:
Năm 1665: Chúa Nguyễn Phúc Tần cho trùng tu, mở rộng quy mô chùa.
Năm 1710: Chúa Nguyễn Phúc Chu cho đúc chiếc Đại Hồng Chung lớn.
Năm 1714: Chúa Nguyễn Phúc Chu tiếp tục đại trùng tu, xây dựng thêm hàng loạt công trình điện thờ, nhà tăng, lầu bia, và biến Thiên Mụ trở thành ngôi chùa tráng lệ nhất xứ Đàng Trong thời bấy giờ.
Thời nhà Nguyễn (thế kỷ XIX): Các vua Gia Long, Minh Mạng, Thiệu Trị, Tự Đức đều nhiều lần cho trùng tu chùa. Vua Thiệu Trị đã cho xây dựng tháp Phước Duyên (1844) - biểu tượng ngày nay của chùa.
Năm 1904: Một trận bão lịch sử (năm Thìn) đã tàn phá nặng nề nhiều công trình trong chùa, sau đó được vua Thành Thái cho sửa chữa lại nhưng quy mô không còn được như trước.
3. Kiến trúc và Cổ vật có giá trị lịch sử
Chùa Thiên Mụ không chỉ là trung tâm tu học mà còn là một "bảo tàng" ngoài trời lưu giữ nhiều cổ vật quý giá qua các triều đại:
Tháp Phước Duyên:
Xây dựng năm 1844 dưới thời vua Thiệu Trị, ban đầu có tên là tháp Từ Nhân.
Tháp cao 7 tầng (khoảng 21m), hình bát giác, mỗi tầng thờ một Đức Phật khác nhau. Đây là một trong những ngọn tháp cổ mang tính biểu tượng cao nhất tại Việt Nam.
Đại Hồng Chung (Chuông đồng):
Được chúa Nguyễn Phúc Chu cho đúc vào năm 1710.
Chuông cao 2,5m, đường kính miệng chuông 1,4m và nặng tới 3.285 cân (khoảng hơn 2 tấn). Tiếng chuông Thiên Mụ từ lâu đã đi vào ca dao, văn học vì độ vang xa và trầm ấm âm vang khắp vùng kinh thành xưa.
Bia đá thời Chúa Nguyễn Phúc Chu (1715):
Đặt trên lưng một con rùa đá lớn bằng cẩm thạch. Bia cao hơn 2,5m, khắc bài văn do chính chúa Nguyễn Phúc Chu soạn, ghi lại việc xây dựng và trùng tu chùa, thể hiện tư tưởng Phật giáo thời bấy giờ.
Điện Đại Hùng:
Là ngôi chính điện của chùa, kiến trúc theo lối trùng thiềm điệp ốc đặc trưng của Huế. Bên trong thờ tượng Phật Tam Thế, tượng Di Lặc và các vị Phật, Bồ Tát khác. Khung cửa và các hệ thống cột kèo gỗ đều được chạm khắc cực kỳ tinh xảo.
Chiếc xe ô tô di vật của Hòa thượng Thích Quảng Đức:
Nằm ở khu vực phía sau khuôn viên chùa, nơi đây trưng bày chiếc xe ô tô Austin màu xanh xám. Đây chính là chiếc xe đã đưa Bồ tát Thích Quảng Đức từ Huế vào Sài Gòn trước khi ngài thực hiện cuộc tự thiêu vị pháp thiêu thân vào năm 1963 để phản đối chính sách đàn áp Phật giáo của chế độ Ngô Đình Diệm.
4. Giá trị tâm linh và văn hóa
Chùa Thiên Mụ nằm trong danh sách các điểm di tích thuộc Quần thể di tích Cố đô Huế được UNESCO công nhận là Di sản Văn hóa Thế giới năm 1993.
Ngôi chùa gắn liền với đời sống tâm linh của người dân xứ Huế, là nguồn cảm hứng bất tận cho thi ca, nhạc họa qua nhiều thế hệ nhờ vẻ đẹp tĩnh lặng, thanh tịnh giữa không gian trầm mặc của vùng đất Thần Kinh.', 16.453, 107.558, 'Huế', '/media/heritage/chua-thien-mu/cover.webp')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Chùa thiên mụ
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000005-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Tháp Phước Duyên', '/media/heritage/chua-thien-mu/cover.webp', 'Thời nhà Nguyễn (thế kỷ XIX): Các vua Gia Long, Minh Mạng, Thiệu Trị, Tự Đức đều nhiều lần cho trùng tu chùa. Vua Thiệu Trị đã cho xây dựng tháp Phước Duyên (1844) - biểu tượng ngày nay của chùa.
Năm 1904: Một trận bão lịch sử (năm Thìn) đã tàn phá nặng nề nhiều công trình trong chùa, sau đó…', 'Thời nhà Nguyễn (thế kỷ XIX): Các vua Gia Long, Minh Mạng, Thiệu Trị, Tự Đức đều nhiều lần cho trùng tu chùa. Vua Thiệu Trị đã cho xây dựng tháp Phước Duyên (1844) - biểu tượng ngày nay của chùa.
Năm 1904: Một trận bão lịch sử (năm Thìn) đã tàn phá nặng nề nhiều công trình trong chùa, sau đó được vua Thành Thái cho sửa chữa lại nhưng quy mô không còn được như trước.
3. Kiến trúc và Cổ vật có giá trị lịch sử
Chùa Thiên Mụ không chỉ là trung tâm tu học mà còn là một "bảo tàng" ngoài trời lưu giữ nhiều cổ vật quý giá qua các triều đại:', 'artifact:chua-thien-mu-thap-phuoc-duyen', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000006-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Đại Hồng Chung', '/media/heritage/chua-thien-mu/cover.webp', 'Năm 1710: Chúa Nguyễn Phúc Chu cho đúc chiếc Đại Hồng Chung lớn.
Năm 1714: Chúa Nguyễn Phúc Chu tiếp tục đại trùng tu, xây dựng thêm hàng loạt công trình điện thờ, nhà tăng, lầu bia, và biến Thiên Mụ trở thành ngôi chùa tráng lệ nhất xứ Đàng Trong thời bấy giờ.
Thời nhà Nguyễn (thế kỷ XIX): Các…', 'Năm 1710: Chúa Nguyễn Phúc Chu cho đúc chiếc Đại Hồng Chung lớn.
Năm 1714: Chúa Nguyễn Phúc Chu tiếp tục đại trùng tu, xây dựng thêm hàng loạt công trình điện thờ, nhà tăng, lầu bia, và biến Thiên Mụ trở thành ngôi chùa tráng lệ nhất xứ Đàng Trong thời bấy giờ.
Thời nhà Nguyễn (thế kỷ XIX): Các vua Gia Long, Minh Mạng, Thiệu Trị, Tự Đức đều nhiều lần cho trùng tu chùa. Vua Thiệu Trị đã cho xây dựng tháp Phước Duyên (1844) - biểu tượng ngày nay của chùa.
Năm 1904: Một trận bão lịch sử (năm Thìn) đã tàn phá nặng nề nhiều công trình trong chùa, sau đó được vua Thành Thái cho sửa chữa lại nhưng quy mô không còn được như trước.
3. Kiến trúc và Cổ vật có giá trị lịch sử
Chùa Thiên Mụ không chỉ là trung tâm tu học mà còn là một "bảo tàng" ngoài trời lưu giữ nhiều cổ vật quý giá qua các triều đại:', 'artifact:chua-thien-mu-dai-hong-chung', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000007-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Bia đá thời Chúa Nguyễn Phúc Chu', '/media/heritage/chua-thien-mu/cover.webp', 'Bia đá thời Chúa Nguyễn Phúc Chu (1715):
Đặt trên lưng một con rùa đá lớn bằng cẩm thạch. Bia cao hơn 2,5m, khắc bài văn do chính chúa Nguyễn Phúc Chu soạn, ghi lại việc xây dựng và trùng tu chùa, thể hiện tư tưởng Phật giáo thời bấy giờ.', 'Bia đá thời Chúa Nguyễn Phúc Chu (1715):
Đặt trên lưng một con rùa đá lớn bằng cẩm thạch. Bia cao hơn 2,5m, khắc bài văn do chính chúa Nguyễn Phúc Chu soạn, ghi lại việc xây dựng và trùng tu chùa, thể hiện tư tưởng Phật giáo thời bấy giờ.', 'artifact:chua-thien-mu-bia-da', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000008-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Điện Đại Hùng', '/media/heritage/chua-thien-mu/cover.webp', 'Điện Đại Hùng:
Là ngôi chính điện của chùa, kiến trúc theo lối trùng thiềm điệp ốc đặc trưng của Huế. Bên trong thờ tượng Phật Tam Thế, tượng Di Lặc và các vị Phật, Bồ Tát khác. Khung cửa và các hệ thống cột kèo gỗ đều được chạm khắc cực kỳ tinh xảo.', 'Điện Đại Hùng:
Là ngôi chính điện của chùa, kiến trúc theo lối trùng thiềm điệp ốc đặc trưng của Huế. Bên trong thờ tượng Phật Tam Thế, tượng Di Lặc và các vị Phật, Bồ Tát khác. Khung cửa và các hệ thống cột kèo gỗ đều được chạm khắc cực kỳ tinh xảo.', 'artifact:chua-thien-mu-dien-dai-hung', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000009-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Xe Austin Thích Quảng Đức', '/media/heritage/chua-thien-mu/cover.webp', 'Chiếc xe ô tô di vật của Hòa thượng Thích Quảng Đức:
Nằm ở khu vực phía sau khuôn viên chùa, nơi đây trưng bày chiếc xe ô tô Austin màu xanh xám. Đây chính là chiếc xe đã đưa Bồ tát Thích Quảng Đức từ Huế vào Sài Gòn trước khi ngài thực hiện cuộc tự thiêu vị pháp thiêu thân vào năm 1963 để phản…', 'Chiếc xe ô tô di vật của Hòa thượng Thích Quảng Đức:
Nằm ở khu vực phía sau khuôn viên chùa, nơi đây trưng bày chiếc xe ô tô Austin màu xanh xám. Đây chính là chiếc xe đã đưa Bồ tát Thích Quảng Đức từ Huế vào Sài Gòn trước khi ngài thực hiện cuộc tự thiêu vị pháp thiêu thân vào năm 1963 để phản đối chính sách đàn áp Phật giáo của chế độ Ngô Đình Diệm.
4. Giá trị tâm linh và văn hóa
Chùa Thiên Mụ nằm trong danh sách các điểm di tích thuộc Quần thể di tích Cố đô Huế được UNESCO công nhận là Di sản Văn hóa Thế giới năm 1993.
Ngôi chùa gắn liền với đời sống tâm linh của người dân xứ Huế, là nguồn cảm hứng bất tận cho thi ca, nhạc họa qua nhiều thế hệ nhờ vẻ đẹp tĩnh lặng, thanh tịnh giữa không gian trầm mặc của vùng đất Thần Kinh.', 'artifact:chua-thien-mu-xe-austin', 'primary', 5)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222203', 'Cố đô Hoa Lư', '1. Tổng quan & Vị trí
Vị trí: Thuộc xã Trường Yên, huyện Hoa Lư, tỉnh Ninh Bình, cách thủ đô Hà Nội khoảng 90km về phía Nam.
Tầm vóc: Hoa Lư là một trong ba vùng lõi cấu thành nên Quần thể danh thắng Tràng An – Di sản Văn hóa và Thiên nhiên thế giới (di sản hỗn hợp duy nhất tại Đông Nam Á) được UNESCO công nhận vào năm 2014.
Địa thế phong thủy: Kinh đô được bao bọc bởi những dãy núi đá vôi trùng điệp, tạo thành một bức tường thành thiên nhiên kiên cố, xen kẽ giữa các thung lũng là hệ thống sông ngòi chằng chịt, mang tính chất một thành trì quân sự hiểm yếu "bất khả xâm phạm".
2. Lịch sử hình thành: Kinh đô của ba triều đại
Cố đô Hoa Lư đóng vai trò là thủ đô của nước Đại Cồ Việt trong suốt 42 năm (968 – 1010), gắn liền với sự nghiệp của ba triều đại liên tiếp:
Nhà Đinh (968 - 980): Sau khi dùng mưu lược và võ công dẹp loạn 12 sứ quân, thống nhất giang sơn, Đinh Bộ Lĩnh lên ngôi hoàng đế (Đinh Tiên Hoàng), đặt tên nước là Đại Cồ Việt và chọn Hoa Lư làm đất định đô. Ông cho đắp thành, đào hào, xây dựng cung điện.
Nhà Tiền Lê (980 - 1009): Vua Lê Hoàn (Lê Đại Hành) lên ngôi, tiếp tục đóng đô tại đây. Ông đã lãnh đạo quân dân đánh tan quân tống xâm lược (981) và mở mang, xây dựng thêm nhiều điện đài lộng lẫy (như điện Bách Bảo Thiên Tuế được dát vàng bạc).
Nhà Lý (Giai đoạn đầu, 1009 - 1010): Vua Lý Thái Tổ (Lý Công Uẩn) lên ngôi tại chính điện Hoa Lư. Nhận thấy vùng đất này tuy hiểm trở nhưng chật hẹp, khó mở mang phát triển lâu dài, mùa thu năm 1010, ông đã viết Chiếu dời đô để chuyển kinh thủ đô về Thăng Long (Hà Nội). Từ đó, Hoa Lư trở thành Cố đô (kinh đô cũ).
3. Di tích kiến trúc & Những báu vật lịch sử
Trải qua hơn 1000 năm với những thăng trầm và tàn phá của thời gian, các cung điện vàng son xưa kia không còn nguyên vẹn trên mặt đất. Tuy nhiên, không gian tâm linh cốt lõi vẫn được lưu giữ qua các công trình đền thờ tôn nghiêm:
Đền vua Đinh Tiên Hoàng:
Nằm ở trung tâm thành Đông, xây dựng theo kiểu "Nội công ngoại quốc" đặc trưng.
Long Sàng (Sập đá): Đặt trước Nghi môn ngoại và Thượng điện, được tạc bằng đá nguyên khối từ thế kỷ XVII. Long sàng chạm khắc hình rồng uốn lượn cực kỳ uyển chuyển, tinh xảo, được công nhận là Bảo vật Quốc gia.
Đền vua Lê Đại Hành:
Cách đền vua Đinh khoảng 300m, quy mô nhỏ hơn một chút nhưng nghệ thuật điêu khắc gỗ và đá vô cùng đặc sắc.
Nơi đây còn lưu giữ những mảng chạm khắc rồng, mây mang đậm phong cách nghệ thuật thời hậu Lê. Đặc biệt là bức tượng vua Lê Đại Hành, thái hậu Dương Vân Nga và kiệu rước cổ.
Chùa Nhất Trụ (Chùa Một Cột của Hoa Lư):
Ngôi chùa cổ do vua Lê Đại Hành xây dựng. Tại đây có Cột kinh Phật bằng đá khắc bài kinh Thủ Lăng Nghiêm, dựng từ năm 995. Đây là bảo vật quốc gia mang giá trị vô song về lịch sử Phật giáo và nghệ thuật thư pháp đá thế kỷ X.
Khu khai quật khảo cổ học lòng đất:
Nằm ngay cạnh đền vua Lê, các nhà khảo cổ đã làm xuất lộ hệ thống nền móng kiến trúc cung điện thế kỷ X, dấu tích ngói lợp, gạch lát nền có chạm khắc hình hoa sen, chim phượng và các loại tiền cổ "Đinh Vạn Tuế Thông Bảo".
4. Ứng dụng AR và Số hóa "Hồi sinh" Cố đô vàng son
Chính vì kiến trúc cung điện gốc bằng gỗ của thế kỷ X đã lùi sâu vào lòng đất, công nghệ AR và số hóa là "chìa khóa vàng" giúp du khách tìm lại diện mạo hào hùng của Hoa Lư:
Phục dựng điện Bách Bảo Thiên Tuế bằng AR: Khi đứng tại sân đền hoặc khu khảo cổ, du khách có thể sử dụng điện thoại thông minh quét các điểm tương tác để nhìn thấy toàn bộ hình ảnh cung điện mô phỏng 3D hiện lên trực quan. Bạn sẽ thấy những mái cung điện lợp ngói uy nghiêm, các cột gỗ sơn son, tái hiện đúng mô tả của lịch sử về một kinh thành kiên cố giữa lòng thung lũng đá vôi.
Mô phỏng 3D Sa bàn Kinh đô: Công nghệ thực tế ảo cho phép người xem đứng từ trên cao (góc nhìn chim bay) để quan sát toàn bộ hệ thống Thành Ngoại, Thành Trong, Thành Nam phối hợp với các ngọn núi đá (núi Mã Yên, núi Cột Cờ) tạo thành chiến lũy phòng thủ tự nhiên ra sao.
Số hóa tương tác Bảo vật Quốc gia: Du khách có thể xoay 360 độ và phóng to từng chi tiết chạm khắc trên Long Sàng đá hay Cột kinh Phật Chùa Nhất Trụ để chiêm ngưỡng kỹ nghệ điêu khắc đá điêu luyện của cha ông mà không làm tổn hại đến hiện vật gốc.
Cố đô Hoa Lư là nơi ngưng đọng hào khí tự cường của dân tộc Việt. Dù kiến trúc vật chất đã mai một, nhưng dòng chảy lịch sử và sự trợ lực của công nghệ AR đang giúp thế hệ hôm nay chạm vào quá khứ một cách chân thực và tự hào nhất.', 20.283, 105.917, 'Ninh Bình', '/media/heritage/co-do-hoa-lu/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Cố đô Hoa Lư
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000010-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222203', 'Long Sàng đá', '/media/heritage/co-do-hoa-lu/cover.jpeg', 'Long Sàng (Sập đá): Đặt trước Nghi môn ngoại và Thượng điện, được tạc bằng đá nguyên khối từ thế kỷ XVII. Long sàng chạm khắc hình rồng uốn lượn cực kỳ uyển chuyển, tinh xảo, được công nhận là Bảo vật Quốc gia.', 'Long Sàng (Sập đá): Đặt trước Nghi môn ngoại và Thượng điện, được tạc bằng đá nguyên khối từ thế kỷ XVII. Long sàng chạm khắc hình rồng uốn lượn cực kỳ uyển chuyển, tinh xảo, được công nhận là Bảo vật Quốc gia.', 'artifact:co-do-hoa-lu-long-sang', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000011-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222203', 'Cột kinh Chùa Nhất Trụ', '/media/heritage/co-do-hoa-lu/cover.jpeg', 'Chùa Nhất Trụ (Chùa Một Cột của Hoa Lư):
Ngôi chùa cổ do vua Lê Đại Hành xây dựng. Tại đây có Cột kinh Phật bằng đá khắc bài kinh Thủ Lăng Nghiêm, dựng từ năm 995. Đây là bảo vật quốc gia mang giá trị vô song về lịch sử Phật giáo và nghệ thuật thư pháp đá thế kỷ X.', 'Chùa Nhất Trụ (Chùa Một Cột của Hoa Lư):
Ngôi chùa cổ do vua Lê Đại Hành xây dựng. Tại đây có Cột kinh Phật bằng đá khắc bài kinh Thủ Lăng Nghiêm, dựng từ năm 995. Đây là bảo vật quốc gia mang giá trị vô song về lịch sử Phật giáo và nghệ thuật thư pháp đá thế kỷ X.', 'artifact:co-do-hoa-lu-cot-kinh', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000012-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222203', 'Đền vua Lê Đại Hành', '/media/heritage/co-do-hoa-lu/cover.jpeg', 'Nhà Tiền Lê (980 - 1009): Vua Lê Hoàn (Lê Đại Hành) lên ngôi, tiếp tục đóng đô tại đây. Ông đã lãnh đạo quân dân đánh tan quân tống xâm lược (981) và mở mang, xây dựng thêm nhiều điện đài lộng lẫy (như điện Bách Bảo Thiên Tuế được dát vàng bạc).
Nhà Lý (Giai đoạn đầu, 1009 - 1010): Vua Lý Thái…', 'Nhà Tiền Lê (980 - 1009): Vua Lê Hoàn (Lê Đại Hành) lên ngôi, tiếp tục đóng đô tại đây. Ông đã lãnh đạo quân dân đánh tan quân tống xâm lược (981) và mở mang, xây dựng thêm nhiều điện đài lộng lẫy (như điện Bách Bảo Thiên Tuế được dát vàng bạc).
Nhà Lý (Giai đoạn đầu, 1009 - 1010): Vua Lý Thái Tổ (Lý Công Uẩn) lên ngôi tại chính điện Hoa Lư. Nhận thấy vùng đất này tuy hiểm trở nhưng chật hẹp, khó mở mang phát triển lâu dài, mùa thu năm 1010, ông đã viết Chiếu dời đô để chuyển kinh thủ đô về Thăng Long (Hà Nội). Từ đó, Hoa Lư trở thành Cố đô (kinh đô cũ).
3. Di tích kiến trúc & Những báu vật lịch sử
Trải qua hơn 1000 năm với những thăng trầm và tàn phá của thời gian, các cung điện vàng son xưa kia không còn nguyên vẹn trên mặt đất. Tuy nhiên, không gian tâm linh cốt lõi vẫn được lưu giữ qua các công trình đền thờ tôn nghiêm:', 'artifact:co-do-hoa-lu-den-le-dai-hanh', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000013-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222203', 'Tiền cổ Đinh Vạn Tuế', '/media/heritage/co-do-hoa-lu/cover.jpeg', 'Nằm ngay cạnh đền vua Lê, các nhà khảo cổ đã làm xuất lộ hệ thống nền móng kiến trúc cung điện thế kỷ X, dấu tích ngói lợp, gạch lát nền có chạm khắc hình hoa sen, chim phượng và các loại tiền cổ "Đinh Vạn Tuế Thông Bảo".
4. Ứng dụng AR và Số hóa "Hồi sinh" Cố đô vàng son
Chính vì kiến trúc cung…', 'Nằm ngay cạnh đền vua Lê, các nhà khảo cổ đã làm xuất lộ hệ thống nền móng kiến trúc cung điện thế kỷ X, dấu tích ngói lợp, gạch lát nền có chạm khắc hình hoa sen, chim phượng và các loại tiền cổ "Đinh Vạn Tuế Thông Bảo".
4. Ứng dụng AR và Số hóa "Hồi sinh" Cố đô vàng son
Chính vì kiến trúc cung điện gốc bằng gỗ của thế kỷ X đã lùi sâu vào lòng đất, công nghệ AR và số hóa là "chìa khóa vàng" giúp du khách tìm lại diện mạo hào hùng của Hoa Lư:
Phục dựng điện Bách Bảo Thiên Tuế bằng AR: Khi đứng tại sân đền hoặc khu khảo cổ, du khách có thể sử dụng điện thoại thông minh quét các điểm tương tác để nhìn thấy toàn bộ hình ảnh cung điện mô phỏng 3D hiện lên trực quan. Bạn sẽ thấy những mái cung điện lợp ngói uy nghiêm, các cột gỗ sơn son, tái hiện đúng mô tả của lịch sử về một kinh thành kiên cố giữa lòng thung lũng đá vôi.
Mô phỏng 3D Sa bàn Kinh đô: Công nghệ thực tế ảo cho phép người xem đứng từ trên cao (góc nhìn chim bay) để quan sát toàn bộ hệ thống Thành Ngoại, Thành Trong, Thành Nam phối hợp với các ngọn núi đá (núi Mã Yên, núi Cột Cờ) tạo thành chiến lũy phòng thủ tự nhiên ra sao.
Số hóa tương tác Bảo vật Quốc gia: Du khách có thể xoay 360 độ và phóng to từng chi tiết chạm khắc trên Long Sàng đá hay Cột kinh Phật Chùa Nhất Trụ để chiêm ngưỡng kỹ nghệ điêu khắc đá điêu luyện của cha ông mà không làm tổn hại đến hiện vật gốc.
Cố đô Hoa Lư là nơi ngưng đọng hào khí tự cường của dân tộc Việt. Dù kiến trúc vật chất đã mai một, nhưng dòng chảy lịch sử và sự trợ lực của công nghệ AR đang giúp thế hệ hôm nay chạm vào quá khứ một cách chân thực và tự hào nhất.', 'artifact:co-do-hoa-lu-tien-co', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222204', 'Hoàng Thành Thăng Long', '1. Tổng quan & Vị trí
Vị trí: Nằm tại số 19C Hoàng Diệu, quận Ba Đình, Hà Nội. Khu di tích có diện tích cực kỳ rộng lớn, bao quanh bởi các con đường lịch sử như Hoàng Diệu, Điện Biên Phủ, Phan Đình Phùng và Nguyễn Tri Phương.
Tầm vóc: Đây là quần thể di tích gắn liền với lịch sử kinh thành Thăng Long - Đông Kinh và tỉnh thành Hà Nội. Đây là minh chứng sống động cho sự phát triển liên tục của trung tâm quyền lực chính trị Việt Nam suốt 13 thế kỷ (từ thời tiền Thăng Long thế kỷ VII, qua các triều đại Lý, Trần, Lê, Mạc, Nguyễn đến thời đại Hồ Chí Minh). Khu trung tâm Hoàng thành Thăng Long được UNESCO công nhận là Di sản Văn hóa Thế giới vào năm 2010.
2. Lịch sử hình thành qua các tầng đất khảo cổ
Điểm đặc biệt nhất của Hoàng Thành Thăng Long chính là "lịch sử chồng chồng lên nhau" ngay trong lòng đất:
Thời kỳ tiền Thăng Long (Thế kỷ VII - IX): Nơi đây từng là trụ sở của An Nam đô hộ phủ thời nhà Đường (thành Đại La).
Định đô Thăng Long (Năm 1010): Vua Lý Thái Tổ dời đô từ Hoa Lư về Đại La, đổi tên thành Thăng Long và cho xây dựng Hoàng Thành trên nền thành cũ.
Các triều đại tiếp nối (Trần, Lê, Mạc): Tiếp tục mở rộng, xây dựng thêm nhiều cung điện nguy nga. Nơi đây là trung tâm đầu não điều hành đất nước, kinh qua các cuộc chiến chống ngoại xâm hiển hách.
Thời nhà Nguyễn (Thế kỷ XIX): Khi triều Nguyễn chuyển kinh đô vào Phú Xuân (Huế), nơi đây trở thành thành Hà Nội. Vua Gia Long cho phá dỡ một số công trình cũ để xây lại thành theo kiến trúc Vauban của Pháp.
Thời kỳ hiện đại: Trong kháng chiến chống Mỹ, Tổng hành dinh Quân đội Nhân dân Việt Nam (Bộ Chính trị và Quân ủy Trung ương) đã chọn nơi đây làm trung tâm chỉ huy đầu não nhờ hệ thống hầm ngầm kiên cố.
3. Các công trình tiêu biểu & Cổ vật nghìn năm
Khu di tích được chia làm hai khu vực chính: Khu di tích lịch sử Thành cổ Hà Nội và Khu khảo cổ học 18 Hoàng Diệu.
A. Các công trình lộ thiên nổi bật
Kỳ Đài (Cột cờ Hà Nội): Xây dựng năm 1812 dưới thời vua Gia Long, cao gần 33m. Đây là công trình hiếm hoi còn nguyên vẹn, sống sót qua thời kỳ thuộc địa Pháp và chiến tranh.
Đoan Môn: Cửa chính dẫn vào Cấm Thành (nơi ở của nhà vua). Công trình được xây bằng gạch vồ thời Lê và sửa sang lại thời Nguyễn, cấu trúc cuốn vòm vững chãi với 3 cửa cuốn vòm.
Điện Kính Thiên (Nền điện): Là hạt nhân trung tâm của hoàng cung qua các thời kỳ Lý - Trần - Lê. Hiện nay cung điện nguy nga xưa chỉ còn lại phần nền móng đá và Đôi rồng đá chầu được tạc từ thế kỷ XV (thời Lê Sơ) – một kiệt tác điêu khắc mang đậm dấu ấn quyền lực hoàng gia.
Bắc Môn (Chính Bắc Môn): Cửa thành duy nhất còn sót lại của thành Hà Nội thời Nguyễn. Trên mặt thành phía ngoài vẫn còn nguyên hai vết đại bác do pháo thuyền Pháp bắn vào từ sông Hồng năm 1882 khi hạ thành.
Nhà và Hầm D67: Công trình mang đậm dấu ấn lịch sử hiện đại. Căn hầm bê tông cốt thép kiên cố là nơi Bộ Chính trị và Quân ủy Trung ương đưa ra những quyết định lịch sử định đoạt vận mệnh đất nước (như Chiến dịch Điện Biên Phủ trên không năm 1972 hay Chiến dịch Hồ Chí Minh năm 1975).
B. Khu khảo cổ 18 Hoàng Diệu & Cổ vật quý hiếm
Nằm đối diện khu thành cổ, đây là hố khai quật khảo cổ phát lộ hàng triệu di vật chồng xếp lên nhau theo dòng thời gian:
Kiến trúc lòng đất: Các móng cột trụ bằng sỏi, đường đi lát gạch, hệ thống cống thoát nước từ thời Lý, Trần, Lê được làm xuất lộ rõ ràng.
Cổ vật hoàng cung:
Các loại gạch ngói chạm khắc hình rồng, phượng tinh xảo, đặc biệt là ngói lá đề chim phượng tráng men xanh thời Lý.
Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn còn nguyên nguồn nước trong vắt.
4. Ứng dụng AR và Trải nghiệm số hóa tại Hoàng Thành
Tương tự như Đại Nội Huế, Hoàng Thành Thăng Long đã và đang ứng dụng rất mạnh mẽ công nghệ thực tế ảo và thực tế tăng cường (AR) để mang lịch sử đến gần hơn với du khách:
"Hồi sinh" Điện Kính Thiên bằng AR: Vì điện Kính Thiên hiện tại chỉ còn phần nền đá và rồng đá, du khách có thể dùng kính VR hoặc ứng dụng AR trên điện thoại để nhìn thấy toàn bộ kiến trúc gỗ quy mô 7 gian 2 chái, mái ngói chồng diêm của Điện Kính Thiên thời Lê Sơ hiển thị ngay tại vị trí thực tế với tỉ lệ 1:1.
Tour đêm "Giải mã Hoàng thành Thăng Long": Trong tour du lịch trải nghiệm này, công nghệ chiếu sáng nghệ thuật kết hợp cùng các ứng dụng quét mã tương tác giúp du khách có thể tự tay "giải mã" các cổ vật bằng cách chạm vào màn hình, xem dòng chảy lịch sử của hiện vật đó từ lúc được làm ra cho đến khi bị chôn vùi dưới lòng đất.
Xem các nghi lễ cung đình ảo: Bạn có thể quét các điểm AR tại Đoan Môn để chứng kiến đoàn rước hoàng gia, lính canh mặc giáp trụ di chuyển hay lễ ban sóc diễn ra ngay trước mắt thông qua màn hình thiết bị thông minh.
Hoàng Thành Thăng Long là một cuốn sử ký mở bằng đất đá và cổ vật. Việc tích hợp các công nghệ AR giúp người xem phá vỡ rào cản thời gian, dễ dàng hình dung ra một kinh thành Thăng Long vàng son, lộng lẫy nghìn năm trước.', 21.036, 105.817, 'Hà Nội', '/media/heritage/hoang-thanh-thang-long/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Hoàng Thành Thăng Long
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000014-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222204', 'Cột cờ Hà Nội (Kỳ Đài)', '/media/heritage/hoang-thanh-thang-long/cover.jpeg', 'Kỳ Đài (Cột cờ Hà Nội): Xây dựng năm 1812 dưới thời vua Gia Long, cao gần 33m. Đây là công trình hiếm hoi còn nguyên vẹn, sống sót qua thời kỳ thuộc địa Pháp và chiến tranh.
Đoan Môn: Cửa chính dẫn vào Cấm Thành (nơi ở của nhà vua). Công trình được xây bằng gạch vồ thời Lê và sửa sang lại thời…', 'Kỳ Đài (Cột cờ Hà Nội): Xây dựng năm 1812 dưới thời vua Gia Long, cao gần 33m. Đây là công trình hiếm hoi còn nguyên vẹn, sống sót qua thời kỳ thuộc địa Pháp và chiến tranh.
Đoan Môn: Cửa chính dẫn vào Cấm Thành (nơi ở của nhà vua). Công trình được xây bằng gạch vồ thời Lê và sửa sang lại thời Nguyễn, cấu trúc cuốn vòm vững chãi với 3 cửa cuốn vòm.
Điện Kính Thiên (Nền điện): Là hạt nhân trung tâm của hoàng cung qua các thời kỳ Lý - Trần - Lê. Hiện nay cung điện nguy nga xưa chỉ còn lại phần nền móng đá và Đôi rồng đá chầu được tạc từ thế kỷ XV (thời Lê Sơ) – một kiệt tác điêu khắc mang đậm dấu ấn quyền lực hoàng gia.
Bắc Môn (Chính Bắc Môn): Cửa thành duy nhất còn sót lại của thành Hà Nội thời Nguyễn. Trên mặt thành phía ngoài vẫn còn nguyên hai vết đại bác do pháo thuyền Pháp bắn vào từ sông Hồng năm 1882 khi hạ thành.
Nhà và Hầm D67: Công trình mang đậm dấu ấn lịch sử hiện đại. Căn hầm bê tông cốt thép kiên cố là nơi Bộ Chính trị và Quân ủy Trung ương đưa ra những quyết định lịch sử định đoạt vận mệnh đất nước (như Chiến dịch Điện Biên Phủ trên không năm 1972 hay Chiến dịch Hồ Chí Minh năm 1975).
B. Khu khảo cổ 18 Hoàng Diệu & Cổ vật quý hiếm
Nằm đối diện khu thành cổ, đây là hố khai quật khảo cổ phát lộ hàng triệu di vật chồng xếp lên nhau theo dòng thời gian:
Kiến trúc lòng đất: Các móng cột trụ bằng sỏi, đường đi lát gạch, hệ thống cống thoát nước từ thời Lý, Trần, Lê được làm xuất lộ rõ ràng.', 'artifact:hoang-thanh-thang-long-cot-co', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000015-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222204', 'Đôi rồng đá Điện Kính Thiên', '/media/heritage/hoang-thanh-thang-long/cover.jpeg', 'Điện Kính Thiên (Nền điện): Là hạt nhân trung tâm của hoàng cung qua các thời kỳ Lý - Trần - Lê. Hiện nay cung điện nguy nga xưa chỉ còn lại phần nền móng đá và Đôi rồng đá chầu được tạc từ thế kỷ XV (thời Lê Sơ) – một kiệt tác điêu khắc mang đậm dấu ấn quyền lực hoàng gia.
Bắc Môn (Chính Bắc…', 'Điện Kính Thiên (Nền điện): Là hạt nhân trung tâm của hoàng cung qua các thời kỳ Lý - Trần - Lê. Hiện nay cung điện nguy nga xưa chỉ còn lại phần nền móng đá và Đôi rồng đá chầu được tạc từ thế kỷ XV (thời Lê Sơ) – một kiệt tác điêu khắc mang đậm dấu ấn quyền lực hoàng gia.
Bắc Môn (Chính Bắc Môn): Cửa thành duy nhất còn sót lại của thành Hà Nội thời Nguyễn. Trên mặt thành phía ngoài vẫn còn nguyên hai vết đại bác do pháo thuyền Pháp bắn vào từ sông Hồng năm 1882 khi hạ thành.
Nhà và Hầm D67: Công trình mang đậm dấu ấn lịch sử hiện đại. Căn hầm bê tông cốt thép kiên cố là nơi Bộ Chính trị và Quân ủy Trung ương đưa ra những quyết định lịch sử định đoạt vận mệnh đất nước (như Chiến dịch Điện Biên Phủ trên không năm 1972 hay Chiến dịch Hồ Chí Minh năm 1975).
B. Khu khảo cổ 18 Hoàng Diệu & Cổ vật quý hiếm
Nằm đối diện khu thành cổ, đây là hố khai quật khảo cổ phát lộ hàng triệu di vật chồng xếp lên nhau theo dòng thời gian:
Kiến trúc lòng đất: Các móng cột trụ bằng sỏi, đường đi lát gạch, hệ thống cống thoát nước từ thời Lý, Trần, Lê được làm xuất lộ rõ ràng.', 'artifact:hoang-thanh-thang-long-doi-rong-da', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000016-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222204', 'Ngói chim phượng thời Lý', '/media/heritage/hoang-thanh-thang-long/cover.jpeg', 'Các loại gạch ngói chạm khắc hình rồng, phượng tinh xảo, đặc biệt là ngói lá đề chim phượng tráng men xanh thời Lý.
Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn…', 'Các loại gạch ngói chạm khắc hình rồng, phượng tinh xảo, đặc biệt là ngói lá đề chim phượng tráng men xanh thời Lý.
Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn còn nguyên nguồn nước trong vắt.
4. Ứng dụng AR và Trải nghiệm số hóa tại Hoàng Thành
Tương tự như Đại Nội Huế, Hoàng Thành Thăng Long đã và đang ứng dụng rất mạnh mẽ công nghệ thực tế ảo và thực tế tăng cường (AR) để mang lịch sử đến gần hơn với du khách:
"Hồi sinh" Điện Kính Thiên bằng AR: Vì điện Kính Thiên hiện tại chỉ còn phần nền đá và rồng đá, du khách có thể dùng kính VR hoặc ứng dụng AR trên điện thoại để nhìn thấy toàn bộ kiến trúc gỗ quy mô 7 gian 2 chái, mái ngói chồng diêm của Điện Kính Thiên thời Lê Sơ hiển thị ngay tại vị trí thực tế với tỉ lệ 1:1.
Tour đêm "Giải mã Hoàng thành Thăng Long": Trong tour du lịch trải nghiệm này, công nghệ chiếu sáng nghệ thuật kết hợp cùng các ứng dụng quét mã tương tác giúp du khách có thể tự tay "giải mã" các cổ vật bằng cách chạm vào màn hình, xem dòng chảy lịch sử của hiện vật đó từ lúc được làm ra cho đến khi bị chôn vùi dưới lòng đất.
Xem các nghi lễ cung đình ảo: Bạn có thể quét các điểm AR tại Đoan Môn để chứng kiến đoàn rước hoàng gia, lính canh mặc giáp trụ di chuyển hay lễ ban sóc diễn ra ngay trước mắt thông qua màn hình thiết bị thông minh.', 'artifact:hoang-thanh-thang-long-ngoi-phuong', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000017-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222204', 'Bát sứ ngự dụng', '/media/heritage/hoang-thanh-thang-long/cover.jpeg', 'Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn còn nguyên nguồn nước trong vắt.
4. Ứng dụng AR và Trải nghiệm số hóa tại Hoàng Thành
Tương tự như Đại Nội Huế,…', 'Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn còn nguyên nguồn nước trong vắt.
4. Ứng dụng AR và Trải nghiệm số hóa tại Hoàng Thành
Tương tự như Đại Nội Huế, Hoàng Thành Thăng Long đã và đang ứng dụng rất mạnh mẽ công nghệ thực tế ảo và thực tế tăng cường (AR) để mang lịch sử đến gần hơn với du khách:
"Hồi sinh" Điện Kính Thiên bằng AR: Vì điện Kính Thiên hiện tại chỉ còn phần nền đá và rồng đá, du khách có thể dùng kính VR hoặc ứng dụng AR trên điện thoại để nhìn thấy toàn bộ kiến trúc gỗ quy mô 7 gian 2 chái, mái ngói chồng diêm của Điện Kính Thiên thời Lê Sơ hiển thị ngay tại vị trí thực tế với tỉ lệ 1:1.
Tour đêm "Giải mã Hoàng thành Thăng Long": Trong tour du lịch trải nghiệm này, công nghệ chiếu sáng nghệ thuật kết hợp cùng các ứng dụng quét mã tương tác giúp du khách có thể tự tay "giải mã" các cổ vật bằng cách chạm vào màn hình, xem dòng chảy lịch sử của hiện vật đó từ lúc được làm ra cho đến khi bị chôn vùi dưới lòng đất.
Xem các nghi lễ cung đình ảo: Bạn có thể quét các điểm AR tại Đoan Môn để chứng kiến đoàn rước hoàng gia, lính canh mặc giáp trụ di chuyển hay lễ ban sóc diễn ra ngay trước mắt thông qua màn hình thiết bị thông minh.
Hoàng Thành Thăng Long là một cuốn sử ký mở bằng đất đá và cổ vật. Việc tích hợp các công nghệ AR giúp người xem phá vỡ rào cản thời gian, dễ dàng hình dung ra một kinh thành Thăng Long vàng son, lộng lẫy nghìn năm trước.', 'artifact:hoang-thanh-thang-long-bat-su', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222205', 'Phố Cổ Hội An', '1. Tổng quan & Vị trí
Vị trí: Nằm ở hạ lưu sông Thu Bồn, thuộc địa phận thành phố Hội An, tỉnh Quảng Nam, cách thành phố Đà Nẵng khoảng 30km về phía Nam.
Tầm vóc: Hội An là một ví dụ đặc biệt xuất sắc về một thương cảng truyền thống ở Đông Nam Á được bảo tồn nguyên vẹn gần như cấu trúc đô thị ban đầu. Với những giá trị văn hóa, kiến trúc vật thể và phi vật thể vô giá, Phố cổ Hội An đã được UNESCO công nhận là Di sản Văn hóa Thế giới vào năm 1999.
2. Lịch sử hình thành: Từ cảng thị Champa đến thương cảng quốc tế
Thời kỳ Tiền Hội An: Từ thế kỷ thứ II đến thế kỷ XV, nơi đây từng là cảng biển sầm uất của vương quốc Champa (thời kỳ Lâm Ấp phố), đóng vai trò quan trọng trên Con đường tơ lụa và Con đường gia vị trên biển.
Thời kỳ Hưng thịnh (Thế kỷ XVI - XVIII): Dưới thời các Chúa Nguyễn mở mang bờ cõi về phía Nam (Đàng Trong), Hội An phát triển thành thương cảng quốc tế lớn nhất khu vực. Các thuyền buôn từ Nhật Bản, Trung Quốc, Bồ Đào Nha, Hà Lan, Ấn Độ... tấp nập đến đây trao đổi hàng hóa (lụa là, gốm sứ, trầm hương, lâm sản).
Sự giao thoa văn hóa: Trong thời kỳ này, nhiều thương nhân Trung Quốc và Nhật Bản đã được phép định cư, xây dựng các khu phố, hội quán và nhà ở theo phong cách riêng của họ, hòa quyện với nền tảng kiến trúc truyền thống của người Việt, tạo nên một diện mạo đô thị độc nhất vô nhị.
Thời kỳ suy thoái & May mắn của lịch sử: Đến thế kỷ XIX, do sông Thu Bồn bị bồi lấp trầm tích và nhà Nguyễn thực hiện chính sách đóng cửa (bế quan tỏa cảng), cộng thêm sự trỗi dậy của cảng biển Đà Nẵng do người Pháp xây dựng, Hội An dần mất đi vị thế kinh tế. Tuy nhiên, chính sự "lãng quên" này lại giúp Hội An may mắn thoát khỏi quá trình đô thị hóa và sự tàn phá của chiến tranh để giữ gìn nguyên vẹn cấu trúc khu phố cổ cho đến ngày nay.
3. Kiến trúc giao thoa & Những di sản kiến trúc nổi bật
Kiến trúc Hội An mang đặc trưng với những ngôi nhà hình ống (bề ngang hẹp, chiều sâu rất dài) tường sơn màu vàng rơm nghệ thuật, mái lợp ngói âm dương và cấu trúc khung gỗ chịu lực vững chắc.
Chùa Cầu (Lai Viễn Kiều):
Được các thương nhân Nhật Bản xây dựng vào đầu thế kỷ XVII, là biểu tượng trường tồn của phố cổ.
Đây là công trình kiến trúc độc đáo "trên cầu dưới chùa", mái che cong mềm mại, chạm khắc tinh xảo. Cầu bắc qua một con lạch nhỏ thông ra sông Thu Bồn. Giữa cầu là ngôi miếu nhỏ thờ Bắc Đế Trấn Vũ – vị thần chuyên trị phong ba, lũ lụt theo tâm linh người xưa.
Các Hội quán của người Hoa:
Hội quán Phúc Kiến: Công trình quy mô và lộng lẫy nhất, thờ Thiên Hậu Thánh Mẫu (bà chúa đỡ đầu cho các thương thuyền vượt sóng gió). Hội quán nổi bật với cổng Tam quan rực rỡ, hòn non bộ và chính điện uy nghiêm.
Hội quán Quảng Đông, Triều Châu, Hải Nam: Mỗi hội quán mang một phong cách kiến trúc đặc trưng của từng vùng miền Trung Hoa với nghệ thuật khảm sành sứ và đắp nổi vô cùng sống động.
Các Nhà cổ trăm tuổi:
Nhà cổ Tân Ký, Nhà cổ Phùng Hưng, Nhà cổ Đức An: Đây là những ngôi nhà tư nhân có tuổi đời hơn 200 năm, nơi sinh sống của nhiều thế hệ trong một gia đình. Kiến trúc nhà kết hợp hài hòa giữa 3 nền văn hóa: Việt (mái lợp ngói âm dương, hệ cột kèo gỗ), Nhật (hệ thống xà gồ chồng rường mái hình thanh cua) và Trung Hoa (các chi tiết chạm khắc liễn đối, triện chữ). Trong nhà luôn có giếng trời ở giữa để lấy ánh sáng tự nhiên và điều hòa không khí.
4. Công nghệ AR và Xu hướng số hóa không gian Phố cổ
Hội An cổ kính nhưng không hề đứng ngoài dòng chảy công nghệ. Hiện nay, di sản này đang được ứng dụng mạnh mẽ các giải pháp số hóa để bảo tồn và thu hút du khách:
Số hóa 3D kiến trúc Phố cổ: Toàn bộ không gian các công trình đặc trưng như Chùa Cầu, Hội quán Phúc Kiến hay các ngôi nhà cổ đã được quét la-ze mặt đất (3D Laser Scanning) để lưu trữ dữ liệu kiến trúc chính xác đến từng milimet, phục vụ cho công tác trùng tu chính xác và hiển thị trên không gian số.
Trải nghiệm AR (Thực tế tăng cường) "Xuyên không về quá khứ":
Thông qua các ứng dụng di động thông minh tích hợp công nghệ AR, khi du khách đứng tại bờ sông Hoài hoặc trước các hội quán, bạn có thể quét camera để chứng kiến cảnh tượng thương cảng Hội An thế kỷ XVII tái hiện lại ngay trước mắt: các thuyền buôn gỗ mui buồm lớn của Nhật Bản, Bồ Đào Nha đang neo đậu; cảnh tiểu thương mặc trang phục cổ xưa tấp nập bốc dỡ hàng hóa trên bến dưới thuyền.
Tại di tích Chùa Cầu, AR giúp người xem tương tác, nhìn thấy các cấu trúc gỗ ẩn bên trong móng mố cầu hoặc tìm hiểu lịch sử linh vật Thần Thú (Tượng Khỉ đá và Chó đá trấn giữ hai đầu cầu) một cách trực quan bằng hình ảnh chuyển động 3D.
Bản đồ du lịch thông minh & Tour ảo 360°: Khách du lịch có thể thực hiện chuyến tham quan trực tuyến Phố cổ Hội An từ xa qua các góc nhìn Panorama 360 độ sắc nét, kết hợp trợ lý giọng nói AI hướng dẫn tường tận về lịch sử của từng góc phố, từng làng nghề truyền thống (làng gốm Thanh Hà, làng rau Trà Quế).
Phố cổ Hội An vừa mang vẻ đẹp đằm thắm, dịu dàng của ánh đèn lồng lung linh ban đêm, vừa chứa đựng một chiều sâu lịch sử văn hóa đồ sộ, nay lại càng trở nên sống động hơn bao giờ hết nhờ sự hỗ trợ của các công nghệ AR và số hóa di sản đương đại.', 15.879, 108.326, 'Quảng Nam', '/media/heritage/pho-co-hoi-an/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Phố Cổ Hội An
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000018-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222205', 'Chùa Cầu (Lai Viễn Kiều)', '/media/heritage/pho-co-hoi-an/cover.jpeg', 'Chùa Cầu (Lai Viễn Kiều):
Được các thương nhân Nhật Bản xây dựng vào đầu thế kỷ XVII, là biểu tượng trường tồn của phố cổ.
Đây là công trình kiến trúc độc đáo "trên cầu dưới chùa", mái che cong mềm mại, chạm khắc tinh xảo. Cầu bắc qua một con lạch nhỏ thông ra sông Thu Bồn. Giữa cầu là ngôi miếu…', 'Chùa Cầu (Lai Viễn Kiều):
Được các thương nhân Nhật Bản xây dựng vào đầu thế kỷ XVII, là biểu tượng trường tồn của phố cổ.
Đây là công trình kiến trúc độc đáo "trên cầu dưới chùa", mái che cong mềm mại, chạm khắc tinh xảo. Cầu bắc qua một con lạch nhỏ thông ra sông Thu Bồn. Giữa cầu là ngôi miếu nhỏ thờ Bắc Đế Trấn Vũ – vị thần chuyên trị phong ba, lũ lụt theo tâm linh người xưa.', 'artifact:pho-co-hoi-an-chua-cau', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000019-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222205', 'Hội quán Phúc Kiến', '/media/heritage/pho-co-hoi-an/cover.jpeg', 'Hội quán Phúc Kiến: Công trình quy mô và lộng lẫy nhất, thờ Thiên Hậu Thánh Mẫu (bà chúa đỡ đầu cho các thương thuyền vượt sóng gió). Hội quán nổi bật với cổng Tam quan rực rỡ, hòn non bộ và chính điện uy nghiêm.
Hội quán Quảng Đông, Triều Châu, Hải Nam: Mỗi hội quán mang một phong cách kiến…', 'Hội quán Phúc Kiến: Công trình quy mô và lộng lẫy nhất, thờ Thiên Hậu Thánh Mẫu (bà chúa đỡ đầu cho các thương thuyền vượt sóng gió). Hội quán nổi bật với cổng Tam quan rực rỡ, hòn non bộ và chính điện uy nghiêm.
Hội quán Quảng Đông, Triều Châu, Hải Nam: Mỗi hội quán mang một phong cách kiến trúc đặc trưng của từng vùng miền Trung Hoa với nghệ thuật khảm sành sứ và đắp nổi vô cùng sống động.', 'artifact:pho-co-hoi-an-hoi-quan-phuc-kien', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000020-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222205', 'Nhà cổ Tân Ký', '/media/heritage/pho-co-hoi-an/cover.jpeg', 'Các Nhà cổ trăm tuổi:
Nhà cổ Tân Ký, Nhà cổ Phùng Hưng, Nhà cổ Đức An: Đây là những ngôi nhà tư nhân có tuổi đời hơn 200 năm, nơi sinh sống của nhiều thế hệ trong một gia đình. Kiến trúc nhà kết hợp hài hòa giữa 3 nền văn hóa: Việt (mái lợp ngói âm dương, hệ cột kèo gỗ), Nhật (hệ thống xà gồ…', 'Các Nhà cổ trăm tuổi:
Nhà cổ Tân Ký, Nhà cổ Phùng Hưng, Nhà cổ Đức An: Đây là những ngôi nhà tư nhân có tuổi đời hơn 200 năm, nơi sinh sống của nhiều thế hệ trong một gia đình. Kiến trúc nhà kết hợp hài hòa giữa 3 nền văn hóa: Việt (mái lợp ngói âm dương, hệ cột kèo gỗ), Nhật (hệ thống xà gồ chồng rường mái hình thanh cua) và Trung Hoa (các chi tiết chạm khắc liễn đối, triện chữ). Trong nhà luôn có giếng trời ở giữa để lấy ánh sáng tự nhiên và điều hòa không khí.
4. Công nghệ AR và Xu hướng số hóa không gian Phố cổ
Hội An cổ kính nhưng không hề đứng ngoài dòng chảy công nghệ. Hiện nay, di sản này đang được ứng dụng mạnh mẽ các giải pháp số hóa để bảo tồn và thu hút du khách:
Số hóa 3D kiến trúc Phố cổ: Toàn bộ không gian các công trình đặc trưng như Chùa Cầu, Hội quán Phúc Kiến hay các ngôi nhà cổ đã được quét la-ze mặt đất (3D Laser Scanning) để lưu trữ dữ liệu kiến trúc chính xác đến từng milimet, phục vụ cho công tác trùng tu chính xác và hiển thị trên không gian số.', 'artifact:pho-co-hoi-an-nha-co-tan-ky', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000021-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222205', 'Tượng Khỉ và Chó đá', '/media/heritage/pho-co-hoi-an/cover.jpeg', 'Tại di tích Chùa Cầu, AR giúp người xem tương tác, nhìn thấy các cấu trúc gỗ ẩn bên trong móng mố cầu hoặc tìm hiểu lịch sử linh vật Thần Thú (Tượng Khỉ đá và Chó đá trấn giữ hai đầu cầu) một cách trực quan bằng hình ảnh chuyển động 3D.
Bản đồ du lịch thông minh & Tour ảo 360°: Khách du lịch có…', 'Tại di tích Chùa Cầu, AR giúp người xem tương tác, nhìn thấy các cấu trúc gỗ ẩn bên trong móng mố cầu hoặc tìm hiểu lịch sử linh vật Thần Thú (Tượng Khỉ đá và Chó đá trấn giữ hai đầu cầu) một cách trực quan bằng hình ảnh chuyển động 3D.
Bản đồ du lịch thông minh & Tour ảo 360°: Khách du lịch có thể thực hiện chuyến tham quan trực tuyến Phố cổ Hội An từ xa qua các góc nhìn Panorama 360 độ sắc nét, kết hợp trợ lý giọng nói AI hướng dẫn tường tận về lịch sử của từng góc phố, từng làng nghề truyền thống (làng gốm Thanh Hà, làng rau Trà Quế).
Phố cổ Hội An vừa mang vẻ đẹp đằm thắm, dịu dàng của ánh đèn lồng lung linh ban đêm, vừa chứa đựng một chiều sâu lịch sử văn hóa đồ sộ, nay lại càng trở nên sống động hơn bao giờ hết nhờ sự hỗ trợ của các công nghệ AR và số hóa di sản đương đại.', 'artifact:pho-co-hoi-an-linh-vat-cau', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222206', 'Thành Nhà Hồ', '1. Tổng quan & Vị trí
Vị trí: Nằm trên địa phận hai xã Vĩnh Tiến và Vĩnh Long, thuộc huyện Vĩnh Lộc, tỉnh Thanh Hóa.
Tên gọi khác: Thành Tây Đô, An Tôn, Tây Kinh hay Thạch Thành.
Tầm vóc: Đây là tòa thành kiên cố bằng đá có một không hai tại Việt Nam và khu vực. Thành Nhà Hồ được UNESCO chính thức công nhận là Di sản Văn hóa Thế giới vào năm 2011 nhờ các giá trị nổi bật toàn cầu về kiến trúc, kỹ thuật xây dựng và cảnh quan phong thủy.
2. Lịch sử hình thành: Tòa thành xây thần tốc trong 3 tháng
Bối cảnh lịch sử (1397): Cuối thế kỷ XIV, triều đại nhà Trần suy yếu nghiêm trọng. Quyền thần Hồ Quý Ly (lúc này là Đại sư triều Trần) đã quyết định dời đô từ kinh thành Thăng Long (Hà Nội) về đất An Tôn (Thanh Hóa) nhằm chuẩn bị cho việc thay thế nhà Trần và xây dựng phòng tuyến kháng chiến chống quân Minh xâm lược.
Xây dựng thần tốc: Tòa thành được khởi công vào mùa xuân năm 1397 dưới sự chỉ huy của Thượng thư Lại bộ Thái sử lệnh Đỗ Tỉnh. Điều kinh ngạc là toàn bộ công trình hoàng thành khổng lồ này được xây dựng và hoàn thành chỉ trong vòng 3 tháng (từ tháng Giêng đến tháng Ba năm 1397). Đến năm 1400, Hồ Quý Ly chính thức lên ngôi vua, lập ra nhà Hồ và chọn nơi đây làm kinh đô của nước Đại Ngu.
3. Kỹ thuật xếp đá độc đáo & Các hạng mục công trình
Điểm cốt lõi làm nên giá trị toàn cầu của Thành Nhà Hồ chính là kỹ thuật khai thác, vận chuyển và xây dựng bằng các khối đá vôi khổng lồ mà không cần bất kỳ một chất kết dính nào (như vôi vữa hay mật mía).
A. Kiến trúc Hoàng Thành (Thành Nội)
Cấu trúc: Thành được xây theo hình chữ nhật gần vuông (chiều Nam - Bắc dài 870,5m; Đông - Tây dài 883,5m).
Những khối đá khổng lồ: Tường thành được xây dựng bằng cách đục đẽo các khối đá thạch gọc vuông vức, có khối dài tới 6m, nặng hơn 20 tấn. Các phiến đá được mài nhẵn và xếp chồng lên nhau theo kỹ thuật mộng mẹo khít khao đến mức một lưỡi dao mỏng cũng không thể lách qua được. Mặt trong của tường thành được ốp bằng đất thện nện chặt kiên cố để chịu lực.
4 Cổng thành (Tiền - Hậu - Tả - Hữu): Thành có 4 cổng mở theo 4 hướng Đông, Tây, Nam, Bắc.
Cổng Nam (Cổng Tiền): Là cổng chính, đồ sộ nhất với kiến trúc 3 vòm cuốn bằng đá, cao hơn 8m. Các vòm cuốn được ghép bằng những phiến đá hình múi bưởi cực kỳ chuẩn xác về mặt hình học và kỹ thuật chịu lực. Ba cổng còn lại chỉ có 1 vòm cuốn.
B. Các công trình phụ cận và ngoại vi
Đàn tế Nam Giao: Nằm ở phía Nam ngoài thành (trên núi Đốn Sơn), là nơi vua nhà Hồ tổ chức các nghi lễ tế trời đất, cầu cho quốc thái dân an. Đây là đàn tế Nam Giao còn giữ được mặt bằng nền móng nguyên vẹn nhất trong lịch sử khảo cổ Việt Nam.
La Thành: Hệ thống thành đất vòng ngoài dài hơn 10km, nương theo điều kiện địa hình tự nhiên (sông Mã, sông Bưởi và núi non) để làm rào chắn bảo vệ kinh thành khỏi giặc ngoại xâm và lũ lụt.
Hào thành: Hệ thống hào nước sâu bao bọc ngay sát phía ngoài tường thành đá, nối trực tiếp với sông sông để điều tiết nước.
4. Tiềm năng ứng dụng công nghệ AR và Số hóa tại Thành Nhà Hồ
Dù các công trình điện thờ bằng gỗ bên trong Thành Nội đã bị thời gian và chiến tranh hủy hoại, nhưng công nghệ AR và số hóa đang mở ra cơ hội "hồi sinh" di sản này:
Khám phá 3D cổ vật lòng đất: Qua các cuộc khai quật khảo cổ, các nhà khoa học đã tìm thấy hàng vạn viên gạch in chữ "Tây Đô", ngói mũi hài, các đầu chim phượng bằng đất nung và đặc biệt là hàng trăm quả đạn đá (dùng cho súng thần cơ của Hồ Nguyên Trừng). Các hiện vật này hiện đang được số hóa 3D giúp du khách tương tác xoay lật trên màn hình để nghiên cứu cấu trúc.
Tái hiện Điện Hoàng Nguyên bằng AR: Trong tương lai gần, công nghệ thực tế tăng cường (AR) sẽ giúp phục dựng lại không gian nội thành. Khi đứng trên nền đất trống hiện tại và nhìn qua màn hình thiết bị AR, du khách có thể thấy quần thể kiến trúc gỗ nguy nga như điện Hoàng Nguyên, cung Nhân Thọ... hiển thị chân thực ngay tại tọa độ gốc.
Giải mã kỹ thuật nâng đá bằng mô phỏng ảo: Ứng dụng công nghệ VR/AR để mô phỏng lại cách người thợ thủ công thế kỷ 15 vận chuyển các khối đá nặng hàng chục tấn từ núi An Tôn về thành và dùng hệ thống đòn bẩy, con lăn để đưa đá lên cao – giải mã bí ẩn xây thành chỉ trong 90 ngày cho khách tham quan.
Thành Nhà Hồ không chỉ là minh chứng cho một giai đoạn lịch sử đầy biến động mà còn là đài kỷ niệm tôn vinh tài năng kỹ nghệ, tư duy kiến trúc quân sự vượt thời gian của người Việt cổ.', 20.077, 105.603, 'Thanh Hóa', '/media/heritage/thanh-nha-ho/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Thành Nhà Hồ
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000022-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222206', 'Cổng Nam (Cổng Tiền)', '/media/heritage/thanh-nha-ho/cover.jpeg', 'Cổng Nam (Cổng Tiền): Là cổng chính, đồ sộ nhất với kiến trúc 3 vòm cuốn bằng đá, cao hơn 8m. Các vòm cuốn được ghép bằng những phiến đá hình múi bưởi cực kỳ chuẩn xác về mặt hình học và kỹ thuật chịu lực. Ba cổng còn lại chỉ có 1 vòm cuốn.
B. Các công trình phụ cận và ngoại vi
Đàn tế Nam Giao:…', 'Cổng Nam (Cổng Tiền): Là cổng chính, đồ sộ nhất với kiến trúc 3 vòm cuốn bằng đá, cao hơn 8m. Các vòm cuốn được ghép bằng những phiến đá hình múi bưởi cực kỳ chuẩn xác về mặt hình học và kỹ thuật chịu lực. Ba cổng còn lại chỉ có 1 vòm cuốn.
B. Các công trình phụ cận và ngoại vi
Đàn tế Nam Giao: Nằm ở phía Nam ngoài thành (trên núi Đốn Sơn), là nơi vua nhà Hồ tổ chức các nghi lễ tế trời đất, cầu cho quốc thái dân an. Đây là đàn tế Nam Giao còn giữ được mặt bằng nền móng nguyên vẹn nhất trong lịch sử khảo cổ Việt Nam.
La Thành: Hệ thống thành đất vòng ngoài dài hơn 10km, nương theo điều kiện địa hình tự nhiên (sông Mã, sông Bưởi và núi non) để làm rào chắn bảo vệ kinh thành khỏi giặc ngoại xâm và lũ lụt.
Hào thành: Hệ thống hào nước sâu bao bọc ngay sát phía ngoài tường thành đá, nối trực tiếp với sông sông để điều tiết nước.
4. Tiềm năng ứng dụng công nghệ AR và Số hóa tại Thành Nhà Hồ
Dù các công trình điện thờ bằng gỗ bên trong Thành Nội đã bị thời gian và chiến tranh hủy hoại, nhưng công nghệ AR và số hóa đang mở ra cơ hội "hồi sinh" di sản này:
Khám phá 3D cổ vật lòng đất: Qua các cuộc khai quật khảo cổ, các nhà khoa học đã tìm thấy hàng vạn viên gạch in chữ "Tây Đô", ngói mũi hài, các đầu chim phượng bằng đất nung và đặc biệt là hàng trăm quả đạn đá (dùng cho súng thần cơ của Hồ Nguyên Trừng). Các hiện vật này hiện đang được số hóa 3D giúp du khách tương tác xoay lật trên màn hình để nghiên cứu cấu trúc.', 'artifact:thanh-nha-ho-cong-nam', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000023-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222206', 'Khối đá thạch anh', '/media/heritage/thanh-nha-ho/cover.jpeg', 'Điểm cốt lõi làm nên giá trị toàn cầu của Thành Nhà Hồ chính là kỹ thuật khai thác, vận chuyển và xây dựng bằng các khối đá vôi khổng lồ mà không cần bất kỳ một chất kết dính nào (như vôi vữa hay mật mía).
A. Kiến trúc Hoàng Thành (Thành Nội)
Cấu trúc: Thành được xây theo hình chữ nhật gần vuông…', 'Điểm cốt lõi làm nên giá trị toàn cầu của Thành Nhà Hồ chính là kỹ thuật khai thác, vận chuyển và xây dựng bằng các khối đá vôi khổng lồ mà không cần bất kỳ một chất kết dính nào (như vôi vữa hay mật mía).
A. Kiến trúc Hoàng Thành (Thành Nội)
Cấu trúc: Thành được xây theo hình chữ nhật gần vuông (chiều Nam - Bắc dài 870,5m; Đông - Tây dài 883,5m).
Những khối đá khổng lồ: Tường thành được xây dựng bằng cách đục đẽo các khối đá thạch gọc vuông vức, có khối dài tới 6m, nặng hơn 20 tấn. Các phiến đá được mài nhẵn và xếp chồng lên nhau theo kỹ thuật mộng mẹo khít khao đến mức một lưỡi dao mỏng cũng không thể lách qua được. Mặt trong của tường thành được ốp bằng đất thện nện chặt kiên cố để chịu lực.
4 Cổng thành (Tiền - Hậu - Tả - Hữu): Thành có 4 cổng mở theo 4 hướng Đông, Tây, Nam, Bắc.
Cổng Nam (Cổng Tiền): Là cổng chính, đồ sộ nhất với kiến trúc 3 vòm cuốn bằng đá, cao hơn 8m. Các vòm cuốn được ghép bằng những phiến đá hình múi bưởi cực kỳ chuẩn xác về mặt hình học và kỹ thuật chịu lực. Ba cổng còn lại chỉ có 1 vòm cuốn.
B. Các công trình phụ cận và ngoại vi
Đàn tế Nam Giao: Nằm ở phía Nam ngoài thành (trên núi Đốn Sơn), là nơi vua nhà Hồ tổ chức các nghi lễ tế trời đất, cầu cho quốc thái dân an. Đây là đàn tế Nam Giao còn giữ được mặt bằng nền móng nguyên vẹn nhất trong lịch sử khảo cổ Việt Nam.', 'artifact:thanh-nha-ho-khoi-da', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000024-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222206', 'Gạch in chữ Tây Đô', '/media/heritage/thanh-nha-ho/cover.jpeg', 'Tên gọi khác: Thành Tây Đô, An Tôn, Tây Kinh hay Thạch Thành.
Tầm vóc: Đây là tòa thành kiên cố bằng đá có một không hai tại Việt Nam và khu vực. Thành Nhà Hồ được UNESCO chính thức công nhận là Di sản Văn hóa Thế giới vào năm 2011 nhờ các giá trị nổi bật toàn cầu về kiến trúc, kỹ thuật xây dựng…', 'Tên gọi khác: Thành Tây Đô, An Tôn, Tây Kinh hay Thạch Thành.
Tầm vóc: Đây là tòa thành kiên cố bằng đá có một không hai tại Việt Nam và khu vực. Thành Nhà Hồ được UNESCO chính thức công nhận là Di sản Văn hóa Thế giới vào năm 2011 nhờ các giá trị nổi bật toàn cầu về kiến trúc, kỹ thuật xây dựng và cảnh quan phong thủy.
2. Lịch sử hình thành: Tòa thành xây thần tốc trong 3 tháng
Bối cảnh lịch sử (1397): Cuối thế kỷ XIV, triều đại nhà Trần suy yếu nghiêm trọng. Quyền thần Hồ Quý Ly (lúc này là Đại sư triều Trần) đã quyết định dời đô từ kinh thành Thăng Long (Hà Nội) về đất An Tôn (Thanh Hóa) nhằm chuẩn bị cho việc thay thế nhà Trần và xây dựng phòng tuyến kháng chiến chống quân Minh xâm lược.
Xây dựng thần tốc: Tòa thành được khởi công vào mùa xuân năm 1397 dưới sự chỉ huy của Thượng thư Lại bộ Thái sử lệnh Đỗ Tỉnh. Điều kinh ngạc là toàn bộ công trình hoàng thành khổng lồ này được xây dựng và hoàn thành chỉ trong vòng 3 tháng (từ tháng Giêng đến tháng Ba năm 1397). Đến năm 1400, Hồ Quý Ly chính thức lên ngôi vua, lập ra nhà Hồ và chọn nơi đây làm kinh đô của nước Đại Ngu.
3. Kỹ thuật xếp đá độc đáo & Các hạng mục công trình
Điểm cốt lõi làm nên giá trị toàn cầu của Thành Nhà Hồ chính là kỹ thuật khai thác, vận chuyển và xây dựng bằng các khối đá vôi khổng lồ mà không cần bất kỳ một chất kết dính nào (như vôi vữa hay mật mía).
A. Kiến trúc Hoàng Thành (Thành Nội)', 'artifact:thanh-nha-ho-gach-tay-do', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000025-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222206', 'Đạn đá thần cơ', '/media/heritage/thanh-nha-ho/cover.jpeg', 'Khám phá 3D cổ vật lòng đất: Qua các cuộc khai quật khảo cổ, các nhà khoa học đã tìm thấy hàng vạn viên gạch in chữ "Tây Đô", ngói mũi hài, các đầu chim phượng bằng đất nung và đặc biệt là hàng trăm quả đạn đá (dùng cho súng thần cơ của Hồ Nguyên Trừng). Các hiện vật này hiện đang được số hóa 3D…', 'Khám phá 3D cổ vật lòng đất: Qua các cuộc khai quật khảo cổ, các nhà khoa học đã tìm thấy hàng vạn viên gạch in chữ "Tây Đô", ngói mũi hài, các đầu chim phượng bằng đất nung và đặc biệt là hàng trăm quả đạn đá (dùng cho súng thần cơ của Hồ Nguyên Trừng). Các hiện vật này hiện đang được số hóa 3D giúp du khách tương tác xoay lật trên màn hình để nghiên cứu cấu trúc.
Tái hiện Điện Hoàng Nguyên bằng AR: Trong tương lai gần, công nghệ thực tế tăng cường (AR) sẽ giúp phục dựng lại không gian nội thành. Khi đứng trên nền đất trống hiện tại và nhìn qua màn hình thiết bị AR, du khách có thể thấy quần thể kiến trúc gỗ nguy nga như điện Hoàng Nguyên, cung Nhân Thọ... hiển thị chân thực ngay tại tọa độ gốc.
Giải mã kỹ thuật nâng đá bằng mô phỏng ảo: Ứng dụng công nghệ VR/AR để mô phỏng lại cách người thợ thủ công thế kỷ 15 vận chuyển các khối đá nặng hàng chục tấn từ núi An Tôn về thành và dùng hệ thống đòn bẩy, con lăn để đưa đá lên cao – giải mã bí ẩn xây thành chỉ trong 90 ngày cho khách tham quan.
Thành Nhà Hồ không chỉ là minh chứng cho một giai đoạn lịch sử đầy biến động mà còn là đài kỷ niệm tôn vinh tài năng kỹ nghệ, tư duy kiến trúc quân sự vượt thời gian của người Việt cổ.', 'artifact:thanh-nha-ho-dan-da', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222207', 'văn miếu quốc tử giám', '1. Tổng quan & Vị trí
Vị trí: Nằm tại số 58 phố Quốc Tử Giám, quận Đống Đa, Hà Nội. Khu di tích được bao bọc bởi 4 tuyến phố trung tâm: Nguyễn Thái Học, Tôn Đức Thắng, Văn Miếu và Quốc Tử Giám.
Tầm vóc: Đây là một trong những khu di tích lịch sử - văn hóa trọng điểm của thủ đô và cả nước. Năm 2010, 82 bia Tiến sĩ tại đây đã được UNESCO công nhận là Di sản tư liệu thế giới thuộc Chương trình Ký ức Thế giới.
2. Lịch sử hình thành và Phát triển
Quần thể kiến trúc này thực chất gồm hai phần chính gắn liền với hai mốc thời gian lịch sử:
Văn Miếu (Năm 1070): Được vua Lý Thánh Tông cho xây dựng vào mùa thu năm Thần Vũ thứ 2 để thờ các bậc thánh hiền Nho giáo (Khổng Tử, Tứ Phối) và cũng là nơi hoàng thái tử (sau này là vua Lý Nhân Tông) đến học tập.
Quốc Tử Giám (Năm 1076): Vua Lý Nhân Tông cho xây dựng thêm Quốc Tử Giám ngay sau Văn Miếu. Ban đầu, trường chỉ dành riêng cho con em hoàng tộc và đại thần (nên gọi là Quốc Tử).
Mở rộng cửa cho nhân tài (Năm 1236): Sang thời nhà Trần, vua Trần Thái Tông đổi tên thành Quốc Học Viện và bắt đầu thu nhận cả những con em thường dân có học lực xuất sắc ở các địa phương.
Thời kỳ hưng thịnh (Thời Lê Sơ): Dưới triều vua Lê Thánh Tông, nền khoa bảng Nho học đạt đến đỉnh cao. Ông cho dựng các tấm bia Tiến sĩ đầu tiên (năm 1484) để vinh danh những người đỗ đại khoa, tạo nguồn động lực lớn cho các sĩ tử đương thời.
3. Cấu trúc không gian & Những báu vật quốc gia
Toàn bộ khuôn viên Văn Miếu - Quốc Tử Giám được bao quanh bởi tường gạch vồ kiên cố, bố cục đối xứng từng khu vực theo trục chính từ Nam ra Bắc, mô phỏng theo khuôn mẫu của Văn Miếu Khổng Tử tại Trung Quốc nhưng mang đậm bản sắc kiến trúc dân tộc Việt Nam.
A. Trục tham quan chính (5 khu vực)
Khu thứ nhất (Từ cổng chính Văn Miếu Môn đến cổng Đại Trung Môn): Không gian mở với thảm cỏ xanh, cây cổ thụ, tạo cảm giác thanh tịnh tách biệt với phố thị ồn ào.
Khu thứ hai (Khuê Văn Các):
Được xây dựng vào năm 1805 dưới thời Nguyễn. Đây là lầu vuông tám mái, bốn mặt có cửa sổ tròn lồng các thanh gỗ tỏa ra như vầng mặt trời tỏa sáng.
Khuê Văn Các (gác vẻ đẹp của sao Khuê - ngôi sao chủ về văn học) chính là biểu tượng chính thức của thủ đô Hà Nội.
Khu thứ ba (Hồ Thiên Quang & Vườn bia Tiến sĩ):
Hồ Thiên Quang: Hồ nước hình vuông nằm ngay sau Khuê Văn Các, soi bóng giếng trời, mang ý nghĩa tụ hội tinh hoa đất trời.
82 tấm bia Tiến sĩ: Nằm đối xứng hai bên hồ, mỗi bên 41 bia. Các tấm bia đá được đặt trên lưng Rùa đá (biểu tượng của sự trường tồn, bền vững). Trên bia khắc tên tuổi, quê quán của 1.304 vị Tiến sĩ đỗ đạt qua các kỳ thi đại khoa từ năm 1442 đến 1779.
Khu thứ tư (Cổng Đại Thành & Điện Đại Thành): Khu vực tâm linh chính, nơi thờ Khổng Tử và các học trò xuất sắc của ông. Tại đây cũng thờ Tư nghiệp Quốc Tử Giám Chu Văn An – người thầy mẫu mực, người được mệnh danh là "vạn thế sư biểu" (người thầy của muôn đời) của giáo dục Việt Nam.
Khu thứ sáu (Khu Thái Học): Nơi ngày xưa là trường học, nhà giảng đường, ký túc xá cho sĩ tử. Từng bị Pháp phá hủy hoàn toàn vào năm 1946, khu vực này đã được phục dựng quy mô vào năm 2000 để làm nơi tôn vinh, triển lãm các giá trị giáo dục xưa và nay.
4. Số hóa di sản và các trải nghiệm Công nghệ
Không đứng ngoài làn sóng số hóa, Văn Miếu - Quốc Tử Giám hiện là một trong những không gian di sản chuyển đổi số thành công bậc nhất tại Hà Nội:
Hệ thống thuyết minh tự động (Audio Guide): Hỗ trợ nhiều ngôn ngữ, giúp du khách tự trải nghiệm sâu sắc từng câu chuyện lịch sử của 82 bia Tiến sĩ thông qua thiết bị cầm tay hoặc quét mã QR trên điện thoại.
Trình chiếu công nghệ 3D Mapping (Tour đêm Văn Miếu): Đây là điểm nhấn công nghệ cực kỳ ấn tượng. Vào ban đêm, toàn bộ mặt tiền của khu Thái Học hay Khuê Văn Các trở thành một "màn hình khổng lồ" trình chiếu ánh sáng và âm thanh 3D, tái hiện lại quá trình lều chọi đi thi, vinh quy bái tổ của các sĩ tử ngày xưa một cách vô cùng sống động.
Số hóa tương tác tương lai: Du khách có thể trải nghiệm lớp học thầy đồ ảo, tự tay viết chữ thư pháp trên màn hình tương tác cảm ứng, hoặc ngắm nhìn các hoa văn điêu khắc rùa đá, bia đá được dựng hình 3D xoay 360 độ cực kỳ sắc nét.
Văn Miếu - Quốc Tử Giám không chỉ là nơi để cầu may mắn, đỗ đạt trước mỗi mùa thi, mà còn là không gian văn hóa kết nối quá khứ huy hoàng với hiện tại bằng những trải nghiệm công nghệ đầy sáng tạo.', 21.027, 105.835, 'Hà Nội', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: văn miếu quốc tử giám
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000026-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222207', 'Khuê Văn Các', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg', 'Khu thứ hai (Khuê Văn Các):
Được xây dựng vào năm 1805 dưới thời Nguyễn. Đây là lầu vuông tám mái, bốn mặt có cửa sổ tròn lồng các thanh gỗ tỏa ra như vầng mặt trời tỏa sáng.
Khuê Văn Các (gác vẻ đẹp của sao Khuê - ngôi sao chủ về văn học) chính là biểu tượng chính thức của thủ đô Hà Nội.', 'Khu thứ hai (Khuê Văn Các):
Được xây dựng vào năm 1805 dưới thời Nguyễn. Đây là lầu vuông tám mái, bốn mặt có cửa sổ tròn lồng các thanh gỗ tỏa ra như vầng mặt trời tỏa sáng.
Khuê Văn Các (gác vẻ đẹp của sao Khuê - ngôi sao chủ về văn học) chính là biểu tượng chính thức của thủ đô Hà Nội.', 'artifact:van-mieu-quoc-tu-giam-khue-van-cac', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000027-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222207', '82 bia Tiến sĩ', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg', 'Tầm vóc: Đây là một trong những khu di tích lịch sử - văn hóa trọng điểm của thủ đô và cả nước. Năm 2010, 82 bia Tiến sĩ tại đây đã được UNESCO công nhận là Di sản tư liệu thế giới thuộc Chương trình Ký ức Thế giới.
2. Lịch sử hình thành và Phát triển
Quần thể kiến trúc này thực chất gồm hai…', 'Tầm vóc: Đây là một trong những khu di tích lịch sử - văn hóa trọng điểm của thủ đô và cả nước. Năm 2010, 82 bia Tiến sĩ tại đây đã được UNESCO công nhận là Di sản tư liệu thế giới thuộc Chương trình Ký ức Thế giới.
2. Lịch sử hình thành và Phát triển
Quần thể kiến trúc này thực chất gồm hai phần chính gắn liền với hai mốc thời gian lịch sử:
Văn Miếu (Năm 1070): Được vua Lý Thánh Tông cho xây dựng vào mùa thu năm Thần Vũ thứ 2 để thờ các bậc thánh hiền Nho giáo (Khổng Tử, Tứ Phối) và cũng là nơi hoàng thái tử (sau này là vua Lý Nhân Tông) đến học tập.
Quốc Tử Giám (Năm 1076): Vua Lý Nhân Tông cho xây dựng thêm Quốc Tử Giám ngay sau Văn Miếu. Ban đầu, trường chỉ dành riêng cho con em hoàng tộc và đại thần (nên gọi là Quốc Tử).
Mở rộng cửa cho nhân tài (Năm 1236): Sang thời nhà Trần, vua Trần Thái Tông đổi tên thành Quốc Học Viện và bắt đầu thu nhận cả những con em thường dân có học lực xuất sắc ở các địa phương.
Thời kỳ hưng thịnh (Thời Lê Sơ): Dưới triều vua Lê Thánh Tông, nền khoa bảng Nho học đạt đến đỉnh cao. Ông cho dựng các tấm bia Tiến sĩ đầu tiên (năm 1484) để vinh danh những người đỗ đại khoa, tạo nguồn động lực lớn cho các sĩ tử đương thời.
3. Cấu trúc không gian & Những báu vật quốc gia', 'artifact:van-mieu-quoc-tu-giam-bia-tien-si', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000028-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222207', 'Hồ Thiên Quang', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg', 'Khu thứ ba (Hồ Thiên Quang & Vườn bia Tiến sĩ):
Hồ Thiên Quang: Hồ nước hình vuông nằm ngay sau Khuê Văn Các, soi bóng giếng trời, mang ý nghĩa tụ hội tinh hoa đất trời.
82 tấm bia Tiến sĩ: Nằm đối xứng hai bên hồ, mỗi bên 41 bia. Các tấm bia đá được đặt trên lưng Rùa đá (biểu tượng của sự…', 'Khu thứ ba (Hồ Thiên Quang & Vườn bia Tiến sĩ):
Hồ Thiên Quang: Hồ nước hình vuông nằm ngay sau Khuê Văn Các, soi bóng giếng trời, mang ý nghĩa tụ hội tinh hoa đất trời.
82 tấm bia Tiến sĩ: Nằm đối xứng hai bên hồ, mỗi bên 41 bia. Các tấm bia đá được đặt trên lưng Rùa đá (biểu tượng của sự trường tồn, bền vững). Trên bia khắc tên tuổi, quê quán của 1.304 vị Tiến sĩ đỗ đạt qua các kỳ thi đại khoa từ năm 1442 đến 1779.
Khu thứ tư (Cổng Đại Thành & Điện Đại Thành): Khu vực tâm linh chính, nơi thờ Khổng Tử và các học trò xuất sắc của ông. Tại đây cũng thờ Tư nghiệp Quốc Tử Giám Chu Văn An – người thầy mẫu mực, người được mệnh danh là "vạn thế sư biểu" (người thầy của muôn đời) của giáo dục Việt Nam.
Khu thứ sáu (Khu Thái Học): Nơi ngày xưa là trường học, nhà giảng đường, ký túc xá cho sĩ tử. Từng bị Pháp phá hủy hoàn toàn vào năm 1946, khu vực này đã được phục dựng quy mô vào năm 2000 để làm nơi tôn vinh, triển lãm các giá trị giáo dục xưa và nay.
4. Số hóa di sản và các trải nghiệm Công nghệ
Không đứng ngoài làn sóng số hóa, Văn Miếu - Quốc Tử Giám hiện là một trong những không gian di sản chuyển đổi số thành công bậc nhất tại Hà Nội:
Hệ thống thuyết minh tự động (Audio Guide): Hỗ trợ nhiều ngôn ngữ, giúp du khách tự trải nghiệm sâu sắc từng câu chuyện lịch sử của 82 bia Tiến sĩ thông qua thiết bị cầm tay hoặc quét mã QR trên điện thoại.', 'artifact:van-mieu-quoc-tu-giam-ho-thien-quang', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000029-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222207', 'Tượng Chu Văn An', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg', 'Khu thứ tư (Cổng Đại Thành & Điện Đại Thành): Khu vực tâm linh chính, nơi thờ Khổng Tử và các học trò xuất sắc của ông. Tại đây cũng thờ Tư nghiệp Quốc Tử Giám Chu Văn An – người thầy mẫu mực, người được mệnh danh là "vạn thế sư biểu" (người thầy của muôn đời) của giáo dục Việt Nam.
Khu thứ sáu…', 'Khu thứ tư (Cổng Đại Thành & Điện Đại Thành): Khu vực tâm linh chính, nơi thờ Khổng Tử và các học trò xuất sắc của ông. Tại đây cũng thờ Tư nghiệp Quốc Tử Giám Chu Văn An – người thầy mẫu mực, người được mệnh danh là "vạn thế sư biểu" (người thầy của muôn đời) của giáo dục Việt Nam.
Khu thứ sáu (Khu Thái Học): Nơi ngày xưa là trường học, nhà giảng đường, ký túc xá cho sĩ tử. Từng bị Pháp phá hủy hoàn toàn vào năm 1946, khu vực này đã được phục dựng quy mô vào năm 2000 để làm nơi tôn vinh, triển lãm các giá trị giáo dục xưa và nay.
4. Số hóa di sản và các trải nghiệm Công nghệ
Không đứng ngoài làn sóng số hóa, Văn Miếu - Quốc Tử Giám hiện là một trong những không gian di sản chuyển đổi số thành công bậc nhất tại Hà Nội:
Hệ thống thuyết minh tự động (Audio Guide): Hỗ trợ nhiều ngôn ngữ, giúp du khách tự trải nghiệm sâu sắc từng câu chuyện lịch sử của 82 bia Tiến sĩ thông qua thiết bị cầm tay hoặc quét mã QR trên điện thoại.
Trình chiếu công nghệ 3D Mapping (Tour đêm Văn Miếu): Đây là điểm nhấn công nghệ cực kỳ ấn tượng. Vào ban đêm, toàn bộ mặt tiền của khu Thái Học hay Khuê Văn Các trở thành một "màn hình khổng lồ" trình chiếu ánh sáng và âm thanh 3D, tái hiện lại quá trình lều chọi đi thi, vinh quy bái tổ của các sĩ tử ngày xưa một cách vô cùng sống động.
Số hóa tương tác tương lai: Du khách có thể trải nghiệm lớp học thầy đồ ảo, tự tay viết chữ thư pháp trên màn hình tương tác cảm ứng, hoặc ngắm nhìn các hoa văn điêu khắc rùa đá, bia đá được dựng hình 3D xoay 360 độ cực kỳ sắc nét.
Văn Miếu - Quốc Tử Giám không chỉ là nơi để cầu may mắn, đỗ đạt trước mỗi mùa thi, mà còn là không gian văn hóa kết nối quá khứ huy hoàng với hiện tại bằng những trải nghiệm công nghệ đầy sáng tạo.', 'artifact:van-mieu-quoc-tu-giam-chu-van-an', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222208', 'Đại nội Huế', '1. Tổng quan & Vị trí
Vị trí: Nằm ở bờ Bắc sông Hương, thuộc địa phận kinh thành Huế.
Khái niệm: Đại Nội là cụm di tích bao gồm Hoàng Thành (nơi vua thiết triều, làm việc và các cung điện thờ tự) và Tử Cấm Thành (nơi sinh hoạt, ăn ở của vua và hoàng gia).
Tầm vóc: Đây là công trình có quy mô đồ sộ nhất trong lịch sử Việt Nam, mất gần 30 năm với hàng vạn người tham gia xây dựng và là hạt nhân của Quần thể di tích Cố đô Huế (được UNESCO công nhận là Di sản Văn hóa Thế giới từ năm 1993).
2. Lịch sử hình thành
Khởi dựng (1804): Sau khi lên ngôi và thống nhất đất nước, vua Gia Long đã khảo sát và chọn khu đất bên bờ sông Hương để xây dựng kinh thành mới. Việc xây dựng Hoàng Thành bắt đầu từ mùa hè năm 1804.
Hoàn thiện (1833): Quy hoạch và kiến trúc của Đại Nội chỉ thực sự hoàn chỉnh dưới thời vua Minh Mạng vào năm 1833, khi hàng loạt cung điện, lầu các và hệ thống tường thành bao quanh được hoàn thành.
Biến động lịch sử: Trải qua 143 năm là thủ đô của triều Nguyễn (1802 – 1945), Đại Nội đã chứng kiến nhiều thăng trầm. Do tác động của chiến tranh (đặc biệt là các sự kiện năm 1947 và 1968), nhiều công trình nguy nga bên trong từng bị tàn phá, san phẳng hoàn toàn. Những năm gần đây, công tác trùng tu đang được đẩy mạnh để phục dựng lại nguyên bản các điện thờ lớn.
3. Cấu trúc không gian & Các công trình tiêu biểu
Đại Nội được xây dựng theo sơ đồ gần như hình vuông, chu vi khoảng 2,5km, mặt mặt hướng về phía Nam.
A. Hệ thống cửa ngõ & Khu vực thiết triều (Hoàng Thành)
Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua Bảo Đại thoái vị vào năm 1945, chấm dứt chế độ phong kiến Việt Nam.
Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)
Nằm ngay sau điện Thái Hòa, được giới hạn bởi bức tường thành bảo vệ nghiêm ngặt.
Tả Vu & Hữu Vu: Hai tòa nhà đối xứng ngay sau điện Thái Hòa, nơi các quan chuẩn bị áo mũ chỉnh tề trước khi vào chầu vua.
Điện Cần Chánh & Điện Càn Thành: Nơi vua làm việc hàng ngày và nơi ngủ nghỉ của nhà vua (đang trong quá trình phục dựng).
Cung Diên Thọ & Cung Trường Sanh: Nằm ở phía Tây của Tử Cấm Thành, là nơi ở và sinh hoạt của Hoàng Thái Hậu (mẹ vua) và Thái Hoàng Thái Hậu (bà nội vua). Các công trình này mang vẻ đẹp thanh nhã, có hồ nước và sân vườn rất thoáng đãng.
C. Khu vực thờ tự
Thế Miếu (Thế Tổ Miếu): Nơi thờ các vị vua triều Nguyễn.
Cửu Đỉnh: Đặt trước sân Thế Miếu, gồm 9 chiếc đỉnh đồng lớn được đúc dưới thời vua Minh Mạng (1835). Mỗi chiếc đỉnh mang tên hiệu của một vị vua và được chạm khắc tinh xảo các hình ảnh biểu tượng cho giang sơn, sông núi, sản vật của Việt Nam. Đây là những bảo vật quốc gia có một không hai.
Hiển Lâm Các: Công trình kiến trúc bằng gỗ cao 3 tầng, được ví như đài kỷ niệm ghi nhớ công ơn của các bậc công thần đã có đóng góp cho triều đại.
4. Công nghệ AR & Trải nghiệm thực tế ảo tại Đại Nội
Như dòng trạng thái bạn chia sẻ ("mở khóa trải nghiệm thực tế ảo"), Đại Nội Huế hiện nay là một trong những điểm đi đầu tại Việt Nam trong việc ứng dụng công nghệ vào du lịch di sản:
Trải nghiệm AR (Augmented Reality - Thực tế tăng cường): Du khách có thể dùng điện thoại thông minh hoặc máy tính bảng quét các bảng mã tại điểm di tích để "hồi sinh" các cung điện đã bị chiến tranh phá hủy. Bạn sẽ nhìn thấy kiến trúc 3D nguyên bản của Điện Cần Chánh, Điện Kiến Trung... hiện lên ngay trên nền móng đổ nát cũ một cách sống động.
Không gian ảo VR 3D (Virtual Reality): Tại khu vực Trung tâm Phục dựng Di sản, du khách có thể đeo kính VR để trải nghiệm tour tham quan xuyên không gian, tìm hiểu về lối sống cung đình, xem các nghi lễ triều chính mô phỏng như thật.
Quét mã ngắm cổ vật: Các bảo vật quốc gia như Cửu Đỉnh hay các hiện vật trong điện thờ đều được số hóa, cho phép người xem xoay 360 độ trên thiết bị để nhìn rõ từng đường nét chạm khắc nhỏ nhất.
Đại Nội Huế không chỉ là một chuyến đi tham quan lịch sử đơn thuần, mà là sự giao thoa giữa giá trị cổ kính cốt lõi và dòng chảy công nghệ hiện đại, giúp thế hệ trẻ dễ dàng tiếp cận quá khứ một cách trực quan nhất.', 16.469, 107.579, 'Huế', '/media/heritage/dai-noi-hue/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Đại nội Huế
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000030-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222208', 'Ngọ Môn', '/media/heritage/dai-noi-hue/cover.jpeg', 'Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua…', 'Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua Bảo Đại thoái vị vào năm 1945, chấm dứt chế độ phong kiến Việt Nam.
Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)
Nằm ngay sau điện Thái Hòa, được giới hạn bởi bức tường thành bảo vệ nghiêm ngặt.
Tả Vu & Hữu Vu: Hai tòa nhà đối xứng ngay sau điện Thái Hòa, nơi các quan chuẩn bị áo mũ chỉnh tề trước khi vào chầu vua.
Điện Cần Chánh & Điện Càn Thành: Nơi vua làm việc hàng ngày và nơi ngủ nghỉ của nhà vua (đang trong quá trình phục dựng).
Cung Diên Thọ & Cung Trường Sanh: Nằm ở phía Tây của Tử Cấm Thành, là nơi ở và sinh hoạt của Hoàng Thái Hậu (mẹ vua) và Thái Hoàng Thái Hậu (bà nội vua). Các công trình này mang vẻ đẹp thanh nhã, có hồ nước và sân vườn rất thoáng đãng.
C. Khu vực thờ tự', 'artifact:dai-noi-hue-ngo-mon', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000031-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222208', 'Ngai Vàng Điện Thái Hòa', '/media/heritage/dai-noi-hue/cover.jpeg', 'Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ…', 'Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)
Nằm ngay sau điện Thái Hòa, được giới hạn bởi bức tường thành bảo vệ nghiêm ngặt.
Tả Vu & Hữu Vu: Hai tòa nhà đối xứng ngay sau điện Thái Hòa, nơi các quan chuẩn bị áo mũ chỉnh tề trước khi vào chầu vua.
Điện Cần Chánh & Điện Càn Thành: Nơi vua làm việc hàng ngày và nơi ngủ nghỉ của nhà vua (đang trong quá trình phục dựng).
Cung Diên Thọ & Cung Trường Sanh: Nằm ở phía Tây của Tử Cấm Thành, là nơi ở và sinh hoạt của Hoàng Thái Hậu (mẹ vua) và Thái Hoàng Thái Hậu (bà nội vua). Các công trình này mang vẻ đẹp thanh nhã, có hồ nước và sân vườn rất thoáng đãng.
C. Khu vực thờ tự
Thế Miếu (Thế Tổ Miếu): Nơi thờ các vị vua triều Nguyễn.', 'artifact:dai-noi-hue-ngai-vang', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000032-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222208', 'Cửu Đỉnh', '/media/heritage/dai-noi-hue/cover.jpeg', 'Hoàn thiện (1833): Quy hoạch và kiến trúc của Đại Nội chỉ thực sự hoàn chỉnh dưới thời vua Minh Mạng vào năm 1833, khi hàng loạt cung điện, lầu các và hệ thống tường thành bao quanh được hoàn thành.
Biến động lịch sử: Trải qua 143 năm là thủ đô của triều Nguyễn (1802 – 1945), Đại Nội đã chứng…', 'Hoàn thiện (1833): Quy hoạch và kiến trúc của Đại Nội chỉ thực sự hoàn chỉnh dưới thời vua Minh Mạng vào năm 1833, khi hàng loạt cung điện, lầu các và hệ thống tường thành bao quanh được hoàn thành.
Biến động lịch sử: Trải qua 143 năm là thủ đô của triều Nguyễn (1802 – 1945), Đại Nội đã chứng kiến nhiều thăng trầm. Do tác động của chiến tranh (đặc biệt là các sự kiện năm 1947 và 1968), nhiều công trình nguy nga bên trong từng bị tàn phá, san phẳng hoàn toàn. Những năm gần đây, công tác trùng tu đang được đẩy mạnh để phục dựng lại nguyên bản các điện thờ lớn.
3. Cấu trúc không gian & Các công trình tiêu biểu
Đại Nội được xây dựng theo sơ đồ gần như hình vuông, chu vi khoảng 2,5km, mặt mặt hướng về phía Nam.
A. Hệ thống cửa ngõ & Khu vực thiết triều (Hoàng Thành)
Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua Bảo Đại thoái vị vào năm 1945, chấm dứt chế độ phong kiến Việt Nam.
Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)', 'artifact:dai-noi-hue-cuu-dinh', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000033-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222208', 'Lầu Ngũ Phụng', '/media/heritage/dai-noi-hue/cover.jpeg', 'Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua…', 'Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua Bảo Đại thoái vị vào năm 1945, chấm dứt chế độ phong kiến Việt Nam.
Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)
Nằm ngay sau điện Thái Hòa, được giới hạn bởi bức tường thành bảo vệ nghiêm ngặt.
Tả Vu & Hữu Vu: Hai tòa nhà đối xứng ngay sau điện Thái Hòa, nơi các quan chuẩn bị áo mũ chỉnh tề trước khi vào chầu vua.
Điện Cần Chánh & Điện Càn Thành: Nơi vua làm việc hàng ngày và nơi ngủ nghỉ của nhà vua (đang trong quá trình phục dựng).
Cung Diên Thọ & Cung Trường Sanh: Nằm ở phía Tây của Tử Cấm Thành, là nơi ở và sinh hoạt của Hoàng Thái Hậu (mẹ vua) và Thái Hoàng Thái Hậu (bà nội vua). Các công trình này mang vẻ đẹp thanh nhã, có hồ nước và sân vườn rất thoáng đãng.
C. Khu vực thờ tự', 'artifact:dai-noi-hue-lau-ngu-phung', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222209', 'Đền Hùng Vương', '1. Tổng quan & Vị trí
Vị trí: Nằm trên núi Nghĩa Lĩnh (núi Hùng), thuộc xã Hy Cương, thành phố Việt Trì, tỉnh Phú Thọ, cách thủ đô Hà Nội khoảng 85km về phía Tây Bắc.
Tầm vóc: Đây là khu di tích lịch sử đặc biệt cấp quốc gia, trung tâm thờ tự tổ tiên của người Việt. Năm 2012, Tín ngưỡng thờ cúng Hùng Vương ở Phú Thọ đã được UNESCO công nhận là Di sản văn hóa phi vật thể đại diện của nhân loại.
Cảnh quan: Núi Hùng cao khoảng 175m, xung quanh là hệ thống đồi gò trùng điệp (truyền thuyết gọi là 99 con voi quay đầu về chầu nghĩa quân). Không gian nơi đây quanh năm mây mờ bao phủ, cây cối cổ thụ xanh tươi, mang đậm màu sắc huyền thoại, linh thiêng.
2. Lịch sử hình thành & Giá trị cội nguồn
Thời kỳ lập nước: Theo truyền thuyết, nơi đây chính là trung tâm kinh đô Phong Châu của nước Văn Lang cổ đại. Các vị vua Hùng đã chọn đỉnh núi Nghĩa Lĩnh cao nhất vùng để làm nơi tiến hành các nghi lễ tín ngưỡng bản địa: tế trời đất, tế thần lúa, cầu mong mưa thuận gió hòa, mùa màng bội thu.
Xây dựng qua các triều đại: Hệ thống kiến trúc lăng tẩm, đền đài được xây dựng từng bước và trùng tu lớn qua các thời kỳ Lý, Trần, Lê, Nguyễn. Công trình hiện nay phần lớn mang phong cách kiến trúc thời hậu Lê và thời Nguyễn.
Ngày Quốc Giỗ: Hằng năm, vào ngày mồng 10 tháng Ba âm lịch, hàng triệu người con đất Việt từ khắp mọi miền tổ quốc và kiều bào nước ngoài lại hướng về Đền Hùng để làm lễ dâng hương, thể hiện truyền thống đạo lý "Uống nước nhớ nguồn".
3. Cấu trúc không gian & Hệ thống đền đài từ chân lên đỉnh núi
Hành trình tham quan Đền Hùng là một trải nghiệm leo núi thiêng qua từng bậc đá cổ kính, tương ứng với các tầng di tích:
Đại Môn (Cổng đền): Được xây dựng vào năm 1917 dưới thời vua Khải Định. Cổng có kiến trúc kiểu vòm cuốn nguy nga, trên đỉnh đắp phù điêu hai chiến sĩ cầm chùy canh giữ, chính giữa khắc bốn chữ Hán lớn: Cao sơn cảnh hành (ý nói đức lớn của các vua Hùng cao như núi, đường lối của vua như đường lớn để mọi người noi theo).
Đền Hạ: Nằm ở tầng núi thấp nhất. Theo truyền thuyết, đây là nơi mẹ Âu Cơ đã hạ sinh ra bọc trăm trứng, sau đó nở thành một trăm người con trai, cội nguồn của hai tiếng "đồng bào". Ngay cạnh đền là Chùa Thiên Quang cổ kính.
Đền Trung (Hùng Vương Tổ miếu): Nơi các vua Hùng cùng các lạc hầu, lạc tướng thường lên ngắm cảnh thiên nhiên và bàn việc nước. Đây cũng là nơi gắn liền với câu chuyện vua Hùng thứ 6 nhường ngôi cho Lang Liêu nhờ việc sáng tạo ra bánh chưng, bánh giầy.
Đền Thượng (Kính Thiên lĩnh điện): Nằm trên đỉnh núi cao nhất. Nơi các vua Hùng lập đàn tế trời đất. Phía bên cạnh đền có Lăng Hùng Vương (tương truyền là mộ của vị vua Hùng thứ 6). Tại cửa đền Thượng có cột đá thề do Thục Phán (An Dương Vương) dựng lên sau khi được nhường ngôi, nguyện một lòng bảo vệ giang sơn mà các vua Hùng đã gầy dựng.
Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm" vào không gian huyền thoại một cách trực quan, Đền Hùng đã tích hợp nhiều ứng dụng công nghệ hiện đại:
Trải nghiệm AR "Hồi sinh không gian huyền thoại":
Khi du khách đứng tại Đền Hạ, việc sử dụng thiết bị quét mã AR sẽ tái hiện lại sống động bằng hoạt họa 3D câu chuyện bọc trăm trứng nở thành trăm người con, hay hình ảnh đoàn người lên núi tế trời đất thời cổ đại.
Tại Đền Trung, AR giúp mô phỏng lại hình ảnh Lang Liêu dâng chiếc bánh chưng, bánh giầy vuông tròn tượng trưng cho trời và đất lên vua cha, giúp bài học lịch sử trở nên thu hút, dễ hiểu đối với các bạn trẻ.
Bản đồ số hóa & Thuyết minh tự động: Hệ thống bản đồ 3D hiển thị chi tiết số lượng bậc đá, tọa độ các đền từ chân núi lên đỉnh núi Nghĩa Lĩnh. Du khách có thể dùng điện thoại tự quét mã tại mỗi điểm để nghe trợ lý AI thuyết minh tường tận về các truyền thuyết, ý nghĩa của từng câu đối, hoành phi cổ.
Bảo tàng Hùng Vương ảo: Các cổ vật quý hiếm thời kỳ văn hóa Đông Sơn (trống đồng, rìu chiến, dao găm, đồ ngọc ngự dụng) tìm thấy xung quanh khu vực kinh đô Phong Châu cũ đều được quét 3D. Du khách có thể tương tác xoay lật, phóng to hoa văn chim lạc trên trống đồng qua màn hình ảo một cách sắc nét.
Đền Hùng không chỉ là một danh thắng, mà là một không gian tâm linh linh thiêng bậc nhất, nơi nhắc nhở mỗi người Việt về nguồn cội chung của mình. Sự kết hợp của công nghệ AR như một chiếc cầu nối, giúp các câu chuyện huyền thoại nghìn năm hiển hiện một cách chân thực ngay giữa đời thực.', 21.383, 105.298, 'Phú Thọ', '/media/heritage/den-hung-vuong/cover.webp')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Đền Hùng Vương
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000034-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222209', 'Trống đồng Đông Sơn', '/media/heritage/den-hung-vuong/cover.webp', 'Bảo tàng Hùng Vương ảo: Các cổ vật quý hiếm thời kỳ văn hóa Đông Sơn (trống đồng, rìu chiến, dao găm, đồ ngọc ngự dụng) tìm thấy xung quanh khu vực kinh đô Phong Châu cũ đều được quét 3D. Du khách có thể tương tác xoay lật, phóng to hoa văn chim lạc trên trống đồng qua màn hình ảo một cách sắc…', 'Bảo tàng Hùng Vương ảo: Các cổ vật quý hiếm thời kỳ văn hóa Đông Sơn (trống đồng, rìu chiến, dao găm, đồ ngọc ngự dụng) tìm thấy xung quanh khu vực kinh đô Phong Châu cũ đều được quét 3D. Du khách có thể tương tác xoay lật, phóng to hoa văn chim lạc trên trống đồng qua màn hình ảo một cách sắc nét.
Đền Hùng không chỉ là một danh thắng, mà là một không gian tâm linh linh thiêng bậc nhất, nơi nhắc nhở mỗi người Việt về nguồn cội chung của mình. Sự kết hợp của công nghệ AR như một chiếc cầu nối, giúp các câu chuyện huyền thoại nghìn năm hiển hiện một cách chân thực ngay giữa đời thực.', 'artifact:den-hung-vuong-trong-dong', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000035-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222209', 'Bánh chưng, bánh giầy', '/media/heritage/den-hung-vuong/cover.webp', 'Đền Trung (Hùng Vương Tổ miếu): Nơi các vua Hùng cùng các lạc hầu, lạc tướng thường lên ngắm cảnh thiên nhiên và bàn việc nước. Đây cũng là nơi gắn liền với câu chuyện vua Hùng thứ 6 nhường ngôi cho Lang Liêu nhờ việc sáng tạo ra bánh chưng, bánh giầy.
Đền Thượng (Kính Thiên lĩnh điện): Nằm trên…', 'Đền Trung (Hùng Vương Tổ miếu): Nơi các vua Hùng cùng các lạc hầu, lạc tướng thường lên ngắm cảnh thiên nhiên và bàn việc nước. Đây cũng là nơi gắn liền với câu chuyện vua Hùng thứ 6 nhường ngôi cho Lang Liêu nhờ việc sáng tạo ra bánh chưng, bánh giầy.
Đền Thượng (Kính Thiên lĩnh điện): Nằm trên đỉnh núi cao nhất. Nơi các vua Hùng lập đàn tế trời đất. Phía bên cạnh đền có Lăng Hùng Vương (tương truyền là mộ của vị vua Hùng thứ 6). Tại cửa đền Thượng có cột đá thề do Thục Phán (An Dương Vương) dựng lên sau khi được nhường ngôi, nguyện một lòng bảo vệ giang sơn mà các vua Hùng đã gầy dựng.
Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm" vào không gian huyền thoại một cách trực quan, Đền Hùng đã tích hợp nhiều ứng dụng công nghệ hiện đại:', 'artifact:den-hung-vuong-banh-chung', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000036-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222209', 'Cột đá thề An Dương Vương', '/media/heritage/den-hung-vuong/cover.webp', 'Đền Thượng (Kính Thiên lĩnh điện): Nằm trên đỉnh núi cao nhất. Nơi các vua Hùng lập đàn tế trời đất. Phía bên cạnh đền có Lăng Hùng Vương (tương truyền là mộ của vị vua Hùng thứ 6). Tại cửa đền Thượng có cột đá thề do Thục Phán (An Dương Vương) dựng lên sau khi được nhường ngôi, nguyện một lòng…', 'Đền Thượng (Kính Thiên lĩnh điện): Nằm trên đỉnh núi cao nhất. Nơi các vua Hùng lập đàn tế trời đất. Phía bên cạnh đền có Lăng Hùng Vương (tương truyền là mộ của vị vua Hùng thứ 6). Tại cửa đền Thượng có cột đá thề do Thục Phán (An Dương Vương) dựng lên sau khi được nhường ngôi, nguyện một lòng bảo vệ giang sơn mà các vua Hùng đã gầy dựng.
Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm" vào không gian huyền thoại một cách trực quan, Đền Hùng đã tích hợp nhiều ứng dụng công nghệ hiện đại:', 'artifact:den-hung-vuong-cot-da-the', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000037-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222209', 'Giếng Ngọc', '/media/heritage/den-hung-vuong/cover.webp', 'Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm"…', 'Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm" vào không gian huyền thoại một cách trực quan, Đền Hùng đã tích hợp nhiều ứng dụng công nghệ hiện đại:', 'artifact:den-hung-vuong-gieng-ngoc', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('11111111-1111-1111-1111-111111111111', 'Địa đạo Củ Chi', '1. Tổng quan & Vị trí
Vị trí: Thuộc huyện Củ Chi, TP. Hồ Chí Minh, cách trung tâm thành phố khoảng 70km về phía Tây Bắc. Khu di tích hiện nay được bảo tồn ở hai địa điểm chính: Địa đạo Bến Dược (Căn cứ Khu ủy và Bộ Tư lệnh Quân khu Sài Gòn - Chợ Lớn - Gia Định) và Địa đạo Bến Đình (Căn cứ Huyện ủy Củ Chi).
Tầm vóc: Đây là một hệ thống phòng thủ, căn cứ quân sự trong lòng đất vô cùng đồ sộ. Địa đạo Củ Chi đã được xếp hạng Di sản cấp quốc gia đặc biệt và đang trong quá trình hoàn thiện hồ sơ đệ trình UNESCO công nhận là Di sản Thế giới.
Danh hiệu: Nơi đây được mệnh danh là "Mảnh đất thép thành đồng" nhờ khả năng đứng vững trước hàng triệu tấn bom đạn của kẻ thù.
2. Lịch sử hình thành: Kỳ tích từ những chiếc cuốc thô sơ
Khởi nguồn (1946 - 1948): Hệ thống địa đạo bắt đầu được đào từ thời kỳ kháng chiến chống Pháp. Ban đầu, các xã chỉ đào những hầm bí mật đơn lẻ để ẩn nấp cán bộ và cất giấu tài liệu.
Mở rộng thành "Mạng nhện" (1961 - 1967): Bước sang cuộc kháng chiến chống Mỹ, trước sự càn quét khốc liệt của địch, hệ thống này được phát triển, nối liền các xã với nhau tạo thành một mạng lưới liên hoàn trong lòng đất. Quân và dân Củ Chi đã dùng những công cụ thô sơ như cuốc, xẻng và sọt tre để kiên trì đào xuyên lòng đất ngày đêm, tạo nên một "thành phố ngầm" kỳ vĩ.
3. Kiến trúc lòng đất độc đáo "Bất khả xâm phạm"
Hệ thống địa đạo Củ Chi có tổng chiều dài toàn tuyến lên đến hơn 250 km, chạy ngoằn ngoèo trong lòng đất đất sét pha đá sỏi (loại đất chịu lực tốt, càng ra gió càng cứng, không bị sập).
Cấu trúc 3 tầng biệt lập:
Tầng 1 (Cách mặt đất 3m): Chịu được đạn pháo và xe tăng, xe bọc thép cán qua.
Tầng 2 (Cách mặt đất 6m): Chịu được các loại bom cỡ nhỏ.
Tầng 3 (Cách mặt đất từ 8 - 12m): Tầng sâu nhất, cực kỳ an toàn, bất chấp cả bom cỡ lớn.
Hệ thống phòng ốc liên hoàn: Bên trong địa đạo không chỉ là đường đi lối lại chật hẹp mà còn có đầy đủ các không gian sinh hoạt: phòng họp của Bộ tư lệnh, hầm trú ẩn cho người già và trẻ em, công xưởng chế tạo vũ khí, nhà thương (bệnh viện dã chiến), nhà bếp, giếng nước và kho chứa lương thực.
Bếp Hoàng Cầm: Một phát minh vĩ đại của thời chiến được áp dụng triệt để tại đây. Bếp nấu ăn dưới lòng đất nhưng hệ thống dẫn khói được tản ra đi là là trên mặt đất qua các rãnh nhỏ cách xa bếp, giúp khói tan rã hoàn toàn vào sương sớm, khiến máy bay địch không thể phát hiện ra vị trí trú ẩn.
Lỗ thông hơi và Hệ thống hầm chông: Các lỗ thông hơi được ngụy trang khéo léo thành những tổ mối, gốc cây để lấy dưỡng khí. Bao quanh các lối vào địa đạo là mạng lưới mìn tự tạo và bẫy chông tre, chông sắt dày đặc nhằm ngăn chặn sự xâm nhập của lính thám báo.
4. Ứng dụng AR và Trải nghiệm số hóa tại Địa đạo Củ Chi
Vì địa đạo nằm sâu trong lòng đất, không gian tối và chật hẹp khiến việc tham quan thực tế đôi khi gặp hạn chế đối với một số du khách (người sợ không gian kín hoặc hạn chế về sức khỏe). Công nghệ AR và số hóa đã giải quyết xuất sắc bài toán này:
Mô phỏng "Cắt lớp lòng đất" bằng AR:
Khi đứng trên mặt đất, du khách chỉ cần dùng điện thoại hoặc kính thông minh quét không gian, ứng dụng AR sẽ hiển thị một sơ đồ 3D "xuyên thấu" mặt đất. Bạn sẽ nhìn thấy rõ ràng cấu trúc 3 tầng của địa đạo đang chạy ngay dưới chân mình, thấy cách các chiến sĩ di chuyển, hội họp và sinh hoạt bên dưới một cách trực quan.
AR cũng mô phỏng nguyên lý hoạt động giấu khói thần kỳ của Bếp Hoàng Cầm hay cách hoạt động của các loại bẫy chông ngụy trang một cách sống động mà không cần phải chạm vào hiện vật thật.
Tour tham quan ảo 360° dưới lòng đất: Đối với những du khách không thể xuống lòng đất, hệ thống quét hình ảnh 360 độ độ phân giải cao kết hợp với âm thanh giả lập (tiếng bom đạn, tiếng cuốc đất, tiếng thì thầm thời chiến) giúp họ có trải nghiệm như đang thực sự len lỏi qua từng vách đất của hầm chỉ huy Bến Dược.
Số hóa vũ khí tự chế: Du khách có thể tương tác với các mô hình 3D của các loại vũ khí tự chế độc đáo của du kích Củ Chi (như mìn gạt làm từ vỏ bom lép của địch, chông bập bênh...) trên màn hình cảm ứng, tìm hiểu cơ chế hoạt động lịch sử của chúng.
Địa đạo Củ Chi là một trường ca về lòng yêu nước và sự sáng tạo không giới hạn của con người. Sự hỗ trợ của công nghệ AR giống như một chiếc kính xuyên không, giúp chúng ta nhìn thấu lòng đất để thấu hiểu và tri ân một thời kỳ gian khổ nhưng vô cùng vẻ vang của thế hệ đi trước.', 11.143, 106.461, 'TP.HCM', '/media/heritage/dia-dao-cu-chi/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

COMMIT;