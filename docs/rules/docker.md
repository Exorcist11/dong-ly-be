# Quy Chuẩn Đóng Gói Docker (Docker Guidelines)

Tài liệu này xác định các tiêu chuẩn xây dựng tệp `Dockerfile` đa tầng (Multi-stage build), tối ưu hóa kích thước image, an toàn container và cấu hình JVM trong môi trường container.

---

## 1. Mẫu Dockerfile Đa Tầng Chuẩn (Multi-Stage Build)

Bắt buộc sử dụng Multi-stage build để tách biệt hoàn toàn môi trường biên dịch (chứa Maven, JDK nặng) và môi trường thực thi (chỉ chứa JRE tối giản):

```dockerfile
# ==========================================
# GIAI ĐOẠN 1: Biên dịch mã nguồn (Build Stage)
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Sao chép file cấu hình phụ thuộc trước để tận dụng Docker layer cache
COPY pom.xml mvnw ./
COPY .mvn/ .mvn/

# Tải trước các dependencies
RUN ./mvnw dependency:go-offline -B

# Sao chép toàn bộ mã nguồn và đóng gói
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Giải nén JAR để tận dụng Spring Boot Layered Tools (Khởi động cực nhanh)
RUN java -Djarmode=layertools -jar target/*.jar extract

# ==========================================
# GIAI ĐOẠN 2: Môi trường chạy thực tế (Runtime Stage)
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner

# Tạo người dùng phi đặc quyền (Non-root user)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Sao chép các layer Spring Boot đã tách biệt
COPY --from=builder /build/dependencies/ ./
COPY --from=builder /build/spring-boot-loader/ ./
COPY --from=builder /build/snapshot-dependencies/ ./
COPY --from=builder /build/application/ ./

# Phân quyền cho người dùng phi đặc quyền
RUN chown -R appuser:appgroup /app

# Chuyển sang chạy dưới quyền non-root
USER appuser

# Cổng ứng dụng (Render sẽ ghi đè biến PORT nếu cần)
EXPOSE 8080

# Cấu hình tối ưu JVM trong container
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

---

## 2. Tiêu Chuẩn Bảo Mật Container

1. **Chạy dưới quyền Non-Root**:
   * Tuyệt đối không chạy container dưới tài khoản `root`. Nếu container bị xâm nhập, kẻ tấn công sẽ không thể leo thang đặc quyền ra máy chủ vật lý.
2. **Tuyệt đối không nhúng Secret vào Docker Image**:
   * Không dùng lệnh `COPY .env .env` hoặc nhúng mật khẩu vào lệnh `ENV` trong Dockerfile.
   * Toàn bộ giá trị nhạy cảm phải được truyền động khi khởi chạy container (qua cờ `-e` hoặc cấu hình Environment trên Render).
3. **Sử dụng Base Image Tối Giản (Alpine / Distroless)**:
   * Giảm thiểu diện tích bề mặt tấn công và dung lượng image (dưới 200MB).

---

## 3. Cấu Hình Bộ Nhớ JVM Trong Container

* Sử dụng `-XX:+UseContainerSupport` (đã bật mặc định từ Java 11+) để JVM tự động nhận diện giới hạn RAM của container thay vì đọc RAM của máy chủ vật lý.
* Thiết lập `-XX:MaxRAMPercentage=75.0`: Cho phép JVM heap dùng tối đa 75% RAM được cấp của container, 25% còn lại dành cho Metaspace, luồng và hệ điều hành Alpine.
* Bật `-XX:+ExitOnOutOfMemoryError` để container tự kết thúc ngay khi bị OOM, kích hoạt cơ chế tự phục hồi (restart) của Render hoặc Kubernetes.
