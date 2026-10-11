# Đặc Tả API: Authentication, RBAC & User Management (API Specification)

Tài liệu này quy định chi tiết hợp đồng giao tiếp RESTful API của Module **Authentication, RBAC & User Management (BE-001)**.

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

Phản hồi phân trang tuân thủ mẫu:
```json
{
  "success": true,
  "message": "Lấy danh sách thành công",
  "data": [ ... ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 4,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-10-09T00:00:00Z"
}
```

---

## 2. Nhóm Endpoint Xác Thực (`/api/v1/auth`)

| Method | Endpoint | Quyền hạn yêu cầu | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | **PUBLIC** | Xác thực tài khoản và cấp phát bộ token (Access & Refresh Token). |
| `GET` | `/api/v1/auth/me` | **AUTHENTICATED** | Lấy thông tin tài khoản hiện tại kèm roles và permissions. |
| `POST` | `/api/v1/auth/refresh` | **PUBLIC** | Xoay vòng Refresh Token (Token Rotation) để lấy token mới. |
| `POST` | `/api/v1/auth/logout` | **AUTHENTICATED** | Đăng xuất phiên làm việc và thu hồi Refresh Token hiện tại. |

---

## 3. Nhóm Endpoint Quản Lý Danh Mục Quyền Hạn (`/api/v1/permissions`)

### 3.1. Lấy danh mục quyền hạn hệ thống (Permission Catalog)
* **Endpoint**: `GET /api/v1/permissions`
* **Quyền hạn**: `hasAuthority('PERMISSION_READ') or hasAuthority('ROLE_READ')`
* **Query Parameters**:
  * `module` (tùy chọn): Lọc theo module (`USER`, `ROLE`, `ROUTE`, `FLEET`,...).
* **Response Body (200 OK)**:
```json
{
  "success": true,
  "message": "Lấy danh mục quyền hạn thành công",
  "data": {
    "total": 19,
    "modules": [
      {
        "module": "USER",
        "moduleName": "Quản lý người dùng",
        "permissions": [
          {
            "id": "e5b8...-...",
            "code": "USER_READ",
            "name": "Xem người dùng",
            "description": "Quyền tra cứu danh sách và xem chi tiết người dùng",
            "module": "USER",
            "action": "READ"
          }
        ]
      }
    ]
  },
  "timestamp": "2026-10-10T00:00:00Z"
}
```

---

## 4. Nhóm Endpoint Quản Trị Vai Trò & Phân Quyền (`/api/v1/roles`)

### 4.1. Lấy danh sách vai trò phân trang & tìm kiếm
* **Endpoint**: `GET /api/v1/roles`
* **Quyền hạn**: `hasAuthority('ROLE_READ')`
* **Query Parameters**: `page` (default 0), `size` (default 20, max 100), `sort` (default `createdAt,desc`), `search` (tìm theo code hoặc name), `status` (`ACTIVE`, `INACTIVE`).
* **Response Body (200 OK)**: Trả về danh sách theo chuẩn `PageResponse<RoleResponse>`.

### 4.2. Lấy chi tiết vai trò kèm toàn bộ quyền hạn
* **Endpoint**: `GET /api/v1/roles/{id}`
* **Quyền hạn**: `hasAuthority('ROLE_READ')`
* **Response Body (200 OK)**: Trả về `RoleDetailResponse` gồm thông tin vai trò và mảng `permissions`.

### 4.3. Tạo mới vai trò tùy chỉnh
* **Endpoint**: `POST /api/v1/roles`
* **Quyền hạn**: `hasAuthority('ROLE_CREATE')`
* **Request Body**:
```json
{
  "code": "DISPATCHER",
  "name": "Điều hành bến bãi",
  "description": "Nhân viên điều phối xe tại bến",
  "permissionCodes": ["TRIP_READ", "TRIP_MANAGE"]
}
```
* **Response Body (201 Created)**: Trả về `RoleDetailResponse`.

### 4.4. Cập nhật tên và mô tả vai trò
* **Endpoint**: `PUT /api/v1/roles/{id}`
* **Quyền hạn**: `hasAuthority('ROLE_UPDATE')`
* **Ràng buộc**: Chặn chỉnh sửa vai trò hệ thống (`is_system = true` hoặc `ADMIN`) với mã lỗi 403 `SYSTEM_ROLE_PROTECTED`.
* **Request Body**:
```json
{
  "name": "Điều hành bến xe trung tâm",
  "description": "Cập nhật mô tả điều hành xe"
}
```

### 4.5. Cập nhật trạng thái vai trò
* **Endpoint**: `PATCH /api/v1/roles/{id}/status`
* **Quyền hạn**: `hasAuthority('ROLE_UPDATE')`
* **Ràng buộc**:
  * Không cho phép vô hiệu hóa vai trò hệ thống (`SYSTEM_ROLE_PROTECTED`).
  * Không cho phép vô hiệu hóa vai trò đang được gán cho người dùng (409 `ROLE_IN_USE`).
* **Request Body**:
```json
{
  "status": "INACTIVE"
}
```

### 4.6. Xem danh sách quyền của vai trò
* **Endpoint**: `GET /api/v1/roles/{id}/permissions`
* **Quyền hạn**: `hasAuthority('ROLE_READ')`
* **Response Body (200 OK)**: Mảng danh sách `List<PermissionResponse>`.

### 4.7. Gán toàn bộ danh mục quyền cho vai trò
* **Endpoint**: `PUT /api/v1/roles/{id}/permissions`
* **Quyền hạn**: `hasAuthority('ROLE_ASSIGN')`
* **Ràng buộc**:
  * Thực thi trong một `@Transactional` duy nhất.
  * Toàn bộ mã quyền phải tồn tại trong database (nếu có mã không tồn tại, trả về 404 `RESOURCE_NOT_FOUND` và rollback hoàn toàn).
* **Request Body**:
```json
{
  "permissionCodes": ["ROUTE_READ", "TRIP_READ"]
}
```

### 4.8. Xóa vai trò tùy chỉnh
* **Endpoint**: `DELETE /api/v1/roles/{id}`
* **Quyền hạn**: `hasAuthority('ROLE_DELETE')`
* **Ràng buộc**:
  * Không được xóa vai trò hệ thống (403 `SYSTEM_ROLE_PROTECTED`).
  * Không được xóa vai trò đang được gán cho người dùng (409 `ROLE_IN_USE`).

---

## 5. Nhóm Endpoint Quản Lý Người Dùng & Vai Trò Người Dùng (`/api/v1/users`)

| Method | Endpoint | Quyền hạn yêu cầu | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users` | `hasAuthority('USER_READ')` | Lấy danh sách người dùng phân trang |
| `GET` | `/api/v1/users/{id}` | `isAuthenticated()` | Lấy chi tiết người dùng (Yêu cầu `USER_READ` hoặc chính chủ - Chống IDOR) |
| `POST` | `/api/v1/users` | `hasAuthority('USER_CREATE')` | Tạo người dùng mới và gán danh sách `roleCodes` ban đầu. Chặn gán `ADMIN` nếu người gọi không phải `ADMIN`. |
| `PUT` | `/api/v1/users/{id}` | `hasAuthority('USER_UPDATE')` | Cập nhật hồ sơ và `roleCodes`. Chặn non-admin sửa profile của `ADMIN` hoặc gán role `ADMIN`. |
| `PATCH` | `/api/v1/users/{id}/status` | `hasAuthority('USER_UPDATE')` | Cập nhật trạng thái (`ACTIVE`, `INACTIVE`, `LOCKED`). Chặn khóa tài khoản `ADMIN` bởi non-admin và chặn khóa `ADMIN` cuối cùng. Thu hồi toàn bộ refresh token khi bị khóa. |
| `GET` | `/api/v1/users/{id}/roles` | `isAuthenticated()` | Lấy danh sách các vai trò của người dùng (Yêu cầu `USER_READ`, `ROLE_READ` hoặc chính chủ). |
| `PUT` | `/api/v1/users/{id}/roles` | `hasAuthority('ROLE_ASSIGN') or hasRole('ADMIN')` | Cập nhật danh sách vai trò của người dùng trong một transaction. Chặn tự nâng quyền, validate role `ACTIVE` và tự động thu hồi refresh token của user. |

### 5.1. Cập nhật danh sách vai trò của người dùng (`PUT /api/v1/users/{id}/roles`)
* **Request Body**:
```json
{
  "roleCodes": ["OPERATOR", "STAFF"]
}
```
* **Response Body (200 OK)**:
```json
{
  "success": true,
  "message": "Cập nhật danh sách vai trò của người dùng thành công",
  "data": [
    {
      "id": "c1c11111-1111-1111-1111-111111111111",
      "code": "OPERATOR",
      "name": "Nhân viên điều hành",
      "description": "Quản lý lịch trình và phân bổ chuyến xe",
      "status": "ACTIVE",
      "isSystem": true,
      "createdAt": "2026-10-09T00:00:00Z"
    }
  ],
  "timestamp": "2026-10-10T00:00:00Z"
}
```

---

## 6. Nhóm Endpoint CRM Booking (`/api/v1/crm/bookings`)

| Method | Endpoint | Quyền hạn yêu cầu | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/crm/bookings/trips/search` | `hasAuthority('BOOKING_READ')` | Tìm kiếm chuyến xe mở bán theo ngày, tuyến đường, địa phương xuất phát/đích và từ khóa. Trả về thông tin chuyến, phương tiện, giá vé, số ghế khả dụng. |
| `GET` | `/api/v1/crm/bookings/trips/{tripId}/seat-map` | `hasAuthority('BOOKING_READ')` | Lấy sơ đồ lưới ghế (tầng, hàng, cột), danh sách điểm đón/trả và trạng thái chiếm chỗ thời gian thực từng ghế (`AVAILABLE`, `HELD`, `BOOKED`, `LOCKED`). |
| `POST` | `/api/v1/crm/bookings/hold` | `hasAuthority('BOOKING_CREATE')` | Giữ một hoặc nhiều ghế tạm thời (mặc định 10 phút). Chống race condition với Pessimistic Lock & DB Partial Unique Constraint. |
| `POST` | `/api/v1/crm/bookings/{id}/confirm` | `hasAuthority('BOOKING_CREATE')` | Xác nhận booking giữ ghế thành công: kiểm tra thời hạn, quyền sở hữu, tính lại giá vé, ghi nhận thanh toán (`CASH`, `BANK_TRANSFER`, `VIETQR`) và phát hành vé điện tử. |
| `POST` | `/api/v1/crm/bookings/{id}/cancel-hold` | `hasAuthority('BOOKING_CREATE')` | Hủy giữ ghế chủ động trước khi hết hạn bởi nhân viên sở hữu booking hoặc Quản trị viên. |

### 6.1. Tìm kiếm chuyến xe mở bán (`GET /api/v1/crm/bookings/trips/search`)
* **Query Parameters**:
  * `departureDate` (bắt buộc, định dạng `YYYY-MM-DD`, ví dụ: `2026-10-15`)
  * `routeId` (tùy chọn, UUID)
  * `originLocationId` (tùy chọn, UUID)
  * `destinationLocationId` (tùy chọn, UUID)
  * `keyword` (tùy chọn, chuỗi tìm kiếm mã chuyến, biển số xe, tên tuyến)
  * `page` (tùy chọn, mặc định 0)
  * `size` (tùy chọn, mặc định 20, tối đa 100)
  * `sort` (tùy chọn, mặc định `departureTime,asc`)
* **Response Body (200 OK)**: Chuẩn `PageResponse<CrmTripSearchResultResponse>`.

### 6.2. Lấy sơ đồ ghế chuyến xe thời gian thực (`GET /api/v1/crm/bookings/trips/{tripId}/seat-map`)
* **Path Variable**: `tripId` (UUID chuyến xe)
* **Response Body (200 OK)**: Chuẩn `ApiResponse<CrmTripSeatMapResponse>`.
  * Bao gồm ma trận kích thước xe (`totalFloors`, `totalRows`, `totalColumns`).
  * Chi tiết danh sách ghế với cờ `isBookable`, `calculatedPrice` (`basePrice + seatExtraPrice`) và `occupancyStatus`.
  * Danh sách các điểm dừng đón/trả trên tuyến kèm phụ phí.

### 6.3. Giữ ghế tạm thời (`POST /api/v1/crm/bookings/hold`)
* **Quyền hạn**: `hasAuthority('BOOKING_CREATE')`
* **Request Body**:
```json
{
  "tripId": "d04a6011-8fcb-48c0-bc66-3d758f8e025f",
  "seatIds": ["7a51833c-3dae-4cf4-a957-c584cb0255a4", "b3ec2e0d-13b7-4a6f-bd1a-b67ae2d1920c"],
  "pickupStopId": "38b934b0-a548-4cb9-99fc-767a57a58a9e",
  "dropoffStopId": "3f422e57-a9a3-4a1d-9e66-e04f05ba67bb",
  "note": "Khách gọi hotline giữ chỗ"
}
```
* **Response Body (201 Created)**:
```json
{
  "success": true,
  "message": "Giữ ghế thành công trong 10 phút",
  "data": {
    "bookingId": "c9287c80-e713-4074-b5a8-4c80cb5f7aa1",
    "bookingCode": "DL-261011-ABCD",
    "tripId": "d04a6011-8fcb-48c0-bc66-3d758f8e025f",
    "tripCode": "TRP-HN-TH-01",
    "status": "HOLDING",
    "expiresAt": "2026-10-11T10:25:00Z",
    "remainingSeconds": 600,
    "heldSeats": [
      {
        "seatId": "7a51833c-3dae-4cf4-a957-c584cb0255a4",
        "seatNumber": "A01",
        "floor": 1,
        "price": 200000.00
      }
    ],
    "estimatedTotalPrice": 200000.00
  },
  "timestamp": "2026-10-11T10:15:00Z"
}
```
* **Lỗi có thể trả về**:
  * `409 Conflict` (`SEAT_ALREADY_RESERVED`): Một hoặc nhiều ghế đã có người giữ hoặc đặt thành công.
  * `400 Bad Request` (`SEAT_NOT_AVAILABLE`): Ghế không thuộc chuyến xe hoặc bị khóa hỏng.

### 6.4. Xác nhận booking và phát hành vé (`POST /api/v1/crm/bookings/{id}/confirm`)
* **Path Variable**: `id` (UUID của Booking đang ở trạng thái `HOLDING`)
* **Quyền hạn**: `hasAuthority('BOOKING_CREATE')`
* **Ràng buộc**: Phải được xác nhận bởi chính nhân viên đã giữ chỗ (hoặc người có quyền Admin); Phải còn hạn giữ ghế; Tiền thanh toán `amount` phải khớp chính xác với `totalPrice` được tính lại ở Backend.
* **Request Body**:
```json
{
  "customerName": "Nguyễn Văn A",
  "customerPhone": "0987654321",
  "customerEmail": "nguyenvana@gmail.com",
  "pickupStopId": "38b934b0-a548-4cb9-99fc-767a57a58a9e",
  "dropoffStopId": "3f422e57-a9a3-4a1d-9e66-e04f05ba67bb",
  "passengers": [
    {
      "seatId": "7a51833c-3dae-4cf4-a957-c584cb0255a4",
      "passengerName": "Nguyễn Văn A",
      "passengerPhone": "0987654321"
    }
  ],
  "payment": {
    "method": "CASH",
    "amount": 200000.00,
    "referenceCode": null,
    "note": "Khách thanh toán tiền mặt tại quầy"
  },
  "note": "Đã thu đủ tiền"
}
```
* **Response Body (200 OK)**: Chuẩn `ApiResponse<BookingResponse>`.
  * Trả về thông tin chi tiết Booking, Customer, danh sách Ticket phát hành (`ticketCode`, `qrCode`, `seatNumber`), và Payment.
* **Lỗi có thể trả về**:
  * `410 Gone` (`SEAT_HOLD_EXPIRED`): Đã hết 10 phút giữ ghế, ghế đã được tự động giải phóng.
  * `403 Forbidden` (`FORBIDDEN`): Nhân viên khác cố gắng xác nhận booking không phải do mình giữ.
  * `400 Bad Request` (`INVALID_PAYMENT_AMOUNT`): Số tiền gửi lên không khớp với giá vé backend tính toán.
  * `409 Conflict` (`BOOKING_ALREADY_CONFIRMED`): Booking đã được thanh toán và xác nhận trước đó (Idempotency).

### 6.5. Hủy giữ ghế chủ động (`POST /api/v1/crm/bookings/{id}/cancel-hold`)
* **Path Variable**: `id` (UUID của Booking đang ở trạng thái `HOLDING`)
* **Quyền hạn**: `hasAuthority('BOOKING_CREATE')`
* **Response Body (200 OK)**: Chuẩn `ApiResponse<Void>` thông báo giải phóng ghế thành công.
* **Lỗi có thể trả về**:
  * `403 Forbidden` (`FORBIDDEN`): Không có quyền hủy booking của người khác.
  * `400 Bad Request` (`INVALID_STATUS_TRANSITION`): Booking không ở trạng thái `HOLDING`.

