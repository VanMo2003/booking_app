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

Các domain chính: HotelChain, Hotel (cơ sở), Room, RoomType, Amenity, Dish (món ăn), Booking,
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
     thực đơn) và **tra phòng trống thật** qua công cụ `find_available_rooms` — không tự bịa giá hay
     phòng. Việc cần người (đặt/huỷ/đổi đơn, thanh toán, khiếu nại, câu hỏi hồ sơ không có) → công cụ
     `handoff_to_staff`: cuộc trò chuyện được gắn `needsStaff` + lý do cho đội ngũ cơ sở (khách không
     thấy).
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
   quản lý, mở cơ sở, thêm phòng, tiện ích, dịch vụ, thực đơn món ăn.

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
