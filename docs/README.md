# Tài Liệu Kỹ Thuật & Bộ Quy Tắc Phát Triển Backend (Documentation System)

Chào mừng bạn đến với hệ thống tài liệu và quy chuẩn kỹ thuật chính thức của dự án Backend (Spring Boot).

Thư mục `docs/` này là **Nguồn Chân Lý Duy Nhất (Single Source of Truth)** về kiến trúc, quy chuẩn viết mã, quy trình kiểm thử, triển khai và vận hành hệ thống. Cả kỹ sư con người và AI Agent đều bắt buộc phải tuân theo các quy tắc này.

---

## 1. Nguyên Tắc Điều Hướng (Navigation Flow)

Để tiếp cận và thực thi một nhiệm vụ hiệu quả, quy trình tra cứu thông tin diễn ra như sau:

```text
       AGENTS.md (Root entry point)
           ↓
     docs/README.md (Tài liệu này - Bản đồ chỉ mục)
           ↓
  docs/rules/<rule>.md (Các quy tắc kỹ thuật tương ứng)
           ↓
docs/workflows/<flow>.md (Quy trình triển khai / xử lý tương ứng)
           ↓
    Khảo sát mã nguồn thực tế & Triển khai tối giản
```

---

## 2. Cấu Trúc Thư Mục Tài Liệu

Toàn bộ hệ thống tài liệu được phân bổ khoa học theo cấu trúc sau:

```text
docs/
├── README.md                           # Bản đồ chỉ mục & hướng dẫn sử dụng tài liệu
├── project-overview.md                 # Tổng quan dự án & Yêu cầu nghiệp vụ cốt lõi (Đông Lý)
│
├── architecture/                       # Kiến trúc tổng thể và tổ chức dự án
│   ├── overview.md                     # Tổng quan kiến trúc & các nguyên lý cốt lõi
│   ├── project-structure.md            # Cấu trúc thư mục nguồn theo Modular Monolith
│   └── module-architecture.md          # Thiết kế bên trong từng module nghiệp vụ
│
├── rules/                              # Các quy chuẩn kỹ thuật bắt buộc
│   ├── coding-standards.md             # Tiêu chuẩn viết mã Java chung & đặt tên
│   ├── java.md                         # Quy tắc sử dụng Java (Collections, Streams, Optional, Immutability)
│   ├── spring-boot.md                  # Quy tắc Spring Boot (DI, Component, Controller, Service)
│   ├── clean-code.md                   # Nguyên tắc Clean Code & tránh over-engineering
│   ├── reusability.md                  # Chiến lược tái sử dụng code & chia sẻ logic an toàn
│   ├── api.md                          # Tiêu chuẩn REST API, HTTP methods, status codes, pagination
│   ├── dto.md                          # Quy chuẩn Request/Response DTO, mapping, bảo vệ Entity
│   ├── exception-handling.md           # Xử lý ngoại lệ tập trung (@RestControllerAdvice, error codes)
│   ├── validation.md                   # Chiến lược xác thực dữ liệu 3 lớp (Request, Business, DB)
│   ├── database.md                     # Thiết kế PostgreSQL, schema, migrations (Flyway)
│   ├── jpa.md                          # Tối ưu Hibernate/JPA, tránh N+1, Fetch plan, Lazy loading
│   ├── transaction.md                  # Quản lý giao dịch (@Transactional, boundary, readOnly)
│   ├── security.md                     # Bảo mật Spring Security, JWT, Authorization, bảo vệ bí mật
│   ├── configuration.md                # Quản lý cấu hình đa môi trường (application.yml, profiles, env vars)
│   ├── logging.md                      # Chuẩn ghi log có cấu trúc, che giấu dữ liệu nhạy cảm
│   ├── testing.md                      # Chiến lược kiểm thử tự động (Unit, Integration, Testcontainers)
│   ├── docker.md                       # Tiêu chuẩn Dockerfile, multi-stage build, container an toàn
│   ├── deployment.md                   # Triển khai Render & Neon PostgreSQL, healthcheck, graceful shutdown
│   └── git.md                          # Quy định commit tiếng Việt, nhánh, atomic commits
│
├── workflows/                          # Quy trình từng bước thực thi công việc
│   ├── feature-development.md          # Quy trình phát triển tính năng mới từ yêu cầu đến commit
│   ├── bug-fix.md                      # Quy trình tái hiện, tìm nguyên nhân gốc rễ và sửa lỗi
│   ├── refactoring.md                  # Quy trình tái cấu trúc mã nguồn an toàn không gây hồi quy
│   ├── code-review.md                  # Tiêu chuẩn đánh giá mã nguồn và thang mức độ lỗi
│   └── release.md                      # Quy trình đóng gói, kiểm thử môi trường và phát hành
│
└── decisions/                          # Bản ghi quyết định kiến trúc (ADR)
    └── README.md                       # Danh mục & quy chuẩn viết ADR (Architecture Decision Records)
```

---

## 3. Bản Đồ Tra Cứu Quy Tắc Theo Tác Vụ (Task Mapping)

Khi tiếp nhận một loại công việc cụ thể, hãy tra cứu các tài liệu được chỉ định dưới đây:

| Ngữ cảnh công việc | Tài liệu Quy tắc cần đọc | Quy trình cần tuân thủ |
| :--- | :--- | :--- |
| **Tìm hiểu Nghiệp vụ & Bối cảnh dự án** | [project-overview.md](file:///e:/du-an-ma/Spring-BE/docs/project-overview.md)<br>[overview.md](file:///e:/du-an-ma/Spring-BE/docs/architecture/overview.md) | [feature-development.md](file:///e:/du-an-ma/Spring-BE/docs/workflows/feature-development.md) |
| **Tạo mới hoặc sửa REST API** | [api.md](file:///e:/du-an-ma/Spring-BE/docs/rules/api.md), [dto.md](file:///e:/du-an-ma/Spring-BE/docs/rules/dto.md), [validation.md](file:///e:/du-an-ma/Spring-BE/docs/rules/validation.md), [exception-handling.md](file:///e:/du-an-ma/Spring-BE/docs/rules/exception-handling.md) | [feature-development.md](file:///e:/du-an-ma/Spring-BE/docs/workflows/feature-development.md) |
| **Thay đổi Database / Migration / JPA Entity** | [database.md](file:///e:/du-an-ma/Spring-BE/docs/rules/database.md), [jpa.md](file:///e:/du-an-ma/Spring-BE/docs/rules/jpa.md), [transaction.md](file:///e:/du-an-ma/Spring-BE/docs/rules/transaction.md) | [feature-development.md](file:///e:/du-an-ma/Spring-BE/docs/workflows/feature-development.md) |
| **Cài đặt Xác thực / Phân quyền / Bảo mật** | [security.md](file:///e:/du-an-ma/Spring-BE/docs/rules/security.md), [configuration.md](file:///e:/du-an-ma/Spring-BE/docs/rules/configuration.md) | [feature-development.md](file:///e:/du-an-ma/Spring-BE/docs/workflows/feature-development.md) |
| **Điều tra và Sửa lỗi (Fix Bug)** | [clean-code.md](file:///e:/du-an-ma/Spring-BE/docs/rules/clean-code.md), [logging.md](file:///e:/du-an-ma/Spring-BE/docs/rules/logging.md), [testing.md](file:///e:/du-an-ma/Spring-BE/docs/rules/testing.md) | [bug-fix.md](file:///e:/du-an-ma/Spring-BE/docs/workflows/bug-fix.md) |
| **Dọn dẹp code / Tái cấu trúc logic** | [clean-code.md](file:///e:/du-an-ma/Spring-BE/docs/rules/clean-code.md), [reusability.md](file:///e:/du-an-ma/Spring-BE/docs/rules/reusability.md), [coding-standards.md](file:///e:/du-an-ma/Spring-BE/docs/rules/coding-standards.md) | [refactoring.md](file:///e:/du-an-ma/Spring-BE/docs/workflows/refactoring.md) |
| **Kiểm tra chất lượng code (Review)** | [code-review.md](file:///e:/du-an-ma/Spring-BE/docs/workflows/code-review.md) & các rule liên quan đến thay đổi | [code-review.md](file:///e:/du-an-ma/Spring-BE/docs/workflows/code-review.md) |
| **Đóng gói Docker & Triển khai Render/Neon** | [docker.md](file:///e:/du-an-ma/Spring-BE/docs/rules/docker.md), [deployment.md](file:///e:/du-an-ma/Spring-BE/docs/rules/deployment.md), [configuration.md](file:///e:/du-an-ma/Spring-BE/docs/rules/configuration.md) | [release.md](file:///e:/du-an-ma/Spring-BE/docs/workflows/release.md) |
| **Tạo Git Commit** | [git.md](file:///e:/du-an-ma/Spring-BE/docs/rules/git.md) | Mọi workflow |

---

## 4. Thứ Tự Ưu Tiên Giải Quyết Xung Đột (Rule Priority)

Trong quá trình phát triển, nếu xuất hiện sự xung đột giữa các hướng dẫn hoặc yêu cầu, áp dụng thứ tự ưu tiên sau:

1. **Yêu cầu chỉ đạo trực tiếp từ người dùng (Explicit User Requirement)**: Quyết định tối cao của người dùng trong phiên làm việc.
2. **Yêu cầu nghiệp vụ sản phẩm (Business Specification)**: Tính đúng đắn của logic nghiệp vụ.
3. **Quy tắc bảo mật (Security Rules)**: Tuyệt đối không đánh đổi bảo mật lấy sự tiện lợi.
4. **Quy tắc toàn vẹn dữ liệu (Data Integrity Rules)**: Bảo vệ tính đúng đắn, quan hệ và giao dịch của database.
5. **Kiến trúc phân tầng và module (Architecture Rules)**: Đảm bảo ranh giới module và tính độc lập.
6. **Tiêu chuẩn viết mã (Coding Standards)**: Tính rõ ràng, sạch sẽ, dễ đọc.
7. **Sở thích định dạng mã (Style Preferences)**: Định dạng cú pháp, thụt lề, v.v.

> [!CAUTION]
> Khi phát hiện mâu thuẫn giữa quy tắc bảo mật hoặc toàn vẹn dữ liệu với yêu cầu người dùng chưa rõ, lập trình viên hoặc AI Agent phải cảnh báo rủi ro và xin làm rõ trước khi thực hiện.

---

## 5. Cơ Chế Cập Nhật Và Tiến Hóa Tài Liệu (Document Evolution)

Tài liệu trong thư mục này là **Living Documentation** (tài liệu sống), phản ánh chính xác trạng thái và hướng đi của mã nguồn:

* **Không sao chép trùng lặp**: Mỗi quy tắc kỹ thuật chỉ được định nghĩa tại một tệp duy nhất để tránh xung đột khi cập nhật.
* **Ghi nhận quyết định kiến trúc**: Mọi thay đổi lớn về công nghệ, thư viện lõi, chiến lược cơ sở dữ liệu hay mô hình kiến trúc bắt buộc phải được ghi lại trong thư mục [`docs/decisions/`](file:///e:/du-an-ma/Spring-BE/docs/decisions/README.md) theo mẫu ADR.
* **Đồng bộ hóa**: Khi một quy chuẩn thay đổi trong code (ví dụ: bổ sung quy định mới về phân trang hoặc bảo mật), tài liệu tương ứng trong `docs/rules/` phải được cập nhật đồng thời trong cùng một commit hoặc PR.
