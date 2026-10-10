# Cơ Chế Xác Thực & Phân Quyền (Authentication & Authorization)

Tài liệu này giải thích chi tiết cơ chế bảo mật, quy trình phát hành token, vòng đời Refresh Token và mô hình phân quyền RBAC được triển khai trong dự án **Hệ thống Quản lý Vận tải & Đặt vé Đông Lý**.

---

## 1. Mô Hình Quyền Hạn Hạt Nhân (RBAC + Granular Permissions)

Hệ thống kết hợp giữa **Role-Based Access Control (RBAC)** và **Granular Permissions**:
* **Role**: Đại diện cho vai trò vị trí công việc:
  * **System Role** (`is_system = TRUE`): `ADMIN`, `OPERATOR`, `STAFF`, `CUSTOMER`. Bất biến, không thể xóa hoặc sửa mã.
  * **Custom Role** (`is_system = FALSE`): Các vai trò tùy chỉnh do quản trị viên tạo thêm khi vận hành hệ thống.
* **Permission**: Quyền thao tác cụ thể trên tài nguyên, có cấu trúc module và hành động (`module`, `action`).

Người dùng được gán nhiều Roles; mỗi Role được liên kết với nhiều Permissions qua quan hệ N-N. Khi người dùng xác thực, hệ thống trích xuất tập hợp quyền hợp nhất (effective permissions) từ tất cả các role đang hoạt động (`ACTIVE`) và nạp vào JWT Claims cũng như Spring Security `GrantedAuthority`.

---

## 2. Danh Mục Quyền Hạn Toàn Hệ Thống (Permission Catalog)

Theo migration Flyway `V4__enhance_rbac_tables_and_seed_permissions.sql`, danh mục quyền hạn gồm các nhóm module:

### 2.1. Phân hệ Quản trị Người dùng (Module: `USER`)
* `USER_READ`: Xem danh sách và chi tiết người dùng.
* `USER_CREATE`: Tạo mới người dùng và gán vai trò ban đầu.
* `USER_UPDATE`: Sửa đổi thông tin hồ sơ và trạng thái tài khoản người dùng.
* `USER_DELETE`: Xóa/vô hiệu hóa tài khoản người dùng.

### 2.2. Phân hệ Quản trị Vai trò (Module: `ROLE`)
* `ROLE_READ`: Tra cứu danh sách và xem chi tiết vai trò, xem danh sách quyền của vai trò.
* `ROLE_CREATE`: Tạo mới vai trò tùy chỉnh.
* `ROLE_UPDATE`: Sửa đổi tên, mô tả và trạng thái hoạt động của vai trò.
* `ROLE_DELETE`: Xóa vai trò tùy chỉnh (nếu chưa được gán cho user nào).
* `ROLE_ASSIGN`: Gán hoặc thu hồi quyền hạn của vai trò (`PUT /api/v1/roles/{id}/permissions`) và cập nhật vai trò của người dùng (`PUT /api/v1/users/{id}/roles`).

### 2.3. Phân hệ Danh mục Quyền (Module: `PERMISSION`)
* `PERMISSION_READ`: Tra cứu danh mục quyền hạn của toàn hệ thống (Catalog theo module).

### 2.4. Phân hệ Vận tải & Nghiệp vụ Vé (Đã seed sẵn sàng cho các giai đoạn tiếp theo)
* **Tuyến đường (`ROUTE`)**: `ROUTE_READ`, `ROUTE_MANAGE`.
* **Đội xe & Tài xế (`FLEET`)**: `FLEET_READ`, `FLEET_MANAGE`.
* **Lịch trình chuyến (`TRIP`)**: `TRIP_READ`, `TRIP_MANAGE`.
* **Đặt vé (`BOOKING`)**: `BOOKING_READ`, `BOOKING_MANAGE`.
* **Vé điện tử (`TICKET`)**: `TICKET_READ`.

---

## 3. Cấu Trúc JWT & Quản Lý Phiên Làm Việc

### 3.1. JWT Access Token Payload
JWT Access Token được ký bằng HMAC-SHA256 với các claims tối giản và an toàn:
```json
{
  "sub": "admin@dongly.vn",
  "uid": "b1b11111-1111-1111-1111-111111111111",
  "username": "admin",
  "roles": [
    "ADMIN"
  ],
  "permissions": [
    "USER_READ",
    "USER_CREATE",
    "USER_UPDATE",
    "USER_DELETE",
    "ROLE_READ",
    "ROLE_CREATE",
    "ROLE_UPDATE",
    "ROLE_DELETE",
    "ROLE_ASSIGN",
    "PERMISSION_READ",
    "ROUTE_READ",
    "ROUTE_MANAGE",
    "FLEET_READ",
    "FLEET_MANAGE",
    "TRIP_READ",
    "TRIP_MANAGE",
    "BOOKING_READ",
    "BOOKING_MANAGE",
    "TICKET_READ"
  ],
  "iat": 1775779200,
  "exp": 1775781000
}
```

### 3.2. Vòng Đời Refresh Token & Cơ Chế Xoay Vòng (Token Rotation)
* Raw token (64 ký tự ngẫu nhiên) chỉ gửi về cho client, database chỉ lưu `SHA-256(raw_token)` tại cột `token_hash`.
* Mỗi lần gọi `POST /api/v1/auth/refresh`:
  1. Refresh token cũ được đánh dấu `revoked_at = CURRENT_TIMESTAMP`.
  2. Cặp Access Token và Refresh Token mới được sinh ra trả về cho client.
* **Phát hiện tái sử dụng trái phép (Replay Attack Detection)**: Nếu một refresh token đã bị thu hồi được gửi lên, hệ thống lập tức thu hồi toàn bộ refresh token đang hoạt động của người dùng đó.

### 3.3. Giới Hạn Thu Hồi Quyền Của Stateless Token
* Access token là stateless với thời hạn 30 phút (`1800s`). Trong thời gian 30 phút này, token cũ vẫn có hiệu lực nếu chưa hết hạn.
* Khi vai trò bị thay đổi qua `PUT /api/v1/users/{id}/roles` hoặc tài khoản bị khóa qua `PATCH /api/v1/users/{id}/status`:
  * Hệ thống lập tức gọi `revokeAllActiveTokensByUserId` để hủy toàn bộ refresh token.
  * Khi access token hiện tại hết hạn, người dùng không thể refresh và bắt buộc phải đăng nhập lại để nhận token mới phản ánh đúng quyền hạn cập nhật.

---

## 4. Các Biện Pháp An Toàn & Phòng Thủ (Defense-in-Depth)

1. **Bảo vệ System Role**: Các role hệ thống (`ADMIN`, `OPERATOR`, `STAFF`, `CUSTOMER`) có cờ `is_system = true`, cấm sửa thông tin, cấm vô hiệu hóa và cấm xóa.
2. **Chống tự nâng quyền (Anti-Privilege Escalation)**: Chỉ `ADMIN` mới có thể tạo người dùng có role `ADMIN` hoặc gán role `ADMIN` cho tài khoản khác.
3. **Bảo vệ tài khoản Quản trị viên**: Non-admin không được sửa thông tin cá nhân hoặc khóa tài khoản của `ADMIN`.
4. **Bảo vệ Quản trị viên duy nhất**: Không cho phép vô hiệu hóa hoặc khóa tài khoản `ADMIN` duy nhất trong hệ thống (`countUsersByRoleId <= 1`).
5. **Chống IDOR**: Tại endpoint `GET /api/v1/users/{id}` và `GET /api/v1/users/{id}/roles`, người dùng chỉ được xem thông tin chính mình trừ khi sở hữu quyền `USER_READ` hoặc `ROLE_READ`.
6. **Toàn vẹn giao dịch (`@Transactional`)**: Mọi thao tác gán quyền hoặc đổi vai trò chạy trong transaction. Nếu request bị từ chối (404/403/422), cơ sở dữ liệu được rollback hoàn toàn và không lưu lại trạng thái dở dang.

---

## 5. Hướng Dẫn Kiểm Thử Tự Động

```bash
# Chạy toàn bộ integration test
./mvnw test

# Kiểm tra tuân thủ quy chuẩn mã nguồn Spotless
./mvnw spotless:check
```
