# Chiến Lược Kiểm Thử Tự Động (Testing Guidelines)

Tài liệu này xác định các tiêu chuẩn viết và thực thi kiểm thử tự động, bảo đảm mọi tính năng đưa vào hệ thống đều hoạt động chính xác và có thể tự động hồi quy an toàn.

---

## 1. Nguyên Tắc Cốt Lõi Về Kiểm Thử

> [!IMPORTANT]
> **Một tính năng KHÔNG ĐƯỢC COI LÀ HOÀN THÀNH chỉ vì nó biên dịch thành công.**
> AI Agent hoặc kỹ sư **TUYỆT ĐỐI KHÔNG ĐƯỢC TUYÊN BỐ "TEST PASSED"** nếu chưa thực sự chạy lệnh thực thi kiểm thử trong terminal.

* **Cấm bỏ qua test**: Tuyệt đối không dùng cờ `-DskipTests` hoặc `-Dmaven.test.skip=true` để lươn lẹo đưa mã nguồn lỗi vượt qua quy trình build.

---

## 2. Kim Tự Tháp Kiểm Thử (Testing Pyramid)

```text
         / \
        / API \        - MockMvc / End-to-End Tests (Endpoints quan trọng)
       /───────\
      /  Integ  \      - Spring Data JPA, Repository, Testcontainers PostgreSQL
     /───────────\
    /  Unit Test  \    - Service, Mapper, Business Rules (Nhanh, số lượng lớn nhất)
   /───────────────\
```

---

## 3. Cấu Trúc Và Tiêu Chuẩn Viết Test

### 3.1. Quy Ước Đặt Tên Test (Given - When - Then)
Đặt tên hàm test theo cú pháp rõ nghĩa: `<hành_động>_<điều_kiện>_<kết_quả_kỳ_vọng>`:
* `createUser_withValidRequest_returnsUserResponse()`
* `createUser_withExistingEmail_throwsBusinessRuleException()`
* `calculateDiscount_withExpiredCoupon_returnsOriginalPrice()`

### 3.2. Cấu Trúc AAA (Arrange - Act - Assert)
Mỗi phương thức test phải phân định rõ ràng 3 giai đoạn:
```java
@Test
void createUser_withValidRequest_returnsUserResponse() {
    // 1. Arrange (Chuẩn bị dữ liệu và mock)
    CreateUserRequest request = new CreateUserRequest("Nguyen Van A", "a@example.com", "Password@123");
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
    when(userRepository.save(any(User.class))).thenAnswer(i -> {
        User u = i.getArgument(0);
        u.setId(UUID.randomUUID());
        return u;
    });

    // 2. Act (Thực thi hàm cần kiểm thử)
    UserResponse response = userService.createUser(request);

    // 3. Assert (Kiểm chứng kết quả)
    assertThat(response).isNotNull();
    assertThat(response.email()).isEqualTo("a@example.com");
    verify(userRepository, times(1)).save(any(User.class));
}
```

---

## 4. Các Loại Kiểm Thử Cụ Thể

### 4.1. Unit Test (Tầng Service)
* Sử dụng JUnit 5 (`@ExtendWith(MockitoExtension.class)`) và Mockito.
* **Không nạp Spring ApplicationContext**: Chạy trực tiếp trong JVM, tốc độ tính bằng mili-giây.
* Mock toàn bộ các dependencies bên ngoài (Repositories, External APIs).

### 4.2. Repository Test & Cơ Sở Dữ Liệu Thật (Testcontainers)
* **Khuyến khích Testcontainers PostgreSQL**:
  * Tránh dùng H2 Database cho Integration Test của PostgreSQL vì có sự khác biệt lớn về cú pháp SQL, hàm JSONB, UUID và kiểu dữ liệu.
  * Testcontainers khởi chạy một container Docker PostgreSQL tạm thời giống hệt môi trường Production, bảo đảm tính xác thực 100%.

### 4.3. API Test (Tầng Controller)
* Sử dụng `@WebMvcTest(UserController.class)` kết hợp `MockMvc`.
* Kiểm tra:
  * Mã HTTP Status (200, 201, 400, 404).
  * Kích hoạt Bean Validation khi request body thiếu trường.
  * Cấu trúc JSON trả về (`jsonPath("$.success").value(true)`).

---

## 5. Lệnh Chạy Kiểm Thử Bắt Buộc

Khi kiểm tra mã nguồn trên terminal:
```bash
# Chạy toàn bộ unit test
./mvnw test

# Chạy một lớp test cụ thể
./mvnw test -Dtest=UserServiceTest

# Chạy build kiểm tra toàn diện
./mvnw clean verify
```
