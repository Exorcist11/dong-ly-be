# Quy Trình Phát Triển Tính Năng Mới (Feature Development Workflow)

Tài liệu này xác định quy trình chuẩn mực từng bước khi phát triển một tính năng mới trong dự án.

---

## 1. Sơ Đồ Quy Trình Tổng Thể

```text
Yêu cầu nghiệp vụ (Requirement)
         ↓
Thấu hiểu bản chất (Understand)
         ↓
Khảo sát hiện trạng repository (Inspect repository)
         ↓
Đọc các quy tắc liên quan (Read relevant rules)
         ↓
Tìm kiếm mã nguồn tái sử dụng (Search reusable code)
         ↓
Thiết kế giải pháp (Design)
         ↓
Lập kế hoạch triển khai (Implementation plan)
         ↓
Viết code tối giản & chính xác (Implement)
         ↓
Chạy kiểm thử tự động thực tế (Test)
         ↓
Tự đánh giá mã nguồn (Self Code Review)
         ↓
Biên dịch & đóng gói kiểm tra (Build)
         ↓
Commit với thông điệp tiếng Việt (Commit)
```

---

## 2. Chi Tiết Từng Bước Thực Thi

### Bước 1: Tiếp Nhận & Thấu Hiểu Yêu Cầu (Understand)
* Đọc kỹ yêu cầu người dùng.
* Xác định rõ: Input là gì? Output là gì? Các điều kiện biên (Edge cases)? Quyền hạn truy cập của ai?
* Nếu có điểm mơ hồ: **Dừng lại hỏi người dùng ngay lập tức, không tự suy đoán**.

### Bước 2: Khảo Sát Repository & Đọc Quy Tắc (Inspect & Read Rules)
* Khảo sát các module hiện có xem tính năng mới nên nằm ở module nào hay tạo module mới.
* Đọc các quy tắc tương ứng:
  * Nếu có API: Đọc [`docs/rules/api.md`](file:///e:/du-an-ma/Spring-BE/docs/rules/api.md), [`docs/rules/dto.md`](file:///e:/du-an-ma/Spring-BE/docs/rules/dto.md), [`docs/rules/validation.md`](file:///e:/du-an-ma/Spring-BE/docs/rules/validation.md).
  * Nếu có Database/Entity: Đọc [`docs/rules/database.md`](file:///e:/du-an-ma/Spring-BE/docs/rules/database.md), [`docs/rules/jpa.md`](file:///e:/du-an-ma/Spring-BE/docs/rules/jpa.md), [`docs/rules/transaction.md`](file:///e:/du-an-ma/Spring-BE/docs/rules/transaction.md).

### Bước 3: Tìm Kiếm Mã Nguồn Tái Sử Dụng (Search Reusable Code)
* Tìm kiếm các tiện ích, exception hoặc pattern sẵn có trong dự án để tái sử dụng.
* Tránh viết lại những gì hệ thống đã có.

### Bước 4: Thiết Kế & Lập Kế Hoạch (Design & Implementation Plan)
* Với các tính năng không tầm thường: Phác thảo danh sách các file cần tạo/sửa đổi.
* Chia nhỏ công việc thành các đơn vị hợp lý nếu tính năng quá lớn.

### Bước 5: Triển Khai Mã Nguồn Tối Giản (Implement)
* Tuân thủ kiến trúc phân lớp: Entity -> Repository -> Service -> Controller.
* Tạo file Flyway migration mới nếu có thay đổi cơ sở dữ liệu (`V...__...sql`).
* Áp dụng nguyên tắc giải pháp nhỏ nhất, chính xác nhất (Smallest correct solution), không thêm thắt tính năng thừa thãi.

### Bước 6: Viết & Chạy Kiểm Thử Thực Tế (Test)
* Bổ sung Unit test cho logic nghiệp vụ trong Service.
* Chạy lệnh kiểm thử trên terminal: `./mvnw test`.
* **Tuyệt đối không báo cáo "Test passed" nếu chưa thực sự chạy lệnh**.

### Bước 7: Tự Đánh Giá Mã Nguồn & Kiểm Tra Diff (Self Review)
* Chạy `git diff` để kiểm tra lại toàn bộ thay đổi.
* Kiểm tra: Có import thừa không? Có code bị comment không? Có lộ mật khẩu hay thông tin nhạy cảm không?

### Bước 8: Biên Dịch Dự Án (Build)
* Chạy lệnh: `./mvnw clean compile` hoặc `./mvnw clean package -DskipTests` để đảm bảo đóng gói hoàn hảo không lỗi cú pháp.

### Bước 9: Tạo Git Commit Tiếng Việt (Commit)
* Tạo commit cục bộ với thông điệp tiếng Việt chuẩn Conventional Commits:
  `feat: thêm chức năng <tên tính năng>`
* **Lưu ý**: DỪNG LẠI tại bước local commit. **TUYỆT ĐỐI KHÔNG CHẠY `git push`**. Để người dùng tự review và push code lên remote.
