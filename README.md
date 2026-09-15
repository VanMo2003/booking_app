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
event       -> sự kiện sau khi commit: gửi push, gửi email xét duyệt
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

Các domain chính: HotelChain, Hotel (cơ sở), Room, RoomType, Amenity, Booking, BookingRoom,
BookingService (dịch vụ đi kèm), Customer, Employee, Position, Payroll, Feedback, Payment, Report,
Notification, DeviceToken, Account (auth).

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
   quản lý, mở cơ sở, thêm phòng, tiện ích, dịch vụ.

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
