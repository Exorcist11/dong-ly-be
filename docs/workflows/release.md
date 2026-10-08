# Quy Trình Phát Hành & Triển Khai (Release Workflow)

Tài liệu này xác định các bước kiểm tra, đóng gói, di chuyển cơ sở dữ liệu và triển khai ứng dụng lên môi trường Production (Render & Neon).

---

## 1. Sơ Đồ Quy Trình Phát Hành

```text
Kiểm thử tự động toàn diện (Automated Tests)
         ↓
Đóng gói & Biên dịch Production (Production Build)
         ↓
Kiểm tra tính an toàn Migration (Migration Verification)
         ↓
Xác thực Biến môi trường (Environment Verification)
         ↓
Triển khai Container lên Render (Deploy to Render)
         ↓
Kiểm tra Sức khỏe dịch vụ (Health Check: /actuator/health)
         ↓
Kiểm thử nhanh chức năng cốt lõi (Smoke Test)
         ↓
Sẵn sàng kịch bản hoàn tác nếu có sự cố (Rollback Strategy)
```

---

## 2. Chi Tiết Các Bước Chuẩn Bị Phát Hành

### Bước 1: Chạy Kiểm Thử Tự Động Toàn Diện
Chạy toàn bộ test suite trên nhánh phát hành (thường là `main`):
```bash
./mvnw clean verify
```
Tất cả các bài kiểm thử phải đạt kết quả thành công 100%.

### Bước 2: Kiểm Tra File Migration (Flyway)
* Rà soát các tệp migration mới trong `src/main/resources/db/migration/`:
  * Đảm bảo tên file tuân thủ `V...__...sql`.
  * Đảm bảo câu lệnh SQL là **tương thích ngược (Backward-compatible)** (ví dụ: không xóa cột đang có mà phiên bản code cũ vẫn đang đọc).
  * Đã có chỉ mục đầy đủ cho các cột khóa ngoại hoặc cột tìm kiếm mới.

### Bước 3: Xác Thực Cấu Hình Môi Trường Trên Render
Trước khi kích hoạt deploy, đối chiếu các biến môi trường trên Render Dashboard:
* `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` (kết nối Neon Pooler).
* `JWT_SECRET` đã được cấp khóa bí mật sản xuất chưa?
* `APP_CORS_ALLOWED_ORIGINS` đã cập nhật đúng domain Production của frontend chưa?
* `SPRING_PROFILES_ACTIVE=prod`.

### Bước 4: Triển Khai & Kiểm Tra Sau Triển Khai (Post-Deploy)
* Kích hoạt Deploy trên Render (tự động qua Webhook Git hoặc nút Manual Deploy).
* Quan sát Render Log:
  1. Khởi động Docker container thành công.
  2. Flyway hoàn thành việc áp dụng script SQL mà không có lỗi.
  3. Spring Boot log hiển thị: `Started Application in ... seconds`.
* Gọi kiểm tra endpoint sức khỏe:
  ```bash
  curl -f https://<your-render-service>.onrender.com/actuator/health
  ```
  Kết quả phải trả về: `{"status":"UP"}`.

### Bước 5: Kịch Bản Hoàn Tác (Rollback Considerations)
* Nếu bản deploy mới gặp lỗi nghiêm trọng (crash loop, lỗi 500 diện rộng):
  * **Mã nguồn**: Sử dụng nút **Rollback to previous deploy** trên giao diện Render để lập tức khôi phục bản image container chạy ổn định trước đó.
  * **Cơ sở dữ liệu**: Vì các migration được yêu cầu thiết kế tương thích ngược, bản code cũ sẽ vẫn hoạt động bình thường với schema mới mà không cần rollback cơ sở dữ liệu ngay lập tức.
