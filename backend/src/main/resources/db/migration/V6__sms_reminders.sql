-- REQ-11 / REQ-12: reminder hours per apartment, in the apartment's time zone; NULL = disabled.
ALTER TABLE apartment ADD COLUMN not_planned_reminder_time TIME;
ALTER TABLE apartment ADD COLUMN not_done_reminder_time TIME;

-- REQ-11/12/13, Q-11: one row per (reservation, recipient, reminder type, day). The unique key is what prevents
-- duplicate SMS on retries, restarts or several backend instances.
CREATE TABLE sms_notification (
    id                BIGSERIAL    PRIMARY KEY,
    reservation_id    BIGINT       NOT NULL REFERENCES reservation (id) ON DELETE CASCADE,
    recipient_user_id BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    reminder_type     VARCHAR(20)  NOT NULL CHECK (reminder_type IN ('NOT_PLANNED', 'NOT_DONE')),
    reminder_date     DATE         NOT NULL,
    phone_number      VARCHAR(16)  NOT NULL,
    message           VARCHAR(500) NOT NULL,
    status            VARCHAR(10)  NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts          INT          NOT NULL DEFAULT 0,
    next_attempt_at   TIMESTAMPTZ  NOT NULL,
    last_error        VARCHAR(500),
    created_at        TIMESTAMPTZ  NOT NULL,
    sent_at           TIMESTAMPTZ,
    CONSTRAINT ux_sms_notification_once UNIQUE (reservation_id, recipient_user_id, reminder_type, reminder_date)
);

CREATE INDEX ix_sms_notification_due ON sms_notification (next_attempt_at) WHERE status = 'PENDING';
