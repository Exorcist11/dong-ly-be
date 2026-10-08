# Tiêu Chuẩn Thiết Kế REST API (REST API Guidelines)

Tài liệu này xác định các chuẩn mực thiết kế REST API, quy ước đặt tên endpoint, ngữ nghĩa mã HTTP và cấu trúc phản hồi dữ liệu đồng nhất.

---

## 1. Quy Ước Đặt Tên Endpoint (URI Naming Conventions)

* **Sử dụng danh từ số nhiều** cho tài nguyên: `/api/v1/users`, `/api/v1/orders`.
* **Kebab-case** cho các từ ghép trong URI: `/api/v1/user-profiles`, `/api/v1/order-items`.
* **Phiên bản hóa API trong URI**: Luôn có tiền tố `/api/v1/`.
* **Quan hệ cha - con phân cấp**:
  * Lấy danh sách bình luận của bài viết: `GET /api/v1/posts/{postId}/comments`
  * Tạo bình luận mới cho bài viết: `POST /api/v1/posts/{postId}/comments`
  * Khi tài nguyên con có thể định danh độc lập, không lồng quá 2 cấp: `GET /api/v1/comments/{commentId}`
* **Các hành động phi CRUD (RPC-style Actions)**:
  * Khi hành động không thuần túy là thêm/sửa/xóa CRUD, dùng động từ ở cuối URI:
  * `POST /api/v1/orders/{id}/cancel` (Hủy đơn hàng)
  * `POST /api/v1/auth/forgot-password` (Quên mật khẩu)

---

## 2. Ngữ Nghĩa HTTP Methods & HTTP Status Codes

**TUYỆT ĐỐI KHÔNG** luôn luôn trả về `HTTP 200 OK` cho mọi trường hợp (kể cả khi lỗi). Phải sử dụng chính xác mã trạng thái HTTP:

| Phương thức | Ý nghĩa | Trạng thái thành công thường gặp |
| :--- | :--- | :--- |
| `GET` | Đọc dữ liệu, an toàn (Safe), bất biến trạng thái (Idempotent) | `200 OK` |
| `POST` | Tạo mới tài nguyên hoặc thực thi tác vụ | `201 Created` (kèm URL tài nguyên mới nếu có), `200 OK` |
| `PUT` | Thay thế hoàn toàn bản ghi (Idempotent) | `200 OK` |
| `PATCH`| Cập nhật một phần thuộc tính bản ghi | `200 OK` |
| `DELETE`| Xóa tài nguyên (Idempotent) | `204 No Content` hoặc `200 OK` |

### Bảng Mã Lỗi Chuẩn:
* `400 Bad Request`: Sai định dạng cú pháp JSON, vi phạm Bean Validation (thiếu trường, sai kiểu dữ liệu).
* `401 Unauthorized`: Chưa xác thực (thiếu token, token hết hạn, token không hợp lệ).
* `403 Forbidden`: Đã xác thực nhưng tài khoản không có quyền thực hiện hành động này.
* `404 Not Found`: Không tìm thấy tài nguyên với định danh được yêu cầu.
* `409 Conflict`: Xung đột trạng thái hoặc trùng lặp dữ liệu duy nhất (ví dụ: email/số điện thoại đã tồn tại).
* `422 Unprocessable Entity`: Dữ liệu đúng cú pháp nhưng vi phạm quy tắc nghiệp vụ (ví dụ: số dư không đủ).
* `500 Internal Server Error`: Lỗi hệ thống nội bộ chưa lường trước (che giấu stack trace đối với client).

---

## 3. Cấu Trúc Dữ Liệu Phản Hồi Chuẩn (Standard Response Format)

Mọi phản hồi từ hệ thống phải tuân theo một envelope chuẩn thống nhất:

### 3.1. Phản Hồi Thành Công Đơn Lẻ (`ApiResponse<T>`)
```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": {
    "id": "c1a2b3c4-...",
    "email": "user@example.com",
    "fullName": "Nguyễn Văn A"
  },
  "timestamp": "2026-10-08T15:30:00Z"
}
```

### 3.2. Phản Hồi Danh Sách Phân Trang (`PageResponse<T>`)
```json
{
  "success": true,
  "message": "Lấy danh sách thành công",
  "data": {
    "items": [ ... ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8,
      "isFirst": true,
      "isLast": false
    }
  },
  "timestamp": "2026-10-08T15:30:00Z"
}
```

---

## 4. Chuẩn Hóa Phân Trang, Lọc & Sắp Xếp (Pagination & Sorting)

* **Tham số phân trang mặc định**:
  * `page`: Số trang (chuẩn Spring Data: bắt đầu từ `0`). Mặc định là `0`.
  * `size`: Kích thước trang. Mặc định là `20`.
  * **Giới hạn an toàn (Max page size)**: Bắt buộc chặn `size <= 100` để tránh tấn công làm cạn kiệt bộ nhớ máy chủ (DoS).
* **Tham số sắp xếp**: `sort=fieldName,asc` hoặc `sort=fieldName,desc`. Chỉ cho phép sắp xếp theo danh sách các trường hợp lệ được định nghĩa trước (tránh SQL injection hoặc lỗi query).

---

## 5. Tương Thích Ngược & Tiến Hóa API (Backward Compatibility)

* Không bao giờ xóa hoặc đổi tên trường đang có trong Response DTO trên cùng một phiên bản API.
* Nếu cần bổ sung thông tin mới: thêm trường mới với giá trị mặc định an toàn.
* Nếu cần thay đổi mang tính phá vỡ hợp đồng dữ liệu (Breaking Change): nâng cấp phiên bản lên `/api/v2/`.
