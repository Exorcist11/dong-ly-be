# AGENTS.md - Hướng Dẫn Dành Cho AI Agent

Tài liệu này là **điểm bắt đầu bắt buộc** (entry point) cho mọi AI Agent hoặc lập trình viên khi tiếp nhận, phân tích, lập kế hoạch, phát triển, sửa lỗi hoặc tái cấu trúc mã nguồn trong repository này.

---

## 1. Giới Thiệu Dự Án

* **Loại dự án**: Backend RESTful API phục vụ ứng dụng sản phẩm.
* **Ngôn ngữ & Nền tảng**: Java 21 LTS, Spring Boot 3.x, Maven.
* **Cơ sở dữ liệu**: PostgreSQL (môi trường sản xuất: Neon Serverless PostgreSQL), Flyway migration.
* **Hạ tầng & Vận hành**: Docker, triển khai trên Render.
* **Kiến trúc chủ đạo**: **Modular Monolith** kết hợp tổ chức theo tính năng (Feature-oriented / Package-by-feature) và kiến trúc phân lớp thực dụng (Pragmatic Layered Architecture).
* **Triết lý cốt lõi**: Đơn giản, thực dụng, an toàn, có khả năng mở rộng nhưng **tuyệt đối không over-engineer**. Chỉ tạo abstraction khi thực sự mang lại giá trị rõ ràng.

---

## 2. Quy Trình Làm Việc Tiêu Chuẩn Của AI Agent

Trước khi thực hiện bất kỳ hành động viết hay sửa code nào, Agent **BẮT BUỘC** tuân thủ 12 bước sau:

```text
1. Đọc AGENTS.md (tài liệu này)
    ↓
2. Đọc docs/README.md (sơ đồ chỉ dẫn toàn diện)
    ↓
3. Xác định các quy tắc liên quan đến task hiện tại
    ↓
4. Đọc tài liệu quy tắc tương ứng trong docs/rules/
    ↓
5. Đọc quy trình tương ứng trong docs/workflows/
    ↓
6. Khảo sát mã nguồn hiện có (Inspect existing code)
    ↓
7. Tìm kiếm mã nguồn có thể tái sử dụng (Search reusable code)
    ↓
8. Lập kế hoạch triển khai chi tiết cho các task không tầm thường (Implementation plan)
    ↓
9. Viết giải pháp nhỏ nhất, chính xác nhất (Smallest correct solution)
    ↓
10. Chạy kiểm thử / build thực tế (Run tests / build)
    ↓
11. Tự review lại thay đổi (Review changes & diff)
    ↓
12. Chuẩn bị thông điệp Git commit bằng TIẾNG VIỆT
```

> [!IMPORTANT]
> **Không đọc tràn lan toàn bộ tài liệu**: Agent chỉ đọc các tài liệu thực sự liên quan đến ngữ cảnh của task để tối ưu hiệu quả và ngữ cảnh làm việc.

---

## 3. Chỉ Dẫn Đọc Tài Liệu Theo Loại Task

| Loại công việc | Tài liệu Quy tắc (Rules) bắt buộc đọc | Quy trình (Workflows) |
| :--- | :--- | :--- |
| **Phát triển tính năng API mới** | `docs/rules/api.md`<br>`docs/rules/dto.md`<br>`docs/rules/spring-boot.md`<br>`docs/rules/validation.md` | `docs/workflows/feature-development.md` |
| **Thao tác Database / Entity / JPA** | `docs/rules/database.md`<br>`docs/rules/jpa.md`<br>`docs/rules/transaction.md` | `docs/workflows/feature-development.md` |
| **Xử lý Xác thực & Phân quyền (Auth)** | `docs/rules/security.md`<br>`docs/rules/configuration.md` | `docs/workflows/feature-development.md` |
| **Sửa lỗi (Bug Fix)** | `docs/rules/clean-code.md`<br>`docs/rules/logging.md`<br>`docs/rules/testing.md` | `docs/workflows/bug-fix.md` |
| **Tái cấu trúc mã nguồn (Refactoring)** | `docs/rules/clean-code.md`<br>`docs/rules/reusability.md`<br>`docs/rules/coding-standards.md` | `docs/workflows/refactoring.md` |
| **Review mã nguồn (Code Review)** | Toàn bộ `docs/rules/` liên quan đến PR/diff | `docs/workflows/code-review.md` |
| **Cấu hình Docker / Triển khai** | `docs/rules/docker.md`<br>`docs/rules/deployment.md`<br>`docs/rules/configuration.md` | `docs/workflows/release.md` |

---

## 4. Những Điều AI Agent TUYỆT ĐỐI KHÔNG ĐƯỢC LÀM (Negative Constraints)

1. **KHÔNG tự ý bịa đặt yêu cầu (Never invent requirements)**: Nếu yêu cầu mơ hồ hoặc thiếu dữ liệu, phải dừng lại và hỏi người dùng.
2. **KHÔNG tự ý thay đổi API contract hoặc hành vi DB** mà chưa có sự đồng ý hoặc kế hoạch tương thích ngược.
3. **KHÔNG thêm thư viện/dependency bừa bãi**: Phải tận dụng Spring Boot Starter và thư viện sẵn có trước khi đề xuất thêm dependency mới vào `pom.xml`.
4. **KHÔNG bao bọc interface vô tội vạ**: Không tạo Interface cho Service nếu module đó chỉ có 1 triển khai duy nhất (`UserService` và `UserServiceImpl` là over-engineering nếu không có đa hình thực tế).
5. **KHÔNG để lộ thông tin nhạy cảm**: Tuyệt đối không commit password, API secret, JWT secret, database URI vào git; không log thông tin nhạy cảm hoặc mật khẩu.
6. **KHÔNG tin tưởng dữ liệu từ client**: Toàn bộ kiểm tra quyền hạn (Authorization) và quy tắc nghiệp vụ quan trọng phải được bảo vệ tại Backend.
7. **KHÔNG báo cáo giả mạo kết quả test**: TUYỆT ĐỐI KHÔNG khẳng định "Tests passed" hoặc "Build successful" nếu chưa thực sự gọi lệnh chạy test/build trong terminal.
8. **KHÔNG sửa code ngoài phạm vi (Out-of-scope refactoring)**: Không tiện tay sửa đổi các file không liên quan đến task hiện tại.

---

## 5. Thứ Tự Ưu Tiên Giải Quyết Xung Đột (Rule Priority)

Khi có sự xung đột giữa các nguồn chỉ dẫn, áp dụng thứ tự ưu tiên giảm dần:

```text
1. Yêu cầu rõ ràng từ người dùng (Explicit user requirement)
   ↓
2. Yêu cầu nghiệp vụ / Đặc tả kỹ thuật (Business specification)
   ↓
3. Quy tắc bảo mật (Security rules - docs/rules/security.md)
   ↓
4. Quy tắc an toàn dữ liệu (Database integrity - docs/rules/database.md)
   ↓
5. Kiến trúc hệ thống (Architecture rules - docs/architecture/)
   ↓
6. Chuẩn viết mã (Coding standards - docs/rules/coding-standards.md)
   ↓
7. Sở thích định dạng cá nhân (Style preferences)
```

*Nếu xảy ra mâu thuẫn không thể giải quyết an toàn: **DỪNG LẠI và hỏi người dùng để làm rõ, không đoán mò.***

---

## 6. Quy Định Bắt Buộc Về Git Commit Bằng Tiếng Việt

Sau khi hoàn thành và kiểm thử giải pháp, mọi commit phải được viết bằng tiếng Việt theo định dạng Conventional Commits:

```text
<type>: <mô tả ngắn gọn bằng tiếng Việt>
```

**Các type hợp lệ**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `security`.

**Ví dụ chuẩn**:
* `feat: thêm chức năng đăng ký tài khoản người dùng`
* `fix: sửa lỗi tính sai tổng tiền đơn hàng khi áp dụng mã giảm giá`
* `refactor: tối ưu hóa truy vấn lấy danh sách sản phẩm theo danh mục`
* `test: bổ sung unit test cho chức năng xác thực token JWT`
* `docs: cập nhật tài liệu quy tắc bảo mật API`

Chi tiết xem tại [`docs/rules/git.md`](file:///e:/du-an-ma/Spring-BE/docs/rules/git.md).
