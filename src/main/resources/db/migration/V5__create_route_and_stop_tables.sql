-- V5__create_route_and_stop_tables.sql
-- Khởi tạo cấu trúc các bảng Quản lý Tuyến đường & Điểm đón/trả cho hệ thống vận tải Đông Lý

-- 1. Bảng locations: Danh mục địa phương / Tỉnh thành khai thác
CREATE TABLE IF NOT EXISTS locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    province VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_locations_code UNIQUE (code),
    CONSTRAINT chk_locations_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_locations_code ON locations (code);
CREATE INDEX IF NOT EXISTS idx_locations_status ON locations (status);

-- 2. Bảng stop_points: Danh mục điểm đón/trả vật lý độc lập (Master Data)
CREATE TABLE IF NOT EXISTS stop_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    location_id UUID NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    contact_phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_stop_points_code UNIQUE (code),
    CONSTRAINT fk_stop_points_location FOREIGN KEY (location_id) REFERENCES locations (id) ON DELETE RESTRICT,
    CONSTRAINT chk_stop_points_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_stop_points_code ON stop_points (code);
CREATE INDEX IF NOT EXISTS idx_stop_points_location ON stop_points (location_id);
CREATE INDEX IF NOT EXISTS idx_stop_points_status ON stop_points (status);

-- 3. Bảng routes: Tuyến đường vận tải cố định kết nối 2 địa phương
CREATE TABLE IF NOT EXISTS routes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    origin_location_id UUID NOT NULL,
    destination_location_id UUID NOT NULL,
    distance_km NUMERIC(6, 2),
    estimated_duration_minutes INTEGER,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_routes_code UNIQUE (code),
    CONSTRAINT fk_routes_origin FOREIGN KEY (origin_location_id) REFERENCES locations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_routes_destination FOREIGN KEY (destination_location_id) REFERENCES locations (id) ON DELETE RESTRICT,
    CONSTRAINT chk_routes_origin_dest CHECK (origin_location_id <> destination_location_id),
    CONSTRAINT chk_routes_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_routes_code ON routes (code);
CREATE INDEX IF NOT EXISTS idx_routes_origin ON routes (origin_location_id);
CREATE INDEX IF NOT EXISTS idx_routes_destination ON routes (destination_location_id);
CREATE INDEX IF NOT EXISTS idx_routes_status ON routes (status);

-- 4. Bảng route_stops: Quan hệ N-N điểm dừng gắn trên từng tuyến xe theo chiều và thứ tự
CREATE TABLE IF NOT EXISTS route_stops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id UUID NOT NULL,
    stop_point_id UUID NOT NULL,
    direction VARCHAR(20) NOT NULL DEFAULT 'OUTBOUND',
    sequence INTEGER NOT NULL,
    stop_type VARCHAR(20) NOT NULL DEFAULT 'BOTH',
    extra_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_route_stops_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_route_stops_point FOREIGN KEY (stop_point_id) REFERENCES stop_points (id) ON DELETE RESTRICT,
    CONSTRAINT chk_route_stops_direction CHECK (direction IN ('OUTBOUND', 'RETURN', 'BOTH')),
    CONSTRAINT chk_route_stops_stop_type CHECK (stop_type IN ('PICKUP', 'DROPOFF', 'BOTH')),
    CONSTRAINT chk_route_stops_sequence CHECK (sequence >= 1),
    CONSTRAINT chk_route_stops_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT uq_route_stops_route_dir_seq UNIQUE (route_id, direction, sequence),
    CONSTRAINT uq_route_stops_route_dir_point UNIQUE (route_id, direction, stop_point_id)
);

CREATE INDEX IF NOT EXISTS idx_route_stops_route ON route_stops (route_id);
CREATE INDEX IF NOT EXISTS idx_route_stops_point ON route_stops (stop_point_id);
CREATE INDEX IF NOT EXISTS idx_route_stops_seq ON route_stops (route_id, direction, sequence);

-- 5. Seed dữ liệu địa danh mẫu chủ lực cho nhà xe Đông Lý
INSERT INTO locations (id, code, name, province, status)
VALUES
    ('a1111111-1111-1111-1111-111111111111', 'THANH_HOA', 'Thanh Hóa', 'Thanh Hóa', 'ACTIVE'),
    ('a2222222-2222-2222-2222-222222222222', 'HA_NOI', 'Hà Nội', 'Hà Nội', 'ACTIVE'),
    ('a3333333-3333-3333-3333-333333333333', 'HAI_PHONG', 'Hải Phòng', 'Hải Phòng', 'ACTIVE'),
    ('a4444444-4444-4444-4444-444444444444', 'BAC_NINH', 'Bắc Ninh', 'Bắc Ninh', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;
