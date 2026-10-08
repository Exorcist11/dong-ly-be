# Quy Tắc Bảo Mật Hệ Thống (Security Guidelines)

Tài liệu này xác định các tiêu chuẩn bảo mật bắt buộc đối với hệ thống Backend Spring Boot, bảo đảm an toàn dữ liệu, chống rò rỉ bí mật và kiểm soát truy cập nghiêm ngặt.

---

## 1. Nguyên Tắc Vàng Về Quyền Hạn (Authoritative Backend)

> **Backend là cơ quan thẩm quyền tối cao**.
> Tuyệt đối không bao giờ tin tưởng quyền hạn hoặc định danh mà client gửi lên trong Request Body. Mọi quyết định cho phép hay từ chối truy cập tài nguyên đều phải được kiểm tra độc lập tại Backend.

---

## 2. Cấu Hình Spring Security Chuẩn

Ứng dụng REST API hoạt động theo mô hình phi trạng thái (**Stateless Session**):

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Bật @PreAuthorize ở cấp độ phương thức
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable) // Tắt CSRF vì dùng JWT Bearer token
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

---

## 3. Xác Thực Người Dùng Bằng JWT (Authentication)

* **Access Token**:
  * Thời gian sống ngắn: 15 đến 30 phút.
  * Chứa các claim tối giản: `userId`, `roles`, `email`.
  * **CẤM** đưa thông tin nhạy cảm vào JWT payload (mật khẩu, quyền hạn nội bộ chi tiết, số CCCD/CMND).
* **Refresh Token**:
  * Thời gian sống dài hơn: 7 đến 30 ngày.
  * Lưu trữ an toàn trong Database kèm mã hash hoặc cơ chế Token Rotation (lập tức thu hồi toàn bộ phiên nếu phát hiện Refresh Token bị dùng lại trái phép).
* **Băm Mật Khẩu**:
  * **BẮT BUỘC** sử dụng `BCryptPasswordEncoder` (độ mạnh cost factor tối thiểu là `12`) hoặc `Argon2`.
  * **TUYỆT ĐỐI CẤM** dùng MD5, SHA-1, SHA-256 không có salt để lưu mật khẩu.

---

## 4. Phân Quyền & Chống Lỗ Hổng IDOR (Authorization & Access Control)

### 4.1. Phân Quyền Cấp Độ Phương Thức
Sử dụng `@PreAuthorize` để bảo vệ các hành động nhạy cảm:
```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable UUID id) { ... }
```

### 4.2. Chống Lỗ Hổng Truy Cập Trực Tiếp Đối Tượng (IDOR Prevention)
Khi người dùng yêu cầu xem/sửa tài nguyên theo `id` (ví dụ: `GET /api/v1/orders/{orderId}`):
* **CẤM**: Chỉ kiểm tra `orderRepository.findById(orderId)` rồi trả về.
* **BẮT BUỘC**: Kiểm tra quyền sở hữu của người dùng hiện tại đối với tài nguyên đó:
```java
Order order = orderRepository.findById(orderId)
    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

// Chống IDOR: Chỉ chủ sở hữu hoặc ADMIN mới có quyền truy cập
if (!order.getUserId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
    throw new AccessDeniedException("Bạn không có quyền truy cập đơn hàng này");
}
```

---

## 5. Cấu Hình CORS & Headers Bảo Mật

* **CORS**:
  * Không sử dụng `allowedOrigins("*")` khi có kết hợp gửi kèm thông tin định danh (`allowCredentials(true)`).
  * Danh sách domain client được phép kết nối phải được cấu hình qua biến môi trường (`APP_CORS_ALLOWED_ORIGINS`).
* **Headers**:
  * Bật `X-Content-Type-Options: nosniff`.
  * Bật `X-Frame-Options: DENY` (chống Clickjacking).
  * Bật `Strict-Transport-Security` (HSTS) trên môi trường Production.

---

## 6. Quản Lý Khóa Bí Mật Tuyệt Đối An Toàn (Secrets Management)

* **TUYỆT ĐỐI KHÔNG BAO GIỜ COMMIT LÊN GIT**:
  * Mật khẩu database.
  * Khóa bí mật JWT (`jwt.secret`).
  * Khóa API bên thứ ba (AWS S3, Stripe, Twilio, SendGrid).
* **Mọi bí mật phải được truyền qua Biến Môi Trường (Environment Variables)**:
  * Khai báo mẫu không chứa giá trị thật trong `.env.example`.
  * Đưa `.env`, `*.key`, `*.pem` vào file `.gitignore`.
