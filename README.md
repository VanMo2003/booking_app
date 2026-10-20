# Booking App - Backend

Backend REST API cho hệ thống đặt phòng khách sạn, xây dựng bằng Spring Boot 3.5 / Java 21. Đồ án
tốt nghiệp 2026.

## Tech stack

- **Java 21**, **Spring Boot 3.5.6** (`spring-boot-starter-web`, `-data-jpa`, `-validation`,
  `-oauth2-resource-server`, `-mail`)
- **MySQL 8.4** (JPA/Hibernate, `ddl-auto=update`)
- **JWT** (Nimbus JOSE JWT, HMAC-SHA512) cho authentication/authorization
- **MapStruct** + **Lombok** cho mapping entity &lt;-&gt; DTO
- **VNPay** sandbox cho thanh toán online
- **Apache POI** cho xuất báo cáo Excel
- **Spring Mail** gửi email kết quả xét duyệt, **Firebase Cloud Messaging** (HTTP v1) gửi thông báo đẩy
- **Spring WebSocket** cho nhắn tin realtime giữa khách hàng và khách sạn
- **Google Gemini API** (mặc định, gói miễn phí; đổi được sang Claude hoặc máy chủ chuẩn OpenAI) cho
  trợ lý lễ tân AI và tìm phòng bằng câu nói tự nhiên
- **Docker / Docker Compose** để build & deploy

## Kiến trúc

Layered architecture theo domain, chi tiết xem [CLAUDE.md](CLAUDE.md):

```
controller  -> nhận request, bọc response trong ApiResponse<T>
service     -> interface + impl theo từng entity
repository  -> Spring Data JPA
entity      -> JPA entity, enum trạng thái trong entity/enums
dto         -> request/response riêng cho từng thao tác
mapper      -> MapStruct
config      -> security, CORS, VNPay, seed admin mặc định
security    -> AccessGuard - kiểm tra quyền theo phân cấp khách sạn/cơ sở
event       -> sự kiện sau khi commit: gửi push, gửi email xét duyệt, phát tin nhắn chat
websocket   -> kênh chat realtime /ws/chat (xác thực token, danh sách kết nối)
ai          -> gọi model AI: trợ lý lễ tân trong chat, tìm phòng bằng câu nói
exception   -> AppException + ErrorCode, xử lý tập trung ở GlobalExceptionHandler
scheduler   -> job hết hạn thanh toán pending
```

### Phân cấp quản lý

Hệ thống quản lý **nhiều khách sạn (chuỗi)**, mỗi khách sạn có **nhiều cơ sở**:

- **Khách sạn** (`HotelChain`) — 1 tài khoản `HOTEL_OWNER` sở hữu đúng 1 khách sạn (chuỗi). Chủ khách
  sạn **tự đăng ký** và phải được quản trị viên **duyệt** mới được mở cơ sở, tạo quản lý, xem báo cáo.
- **Cơ sở** (entity `Hotel`, giữ nguyên tên trong code) — mỗi cơ sở có 1 tài khoản `HOTEL_MANAGER`
  quản lý, nhưng 1 quản lý có thể phụ trách nhiều cơ sở. Quản lý cơ sở quản lý nhân viên, tiện ích,
  dịch vụ, phòng, khách hàng, đơn đặt phòng và xem báo cáo của (các) cơ sở mình phụ trách.
- **Nhân viên** (`Employee`, role `STAFF`) — thuộc đúng 1 cơ sở, được quản lý cơ sở tạo qua
  `POST /employees`. Có thể tạo/sửa tiện ích, dịch vụ, phòng, khách hàng, đơn đặt phòng nhưng
  **không được xoá**.
- **`ADMIN`**: xem toàn bộ dữ liệu, **xét duyệt** chủ khách sạn, khoá/mở tài khoản, quản lý danh mục
  (loại phòng, chức vụ). Admin **không tạo** tài khoản, phòng, tiện ích, dịch vụ, nhân viên.
- **`CUSTOMER`**: tài khoản khách hàng, đặt phòng ở bất kỳ cơ sở nào.

Việc kiểm tra quyền theo phân cấp trên nằm ở `AccessGuard` (package `security`), gọi từ tầng
service — không phải chỉ role-check đơn thuần mà còn xác thực quan hệ sở hữu (chain ↔ owner,
branch ↔ manager, staff ↔ branch của họ) và trạng thái duyệt của chủ khách sạn.

Các domain chính: HotelChain, Hotel (cơ sở), Room, RoomType, Amenity, Dish (món ăn), Tour, TourBooking, Booking,
BookingRoom, BookingService (dịch vụ đi kèm), Customer, Employee, Position, Payroll, Feedback,
Payment, Report, Notification, DeviceToken, Conversation + ChatMessage (nhắn tin), Account (auth).

### Thực đơn món ăn

Mỗi cơ sở có thực đơn riêng (`Dish`: tên, mô tả, giá, nhóm món, ảnh, còn phục vụ/tạm hết), xoá cơ
sở thì xoá theo thực đơn. Nhóm món cố định theo thứ tự đọc thực đơn: `APPETIZER` (khai vị),
`MAIN_COURSE`, `SIDE_DISH`, `DESSERT`, `DRINK`, `OTHER`.

| Thao tác | Endpoint | Ai được gọi |
|---|---|---|
| Xem thực đơn | `GET /dishes?hotelId=`, `GET /dishes/{id}` | Công khai, không cần đăng nhập |
| Thêm món | `POST /dishes` | `HOTEL_OWNER` của chuỗi / `HOTEL_MANAGER` của cơ sở |
| Sửa món (sửa một phần) | `PUT /dishes/{id}` | như trên |
| Tải ảnh từ máy | `POST /dishes/{id}/image` (multipart, part `file`) | như trên |
| Xoá món | `DELETE /dishes/{id}` | như trên |

Nhân viên và quản trị viên chỉ xem (`403`). `PUT` chỉ đổi các trường được gửi, nên có thể gửi
`{"available": false}` để đánh dấu tạm hết; `pathImage: ""` để gỡ ảnh. Ảnh có thể là link ngoài
(`https://...`) hoặc đường dẫn `/uploads/...` sau khi tải lên. Danh sách trả về đã sắp theo nhóm món
rồi theo tên. Chưa có đặt món/tính tiền món vào đơn đặt phòng.

### Đổi phương thức thanh toán

Đơn chưa thanh toán và còn mở (chờ xác nhận / đã xác nhận) đổi được phương thức bằng
`PUT /bookings/{id}/payment-method` `{"paymentMethod": "CASH" | "BANK_TRANSFER" | "VN_PAY"}`. Khách
chỉ đổi được đơn của mình; chủ khách sạn, quản lý, nhân viên của cơ sở cũng đổi được; quản trị viên
chỉ xem.

| Đổi sang | Trạng thái thanh toán sau khi đổi |
|---|---|
| `VN_PAY` | `PENDING`, giữ đơn 15 phút để thanh toán online (quá hạn → `FAILED`, thanh toán lại được) |
| `CASH`, `BANK_TRANSFER` | `UNPAID`, không có hạn — trả tại khách sạn, lễ tân ghi nhận khi nhận tiền |

Đơn đã thanh toán, đã hoàn tất hoặc đã huỷ trả `400` mã `1019`. Gửi lại đúng phương thức đang dùng
thì không đổi gì (không làm mới hạn VNPay). Khách đã mở trang VNPay rồi đổi sang tiền mặt nhưng vẫn
trả tiền xong trên VNPay thì đơn được ghi `VN_PAY` + đã thanh toán. Sửa đơn (`PUT /bookings/{id}`) mà
đổi phương thức cũng áp dụng đúng quy tắc trên.

### Tour tham quan

Mỗi cơ sở giới thiệu các tour do khách sạn tổ chức (`Tour`: tên, mô tả/lịch trình, giá **mỗi khách**,
thời lượng, giờ khởi hành, giá đã gồm, số chỗ mỗi chuyến, ảnh, đang nhận khách/tạm ngừng), xoá cơ sở
thì xoá theo tour.

**Tour là gói tour + phòng:** khách sạn gắn cho mỗi tour các phòng cụ thể của cơ sở (`roomIds` — tour
bình dân gắn phòng Standard, tour cao cấp gắn Deluxe/Suite…) và số đêm ở (`stayNights`, mặc định 1).
Đặt tour = đặt luôn phòng: nhận phòng đúng ngày đi tour, ở `stayNights` đêm, khách chọn trong các phòng
của gói còn trống đêm đó. Tổng tiền = giá tour × số khách + giá phòng × số đêm. Tour không gắn phòng thì
chỉ đặt tour. Quyền giống thực đơn:

| Thao tác | Endpoint | Ai được gọi |
|---|---|---|
| Xem tour (kèm `stayNights`, `rooms`) | `GET /tours?hotelId=`, `GET /tours/{id}` | Công khai, không cần đăng nhập |
| Phòng của gói còn trống cho ngày đi | `GET /tours/{id}/rooms/available?date=YYYY-MM-DD` | Công khai |
| Thêm tour | `POST /tours` | `HOTEL_OWNER` của chuỗi / `HOTEL_MANAGER` của cơ sở |
| Sửa tour (sửa một phần) | `PUT /tours/{id}` | như trên |
| Tải ảnh từ máy | `POST /tours/{id}/image` (multipart, part `file`) | như trên |
| Xoá tour | `DELETE /tours/{id}` | như trên |

`PUT` chỉ đổi các trường được gửi: `{"available": false}` để tạm ngừng, `maxGuests: 0` để bỏ giới
hạn số khách, chuỗi rỗng để xoá mô tả/thời lượng/giờ khởi hành/giá đã gồm/ảnh, `roomIds` thay toàn bộ
phòng của gói (`[]` = bỏ hết, phòng phải thuộc cơ sở). Danh sách trả về đặt
tour đang nhận khách lên trước, rồi theo giá, rồi theo tên.

#### Đơn tour

Khách đặt tour trong app (chọn ngày, số khách, phòng) hoặc qua tin nhắn: trợ lý AI tra phòng trống,
gửi tóm tắt, khách nhắn "Đồng ý" thì trợ lý lên đơn (xem *Tích hợp AI*). Đơn tour (`TourBooking`) thanh
toán tại khách sạn, chốt tên tour và giá lúc đặt (sửa hay xoá tour sau đó không đổi đơn cũ). Trạng
thái: `PENDING` (chờ khách sạn xác nhận) → `CONFIRMED` → `COMPLETED`; đơn còn mở thì khách hoặc khách
sạn huỷ được (`CANCELED`).

**Đơn phòng đi kèm:** tour có phòng thì khi đặt, BE tạo luôn một đơn đặt phòng thường (`Booking`,
`bookingId` trong đơn tour): nhận phòng ngày đi tour, trả phòng sau `stayNights` đêm, các phòng khách
chọn, tiền mặt/chưa thanh toán, ghi chú "Phòng kèm đơn tour #…". Phòng vì vậy bị giữ với khách khác và
lễ tân nhận/trả phòng như mọi đơn (khách đổi được sang VNPay). Xác nhận đơn tour → đơn phòng xác nhận
theo; huỷ đơn tour → đơn phòng huỷ theo. Không huỷ riêng được đơn phòng của đơn tour đang mở (`400`
`1020`: "Phòng này thuộc đơn tour #…").

| Thao tác | Endpoint | Ai gọi |
|---|---|---|
| Đặt tour cho mình | `POST /tour-bookings` `{tourId, tourDate, guests, roomIds, note}` | Khách hàng (có hồ sơ) |
| Đơn tour của tôi | `GET /tour-bookings/me` | Khách hàng |
| Đơn tour của cơ sở | `GET /tour-bookings?hotelId=&status=` | Chủ / quản lý / nhân viên cơ sở (quản trị viên chỉ xem) |
| Chi tiết | `GET /tour-bookings/{id}` | Khách của đơn, đội ngũ cơ sở |
| Xác nhận / hoàn tất | `PUT /tour-bookings/{id}/confirm`, `/complete` | Chủ / quản lý / nhân viên cơ sở |
| Huỷ | `PUT /tour-bookings/{id}/cancel` `{reason}` | Khách của đơn (lý do tuỳ chọn) hoặc đội ngũ cơ sở (bắt buộc lý do) |

- Kiểm tra khi đặt: tour đang nhận khách, ngày đi từ hôm nay đến 1 năm tới, 1–50 khách mỗi đơn,
  `maxGuests` là **số chỗ mỗi chuyến** (tổng khách các đơn còn mở cùng ngày không vượt quá); tour có phòng
  thì bắt buộc chọn phòng, phòng phải thuộc gói, còn trống cả kỳ ở (không bảo trì, không trùng đơn chờ
  xác nhận/đã xác nhận) và đủ chỗ cho số khách. Lỗi trả `400` mã `1020` kèm câu tiếng Việt ("Phòng 101 đã
  có khách từ 18/10/2026 đến 19/10/2026. Hãy chọn phòng khác.").
- Khách đặt lại đúng tour, ngày, số khách, phòng thì nhận lại đơn đang mở (không tạo trùng); khác thì báo
  đã có đơn.
- **Thông báo** (trong app + đẩy FCM, deep link `bookingapp://tour-bookings/{id}`): đơn mới hoặc khách
  huỷ → chủ khách sạn, quản lý và nhân viên của cơ sở; khách sạn xác nhận hoặc huỷ (kèm lý do) → khách.

### Nhắn tin khách hàng ↔ khách sạn

Khách hàng (đã có hồ sơ) nhắn tin với **một cơ sở**; phía cơ sở là **hộp thư chung** — chủ khách
sạn, quản lý cơ sở và mọi nhân viên của cơ sở đều nhận và trả lời được, trạng thái "đã đọc" tính
theo phía (một người của cơ sở mở là cả cơ sở đã đọc). Quản trị viên không xem được tin nhắn.

- **REST** (gửi tin, lịch sử, hộp thư) dưới `/conversations`:

| Thao tác | Endpoint | Ai gọi |
|---|---|---|
| Mở (hoặc lấy lại) cuộc trò chuyện với cơ sở | `POST /conversations` `{hotelId}` | Khách hàng |
| Hộp thư của khách | `GET /conversations/me?page&size` | Khách hàng |
| Hộp thư của cơ sở | `GET /conversations/inbox?hotelId&page&size` (bỏ `hotelId` = mọi cơ sở mình phụ trách) | Chủ / quản lý / nhân viên |
| Số tin chưa đọc | `GET /conversations/unread-count?hotelId` | Cả hai phía |
| Chi tiết | `GET /conversations/{id}` | Hai bên của cuộc trò chuyện |
| Tin nhắn (mới nhất trước) | `GET /conversations/{id}/messages?beforeId&size` | Hai bên |
| Gửi tin (≤ 2000 ký tự) | `POST /conversations/{id}/messages` `{content}` | Hai bên |
| Đánh dấu đã đọc | `PUT /conversations/{id}/read` | Hai bên |

- **WebSocket** (nhận tin tức thì): `ws://<host>:8080/booking-app/api/v1/ws/chat?token=<accessToken>`
  (WebSocket thuần, không STOMP). Máy chủ đẩy JSON `{"type": "CONNECTED" | "MESSAGE" | "READ" | "PONG",
  "conversation": {...}, "message": {...}}` tới mọi thiết bị đang mở của cả hai phía; client gửi
  `{"type":"PING"}` để giữ kết nối. Token sai/đã đăng xuất → `401`, tài khoản quản trị → `403`.
- Người nhận **không mở app** (không có kết nối WebSocket) được gửi **thông báo đẩy FCM** kèm deep
  link `bookingapp://chat/{id}`; tin nhắn không tạo thông báo trong hộp thông báo.
- Xoá cơ sở hoặc hồ sơ khách thì các cuộc trò chuyện và tin nhắn bị xoá theo (ràng buộc
  `ON DELETE CASCADE`). Tên, vai trò người gửi lưu kèm từng tin nên xoá tài khoản nhân viên vẫn giữ
  được lịch sử.
- Chỉ chạy một instance: danh sách kết nối nằm trong bộ nhớ (`ChatSessionRegistry`); chạy nhiều máy
  chủ thì cần message broker dùng chung.

### Tích hợp AI (mặc định Google Gemini, gói miễn phí)

Mọi lời gọi AI đi qua BE (`ai/AiClient`) — app không bao giờ giữ API key. Nhà cung cấp chọn bằng biến
môi trường, logic AI không đổi:

| `AI_PROVIDER` | Key | Model mặc định (trợ lý / tìm kiếm) |
|---|---|---|
| `gemini` (tự chọn khi có `GEMINI_API_KEY`) | `GEMINI_API_KEY` — aistudio.google.com → *Get API key* | `gemini-3.8-flash` / `gemini-3.5-flash-lite` (đều có gói free) |
| `anthropic` | `ANTHROPIC_API_KEY` (trả phí) | `claude-sonnet-5` / `claude-haiku-4-5-20251001` |
| `openai` (Groq, OpenRouter, Ollama… qua `AI_BASE_URL`) | `AI_API_KEY` | đặt bằng `AI_ASSISTANT_MODEL` / `AI_FAST_MODEL` |

Gemini được gọi qua endpoint chuẩn OpenAI của Google (`…/v1beta/openai/chat/completions`);
`ai/OpenAiCompat` đổi qua lại giữa định dạng đó và định dạng nội bộ, và gửi lại nguyên văn lượt gọi
công cụ (gồm *thought signature* mà Gemini 3 bắt buộc). Không có key nào thì mọi tính năng AI tự tắt,
`GET /ai/status` trả `enabled: false` và app ẩn các nút AI. **Lưu ý:** ở gói miễn phí, Google có thể
dùng dữ liệu gửi lên để cải thiện sản phẩm — chỉ demo với dữ liệu test.

1. **Trợ lý lễ tân trong chat** (`ai/ChatAssistant`):
   - AI chỉ biết **hồ sơ cơ sở** lấy từ DB (`ai/HotelKnowledge`: loại phòng và giá, tiện ích, dịch vụ,
     thực đơn, tour tham quan) và **tra phòng trống thật** qua công cụ `find_available_rooms` — không
     tự bịa giá hay phòng. Việc cần người (đặt/huỷ/đổi đơn, thanh toán, khiếu nại, câu hỏi hồ sơ không
     có) → công cụ `handoff_to_staff`: cuộc trò chuyện được gắn `needsStaff` + lý do cho đội ngũ cơ sở
     (khách không thấy).
   - **Gợi ý tour:** khách hỏi đi chơi, tham quan, ăn uống quanh khu vực hoặc nói sẽ ở vài ngày thì
     trợ lý gợi ý 1–3 tour phù hợp của cơ sở (tên, giá mỗi khách, thời lượng, giờ khởi hành), không
     gợi ý tour tạm ngừng.
   - **Lên đơn tour (công cụ `find_tour_rooms` + `create_tour_booking`), 2 bước:** (1) đủ tour, ngày đi,
     số khách thì trợ lý tra phòng của gói còn trống (tour kèm phòng), cho khách chọn hoặc gợi ý phòng rẻ
     nhất đủ chỗ, gửi tóm tắt gồm phòng, ngày nhận/trả, tiền tour + tiền phòng và mời khách nhắn "Đồng ý"; (2) khách đồng ý thì trợ lý lên đơn, báo mã đơn,
     khách sạn nhận thông báo. Máy chủ tự chặn: tin mới nhất của khách phải là lời đồng ý ("đồng ý", "ok",
     "xác nhận", "chốt", "được"… — không dấu cũng nhận) thì mới tạo đơn, nên model bỏ qua bước tóm tắt
     cũng không đặt được. Trợ lý thấy các đơn tour gần đây của khách ở cơ sở nên trả lời được "đơn của tôi
     xác nhận chưa" và không đặt trùng. Đổi ngày/số khách → chuyển nhân viên; huỷ → khách tự huỷ trong app.
   - **Gợi ý trả lời:** `POST /ai/conversations/{id}/suggest-reply` (chủ/quản lý/nhân viên của cơ sở)
     trả bản nháp, **không gửi gì cho khách**.
   - **Tự trả lời:** chủ khách sạn/quản lý bật bằng `PUT /ai/branches/{hotelId}/settings
     {"autoReply": true}` (`GET` để xem). Khách nhắn mà sau `AI_AUTO_REPLY_DELAY_SECONDS` (mặc định 90
     giây) chưa ai của cơ sở trả lời thì trợ lý trả lời, tin mang nhãn "Trợ lý AI" (`aiGenerated`).
     Nhân viên trả lời kịp hoặc khách nhắn tiếp thì bỏ lượt đó; nhân viên trả lời thì `needsStaff` tự xoá.
2. **Tìm phòng bằng câu nói tự nhiên:** `POST /ai/search {"query": "..."}` (công khai, 20 lần/10 phút mỗi
   tài khoản hoặc IP). Model nhanh chỉ **tách câu thành bộ lọc** (ngày, số khách, giá, khu vực, tiện ích);
   lọc cơ sở và phòng trống do BE làm trên dữ liệu thật. Điều kiện không dùng được (ngày đã qua, tiện ích
   không cơ sở nào có) được bỏ kèm ghi chú `notes`.

**Khi Gemini quá tải hoặc hết lượt:** model mới nhất ở gói miễn phí hay trả `503` "This model is currently
experiencing high demand" (lỗi phía Google, không phải hết lượt). BE tự thử lại 2 lần (chờ 1 s rồi
2,5 s), vẫn lỗi hoặc gặp `429` (hết lượt — mỗi model có hạn mức riêng) thì chuyển sang model dự phòng:
trợ lý `gemini-3.8-flash` → `gemini-3.5-flash` → `gemini-2.5-flash`, tìm kiếm `gemini-3.5-flash-lite` →
`gemini-2.5-flash-lite`. Một lượt trả lời (kể cả các vòng gọi công cụ) luôn ở trên một model. Đổi chuỗi
dự phòng bằng `AI_ASSISTANT_FALLBACK_MODELS` / `AI_FAST_FALLBACK_MODELS` (danh sách cách nhau bởi dấu
phẩy, `none` = tắt).

Mã lỗi: `1015 AI_UNAVAILABLE` (503 — chưa có key hoặc lỗi khác), `1016 AI_RATE_LIMITED` (429 — giới hạn
của chính app), `1017 AI_OVERLOADED` (503 — mọi model đều quá tải), `1018 AI_QUOTA_EXCEEDED` (429 — mọi
model đều hết lượt).

### Đăng ký chủ khách sạn và xét duyệt

1. Khách vãng lai gửi `POST /owner-registrations` (công khai): tên đăng nhập, mật khẩu, thông tin
   khách sạn (tên, địa chỉ, SĐT, email, người đại diện, giới thiệu). Tài khoản `HOTEL_OWNER` và
   khách sạn được tạo với trạng thái `PENDING`; quản trị viên nhận thông báo.
2. Chủ khách sạn đăng nhập được ngay nhưng chỉ xem được `GET /owner-registrations/me` và sửa/gửi lại
   bằng `PUT /owner-registrations/me`. Mọi thao tác vận hành trả `403` mã `1011`.
3. Quản trị viên xem hàng chờ `GET /owner-registrations?status=PENDING`, rồi
   `PUT /owner-registrations/{id}/approve` hoặc `PUT /owner-registrations/{id}/reject` (bắt buộc
   `reason`).
4. Chủ khách sạn nhận thông báo trong app (`GET /notifications`), thông báo đẩy FCM và email. Nút
   trong email mở `GET /app-links/owner-status`, trang này chuyển điện thoại sang deep link
   `bookingapp://owner-status` của app.
5. Bị từ chối thì sửa theo lý do và gửi lại (quay về bước 3). Được duyệt thì chủ khách sạn tự tạo
   quản lý, mở cơ sở, thêm phòng, tiện ích, dịch vụ, thực đơn món ăn, tour tham quan.

### Báo cáo doanh thu

Mỗi báo cáo có hai bản: cấp cơ sở `/reports/...?hotelId=` (chủ chuỗi hoặc quản lý cơ sở) và cấp chuỗi
`/reports/chain/...?hotelChainId=` (chủ chuỗi). Doanh thu chỉ tính đơn **hoàn tất**: đơn phòng ghi
nhận vào ngày trả phòng, đơn tour vào ngày đi. Tiền phòng của gói tour nằm ở đơn phòng đi kèm nên chỉ
được tính một lần, khi khách trả phòng.

| Báo cáo | Endpoint | Ghi chú |
|---|---|---|
| Tổng quan kỳ | `GET /reports/summary?from=&to=` | Tổng doanh thu (phòng + dịch vụ + tour), ADR, RevPAR, lấp đầy, tỷ lệ huỷ; so với kỳ liền trước cùng số ngày; chia theo phương thức thanh toán và kênh bán (khách tự đặt / tại quầy / tour). `from`, `to` tính cả hai đầu |
| Doanh thu theo thời gian | `GET /reports/daily`, `/weekly`, `/weekly-in-month`, `/monthly`, `/monthly/last-5-months`, `/yearly` | Chỉ đơn phòng (đã gồm dịch vụ) |
| Lấp đầy | `GET /reports/occupancy`, `/occupancy/daily` | `[from, to)`; theo ngày tối đa 92 ngày, chỉ cấp cơ sở |
| Doanh thu theo tour | `GET /reports/revenue-by-tour?from=&to=` | Đơn tour hoàn tất gộp theo tour: số đơn, số khách, tiền tour, tiền phòng gói (chỉ tham khảo), số đơn do AI lên |
| Cơ cấu, khách, nhân viên | `GET /reports/revenue-by-room-type`, `/revenue-by-service`, `/top-customers`, `/staff-performance`, `/cancellation-rate` | |
| Xuất Excel | `GET /reports/export` | Nhiều sheet |

Cách tính trong tổng quan: tiền phòng = tổng đơn − dòng dịch vụ; ADR = tiền phòng / đêm đã bán; RevPAR =
tiền phòng / (số phòng × số ngày). Mức tăng trưởng là % so với kỳ trước, bị bỏ khỏi JSON khi kỳ trước
bằng 0; lấp đầy so bằng điểm phần trăm (`occupancyChange`).

## Yêu cầu môi trường

- JDK 21
- Maven (hoặc dùng `./mvnw` / `mvnw.cmd` đã kèm sẵn)
- MySQL 8.x (nếu chạy local không dùng Docker)
- Docker + Docker Compose (nếu chạy bằng container)

## Chạy local (không Docker)

1. Tạo database MySQL `booking_app_db` (mặc định user `root`, xem `application.yaml` để đổi).
2. Set các biến môi trường cần thiết nếu muốn override default (xem bảng biến môi trường bên dưới),
   hoặc sửa trực tiếp `src/main/resources/application.yaml`.
3. Build & chạy:

```bash
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

API sẽ chạy tại `http://localhost:8080/booking-app/api/v1`.

Khi khởi động lần đầu, ứng dụng tự tạo tài khoản admin mặc định `admin123` / `admin123`
(`ApplicationInitConfig`) — **đổi ngay sau khi chạy production**. Các khách sạn tạo trước khi có quy
trình duyệt được tự đánh dấu `APPROVED` khi khởi động.

> **Lưu ý khi cập nhật từ schema cũ:** entity `Hotel` vừa được thêm quan hệ bắt buộc tới
> `HotelChain` (`hotel_chain_id NOT NULL`) và đổi `Hotel.account` từ 1-1 sang N-1. Vì project dùng
> `ddl-auto=update` (không có Flyway/Liquibase), Hibernate không tự ALTER được các thay đổi này trên
> dữ liệu cũ — cần xoá và tạo lại database (`DROP DATABASE booking_app_db; CREATE DATABASE
> booking_app_db;`) trước lần chạy đầu tiên sau khi cập nhật. Các cột duyệt chủ khách sạn và bảng
> `notifications`, `device_tokens` thì Hibernate tự thêm, không cần tạo lại database.

## Chạy bằng Docker

```bash
cp .env.example .env   # tùy chọn, chỉnh nếu muốn đổi config mặc định
docker compose up -d --build
# hoặc
./deploy.sh
```

App chạy ở `http://localhost/booking-app/api/v1` (port 80 map vào container 8080). Thư mục
`uploads/` được mount làm volume để ảnh không mất khi container restart; `secrets/` được mount
chỉ-đọc vào `/app/secrets` cho file khoá Firebase.

Kiểm tra:

```bash
docker compose ps
docker compose logs -f booking-app
docker compose logs -f booking-db
```

Hướng dẫn deploy lên EC2: [DEPLOY_EC2.md](DEPLOY_EC2.md).

## Email và thông báo đẩy

Cả hai đều **tuỳ chọn**: chưa cấu hình thì BE vẫn chạy, thông báo trong app vẫn có, email chỉ được
ghi log.

**Email (Gmail):**

1. Bật xác minh 2 bước cho tài khoản Google, vào *Bảo mật → Mật khẩu ứng dụng* tạo App Password.
2. Đặt `MAIL_USERNAME` = địa chỉ Gmail, `MAIL_PASSWORD` = App Password 16 ký tự (không dấu cách).
3. Đặt `APP_PUBLIC_URL` là địa chỉ API mà **điện thoại mở được** (ví dụ
   `http://192.168.1.10:8080/booking-app/api/v1` khi chạy trong mạng LAN), vì nút "Mở ứng dụng" trong
   email trỏ về đây.

**Thông báo đẩy (Firebase Cloud Messaging):**

1. Tạo project trên [Firebase console](https://console.firebase.google.com), thêm app Android với
   package `com.example.booking_app_demo`, tải `google-services.json` cho app Flutter (xem README FE).
2. *Project settings → Service accounts → Generate new private key*, lưu file JSON (không commit —
   `.gitignore` đã chặn `secrets/` và `*service-account*.json`).
3. Đặt `FIREBASE_CREDENTIALS` = đường dẫn tới file đó (Docker: đặt file vào `./secrets/` và dùng
   `/app/secrets/<tên-file>.json`). Log khởi động không còn dòng `FIREBASE_CREDENTIALS is not set`
   là đã bật.

Thông báo hiện cả khi app **chạy nền hoặc đã tắt**: mọi push (tin nhắn, đơn tour, xét duyệt hồ sơ)
gửi kèm khối `notification`, `priority: high`, vào kênh Android `booking_app_alerts` mà app tạo sẵn ở
mức ưu tiên cao (bật lên đầu màn hình, có âm thanh). Tin nhắn chat chỉ đẩy cho người **không** đang mở
app (không có WebSocket); socket im lặng quá 70 giây — điện thoại rớt mạng hay bị hệ điều hành tắt mà
không kịp đóng — bị BE đóng để người đó không bị bỏ sót push.

Thử nhận thông báo: `POST /notifications/devices/test?delaySeconds=10` gửi "Thông báo thử" tới các thiết
bị của chính tài khoản đang đăng nhập sau `delaySeconds` giây (0–60), không thêm vào hộp thông báo; trả
`{pushEnabled, devices, delaySeconds}` (`pushEnabled: false` = BE chưa có `FIREBASE_CREDENTIALS`,
`devices: 0` = máy chưa đăng ký token).

## Biến môi trường chính

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `PORT` | `8080` | Port ứng dụng lắng nghe |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/booking_app_db` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `root` / (xem `application.yaml`) | Thông tin DB |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Chiến lược Hibernate DDL |
| `JWT_SIGNER_KEY` | key demo trong `application.yaml` | Secret ký JWT (HS512) — **bắt buộc đổi khi deploy thật** |
| `VNPAY_TMN_CODE` / `VNPAY_SECRET_KEY` / `VNPAY_URL` / `VNPAY_RETURN_URL` | sandbox VNPay | Cấu hình cổng thanh toán VNPay |
| `MAIL_HOST` / `MAIL_PORT` | `smtp.gmail.com` / `587` | Máy chủ SMTP |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | trống | Tài khoản SMTP; trống thì chỉ ghi log email |
| `MAIL_FROM` | trống (dùng `MAIL_USERNAME`) | Địa chỉ người gửi |
| `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` | `true` / `true` | Tắt khi thử với SMTP giả trên máy |
| `APP_PUBLIC_URL` | `http://localhost:8080/booking-app/api/v1` | Địa chỉ API công khai cho link trong email |
| `FIREBASE_CREDENTIALS` | trống | Đường dẫn file JSON service account Firebase; trống thì tắt push |
| `GEMINI_API_KEY` | trống | Key Google AI Studio — bật AI với Gemini (gói miễn phí) |
| `ANTHROPIC_API_KEY` | trống | Key Claude (trả phí), dùng khi `AI_PROVIDER=anthropic` |
| `AI_PROVIDER` | tự chọn theo key | `gemini` \| `anthropic` \| `openai` |
| `AI_API_KEY`, `AI_BASE_URL` | trống | Key / địa chỉ cho máy chủ chuẩn OpenAI khác (Groq, OpenRouter, Ollama) hoặc máy chủ giả lập khi test |
| `AI_ASSISTANT_MODEL`, `AI_FAST_MODEL` | theo nhà cung cấp | Đổi model trợ lý / tìm kiếm |
| `AI_AUTO_REPLY_DELAY_SECONDS` | `90` | Chờ bao lâu không ai trả lời thì trợ lý tự trả lời |

Xem đầy đủ trong [.env.example](.env.example).

## Test

```bash
./mvnw test
```

Bộ test đầu-cuối là `postman/booking_app.postman_collection.json` (chạy tuần tự trong Collection
Runner), gồm cả luồng đăng ký → từ chối → gửi lại → duyệt chủ khách sạn và các ca admin chỉ được xem.

## Ghi chú

- File `src/main/resources/static/booking_db.sql` là bản dump schema/seed tham khảo, **không**
  phải migration tool — schema thực tế do Hibernate tự đồng bộ (`ddl-auto=update`).
- Format code tự động chạy qua Spotless (Palantir Java Format) ở phase `compile` của Maven — chạy
  `./mvnw compile` sẽ format lại **toàn bộ** file Java trong project, không chỉ file vừa sửa, nên
  đừng bất ngờ nếu `git status` báo nhiều file "modified" hơn số file bạn thực sự chỉnh.
