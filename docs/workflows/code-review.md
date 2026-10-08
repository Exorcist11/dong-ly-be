# Quy Trình Đánh Giá Mã Nguồn (Code Review Workflow)

Tài liệu này xác định các tiêu chí đánh giá chất lượng mã nguồn khi kỹ sư hoặc AI Agent thực hiện rà soát thay đổi (Pull Request / Git Diff).

---

## 1. Các Trọng Tâm Đánh Giá (Review Checklist)

Khi đánh giá một pull request hoặc diff, tập trung vào 8 khía cạnh cốt lõi:

1. **Tính Đúng Đắn (Correctness)**: Mã nguồn có thực sự giải quyết đúng yêu cầu không? Có xử lý các trường hợp biên (Edge cases), giá trị rỗng (Null/Empty) không?
2. **Kiến Trúc & Ranh Giới (Architecture & Boundaries)**: Có tôn trọng mô hình Modular Monolith không? Có module nào gọi lén Repository của module khác không? Entity có bị lộ ra API không?
3. **Bảo Mật (Security)**: Có lỗ hổng IDOR không? Có để lộ thông tin nhạy cảm trong log/API không? Mật khẩu có được băm với BCrypt không? Token/Secret có bị commit không?
4. **Hiệu Năng & Cơ Sở Dữ Liệu (Performance & DB)**: Có nguy cơ N+1 query không? Có đánh index cho khóa ngoại không? Có giữ transaction quá dài khi gọi API ngoài không?
5. **Xác Thực Dữ Liệu (Validation)**: Đã validate đầy đủ ở Request DTO chưa? Có tin tưởng mù quáng dữ liệu từ client không?
6. **Kiểm Thử (Test Coverage)**: Có bài test tự động đi kèm không? Bài test có kiểm tra các trường hợp thất bại (Failure cases) không? Test đã thực sự chạy pass chưa?
7. **Tránh Over-Engineering**: Có interface dư thừa nào được tạo ra cho một Service đơn lẻ không? Có tạo lớp "Helper/Utils" vô định hình không?
8. **Quy Chuẩn Viết Mã (Standards & Git)**: Đặt tên có chuẩn không? Commit message có bằng tiếng Việt theo chuẩn không?

---

## 2. Thang Phân Loại Mức Độ Vấn Đề (Finding Severity Classification)

Mọi nhận xét trong quá trình review bắt buộc phải được gắn nhãn mức độ rõ ràng:

| Mức độ | Định nghĩa & Tác động | Yêu cầu hành động |
| :--- | :--- | :--- |
| **`CRITICAL`** | Lỗ hổng bảo mật nghiêm trọng (lộ secret, IDOR, SQL injection), mất mát hoặc làm hỏng dữ liệu, lỗi crash hệ thống. | **BẮT BUỘC SỬA NGAY**. Chặn merge/deploy. |
| **`HIGH`** | Vi phạm quy tắc nghiệp vụ cốt lõi, lỗi N+1 query nặng, vi phạm nghiêm trọng ranh giới module, thiếu kiểm thử cho logic rủi ro cao. | Phải sửa trước khi merge. |
| **`MEDIUM`** | Xử lý ngoại lệ chưa tối ưu, thiếu validation ở một số trường phụ, vi phạm nguyên tắc Clean Code (phương thức quá dài, đặt tên khó hiểu). | Nên sửa, có thể tạo task theo dõi nếu gấp. |
| **`LOW`** | Thiếu sót nhỏ về tài liệu, format chưa đồng nhất, biến không cần thiết. | Sửa nhanh nếu thuận tiện. |
| **`SUGGESTION`** | Đề xuất giải pháp thay thế tinh tế hơn, tối ưu nhỏ về cú pháp, góc nhìn cá nhân. | Tùy chọn cân nhắc (Optional). |

> [!IMPORTANT]
> **Tuyệt đối không biến sở thích phong cách cá nhân thành lỗi CRITICAL hoặc HIGH.** Hãy phân biệt rõ giữa "Sai phạm kỹ thuật/bảo mật" và "Ý kiến cá nhân về thẩm mỹ".
