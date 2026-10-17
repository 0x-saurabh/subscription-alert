-- Subscription Reminder Manager - database setup
-- Safe to run more than once: it never deletes existing data.

CREATE DATABASE IF NOT EXISTS subscription_manager
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE subscription_manager;

CREATE TABLE IF NOT EXISTS subscriptions (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(100)   NOT NULL,
    amount            DECIMAL(10, 2) NOT NULL,
    billing_cycle     ENUM('MONTHLY', 'YEARLY')    NOT NULL,
    renewal_date      DATE           NOT NULL,
    email             VARCHAR(255)   NOT NULL,
    status            ENUM('ACTIVE', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    reminder_sent_for DATE           NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    INDEX idx_status_renewal (status, renewal_date)
);
