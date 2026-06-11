-- Drop tables in reverse dependency order (orders references customer_sessions)
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS customer_sessions;
-- Note: 'users' table was a Phase 1 placeholder; it has been fully removed.

-- Customer sessions table
CREATE TABLE IF NOT EXISTS customer_sessions (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_uuid    VARCHAR(36) NOT NULL UNIQUE,
    table_id        INT NOT NULL,
    checked_in_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status          ENUM('ACTIVE', 'EXPIRED', 'CHECKED_OUT') DEFAULT 'ACTIVE',
    INDEX idx_session_uuid (session_uuid),
    INDEX idx_status (status)
);

-- Orders table
-- Phase 4: added 'price' column (nullable to allow schema migrations on existing DBs)
CREATE TABLE IF NOT EXISTS orders (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_uuid VARCHAR(36) NOT NULL,
    table_id     INT NOT NULL,
    item_name    VARCHAR(255) NOT NULL,
    price        INT NULL COMMENT 'Price in rupees at time of order (null for legacy Phase 1/2 orders)',
    status       ENUM('RECEIVED', 'PREPARING', 'SERVED') DEFAULT 'RECEIVED',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_session (session_uuid),
    INDEX idx_order_table   (table_id),
    INDEX idx_order_status  (status),
    FOREIGN KEY (session_uuid) REFERENCES customer_sessions(session_uuid)
);