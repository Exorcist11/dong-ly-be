# Tổng Quan Kiến Trúc Hệ Thống (Architecture Overview)

Tài liệu này định nghĩa các nguyên lý kiến trúc nền tảng cho hệ thống Backend Spring Boot của dự án. Mục tiêu tối thượng là xây dựng một hệ thống **chuyên nghiệp, sẵn sàng cho môi trường production, sạch sẽ, dễ bảo trì, an toàn, dễ hiểu cho con người và thân thiện cho AI Agent thao tác**.

---

## 1. Công Nghệ Chủ Đạo (Technology Stack)

* **Ngôn ngữ**: Java 21 LTS. Tận dụng cú pháp hiện đại: Record, Pattern Matching, Switch Expressions, Text Blocks, Sequenced Collections và Virtual Threads (Project Loom).
* **Framework**: Spring Boot 3.x (Spring Framework 6).
* **Quản lý dự án & phụ thuộc**: Apache Maven.
* **Cơ sở dữ liệu**:
  * Phát triển cục bộ: PostgreSQL 16+.
  * Môi trường Production: **Neon Serverless PostgreSQL** (hỗ trợ connection pooling qua PgBouncer, auto-scaling).
  * Công cụ di chuyển lược đồ (Schema Migration): **Flyway**.
* **Xác thực & Bảo mật**: Spring Security 6.x + JWT (Stateless REST API).
* **Đóng gói & Vận hành**: Docker (Multi-stage build), triển khai trên nền tảng đám mây **Render**.

---

## 2. Mô Hình Kiến Trúc Chủ Đạo: Modular Monolith

Dự án áp dụng mô hình **Modular Monolith** kết hợp tổ chức theo tính năng (**Package-by-Feature**):

```text
               Client (Web / Mobile / Third-party)
                                ↓ [HTTPS / JSON]
         ┌───────────────────────────────────────────────┐
         │             SPRING BOOT APPLICATION           │
         │                                               │
         │  ┌─────────────┐ ┌─────────────┐ ┌──────────┐ │
         │  │ Auth Module │ │ User Module │ │ ...      │ │
         │  └─────────────┘ └─────────────┘ └──────────┘ │
         │         │               │              │      │
         │  ┌──────────────────────────────────────────┐ │
         │  │       Shared / Common Infrastructure     │ │
         │  └──────────────────────────────────────────┘ │
         └───────────────────────────────────────────────┘
                                ↓ [JDBC / HikariCP]
                 Neon PostgreSQL (Database Engine)
```

### Tại sao chọn Modular Monolith?
1. **Tránh over-engineering của Microservices**: Không gặp phức tạp về mạng phân tán, distributed transactions, deploy phân tán hay service mesh khi quy mô chưa đòi hỏi.
2. **Duy trì ranh giới rõ ràng**: Mã nguồn được cô lập thành các module nghiệp vụ tự trị. Khi có nhu cầu tách thành microservice trong tương lai, việc bóc tách một module sẽ diễn ra tự nhiên và ít tốn kém nhất.
3. **Triển khai đơn giản**: Đóng gói thành một tệp JAR duy nhất, triển khai nhẹ nhàng trên Docker và Render.

---

## 3. Kiến Trúc Phân Lớp Thực Dụng (Pragmatic Layered Architecture)

Bên trong mỗi module hoặc toàn ứng dụng, luồng dữ liệu tuân thủ nghiêm ngặt theo một chiều duy nhất:

```text
      HTTP Request (JSON)
              ↓
    ┌───────────────────┐
    │    Controller     │   - Tiếp nhận HTTP request, phân giải route
    └───────────────────┘   - Xác thực cấu trúc đầu vào (@Valid DTO)
              ↓             - Gọi Service & trả về HTTP Response (DTO)
    ┌───────────────────┐
    │     Service       │   - Chứa toàn bộ nghiệp vụ (Business Rules)
    └───────────────────┘   - Quản lý ranh giới giao dịch (@Transactional)
              ↓             - Điều phối dữ liệu & chuyển đổi DTO ↔ Entity
    ┌───────────────────┐
    │    Repository     │   - Tương tác với cơ sở dữ liệu qua Spring Data JPA
    └───────────────────┘   - Truy vấn tối ưu, xử lý projection
              ↓
    ┌───────────────────┐
    │     Database      │   - PostgreSQL / Neon lưu trữ và bảo đảm toàn vẹn
    └───────────────────┘
```

### Các nguyên tắc phân lớp bắt buộc:
* **Controller siêu mỏng (Thin Controller)**: Không chứa logic nghiệp vụ, không gọi trực tiếp Repository, không xử lý tính toán.
* **Service nắm quyền điều phối (Rich Business Logic)**: Chịu trách nhiệm thực thi quy tắc nghiệp vụ, tính toàn vẹn và chuyển đổi DTO.
* **Repository tập trung dữ liệu**: Chỉ chịu trách nhiệm lưu trữ và truy vấn dữ liệu. Không chứa logic quyết định nghiệp vụ.
* **Không phụ thuộc ngược**: Repository không bao giờ biết đến Controller hay Service; Service không biết đến Controller hay các giao thức vận chuyển (HTTP, Header, Session).

---

## 4. Triết Lý Thiết Kế Cốt Lõi (Core Design Principles)

### 4.1. Không Over-Engineering (KISS & YAGNI)
* **Chỉ tạo abstraction khi có lý do chính đáng**: Tuyệt đối không tạo interface đại trà cho mọi Service (ví dụ: không tạo `UserService` interface nếu chỉ có duy nhất một `UserServiceImpl`). Hãy dùng trực tiếp `UserService` class. Nếu sau này cần đa hình (ví dụ: nhiều chiến lược thanh toán `PaymentGateway`), lúc đó mới trích xuất interface.
* Không tạo các lớp kế thừa vô nghĩa hoặc generic layer rối rắm (tránh `GenericService<T, ID>`, `BaseController<T>` làm giảm tính tường minh).

### 4.2. Bảo Vệ Dữ Liệu & Ranh Giới (Isolation & Encapsulation)
* **Tuyệt đối không để lộ Entity ra ngoài API**: Entity của JPA chỉ tồn tại từ Service layer trở xuống. Controller chỉ nhận Request DTO và trả về Response DTO.
* **Xác thực nhiều lớp (Defense in Depth)**: Validate tại DTO -> Validate logic tại Service -> Ràng buộc toàn vẹn khóa ngoại/unique tại Database.

### 4.3. An Toàn Mặc Định (Secure by Default)
* Mọi API endpoint mặc định là được bảo vệ (authenticated), trừ khi được chỉ định rõ ràng là public (như đăng nhập, đăng ký, webhook xác thực).
* Không bao giờ tin tưởng quyền hạn client gửi lên; backend là cơ quan quyết định ủy quyền tối cao.
