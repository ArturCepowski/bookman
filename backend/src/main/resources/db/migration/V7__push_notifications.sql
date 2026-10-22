-- Owner decision (Q-24): reminders go to the phone app as push notifications (Firebase Cloud Messaging), not SMS.
DROP TABLE sms_notification;

-- FCM registration tokens of the user's phones. A token belongs to one user at a time (last login wins).
CREATE TABLE device_token (
    id           BIGSERIAL     PRIMARY KEY,
    user_id      BIGINT        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token        VARCHAR(4096) NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL,
    last_seen_at TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ux_device_token UNIQUE (token)
);

CREATE INDEX ix_device_token_user ON device_token (user_id);

-- REQ-11/12, Q-11: one reminder per (reservation, recipient, type, day); the unique key prevents duplicates on
-- retries, restarts or several backend instances. It is sent to all of the recipient's devices.
CREATE TABLE reminder_notification (
    id                BIGSERIAL    PRIMARY KEY,
    reservation_id    BIGINT       NOT NULL REFERENCES reservation (id) ON DELETE CASCADE,
    recipient_user_id BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    reminder_type     VARCHAR(20)  NOT NULL CHECK (reminder_type IN ('NOT_PLANNED', 'NOT_DONE')),
    reminder_date     DATE         NOT NULL,
    title             VARCHAR(200) NOT NULL,
    body              VARCHAR(500) NOT NULL,
    status            VARCHAR(10)  NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'NO_DEVICE')),
    attempts          INT          NOT NULL DEFAULT 0,
    next_attempt_at   TIMESTAMPTZ  NOT NULL,
    last_error        VARCHAR(500),
    created_at        TIMESTAMPTZ  NOT NULL,
    sent_at           TIMESTAMPTZ,
    CONSTRAINT ux_reminder_notification_once UNIQUE (reservation_id, recipient_user_id, reminder_type, reminder_date)
);

CREATE INDEX ix_reminder_notification_due ON reminder_notification (next_attempt_at) WHERE status = 'PENDING';
