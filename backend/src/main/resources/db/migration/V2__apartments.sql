CREATE TABLE apartment (
    id          BIGSERIAL     PRIMARY KEY,
    name        VARCHAR(60)   NOT NULL CHECK (char_length(name) BETWEEN 1 AND 60),
    booking_url VARCHAR(2048) NOT NULL,
    -- NFR-03: secret, returned only to ADMIN users.
    ical_url    VARCHAR(2048) NOT NULL,
    time_zone   VARCHAR(64)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL
);

-- Q-09: roles live only here; one role per user per apartment.
CREATE TABLE apartment_member (
    apartment_id BIGINT      NOT NULL REFERENCES apartment (id) ON DELETE CASCADE,
    user_id      BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role         VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'CLEANING_SERVICE')),
    created_at   TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (apartment_id, user_id)
);

CREATE INDEX ix_apartment_member_user ON apartment_member (user_id);

-- Exactly one admin per apartment (spec 6: multiple admins are out of scope).
CREATE UNIQUE INDEX ux_apartment_member_single_admin ON apartment_member (apartment_id) WHERE role = 'ADMIN';
