-- REQ-09 / REQ-10 / Q-09
CREATE TABLE invitation (
    id           BIGSERIAL    PRIMARY KEY,
    apartment_id BIGINT       NOT NULL REFERENCES apartment (id) ON DELETE CASCADE,
    -- Stored normalized (trimmed, lower-case) like app_user.email (Q-14).
    email        VARCHAR(254) NOT NULL,
    role         VARCHAR(20)  NOT NULL CHECK (role IN ('CLEANING_SERVICE')),
    status       VARCHAR(20)  NOT NULL
                 CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED', 'EXPIRED')),
    invited_by   BIGINT       REFERENCES app_user (id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    responded_at TIMESTAMPTZ
);

-- At most one pending invitation per apartment and e-mail.
CREATE UNIQUE INDEX ux_invitation_pending ON invitation (apartment_id, lower(email)) WHERE status = 'PENDING';
-- "My invitations" lookup after login/registration.
CREATE INDEX ix_invitation_pending_email ON invitation (lower(email)) WHERE status = 'PENDING';
