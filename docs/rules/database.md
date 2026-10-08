# Quy Tắc Cơ Sở Dữ Liệu & Di Chuyển Lược Đồ (Database Guidelines)

Tài liệu này xác định các tiêu chuẩn thiết kế cơ sở dữ liệu PostgreSQL, quy trình di chuyển lược đồ (Migration) bằng Flyway và các lưu ý đặc thù khi vận hành trên **Neon Serverless PostgreSQL**.

---

## 1. Nền Tảng Cơ Sở Dữ Liệu

* **Môi trường phát triển cục bộ**: PostgreSQL 16+.
* **Môi trường Production**: **Neon Serverless PostgreSQL**.
  * Lưu ý Neon: Tận dụng cơ chế Pooled Connection URL (PgBouncer) để tiết kiệm số lượng kết nối đồng thời và giảm độ trễ khi khởi động lại (scale-to-zero).

---

## 2. Tiêu Chuẩn Thiết Kế Lược Đồ (Schema Design)

### 2.1. Quy Ước Đặt Tên (Naming Conventions)
* **Tên bảng**: `snake_case`, danh từ số nhiều: `users`, `orders`, `order_items`, `user_tokens`.
* **Tên cột**: `snake_case`, danh từ rõ nghĩa: `id`, `full_name`, `email`, `created_at`, `is_active`.
* **Khóa chính**: Luôn đặt tên cột là `id`.
* **Khóa ngoại**: `<tên_bảng_số_ít>_id` (ví dụ: `user_id`, `category_id`).
* **Tên chỉ mục (Index)**: `idx_<tên_bảng>_<tên_cột>` (ví dụ: `idx_users_email`).
* **Tên ràng buộc duy nhất (Unique Constraint)**: `uq_<tên_bảng>_<tên_cột>`.
* **Tên ràng buộc khóa ngoại**: `fk_<bảng_chứa_fk>_<bảng_được_trỏ>`.

### 2.2. Kiểu Dữ Liệu Khóa Chính (Primary Keys)
* **Khuyến nghị sử dụng UUID v7 hoặc UUID v4**:
  * Kiểu dữ liệu PostgreSQL: `UUID`.
  * Giá trị mặc định: `DEFAULT gen_random_uuid()`.
  * *Lý do*: Không làm lộ số lượng bản ghi thực tế ra ngoài API công khai, an toàn trước tấn công duyệt số tự tăng (ID Enumeration).
* Với các bảng nhật ký (Audit Log) ghi tuần tự cực lớn: Có thể dùng `BIGSERIAL` / `BIGINT GENERATED ALWAYS AS IDENTITY`.

### 2.3. Cột Kiểm Toán Bắt Buộc (Audit Columns)
Mọi bảng nghiệp vụ cốt lõi bắt buộc phải có hai cột:
```sql
created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
```

### 2.4. Khóa Ngoại & Chỉ Mục (Foreign Keys & Indexes)
* Mọi quan hệ giữa các bảng phải khai báo ràng buộc `FOREIGN KEY` tường minh.
* **BẮT BUỘC** tạo Index trên các cột khóa ngoại. PostgreSQL không tự động đánh index trên cột khóa ngoại; việc thiếu index sẽ gây quét toàn bộ bảng (Seq Scan) khi JOIN hoặc khi xóa bản ghi cha.

---

## 3. Quản Lý Di Chuyển Lược Đồ Với Flyway (Schema Migration)

Mọi thay đổi cấu trúc bảng, kiểu dữ liệu, index đều phải được quản lý bằng script di chuyển trong thư mục:
`src/main/resources/db/migration/`

### 3.1. Quy Ước Đặt Tên Tệp Flyway
* Định dạng: `V<Phiên_bản>__<Mô_tả_ngắn_gọn>.sql`
  * Hai dấu gạch dưới (`__`) phân tách giữa phiên bản và mô tả.
  * Phiên bản tăng dần theo số nguyên: `V1__init_users_table.sql`, `V2__add_phone_to_users.sql`, `V3__create_orders_table.sql`.
* **Quy tắc bất di bất dịch**: Một file migration đã được áp dụng (đã commit và chạy trên dev/staging/prod) **TUYỆT ĐỐI KHÔNG ĐƯỢC PHÉP CHỈNH SỬA HOẶC XÓA**. Mọi thay đổi tiếp theo phải tạo một file migration mới (`V...__...sql`).

### 3.2. Cấm Sử Dụng Hibernate DDL Auto Trên Production
* **TUYỆT ĐỐI CẤM** thiết lập:
  * `spring.jpa.hibernate.ddl-auto=create`
  * `spring.jpa.hibernate.ddl-auto=create-drop`
  * `spring.jpa.hibernate.ddl-auto=update`
* **Trên môi trường Production**: Thiết lập bắt buộc là:
  ```yaml
  spring:
    jpa:
      hibernate:
        ddl-auto: validate
  ```
  Lược đồ cơ sở dữ liệu chỉ được thay đổi duy nhất thông qua các script Flyway đã được kiểm duyệt.

---

## 4. Tối Ưu Kết Nối Với Neon PostgreSQL

* **Sử dụng Pooled Connection**: Khi triển khai trên Render kết nối tới Neon, sử dụng chuỗi kết nối chứa `-pooler` trong host của Neon (cổng PgBouncer 5432 hoặc 6543) để tránh quá tải kết nối.
* **Cấu hình HikariCP**:
  * Giới hạn `maximum-pool-size: 10` trên các container nhỏ của Render để không làm cạn kiệt tài nguyên kết nối của Neon.
  * Thiết lập `connection-timeout: 30000` (30 giây) và `keepalive-time: 30000` để giữ kết nối ổn định qua hạ tầng serverless.
