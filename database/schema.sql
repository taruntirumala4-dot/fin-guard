-- ============================================================
--  FinGuard – MySQL 5.5 Compatible Schema
--  Run this before seed.sql
--  Usage: source C:/path/to/database/schema.sql
--
--  MySQL 5.5 Notes:
--  - DATETIME cannot use DEFAULT CURRENT_TIMESTAMP (added in 5.6.5)
--  - Only ONE TIMESTAMP column per table can use DEFAULT CURRENT_TIMESTAMP
--  - Both issues fixed below using TIMESTAMP types appropriately
-- ============================================================

-- Relax sql_mode for MySQL 5.5 compatibility
SET sql_mode = 'NO_ENGINE_SUBSTITUTION';
SET NAMES utf8mb4;
SET character_set_client = utf8mb4;

-- ─────────────────────────────────────────────────────────────
-- TABLE: users
-- ─────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT                  PRIMARY KEY,
    name          VARCHAR(100)                           NOT NULL,
    -- VARCHAR(191): utf8mb4 uses 4 bytes/char, so 191*4=764 bytes < 767-byte InnoDB limit
    email         VARCHAR(191)                           NOT NULL UNIQUE,
    password_hash VARCHAR(255)                           NOT NULL,
    role          ENUM('USER','ADMIN','ANALYST')         NOT NULL DEFAULT 'USER',
    -- TIMESTAMP (not DATETIME): supports DEFAULT CURRENT_TIMESTAMP in MySQL 5.5
    created_at    TIMESTAMP                              NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────────────────────
-- TABLE: transactions
-- ─────────────────────────────────────────────────────────────
-- MySQL 5.5 restriction: only ONE TIMESTAMP column per table
-- can have DEFAULT CURRENT_TIMESTAMP.
-- 'timestamp' gets the auto-default; 'created_at' uses a fixed default.

CREATE TABLE IF NOT EXISTS transactions (
    id               BIGINT AUTO_INCREMENT                              PRIMARY KEY,
    transaction_id   VARCHAR(50)                                        NOT NULL UNIQUE,
    user_id          BIGINT                                             NOT NULL,
    amount           DECIMAL(15, 2)                                     NOT NULL,
    currency         VARCHAR(10)                                        NOT NULL DEFAULT 'INR',
    merchant         VARCHAR(255)                                       NOT NULL,
    location         VARCHAR(255)                                       NOT NULL,
    transaction_type ENUM('PAYMENT','TRANSFER','WITHDRAWAL','DEPOSIT')  NOT NULL,
    device_id        VARCHAR(100)                                       NOT NULL,
    -- First TIMESTAMP gets DEFAULT CURRENT_TIMESTAMP (MySQL 5.5 allows this on first one)
    timestamp        TIMESTAMP                                          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status           ENUM('SUCCESS','FAILED','PENDING','BLOCKED')       NOT NULL DEFAULT 'PENDING',
    fraud_score      DECIMAL(5, 2)                                      NOT NULL DEFAULT 0.00,
    fraud_status     ENUM('SAFE','SUSPICIOUS','FRAUD')                  NOT NULL DEFAULT 'SAFE',
    -- Second TIMESTAMP column: use a fixed default (MySQL 5.5 limitation)
    created_at       TIMESTAMP                                          NOT NULL DEFAULT '2000-01-01 00:00:00',
    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────────────────────
-- TABLE: fraud_alerts
-- ─────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS fraud_alerts (
    id             BIGINT AUTO_INCREMENT                                    PRIMARY KEY,
    transaction_id VARCHAR(50)                                              NOT NULL,
    alert_type     VARCHAR(100)                                             NOT NULL,
    severity       ENUM('LOW','MEDIUM','HIGH','CRITICAL')                   NOT NULL,
    reason         TEXT                                                     NOT NULL,
    fraud_score    DECIMAL(5, 2)                                            NOT NULL DEFAULT 0.00,
    status         ENUM('OPEN','INVESTIGATING','RESOLVED','FALSE_POSITIVE') NOT NULL DEFAULT 'OPEN',
    -- Single TIMESTAMP column per table: DEFAULT CURRENT_TIMESTAMP is fine
    created_at     TIMESTAMP                                                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alerts_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────────────────────
-- INDEXES
-- Note: FULLTEXT on InnoDB is NOT supported in MySQL 5.5
--       (added in MySQL 5.6) – using regular indexes instead
-- ─────────────────────────────────────────────────────────────

-- users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role  ON users(role);

-- transactions
CREATE INDEX idx_transactions_transaction_id ON transactions(transaction_id);
CREATE INDEX idx_transactions_user_id        ON transactions(user_id);
CREATE INDEX idx_transactions_timestamp      ON transactions(timestamp);
CREATE INDEX idx_transactions_fraud_status   ON transactions(fraud_status);
CREATE INDEX idx_transactions_fraud_score    ON transactions(fraud_score);
CREATE INDEX idx_transactions_status         ON transactions(status);
CREATE INDEX idx_transactions_merchant       ON transactions(merchant(191));
CREATE INDEX idx_transactions_location       ON transactions(location(191));
-- Composite index for user+time range queries
CREATE INDEX idx_transactions_user_timestamp ON transactions(user_id, timestamp);

-- fraud_alerts
CREATE INDEX idx_fraud_alerts_transaction_id ON fraud_alerts(transaction_id);
CREATE INDEX idx_fraud_alerts_severity       ON fraud_alerts(severity);
CREATE INDEX idx_fraud_alerts_status         ON fraud_alerts(status);
CREATE INDEX idx_fraud_alerts_created_at     ON fraud_alerts(created_at);
CREATE INDEX idx_fraud_alerts_fraud_score    ON fraud_alerts(fraud_score);
