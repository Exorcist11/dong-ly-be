-- V7__create_triprun_and_trip_tables.sql
-- Khởi tạo cấu trúc bảng Lịch vòng chạy (trip_runs) và Chuyến xe thực tế (trips) cho nhà xe Đông Lý

-- 1. Bảng trip_runs: Cấu hình lịch vận hành định kỳ
CREATE TABLE IF NOT EXISTS trip_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    route_id UUID NOT NULL,
    departure_time TIME NOT NULL,
    days_of_week VARCHAR(50) NOT NULL DEFAULT '1,2,3,4,5,6,7',
    start_date DATE NOT NULL,
    end_date DATE,
    default_vehicle_id UUID,
    default_driver_id UUID,
    default_assistant_driver_id UUID,
    base_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    note TEXT,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_trip_runs_code UNIQUE (code),
    CONSTRAINT fk_trip_runs_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_trip_runs_default_vehicle FOREIGN KEY (default_vehicle_id) REFERENCES vehicles (id) ON DELETE SET NULL,
    CONSTRAINT fk_trip_runs_default_driver FOREIGN KEY (default_driver_id) REFERENCES drivers (id) ON DELETE SET NULL,
    CONSTRAINT fk_trip_runs_default_assistant FOREIGN KEY (default_assistant_driver_id) REFERENCES drivers (id) ON DELETE SET NULL,
    CONSTRAINT chk_trip_runs_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_trip_runs_date_range CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_trip_runs_default_drivers CHECK (
        default_driver_id IS NULL OR default_assistant_driver_id IS NULL OR default_driver_id <> default_assistant_driver_id
    )
);

CREATE INDEX IF NOT EXISTS idx_trip_runs_code ON trip_runs (code);
CREATE INDEX IF NOT EXISTS idx_trip_runs_route ON trip_runs (route_id);
CREATE INDEX IF NOT EXISTS idx_trip_runs_status ON trip_runs (status);
CREATE INDEX IF NOT EXISTS idx_trip_runs_dates ON trip_runs (start_date, end_date);

-- 2. Bảng trips: Quản lý chuyến xe vận hành thực tế
CREATE TABLE IF NOT EXISTS trips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    trip_run_id UUID,
    route_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    assistant_driver_id UUID NOT NULL,
    departure_time TIMESTAMP WITH TIME ZONE NOT NULL,
    estimated_arrival_time TIMESTAMP WITH TIME ZONE NOT NULL,
    actual_departure_time TIMESTAMP WITH TIME ZONE,
    actual_arrival_time TIMESTAMP WITH TIME ZONE,
    base_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    note TEXT,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_trips_code UNIQUE (code),
    CONSTRAINT uq_trips_run_departure UNIQUE (trip_run_id, departure_time),
    CONSTRAINT fk_trips_run FOREIGN KEY (trip_run_id) REFERENCES trip_runs (id) ON DELETE SET NULL,
    CONSTRAINT fk_trips_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_trips_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_trips_driver FOREIGN KEY (driver_id) REFERENCES drivers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_trips_assistant FOREIGN KEY (assistant_driver_id) REFERENCES drivers (id) ON DELETE RESTRICT,
    CONSTRAINT chk_trips_driver_assistant CHECK (driver_id <> assistant_driver_id),
    CONSTRAINT chk_trips_time CHECK (estimated_arrival_time > departure_time),
    CONSTRAINT chk_trips_status CHECK (status IN ('SCHEDULED', 'READY', 'DEPARTED', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_trips_code ON trips (code);
CREATE INDEX IF NOT EXISTS idx_trips_run ON trips (trip_run_id);
CREATE INDEX IF NOT EXISTS idx_trips_route ON trips (route_id);
CREATE INDEX IF NOT EXISTS idx_trips_vehicle_time ON trips (vehicle_id, departure_time, estimated_arrival_time);
CREATE INDEX IF NOT EXISTS idx_trips_driver_time ON trips (driver_id, departure_time, estimated_arrival_time);
CREATE INDEX IF NOT EXISTS idx_trips_assistant_time ON trips (assistant_driver_id, departure_time, estimated_arrival_time);
CREATE INDEX IF NOT EXISTS idx_trips_departure_time ON trips (departure_time);
CREATE INDEX IF NOT EXISTS idx_trips_status ON trips (status);
