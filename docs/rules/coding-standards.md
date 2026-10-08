# Tiêu Chuẩn Viết Mã (Coding Standards)

Tài liệu này quy định các chuẩn mực viết mã Java áp dụng đồng nhất trong toàn bộ dự án.

---

## 1. Quy Tắc Đặt Tên (Naming Conventions)

* **Package**: Chữ thường hoàn toàn, ngắn gọn, danh từ số ít. Không dùng dấu gạch dưới hoặc camelCase.
  * *Ví dụ*: `com.company.project.modules.auth`, `com.company.project.modules.user`
* **Class & Interface & Record**: PascalCase, danh từ hoặc cụm danh từ mô tả chính xác vai trò.
  * *Ví dụ*: `UserController`, `AuthenticationService`, `CreateUserRequest`
* **Method**: camelCase, động từ hoặc cụm động từ mô tả hành động.
  * *Ví dụ*: `findById()`, `createUser()`, `calculateDiscountAmount()`
  * Tránh đặt tên vô nghĩa hoặc chung chung như `handle()`, `process()`, `doSomething()`.
* **Biến & Thuộc tính (Variable & Field)**: camelCase, tên có ý nghĩa rõ ràng.
  * Tránh đặt tên tắt 1-2 ký tự (như `u`, `tmp`, `rs`) trừ biến đếm vòng lặp ngắn (`i`, `j`).
* **Hằng số (Constant)**: `UPPER_SNAKE_CASE`, phải khai báo `public static final` hoặc `private static final`.
  * *Ví dụ*: `DEFAULT_PAGE_SIZE`, `MAX_LOGIN_ATTEMPTS`
* **Enum**: Tên enum là PascalCase, các giá trị enum là `UPPER_SNAKE_CASE`.
  * *Ví dụ*: `OrderStatus { PENDING, PROCESSING, COMPLETED, CANCELLED }`

---

## 2. Kích Thước Và Trách Nhiệm Của Lớp & Phương Thức (Class & Method Size)

* **Single Responsibility Principle (SRP)**: Mỗi lớp chỉ nên có một lý do duy nhất để thay đổi.
* **Kích thước phương thức**: Một phương thức nên nằm trong khoảng 15-30 dòng code. Nếu dài hơn, phải xem xét tách thành các phương thức private phụ trợ với tên gọi rõ nghĩa.
* **Kích thước lớp**: Một lớp không nên vượt quá 300-400 dòng code. Nếu quá dài, lớp đó đang ôm đồm quá nhiều trách nhiệm.
* **Mức độ lồng nhau (Nesting depth)**: Tối đa 2-3 cấp thụt lề (`if`, `for`). Ưu tiên sử dụng kỹ thuật **Guard Clauses** (trả về sớm / Early Return) để làm phẳng luồng xử lý:

```java
// ❌ TRÁNH: Lồng nhiều tầng
public void processOrder(Order order) {
    if (order != null) {
        if (order.isValid()) {
            if (order.isPaid()) {
                // xử lý đơn hàng
            }
        }
    }
}

// ✅ NÊN: Guard Clauses (Early Return)
public void processOrder(Order order) {
    if (order == null || !order.isValid()) {
        throw new InvalidOrderException("Đơn hàng không hợp lệ");
    }
    if (!order.isPaid()) {
        throw new OrderNotPaidException("Đơn hàng chưa thanh toán");
    }
    // Xử lý đơn hàng trực diện tại đây
}
```

---

## 3. Dependency Injection: Luôn Sử Dụng Constructor Injection

* **BẮT BUỘC**: Sử dụng Constructor Injection (khuyến khích dùng `@RequiredArgsConstructor` của Lombok hoặc khai báo constructor tường minh).
* **TUYỆT ĐỐI CẤM**: Field injection bằng `@Autowired` trực tiếp trên trường thuộc tính.
  * *Lý do*: Field injection gây khó khăn cho Unit Test, giấu kín sự phụ thuộc và vi phạm tính bất biến (immutability).

```java
// ❌ CẤM: Field injection
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
}

// ✅ CHUẨN: Constructor injection với Lombok
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
}
```

---

## 4. Tính Bất Biến (Immutability) & Java Records

* Sử dụng từ khóa `final` cho các trường dependencies trong Service và Controller.
* Với DTOs (Request & Response) và Value Objects, **BẮT BUỘC sử dụng Java Record**:
  * Tự động cung cấp tính bất biến (immutable), `equals()`, `hashCode()`, `toString()`.
  * Tránh mã boilerplate.

```java
public record CreateUserRequest(
    @NotBlank(message = "Tên không được để trống")
    String fullName,

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    String email,

    @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
    String password
) {}
```

---

## 5. Xử Lý Null & Sử Dụng `Optional` Đúng Cách

* **Không bao giờ trả về `null` cho Collection/Array**: Trả về `Collections.emptyList()`, `Collections.emptySet()`, hoặc `List.of()`.
* **Sử dụng `Optional`**:
  * `Optional` CHỈ được dùng làm kiểu trả về của phương thức khi giá trị có thể không tồn tại (đặc biệt trong Repository hoặc lookup service).
  * **CẤM** sử dụng `Optional` làm tham số đầu vào của phương thức (Method parameter).
  * **CẤM** sử dụng `Optional` làm kiểu dữ liệu của thuộc tính lớp (Class field).
  * Tránh gọi trực tiếp `.get()` mà không kiểm tra `.isPresent()`. Hãy dùng `.orElseThrow()`, `.orElse()`, `.map()`, hoặc `.ifPresent()`.

```java
// ❌ TRÁNH:
Optional<User> userOpt = userRepository.findById(id);
if (userOpt.isPresent()) {
    User user = userOpt.get();
}

// ✅ NÊN:
User user = userRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + id));
```

---

## 6. Collections & Java Stream API

* Sử dụng Stream API khi cần chuyển đổi (transform), lọc (filter) hoặc thu thập dữ liệu một cách khai báo.
* Không lạm dụng Stream cho các logic quá phức tạp hoặc có tác dụng phụ (side-effects). Không lồng Stream bên trong Stream nếu có thể viết đơn giản hơn.
* Tránh gọi truy vấn database bên trong vòng lặp Stream (gây lỗi N+1).

---

## 7. Ưu Tiên Composition Thay Vì Kế Thừa (Favor Composition Over Inheritance)

* Hạn chế tối đa việc tạo các lớp cha trừu tượng dày đặc (như `BaseService`, `GenericManager`, `BaseController`).
* Sử dụng Composition (ghép nối đối tượng và ủy quyền trách nhiệm) để tái sử dụng logic thay vì kế thừa lớp.
* Chỉ sử dụng Interface khi:
  1. Có nhiều hơn một cách cài đặt trong thực tế (đa hình runtime, ví dụ: các cổng thanh toán `PaymentGateway`).
  2. Cần phân tách ranh giới module rõ ràng.
* **KHÔNG** tạo Interface cho Service một cách mù quáng chỉ vì thói quen hình thức.
