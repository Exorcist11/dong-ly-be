# Quy Tắc Phát Triển Spring Boot (Spring Boot Guidelines)

Tài liệu này xác định các tiêu chuẩn kiến trúc và nguyên tắc lập trình khi làm việc với Spring Boot 3.x trong dự án.

---

## 1. Tầng Controller (REST Controllers)

Tầng Controller là cửa ngõ tiếp nhận các yêu cầu HTTP. Controller phải **cực kỳ mỏng (Thin Controller)**.

### Trách nhiệm của Controller:
1. Tiếp nhận HTTP Request (Path variable, Query param, Request Body, Header).
2. Kích hoạt xác thực dữ liệu đầu vào bằng `@Valid` hoặc `@Validated`.
3. Chuyển tiếp dữ liệu vào Service layer thích hợp.
4. Nhận kết quả từ Service và gói thành HTTP Response với HTTP Status code chính xác.

### Những điều CẤM trong Controller:
* ❌ **CẤM** chứa logic nghiệp vụ, tính toán, kiểm tra điều kiện nghiệp vụ phức tạp.
* ❌ **CẤM** inject hoặc gọi trực tiếp `Repository`. Mọi truy xuất dữ liệu phải qua `Service`.
* ❌ **CẤM** nhận trực tiếp hoặc trả về JPA Entity cho client.
* ❌ **CẤM** bao bọc logic trong khối `try-catch` cục bộ để bắt exception trả về mã lỗi thủ công (phải để `GlobalExceptionHandler` xử lý tập trung).

```java
// ✅ VÍ DỤ CONTROLLER CHUẨN:
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo người dùng thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

---

## 2. Tầng Service (Business Logic Layer)

Tầng Service nắm giữ toàn bộ sức mạnh và quy tắc nghiệp vụ của hệ thống.

### Trách nhiệm của Service:
1. Thực thi các quy tắc kiểm tra nghiệp vụ (Business validation).
2. Quản lý ranh giới giao dịch cơ sở dữ liệu (`@Transactional`).
3. Điều phối dữ liệu giữa các Repository và các Module Service khác.
4. Chuyển đổi dữ liệu (Mapping) giữa Request DTO -> Entity và Entity -> Response DTO.
5. Kích hoạt Domain Events nếu có hành động phụ thuộc.
6. Ném ra các ngoại lệ nghiệp vụ cụ thể (Domain Exceptions) khi quy tắc bị vi phạm.

### Nguyên tắc về Service Interface:
* **Không tạo interface dư thừa**: Nếu một Service chỉ có 1 lớp cài đặt duy nhất, khai báo trực tiếp class `@Service public class UserService`. Không tạo cặp `UserService` interface + `UserServiceImpl` class một cách máy móc.

---

## 3. Tầng Repository (Data Access Layer)

* Sử dụng `JpaRepository<Entity, ID>` của Spring Data JPA.
* Chỉ chịu trách nhiệm truy vấn, cập nhật và xóa dữ liệu.
* Đặt tên phương thức theo chuẩn Query Method của Spring Data (`findByEmail`, `existsByUsername`, `findAllByStatus`).
* Với truy vấn phức tạp hoặc cần tối ưu hiệu năng: Dùng `@Query` với JPQL tường minh hoặc Projection interface/record. Không lạm dụng Native Query trừ khi cần tận dụng tính năng chuyên biệt của PostgreSQL.

---

## 4. Quản Lý Cấu Hình (Configuration & Properties)

* **Ưu tiên `@ConfigurationProperties` có kiểu rõ ràng (Type-safe)** thay vì rải rác `@Value("${app.jwt.secret}")` ở nhiều class.
* Bật `@Validated` trên cấu hình để phát hiện thiếu biến môi trường ngay khi khởi động ứng dụng:

```java
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public record JwtProperties(
    @NotBlank String secret,
    @Positive long accessTokenExpirationMs,
    @Positive long refreshTokenExpirationMs
) {}
```

* Kích hoạt trong Spring Boot bằng `@EnableConfigurationProperties(JwtProperties.class)`.

---

## 5. Scope Của Spring Bean & Quản Lý Trạng Thái (Bean Scopes)

* Mặc định mọi `@Component`, `@Service`, `@Repository`, `@RestController` đều là **Singleton scope**.
* Một Singleton Bean được dùng chung bởi mọi luồng xử lý HTTP request:
  * **TUYỆT ĐỐI KHÔNG** lưu trữ trạng thái người dùng (user context, request-scoped data) trong biến instance của Singleton Bean.
  * Sử dụng `SecurityContextHolder` để lấy thông tin người dùng hiện tại an toàn theo từng luồng (ThreadLocal).
