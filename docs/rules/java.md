# Quy Tắc Sử Dụng Java Hiện Đại (Java Guidelines)

Tài liệu này xác định các quy chuẩn kỹ thuật khi làm việc với ngôn ngữ Java (phiên bản chuẩn hóa **Java 21 LTS**) trong dự án.

---

## 1. Tận Dụng Các Tính Năng Java Hiện Đại

### 1.1. Java Records Cho Dữ Liệu Bất Biến (DTOs, Value Objects)
* Dùng `record` thay cho class thông thường khi khai báo Request DTO, Response DTO, và Event payload.
* Record tự động tạo constructor, getters, `equals()`, `hashCode()`, và `toString()`.

```java
// Khai báo DTO bằng record ngắn gọn, an toàn
public record UserSummaryResponse(
    UUID id,
    String email,
    String fullName,
    Instant createdAt
) {}
```

### 1.2. Pattern Matching & Switch Expressions
* Sử dụng Pattern Matching cho `instanceof` để loại bỏ việc ép kiểu (cast) thủ công:
```java
// ✅ NÊN
if (event instanceof OrderPaidEvent orderPaid) {
    processShipping(orderPaid.orderId());
}
```
* Sử dụng Switch Expression dạng mũi tên (`->`) thay cho cú pháp switch truyền thống nhiều lệnh `break`:
```java
String statusLabel = switch (status) {
    case PENDING -> "Chờ xử lý";
    case PROCESSING -> "Đang xử lý";
    case COMPLETED -> "Hoàn thành";
    case CANCELLED -> "Đã hủy";
};
```

### 1.3. Text Blocks Cho Chuỗi Nhiều Dòng
* Sử dụng cú pháp Text Blocks (`"""`) cho các chuỗi SQL Native, JSON mẫu, hoặc HTML template:
```java
String query = """
    SELECT u.id, u.email, u.full_name
    FROM users u
    WHERE u.status = :status AND u.created_at >= :since
    ORDER BY u.created_at DESC
    """;
```

### 1.4. Quy Tắc Sử Dụng Từ Khóa `var` (Local Variable Type Inference)
* **CHỈ SỬ DỤNG** `var` khi kiểu dữ liệu ở vế phải hoàn toàn tường minh:
  ```java
  var user = new User();
  var usersList = new ArrayList<User>();
  ```
* **KHÔNG DÙNG** `var` khi vế phải là kết quả trả về của hàm không rõ kiểu dữ liệu, làm giảm tính dễ đọc:
  ```java
  // ❌ TRÁNH: Người đọc không biết result là gì
  var result = paymentService.execute(req);

  // ✅ NÊN: Rõ ràng kiểu trả về
  PaymentResult result = paymentService.execute(req);
  ```

---

## 2. Tiền Tệ & Phép Tính Tài Chính (Money & Calculations)

* **TUYỆT ĐỐI KHÔNG** sử dụng `float` hoặc `double` cho các giá trị tiền tệ, đơn giá, phần trăm chiết khấu hoặc số dư tài khoản vì sai số dấu phẩy động (floating-point rounding errors).
* **BẮT BUỘC** sử dụng `BigDecimal` cho mọi phép tính tài chính.
* Luôn xác định rõ ràng quy tắc làm tròn (`RoundingMode.HALF_UP`) và số chữ số thập phân (`scale`):
```java
BigDecimal unitPrice = new BigDecimal("199000.00");
BigDecimal quantity = BigDecimal.valueOf(3);
BigDecimal total = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
```

---

## 3. Quản Lý Thời Gian: Java Date-Time API (`java.time`)

* **NGHIÊM CẤM** sử dụng các lớp cổ xưa: `java.util.Date`, `java.util.Calendar`, `java.sql.Date`, `java.text.SimpleDateFormat`.
* **BẮT BUỘC** sử dụng chuẩn `java.time`:
  * Thời điểm tuyệt đối trên dòng thời gian: `Instant` (khuyến khích lưu trữ trong Database theo UTC).
  * Ngày giờ không có múi giờ: `LocalDateTime` (khi ngữ cảnh đã ngầm định thời gian cục bộ).
  * Chỉ ngày: `LocalDate` (ví dụ: ngày sinh, ngày phát hành hóa đơn).
  * Chỉ giờ: `LocalTime`.
  * Có kèm múi giờ/độ lệch: `OffsetDateTime` hoặc `ZonedDateTime`.
* Sử dụng `Clock` inject vào Service nếu cần test thời gian giả lập (`Clock.fixed()`).

---

## 4. Xử Lý Chuỗi & Collections Bất Biến

* **Tạo Collection bất biến**:
  * Sử dụng `List.of()`, `Set.of()`, `Map.of()` thay cho `Arrays.asList()` hoặc tạo danh sách có thể sửa đổi bất cẩn.
  * Trong Stream API: Dùng `.toList()` (từ Java 16+) thay vì `.collect(Collectors.toList())`.
* **Nối chuỗi trong vòng lặp**: Luôn dùng `StringBuilder` bên trong các vòng lặp lớn, không dùng toán tử cộng chuỗi `+` làm phát sinh nhiều đối tượng bộ nhớ tạm rác.

---

## 5. An Toàn Luồng (Thread Safety)

* Ứng dụng Spring Boot chạy trên môi trường đa luồng (Multi-threaded servlet container / Tomcat).
* Tránh khai báo biến trạng thái có thể thay đổi (`mutable static fields`) trong bất kỳ Service, Controller hay Helper class nào.
* Các dependencies inject vào Service phải là stateless.

---

## 6. Tính Năng Đặc Thù Của Java 21 LTS

### 6.1. Virtual Threads (Project Loom)
* Java 21 giới thiệu Virtual Threads giúp xử lý hàng chục ngàn kết nối I/O đồng thời mà không tốn kém tài nguyên OS Thread.
* Trong Spring Boot 3.2+, kích hoạt Virtual Threads cực kỳ đơn giản qua cấu hình:
  ```yaml
  spring:
    threads:
      virtual:
        enabled: true
  ```
* **Lưu ý**: Khi dùng Virtual Threads, tránh dùng khối `synchronized` cho các tác vụ I/O nặng (thay bằng `ReentrantLock` nếu cần) để không gây hiện tượng "thread pinning".

### 6.2. Sequenced Collections
* Tận dụng các phương thức có thứ tự trực tiếp của Java 21 thay vì thao tác chỉ mục thủ công:
  * `list.getFirst()`, `list.getLast()`
  * `list.addFirst()`, `list.addLast()`
  * `list.reversed()`
