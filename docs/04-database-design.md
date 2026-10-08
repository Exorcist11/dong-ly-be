# Thiết Kế Cơ Sở Dữ Liệu: Authentication & User Management (Database Design)

Tài liệu này đặc tả chi tiết lược đồ cơ sở dữ liệu PostgreSQL cho Module **Authentication & User Management (BE-001)** của Hệ thống Quản lý Vận tải & Đặt vé Đông Lý.

---

## 1. Lược Đồ Bảng (Database Schema)

### 1.1. Bảng `users` (Người dùng hệ thống)
Lưu trữ thông tin tài khoản người dùng, nhân viên, quản trị viên và khách hàng.

```sql
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_status ON users (status);
```

### 1.2. Bảng `roles` (Vai trò)
Định nghĩa danh mục các vai trò trong mô hình RBAC.

```sql
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_roles_code UNIQUE (code)
);

CREATE INDEX idx_roles_code ON roles (code);
```

### 1.3. Bảng `permissions` (Quyền hạn hạt nhân)
Định nghĩa các quyền hạn cụ thể trong hệ thống.

```sql
CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_permissions_code UNIQUE (code)
);

CREATE INDEX idx_permissions_code ON permissions (code);
```

### 1.4. Bảng `user_roles` (Liên kết N-N Người dùng ↔ Vai trò)

```sql
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
```

### 1.5. Bảng `role_permissions` (Liên kết N-N Vai trò ↔ Quyền hạn)

```sql
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions (role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);
```

### 1.6. Bảng `refresh_tokens` (Mã làm mới phiên đăng nhập)
Lưu trữ hash SHA-256 của refresh token phục vụ thu hồi và xoay vòng token an toàn.

```sql
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_by_ip VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
```

---

## 2. Danh Sách Flyway Migrations Đã Thực Thi

1. `V1__init_schema.sql`: Kích hoạt extension PostgreSQL `pgcrypto`.
2. `V2__create_auth_tables.sql`: Khởi tạo cấu trúc 6 bảng `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens` cùng indexes và constraints.
3. `V3__seed_auth_roles_permissions.sql`: Khởi tạo dữ liệu hạt giống:
   * Roles: `ADMIN`, `OPERATOR`, `STAFF`, `CUSTOMER`.
   * Permissions: `USER_READ`, `USER_CREATE`, `USER_UPDATE`, `USER_DELETE`.
   * Gán quyền `USER_*` cho vai trò `ADMIN`.
   * Khởi tạo tài khoản quản trị viên mặc định `admin` / `admin@dongly.vn` (mật khẩu mã hóa BCrypt cost 12).
