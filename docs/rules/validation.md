# Quy Tắc Xác Thực Dữ Liệu (Validation Guidelines)

Tài liệu này xác định chiến lược kiểm tra tính hợp lệ của dữ liệu theo mô hình phòng thủ nhiều lớp (Defense in Depth).

---

## 1. Nguyên Tắc Cốt Lõi: Không Tin Tưởng Dữ Liệu Client

* **Frontend validation chỉ nhằm tối ưu trải nghiệm người dùng (UX)**: Người dùng có thể vô hiệu hóa JavaScript, can thiệp DevTools hoặc dùng Postman/cURL để gửi trực tiếp request độc hại.
* **Backend là chốt chặn phòng thủ bắt buộc**: Mọi dữ liệu đi vào máy chủ đều bị coi là không đáng tin cậy cho đến khi được kiểm chứng.

---

## 2. Mô Hình Xác Thực 3 Lớp (Three-Layer Validation Strategy)

```text
       HTTP Request
            ↓
┌───────────────────────┐
│ 1. Request Validation │  - Kiểm tra cú pháp, định dạng, độ dài, null/blank (@Valid DTO)
└───────────────────────┘  - Thực thi tại tầng Controller bằng Jakarta Bean Validation
            ↓
┌───────────────────────┐
│ 2. Business Validation│  - Kiểm tra trạng thái nghiệp vụ, tồn tại, số dư, quyền hạn
└───────────────────────┘  - Thực thi tại tầng Service bằng code Java tường minh
            ↓
┌───────────────────────┐
│ 3. Database Integrity │  - Ràng buộc toàn vẹn dữ liệu: NOT NULL, UNIQUE, FK, CHECK
└───────────────────────┘  - Chốt chặn cuối cùng tại PostgreSQL Schema
```

---

## 3. Lớp 1: Request Validation (Cấu Trúc & Cú Pháp)

* Đặt các annotation kiểm tra hợp lệ trực tiếp trên các trường của Request DTO:
  * `@NotBlank`: Cho chuỗi văn bản không được rỗng và không chỉ chứa khoảng trắng.
  * `@NotNull`: Cho các đối tượng, số, Enum, UUID, Boolean.
  * `@Email`: Cho định dạng địa chỉ email.
  * `@Size(min = ..., max = ...)`: Cho độ dài chuỗi hoặc kích thước collection.
  * `@Positive` / `@PositiveOrZero`: Cho giá trị số tiền, số lượng.
  * `@Past` / `@Future`: Cho ngày tháng hợp lệ.
* Kích hoạt xác thực tại Controller bằng `@Valid` trước `@RequestBody`:

```java
@PostMapping
public ResponseEntity<ApiResponse<Void>> register(
        @Valid @RequestBody RegisterRequest request) {
    // Chỉ được gọi tới đây nếu request đã vượt qua toàn bộ Bean Validation
}
```

---

## 4. Lớp 2: Business Validation (Quy Tắc Nghiệp Vụ)

* Thao tác kiểm tra nghiệp vụ phải nằm trong **Service layer**, không đặt trong Controller:
  * Kiểm tra email đã được đăng ký trước đó hay chưa.
  * Kiểm tra tài khoản có bị khóa hoặc vô hiệu hóa không.
  * Kiểm tra số lượng tồn kho có đủ để xuất hàng không.
  * Kiểm tra mã giảm giá còn hiệu lực và đúng điều kiện giá trị đơn hàng không.
* **Không tạo Custom Annotation (`@Constraint`) cho logic cần gọi database**:
  * Việc tạo annotation `@UniqueEmail` rồi inject repository vào bên trong validator annotation gây khó khăn cho việc quản lý vòng đời Spring bean và che giấu truy vấn ngầm định. Hãy viết phương thức kiểm tra tường minh trong Service!

```java
@Transactional
public UserResponse register(RegisterRequest request) {
    // Business validation rõ ràng, minh bạch
    if (userRepository.existsByEmail(request.email().toLowerCase().trim())) {
        throw new BusinessRuleException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email đã được sử dụng");
    }
    // Tiến hành tạo user
}
```

---

## 5. Lớp 3: Database Integrity (Ràng Buộc Cơ Sở Dữ Liệu)

Cơ sở dữ liệu là phòng tuyến bảo vệ dữ liệu cuối cùng khi có xung đột đồng thời (Race Condition):
* Cột bắt buộc luôn có `NOT NULL`.
* Cột không được trùng lặp luôn có chỉ mục `UNIQUE` (ví dụ: `users.email`).
* Quan hệ giữa các bảng luôn có ràng buộc khóa ngoại `FOREIGN KEY`.
* Giá trị số không âm có ràng buộc `CHECK (price >= 0)`.

---

## 6. Làm Sạch Dữ Liệu Đầu Vào (Data Sanitization)

Trước khi xử lý hoặc lưu trữ, Service phải chuẩn hóa dữ liệu:
* Cắt tỉa khoảng trắng thừa (`trim()`) ở đầu và cuối chuỗi họ tên, mã định danh.
* Chuyển email về dạng chữ thường hoàn toàn (`toLowerCase()`) để tránh lỗi trùng lặp do phân biệt hoa thường.
* Lọc ký tự nguy hiểm nếu dữ liệu có nguy cơ hiển thị lại trên giao diện web (chống XSS).
