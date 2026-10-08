# Bản Ghi Quyết Định Kiến Trúc (Architecture Decision Records - ADR)

Thư mục này lưu trữ các quyết định kiến trúc quan trọng trong suốt vòng đời phát triển dự án.

Mỗi khi nhóm kỹ thuật đưa ra một quyết định mang tính chiến lược (lựa chọn công nghệ, thay đổi cấu trúc dữ liệu, giải pháp bảo mật, mô hình phân chia module), quyết định đó phải được ghi lại thành một ADR để làm căn cứ lịch sử, tránh việc các thế hệ lập trình viên hoặc AI Agent sau này thắc mắc hoặc vô tình đảo ngược quyết định mà không hiểu bối cảnh ban đầu.

---

## 1. Mẫu Bản Ghi Quyết Định (ADR Template)

Mỗi file ADR mới được đặt tên theo định dạng `NNN-<tên-ngắn-gọn>.md` (ví dụ: `001-modular-monolith.md`) với cấu trúc chuẩn sau:

```markdown
# ADR-NNN: [Tiêu đề quyết định]

* **Trạng thái**: [PROPOSED | ACCEPTED | DEPRECATED | SUPERSEDED]
* **Ngày quyết định**: YYYY-MM-DD
* **Người đề xuất**: [Tên kỹ sư / AI Agent]

## 1. Ngữ Cảnh & Vấn Đề (Context)
[Mô tả bối cảnh và vấn đề kỹ thuật hoặc nghiệp vụ cần giải quyết]

## 2. Quyết Định Đã Chọn (Decision)
[Mô tả chính xác giải pháp kỹ thuật được thống nhất áp dụng]

## 3. Lý Do Lựa Chọn (Reasoning)
[Tại sao giải pháp này được chọn? Các ưu điểm vượt trội của nó]

## 4. Các Phương Án Đã Cân Nhắc (Alternatives Considered)
[Các lựa chọn khác đã được đưa ra xem xét và lý do vì sao bị từ chối]

## 5. Hệ Quả & Đánh Đổi (Consequences & Trade-offs)
* **Tích cực**: [Lợi ích mang lại]
* **Tiêu cực / Rủi ro**: [Khó khăn hoặc giới hạn cần chú ý quản lý]
```

---

## 2. Danh Mục Các Quyết Định Kiến Trúc Nền Tảng Đã Xác Lập

Dưới đây là các quyết định nền tảng đã được thông qua cho dự án:

### ADR-001: Lựa Chọn Mô Hình Modular Monolith Thay Vì Microservices
* **Trạng thái**: ACCEPTED
* **Ngữ cảnh**: Cần một kiến trúc backend vững chắc, dễ phát triển, nhanh chóng đưa ra thị trường nhưng vẫn bảo đảm tính mở rộng khi quy mô người dùng tăng.
* **Quyết định**: Sử dụng mô hình **Modular Monolith** tổ chức theo tính năng (Package-by-Feature).
* **Lý do**: Microservices mang lại chi phí vận hành phân tán (Distributed Tracing, Network Latency, Data Consistency, CI/CD phức tạp) quá lớn ở giai đoạn hiện tại. Modular Monolith giữ sự đơn giản của một ứng dụng đơn khối nhưng bảo đảm ranh giới độc lập giữa các module, cho phép tách thành Microservice bất kỳ lúc nào nếu cần.

### ADR-002: Lựa Chọn Neon Serverless PostgreSQL & Flyway Migration
* **Trạng thái**: ACCEPTED
* **Ngữ cảnh**: Cần một hệ quản trị cơ sở dữ liệu quan hệ mạnh mẽ, hỗ trợ ACID, tin cậy cao và tối ưu chi phí vận hành trên đám mây.
* **Quyết định**: Sử dụng PostgreSQL với nhà cung cấp **Neon Serverless PostgreSQL** và quản lý di chuyển lược đồ tự động bằng **Flyway**.
* **Lý do**: Neon hỗ trợ tính năng serverless auto-scaling, phân tách lưu trữ và tính toán, hỗ trợ connection pooling qua PgBouncer. Flyway bảo đảm mọi thay đổi cấu trúc bảng đều có lịch sử phiên bản trong git và tự động chạy khi khởi động mà không cho phép Hibernate can thiệp làm hỏng dữ liệu production (`ddl-auto=validate`).

### ADR-003: Lựa Chọn Đóng Gói Docker & Nền Tảng Render
* **Trạng thái**: ACCEPTED
* **Ngữ cảnh**: Cần một quy trình đóng gói và môi trường máy chủ đám mây ổn định, có hỗ trợ HTTPS tự động, chi phí hợp lý và dễ triển khai.
* **Quyết định**: Đóng gói ứng dụng bằng **Dockerfile Multi-Stage Build** (Alpine runtime) và triển khai dưới dạng Web Service trên **Render**.
* **Lý do**: Docker tách biệt môi trường biên dịch và môi trường chạy, tạo ra container siêu nhẹ (<200MB) và an toàn với non-root user. Render cung cấp cơ chế tự động deploy qua git webhook, quản lý biến môi trường an toàn và hỗ trợ zero-downtime healthcheck `/actuator/health`.

### ADR-004: Lựa Chọn Xác Thực Phi Trạng Thái JWT (Stateless Authentication)
* **Trạng thái**: ACCEPTED
* **Ngữ cảnh**: Cần xây dựng RESTful API phục vụ nhiều loại client (Web SPA, Mobile App, đối tác thứ ba).
* **Quyết định**: Sử dụng cơ chế xác thực phi trạng thái (Stateless) với bộ đôi **Access Token (ngắn hạn)** và **Refresh Token (dài hạn)**.
* **Lý do**: Không cần duy trì HTTP Session phía máy chủ, giúp backend dễ dàng scale theo chiều ngang (Horizontal Scaling) mà không cần cấu hình cụm Redis session phức tạp ở giai đoạn ban đầu.

### ADR-005: Quy Định Bắt Buộc Thông Điệp Git Commit Bằng Tiếng Việt
* **Trạng thái**: ACCEPTED
* **Ngữ cảnh**: Cần một chuẩn mực lịch sử Git thống nhất, rõ ràng, giúp toàn bộ đội ngũ lập trình viên người Việt và AI Agent dễ dàng nắm bắt chính xác nội dung thay đổi.
* **Quyết định**: Mọi commit bắt buộc tuân theo định dạng `<type>: <mô tả bằng tiếng Việt>`.
* **Lý do**: Tránh các commit tiếng Anh viết vội vô nghĩa hoặc không nhất quán về ngữ nghĩa; mô tả tiếng Việt chi tiết phản ánh trung thực bản chất công việc đã làm.

### ADR-006: Lựa Chọn Java 21 LTS Làm Phiên Bản Nền Tảng Cốt Lõi
* **Trạng thái**: ACCEPTED
* **Ngữ cảnh**: Cần lựa chọn phiên bản Java chuẩn hóa lâu dài (LTS) có hiệu năng cao, tính năng ngôn ngữ hiện đại và tương thích tốt nhất với Spring Boot 3.x.
* **Quyết định**: Chuẩn hóa toàn bộ dự án trên **Java 21 LTS**.
* **Lý do**:
  * Java 21 là phiên bản Long-Term Support mới nhất của Java.
  * Hỗ trợ **Virtual Threads (Project Loom)** giúp tăng thông lượng xử lý I/O đồng thời mà không làm tăng chi phí phần cứng.
  * Cung cấp các cải tiến ngôn ngữ vượt trội: Record Patterns, Pattern Matching for switch, Sequenced Collections.
  * Tương thích hoàn hảo với Spring Boot 3.2+ và tối ưu hóa thời gian khởi động container.
