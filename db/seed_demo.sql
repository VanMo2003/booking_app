-- =====================================================================================
-- booking_app - dữ liệu mẫu để demo và để chạy bộ Postman
-- =====================================================================================
--
-- CHẠY KHI NÀO
--   Chạy SAU khi đã khởi động backend ít nhất một lần. Dự án dùng
--   spring.jpa.hibernate.ddl-auto=update, nghĩa là Hibernate tự tạo bảng lúc khởi động;
--   file này chỉ chèn dữ liệu, không tạo bảng.
--
-- CHẠY THẾ NÀO
--   mysql -u root -p booking_app_db < db/seed_demo.sql
--
-- CHẠY LẠI ĐƯỢC
--   Phần đầu xoá sạch dữ liệu demo theo đúng mã đã gán (9xx) rồi chèn lại, nên chạy
--   bao nhiêu lần cũng ra cùng một kết quả. Dữ liệu bạn tự tạo trên app KHÔNG bị xoá.
--
-- TÀI KHOẢN ĐĂNG NHẬP SAU KHI CHẠY (mật khẩu đã băm BCrypt sẵn trong file)
--   admin123    / admin123      ADMIN          (tài khoản này do app tự tạo lúc khởi động)
--   owner_demo  / owner123      HOTEL_OWNER    chủ chuỗi "Chuỗi Mơ Mộng", đã được duyệt
--   manager_demo/ manager123    HOTEL_MANAGER  quản lý cả hai cơ sở
--   staff_demo  / staff123      STAFF          nhân viên cơ sở Hoàn Kiếm
--   cust_demo   / customer123   CUSTOMER       khách Trần Thu Hà
--   cust_demo2  / customer123   CUSTOMER       khách Lê Minh Quân
--
-- GỢI Ý: muốn có bộ dữ liệu đầy đủ hơn (đơn phòng, đơn tour, chat, báo cáo…) thì chạy
-- bộ Postman trong postman/booking_app.postman_collection.json - nó tạo mọi thứ qua API.
-- =====================================================================================

SET NAMES utf8mb4;
SET @OLD_FK = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 1. Xoá dữ liệu demo cũ (chỉ các mã 9xx của file này)
-- ---------------------------------------------------------------------------
DELETE FROM feedbacks        WHERE id BETWEEN 900 AND 999;
DELETE FROM booking_rooms    WHERE id BETWEEN 900 AND 999;
DELETE FROM bookings         WHERE id BETWEEN 900 AND 999;
DELETE FROM tour_itinerary   WHERE tour_id BETWEEN 900 AND 999;
DELETE FROM tour_themes      WHERE tour_id BETWEEN 900 AND 999;
DELETE FROM tour_rooms       WHERE id BETWEEN 900 AND 999;
DELETE FROM tours            WHERE id BETWEEN 900 AND 999;
DELETE FROM vouchers         WHERE id BETWEEN 900 AND 999;
DELETE FROM dishes           WHERE id BETWEEN 900 AND 999;
DELETE FROM services         WHERE id BETWEEN 900 AND 999;
DELETE FROM room_amenity     WHERE id BETWEEN 900 AND 999;
DELETE FROM amenity          WHERE id BETWEEN 900 AND 999;
DELETE FROM rooms            WHERE id BETWEEN 900 AND 999;
DELETE FROM employees        WHERE id BETWEEN 900 AND 999;
DELETE FROM customer_interests WHERE customer_id BETWEEN 900 AND 999;
DELETE FROM customers        WHERE id BETWEEN 900 AND 999;
DELETE FROM hotels           WHERE id BETWEEN 900 AND 999;
DELETE FROM hotel_chains     WHERE id BETWEEN 900 AND 999;
DELETE FROM positions        WHERE id BETWEEN 900 AND 999;
DELETE FROM room_types       WHERE id BETWEEN 900 AND 999;
DELETE FROM accounts         WHERE id LIKE 'demo-%';

-- ---------------------------------------------------------------------------
-- 2. Tài khoản đăng nhập
--    Mật khẩu đã băm BCrypt (cost 10) đúng kiểu backend dùng, nên đăng nhập được ngay.
-- ---------------------------------------------------------------------------
INSERT INTO accounts (id, username, password, role, status, on_create, on_update) VALUES
  ('demo-owner',    'owner_demo',   '$2a$10$n6yNQJDiVSYnOGswMRGmlOauwGQAxNp6Iio2QEX/z3yh7tKMzVLom', 'HOTEL_OWNER',   1, NOW(6), NOW(6)),
  ('demo-manager',  'manager_demo', '$2a$10$rfbkSNcc7yIUUMtm97Mvw.zUu5jCOrQwjgFMyIpHAIx.XIUFFxARm', 'HOTEL_MANAGER', 1, NOW(6), NOW(6)),
  ('demo-staff',    'staff_demo',   '$2a$10$.n6AbvHWJiEfVvk6ctHS8.vTT9SSuUrKU.UPAbZVJpvQnDKfvcXKa', 'STAFF',         1, NOW(6), NOW(6)),
  ('demo-cust-1',   'cust_demo',    '$2a$10$/BNakl4eDLZFigiqbauk6Ov/x376wmZ6.qNzBPHHplaKy/HBC.RYm', 'CUSTOMER',      1, NOW(6), NOW(6)),
  ('demo-cust-2',   'cust_demo2',   '$2a$10$/BNakl4eDLZFigiqbauk6Ov/x376wmZ6.qNzBPHHplaKy/HBC.RYm', 'CUSTOMER',      1, NOW(6), NOW(6));

-- ---------------------------------------------------------------------------
-- 3. Chuỗi khách sạn (đã được admin duyệt, nếu không chủ chuỗi bị khoá mọi thao tác)
-- ---------------------------------------------------------------------------
INSERT INTO hotel_chains
  (id, name, description, path_image, owner_name, email, phone, address,
   approval_status, rejection_reason, submitted_at, reviewed_at, account_id, on_create, on_update)
VALUES
  (900, 'Chuỗi Mơ Mộng',
   'Chuỗi lưu trú nhỏ ở Hà Nội, tập trung vào khách du lịch tự túc và khách công tác ngắn ngày.',
   NULL, 'Nguyễn Văn Mơ', 'owner.demo@example.com', '0901000001', '12 Hàng Bạc, Hoàn Kiếm, Hà Nội',
   'APPROVED', NULL, NOW(6), NOW(6), 'demo-owner', NOW(6), NOW(6));

-- ---------------------------------------------------------------------------
-- 4. Hai cơ sở (branch). Cùng một quản lý để đăng nhập một lần xem được cả hai.
-- ---------------------------------------------------------------------------
INSERT INTO hotels
  (id, path_image, name, address, phone, description, category, rating,
   account_id, hotel_chain_id, active, status, ai_auto_reply, on_create, on_update)
VALUES
  (900, NULL, 'Mơ Mộng Hoàn Kiếm', '12 Hàng Bạc, Hoàn Kiếm, Hà Nội', '0901000011',
   'Khách sạn nhỏ trong khu phố cổ, đi bộ ra Hồ Hoàn Kiếm khoảng 5 phút. Phòng cách âm, có thang máy, lễ tân trực 24/7 và hỗ trợ giữ hành lý trước giờ nhận phòng.',
   -- status la enum luu dang so (0 = AVAILABLE), khac voi cac enum khac trong du an
   'Standard', 4.3, 'demo-manager', 900, 1, 0, 0, NOW(6), NOW(6)),
  (901, NULL, 'Mơ Mộng Ba Đình', '5 Đội Cấn, Ba Đình, Hà Nội', '0901000012',
   'Cơ sở yên tĩnh gần khu Lăng Bác và các đại sứ quán, phù hợp khách đi công tác. Có phòng làm việc chung và bữa sáng tự chọn.',
   'Grand', 4.1, 'demo-manager', 900, 1, 0, 0, NOW(6), NOW(6));

-- ---------------------------------------------------------------------------
-- 5. Danh mục dùng chung: hạng phòng và chức danh
-- ---------------------------------------------------------------------------
INSERT INTO room_types (id, name, description) VALUES
  (900, 'Standard Demo', 'Phòng tiêu chuẩn, 1 giường đôi.'),
  (901, 'Deluxe Demo',   'Phòng rộng hơn, có ban công.'),
  (902, 'Suite Demo',    'Phòng gia đình, 2 phòng ngủ.');

INSERT INTO positions (id, name, description) VALUES
  (900, 'Lễ tân Demo', 'Nhận đặt phòng, làm thủ tục nhận và trả phòng.');

-- ---------------------------------------------------------------------------
-- 6. Phòng
-- ---------------------------------------------------------------------------
INSERT INTO rooms
  (id, path_image, room_number, price, description, capacity, hotel_id, room_type_id, status, on_create, on_update)
VALUES
  (900, NULL, '101', 650000,  'Phòng tiêu chuẩn tầng 1, cửa sổ nhìn ra ngõ nhỏ.', 2, 900, 900, 'AVAILABLE', NOW(6), NOW(6)),
  (901, NULL, '102', 650000,  'Phòng tiêu chuẩn tầng 1, yên tĩnh.',              2, 900, 900, 'AVAILABLE', NOW(6), NOW(6)),
  (902, NULL, '201', 950000,  'Phòng deluxe tầng 2, có ban công.',               3, 900, 901, 'AVAILABLE', NOW(6), NOW(6)),
  (903, NULL, '301', 1500000, 'Phòng gia đình tầng 3, hai phòng ngủ.',           4, 900, 902, 'AVAILABLE', NOW(6), NOW(6)),
  (904, NULL, '401', 800000,  'Phòng đôi tầng 4, bàn làm việc rộng.',            2, 901, 900, 'AVAILABLE', NOW(6), NOW(6)),
  (905, NULL, '402', 1200000, 'Phòng deluxe tầng 4, nhìn ra phố Đội Cấn.',       3, 901, 901, 'MAINTENANCE', NOW(6), NOW(6));

-- ---------------------------------------------------------------------------
-- 7. Tiện ích (common = tiện ích chung của cơ sở, ngược lại là tiện ích trong phòng)
-- ---------------------------------------------------------------------------
INSERT INTO amenity (id, name, description, common, hotel_id, active, on_create, on_update) VALUES
  (900, 'Wifi miễn phí',   'Phủ toàn bộ toà nhà.',            1, 900, 1, NOW(6), NOW(6)),
  (901, 'Lễ tân 24/7',     'Có người trực cả đêm.',           1, 900, 1, NOW(6), NOW(6)),
  (902, 'Thang máy',       NULL,                              1, 900, 1, NOW(6), NOW(6)),
  (903, 'Điều hoà',        'Điều hoà hai chiều.',             0, 900, 1, NOW(6), NOW(6)),
  (904, 'Ban công',        NULL,                              0, 900, 1, NOW(6), NOW(6)),
  (905, 'Bữa sáng tự chọn',NULL,                              1, 901, 1, NOW(6), NOW(6)),
  (906, 'Phòng làm việc chung', 'Có ổ cắm và màn hình rời.',  1, 901, 1, NOW(6), NOW(6));

INSERT INTO room_amenity (id, room_id, amenity_id, quantity) VALUES
  (900, 900, 903, 1),
  (901, 901, 903, 1),
  (902, 902, 903, 1),
  (903, 902, 904, 1),
  (904, 903, 903, 2),
  (905, 904, 903, 1);

-- ---------------------------------------------------------------------------
-- 8. Dịch vụ tính thêm
-- ---------------------------------------------------------------------------
INSERT INTO services (id, name, unit_price, description, hotel_id, on_create, on_update) VALUES
  (900, 'Đưa đón sân bay', 350000, 'Xe 4 chỗ, đón tại sảnh đến Nội Bài.', 900, NOW(6), NOW(6)),
  (901, 'Giặt là',         60000,  'Tính theo kilôgam, trả trong ngày.',  900, NOW(6), NOW(6)),
  (902, 'Thuê xe máy',     150000, 'Một ngày, đã gồm mũ bảo hiểm.',       901, NOW(6), NOW(6));

-- ---------------------------------------------------------------------------
-- 9. Thực đơn
-- ---------------------------------------------------------------------------
INSERT INTO dishes (id, name, description, price, category, path_image, available, hotel_id, on_create, on_update) VALUES
  (900, 'Phở bò',        'Nước dùng ninh xương bò, bánh phở tươi.',        65000,  'MAIN_COURSE', NULL, 1, 900, NOW(6), NOW(6)),
  (901, 'Bún chả',       'Chả nướng than hoa, ăn kèm bún và rau sống.',    70000,  'MAIN_COURSE', NULL, 1, 900, NOW(6), NOW(6)),
  (902, 'Nem rán',       'Sáu chiếc, nhân thịt và mộc nhĩ.',               55000,  'APPETIZER',   NULL, 1, 900, NOW(6), NOW(6)),
  (903, 'Chè khúc bạch', NULL,                                             35000,  'DESSERT',     NULL, 1, 900, NOW(6), NOW(6)),
  (904, 'Cà phê trứng',  'Đặc sản Hà Nội, pha theo kiểu truyền thống.',    45000,  'DRINK',       NULL, 1, 900, NOW(6), NOW(6)),
  (905, 'Xôi xéo',       'Phục vụ buổi sáng.',                             40000,  'MAIN_COURSE', NULL, 1, 901, NOW(6), NOW(6));

-- ---------------------------------------------------------------------------
-- 10. Tour (900 là tour kèm phòng, 901 là tour đi trong ngày)
-- ---------------------------------------------------------------------------
INSERT INTO tours
  (id, name, description, price, duration, departure, includes, destination,
   child_price, cancellation_policy, featured, discount_percent, promotion_ends_at,
   max_guests, stay_nights, path_image, available, hotel_id, on_create, on_update)
VALUES
  (900, 'Food tour phố cổ',
   'Đi bộ qua sáu hàng quán lâu năm trong khu phố cổ cùng hướng dẫn viên địa phương, nghe chuyện từng món và tự chọn khẩu phần.',
   450000, '4 giờ', '18:00 hằng ngày', 'Hướng dẫn viên, đồ ăn tại 6 quán, nước uống', 'Hà Nội',
   250000, 'Huỷ miễn phí trước 24 giờ.', 1, 0, NULL, 10, 1, NULL, 1, 900, NOW(6), NOW(6)),
  (901, 'Hạ Long 2 ngày 1 đêm',
   'Xe đón tại khách sạn, du thuyền ngủ đêm trên vịnh, chèo kayak và thăm hang Sửng Sốt.',
   2400000, '2 ngày 1 đêm', '07:30 thứ sáu hằng tuần', 'Xe đưa đón, du thuyền, 3 bữa ăn, vé thắng cảnh', 'Hạ Long',
   1500000, 'Huỷ trước 72 giờ hoàn 100%, trong 72 giờ hoàn 50%.', 1, 15, DATE_ADD(CURDATE(), INTERVAL 30 DAY),
   16, 1, NULL, 1, 900, NOW(6), NOW(6));

INSERT INTO tour_themes (tour_id, theme) VALUES
  (900, 'FOOD'), (900, 'CITY'), (900, 'CULTURE'),
  (901, 'BEACH'), (901, 'NATURE'), (901, 'ADVENTURE');

INSERT INTO tour_itinerary (tour_id, step_order, step_time, step_title, step_description) VALUES
  (900, 0, '18:00', 'Gặp hướng dẫn viên', 'Tại sảnh khách sạn.'),
  (900, 1, '18:30', 'Bún chả Hàng Quạt',  'Món chính đầu tiên.'),
  (900, 2, '19:30', 'Chè và cà phê trứng','Tráng miệng ở phố Đinh Tiên Hoàng.'),
  (901, 0, '07:30', 'Xe đón tại khách sạn', 'Đi theo cao tốc Hà Nội - Hạ Long.'),
  (901, 1, '12:00', 'Lên du thuyền',        'Nhận phòng và ăn trưa trên tàu.'),
  (901, 2, '15:00', 'Chèo kayak',           'Khoảng một tiếng ở khu vực hang Luồn.'),
  (901, 3, '08:00', 'Thăm hang Sửng Sốt',   'Ngày thứ hai, về tới Hà Nội khoảng 16:00.');

-- Tour 900 bán kèm phòng: khách chọn một trong các phòng dưới đây
INSERT INTO tour_rooms (id, tour_id, room_id) VALUES
  (900, 900, 900),
  (901, 900, 902);

-- ---------------------------------------------------------------------------
-- 11. Mã giảm giá (một mã toàn chuỗi đang chạy, một mã riêng cơ sở Hoàn Kiếm)
-- ---------------------------------------------------------------------------
INSERT INTO vouchers
  (id, code, title, description, discount_type, value, max_discount, min_order_amount,
   starts_at, ends_at, usage_limit, used_count, active, hotel_chain_id, hotel_id, on_create)
VALUES
  (900, 'MOMONG10', 'Giảm 10% toàn chuỗi', 'Áp dụng cho đơn tour từ 500.000 đ.',
   'PERCENT', 10, 300000, 500000, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 30 DAY),
   100, 0, 1, 900, NULL, NOW(6)),
  (901, 'HOANKIEM50K', 'Giảm 50.000 đ tại Hoàn Kiếm', NULL,
   'AMOUNT', 50000, NULL, 400000, DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY),
   50, 0, 1, 900, 900, NOW(6));

-- ---------------------------------------------------------------------------
-- 12. Nhân viên và khách
-- ---------------------------------------------------------------------------
INSERT INTO employees
  (id, path_image, account_id, hotel_id, full_name, phone_number, gender, date_of_birth, hometown,
   salary, position_id, on_create, on_update)
VALUES
  (900, NULL, 'demo-staff', 900, 'Phạm Thu Trang', '0901000021', 'Nữ', '2001-05-12', 'Hà Nội',
   8000000, 900, NOW(6), NOW(6));

INSERT INTO customers
  (id, path_image, account_id, full_name, phone_number, gender, hometown, on_create, on_update)
VALUES
  (900, NULL, 'demo-cust-1', 'Trần Thu Hà',   '0902000001', 'Nữ',  'Hải Phòng', NOW(6), NOW(6)),
  (901, NULL, 'demo-cust-2', 'Lê Minh Quân',  '0902000002', 'Nam', 'Đà Nẵng',   NOW(6), NOW(6)),
  -- Khách vãng lai: đặt tại quầy, chưa có tài khoản đăng nhập.
  (902, NULL, NULL,          'Ngô Bảo Khánh', '0902000003', 'Nam', 'Nam Định',  NOW(6), NOW(6));

INSERT INTO customer_interests (customer_id, theme) VALUES
  (900, 'FOOD'), (900, 'CITY'),
  (901, 'BEACH');

-- ---------------------------------------------------------------------------
-- 13. Đơn phòng. Có đơn đã hoàn tất (để báo cáo doanh thu có số), đơn đang chờ
--     xác nhận (để "Việc hôm nay" có việc) và đơn đã huỷ (để báo cáo huỷ có số).
-- ---------------------------------------------------------------------------
INSERT INTO bookings
  (id, checkin_date, checkout_date, booking_status, payment_method, payment_status,
   payment_expire_at, paid_at, hotel_id, customer_id, handled_by_account_id,
   total_amount, note, canceled_at, canceled_by, on_create, on_update)
VALUES
  -- Đã hoàn tất và đã thu tiền: nguồn doanh thu cho báo cáo
  (900, DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_SUB(CURDATE(), INTERVAL 10 DAY),
   'COMPLETED', 'CASH', 'PAID', NULL, DATE_SUB(NOW(6), INTERVAL 10 DAY),
   900, 900, 'demo-staff', 1300000, 'Khách đi hai người.', NULL, NULL,
   DATE_SUB(NOW(6), INTERVAL 15 DAY), DATE_SUB(NOW(6), INTERVAL 10 DAY)),
  (901, DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_SUB(CURDATE(), INTERVAL 4 DAY),
   'COMPLETED', 'VN_PAY', 'PAID', NULL, DATE_SUB(NOW(6), INTERVAL 6 DAY),
   900, 901, NULL, 1900000, NULL, NULL, NULL,
   DATE_SUB(NOW(6), INTERVAL 9 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
  -- Đã hoàn tất nhưng chưa thu tiền: hiện trong "đơn đã xong chưa thu tiền"
  (902, DATE_SUB(CURDATE(), INTERVAL 9 DAY), DATE_SUB(CURDATE(), INTERVAL 8 DAY),
   'COMPLETED', 'CASH', 'UNPAID', NULL, NULL,
   900, 902, 'demo-staff', 650000, 'Khách vãng lai đặt tại quầy.', NULL, NULL,
   DATE_SUB(NOW(6), INTERVAL 9 DAY), DATE_SUB(NOW(6), INTERVAL 8 DAY)),
  -- Đang chờ cơ sở xác nhận, đã chờ hơn một ngày
  (903, DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY),
   'PENDING', 'CASH', 'UNPAID', NULL, NULL,
   900, 900, NULL, 1900000, NULL, NULL, NULL,
   DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
  -- Đã xác nhận, khách nhận phòng hôm nay
  (904, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
   'CONFIRMED', 'CASH', 'UNPAID', NULL, NULL,
   901, 901, 'demo-manager', 1600000, NULL, NULL, NULL,
   DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
  -- Khách tự huỷ sát ngày nhận phòng
  (905, DATE_SUB(CURDATE(), INTERVAL 2 DAY), CURDATE(),
   'CANCELED', 'CASH', 'UNPAID', NULL, NULL,
   900, 901, NULL, 950000, NULL, DATE_SUB(NOW(6), INTERVAL 3 DAY), 'CUSTOMER',
   DATE_SUB(NOW(6), INTERVAL 8 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY));

INSERT INTO booking_rooms (id, booking_id, room_id) VALUES
  (900, 900, 900),
  (901, 901, 902),
  (902, 901, 901),
  (903, 902, 901),
  (904, 903, 902),
  (905, 904, 904),
  (906, 905, 902);

-- ---------------------------------------------------------------------------
-- 14. Đánh giá. Đủ số lượng để phần tóm tắt đánh giá của AI có dữ liệu,
--     có một đánh giá thấp để thấy dòng cảnh báo trong "Việc hôm nay".
-- ---------------------------------------------------------------------------
INSERT INTO feedbacks (id, booking_id, customer_id, rating, comments, review_date, reply, reply_at, replied_by_account_id) VALUES
  (900, 900, 900, 5, 'Phòng sạch, lễ tân nhiệt tình, vị trí đi bộ ra hồ rất gần.',
   DATE_SUB(CURDATE(), INTERVAL 9 DAY),
   'Cảm ơn chị đã dành thời gian viết đánh giá, mong được đón chị lần sau.',
   DATE_SUB(NOW(6), INTERVAL 8 DAY), 'demo-manager'),
  (901, 901, 901, 4, 'Phòng ổn so với giá. Buổi tối hơi ồn vì gần quán ăn.',
   DATE_SUB(CURDATE(), INTERVAL 3 DAY), NULL, NULL, NULL),
  (902, 902, 902, 2, 'Nhận phòng chờ khá lâu, điều hoà kêu to về đêm.',
   DATE_SUB(CURDATE(), INTERVAL 2 DAY), NULL, NULL, NULL),
  (903, 905, 901, 5, 'Nhân viên hỗ trợ đổi lịch nhanh, rất đáng khen.',
   DATE_SUB(CURDATE(), INTERVAL 1 DAY), NULL, NULL, NULL);

SET FOREIGN_KEY_CHECKS = @OLD_FK;

-- ---------------------------------------------------------------------------
-- Kiểm tra nhanh sau khi chạy
-- ---------------------------------------------------------------------------
SELECT 'accounts' AS bang, COUNT(*) AS so_dong FROM accounts WHERE id LIKE 'demo-%'
UNION ALL SELECT 'hotels',   COUNT(*) FROM hotels   WHERE id BETWEEN 900 AND 999
UNION ALL SELECT 'rooms',    COUNT(*) FROM rooms    WHERE id BETWEEN 900 AND 999
UNION ALL SELECT 'tours',    COUNT(*) FROM tours    WHERE id BETWEEN 900 AND 999
UNION ALL SELECT 'dishes',   COUNT(*) FROM dishes   WHERE id BETWEEN 900 AND 999
UNION ALL SELECT 'bookings', COUNT(*) FROM bookings WHERE id BETWEEN 900 AND 999
UNION ALL SELECT 'feedbacks',COUNT(*) FROM feedbacks WHERE id BETWEEN 900 AND 999;
