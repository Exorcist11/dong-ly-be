# Quy Chuẩn Git & Thông Điệp Commit Tiếng Việt (Git Guidelines)

Tài liệu này xác định các quy tắc quản lý lịch sử mã nguồn Git và **Quy định bắt buộc về thông điệp commit bằng tiếng Việt**.

---

## 1. Nguyên Tắc Commit Nguyên Tử (Atomic Commits)

> **Mỗi commit phải là một đơn vị thay đổi có ý nghĩa, có thể review độc lập, chạy test độc lập và có thể rollback an toàn mà không làm hỏng ứng dụng.**

### Các nguyên tắc vàng:
1. **Không trộn lẫn thay đổi không liên quan**: Không gộp việc "Sửa lỗi login" cùng với "Format lại code toàn bộ UserController" và "Nâng cấp thư viện Jackson" trong cùng 1 commit.
2. **Không commit mã nguồn dở dang (Broken builds)**: Trước khi tạo commit, mã nguồn phải biên dịch thành công và vượt qua kiểm thử tự động.
3. **Không tạo micro-commit vô nghĩa**: Tránh tạo các commit rác như `fix typo`, `fix again`, `test 1`, `update`. Hãy gom các bước nhỏ thành một thay đổi hoàn chỉnh bằng `git commit --amend` trước khi đẩy lên remote.

---

## 2. Quy Định Bắt Buộc Về Commit Bằng Tiếng Việt (Vietnamese Commit Requirement)

Mọi commit trong repository này **BẮT BUỘC** tuân thủ định dạng Conventional Commits kết hợp mô tả bằng **TIẾNG VIỆT**:

```text
<type>: <mô tả ngắn gọn bằng tiếng Việt>
```

### 2.1. Danh Sách Các Type Được Phép Sử Dụng:
* **`feat`**: Bổ sung tính năng mới cho người dùng hoặc API mới.
* **`fix`**: Sửa chữa lỗi nghiệp vụ, lỗi hệ thống, hoặc bug phát sinh.
* **`refactor`**: Tái cấu trúc mã nguồn (không thêm tính năng, không sửa lỗi, chỉ cải thiện thiết kế mã).
* **`test`**: Bổ sung hoặc chỉnh sửa các bài kiểm thử tự động (Unit test, Integration test).
* **`docs`**: Cập nhật hoặc bổ sung tài liệu kỹ thuật, README, hướng dẫn quy tắc.
* **`chore`**: Công việc bảo trì dự án, cập nhật `.gitignore`, cấu hình Maven, Docker.
* **`perf`**: Cải tiến hiệu năng truy vấn database, thuật toán, bộ nhớ.
* **`security`**: Nâng cấp bảo mật, vá lỗ hổng an toàn thông tin, cấu hình phân quyền.

### 2.2. Các Ví Dụ Chuẩn Mực:
* `feat: thêm chức năng đăng ký tài khoản người dùng`
* `feat: thêm API quản lý danh mục sản phẩm`
* `feat: thêm cơ chế xác thực đăng nhập bằng JWT`
* `fix: sửa lỗi không tìm thấy người dùng theo email`
* `fix: sửa lỗi phân trang danh sách sách khi số trang vượt quá giới hạn`
* `refactor: tách logic xử lý đơn hàng khỏi controller sang service`
* `test: bổ sung unit test cho chức năng xác thực token JWT`
* `docs: cập nhật tài liệu quy tắc thiết kế REST API`
* `perf: đánh index cho cột user_id trên bảng orders để tối ưu truy vấn`
* `security: cấu hình mã hóa mật khẩu người dùng với BCrypt`

### 2.3. ❌ Tuyệt Đối Tránh Các Commit Vô Nghĩa:
* `update`
* `fix`
* `changes`
* `done`
* `test`
* `abc`
* `fix bug`

---

## 3. Quy Trình 5 Bước Trước Khi Commit

Trước khi thực hiện lệnh commit, lập trình viên hoặc AI Agent phải trải qua đúng 5 bước:

```text
1. Triển khai giải pháp (Implement)
    ↓
2. Chạy kiểm thử tự động thực tế (Test: ./mvnw test)
    ↓
3. Tự review lại diff xem có sót code thừa không (Review: git diff)
    ↓
4. Biên dịch thử toàn bộ dự án (Build: ./mvnw clean compile)
    ↓
5. Tạo Git commit cục bộ với thông điệp tiếng Việt chuẩn (Local Commit)
```

---

## 4. Quy Định Tuyệt Đối Về Lệnh `git push`

> [!CAUTION]
> **AI Agent TUYỆT ĐỐI KHÔNG ĐƯỢC PHÉP CHẠY LỆNH `git push`.**
> 
> * Mọi thao tác đẩy commit lên máy chủ từ xa (Remote repository như GitHub / GitLab) **hoàn toàn do Người dùng (USER) tự tay quyết định và thực thi**.
> * Sau khi hoàn thành kiểm thử và tạo local commit an toàn, AI Agent phải dừng lại, báo cáo kết quả và thông báo để người dùng chủ động kiểm tra và thực hiện `git push`.
