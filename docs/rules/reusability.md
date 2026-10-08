# Quy Tắc Tái Sử Dụng Mã Nguồn (Code Reusability)

Tài liệu này xác định quy trình và tiêu chí đánh giá khi tái sử dụng, chia sẻ mã nguồn và trích xuất logic dùng chung trong dự án.

---

## 1. Quy Trình Bắt Buộc Trước Khi Viết Code Mới (Search-Before-Write)

Trước khi tạo mới bất kỳ hàm tiện ích, component hay logic xử lý nào, lập trình viên hoặc AI Agent **BẮT BUỘC** thực hiện 5 bước sau:

```text
1. Tìm kiếm trong repository (Grep / Search existing codebase)
    ↓
2. Nhận diện các đoạn mã hoặc component có chức năng tương tự
    ↓
3. Đánh giá xem có thể tái sử dụng trực tiếp hoặc mở rộng an toàn không
    ↓
4. Tái sử dụng nếu phù hợp và không vi phạm ranh giới module
    ↓
5. Chỉ trích xuất thành logic dùng chung khi có sự trùng lặp thực tế có ý nghĩa
```

---

## 2. Quy Tắc Số Ba (Rule of Three)

* **Lần thứ nhất**: Bạn viết code để giải quyết vấn đề cụ thể trong module hiện tại.
* **Lần thứ hai**: Bạn gặp bài toán tương tự trong module khác. Hãy chấp nhận lặp lại một đoạn code ngắn nếu logic có nguy cơ rẽ nhánh khác nhau trong tương lai. **Đừng vội vàng trích xuất abstraction**.
* **Lần thứ ba**: Bạn gặp bài toán tương tự lần thứ ba và logic hoàn toàn đồng nhất về bản chất. **LÚC NÀY** mới tiến hành tái cấu trúc để trích xuất hàm hoặc lớp dùng chung.

> *"Trùng lặp nhỏ tốt hơn rất nhiều so với một Abstraction sai lầm (Wrong Abstraction)."*
> Việc ghép nối gượng ép hai logic chỉ vì chúng "trông giống nhau" sẽ tạo ra sự phụ thuộc chặt (tight coupling) tai hại khi một bên cần thay đổi.

---

## 3. Ngăn Chặn Các "Bãi Rác" Chứa Code Dùng Chung

### ❌ CẤM các lớp vô định hình (Dumping Grounds):
Nghiêm cấm tạo hoặc nhồi nhét code vào các lớp có tên chung chung sau:
* `Utils` / `CommonUtils` / `GlobalHelper`
* `CommonService` / `BaseService` / `GenericService`
* `MiscService` / `AppHelper`

### ✅ NÊN: Tạo các thành phần có trách nhiệm chuyên biệt rõ ràng:
Nếu cần trích xuất tiện ích kỹ thuật, hãy đặt tên theo chính xác chức năng đơn lẻ của nó:
* `PasswordHasher`: Chuyên băm và kiểm tra mật khẩu.
* `JwtTokenProvider`: Chuyên sinh và thẩm định JWT token.
* `SlugGenerator`: Chuyên tạo URL slug tiếng Việt chuẩn.
* `CsvExportUtil`: Chuyên xử lý định dạng xuất file CSV.

---

## 4. Tiêu Chí Trích Xuất Code Vào Thư Mục `common/`

Một đoạn mã chỉ được phép đưa vào `src/main/java/.../common/` khi thỏa mãn đồng thời cả 3 điều kiện:

1. **Phi nghiệp vụ (Domain-agnostic)**: Không phụ thuộc vào bất kỳ bảng cơ sở dữ liệu hoặc logic nghiệp vụ cụ thể nào của dự án (ví dụ: wrapper chuẩn cho API Response, bộ lọc Security, Exception chung).
2. **Không gây phá vỡ (Non-breaking)**: Thay đổi của một module tiêu thụ không làm ảnh hưởng đến các module khác.
3. **Ổn định cao (High stability)**: Mã nguồn rất hiếm khi cần sửa đổi sau khi đã hoàn thiện và được kiểm thử kỹ lưỡng.

Nếu một logic chỉ dùng chung giữa `OrderModule` và `PaymentModule`, hãy giao tiếp qua Service công khai hoặc Event của module liên quan, **KHÔNG** vội ném vào `common/`.
