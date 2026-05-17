-- =====================================================
-- Migration: V001 - Create INF_NOTI schema and core tables
-- Purpose: Initialize notification service database structure
-- Author: Infinite World Team
-- Date: 2026-04-27
-- =====================================================

-- Create schema
CREATE SCHEMA IF NOT EXISTS INF_NOTI;

-- =====================================================
-- Table: notification_request
-- Purpose: Audit all incoming notification requests from other services
-- Idempotency: event_id and idempotency_key are unique
-- =====================================================
CREATE TABLE INF_NOTI.notification_request (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    request_id VARCHAR(100),
    trace_id VARCHAR(100),
    source_service VARCHAR(100) NOT NULL,
    source_module VARCHAR(100),
    source_action VARCHAR(100),
    schema_version VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_request_event UNIQUE (event_id),
    CONSTRAINT uk_notification_request_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_notification_request_status ON INF_NOTI.notification_request(status, created_at DESC);
CREATE INDEX idx_notification_request_source ON INF_NOTI.notification_request(source_service, source_action);

COMMENT ON TABLE INF_NOTI.notification_request IS 'Stores all incoming notification requests for audit and idempotency';
COMMENT ON COLUMN INF_NOTI.notification_request.idempotency_key IS 'Prevents duplicate notification creation on retry';

-- =====================================================
-- Table: notification_template
-- Purpose: Normalized notification content ready for delivery
-- =====================================================
CREATE TABLE INF_NOTI.notification_template (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    code VARCHAR(100),
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    priority SMALLINT NOT NULL DEFAULT 0,
    image_url VARCHAR(1000),
    action_payload JSONB,
    reward_payload JSONB,
    channel_payload JSONB NOT NULL,
    start_at TIMESTAMP NULL,
    expire_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_template_request
        FOREIGN KEY (request_id)
        REFERENCES INF_NOTI.notification_request(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_notification_template_request ON INF_NOTI.notification_template(request_id);
CREATE INDEX idx_notification_template_status ON INF_NOTI.notification_template(status, created_at DESC);
CREATE INDEX idx_notification_template_code ON INF_NOTI.notification_template(code);

COMMENT ON TABLE INF_NOTI.notification_template IS 'Normalized notification content ready for fanout';

-- =====================================================
-- Table: notification_target_rule
-- Purpose: Defines who should receive the notification
-- =====================================================
CREATE TABLE INF_NOTI.notification_target_rule (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    rule_type VARCHAR(30) NOT NULL,
    rule_payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_target_rule_template
        FOREIGN KEY (notification_id)
        REFERENCES INF_NOTI.notification_template(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_target_rule_notification ON INF_NOTI.notification_target_rule(notification_id);

COMMENT ON TABLE INF_NOTI.notification_target_rule IS 'Defines target audience for notifications';

-- =====================================================
-- Table: notification_delivery_job
-- Purpose: Tracks fanout job progress for large-scale delivery
-- =====================================================
CREATE TABLE INF_NOTI.notification_delivery_job (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    job_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_target BIGINT NOT NULL DEFAULT 0,
    processed_target BIGINT NOT NULL DEFAULT 0,
    success_target BIGINT NOT NULL DEFAULT 0,
    failed_target BIGINT NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    scheduled_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_job_template
        FOREIGN KEY (notification_id)
        REFERENCES INF_NOTI.notification_template(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_delivery_job_notification ON INF_NOTI.notification_delivery_job(notification_id);
CREATE INDEX idx_delivery_job_status ON INF_NOTI.notification_delivery_job(status, scheduled_at);

COMMENT ON TABLE INF_NOTI.notification_delivery_job IS 'Tracks delivery job progress for scalable fanout';

-- =====================================================
-- Table: notification_delivery_batch
-- Purpose: Breaks large fanout into manageable batches
-- =====================================================
CREATE TABLE INF_NOTI.notification_delivery_batch (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    batch_no INT NOT NULL,
    cursor_value BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expected_count INT NOT NULL DEFAULT 0,
    processed_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_batch_job
        FOREIGN KEY (job_id)
        REFERENCES INF_NOTI.notification_delivery_job(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_delivery_batch UNIQUE (job_id, batch_no)
);

CREATE INDEX idx_delivery_batch_job ON INF_NOTI.notification_delivery_batch(job_id);
CREATE INDEX idx_delivery_batch_status ON INF_NOTI.notification_delivery_batch(status, created_at);

COMMENT ON TABLE INF_NOTI.notification_delivery_batch IS 'Manages batch processing for large-scale fanout';

-- =====================================================
-- Table: user_notification
-- Purpose: User inbox - actual notifications delivered to users
-- =====================================================
CREATE TABLE INF_NOTI.user_notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    priority SMALLINT NOT NULL DEFAULT 0,
    image_url VARCHAR(1000),
    action_payload JSONB,
    reward_payload JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    is_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    claimed_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    expire_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_notification UNIQUE (user_id, notification_id)
);

CREATE INDEX idx_un_user_created ON INF_NOTI.user_notification(user_id, created_at DESC);
CREATE INDEX idx_un_user_status ON INF_NOTI.user_notification(user_id, status, is_deleted);
CREATE INDEX idx_un_notification ON INF_NOTI.user_notification(notification_id);
CREATE INDEX idx_un_expire ON INF_NOTI.user_notification(expire_at) WHERE expire_at IS NOT NULL;

COMMENT ON TABLE INF_NOTI.user_notification IS 'User inbox - stores notifications delivered to each user';
COMMENT ON COLUMN INF_NOTI.user_notification.status IS 'UNREAD, READ';
COMMENT ON INDEX INF_NOTI.idx_un_user_created IS 'Optimizes inbox query by user';
COMMENT ON INDEX INF_NOTI.idx_un_user_status IS 'Optimizes unread count query';

-- =====================================================
-- Table: user_notification_claim_log
-- Purpose: Audit trail for reward claims
-- =====================================================
CREATE TABLE INF_NOTI.user_notification_claim_log (
    id BIGSERIAL PRIMARY KEY,
    user_notification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reward_payload JSONB NOT NULL,
    claimed_result VARCHAR(20) NOT NULL,
    reference_code VARCHAR(100),
    claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_claim_user_notification
        FOREIGN KEY (user_notification_id)
        REFERENCES INF_NOTI.user_notification(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_claim_log_user_notification ON INF_NOTI.user_notification_claim_log(user_notification_id);
CREATE INDEX idx_claim_log_user ON INF_NOTI.user_notification_claim_log(user_id, claimed_at DESC);

COMMENT ON TABLE INF_NOTI.user_notification_claim_log IS 'Audit log for reward claims to prevent duplicate claims';

-- =====================================================
-- Table: email_delivery_log
-- Purpose: Tracks email delivery status and enables retry
-- =====================================================
CREATE TABLE INF_NOTI.email_delivery_log (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    notification_id BIGINT NULL,
    user_id BIGINT NULL,
    source_service VARCHAR(100),
    source_action VARCHAR(100),
    to_email VARCHAR(254) NOT NULL,
    email_type VARCHAR(50),
    template_code VARCHAR(100),
    subject VARCHAR(255),
    payload JSONB,
    provider VARCHAR(50),
    provider_message_id VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_email_delivery_event UNIQUE (event_id)
);

CREATE INDEX idx_email_delivery_status ON INF_NOTI.email_delivery_log(status, requested_at);
CREATE INDEX idx_email_delivery_notification ON INF_NOTI.email_delivery_log(notification_id) WHERE notification_id IS NOT NULL;
CREATE INDEX idx_email_delivery_user ON INF_NOTI.email_delivery_log(user_id, requested_at DESC) WHERE user_id IS NOT NULL;

COMMENT ON TABLE INF_NOTI.email_delivery_log IS 'Tracks email delivery for audit and retry';
COMMENT ON COLUMN INF_NOTI.email_delivery_log.status IS 'PENDING, SENT, DELIVERED, FAILED';

-- =====================================================
-- Retention Policy Documentation
-- =====================================================
-- user_notification: 180 days (6 months)
-- email_delivery_log: 90 days (3 months)
-- notification_request: 365 days (1 year) for audit
-- notification_delivery_*: 90 days (3 months)
-- Cleanup jobs will be implemented in later phases
-- =====================================================
