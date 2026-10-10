-- V4__enhance_rbac_tables_and_seed_permissions.sql
-- Nâng cấp cấu trúc bảng roles, permissions và seed bổ sung danh mục quyền hạn RBAC & phân hệ vận tải

-- 1. Bổ sung các cột trạng thái và cờ hệ thống cho bảng roles
ALTER TABLE roles ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE roles ADD COLUMN IF NOT EXISTS is_system BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_roles_status'
    ) THEN
        ALTER TABLE roles ADD CONSTRAINT chk_roles_status CHECK (status IN ('ACTIVE', 'INACTIVE'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_roles_status ON roles (status);

-- 2. Bổ sung các cột module và action cho bảng permissions
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS module VARCHAR(50) NOT NULL DEFAULT 'SYSTEM';
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS action VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_permissions_module ON permissions (module);

-- 3. Cập nhật các role mặc định thành system role (bảo vệ khỏi việc xóa/sửa nguy hiểm)
UPDATE roles
SET is_system = TRUE
WHERE code IN ('ADMIN', 'OPERATOR', 'STAFF', 'CUSTOMER');

-- 4. Chuẩn hóa module và action cho các permission hiện hữu
UPDATE permissions SET module = 'USER', action = 'READ' WHERE code = 'USER_READ';
UPDATE permissions SET module = 'USER', action = 'CREATE' WHERE code = 'USER_CREATE';
UPDATE permissions SET module = 'USER', action = 'UPDATE' WHERE code = 'USER_UPDATE';
UPDATE permissions SET module = 'USER', action = 'DELETE' WHERE code = 'USER_DELETE';

-- 5. Seed danh mục quyền quản trị RBAC (Roles & Permissions)
INSERT INTO permissions (id, code, name, description, module, action)
VALUES
    (gen_random_uuid(), 'ROLE_READ', 'Xem vai trò', 'Quyền tra cứu danh sách và xem chi tiết vai trò', 'ROLE', 'READ'),
    (gen_random_uuid(), 'ROLE_CREATE', 'Tạo vai trò', 'Quyền tạo mới vai trò trong hệ thống', 'ROLE', 'CREATE'),
    (gen_random_uuid(), 'ROLE_UPDATE', 'Cập nhật vai trò', 'Quyền sửa đổi thông tin và trạng thái vai trò', 'ROLE', 'UPDATE'),
    (gen_random_uuid(), 'ROLE_DELETE', 'Vô hiệu hóa/Xóa vai trò', 'Quyền vô hiệu hóa hoặc xóa vai trò tùy chỉnh', 'ROLE', 'DELETE'),
    (gen_random_uuid(), 'ROLE_ASSIGN', 'Gán quyền cho vai trò', 'Quyền gán hoặc thu hồi quyền hạn của vai trò và người dùng', 'ROLE', 'ASSIGN'),
    (gen_random_uuid(), 'PERMISSION_READ', 'Xem danh mục quyền', 'Quyền tra cứu danh mục quyền hạn của hệ thống', 'PERMISSION', 'READ')
ON CONFLICT (code) DO UPDATE
SET module = EXCLUDED.module,
    action = EXCLUDED.action,
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 6. Chuẩn bị catalog quyền hạn các phân hệ vận tải cho các giai đoạn tiếp theo
INSERT INTO permissions (id, code, name, description, module, action)
VALUES
    (gen_random_uuid(), 'ROUTE_READ', 'Xem tuyến đường', 'Quyền xem danh sách tuyến đường, hướng tuyến và điểm dừng', 'ROUTE', 'READ'),
    (gen_random_uuid(), 'ROUTE_MANAGE', 'Quản lý tuyến đường', 'Quyền tạo, sửa, xóa cấu hình tuyến và điểm đón/trả', 'ROUTE', 'MANAGE'),
    (gen_random_uuid(), 'FLEET_READ', 'Xem đội xe & tài xế', 'Quyền xem phương tiện, sơ đồ ghế và danh sách tài xế', 'FLEET', 'READ'),
    (gen_random_uuid(), 'FLEET_MANAGE', 'Quản lý đội xe & tài xế', 'Quyền quản lý phương tiện, cấu hình ghế và tài xế', 'FLEET', 'MANAGE'),
    (gen_random_uuid(), 'TRIP_READ', 'Xem lịch trình chuyến', 'Quyền xem vòng chạy xe (TripRun) và lịch chuyến xe (Trip)', 'TRIP', 'READ'),
    (gen_random_uuid(), 'TRIP_MANAGE', 'Điều hành chuyến xe', 'Quyền lập lịch, điều phối phương tiện, gán tài xế và mở bán vé', 'TRIP', 'MANAGE'),
    (gen_random_uuid(), 'BOOKING_READ', 'Tra cứu đặt vé', 'Quyền tra cứu đơn đặt vé và danh sách hành khách', 'BOOKING', 'READ'),
    (gen_random_uuid(), 'BOOKING_MANAGE', 'Xử lý đơn đặt vé', 'Quyền tạo đặt vé tại quầy, đổi ghế, xác nhận thanh toán hoặc hủy vé', 'BOOKING', 'MANAGE'),
    (gen_random_uuid(), 'TICKET_READ', 'Tra cứu & In vé', 'Quyền tra cứu vé điện tử, in vé và kiểm soát vé', 'TICKET', 'READ')
ON CONFLICT (code) DO UPDATE
SET module = EXCLUDED.module,
    action = EXCLUDED.action,
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 7. Gán toàn bộ quyền mới cho vai trò ADMIN (idempotent)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;
