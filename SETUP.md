# Cài đặt booking_app từ đầu đến cuối

Hướng dẫn này đi từ một máy Windows trắng tới chỗ chạy được **backend**, **app Android** và
**app web**, kèm dữ liệu mẫu để bấm vào đâu cũng có nội dung.

Dự án gồm hai kho mã:

| Kho | Nội dung | Địa chỉ |
| --- | --- | --- |
| `booking_app` | Backend Spring Boot (API, cơ sở dữ liệu, AI) | https://github.com/VanMo2003/booking_app |
| `booking_app_demo` | App Flutter (Android + web) | https://github.com/VanMo2003/booking_app_demo |

Cả hai làm việc trên nhánh **`develop`**, không phải `main`.

> **Nếu bạn là AI đang đọc file này để tự cài:** làm lần lượt từ phần 1 đến phần 7. Sau mỗi phần có
> mục *Kiểm tra* — chạy đúng lệnh đó và chỉ đi tiếp khi kết quả khớp. Phần 8 là các lỗi thường gặp và
> cách xử lý. Đừng bỏ qua phần 4 (cơ sở dữ liệu): backend không khởi động nổi nếu thiếu MySQL.

---

## 0. Tóm tắt cho người vội

```bash
git clone -b develop https://github.com/VanMo2003/booking_app.git
git clone -b develop https://github.com/VanMo2003/booking_app_demo.git
```

Rồi: cài JDK 21 + MySQL 8 + Flutter 3.47 → tạo `booking_app/.env` → `mvnw.cmd spring-boot:run` →
nạp `db/seed_demo.sql` → `flutter run`. Chi tiết ở dưới.

---

## 1. Phần mềm cần cài

> **Đi đường tắt:** nếu đã có Git và đã clone kho `booking_app`, chạy script dưới đây là biết ngay
> còn thiếu gì, và thêm `-Install` thì nó cài giúp luôn (xem [phần 1.0](#10-script-kiểm-tra-và-cài-giúp)).
> Vẫn chưa có Git thì cài Git ở mục 1.1 trước đã.

### 1.0 Script kiểm tra và cài giúp

```powershell
# chỉ kiểm tra, không đụng gì vào máy
powershell -ExecutionPolicy Bypass -File .\tools\check-env.ps1

# cài những thứ còn thiếu (mở PowerShell bằng "Run as administrator")
powershell -ExecutionPolicy Bypass -File .\tools\check-env.ps1 -Install -Yes
```

Script in ra một bảng: `[OK]` là xong, `[THIEU]`/`[SAI]` là phải xử lý, `[CAN XEM]` là không bắt
buộc nhưng nên đọc. Cuối bảng có danh sách việc còn lại kèm lệnh cần chạy. Mã thoát 0 nghĩa là đủ,
1 nghĩa là còn thiếu thứ bắt buộc.

Nó kiểm tra cả công cụ (Git, JDK **đúng bản 21**, `JAVA_HOME`, MySQL, Flutter đúng phiên bản tối
thiểu, Android SDK, Chrome) lẫn tình trạng dự án (đã clone chưa, đúng nhánh chưa, đã sinh mã chưa,
có `.env` chưa và `JWT_SIGNER_KEY` đủ dài chưa, database đã tạo và đã nạp dữ liệu mẫu chưa, backend
có đang chạy không).

Hai điều script **không** làm, phải tự làm tay: điền `.env` (nó không đoán mật khẩu MySQL của bạn)
và tạo máy ảo Android.

Phần còn lại của mục 1 là cách cài tay, dùng khi script báo lỗi hoặc khi muốn hiểu nó đang làm gì.

### Cài tay

Cài theo đúng thứ tự này. Sau mỗi mục có lệnh kiểm tra, mở **PowerShell mới** rồi chạy (cửa sổ cũ
không thấy biến môi trường vừa cài).

### 1.1 Git

Tải tại https://git-scm.com/download/win, cài với tuỳ chọn mặc định.

```powershell
git --version
```

### 1.2 JDK 21

Backend cần **Java 21** (không chạy được trên Java 17 hay 25). Tải Temurin 21 LTS tại
https://adoptium.net/temurin/releases/?version=21 — chọn bản **JDK**, kiểu **.msi**, kiến trúc x64.

Trong lúc cài, bật mục **Set JAVA_HOME variable** (mặc định đang tắt).

```powershell
java -version        # phải thấy 21.x
echo $env:JAVA_HOME  # phải trỏ tới thư mục JDK 21
```

Máy đã có nhiều JDK thì đặt `JAVA_HOME` về bản 21: *Settings → System → About → Advanced system
settings → Environment Variables*.

### 1.3 MySQL 8

Tải MySQL Installer tại https://dev.mysql.com/downloads/installer/ → chọn **Server only**.

Trong lúc cài:
- Cổng: **3306** (mặc định)
- Authentication: **Use Strong Password Encryption**
- Đặt mật khẩu cho `root` và **nhớ lại** — lát nữa cần điền vào `.env`
- Bật **Start the MySQL Server at System Startup**

```powershell
mysql --version
```

Không thấy lệnh `mysql` thì thêm `C:\Program Files\MySQL\MySQL Server 8.0\bin` vào biến `Path`.

### 1.4 Flutter 3.47 trở lên

Theo https://docs.flutter.dev/get-started/install/windows — tải bản zip, giải nén vào
`C:\src\flutter` (tránh thư mục có dấu cách và tránh `Program Files`), rồi thêm
`C:\src\flutter\bin` vào `Path`.

```powershell
flutter --version    # cần 3.47 trở lên, Dart 3.5 trở lên
```

### 1.5 Android Studio (để chạy app trên Android)

Tải tại https://developer.android.com/studio. Mở lần đầu, nó sẽ tải Android SDK.

Sau đó vào **More Actions → SDK Manager → SDK Tools** và bật thêm:
- **Android SDK Command-line Tools (latest)**
- **Android SDK Platform-Tools**

Tạo máy ảo: **More Actions → Virtual Device Manager → Create Device** → chọn Pixel bất kỳ → tải ảnh
hệ thống API 34 hoặc mới hơn → Finish.

Chấp nhận giấy phép SDK:

```powershell
flutter doctor --android-licenses   # bấm y cho tới hết
flutter doctor
```

`flutter doctor` phải xanh ở **Flutter** và **Android toolchain**. Mục Visual Studio (để build
Windows desktop) đỏ cũng không sao — dự án này chỉ cần Android và web.

### 1.6 Chrome

Cần cho bản web. Flutter tự tìm Chrome đã cài sẵn, không phải cấu hình gì.

---

## 2. Lấy mã nguồn

Chọn một thư mục làm việc, ví dụ `D:\datn`:

```powershell
mkdir D:\datn
cd D:\datn
git clone -b develop https://github.com/VanMo2003/booking_app.git
git clone -b develop https://github.com/VanMo2003/booking_app_demo.git
```

**Kiểm tra:** cả hai thư mục đều có mã và đang ở nhánh `develop`.

```powershell
cd D:\datn\booking_app;      git branch --show-current   # develop
cd D:\datn\booking_app_demo; git branch --show-current   # develop
```

---

## 3. Tạo cơ sở dữ liệu rỗng

```powershell
mysql -u root -p
```

Nhập mật khẩu root đã đặt ở bước 1.3, rồi gõ trong dấu nhắc `mysql>`:

```sql
CREATE DATABASE booking_app_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SHOW DATABASES;
EXIT;
```

**Kiểm tra:** danh sách in ra có `booking_app_db`.

> Dự án dùng `ddl-auto=update`, nên **không cần tạo bảng**: Hibernate tự tạo khi backend khởi động
> lần đầu.

---

## 4. Cấu hình backend

Tạo file `D:\datn\booking_app\.env`. File này nằm trong `.gitignore` nên không bao giờ bị đẩy lên
GitHub — đó là chỗ duy nhất để khoá bí mật.

```powershell
cd D:\datn\booking_app
notepad .env
```

Dán nội dung sau, thay `MAT_KHAU_ROOT_CUA_BAN`:

```dotenv
# Cơ sở dữ liệu
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/booking_app_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=MAT_KHAU_ROOT_CUA_BAN

# Khoá ký JWT: chuỗi bất kỳ, tối thiểu 64 ký tự (HMAC-SHA512)
JWT_SIGNER_KEY=doi_chuoi_nay_thanh_mot_chuoi_ngau_nhien_that_dai_it_nhat_64_ky_tu_nhe

# Tính năng AI: để trống thì app vẫn chạy, chỉ là mọi lối vào AI tự ẩn.
# Lấy key miễn phí tại https://aistudio.google.com > Get API key
GEMINI_API_KEY=

# VNPay sandbox (để trống thì chỉ không thanh toán online được)
VNPAY_TMN_CODE=
VNPAY_SECRET_KEY=
```

**Không dùng dấu nháy** quanh giá trị và không để dấu cách quanh dấu `=`.

### Nạp `.env` rồi chạy backend

PowerShell không tự đọc `.env`, nên nạp bằng lệnh này mỗi khi mở cửa sổ mới:

```powershell
cd D:\datn\booking_app
Get-Content .env | Where-Object { $_ -match '^[A-Z]' } | ForEach-Object {
  $pair = $_ -split '=', 2
  [Environment]::SetEnvironmentVariable($pair[0].Trim(), $pair[1].Trim(), 'Process')
}
.\mvnw.cmd spring-boot:run
```

Lần đầu Maven tải thư viện nên mất vài phút.

**Kiểm tra:** mở cửa sổ PowerShell **thứ hai** và gọi:

```powershell
curl.exe http://localhost:8080/booking-app/api/v1/ai/status
```

Phải nhận về JSON có `"enabled"`. `enabled: false` là đúng khi chưa có `GEMINI_API_KEY`; mọi thứ
khác vẫn chạy, chỉ các màn AI tự ẩn trong app.

Trong log khởi động sẽ thấy dòng tạo tài khoản quản trị mặc định: **admin123 / admin123**.

> **Cách khác, không cần cài MySQL:** nếu máy đã có Docker Desktop thì chỉ cần
> `docker compose up -d --build` trong `booking_app` — nó dựng cả MySQL lẫn backend. Khi đó bỏ qua
> phần 3 và phần nạp `.env` ở trên, chỉ cần `copy .env.example .env` rồi sửa.

---

## 5. Nạp dữ liệu mẫu

Chỉ chạy **sau khi backend đã khởi động xong ít nhất một lần** (để Hibernate tạo bảng).

```powershell
cd D:\datn\booking_app
mysql -u root -p booking_app_db < db\seed_demo.sql
```

Cuối lệnh in ra một bảng đếm số dòng của từng loại dữ liệu — tất cả phải lớn hơn 0.

Sau bước này có sẵn:

| Đăng nhập | Mật khẩu | Vai trò |
| --- | --- | --- |
| `admin123` | `admin123` | Quản trị hệ thống |
| `owner_demo` | `owner123` | Chủ chuỗi "Chuỗi Mơ Mộng" (đã duyệt) |
| `manager_demo` | `manager123` | Quản lý hai cơ sở |
| `staff_demo` | `staff123` | Nhân viên cơ sở Hoàn Kiếm |
| `cust_demo` | `customer123` | Khách Trần Thu Hà |
| `cust_demo2` | `customer123` | Khách Lê Minh Quân |

Kèm theo: 2 cơ sở, 6 phòng, 2 tour (một tour bán kèm phòng), 6 món ăn, 3 dịch vụ, 2 mã giảm giá,
6 đơn phòng ở các trạng thái khác nhau và 4 đánh giá — đủ để báo cáo, "Việc hôm nay" và phần tóm
tắt đánh giá của AI đều có số liệu thật.

File này chạy lại được bao nhiêu lần cũng được: nó xoá đúng phần dữ liệu mẫu (mã 9xx) rồi chèn lại,
không đụng tới dữ liệu bạn tự tạo trên app.

---

## 6. Chạy app Android

```powershell
cd D:\datn\booking_app_demo
flutter pub get
dart run build_runner build --delete-conflicting-outputs
```

Lệnh thứ ba sinh mã cho router, dependency injection và các lớp gọi API. **Bắt buộc chạy**, nếu
không sẽ gặp hàng loạt lỗi "không tìm thấy `app_router.gr.dart`".

Mở máy ảo (Android Studio → Virtual Device Manager → nút ▶) rồi:

```powershell
flutter devices    # thấy emulator-5554 là được
flutter run
```

App tự biết gọi backend ở đâu:
- Máy ảo Android → `http://10.0.2.2:8080` (địa chỉ máy tính nhìn từ máy ảo)
- Điện thoại thật cắm USB → IPv4 của máy tính trong cùng mạng Wi-Fi

**Kiểm tra:** app mở ra trang danh sách cơ sở và thấy **Mơ Mộng Hoàn Kiếm**. Đăng nhập
`manager_demo` / `manager123` rồi vào tab **Quản lý** sẽ thấy phòng, đơn và báo cáo có số.

### Điện thoại thật

Bật *Developer options → USB debugging* trên máy, cắm cáp, rồi `flutter run`. Máy tính và điện
thoại phải chung một mạng Wi-Fi. Nếu app báo không gọi được API, mở cổng 8080 trong Windows
Firewall:

```powershell
# chạy PowerShell với quyền Administrator
New-NetFirewallRule -DisplayName "booking_app 8080" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
```

---

## 7. Chạy app web

```powershell
cd D:\datn\booking_app_demo
flutter run -d chrome
```

Chrome tự mở. Bản web gọi thẳng `http://localhost:8080`, và backend đã mở sẵn CORS cho mọi origin
nên không cần proxy gì thêm.

**Kiểm tra:** mở DevTools (F12) → tab Network → tải lại trang. Lời gọi
`.../hotels` phải trả 200. Nếu thấy lỗi CORS, xem mục 8.5.

Muốn build ra thư mục tĩnh để đem đi nơi khác:

```powershell
flutter build web --release
# kết quả nằm trong build\web
```

Bản build sẵn này mặc định vẫn gọi `localhost:8080`. Trỏ sang máy chủ khác thì truyền lúc build:

```powershell
flutter build web --release --dart-define=API_BASE_URL=http://192.168.1.50:8080/booking-app/api/v1
```

> Mở `build\web\index.html` bằng cách bấm đúp sẽ **không chạy** (giao thức `file://`). Phải phục vụ
> qua một máy chủ web, ví dụ `python -m http.server 5500` rồi mở `http://localhost:5500`.

---

## 8. Lỗi thường gặp

### 8.1 Backend báo `Unable to determine Dialect without JDBC metadata`

Backend không kết nối được MySQL. Kiểm tra theo thứ tự: MySQL đang chạy chưa
(`Get-Service MySQL*`), `.env` đã được nạp vào cửa sổ hiện tại chưa (`echo $env:SPRING_DATASOURCE_PASSWORD`),
mật khẩu đúng chưa, và `booking_app_db` đã tạo chưa.

### 8.2 `Access denied for user 'root'@'localhost'`

Sai mật khẩu trong `.env`, hoặc `.env` chưa được nạp nên backend dùng mật khẩu rỗng.

### 8.3 `Unsupported class file major version` khi chạy `mvnw.cmd`

`JAVA_HOME` đang trỏ sang JDK khác 21. Kiểm tra `echo $env:JAVA_HOME` và `java -version`.

### 8.4 Flutter báo thiếu `*.g.dart` hoặc `*.gr.dart`

Chưa chạy bước sinh mã:

```powershell
dart run build_runner build --delete-conflicting-outputs
```

Vẫn lỗi thì xoá cache rồi làm lại: `flutter clean; flutter pub get;` và chạy lại lệnh trên.

### 8.5 Web báo lỗi CORS

Backend đã bật CORS cho mọi origin (`CorsConfig` + `.cors()` trong `SecurityConfig`). Gặp lỗi này
nghĩa là đang chạy bản backend cũ — vào `booking_app` chạy `git pull`, dừng backend rồi chạy lại.

### 8.6 App trên máy ảo không gọi được API

Máy ảo phải gọi `10.0.2.2` chứ không phải `localhost` — app tự xử lý việc này. Nếu vẫn lỗi, kiểm
tra backend có đang chạy không, và thử trong trình duyệt của chính máy ảo:
`http://10.0.2.2:8080/booking-app/api/v1/ai/status`.

### 8.7 Các màn AI không hiện trong app

Đúng như thiết kế khi chưa có khoá AI. Lấy khoá miễn phí ở https://aistudio.google.com, điền vào
`GEMINI_API_KEY` trong `.env`, khởi động lại backend. Kiểm tra bằng
`curl.exe http://localhost:8080/booking-app/api/v1/ai/status` — phải thấy `"enabled":true`.

### 8.8 Gradle lỗi khi `flutter run` lần đầu

Lần đầu Gradle tải khá nhiều thứ, cần mạng ổn định và có thể mất 5–10 phút. Nếu đứt giữa chừng thì
chạy lại. Lỗi còn lại thì xoá cache: `cd android; .\gradlew.bat clean; cd ..; flutter clean`.

---

## 9. Thử API bằng Postman

Trong thư mục `booking_app\postman` có hai file:

| File | Import vào Postman bằng |
| --- | --- |
| `booking_app.postman_collection.json` | *Import* → chọn file → thành một Collection |
| `booking_app.postman_environment.json` | *Import* → chọn file → chọn ở góc phải trên |

Environment đã điền sẵn `baseUrl`, tài khoản đăng nhập và mã các dòng dữ liệu mẫu, nên gọi thử một
request bất kỳ là chạy được ngay.

Muốn chạy toàn bộ: bấm chuột phải vào Collection → **Run collection** → **Run**. Bộ test chạy từ
trên xuống, tự tạo dữ liệu riêng của nó và tự dọn ở thư mục cuối, nên chạy lại nhiều lần được. Các
bước AI sẽ fail nếu chưa có `GEMINI_API_KEY` — đó là bình thường.

---

## 10. Mỗi lần làm việc sau này

Ba cửa sổ PowerShell:

```powershell
# 1 — backend
cd D:\datn\booking_app
Get-Content .env | Where-Object { $_ -match '^[A-Z]' } | ForEach-Object {
  $pair = $_ -split '=', 2
  [Environment]::SetEnvironmentVariable($pair[0].Trim(), $pair[1].Trim(), 'Process')
}
.\mvnw.cmd spring-boot:run

# 2 — app Android
cd D:\datn\booking_app_demo
flutter run

# 3 — app web
cd D:\datn\booking_app_demo
flutter run -d chrome
```

Lấy mã mới nhất:

```powershell
cd D:\datn\booking_app;      git pull
cd D:\datn\booking_app_demo; git pull; flutter pub get; dart run build_runner build --delete-conflicting-outputs
```

---

## 11. Đọc thêm

| File | Nội dung |
| --- | --- |
| `booking_app/README.md` | Toàn bộ tính năng backend, trong đó có 19 tính năng AI |
| `booking_app/CLAUDE.md` | Kiến trúc chi tiết, dành cho người (hoặc AI) sửa mã |
| `booking_app/DEPLOY_EC2.md` | Triển khai lên máy chủ EC2 |
| `booking_app_demo/README.md` | Cấu trúc app Flutter và các màn hình |
| `booking_app/db/seed_demo.sql` | Dữ liệu mẫu, có ghi chú từng phần |
| `booking_app/tools/check-env.ps1` | Script kiểm tra môi trường, có chế độ cài giúp |
