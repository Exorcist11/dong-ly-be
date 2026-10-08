# Quy Chuẩn Đối Tượng Dữ Liệu (DTO Guidelines)

Tài liệu này xác định các quy tắc quản lý và chuyển đổi Data Transfer Object (DTO), bảo đảm sự phân tách rõ ràng giữa lớp dữ liệu lưu trữ (Entity) và lớp giao tiếp mạng (API Contract).

---

## 1. Phân Tách 3 Tầng Dữ Liệu Rõ Ràng

Hệ thống phân định 3 vai trò dữ liệu hoàn toàn tách biệt:

```text
[ Client ]
    ↕ (JSON)
[ Request DTO / Response DTO ]  ← Lớp giao tiếp API
    ↕ (Mapper)
[ JPA Entity ]                  ← Lớp mô hình cơ sở dữ liệu
    ↕ (JDBC/Hibernate)
[ PostgreSQL Database ]
```

### ❌ TUYỆT ĐỐI CẤM phơi bày trực tiếp JPA Entity ra ngoài REST API:
1. **Lộ thông tin nhạy cảm**: Vô tình làm lộ `passwordHash`, thông tin kiểm toán nội bộ, cờ `isDeleted`.
2. **Lỗi tuần hoàn JSON (Infinite recursion)**: Xảy ra khi tuần tự hóa các quan hệ 2 chiều (`@OneToMany`, `@ManyToOne`).
3. **Lỗi `LazyInitializationException`**: Xảy ra khi Jackson cố gắng truy cập getter của thuộc tính Lazy ngoài ranh giới `@Transactional`.
4. **Lỗ hổng gán hàng loạt (Mass Assignment)**: Kẻ tấn công có thể gửi thêm trường `isAdmin: true` hoặc `balance: 999999` để chiếm đoạt tài nguyên nếu controller bind trực tiếp vào Entity.

---

## 2. Quy Ước Đặt Tên DTO Tường Minh

Đặt tên DTO phản ánh chính xác hành vi và ngữ cảnh nghiệp vụ:

### 2.1. Request DTOs
* Dùng hậu tố `Request`:
  * Tạo mới: `Create<Entity>Request` (ví dụ: `CreateUserRequest`, `CreateProductRequest`)
  * Cập nhật: `Update<Entity>Request` (ví dụ: `UpdateUserRequest`)
  * Tác vụ chuyên biệt: `ChangePasswordRequest`, `LoginRequest`, `CheckoutRequest`
* *Tránh*: `UserDTO`, `UserData`, `RequestObj`.

### 2.2. Response DTOs
* Dùng hậu tố `Response`:
  * Danh sách rút gọn: `<Entity>SummaryResponse` (chứa ít trường để tải nhanh danh sách)
  * Chi tiết đầy đủ: `<Entity>DetailResponse` hoặc `<Entity>Response`
  * Dữ liệu phân quyền: `UserProfileResponse`, `AuthTokenResponse`

---

## 3. Khai Báo DTO Bằng Java Record

* Ưu tiên tuyệt đối khai báo DTO bằng **Java Record**:
  * Đảm bảo tính bất biến (Immutability).
  * Cú pháp cô đọng, loại bỏ mã thừa.
  * Hỗ trợ đầy đủ các annotation của Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, ...).

```java
public record CreateUserRequest(
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự")
    String fullName,

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    String email,

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự")
    String password
) {}
```

---

## 4. Chiến Lược Chuyển Đổi Dữ Liệu (Mapping Strategy)

### 4.1. Khuyến Nghị Sử Dụng: Mapper Thủ Công Tường Minh Hoặc MapStruct
* **Cách 1: Mapper thủ công (Explicit Static Mapper)** - Khuyên dùng cho hầu hết các module vì đơn giản, an toàn kiểu dữ liệu compile-time và không cần thêm dependency:

```java
public final class UserMapper {
    private UserMapper() {}

    public static User toEntity(CreateUserRequest request, String passwordHash) {
        return User.builder()
                .fullName(request.fullName())
                .email(request.email().toLowerCase().trim())
                .passwordHash(passwordHash)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public static UserResponse toResponse(User entity) {
        return new UserResponse(
                entity.getId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
```

* **Cách 2: MapStruct** - Cho phép dùng nếu dự án có số lượng trường mapping lớn, đảm bảo code generation an toàn tại thời điểm biên dịch.

### 4.2. ❌ CẤM Sử Dụng `BeanUtils.copyProperties` Hoặc `ModelMapper`
* Sử dụng reflection ngầm định, chậm hiệu năng.
* Không có kiểm tra kiểu an toàn tại thời điểm biên dịch (Compile-time type checking).
* Khi đổi tên trường hoặc xóa trường trong DTO/Entity, các công cụ này âm thầm bỏ qua mà không báo lỗi, dẫn đến việc dữ liệu bị gán `null` ngoài ý muốn.
