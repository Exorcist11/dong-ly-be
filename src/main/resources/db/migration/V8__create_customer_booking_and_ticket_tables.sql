-- V8__create_customer_booking_and_ticket_tables.sql
-- Khởi tạo cấu trúc các bảng Quản lý Khách hàng, Đặt vé (Booking), Chi tiết vé (BookingItem), Thanh toán và Vé điện tử cho Đông Lý

-- 1. Bảng customers: Hồ sơ khách hàng đặt vé (CRM / Khách quen)
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    note TEXT,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customers_phone UNIQUE (phone),
    CONSTRAINT chk_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers (phone);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers (status);

-- 2. Bảng bookings: Đơn đặt vé tổng
CREATE TABLE IF NOT EXISTS bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_code VARCHAR(50) NOT NULL,
    trip_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'CRM_PHONE',
    status VARCHAR(20) NOT NULL DEFAULT 'HELD',
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    hold_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    note TEXT,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_bookings_code UNIQUE (booking_code),
    CONSTRAINT fk_bookings_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT,
    CONSTRAINT chk_bookings_channel CHECK (channel IN ('CRM_PHONE', 'CRM_POS', 'ONLINE')),
    CONSTRAINT chk_bookings_status CHECK (status IN ('HELD', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'COMPLETED'))
);

CREATE INDEX IF NOT EXISTS idx_bookings_code ON bookings (booking_code);
CREATE INDEX IF NOT EXISTS idx_bookings_trip ON bookings (trip_id);
CREATE INDEX IF NOT EXISTS idx_bookings_customer ON bookings (customer_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings (status);
CREATE INDEX IF NOT EXISTS idx_bookings_hold_expires ON bookings (hold_expires_at);

-- 3. Bảng booking_items: Từng vị trí ghế / hành khách trong đơn vé
CREATE TABLE IF NOT EXISTS booking_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    seat_code VARCHAR(20) NOT NULL,
    passenger_name VARCHAR(100),
    passenger_phone VARCHAR(20),
    pickup_stop_id UUID,
    dropoff_stop_id UUID,
    base_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    seat_extra_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    pickup_extra_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    dropoff_extra_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    final_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'HELD',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_items_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_items_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_items_seat FOREIGN KEY (seat_id) REFERENCES vehicle_seats (id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_items_pickup FOREIGN KEY (pickup_stop_id) REFERENCES route_stops (id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_items_dropoff FOREIGN KEY (dropoff_stop_id) REFERENCES route_stops (id) ON DELETE RESTRICT,
    CONSTRAINT chk_booking_items_status CHECK (status IN ('HELD', 'CONFIRMED', 'CANCELLED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_booking_items_booking ON booking_items (booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_items_trip ON booking_items (trip_id);
CREATE INDEX IF NOT EXISTS idx_booking_items_seat ON booking_items (seat_id);
CREATE INDEX IF NOT EXISTS idx_booking_items_status ON booking_items (status);

-- 4. PARTIAL UNIQUE INDEX: BẢO VỆ CHỐNG GIỮ / ĐẶT TRÙNG GHẾ TRÊN CÙNG CHUYẾN XE (DATABASE LEVEL CONSTRAINT)
CREATE UNIQUE INDEX IF NOT EXISTS uq_booking_items_active_trip_seat
ON booking_items (trip_id, seat_id)
WHERE status IN ('HELD', 'CONFIRMED');

-- 5. Bảng payments: Giao dịch thanh toán & hoàn tiền
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    payment_method VARCHAR(30) NOT NULL DEFAULT 'CASH',
    status VARCHAR(20) NOT NULL DEFAULT 'PAID',
    amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    transaction_code VARCHAR(100),
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    received_by VARCHAR(50),
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE RESTRICT,
    CONSTRAINT chk_payments_method CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'VIETQR')),
    CONSTRAINT chk_payments_status CHECK (status IN ('PAID', 'CANCELLED', 'REFUNDED'))
);

CREATE INDEX IF NOT EXISTS idx_payments_booking ON payments (booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments (status);

-- 6. Bảng tickets: Vé điện tử chính thức được phát hành kèm mã QR
CREATE TABLE IF NOT EXISTS tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_code VARCHAR(50) NOT NULL,
    booking_item_id UUID NOT NULL,
    qr_code_data TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checked_in_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tickets_code UNIQUE (ticket_code),
    CONSTRAINT uq_tickets_item UNIQUE (booking_item_id),
    CONSTRAINT fk_tickets_item FOREIGN KEY (booking_item_id) REFERENCES booking_items (id) ON DELETE RESTRICT,
    CONSTRAINT chk_tickets_status CHECK (status IN ('ISSUED', 'CHECKED_IN', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_tickets_code ON tickets (ticket_code);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets (status);
