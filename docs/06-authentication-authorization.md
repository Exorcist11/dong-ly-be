# Cơ Chế Xác Thực & Phân Quyền (Authentication & Authorization)

Tài liệu này giải thích chi tiết cơ chế bảo mật, quy trình phát hành token, vòng đời Refresh Token và mô hình phân quyền RBAC được triển khai trong dự án.

---

## 1. Mô Hình Quyền Hạn Hạt Nhân (RBAC + Granular Permissions)

Hệ thống kết hợp giữa **Role-Based Access Control (RBAC)** và **Granular Permissions**:
* **Role**: Đại diện cho vai trò vị trí công việc (`ADMIN`, `OPERATOR`, `STAFF`, `CUSTOMER`).
* **Permission**: Đại diện cho quyền thao tác cụ thể trên tài nguyên (`USER_READ`, `USER_CREATE`, `USER_UPDATE`, `USER_DELETE`).

Người dùng được gán nhiều Roles; mỗi Role được cấu hình nhiều Permissions. Khi người dùng xác thực, hệ thống trích xuất toàn bộ Permissions từ các Roles tương ứng và nạp vào JWT Claims cũng như Spring Security `GrantedAuthority`.

---

## 2. Vòng Đời Refresh Token & Cơ Chế Xoay Vòng (Token Rotation)

### 2.1. Quyết định kỹ thuật: Lưu trữ băm SHA-256
* Client nhận chuỗi ngẫu nhiên bảo mật 64 ký tự (raw token).
* Database chỉ lưu trữ `SHA-256(raw_token)` trong cột `token_hash`.
* Ngay cả khi cơ sở dữ liệu bị lộ lọt (DB dump), kẻ tấn công không thể giải mã hay tạo ra token nguyên bản để gọi API.

### 2.2. Xoay vòng Token (Rotation) & Phát hiện tấn công Replay
* Mỗi khi client gọi `POST /api/v1/auth/refresh`:
  1. Token cũ lập tức được đánh dấu `revoked_at = CURRENT_TIMESTAMP`.
  2. Một refresh token mới và access token mới được sinh ra trả về cho client.
* **Cơ chế chống đánh cắp token (Replay Attack Detection)**:
  * Nếu một Refresh Token đã bị thu hồi (`revoked_at != null`) được gửi lên để refresh, hệ thống nhận diện phiên làm việc có dấu hiệu bị rò rỉ / kẻ xấu cố ý dùng lại token cũ.
  * Hệ thống lập tức thu hồi **toàn bộ các refresh token đang hoạt động** của người dùng đó và yêu cầu đăng nhập lại từ đầu.

---

## 3. Cấu Trúc JWT Payload

JWT Access Token được ký bằng thuật toán HMAC-SHA256 (`Keys.hmacShaKeyFor`) với các claims tối giản và an toàn:

```json
{
  "sub": "admin@dongly.vn",
  "uid": "b1b11111-1111-1111-1111-111111111111",
  "username": "admin",
  "roles": ["ADMIN"],
  "permissions": [
    "USER_READ",
    "USER_CREATE",
    "USER_UPDATE",
    "USER_DELETE"
  ],
  "iat": 1775779200,
  "exp": 1775781000
}
```

---

## 4. Các Quyết Định An Toàn & Phòng Thủ (Defense-in-Depth)

1. **Băm mật khẩu**: `BCryptPasswordEncoder` với độ mạnh cost factor `12`.
2. **Chống dò quét tài khoản (Anti-Account Enumeration)**: Lỗi đăng nhập luôn trả về thông điệp chung `"Tên đăng nhập hoặc mật khẩu không chính xác"` khi sai username hoặc sai password.
3. **Chống lỗ hổng IDOR**: Tại endpoint `GET /api/v1/users/{id}`, người dùng chỉ được xem thông tin chính mình trừ khi sở hữu quyền `USER_READ`.
4. **Bảo vệ tài khoản quản trị**: Quản trị viên không thể tự khóa tài khoản của chính mình.
5. **Thu hồi phiên khi vô hiệu hóa**: Chuyển trạng thái người dùng sang `INACTIVE` hoặc `LOCKED` lập tức hủy toàn bộ các Refresh Token của tài khoản đó.
