# Dong Ly Backend - Nền Tảng Kỹ Thuật (Technical Foundation)

Chào mừng bạn đến với dự án Backend của nền tảng **Đông Ly**. Dự án được xây dựng trên nền tảng **Java 21 LTS** và **Spring Boot 3.3.x**, tuân thủ nghiêm ngặt mô hình kiến trúc **Modular Monolith** kết hợp tổ chức theo tính năng (**Package-by-Feature**).

---

## 1. Tổng Quan Công Nghệ (Tech Stack)

* **Ngôn ngữ**: Java 21 LTS
* **Framework**: Spring Boot 3.3.4 (Spring Framework 6)
* **Quản lý dự án**: Maven 3.9+ (tích hợp sẵn Maven Wrapper `./mvnw`)
* **Cơ sở dữ liệu**:
  * Phát triển cục bộ: PostgreSQL 16+ (Docker Compose)
  * Môi trường Production: **Neon Serverless PostgreSQL** (PgBouncer connection pooling)
* **Di chuyển lược đồ dữ liệu**: Flyway
* **ORM / Data Access**: Spring Data JPA & Hibernate 6
* **Bảo mật**: Spring Security 6, JWT (JSON Web Token), BCrypt (cost factor 12)
* **Kiểm tra dữ liệu**: Jakarta Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`, ...)
* **Giám sát & Vận hành**: Spring Boot Actuator (`/actuator/health`)
* **Tài liệu API**: OpenAPI 3 / Swagger (`/swagger-ui/index.html`, `/api-docs`)
* **Đóng gói container**: Docker (Multi-stage build, người dùng non-root `appuser`)
* **Kiểm thử tự động**: JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers

---

## 2. Cấu Trúc Mã Nguồn (Project Structure)

```text
src/
├── main/
│   ├── java/com/dongly/
│   │   ├── Application.java             # Điểm khởi chạy ứng dụng Spring Boot
│   │   │
│   │   ├── common/                      # Thành phần dùng chung toàn hệ thống
│   │   │   ├── api/                     # ApiResponse, PageResponse
│   │   │   └── exception/               # GlobalExceptionHandler, AppException, ErrorCode
│   │   │
│   │   ├── config/                      # Cấu hình hệ thống (Security, OpenAPI, CORS, JPA)
│   │   │   ├── CorsConfig.java
│   │   │   ├── JacksonConfig.java
│   │   │   ├── JpaAuditingConfig.java
│   │   │   ├── OpenApiConfig.java
│   │   │   └── SecurityConfig.java
│   │   │
│   │   ├── security/                    # Hạ tầng bảo mật Stateless JWT
│   │   │   ├── CurrentUser.java
│   │   │   ├── JwtAccessDeniedHandler.java
│   │   │   ├── JwtAuthenticationEntryPoint.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── JwtProperties.java
│   │   │   ├── JwtTokenProvider.java
│   │   │   └── SecurityUtils.java
│   │   │
│   │   └── modules/                     # CÁC MODULE NGHIỆP VỤ ĐỘC LẬP (Feature Modules)
│   │       └── package-info.java        # Hướng dẫn mở rộng module tương lai
│   │
│   └── resources/
│       ├── application.yml              # Cấu hình gốc mặc định
│       ├── application-dev.yml          # Môi trường phát triển cục bộ
│       ├── application-prod.yml         # Môi trường Production (Render & Neon)
│       ├── application-test.yml         # Môi trường chạy kiểm thử tự động
│       └── db/
│           └── migration/               # Quản lý script di chuyển Flyway
│               └── V1__init_schema.sql
│
└── test/
    └── java/com/dongly/
        ├── ApplicationTests.java
        ├── common/exception/GlobalExceptionHandlerTest.java
        ├── config/SecurityConfigTest.java
        └── security/JwtTokenProviderTest.java
```

---

## 3. Yêu Cầu Cài Đặt (Prerequisites)

* **Java JDK 21**: Cài đặt JDK 21 trở lên (Eclipse Temurin, Oracle JDK hoặc Corretto).
* **Docker & Docker Compose**: Để chạy PostgreSQL cục bộ và kiểm thử container.
* *(Tùy chọn)* Maven: Không bắt buộc cài sẵn Maven vì đã có `./mvnw` và `mvnw.cmd` đi kèm trong repository.

---

## 4. Thiết Lập Biến Môi Trường (Environment Variables)

Sao chép tệp mẫu cấu hình:

```bash
cp .env.example .env
```

| Biến môi trường | Ý nghĩa | Mẫu giá trị Dev | Mẫu giá trị Prod (Render & Neon) |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Profile kích hoạt | `dev` | `prod` |
| `PORT` | Cổng HTTP | `8080` | Được Render tự cấp phát |
| `DATABASE_URL` | Chuỗi JDBC kết nối DB | `jdbc:postgresql://localhost:5432/dongly_dev` | `jdbc:postgresql://ep-xyz-pooler.us-east-2.aws.neon.tech/neondb?sslmode=require` |
| `DATABASE_USERNAME` | Tên tài khoản DB | `postgres` | Tài khoản Neon cấp |
| `DATABASE_PASSWORD` | Mật khẩu DB | `postgres_dev_password` | Mật khẩu Neon cấp |
| `JWT_SECRET` | Khóa bí mật ký token | Chuỗi 256 bits ngẫu nhiên | Khóa bí mật ngẫu nhiên bảo mật cao |
| `APP_CORS_ALLOWED_ORIGINS` | Danh sách domain CORS | `http://localhost:3000,http://localhost:5173` | `https://dongly.vn` |

---

## 5. Hướng Dẫn Chạy Cục Bộ (Local Development)

### Bước 1: Khởi động cơ sở dữ liệu PostgreSQL cục bộ
Sử dụng Docker Compose:

```bash
docker compose up -d
```

Lệnh này sẽ khởi tạo dịch vụ PostgreSQL 16 tại cổng `localhost:5432` với database `dongly_dev`.

### Bước 2: Khởi chạy ứng dụng Backend
Sử dụng Maven Wrapper:

* Trên Windows:
  ```cmd
  mvnw.cmd spring-boot:run
  ```
* Trên Linux / macOS:
  ```bash
  ./mvnw spring-boot:run
  ```

---

## 6. Kiểm Thử Và Đóng Gói (Testing & Building)

### Chạy toàn bộ bài kiểm thử tự động:
```bash
./mvnw clean test
```

### Chạy kiểm tra đóng gói toàn diện (Verify):
```bash
./mvnw clean verify
```

---

## 7. Đóng Gói Và Thực Thi Với Docker

### Xây dựng Docker Image (Multi-stage build):
```bash
docker build -t dongly-backend:latest .
```

### Chạy Docker Container:
```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/dongly_dev \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=postgres_dev_password \
  dongly-backend:latest
```

---

## 8. Cơ Sở Dữ Liệu & Flyway Migrations

* Quản lý lược đồ cơ sở dữ liệu hoàn toàn bằng các tệp migration của **Flyway** đặt tại `src/main/resources/db/migration/`.
* Định dạng đặt tên file: `V<Phiên_bản>__<Mô_tả>.sql` (ví dụ: `V1__init_schema.sql`).
* Trên môi trường Production, cơ chế `hibernate.ddl-auto` được đặt cố định là `validate` để đảm bảo toàn vẹn dữ liệu. Tuyệt đối không sử dụng `update` hoặc `create-drop`.

---

## 9. Tài Liệu Hóa API (Swagger / OpenAPI)

Sau khi ứng dụng khởi chạy thành công:
* **Swagger UI Giao Diện Trực Quan**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **OpenAPI Schema (JSON)**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

Để kiểm thử các endpoint được bảo vệ trên Swagger UI:
1. Nhấn nút **Authorize** ở góc phải giao diện.
2. Nhập token JWT theo định dạng `Bearer <token>`.

---

## 10. Kiểm Tra Sức Khỏe Hệ Thống (Health Check)

Endpoint kiểm tra tình trạng dịch vụ phục vụ giám sát và triển khai không gián đoạn (Zero-Downtime Deployment) trên Render:
* **Health Check**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
* **Thông tin ứng dụng**: [http://localhost:8080/actuator/info](http://localhost:8080/actuator/info)

---

## 11. Triển Khai Trên Render & Neon (Deployment Overview)

1. **Database**: Khởi tạo project PostgreSQL trên **Neon**. Lấy chuỗi Pooled connection string (chứa `-pooler` trong host).
2. **Web Service Render**:
   * Liên kết với GitHub repository.
   * Chọn môi trường: `Docker`.
   * Cấu hình Environment Variables trên Render Dashboard theo bảng mục 4 với `SPRING_PROFILES_ACTIVE=prod`.
   * Health Check Path: `/actuator/health`.
3. Ứng dụng hỗ trợ Graceful Shutdown (tối đa 30s) đảm bảo các kết nối đang xử lý được hoàn thành an toàn trước khi dừng container.
