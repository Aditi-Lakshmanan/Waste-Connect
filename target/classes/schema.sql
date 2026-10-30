-- WasteConnect Database Schema
-- H2 dialect

CREATE TABLE IF NOT EXISTS zones (
    id IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(256) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    name VARCHAR(100),
    phone VARCHAR(15),
    address VARCHAR(255),
    zone_id BIGINT,
    status VARCHAR(20),
    extra VARCHAR(255) -- JSON or CSV for role‑specific fields
);

CREATE TABLE IF NOT EXISTS bins (
    id IDENTITY PRIMARY KEY,
    household_user_id VARCHAR(50) REFERENCES users(user_id),
    zone_id BIGINT REFERENCES zones(id),
    fill_level INT CHECK (fill_level BETWEEN 0 AND 100),
    waste_type VARCHAR(30),
    status VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS vehicles (
    id IDENTITY PRIMARY KEY,
    truck_type VARCHAR(30),
    capacity_kg INT,
    licence_plate VARCHAR(20) UNIQUE,
    collector_user_id VARCHAR(50) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS routes (
    id IDENTITY PRIMARY KEY,
    zone_id BIGINT REFERENCES zones(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) -- OPEN, ACCEPTED, IN_PROGRESS, COMPLETED
);

CREATE TABLE IF NOT EXISTS route_stops (
    id IDENTITY PRIMARY KEY,
    route_id BIGINT REFERENCES routes(id),
    stop_order INT,
    household_user_id VARCHAR(50) REFERENCES users(user_id),
    status VARCHAR(20) -- PENDING, COLLECTED, NOT_COLLECTED, BLOCKED
);

CREATE TABLE IF NOT EXISTS collection_requests (
    id IDENTITY PRIMARY KEY,
    citizen_user_id VARCHAR(50) REFERENCES users(user_id),
    waste_type VARCHAR(30),
    approx_weight_kg INT,
    preferred_slot TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING'
);

CREATE TABLE IF NOT EXISTS complaints (
    id IDENTITY PRIMARY KEY,
    citizen_user_id VARCHAR(50) REFERENCES users(user_id),
    zone_id BIGINT REFERENCES zones(id),
    category VARCHAR(50),
    description CLOB,
    status VARCHAR(20) DEFAULT 'OPEN',
    assigned_collector_user_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    note CLOB
);

CREATE TABLE IF NOT EXISTS recycle_tickets (
    id IDENTITY PRIMARY KEY,
    citizen_user_id VARCHAR(50) REFERENCES users(user_id),
    picker_user_id VARCHAR(50) REFERENCES users(user_id),
    item_type VARCHAR(50),
    weight_kg INT,
    status VARCHAR(20) DEFAULT 'REQUESTED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id IDENTITY PRIMARY KEY,
    recipient_user_id VARCHAR(50) REFERENCES users(user_id),
    message CLOB,
    read_flag BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed admin account (password will be hashed on first run by UserService)
INSERT INTO users (user_id, password_hash, salt, role, name, status)
SELECT 'admin@wasteconnect.com', 'ADMIN_SEED', 'SALT', 'ADMIN', 'Admin', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_id = 'admin@wasteconnect.com');
