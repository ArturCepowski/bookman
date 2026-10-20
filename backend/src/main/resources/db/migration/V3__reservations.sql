-- REQ-06 / REQ-08
CREATE TABLE reservation (
    id                  BIGSERIAL    PRIMARY KEY,
    apartment_id        BIGINT       NOT NULL REFERENCES apartment (id) ON DELETE CASCADE,
    -- REQ-06: reservation identity within an apartment = iCal event UID.
    external_id         VARCHAR(255) NOT NULL,
    check_in            DATE         NOT NULL,
    check_out           DATE         NOT NULL,
    cleaning_status     VARCHAR(20)  NOT NULL DEFAULT 'NOT_PLANNED'
                        CHECK (cleaning_status IN ('NOT_PLANNED', 'TAKEN', 'DONE')),
    -- REQ-08: the user who set TAKEN.
    responsible_user_id BIGINT       REFERENCES app_user (id) ON DELETE SET NULL,
    -- REQ-06 / Q-04: set when the reservation disappeared from the feed before it ended.
    cancelled_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ux_reservation_external UNIQUE (apartment_id, external_id),
    CONSTRAINT ck_reservation_dates CHECK (check_out >= check_in)
);

CREATE INDEX ix_reservation_apartment_check_out ON reservation (apartment_id, check_out);

-- REQ-08: every cleaning status change records who changed it and when.
CREATE TABLE reservation_status_change (
    id             BIGSERIAL   PRIMARY KEY,
    reservation_id BIGINT      NOT NULL REFERENCES reservation (id) ON DELETE CASCADE,
    from_status    VARCHAR(20) NOT NULL,
    to_status      VARCHAR(20) NOT NULL,
    changed_by     BIGINT      REFERENCES app_user (id) ON DELETE SET NULL,
    changed_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_reservation_status_change_reservation ON reservation_status_change (reservation_id);
