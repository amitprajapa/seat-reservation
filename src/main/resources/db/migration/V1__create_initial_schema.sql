
-- ============================================
-- SEAT RESERVATION SYSTEM
-- Database: MySQL 8.4+
-- Primary IDs: BIGINT AUTO_INCREMENT
-- Storage Engine: InnoDB
-- ============================================

-- ============================================
-- 1. USERS
-- ============================================

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_users_email (email),

    CONSTRAINT chk_users_role
        CHECK (role IN ('CUSTOMER', 'ADMIN'))

) ENGINE=InnoDB;


-- ============================================
-- 2. SHOWS
-- ============================================

CREATE TABLE shows (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    price_paise BIGINT NOT NULL,
    per_user_limit INT NOT NULL DEFAULT 4,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT chk_show_price
        CHECK (price_paise > 0),

    CONSTRAINT chk_show_user_limit
        CHECK (per_user_limit > 0)

) ENGINE=InnoDB;


-- ============================================
-- 3. RESERVATIONS
-- ============================================

CREATE TABLE reservations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    show_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount_paise BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',

    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT fk_reservation_show
        FOREIGN KEY (show_id)
        REFERENCES shows(id),

    CONSTRAINT fk_reservation_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT chk_reservation_amount
        CHECK (amount_paise > 0),

    CONSTRAINT chk_reservation_status
        CHECK (status IN ('CONFIRMED', 'CANCELLED')),

    INDEX idx_reservation_user_show_status
        (user_id, show_id, status),

    INDEX idx_reservation_show_status
        (show_id, status)

) ENGINE=InnoDB;


-- ============================================
-- 4. SEATS
-- ============================================

CREATE TABLE seats (
    id BIGINT NOT NULL AUTO_INCREMENT,
    show_id BIGINT NOT NULL,
    seat_number VARCHAR(20) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',

    current_reservation_id BIGINT NULL,

    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT fk_seat_show
        FOREIGN KEY (show_id)
        REFERENCES shows(id),

    CONSTRAINT chk_seat_status
        CHECK (status IN ('AVAILABLE', 'CONFIRMED')),

    UNIQUE KEY uk_seat_show_number
        (show_id, seat_number),

    INDEX idx_seat_show_status
        (show_id, status),

    INDEX idx_seat_current_reservation
        (current_reservation_id)

) ENGINE=InnoDB;


-- ============================================
-- 5. RESERVATION SEATS
-- ============================================

CREATE TABLE reservation_seats (
    id BIGINT NOT NULL AUTO_INCREMENT,

    reservation_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,

    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT fk_reservation_seat_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(id),

    CONSTRAINT fk_reservation_seat_seat
        FOREIGN KEY (seat_id)
        REFERENCES seats(id),

    UNIQUE KEY uk_reservation_seat
        (reservation_id, seat_id),

    INDEX idx_reservation_seat_seat
        (seat_id)

) ENGINE=InnoDB;


-- ============================================
-- 6. USER SHOW QUOTAS
-- ============================================

CREATE TABLE user_show_quotas (
    user_id BIGINT NOT NULL,
    show_id BIGINT NOT NULL,

    active_seats INT NOT NULL DEFAULT 0,

    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (user_id, show_id),

    CONSTRAINT fk_quota_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_quota_show
        FOREIGN KEY (show_id)
        REFERENCES shows(id),

    CONSTRAINT chk_quota_active_seats
        CHECK (active_seats >= 0)

) ENGINE=InnoDB;


-- ============================================
-- 7. IDEMPOTENCY RECORDS
-- ============================================

CREATE TABLE idempotency_records (
    id BIGINT NOT NULL AUTO_INCREMENT,

    user_id BIGINT NOT NULL,
    show_id BIGINT NOT NULL,

    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,

    reservation_id BIGINT NULL,

    response_status INT NULL,
    response_json JSON NULL,

    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT fk_idempotency_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_idempotency_show
        FOREIGN KEY (show_id)
        REFERENCES shows(id),

    CONSTRAINT fk_idempotency_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(id),

    UNIQUE KEY uk_idempotency_user_show_key
        (user_id, show_id, idempotency_key),

    INDEX idx_idempotency_reservation
        (reservation_id)

) ENGINE=InnoDB;


-- ============================================
-- 8. SEAT CURRENT RESERVATION FOREIGN KEY
-- Added after reservations and seats exist.
-- ============================================

ALTER TABLE seats
    ADD CONSTRAINT fk_seat_current_reservation
        FOREIGN KEY (current_reservation_id)
        REFERENCES reservations(id);