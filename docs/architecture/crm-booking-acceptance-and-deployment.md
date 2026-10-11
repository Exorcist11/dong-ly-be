# BÁO CÁO NGHIỆM THU, HƯỚNG DẪN TRIỂN KHAI VÀ KẾ HOẠCH ROLLBACK
## MODULE CRM BOOKING MANAGEMENT - DỰ ÁN VẬN TẢI ĐÔNG LÝ (MỐC 7)

---

## 1. TỔNG QUAN VÀ MỤC TIÊU NGHIỆM THU

Module **CRM Booking Management** đã hoàn thành toàn bộ chuỗi 7 mốc triển khai kỹ thuật:
- **Mốc 1**: Audit codebase FE/BE, lập kế hoạch nghiệp vụ & kiến trúc.
- **Mốc 2**: Backend tìm kiếm chuyến xe khả dụng & sơ đồ ghế thời gian thực.
- **Mốc 3**: Backend cơ chế giữ ghế (Hold 10 phút), database locking, tính giá backend & tạo booking.
- **Mốc 4**: Frontend tìm chuyến & sơ đồ ghế tương tác trực quan.
- **Mốc 5**: Frontend Wizard đặt vé 5 bước tích hợp API thực, timer đếm ngược, validation.
- **Mốc 6**: Giao diện Quản lý danh sách Booking CRM (phân trang, bộ lọc đa tiêu chí, xem chi tiết vé & hủy đơn giải phóng ghế).
- **Mốc 7**: Kiểm thử tích hợp toàn diện (E2E), Concurrency race-condition, nghiệm thu, rà soát bảo mật và lập quy trình triển khai/rollback.

---

## 2. MA TRẬN KIỂM THỬ VÀ KẾT QUẢ THỰC TẾ (TEST VERIFICATION MATRIX)

| STT | Kịch bản kiểm thử (Test Scenario) | Loại kiểm thử | Kết quả thực tế | Trạng thái |
| :--- | :--- | :--- | :--- | :--- |
| **TC-01** | **Chống đặt trùng ghế đồng thời (Race Condition)**: 10 luồng cùng tranh chấp giữ 1 ghế tại cùng 1 microsecond. | Multi-threaded Integration Test | Đúng 1 luồng thành công, 9 luồng bị chặn an toàn. DB chỉ tạo duy nhất 1 booking item. | **PASSED** |
| **TC-02** | **Unique Constraint tầng Database**: Giả lập xung đột ở tầng DB commit. | Unit / Service Test | Bắt `DataIntegrityViolationException` và chuyển đổi thành lỗi thân thiện `SEAT_ALREADY_RESERVED`. | **PASSED** |
| **TC-03** | **Thời hạn giữ ghế 10 phút & Tự động dọn dẹp**: Hold hết hạn trong quá khứ bị từ chối xác nhận. | Integration E2E Test | Ném `SEAT_HOLD_EXPIRED`, tự động chuyển trạng thái Booking & Item sang `EXPIRED` qua transaction độc lập (`REQUIRES_NEW`), giải phóng ghế ngay lập tức. | **PASSED** |
| **TC-04** | **Chống tạo booking trùng khi retry (Idempotency)**: Client bấm xác nhận lại khi đơn đã `CONFIRMED`. | Integration E2E Test | Bị từ chối với mã lỗi `BOOKING_ALREADY_CONFIRMED`, không sinh trùng Payment hay Ticket. | **PASSED** |
| **TC-05** | **Chống giả mạo giá vé (Client Price Tampering)**: Client gửi request thanh toán sai lệch với giá BE tính. | Integration E2E Test | Ném `BusinessRuleException` ("Số tiền không khớp..."), rollback toàn bộ dữ liệu, giữ nguyên trạng thái `HELD`. | **PASSED** |
| **TC-06** | **Kiểm soát quyền sở hữu (Ownership Protection)**: Nhân viên A không thể can thiệp xác nhận đơn do Nhân viên B tạo. | Service Test | Ném lỗi `ACCESS_DENIED`, chỉ người tạo hoặc `ADMIN` mới có quyền xác nhận đơn. | **PASSED** |
| **TC-07** | **Bảo vệ Endpoint bằng RBAC**: Gọi API khi chưa đăng nhập hoặc thiếu authority `BOOKING_READ`/`BOOKING_MANAGE`. | Spring MockMvc Integration Test | Trả về HTTP 403 Forbidden ("Access Denied"). | **PASSED** |
| **TC-08** | **Hủy đơn vé & Giải phóng ghế**: Hủy đơn hợp lệ chuyển trạng thái sang `CANCELLED`, ghế trên chuyến sẵn sàng cho người khác đặt lại. | Integration E2E Test | Booking & BookingItem chuyển sang `CANCELLED`, ghế được giải phóng ngay lập tức trên chuyến xe. Không tự động hoàn tiền. | **PASSED** |
| **TC-09** | **Chặn chuyển đổi trạng thái tùy ý (Invalid State Transitions)**: Chặn hủy đơn đã `COMPLETED` hoặc hủy lại đơn đã `CANCELLED`. | Service Test | Ném `BusinessRuleException` chặn thao tác không hợp lệ. | **PASSED** |
| **TC-10** | **Frontend Service Tests**: Kiểm thử 8 endpoint gọi API từ Client. | Vitest Frontend Tests | 8/8 tests pass, kiểm tra đúng query params, header và body. | **PASSED** |
| **TC-11** | **Frontend Seat Map & UI Components**: Render sơ đồ tầng, chọn/bỏ chọn ghế, đếm ngược hold timer. | Component Vitest Tests | 5/5 tests pass, hiển thị trực quan các trạng thái ghế. | **PASSED** |

### Tổng hợp số lượng test:
- **Backend (Spring Boot)**: **23/23 tests PASSED** (0 failures, 0 errors, 100% pass).
- **Frontend (Angular 21)**: **318/318 tests PASSED** (45 test files, 100% pass). Typecheck sạch (0 errors), Build bundle thành công.

---

## 3. BẢNG PHÂN LOẠI LỖI VÀ TRẠNG THÁI KHẮC PHỤC (DEFECT LOG)

| Mã lỗi | Phân loại | Mô tả lỗi | Nguyên nhân gốc rễ | Biện pháp xử lý | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **DEF-01** | **CRITICAL** | Trạng thái `EXPIRED` không được persist khi `confirmBooking` ném exception hết hạn giữ ghế. | Mặc định Spring `@Transactional` rollback toàn bộ transaction khi ném unchecked exception, làm mất câu lệnh `save(EXPIRED)`. | Áp dụng `TransactionTemplate` với `Propagation.REQUIRES_NEW` để commit cập nhật `EXPIRED` độc lập trước khi ném `SEAT_HOLD_EXPIRED`. | **ĐÃ XỬ LÝ & PASSED** |
| **DEF-02** | **HIGH** | Thiếu `assistantDriver` khi khởi tạo Trip trong môi trường kiểm thử tích hợp E2E. | Schema database quy định `assistant_driver_id` NOT NULL. | Bổ sung tài xế phụ (`driver2`) trong dữ liệu setup test trip. | **ĐÃ XỬ LÝ & PASSED** |
| **DEF-03** | **MEDIUM** | Lỗi TypeScript `ButtonSize` gán nhầm `'sm'` thay vì `'small'` trong cột Actions bảng đơn vé. | Type definition của `ButtonComponent` chuẩn hóa `ButtonSize = 'small' \| 'medium' \| 'large'`. | Sửa thuộc tính `size="small"` trên HTML template. | **ĐÃ XỬ LÝ & PASSED** |
| **DEF-04** | **LOW** | Warning dung lượng CSS vượt budget 8KB (`booking-management-page.component.scss` đạt 11.8KB). | SCSS chứa đầy đủ styles cho cả Wizard 5 bước và bảng quản lý phân trang. | Không ảnh hưởng đến runtime hay tính năng; chấp nhận rủi ro hoặc có thể tách nhỏ SCSS trong đợt refactor UI kế tiếp. | **CHẤP NHẬN RỦI RO (LOW)** |

> [!NOTE]
> Không còn lỗi nào thuộc mức **CRITICAL** hoặc **HIGH** tồn đọng. Hệ thống đáp ứng đầy đủ điều kiện nghiệm thu.

---

## 4. HƯỚNG DẪN CHẠY VÀ KIỂM THỬ (LOCAL RUN & TEST GUIDE)

### 4.1. Chạy Backend (Spring Boot 3.3.4 - Java 21)
```bash
cd Spring-BE

# Chạy toàn bộ automated tests của module Booking
.\mvnw.cmd test "-Dtest=CrmBooking*,Booking*"

# Đóng gói file JAR thực thi
.\mvnw.cmd clean package -DskipTests

# Khởi chạy ứng dụng với profile dev
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```
Backend lắng nghe tại: `http://localhost:8080` (Swagger UI: `http://localhost:8080/swagger-ui/index.html`).

### 4.2. Chạy Frontend (Angular 21 - Node.js 20+)
```bash
cd Angular-FE

# Kiểm tra tính toàn vẹn kiểu dữ liệu
npm run typecheck

# Chạy toàn bộ automated unit tests (vitest)
npm test -- --watch=false

# Đóng gói build production
npm run build

# Khởi chạy server phát triển
npm start
```
Frontend mở tại: `http://localhost:4200` (Đường dẫn module: `/bookings`).

---

## 5. HƯỚNG DẪN TRIỂN KHAI MÔI TRƯỜNG PRODUCTION (DEPLOYMENT RUNBOOK)

### 5.1. Yêu cầu tiên quyết
- **Java Runtime**: OpenJDK 21 (LTS).
- **PostgreSQL**: Phiên bản 15 trở lên.
- **Reverse Proxy**: Nginx hoặc Caddy hỗ trợ SSL termination và HTTP/2.

### 5.2. Biến môi trường bắt buộc (Environment Variables)
```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://<db-host>:5432/dongly_prod
DATABASE_USERNAME=<db-username>
DATABASE_PASSWORD=<db-password>
JWT_SECRET=<chuoi-bi-mat-jwt-ngau-nhien-toi-thieu-256-bit>
APP_CORS_ALLOWED_ORIGINS=https://admin.dongly.vn
PORT=8080
```

### 5.3. Trình tự thực hiện triển khai (Step-by-step Execution)
1. **Sao lưu Database**:
   ```bash
   pg_dump -U dongly_user -h <db-host> -d dongly_prod -Fc -f backup_pre_crm_booking_$(date +%Y%m%d_%H%M%S).dump
   ```
2. **Triển khai Backend**:
   - Copy file `dong-ly-be-0.0.1-SNAPSHOT.jar` lên server.
   - Khi ứng dụng khởi động, Flyway tự động thực thi migration `V8__create_customer_booking_and_ticket_tables.sql` để tạo cấu trúc bảng, constraints và partial unique index `uq_booking_items_active_trip_seat`.
   - Kiểm tra health check: `curl http://localhost:8080/actuator/health`.
3. **Triển khai Frontend**:
   - Đồng bộ thư mục `Angular-FE/dist/dongly-admin/browser` tới thư mục root của web server (vd: `/var/www/dongly-admin`).
   - Cấu hình Nginx rewrite fallback về `index.html` cho SPA routing.
   - Reload Nginx: `sudo systemctl reload nginx`.

---

## 6. CHECKLIST NGHIỆM THU (ACCEPTANCE CHECKLIST)

- [x] **Tìm chuyến & Kiểm tra sơ đồ ghế**: Tìm theo ngày/tuyến/điểm đón trả; sơ đồ ghế phân biệt rõ `AVAILABLE`, `HELD`, `CONFIRMED`, `MAINTENANCE`.
- [x] **Giữ ghế (Hold Seats)**: Khóa tạm thời 10 phút; hiển thị đếm ngược thời gian thực trên FE; chống race-condition bằng DB lock.
- [x] **Xác nhận đặt vé (Confirm)**: Backend tính toán lại giá vé chính xác; xác thực số tiền thanh toán; cấp vé kèm QR code.
- [x] **Hết hạn giữ chỗ**: Chặn xác nhận khi hold hết hạn; tự động dọn dẹp và giải phóng ghế cho khách khác.
- [x] **Chống retry trùng**: Chặn tạo booking/ticket lần 2 khi đơn đã hoàn tất (`BOOKING_ALREADY_CONFIRMED`).
- [x] **Quản lý danh sách đơn**: Phân trang, lọc theo mã vé/khách hàng/trạng thái/khoảng ngày; xem chi tiết đầy đủ.
- [x] **Hủy vé có kiểm soát**: Cho phép hủy đơn hợp lệ kèm ghi nhận lý do hủy; giải phóng ghế ngay lập tức; không tự ý hoàn tiền.
- [x] **Phân quyền RBAC**: `BOOKING_READ` (xem) và `BOOKING_MANAGE` (thao tác giữ, xác nhận, hủy).

---

## 7. KẾ HOẠCH ROLLBACK KHI XẢY RA SỰ CỐ (ROLLBACK PLAN)

Trong trường hợp phát sinh sự cố nghiêm trọng trên môi trường Production:

1. **Rollback Ứng dụng Backend**:
   - Dừng service mới: `sudo systemctl stop dongly-be`.
   - Trỏ symlink JAR về phiên bản trước đó (`dong-ly-be-pre-m7.jar`).
   - Khởi động lại service: `sudo systemctl start dongly-be`.
2. **Rollback Database (Nếu cần khôi phục schema)**:
   ```sql
   -- Xóa các bảng CRM Booking theo đúng thứ tự ràng buộc khóa ngoại:
   DROP TABLE IF EXISTS tickets CASCADE;
   DROP TABLE IF EXISTS payments CASCADE;
   DROP TABLE IF EXISTS booking_items CASCADE;
   DROP TABLE IF EXISTS bookings CASCADE;
   DROP TABLE IF EXISTS customers CASCADE;
   DELETE FROM flyway_schema_history WHERE version = '8';
   ```
   Hoặc khôi phục file dump: `pg_restore -U dongly_user -d dongly_prod -c backup_pre_crm_booking_*.dump`.
3. **Rollback Frontend**:
   - Khôi phục thư mục web server từ backup trước đó: `rsync -avz /var/www/dongly-admin-backup/ /var/www/dongly-admin/`.
   - Xóa cache Cloudflare / CDN nếu có cấu hình.
