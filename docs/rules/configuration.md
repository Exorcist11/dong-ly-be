# Quản Lý Cấu Hình Đa Môi Trường (Configuration Guidelines)

Tài liệu này xác định chiến lược quản lý tệp cấu hình `application.yml`, các profile môi trường (`dev`, `prod`, `test`) và việc sử dụng biến môi trường.

---

## 1. Cấu Trúc File Cấu Hình Spring Boot

Cấu hình của dự án được chia tách thành các tệp chuyên biệt:

```text
src/main/resources/
├── application.yml          # Cấu hình gốc, mặc định cho mọi môi trường
├── application-dev.yml      # Cấu hình phát triển cục bộ (Local Development)
├── application-prod.yml     # Cấu hình sản xuất (Production - Render & Neon)
└── application-test.yml     # Cấu hình kiểm thử tự động (Unit / Integration Test)
```

---

## 2. Nguyên Tắc Quản Lý Cấu Hình

### 2.1. Cấu Hình Gốc (`application.yml`)
* Khai báo các thiết lập phi nhạy cảm, có giá trị mặc định an toàn.
* Tên ứng dụng, định dạng thời gian, cổng mặc định, tiền tố API:
```yaml
spring:
  application:
    name: spring-backend
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev} # Mặc định là dev nếu không chỉ định
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false

server:
  port: ${PORT:8080} # Render tự động gán biến PORT ngẫu nhiên
  servlet:
    context-path: /
  error:
    include-stacktrace: never
```

### 2.2. Môi Trường Cục Bộ (`application-dev.yml`)
* Kết nối tới PostgreSQL nội bộ (localhost:5432 hoặc Docker Compose).
* Bật log SQL phục vụ gỡ lỗi:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/spring_dev_db
    username: postgres
    password: postgres_dev_password
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    com.company.project: DEBUG
```

### 2.3. Môi Trường Sản Xuất (`application-prod.yml`)
* **BẮT BUỘC** nhận toàn bộ thông số kết nối từ biến môi trường:
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 10
      connection-timeout: 30000
      keepalive-time: 30000
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate # Không bao giờ để update hay create
  flyway:
    enabled: true
    baseline-on-migrate: true

logging:
  level:
    root: INFO
    com.company.project: INFO
```

---

## 3. Khởi Động Thất Bại Ngay Lập Tức (Fail-Fast Principle)

* Nếu một biến môi trường quan trọng bị thiếu (như `DATABASE_URL`, `JWT_SECRET`), ứng dụng **BẮT BUỘC PHẢI KHÔNG THỂ KHỞI ĐỘNG** và in ra thông báo lỗi rõ ràng.
* Không thiết lập giá trị bí mật mặc định giả mạo trên code để "chữa cháy" (ví dụ: cấm `jwt.secret: ${JWT_SECRET:secret123456}`).
* Sử dụng `@ConfigurationProperties` kết hợp Jakarta Bean Validation (`@NotBlank`) để kiểm tra toàn bộ cấu hình ngay khi ứng dụng khởi chạy.
