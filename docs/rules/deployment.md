# Tiêu Chuẩn Triển Khai Hệ Thống (Deployment Guidelines)

Tài liệu này xác định các tiêu chuẩn triển khai ứng dụng Spring Boot lên nền tảng đám mây **Render** kết hợp cơ sở dữ liệu **Neon Serverless PostgreSQL**.

---

## 1. Nguyên Tắc Cốt Lõi: Độc Lập Triển Khai (Zero-Change Deployment)

> **Mã nguồn không bao giờ phải thay đổi để phục vụ việc triển khai.**
> Mọi sai khác giữa môi trường máy tính cá nhân (Local), máy chủ thử nghiệm (Staging) và môi trường sản xuất (Production) phải được điều khiển hoàn toàn thông qua **Biến Môi Trường (Environment Variables)**.

---

## 2. Triển Khai Trên Render (Web Service)

### 2.1. Cấu Hình Cổng Động (Dynamic PORT)
* Nền tảng Render tự động cấp phát một cổng ngẫu nhiên thông qua biến môi trường `PORT`.
* Ứng dụng phải tự động nhận cổng này:
  ```yaml
  server:
    port: ${PORT:8080}
  ```

### 2.2. Kiểm Tra Sức Khỏe (Health Check Endpoint)
* Render sử dụng Health Check để kiểm tra xem bản deploy mới đã sẵn sàng nhận tải chưa trước khi ngắt bản cũ (Zero-Downtime Deployment).
* Endpoint cấu hình trong Render Dashboard:
  `Path: /actuator/health`
* Đảm bảo Spring Boot Actuator đã được cấu hình mở endpoint này không cần xác thực token trong `SecurityConfig`.

### 2.3. Tắt Ứng Dụng Êm Đẹp (Graceful Shutdown)
Khi Render điều phối container hoặc thay thế bản phát hành mới, nó gửi tín hiệu `SIGTERM`:
```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```
* Tính năng này giúp Spring Boot ngừng tiếp nhận request mới nhưng cho phép các HTTP request đang xử lý dở dang có tối đa 30 giây để hoàn thành trước khi tiến trình bị tắt hẳn.

---

## 3. Kết Nối Neon Serverless PostgreSQL

### 3.1. Danh Sách Biến Môi Trường Cần Thiết Trên Render
Khi tạo Web Service trên Render, cấu hình các biến môi trường sau:

| Tên biến môi trường | Giá trị mẫu / Mô tả |
| :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | `jdbc:postgresql://ep-xyz-pooler.us-east-2.aws.neon.tech/neondb?sslmode=require` |
| `DATABASE_USERNAME` | Tên tài khoản Neon cấp |
| `DATABASE_PASSWORD` | Mật khẩu tài khoản Neon cấp |
| `JWT_SECRET` | Chuỗi ngẫu nhiên tối thiểu 256 bits (Base64) |
| `APP_CORS_ALLOWED_ORIGINS` | Domain frontend chính thức (ví dụ: `https://myfrontend.com`) |

### 3.2. Chế Độ Connection Pooling (PgBouncer)
* **Bắt buộc**: Sử dụng chuỗi kết nối Pooled của Neon (có chứa tiền tố `-pooler` trong hostname) để kết nối qua PgBouncer.
* Ngăn chặn tình trạng ứng dụng tạo quá nhiều kết nối vượt quá ngưỡng tính phí hoặc giới hạn của Neon Serverless.

---

## 4. Tự Động Di Chuyển Lược Đồ (Database Migrations)

* Khi container khởi động trên Render, Spring Boot kết hợp với Flyway sẽ tự động chạy toàn bộ các file SQL migration mới chưa được áp dụng trong `src/main/resources/db/migration/`.
* Quá trình này được thực thi an toàn trong cùng một giao dịch DDL của PostgreSQL trước khi ứng dụng chính thức mở cổng đón nhận HTTP request.
