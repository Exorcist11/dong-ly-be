-- V6__create_fleet_vehicles_seats_and_drivers.sql
-- Khởi tạo cấu trúc các bảng Quản lý Phương tiện, Sơ đồ ghế và Tài xế cho nhà xe Đông Lý

-- 1. Bảng vehicles: Quản lý phương tiện vận tải
CREATE TABLE IF NOT EXISTS vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plate_number VARCHAR(20) NOT NULL,
    vehicle_type VARCHAR(30) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100),
    manufacture_year INTEGER,
    total_floors INTEGER NOT NULL DEFAULT 1,
    total_rows INTEGER NOT NULL DEFAULT 5,
    total_columns INTEGER NOT NULL DEFAULT 4,
    total_seats INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_vehicles_plate_number UNIQUE (plate_number),
    CONSTRAINT chk_vehicles_status CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'INACTIVE')),
    CONSTRAINT chk_vehicles_type CHECK (vehicle_type IN ('SLEEPER', 'LIMOUSINE', 'SEATER')),
    CONSTRAINT chk_vehicles_floors CHECK (total_floors IN (1, 2)),
    CONSTRAINT chk_vehicles_grid CHECK (total_rows >= 1 AND total_columns >= 1)
);

CREATE INDEX IF NOT EXISTS idx_vehicles_plate_number ON vehicles (plate_number);
CREATE INDEX IF NOT EXISTS idx_vehicles_status ON vehicles (status);
CREATE INDEX IF NOT EXISTS idx_vehicles_type ON vehicles (vehicle_type);

-- 2. Bảng vehicle_seats: Từng vị trí ghế/giường vật lý trên phương tiện
CREATE TABLE IF NOT EXISTS vehicle_seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL,
    seat_code VARCHAR(20) NOT NULL,
    floor INTEGER NOT NULL DEFAULT 1,
    row_index INTEGER NOT NULL,
    column_index INTEGER NOT NULL,
    seat_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    extra_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_seats_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE RESTRICT,
    CONSTRAINT chk_vehicle_seats_floor CHECK (floor IN (1, 2)),
    CONSTRAINT chk_vehicle_seats_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'INACTIVE')),
    CONSTRAINT chk_vehicle_seats_type CHECK (seat_type IN ('STANDARD', 'VIP', 'SLEEPER', 'LUXURY_ROOM')),
    CONSTRAINT chk_vehicle_seats_coords CHECK (row_index >= 1 AND column_index >= 1),
    CONSTRAINT uq_vehicle_seats_vehicle_code UNIQUE (vehicle_id, seat_code),
    CONSTRAINT uq_vehicle_seats_vehicle_coords UNIQUE (vehicle_id, floor, row_index, column_index)
);

CREATE INDEX IF NOT EXISTS idx_vehicle_seats_vehicle ON vehicle_seats (vehicle_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_seats_status ON vehicle_seats (status);
CREATE INDEX IF NOT EXISTS idx_vehicle_seats_code ON vehicle_seats (vehicle_id, seat_code);

-- 3. Bảng drivers: Hồ sơ tài xế nhà xe Đông Lý
CREATE TABLE IF NOT EXISTS drivers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    license_number VARCHAR(30) NOT NULL,
    license_class VARCHAR(10) NOT NULL,
    license_expiry_date DATE,
    date_of_birth DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    note TEXT,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_drivers_code UNIQUE (code),
    CONSTRAINT uq_drivers_phone UNIQUE (phone),
    CONSTRAINT uq_drivers_license UNIQUE (license_number),
    CONSTRAINT chk_drivers_status CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_drivers_code ON drivers (code);
CREATE INDEX IF NOT EXISTS idx_drivers_phone ON drivers (phone);
CREATE INDEX IF NOT EXISTS idx_drivers_license ON drivers (license_number);
CREATE INDEX IF NOT EXISTS idx_drivers_status ON drivers (status);

-- 4. Seed dữ liệu mẫu: 2 phương tiện và 2 tài xế chủ lực của Đông Lý
INSERT INTO vehicles (id, plate_number, vehicle_type, brand, model, manufacture_year, total_floors, total_rows, total_columns, total_seats, status, description, created_by)
VALUES
    ('b1111111-1111-1111-1111-111111111111', '36B-028.68', 'LIMOUSINE', 'Thaco Mobihome', 'VIP 22 Phòng', 2024, 2, 6, 3, 22, 'ACTIVE', 'Xe Limousine Cung Điện VIP 22 phòng cao cấp', 'SYSTEM'),
    ('b2222222-2222-2222-2222-222222222222', '36B-031.99', 'SLEEPER', 'Hyundai Universe', 'Giường nằm 34 phòng', 2023, 2, 6, 3, 34, 'ACTIVE', 'Xe giường nằm 34 phòng tiêu chuẩn tuyến Triệu Sơn - Giáp Bát', 'SYSTEM')
ON CONFLICT (plate_number) DO NOTHING;

-- Seed sơ đồ ghế mẫu cho xe Limousine 36B-028.68 (Tầng 1: 11 phòng A01-A11; Tầng 2: 11 phòng B01-B11)
INSERT INTO vehicle_seats (vehicle_id, seat_code, floor, row_index, column_index, seat_type, extra_price, status, created_by)
VALUES
    -- Tầng 1 (Cột 1 và Cột 3 là phòng nằm, Cột 2 là lối đi)
    ('b1111111-1111-1111-1111-111111111111', 'A01', 1, 1, 1, 'LUXURY_ROOM', 50000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'A02', 1, 1, 3, 'LUXURY_ROOM', 50000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'A03', 1, 2, 1, 'LUXURY_ROOM', 50000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'A04', 1, 2, 3, 'LUXURY_ROOM', 50000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'A05', 1, 3, 1, 'LUXURY_ROOM', 50000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'A06', 1, 3, 3, 'LUXURY_ROOM', 50000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'A07', 1, 4, 1, 'LUXURY_ROOM', 50000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'A08', 1, 4, 3, 'LUXURY_ROOM', 50000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'A09', 1, 5, 1, 'LUXURY_ROOM', 50000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'A10', 1, 5, 3, 'LUXURY_ROOM', 50000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'A11', 1, 6, 2, 'LUXURY_ROOM', 50000.00, 'ACTIVE', 'SYSTEM'),
    -- Tầng 2
    ('b1111111-1111-1111-1111-111111111111', 'B01', 2, 1, 1, 'LUXURY_ROOM', 30000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'B02', 2, 1, 3, 'LUXURY_ROOM', 30000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'B03', 2, 2, 1, 'LUXURY_ROOM', 30000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'B04', 2, 2, 3, 'LUXURY_ROOM', 30000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'B05', 2, 3, 1, 'LUXURY_ROOM', 30000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'B06', 2, 3, 3, 'LUXURY_ROOM', 30000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'B07', 2, 4, 1, 'LUXURY_ROOM', 30000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'B08', 2, 4, 3, 'LUXURY_ROOM', 30000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'B09', 2, 5, 1, 'LUXURY_ROOM', 30000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'B10', 2, 5, 3, 'LUXURY_ROOM', 30000.00, 'ACTIVE', 'SYSTEM'),
    ('b1111111-1111-1111-1111-111111111111', 'B11', 2, 6, 2, 'LUXURY_ROOM', 30000.00, 'ACTIVE', 'SYSTEM')
ON CONFLICT (vehicle_id, seat_code) DO NOTHING;

-- Seed hồ sơ tài xế mẫu
INSERT INTO drivers (id, code, full_name, phone, license_number, license_class, license_expiry_date, date_of_birth, status, note, created_by)
VALUES
    ('c1111111-1111-1111-1111-111111111111', 'TX-001', 'Nguyễn Văn Đông', '0912345678', '010123456789', 'E', '2029-12-31', '1982-05-15', 'ACTIVE', 'Tài xế chính tuyến Triệu Sơn - Mỹ Đình', 'SYSTEM'),
    ('c2222222-2222-2222-2222-222222222222', 'TX-002', 'Lê Hữu Lý', '0987654321', '010987654321', 'E', '2028-06-30', '1985-11-20', 'ACTIVE', 'Tài xế chính tuyến Triệu Sơn - Giáp Bát', 'SYSTEM')
ON CONFLICT (code) DO NOTHING;
