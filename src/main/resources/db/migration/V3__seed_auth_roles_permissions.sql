-- V3__seed_auth_roles_permissions.sql
-- Khởi tạo dữ liệu hạt giống ban đầu (Roles, Permissions, RolePermissions, Admin User)

-- 1. Seed Roles cơ bản
INSERT INTO roles (id, code, name, description)
VALUES 
    (gen_random_uuid(), 'ADMIN', 'Quản trị viên', 'Quản trị toàn quyền hệ thống và phân quyền người dùng'),
    (gen_random_uuid(), 'OPERATOR', 'Điều hành vận tải', 'Quản lý lịch trình, điều phối chuyến và phương tiện'),
    (gen_random_uuid(), 'STAFF', 'Nhân viên bán vé', 'Bán vé, tra cứu và xử lý nghiệp vụ đặt vé tại quầy/tổng đài'),
    (gen_random_uuid(), 'CUSTOMER', 'Khách hàng', 'Người dùng tra cứu và đặt vé xe trực tuyến')
ON CONFLICT (code) DO NOTHING;

-- 2. Seed Permissions nền tảng cho User Management
INSERT INTO permissions (id, code, name, description)
VALUES
    (gen_random_uuid(), 'USER_READ', 'Xem người dùng', 'Quyền tra cứu danh sách và xem chi tiết hồ sơ người dùng'),
    (gen_random_uuid(), 'USER_CREATE', 'Tạo người dùng', 'Quyền tạo mới tài khoản người dùng và gán vai trò'),
    (gen_random_uuid(), 'USER_UPDATE', 'Cập nhật người dùng', 'Quyền sửa đổi thông tin hoặc cập nhật trạng thái người dùng'),
    (gen_random_uuid(), 'USER_DELETE', 'Vô hiệu hóa người dùng', 'Quyền vô hiệu hóa tài khoản người dùng')
ON CONFLICT (code) DO NOTHING;

-- 3. Gán toàn bộ Permissions cho vai trò ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 4. Seed tài khoản Admin mặc định (mật khẩu mặc định: Admin@123)
-- Hash BCrypt cost 12: $2a$12$3SZ83askNMehOtF1FIc1GOqjqHIZQDs/XOU.v5OJ6IEcQXqkKDBu2
INSERT INTO users (id, username, email, password_hash, full_name, phone, status, created_by)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@dongly.vn',
    '$2a$12$3SZ83askNMehOtF1FIc1GOqjqHIZQDs/XOU.v5OJ6IEcQXqkKDBu2',
    'Quản trị viên Hệ thống Đông Lý',
    '0987654321',
    'ACTIVE',
    'SYSTEM'
)
ON CONFLICT (username) DO NOTHING;

-- 5. Gán vai trò ADMIN cho tài khoản admin mặc định
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.code = 'ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
