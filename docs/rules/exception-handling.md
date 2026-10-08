# Xử Lý Ngoại Lệ Tập Trung (Exception Handling)

Tài liệu này xác định cơ chế xử lý ngoại lệ thống nhất cho toàn bộ hệ thống, bảo đảm tính an toàn thông tin và trải nghiệm đồng bộ cho client tích hợp.

---

## 1. Cơ Chế Xử Lý Tập Trung Với `@RestControllerAdvice`

* Toàn bộ ngoại lệ phát sinh từ Controller, Service hoặc Repository phải được bắt và xử lý tập trung tại một lớp `@RestControllerAdvice` trong package `common.exception`.
* **CẤM** lập trình viên tự ý bắt ngoại lệ bằng khối `try-catch` trong Controller rồi trả về mã lỗi tùy tiện.

---

## 2. Cấu Trúc Phản Hồi Lỗi Chuẩn (Standard Error Response)

Mọi phản hồi lỗi trả về cho client phải tuân theo cấu trúc JSON duy nhất:

```json
{
  "timestamp": "2026-10-08T15:30:00Z",
  "status": 400,
  "code": "USER_EMAIL_EXISTS",
  "message": "Email đã được sử dụng trong hệ thống",
  "path": "/api/v1/users",
  "errors": [
    {
      "field": "email",
      "rejectedValue": "test@example.com",
      "message": "Email đã được sử dụng"
    }
  ]
}
```

* **`status`**: Mã trạng thái HTTP (400, 401, 403, 404, 409, 422, 500).
* **`code`**: Mã định danh lỗi ổn định (Business Error Code) bằng chữ in hoa gạch dưới (ví dụ: `USER_NOT_FOUND`, `INVALID_PASSWORD`), giúp frontend dễ dàng ánh xạ thông báo đa ngôn ngữ.
* **`message`**: Thông điệp tóm tắt thân thiện với người dùng (viết bằng tiếng Việt).
* **`path`**: URI của yêu cầu gặp lỗi.
* **`errors`**: Danh sách chi tiết lỗi theo từng trường (đặc biệt khi vi phạm Bean Validation). Có thể `null` nếu là lỗi đơn lẻ.

---

## 3. Hệ Thống Ngoại Lệ Nghiệp Vụ (Custom Domain Exceptions)

Xây dựng hệ thống Exception phân cấp kế thừa từ `RuntimeException`:

```java
// Base Exception cho toàn bộ lỗi nghiệp vụ của ứng dụng
@Getter
public class AppException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public AppException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}

// Các ngoại lệ cụ thể thường dùng:
public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }
}

public class BusinessRuleException extends AppException {
    public BusinessRuleException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
```

---

## 4. Xử Lý Các Loại Ngoại Lệ Cốt Lõi

Bộ `GlobalExceptionHandler` bắt buộc xử lý tối thiểu các nhóm sau:

1. **`MethodArgumentNotValidException`**: Bắt lỗi vi phạm `@Valid` từ Request DTO. Trả về `400 Bad Request` kèm chi tiết mảng `errors`.
2. **`AppException` & các lớp con**: Xử lý lỗi nghiệp vụ chủ động. Trả về đúng `httpStatus` và `errorCode` đã được định nghĩa.
3. **`AccessDeniedException`**: Bắt lỗi từ Spring Security. Trả về `403 Forbidden` với mã `ACCESS_DENIED`.
4. **`AuthenticationException`**: Bắt lỗi xác thực. Trả về `401 Unauthorized` với mã `UNAUTHORIZED`.
5. **`Exception.class` (Fallback tổng quát)**:
   * Bắt toàn bộ lỗi ngoài dự kiến (NPE, lỗi kết nối DB, v.v.).
   * Ghi log đầy đủ stack trace vào file log nội bộ (với mức `ERROR`).
   * Trả về cho client `500 Internal Server Error` với thông điệp an toàn: *"Đã xảy ra lỗi hệ thống nội bộ. Vui lòng thử lại sau."*

---

## 5. Tuyệt Đối Bảo Mật Thông Tin Khi Xử Lý Lỗi (Security Rules)

### ❌ TUYỆT ĐỐI CẤM để lộ trong phản hồi API:
* **Stack trace chi tiết**: Không bao giờ gửi stack trace của Java cho client.
* **Chi tiết SQL hoặc cấu trúc bảng**: Không để lộ tên bảng, câu lệnh `SELECT/INSERT` bị lỗi, hoặc cú pháp PostgreSQL.
* **Thông tin cấu hình máy chủ / cơ sở dữ liệu**: Tên host, địa chỉ IP, tên tài khoản DB, khóa bí mật.

> [!IMPORTANT]
> Trong file cấu hình `application.yml`, bắt buộc thiết lập:
> ```yaml
> server:
>   error:
>     include-stacktrace: never
>     include-message: never
> ```
