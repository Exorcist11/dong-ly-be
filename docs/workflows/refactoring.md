# Quy Trình Tái Cấu Trúc Mã Nguồn (Refactoring Workflow)

Tài liệu này xác định quy trình cải tiến chất lượng và cấu trúc mã nguồn mà **không làm thay đổi hành vi bên ngoài của hệ thống**.

---

## 1. Nguyên Tắc Vàng Khi Tái Cấu Trúc

> [!WARNING]
> **Tuyệt đối không gộp việc tái cấu trúc lớn (Large refactor) vào cùng một commit phát triển tính năng mới hoặc sửa bug.**
> Refactoring phải là một hành động độc lập, có thể theo dõi và kiểm chứng riêng biệt.

---

## 2. Quy Trình 6 Bước Chuẩn Mực

```text
1. Nhận diện vấn đề cụ thể (Code Smell / Bottleneck)
    ↓
2. Giải thích lý do vì sao cần tái cấu trúc (Justification)
    ↓
3. Xác nhận đã có kiểm thử tự động bảo vệ (Ensure Safety Net)
    ↓
4. Xác định các module và thành phần bị ảnh hưởng (Impact Analysis)
    ↓
5. Tái cấu trúc từng bước nhỏ an toàn (Incremental Changes)
    ↓
6. Chạy toàn bộ kiểm thử hồi quy & Commit (Verify & Commit)
```

---

## 3. Chi Tiết Các Bước Thực Hiện

### Bước 1: Nhận Diện Vấn Đề (Identify Problem)
* Phương thức quá dài, lớp quá nhiều trách nhiệm (God class).
* Trùng lặp logic rõ ràng (vi phạm DRY sau Quy tắc số ba).
* Tên biến, tên hàm khó hiểu, gây nhầm lẫn.
* Câu truy vấn JPA gây N+1 làm giảm hiệu năng.

### Bước 2: Bảo Đảm Có "Lưới An Toàn" (Safety Net)
* **Trước khi sửa đổi**: Phải có sẵn các bài Unit Test hoặc Integration Test đang chạy thành công (Green) bao phủ vùng mã nguồn đó.
* Nếu chưa có test: **Phải viết test bao phủ hành vi hiện tại trước**, chạy pass rồi mới bắt đầu refactor.

### Bước 3: Tái Cấu Trúc Từng Bước Nhỏ (Incremental Refactoring)
* Áp dụng các kỹ thuật refactoring cơ bản:
  * Trích xuất phương thức (Extract Method).
  * Đổi tên biến/hàm rõ nghĩa (Rename Variable/Method).
  * Di chuyển phương thức sang đúng lớp sở hữu dữ liệu (Move Method).
  * Chuyển đổi vòng lặp phức tạp sang Stream API rõ nghĩa (hoặc ngược lại nếu Stream quá rối).
* Sau mỗi thay đổi nhỏ, chạy lại test ngay để phát hiện lỗi tức thời.

### Bước 4: Kiểm Thử Toàn Diện & Tạo Commit
* Chạy toàn bộ test suite của ứng dụng: `./mvnw test`.
* Biên dịch dự án: `./mvnw clean compile`.
* Tạo commit với thông điệp tiếng Việt:
  `refactor: <mô tả sự cải tiến cấu trúc>`
  * *Ví dụ*: `refactor: tách logic tính phí giao hàng ra khỏi OrderService`
