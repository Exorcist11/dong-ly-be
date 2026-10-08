# Quy Tắc Clean Code & Tránh Over-Engineering (Clean Code)

Tài liệu này xác định các tiêu chí giữ cho mã nguồn luôn sạch sẽ, dễ hiểu, dễ kiểm thử và ngăn chặn tình trạng kỹ thuật phức tạp hóa vấn đề (Over-Engineering).

---

## 1. Các Nguyên Lý Thiết Kế Nền Tảng

1. **KISS (Keep It Simple, Stupid)**: Giải pháp tốt nhất là giải pháp đơn giản nhất đáp ứng đầy đủ và an toàn yêu cầu bài toán.
2. **YAGNI (You Aren't Gonna Need It)**: Không viết mã đón đầu tương lai viển vông. Chỉ cài đặt những gì hệ thống thực sự cần ở thời điểm hiện tại.
3. **DRY (Don't Repeat Yourself) một cách thực dụng**:
   * Tránh lặp lại logic nghiệp vụ cốt lõi hoặc công thức tính toán.
   * *Lưu ý*: Sự trùng hợp ngẫu nhiên về cấu trúc giữa 2 DTO hoặc 2 màn hình không đồng nghĩa với việc chúng phải dùng chung một class. Trùng lặp mã nhỏ tốt hơn là ghép sai abstraction (Premature Abstraction).
4. **Boy Scout Rule**: Luôn để lại vùng mã nguồn bạn vừa chạm vào sạch hơn lúc bạn tìm thấy nó một chút (chỉnh sửa tên biến rõ nghĩa hơn, bỏ import thừa, xóa code chết).

---

## 2. Ngăn Chặn Over-Engineering (Anti-Patterns Cần Tránh)

### 2.1. Lạm Dụng Abstraction Vô Nghĩa
* **Hiện tượng**: Tạo Interface cho mọi Service, tạo `BaseController`, `BaseService`, `GenericRepositoryImpl`, `AbstractStrategyProcessorFactory`.
* **Hậu quả**: Để đọc một luồng xử lý đơn giản, lập trình viên phải nhảy qua 5 tầng file trung gian mà không thu lại bất kỳ giá trị đa hình hay đóng gói nào.
* **Quy chuẩn**: Không tạo Interface nếu không có ít nhất 2 implementation thực tế hoặc lý do ranh giới module rõ rệt.

### 2.2. Lớp Thần Thánh (God Class) & Phương Thức Quái Vật (Monster Method)
* Không gom toàn bộ nghiệp vụ của 5 chức năng vào một phương thức dài 200 dòng.
* Tách các bước phụ trợ thành các private method rõ nghĩa để phương thức chính đọc giống như một bản tóm tắt quy trình:

```java
// ✅ ĐỌC GIỐNG MỘT BẢN KỊCH BẢN RÕ NGHĨA:
@Transactional
public OrderResponse checkout(CheckoutRequest request) {
    User user = findUserOrThrow(request.userId());
    List<CartItem> items = validateAndLockCartItems(request.cartItemIds());
    BigDecimal totalAmount = calculateTotalPrice(items, request.couponCode());
    
    Order order = createPendingOrder(user, items, totalAmount);
    paymentService.initiatePayment(order);
    
    eventPublisher.publishEvent(new OrderCreatedEvent(order.getId()));
    return OrderMapper.toResponse(order);
}
```

### 2.3. Bãi Rác "Utils" & "Helper"
* Tránh tạo các lớp như `CommonUtils`, `GeneralHelper`, `BaseHelper` rồi nhét mọi hàm linh tinh không biết để đâu vào đó.
* Xem quy chuẩn chi tiết tại [`docs/rules/reusability.md`](file:///e:/du-an-ma/Spring-BE/docs/rules/reusability.md).

---

## 3. Mã Tự Thuyết Minh (Self-Documenting Code) & Comment

* Mã nguồn tốt tự giải thích được chính nó thông qua tên biến, tên hàm và luồng dữ liệu rõ ràng.
* **Quy tắc viết Comment**:
  * **KHÔNG** viết comment giải thích *Mã đang làm gì* (What the code does) khi tên hàm đã nói lên điều đó:
    ```java
    // ❌ VÔ NGHĨA:
    // Tăng i lên 1
    i++;
    // Tìm người dùng bằng email
    userRepository.findByEmail(email);
    ```
  * **CHỈ** viết comment giải thích *Tại sao lại làm như vậy* (Why the code does it) - giải thích các quyết định kiến trúc đặc thù, quy định pháp lý, thuật toán phức tạp hoặc xử lý bug biên đặc biệt:
    ```java
    // ✅ HỮU ÍCH:
    // Cần trừ thêm thuế nhà thầu FCT 5% theo quy định thông tư 103/2014/TT-BTC đối với nhà cung cấp nước ngoài
    BigDecimal taxAmount = subtotal.multiply(FCT_TAX_RATE);
    ```

---

## 4. Xóa Bỏ Code Chết & Code Bị Comment (Dead Code)

* **TUYỆT ĐỐI KHÔNG** giữ lại code bị comment (`// doSomethingOld();`) trong commit hoặc file nguồn. Git đã có lịch sử lưu vết, hãy tự tin xóa bỏ code thừa.
* Xóa các biến, phương thức private hoặc import không còn được sử dụng ở bất kỳ đâu.
* Loại bỏ các giá trị hằng số ma thuật (Magic numbers/strings). Thay vào đó, đặt tên hằng số rõ ràng:
  ```java
  // ❌ TRÁNH: if (status == 3) ...
  // ✅ NÊN:   if (status == OrderStatus.DELIVERED) ...
  ```
