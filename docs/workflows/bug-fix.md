# Quy Trình Sửa Lỗi (Bug Fix Workflow)

Tài liệu này xác định quy trình điều tra, tái hiện và khắc phục sự cố, bảo đảm giải quyết triệt để nguyên nhân gốc rễ và không gây lỗi hồi quy (Regression).

---

## 1. Sơ Đồ Quy Trình Sửa Lỗi Chuẩn

```text
Tiếp nhận báo lỗi (Bug Report)
         ↓
Tái hiện lỗi (Reproduce)
         ↓
Điều tra nguyên nhân (Investigate)
         ↓
Truy vết luồng dữ liệu (Trace Data Flow)
         ↓
Xác định nguyên nhân gốc rễ (Find Root Cause)
         ↓
Thiết kế giải pháp sửa tối thiểu (Design Minimal Fix)
         ↓
Triển khai giải pháp (Implement)
         ↓
Kiểm thử hồi quy (Regression Test)
         ↓
Tự đánh giá mã nguồn (Review)
         ↓
Biên dịch kiểm tra (Build)
         ↓
Commit với thông điệp tiếng Việt (Commit)
```

---

## 2. Các Nguyên Tắc Vàng Khi Sửa Lỗi

> [!CAUTION]
> **Tuyệt đối không vá triệu chứng (Do not patch symptoms)**: Việc thêm một lệnh `if (obj != null)` vô căn cứ chỉ để dập tắt `NullPointerException` mà không hiểu vì sao `obj` bị null sẽ tạo ra những lỗi tiềm ẩn nghiêm trọng hơn trong tương lai. Phải tìm và khắc phục tận gốc rễ!

---

## 3. Chi Tiết Các Bước Thực Thi

### Bước 1: Tái Hiện Lỗi (Reproduce)
* Thu thập đủ thông tin: Input gây lỗi, tài khoản thực hiện, thời điểm xảy ra, stack trace trong log.
* Tái hiện lỗi trên môi trường dev hoặc viết một bài Unit Test mô phỏng chính xác trường hợp lỗi (Red Test).

### Bước 2: Truy Vết & Tìm Nguyên Nhân Gốc Rễ (Trace & Root Cause)
* Đọc kỹ log ngoại lệ: Bắt đầu từ dòng `Caused by:` sâu nhất.
* Lần theo luồng dữ liệu từ Controller -> Service -> Repository -> Database để tìm điểm lệch logic so với kỳ vọng.

### Bước 3: Thiết Kế & Triển Khai Giải Pháp Tối Thiểu (Design & Implement)
* Sửa đúng vị trí phát sinh lỗi với mức độ thay đổi nhỏ nhất có thể (Minimal Fix).
* Tránh "tiện tay" tái cấu trúc các vùng code khác không liên quan trong khi đang sửa bug.

### Bước 4: Kiểm Thử Hồi Quy (Regression Test)
* Chạy bài test vừa viết để xác nhận lỗi đã được khắc phục hoàn toàn (Green Test).
* Chạy toàn bộ test suite của module để bảo đảm việc sửa lỗi không làm hỏng các tính năng đang hoạt động bình thường khác (`./mvnw test`).

### Bước 5: Build & Commit Tiếng Việt
* Chạy biên dịch kiểm tra: `./mvnw clean compile`.
* Tạo commit theo định dạng:
  `fix: sửa lỗi <mô tả ngắn gọn về bản chất lỗi>`
  * *Ví dụ*: `fix: sửa lỗi tính sai tổng tiền đơn hàng khi áp dụng voucher giảm giá`
* **Lưu ý**: Chỉ tạo local commit, **KHÔNG CHẠY `git push`** để người dùng tự review và push code.
