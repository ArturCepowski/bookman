CREATE TABLE app_user (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    phone_number  VARCHAR(16)  NOT NULL CHECK (phone_number ~ '^\+[1-9][0-9]{6,14}$'),
    language      VARCHAR(2)   NOT NULL CHECK (language IN ('EN', 'PL')),
    created_at    TIMESTAMPTZ  NOT NULL
);

-- REQ-01 / Q-09: e-mail is unique case-insensitively.
CREATE UNIQUE INDEX ux_app_user_email_lower ON app_user (lower(email));
