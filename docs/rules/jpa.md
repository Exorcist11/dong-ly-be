# Quy Tắc Tối Ưu Hibernate & JPA (JPA Guidelines)

Tài liệu này xác định các tiêu chuẩn kỹ thuật khi làm việc với Spring Data JPA và Hibernate, đặc biệt tập trung vào việc ngăn chặn vấn đề hiệu năng và lỗi rò rỉ bộ nhớ.

---

## 1. Quy Tắc FetchType: Luôn Sử Dụng `LAZY` Cho Mọi Quan Hệ

Trong đặc tả chuẩn JPA:
* `@OneToMany` và `@ManyToMany` mặc định là `LAZY`.
* **NHƯNG** `@ManyToOne` và `@OneToOne` mặc định là **`EAGER`**.

> [!CAUTION]
> `EAGER` là nguyên nhân hàng đầu gây sụt giảm hiệu năng nghiêm trọng vì tự động thực hiện các câu lệnh JOIN hoặc SELECT phụ không mong muốn.
> **BẮT BUỘC**: Luôn khai báo tường minh `fetch = FetchType.LAZY` cho TẤT CẢ các quan hệ `@ManyToOne` và `@OneToOne`.

```java
// ❌ SAI: Ngầm định EAGER
@ManyToOne
private Category category;

// ✅ ĐÚNG: Tường minh LAZY
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id", nullable = false)
private Category category;
```

---

## 2. Ngăn Chặn Vấn Đề N+1 Truy Vấn (N+1 Query Prevention)

Vấn đề N+1 xảy ra khi lấy 1 danh sách N phần tử, sau đó Hibernate sinh ra thêm N câu lệnh SELECT riêng lẻ để lấy dữ liệu quan hệ Lazy.

### Các giải pháp bắt buộc áp dụng khi truy vấn dữ liệu liên kết:

#### Cách 1: Sử dụng `JOIN FETCH` trong câu truy vấn JPQL
```java
@Query("""
    SELECT o FROM Order o
    JOIN FETCH o.user
    JOIN FETCH o.orderItems
    WHERE o.status = :status
""")
List<Order> findAllByStatusWithDetails(@Param("status") OrderStatus status);
```

#### Cách 2: Sử dụng `@EntityGraph`
```java
@EntityGraph(attributePaths = {"user", "orderItems"})
@Query("SELECT o FROM Order o WHERE o.id = :id")
Optional<Order> findDetailById(@Param("id") UUID id);
```

#### Cách 3: Sử dụng DTO Projection Trực Tiếp (Tối ưu nhất cho API đọc)
Nếu chỉ cần hiển thị dữ liệu ra API và không cần chỉnh sửa Entity, hãy truy vấn thẳng vào Java Record để bỏ qua hoàn toàn overhead của Hibernate Entity State Tracking:
```java
@Query("""
    SELECT new com.company.project.modules.order.dto.OrderSummaryResponse(
        o.id, o.code, o.totalAmount, u.fullName, o.createdAt
    )
    FROM Order o JOIN o.user u
    WHERE o.status = :status
""")
Page<OrderSummaryResponse> findSummaries(@Param("status") OrderStatus status, Pageable pageable);
```

---

## 3. Cảnh Báo Lombok Khi Dùng Với JPA Entity

Lombok giúp giảm thiểu boilerplate nhưng dễ gây lỗi nghiêm trọng với JPA nếu dùng sai cách:

1. **TUYỆT ĐỐI CẤM** dùng `@Data` trên JPA Entity:
   * `@Data` tự động sinh ra `toString()`, `equals()` và `hashCode()` bao trùm toàn bộ các trường.
   * Khi quan hệ 2 chiều tồn tại, `toString()` sẽ gọi đệ quy lẫn nhau dẫn đến `StackOverflowError`.
2. **Khuyến nghị sử dụng**:
   * `@Getter`, `@Setter`
   * `@NoArgsConstructor(access = AccessLevel.PROTECTED)` (Hibernate yêu cầu no-args constructor)
   * `@AllArgsConstructor(access = AccessLevel.PRIVATE)` + `@Builder`
3. **Cấu hình `equals` và `hashCode`**:
   * Chỉ so sánh trên thuộc tính khóa chính tự nhiên (Business Key) hoặc sử dụng lớp `Objects.equals(getId(), other.getId())` sau khi đã persist.

---

## 4. Quản Lý Quan Hệ Hai Chiều (Bidirectional Relationships)

Khi sử dụng quan hệ 2 chiều (`@OneToMany` - `@ManyToOne`), bắt buộc phải có các phương thức trợ giúp (Helper methods) để đồng bộ cả hai chiều:

```java
@Entity
@Table(name = "orders")
public class Order {
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
}
```

---

## 5. Tối Ưu Hóa Ghi Dữ Liệu Hàng Loạt (Batch Insert/Update)

Trong cấu hình `application.yml`, bật JDBC Batching để gom nhiều lệnh INSERT/UPDATE thành 1 lượt truyền mạng:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
```
*Lưu ý: Nếu sử dụng chiến lược sinh ID dạng `GenerationType.IDENTITY`, Hibernate sẽ vô hiệu hóa batch insert. Khuyến nghị dùng UUID hoặc SEQUENCE.*
