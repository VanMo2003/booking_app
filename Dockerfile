# --- Giai đoạn 1: Build bằng Maven & JDK 21 ---
# Quan trọng: Phải dùng image có tag '21' để hỗ trợ Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml trước để cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code và build
COPY src ./src
RUN mvn clean package -DskipTests

# --- Giai đoạn 2: Chạy ứng dụng bằng JRE 21 ---
# Quan trọng: Môi trường chạy cũng phải là Java 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cài đặt múi giờ Việt Nam
RUN apk add --no-cache tzdata
ENV TZ=Asia/Ho_Chi_Minh

# Tạo thư mục uploads (để tránh lỗi nếu code tự động lưu file)
RUN mkdir -p uploads

# Copy toàn bộ ảnh từ máy local vào thư mục uploads trong container
COPY uploads ./uploads

# Copy file .jar từ giai đoạn build
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]