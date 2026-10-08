# Đặc Tả API: Authentication & User Management (API Specification)

Tài liệu này quy định chi tiết hợp đồng giao tiếp RESTful API của Module **Authentication & User Management (BE-001)**.

---

## 1. Chuẩn Phản Hồi Chung (Standard Envelope)

Mọi phản hồi thành công tuân thủ mẫu:
```json
{
  "success": true,
  "message": "Thông điệp xử lý",
  "data": { ... },
  "timestamp": "2026-10-09T00:00:00Z"
}
```

Mọi phản hồi lỗi tuân thủ mẫu:
```json
{
  "status": 401,
  "code": "UNAUTHORIZED",
  "message": "Tên đăng nhập hoặc mật khẩu không chính xác",
  "path": "/api/v1/auth/login",
  "timestamp": "2026-10-09T00:00:00Z"
}
```

---

## 2. Nhóm Endpoint Xác Thực (`/api/v1/auth`)

### 2.1. Đăng nhập hệ thống (Login)
* **Endpoint**: `POST /api/v1/auth/login`
* **Quyền hạn**: **PUBLIC**
* **Mô tả**: Xác thực tài khoản và cấp phát bộ token. **Chỉ trả về Token tương ứng, không đính kèm thông tin user**.
* **Request Body**:
```json
{
  "username": "admin",
  "password": "Password@123"
}
```
* **Response Body (200 OK)**:
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "4a7b3c2d1e-8f9a-4b5c-9d8e-7f6a5b4c3d2e",
    "tokenType": "Bearer",
    "expiresIn": 1800
  },
  "timestamp": "2026-10-09T00:00:00Z"
}
```

### 2.2. Lấy thông tin người dùng hiện tại (Me Profile)
* **Endpoint**: `GET /api/v1/auth/me`
* **Quyền hạn**: **AUTHENTICATED** (Header: `Authorization: Bearer <accessToken>`)
* **Mô tả**: Sử dụng Access Token để lấy hồ sơ chi tiết, vai trò và toàn bộ quyền hạn được cấp.
* **Response Body (200 OK)**:
```json
{
  "success": true,
  "message": "Lấy thông tin người dùng thành công",
  "data": {
    "id": "b1b11111-1111-1111-1111-111111111111",
    "username": "admin",
    "email": "admin@dongly.vn",
    "fullName": "Quản trị viên Hệ thống Đông Lý",
    "phone": "0987654321",
    "status": "ACTIVE",
    "roles": ["ADMIN"],
    "permissions": ["USER_READ", "USER_CREATE", "USER_UPDATE", "USER_DELETE"],
    "createdAt": "2026-10-09T00:00:00Z"
  },
  "timestamp": "2026-10-09T00:00:00Z"
}
```

### 2.3. Làm mới Access Token (Token Refresh)
* **Endpoint**: `POST /api/v1/auth/refresh`
* **Quyền hạn**: **PUBLIC**
* **Mô tả**: Sử dụng Refresh Token để lấy cặp Access Token & Refresh Token mới (Xoay vòng token - Token Rotation).
* **Request Body**:
```json
{
  "refreshToken": "4a7b3c2d1e-8f9a-4b5c-9d8e-7f6a5b4c3d2e"
}
```
* **Response Body (200 OK)**:
```json
{
  "success": true,
  "message": "Làm mới mã thông báo thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "9f8e7d6c5b-4a3b-2c1d-0e9f-8a7b6c5d4e3f",
    "tokenType": "Bearer",
    "expiresIn": 1800
  },
  "timestamp": "2026-10-09T00:00:00Z"
}
```

### 2.4. Đăng xuất tài khoản (Logout)
* **Endpoint**: `POST /api/v1/auth/logout`
* **Quyền hạn**: **AUTHENTICATED**
* **Mô tả**: Thu hồi và vô hiệu hóa Refresh Token của phiên làm việc hiện tại.
* **Request Body**:
```json
{
  "refreshToken": "9f8e7d6c5b-4a3b-2c1d-0e9f-8a7b6c5d4e3f"
}
```
* **Response Body (200 OK)**:
```json
{
  "success": true,
  "message": "Đăng xuất thành công",
  "data": null,
  "timestamp": "2026-10-09T00:00:00Z"
}
```

---

## 3. Nhóm Endpoint Quản Lý Người Dùng (`/api/v1/users`)

| Method | Endpoint | Quyền hạn yêu cầu | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users` | `hasAuthority('USER_READ')` | Lấy danh sách người dùng phân trang |
| `GET` | `/api/v1/users/{id}` | `hasAuthority('USER_READ')` hoặc Self | Lấy chi tiết người dùng theo ID (Chống IDOR) |
| `POST` | `/api/v1/users` | `hasAuthority('USER_CREATE')` | Tạo người dùng mới và gán vai trò |
| `PUT` | `/api/v1/users/{id}` | `hasAuthority('USER_UPDATE')` | Cập nhật hồ sơ người dùng |
| `PATCH` | `/api/v1/users/{id}/status`| `hasAuthority('USER_UPDATE')` | Thay đổi trạng thái (`ACTIVE`, `INACTIVE`, `LOCKED`) |
