# Kiến Trúc Module & Ranh Giới Nghiệp Vụ (Module Architecture)

Tài liệu này xác định các quy tắc thiết kế, ranh giới độc lập và cơ chế giao tiếp giữa các module trong kiến trúc **Modular Monolith** của dự án.

---

## 1. Ranh Giới Độc Lập Của Module (Module Boundary)

Mỗi module nghiệp vụ (nằm trong `modules/<feature>/`) phải hoạt động như một thành phần có tính kết dính cao (High Cohesion) và ghép nối lỏng lẻo (Loose Coupling) với các module khác.

### Các nguyên tắc ranh giới bắt buộc:
1. **Không truy cập chéo Repository**: Module A tuyệt đối **KHÔNG ĐƯỢC** inject hoặc gọi trực tiếp `Repository` của Module B.
   * *Sai*: `OrderService` inject `UserRepository` để tìm thông tin user.
   * *Đúng*: `OrderService` inject `UserService` và gọi phương thức cung cấp public DTO của `UserService`.
2. **Không sửa đổi Entity của module khác**: Module A không được tạo, sửa hoặc xóa trực tiếp Entity thuộc quyền sở hữu của Module B. Mọi biến đổi dữ liệu phải thông qua Service của module chủ quản.
3. **Không rò rỉ JPA Entity ra ngoài module**: Khi Module B cần dữ liệu từ Module A, Module A phải trả về một DTO (hoặc Read-only projection / Value Object), không trả về Entity có liên kết JPA sống.

---

## 2. Các Hình Thức Giao Tiếp Giữa Các Module (Inter-Module Communication)

Trong Modular Monolith, có 2 cơ chế giao tiếp được cho phép:

### 2.1. Giao Tiếp Đồng Bộ Qua Service (Direct Service Call)
Áp dụng cho các trường hợp bắt buộc phải có dữ liệu ngay lập tức để hoàn thành luồng xử lý:

```text
[OrderController] → [OrderService] ──── (gọi UserService.getUserSummary(userId)) ────→ [UserService]
                                                                                            ↓
                                                                                     [UserRepository]
```

* **Quy tắc**:
  * Chỉ truyền và nhận DTOs không gắn liền với Hibernate Session.
  * Giữ ranh giới tham số đơn giản, tránh truyền đối tượng phức tạp không cần thiết.

### 2.2. Giao Tiếp Bất Đồng Bộ Qua Event (Spring ApplicationEvent)
Áp dụng khi hoàn thành một hành động chính và cần kích hoạt các hành động phụ mà không muốn làm chậm hoặc gắn kết chặt luồng chính:

* **Ví dụ**: Người dùng đăng ký thành công (`UserRegisteredEvent`) -> Gửi email chào mừng, cấp phát voucher tân thủ, ghi nhật ký kiểm toán.
* **Cách thực hiện**:
  * Module phát ra sự kiện: `ApplicationEventPublisher.publishEvent(new UserRegisteredEvent(userId, email))`.
  * Module lắng nghe sự kiện: Sử dụng `@EventListener` hoặc `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
* **Lợi ích**: Tách rời hoàn toàn sự phụ thuộc code giữa các module.

---

## 3. Chống Phụ Thuộc Vòng (No Circular Dependencies)

* **Quy tắc nghiêm ngặt**: Nghiêm cấm tuyệt đối phụ thuộc vòng giữa các module (ví dụ: `Module A -> Module B -> Module A`).
* Nếu phát hiện phụ thuộc vòng:
  1. Xem xét lại ranh giới trách nhiệm: Logic đó thực sự thuộc về module nào?
  2. Sử dụng sự kiện (Event-driven) để đảo ngược chiều phụ thuộc.
  3. Hoặc trích xuất một thành phần chung vào `common/` nếu nó là kiểu dữ liệu thuần túy không mang logic điều hướng.

```text
❌ SAI:
[AuthModule] ──phụ thuộc──> [UserModule]
     ▲                            │
     └────────phụ thuộc───────────┘

✅ ĐÚNG (Một chiều hoặc Event):
[AuthModule] ──gọi Service──> [UserModule]
[UserModule] ──publish Event──> [NotificationModule] (nghe Event)
```

---

## 4. Quản Lý Giao Dịch Xuyên Module (Cross-Module Transactions)

* **Giao dịch đơn nhất**: Trong Modular Monolith, khi Service A gọi Service B trong cùng một luồng, cả hai mặc định nằm trong cùng một cơ sở dữ liệu và cùng một Hibernate transaction (`Propagation.REQUIRED`).
* **Hạn chế giao dịch kéo dài (Long-running transactions)**: Không giữ transaction mở trong khi gọi API bên thứ ba (như cổng thanh toán, dịch vụ gửi mail, dịch vụ SMS).
* **Quy trình chuẩn cho các tác vụ bên ngoài**:
  1. Chuẩn bị dữ liệu và lưu trạng thái PENDING trong DB (trong transaction).
  2. Commit transaction.
  3. Gọi dịch vụ bên ngoài (ngoài transaction).
  4. Mở transaction mới để cập nhật trạng thái SUCCESS hoặc FAILED.
