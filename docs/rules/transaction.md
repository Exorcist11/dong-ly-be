# Quản Lý Giao Dịch (Transaction Management)

Tài liệu này xác định các quy tắc quản lý ranh giới giao dịch (Transaction Boundaries) bằng annotation `@Transactional` trong Spring Boot.

---

## 1. Vị Trí Đặt Annotation `@Transactional`

* **ĐẶT TẠI**: Tầng **Service** (`@Service`). Đây là nơi nắm giữ logic nghiệp vụ và điều phối nhiều thao tác dữ liệu cần tính nguyên tử (Atomicity).
* **TUYỆT ĐỐI KHÔNG ĐẶT TẠI**:
  * ❌ Tầng **Controller**: Giao dịch bị mở quá sớm ngay khi tiếp nhận HTTP request và bị giữ mở cho đến khi response hoàn tất, gây chiếm dụng kết nối database (connection starvation).
  * ❌ Tầng **Repository**: Spring Data JPA đã tự động quản lý transaction ở cấp độ truy vấn đơn lẻ; đặt thêm ở đây là dư thừa.

---

## 2. Chiến Lược Đọc Dữ Liệu: `@Transactional(readOnly = true)`

Mặc định khai báo `@Transactional(readOnly = true)` ở cấp độ toàn bộ class Service. Với các phương thức ghi (Tạo, Sửa, Xóa), ghi đè bằng `@Transactional`:

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Mặc định toàn bộ hàm đọc tối ưu
public class UserService {

    private final UserRepository userRepository;

    // Kế thừa readOnly = true: Hibernate bỏ qua kiểm tra Dirty Checking,
    // tối ưu bộ nhớ và cho phép database định tuyến tới Replica nếu có.
    public UserResponse getUserById(UUID id) {
        // ...
    }

    // Ghi đè: Mở giao dịch Đọc-Ghi
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // ...
    }
}
```

---

## 3. Bẫy Tự Gọi Phương Thức Cùng Class (Self-Invocation Pitfall)

Spring quản lý Transaction thông qua cơ chế Dynamic Proxy:
* Khi phương thức A gọi phương thức B **trong cùng một lớp** Service, lời gọi này đi trực tiếp qua con trỏ `this`, **hoàn toàn bỏ qua Spring Proxy**.
* Kết quả: Annotation `@Transactional` trên phương thức B sẽ **KHÔNG ĐƯỢC KÍCH HOẠT**.

```java
@Service
public class OrderService {

    public void processAll() {
        // ❌ SAI LẦM: updateOrder() sẽ CHẠY KHÔNG CÓ TRANSACTION!
        this.updateOrder();
    }

    @Transactional
    public void updateOrder() {
        // ...
    }
}
```

* **Khắc phục**: Tách phương thức cần mở transaction độc lập sang một Service bean khác hoặc thiết kế luồng gọi từ ngoài vào.

---

## 4. Cơ Chế Rollback Ngoại Lệ (Rollback Rules)

* **Mặc định của Spring**: Chỉ tự động rollback khi phát sinh **Unchecked Exception** (`RuntimeException` hoặc `Error`). Các checked exception (`Exception`, `IOException`) sẽ **KHÔNG** làm rollback transaction!
* **Quy chuẩn dự án**:
  * Luôn sử dụng unchecked exceptions cho lỗi nghiệp vụ (`AppException` kế thừa từ `RuntimeException`).
  * Nếu bắt buộc phải xử lý checked exception, phải khai báo tường minh:
    ```java
    @Transactional(rollbackFor = Exception.class)
    ```

---

## 5. Rút Ngắn Thời Gian Giao Dịch (Keep Transactions Short)

Giữ kết nối database càng ngắn càng tốt để giảm nguy cơ nghẽn Connection Pool (đặc biệt quan trọng với Neon Serverless):

1. **Tuyệt đối không gọi API bên thứ ba bên trong `@Transactional`**:
   * Nếu API bên ngoài mất 5 giây phản hồi, kết nối database sẽ bị treo trong 5 giây đó.
2. **Quy trình chuẩn khi có tích hợp bên thứ ba**:
   ```text
   Bước 1: [Transaction 1] Ghi trạng thái đơn hàng PENDING vào Database.
   Bước 2: [Ngoài Transaction] Gọi API Cổng thanh toán (VNPAY / Momo / Stripe).
   Bước 3: [Transaction 2] Cập nhật kết quả thanh toán SUCCESS/FAILED vào Database.
   ```

---

## 6. Xử Lý Xung Đột Đồng Thời (Optimistic Locking)

Với các thực thể có nguy cơ cập nhật đồng thời cao (như số lượng tồn kho sản phẩm, số dư ví):
* Bổ sung cột `@Version private Long version;` vào Entity.
* Khi có 2 luồng cùng ghi đè, luồng đến sau sẽ ném ra `OptimisticLockingFailureException`.
* Bắt ngoại lệ này và thông báo cho người dùng thực hiện lại thao tác, bảo đảm không bị ghi đè mất mát dữ liệu (Lost Update).
