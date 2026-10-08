# Tiêu Chuẩn Ghi Nhật Ký (Logging Guidelines)

Tài liệu này xác định các quy chuẩn ghi log hệ thống bằng SLF4J / Logback, bảo đảm khả năng giám sát (Observability), điều tra sự cố và tuân thủ an toàn thông tin.

---

## 1. Các Cấp Độ Ghi Log (Log Levels)

* **`ERROR`**:
  * Các lỗi nghiêm trọng khiến một tác vụ bị thất bại hoàn toàn.
  * Các lỗi ngoài dự kiến (500 Internal Server Error, mất kết nối Database, sập dịch vụ bên thứ ba).
  * Luôn ghi kèm đối tượng ngoại lệ (exception) để lưu lại stack trace.
* **`WARN`**:
  * Các tình huống bất thường nhưng hệ thống vẫn tự phục hồi hoặc xử lý an toàn được.
  * Lạm dụng API, chạm ngưỡng Rate Limiting, thư viện sắp bị khai tử (deprecated).
* **`INFO`**:
  * Các mốc quan trọng trong vòng đời hệ thống hoặc nghiệp vụ:
  * Khởi động ứng dụng thành công, hoàn thành Flyway migration.
  * Người dùng đăng ký tài khoản mới, đơn hàng được tạo, thanh toán thành công.
* **`DEBUG`**:
  * Thông tin chi tiết phục vụ lập trình viên gỡ lỗi trên môi trường `dev`.
  * Dữ liệu tham số truy vấn, thời gian thực thi của một hàm phức tạp.
  * *Tự động tắt trên môi trường Production*.
* **`TRACE`**: Chi tiết cực sâu, chỉ bật khi cần phân tích luồng mạng cấp thấp.

---

## 2. Cú Pháp Ghi Log Chuẩn (SLF4J)

* Sử dụng annotation `@Slf4j` của Lombok.
* Luôn sử dụng cú pháp giữ chỗ dạng ngoặc nhọn `{}` (Parameterized logging) thay vì cộng chuỗi bằng toán tử `+`:

```java
// ❌ SAI: Gây tốn bộ nhớ vì luôn cộng chuỗi kể cả khi cấp độ log bị tắt
log.debug("Người dùng " + userId + " đăng nhập từ IP " + clientIp);

// ✅ ĐÚNG: Nhanh, không lãng phí bộ nhớ
log.info("Người dùng {} đăng nhập thành công từ IP {}", userId, clientIp);

// ✅ ĐÚNG: Ghi log lỗi kèm Exception (đặt ngoại lệ ở tham số cuối cùng, không cần {})
log.error("Lỗi khi xử lý thanh toán cho đơn hàng {}: {}", orderId, ex.getMessage(), ex);
```

---

## 3. Nhật Ký Có Ngữ Cảnh Với MDC (Mapped Diagnostic Context)

Để liên kết toàn bộ các dòng log của cùng một HTTP request, sử dụng MDC filter để sinh mã truy vết (`traceId` / `requestId`):

```java
// Trong Filter tiếp nhận request:
MDC.put("traceId", UUID.randomUUID().toString());
MDC.put("userId", currentUserId != null ? currentUserId.toString() : "anonymous");
try {
    filterChain.doFilter(request, response);
} finally {
    MDC.clear(); // Bắt buộc xóa sạch sau khi kết thúc request để tránh rò rỉ luồng Tomcat
}
```

---

## 4. Danh Mục TUYỆT ĐỐI CẤM Ghi Log (Data Privacy & Masking)

Để tuân thủ các tiêu chuẩn bảo mật (OWASP, GDPR, PCI-DSS), **NGHIÊM CẤM GHI VÀO LOG**:

1. **Mật khẩu thuần hoặc mã OTP**: Cấm log `password`, `confirmPassword`, mã OTP gửi qua SMS/Email.
2. **Token xác thực**: Cấm log toàn bộ nội dung Access Token, Refresh Token, Bearer header.
3. **Thông tin thẻ tín dụng/ngân hàng**: Cấm log số thẻ (PAN), ngày hết hạn, mã CVV/CVC.
4. **Dữ liệu định danh cá nhân nhạy cảm (PII)**: Số căn cước công dân, hộ chiếu, mã số thuế.
5. **Khóa bí mật & Chuỗi kết nối**: Khóa API secret, `DATABASE_URL` chứa mật khẩu.

> [!WARNING]
> Nếu cần log request body để audit, bắt buộc phải qua bộ lọc che dữ liệu nhạy cảm (ví dụ: biến `"password": "secret"` thành `"password": "******"`).
