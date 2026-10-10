# Quy Tắc Bảo Mật Hệ Thống (Security Guidelines)

Tài liệu này xác định các tiêu chuẩn bảo mật bắt buộc đối với hệ thống Backend Spring Boot, bảo đảm an toàn dữ liệu, chống rò rỉ bí mật và kiểm soát truy cập nghiêm ngặt theo mô hình RBAC Đông Lý.

---

## 1. Nguyên Tắc Vàng Về Quyền Hạn (Authoritative Backend)

> **Backend là cơ quan thẩm quyền tối cao**.
> Tuyệt đối không bao giờ tin tưởng quyền hạn hoặc định danh mà client gửi lên trong Request Body. Mọi quyết định cho phép hay từ chối truy cập tài nguyên đều phải được kiểm tra độc lập tại Backend.

---

## 2. Cấu Hình Spring Security & Cơ Chế Hoạt Động Của `@PreAuthorize`

### 2.1. Cấu hình Stateless Security
Hệ thống REST API hoạt động theo mô hình phi trạng thái (**Stateless Session**):
* CSRF bị vô hiệu hóa vì hệ thống dùng JWT Bearer Token.
* `SessionCreationPolicy.STATELESS` đảm bảo không lưu session state trên server.
* Bật `@EnableMethodSecurity` để kích hoạt kiểm tra quyền hạn cấp độ phương thức thông qua `@PreAuthorize`.

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/actuator/health", "/docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

### 2.2. Cách `@PreAuthorize` Hoạt Động & Cơ Chế Mapping Quyền
* Trong `JwtAuthenticationFilter`, khi giải mã JWT hợp lệ:
  * Mỗi vai trò trong claim `roles` (ví dụ `ADMIN`) được chuyển thành authority dạng `ROLE_ADMIN`.
  * Mỗi quyền hạn trong claim `permissions` (ví dụ `USER_READ`, `ROLE_ASSIGN`) được nạp trực tiếp thành `GrantedAuthority` nguyên bản (`USER_READ`, `ROLE_ASSIGN`).
* Do đó, `@PreAuthorize` hỗ trợ:
  * `hasAuthority('PERMISSION_CODE')`: Kiểm tra granular permission cụ thể (khuyến nghị cho mọi endpoint nghiệp vụ).
  * `hasRole('ADMIN')`: Kiểm tra role với tiền tố `ROLE_` ngầm định.
  * Biểu thức logic kết hợp: `@PreAuthorize("hasAuthority('ROLE_ASSIGN') or hasRole('ADMIN')")`.

---

## 3. Quản Lý Phiên Làm Việc (JWT, Refresh Token & Giới Hạn Thu Hồi Quyền)

### 3.1. Phân Tách Token
* **Access Token (Stateless JWT)**:
  * Thời hạn sống: 30 phút (`1800` giây).
  * Chứa claims: `sub` (email), `uid` (user ID), `username`, `roles` (mảng mã vai trò), `permissions` (mảng mã quyền hạn hữu hiệu).
  * Được ký bằng HMAC-SHA256 với secret key tối thiểu 256 bits.
* **Refresh Token (Stateful & Database-backed)**:
  * Chuỗi ngẫu nhiên cryptographically secure 64 ký tự.
  * Chỉ lưu bản băm `SHA-256` (`token_hash`) trong database.
  * Hỗ trợ xoay vòng (Token Rotation) và phát hiện tấn công tái sử dụng (Replay Attack Detection).

### 3.2. Cửa Sổ Hiệu Lực Của Stateless Token & Giới Hạn Thu Hồi (Revocation Boundary)
* **Giới hạn kỹ thuật của Stateless Token**:
  * Khi quyền hạn của người dùng bị thay đổi hoặc tài khoản bị khóa, Access Token đã cấp phát **vẫn có hiệu lực cho đến khi hết hạn (tối đa 30 phút)**, vì hệ thống không truy vấn database trong từng request để tối ưu hiệu năng.
* **Biện pháp đối ứng đã triển khai**:
  * Ngay khi tài khoản bị khóa/vô hiệu hóa (`PATCH /api/v1/users/{id}/status`) hoặc thay đổi vai trò (`PUT /api/v1/users/{id}/roles`), hệ thống **lập tức thu hồi toàn bộ Refresh Token** của người dùng (`refreshTokenRepository.revokeAllActiveTokensByUserId`).
  * Người dùng bị ảnh hưởng sẽ không thể refresh token và bắt buộc phải đăng nhập lại sau khi access token hiện tại hết hạn.

---

## 4. Chống Leo Thang Quyền Hạn (Privilege Escalation) & Bảo Vệ Tài Khoản Quản Trị

1. **Ngăn người dùng tự nâng quyền hoặc gán quyền vượt cấp**:
   * Chỉ tài khoản có quyền `ROLE_ASSIGN` hoặc sở hữu vai trò `ADMIN` mới được phép gán vai trò (`PUT /api/v1/users/{id}/roles`).
   * Khi tạo mới người dùng (`POST /api/v1/users`) hoặc cập nhật người dùng (`PUT /api/v1/users/{id}`), nếu danh sách `roleCodes` chứa vai trò `ADMIN`, bắt buộc người thực hiện phải là `ADMIN`. Tài khoản quản trị cấp thấp (như `OPERATOR` có `USER_CREATE`/`USER_UPDATE`) bị từ chối với HTTP 403 `ACCESS_DENIED`.
2. **Chống can thiệp tài khoản Quản trị viên (Admin Tampering Protection)**:
   * Tài khoản có quyền `USER_UPDATE` nhưng không phải `ADMIN` bị nghiêm cấm:
     * Chỉnh sửa thông tin hồ sơ (email, tên, SĐT) của bất kỳ tài khoản nào mang vai trò `ADMIN`.
     * Thay đổi trạng thái (khóa/vô hiệu hóa) của tài khoản mang vai trò `ADMIN`.
3. **Bảo vệ Quản trị viên duy nhất (Last Admin Lockout Prevention)**:
   * Hệ thống cấm vô hiệu hóa hoặc khóa tài khoản `ADMIN` nếu người đó là Quản trị viên duy nhất còn lại trong hệ thống (`countUsersByRoleId <= 1`). Vi phạm sẽ trả về HTTP 422 `BUSINESS_RULE_VIOLATION`.
   * Quản trị viên không thể tự khóa tài khoản của chính mình.

---

## 5. Bảo Vệ Vai Trò Hệ Thống (System Role Protection)

Hệ thống định nghĩa 4 System Role cố định khởi tạo qua Migration Flyway: `ADMIN`, `OPERATOR`, `STAFF`, `CUSTOMER` với cờ `is_system = true`.

Các quy tắc bất biến đối với System Role:
* **CẤM sửa mã (`code`)**: Mã vai trò không thể thay đổi sau khi tạo.
* **CẤM sửa thông tin System Role**: `PUT /api/v1/roles/{id}` chặn mọi nỗ lực chỉnh sửa vai trò hệ thống, trả về HTTP 403 `SYSTEM_ROLE_PROTECTED`.
* **CẤM vô hiệu hóa System Role**: `PATCH /api/v1/roles/{id}/status` chặn chuyển trạng thái `INACTIVE` đối với System Role.
* **CẤM xóa System Role**: `DELETE /api/v1/roles/{id}` chặn xóa vai trò hệ thống.
* **CẤM xóa/vô hiệu hóa vai trò đang được sử dụng**: Nếu vai trò đang được gán cho người dùng, từ chối với HTTP 409 `ROLE_IN_USE`.

---

## 6. Tính Toàn Vẹn Dữ Liệu & Quản Lý Giao Dịch (`@Transactional`)

* Mọi thao tác ghi phân quyền nhiều bước (như `assignRolePermissions`, `updateUserRoles`) **BẮT BUỘC** thực thi trong `@Transactional`.
* Toàn bộ thao tác xóa liên kết cũ và chèn liên kết mới phải nằm trong cùng một boundary:
  * Nếu bất kỳ bước nào thất bại (ví dụ: role không tồn tại, role bị khóa, vi phạm validation), toàn bộ giao dịch bị rollback.
  * Request bị từ chối **tuyệt đối không để lại bất kỳ thay đổi nào trong cơ sở dữ liệu**.

---

## 7. Danh Mục Mã Lỗi An Ninh & RBAC (Standard Error Codes)

| HTTP Status | Mã ErrorCode | Ý nghĩa & Ngữ cảnh xuất hiện |
| :--- | :--- | :--- |
| `401 Unauthorized` | `UNAUTHORIZED` | Chưa đăng nhập hoặc thiếu Bearer Token trong header `Authorization`. |
| `401 Unauthorized` | `INVALID_TOKEN` | Token bị sai chữ ký, format hỏng hoặc token đã hết hạn (`ExpiredJwtException`). |
| `403 Forbidden` | `ACCESS_DENIED` | Người dùng đã đăng nhập nhưng không đủ quyền hạn thực hiện thao tác; hoặc cố tình can thiệp tài khoản Quản trị viên cấp cao. |
| `403 Forbidden` | `SYSTEM_ROLE_PROTECTED` | Nỗ lực sửa đổi thông tin hoặc xóa vai trò hệ thống (`is_system = true`). |
| `404 Not Found` | `RESOURCE_NOT_FOUND` | Không tìm thấy User, Role hoặc Permission tương ứng. |
| `409 Conflict` | `RESOURCE_ALREADY_EXISTS` | Mã vai trò (`code`) đã tồn tại trong hệ thống. |
| `409 Conflict` | `ROLE_IN_USE` | Cố gắng xóa hoặc vô hiệu hóa vai trò đang được gán cho ít nhất một người dùng. |
| `422 Unprocessable Entity` | `BUSINESS_RULE_VIOLATION` | Vi phạm quy tắc nghiệp vụ: gán vai trò đang bị `INACTIVE`, hoặc cố khóa tài khoản Quản trị viên duy nhất. |

---

## 8. Quy Trình Bổ Sung Quyền Hạn Mới (Permission Workflow)

Khi phát triển phân hệ mới (ví dụ: Chuyến xe, Tuyến đường, Đặt vé), quy trình chuẩn gồm 4 bước:

```text
1. Khảo sát danh mục Permission hiện có (tránh trùng lặp mã quyền)
    ↓
2. Tạo Flyway Migration mới (V{x}__add_{feature}_permissions.sql)
    - INSERT INTO permissions (code, name, description, module, action)
    - INSERT INTO role_permissions (gán quyền mặc định cho ADMIN)
    ↓
3. Bảo vệ Controller Endpoint bằng @PreAuthorize("hasAuthority('NEW_PERMISSION_CODE')")
    ↓
4. Bổ sung Integration Test kiểm tra 401, 403 (thiếu quyền) và 200 (có quyền)
```

---

## 9. Hướng Dẫn Chạy Kiểm Thử An Ninh (Running Security Tests)

Để xác thực tính đúng đắn của toàn bộ các chốt chặn an ninh:

```bash
# Chạy toàn bộ integration test của hệ thống (bao gồm Auth, User, Role, Permission)
./mvnw test

# Chạy riêng integration test kiểm tra bảo mật User & Privilege Escalation
./mvnw test -Dtest=UserControllerIntegrationTest

# Chạy riêng integration test kiểm tra bảo mật Role & System Role
./mvnw test -Dtest=RoleControllerIntegrationTest

# Kiểm tra định dạng code tuân thủ Spotless
./mvnw spotless:check
```
