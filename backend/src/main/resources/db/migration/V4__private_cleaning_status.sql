-- Q-20 (owner decision): PRIVATE marks dates the owner blocked by hand; only an ADMIN may set it.
ALTER TABLE reservation DROP CONSTRAINT reservation_cleaning_status_check;
ALTER TABLE reservation ADD CONSTRAINT reservation_cleaning_status_check
    CHECK (cleaning_status IN ('NOT_PLANNED', 'TAKEN', 'DONE', 'PRIVATE'));
