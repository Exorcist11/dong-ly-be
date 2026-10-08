# Cấu Trúc Dự Án (Project Structure)

Tài liệu này quy định chi tiết cấu trúc thư mục và quy tắc tổ chức package mã nguồn trong dự án. Việc tổ chức nhất quán giúp lập trình viên và AI Agent dễ dàng định vị mã nguồn, giảm thiểu xung đột và bảo đảm tính đóng gói (encapsulation).

---

## 1. Tổ Chức Thư Mục Cấp Cao (Root Structure)

```text
Spring-BE/
├── .github/                     # Workflows CI/CD, templates
├── docs/                        # Hệ thống tài liệu kỹ thuật & quy tắc (Single Source of Truth)
│   ├── architecture/
│   ├── rules/
│   ├── workflows/
│   └── decisions/
├── src/
│   ├── main/
│   │   ├── java/com/company/project/
│   │   └── resources/
│   └── test/
│       ├── java/com/company/project/
│       └── resources/
├── Dockerfile                   # Cấu hình containerization cho ứng dụng
├── docker-compose.yml           # Môi trường chạy PostgreSQL & PgAdmin cục bộ
├── pom.xml                      # Quản lý phụ thuộc Maven
├── AGENTS.md                    # Điểm vào chỉ dẫn dành cho AI Agent
└── README.md                    # Hướng dẫn tổng quan & khởi chạy nhanh dự án
```

---

## 2. Tổ Chức Mã Nguồn: Package-by-Feature (Modular Monolith)

Dự án áp dụng mô hình **Package-by-Feature (Tổ chức theo tính năng)** thay vì *Package-by-Layer toàn cục*.

### ❌ KHÔNG sử dụng cấu trúc Package-by-Layer toàn cục:
```text
# TRÁNH cấu trúc này vì khó bảo trì khi dự án phát triển lớn:
com.company.project/
├── controller/    # Chứa 50 controllers của tất cả tính năng lẫn lộn
├── service/       # Chứa 50 services của tất cả tính năng
├── repository/    # Chứa 50 repositories
└── entity/        # Chứa 50 JPA entities
```

### ✅ BẮT BUỘC sử dụng cấu trúc Modular Feature:
```text
com.company.project/
├── Application.java                 # Entry point khởi chạy Spring Boot
│
├── common/                          # Các thành phần dùng chung toàn hệ thống
│   ├── api/                         # ApiResponse wrapper, PageResponse, ErrorResponse
│   ├── exception/                   # Base exceptions, GlobalExceptionHandler
│   ├── security/                    # JwtTokenProvider, SecurityFilter, CurrentUserResolver
│   └── util/                        # Tiện ích chung CỰC KỲ HẠN CHẾ (xem docs/rules/reusability.md)
│
├── config/                          # Cấu hình Spring (SecurityConfig, JpaConfig, OpenApiConfig)
│
└── modules/                         # CÁC MODULE NGHIỆP VỤ ĐỘC LẬP
    ├── auth/                        # Module Xác thực (Authentication & Authorization)
    │   ├── AuthController.java
    │   ├── AuthService.java
    │   ├── dto/
    │   │   ├── LoginRequest.java
    │   │   ├── RegisterRequest.java
    │   │   └── AuthResponse.java
    │   └── mapper/                  # Chuyển đổi DTO ↔ Model (nếu cần)
    │
    ├── user/                        # Module Quản lý người dùng
    │   ├── UserController.java
    │   ├── UserService.java
    │   ├── UserRepository.java
    │   ├── entity/
    │   │   └── User.java
    │   ├── dto/
    │   │   ├── CreateUserRequest.java
    │   │   ├── UpdateUserRequest.java
    │   │   └── UserResponse.java
    │   └── mapper/
    │       └── UserMapper.java
    │
    └── <other-feature>/             # Các module tính năng nghiệp vụ khác (order, product, ...)
        ├── <Feature>Controller.java
        ├── <Feature>Service.java
        ├── <Feature>Repository.java
        ├── entity/
        └── dto/
```

---

## 3. Quy Tắc Phân Bổ Thành Phần Trong Module

Mỗi module nghiệp vụ chịu trách nhiệm hoàn chỉnh về miền dữ liệu của nó:

1. **`entity/`**: Chứa các thực thể JPA ánh xạ tới bảng database của module. Entity phải có tính đóng gói cao, không được rò rỉ ra ngoài API.
2. **`dto/`**: Chứa toàn bộ Data Transfer Objects gồm `*Request` (dữ liệu đầu vào) và `*Response` (dữ liệu trả về cho client). Ưu tiên sử dụng Java `record`.
3. **`repository/`**: Chứa các Spring Data JPA Repositories truy xuất dữ liệu thuộc phạm vi module.
4. **`service/`**: Chứa lớp cài đặt nghiệp vụ. Không tạo interface dư thừa nếu chỉ có 1 implementation.
5. **`controller/`**: Chứa REST Controller tiếp nhận request HTTP, kiểm tra hợp lệ DTO và trả về kết quả chuẩn hóa.
6. **`mapper/`**: Chứa mapper chuyển đổi giữa Entity và DTO (viết hàm tĩnh hoặc MapStruct, xem [`docs/rules/dto.md`](file:///e:/du-an-ma/Spring-BE/docs/rules/dto.md)).

---

## 4. Tổ Chức Thư Mục Tài Nguyên (src/main/resources)

```text
src/main/resources/
├── application.yml                  # Cấu hình chung cho mọi môi trường
├── application-dev.yml              # Cấu hình môi trường phát triển cục bộ
├── application-prod.yml             # Cấu hình môi trường Production (Render + Neon)
│
└── db/
    └── migration/                   # Các script Flyway di chuyển database
        ├── V1__init_schema.sql
        ├── V2__create_users_table.sql
        └── ...
```

---

## 5. Tổ Chức Thư Mục Kiểm Thử (src/test)

Cấu trúc package của `src/test/java` phải phản chiếu chính xác cấu trúc của `src/main/java`:

```text
src/test/java/com/company/project/
├── modules/
│   ├── auth/
│   │   └── AuthServiceTest.java          # Unit test cho logic nghiệp vụ Auth
│   └── user/
│       ├── UserControllerTest.java       # MockMvc test cho Controller User
│       ├── UserServiceTest.java          # Unit test cho UserService
│       └── UserRepositoryTest.java       # DataJpaTest cho UserRepository
└── ...
```
