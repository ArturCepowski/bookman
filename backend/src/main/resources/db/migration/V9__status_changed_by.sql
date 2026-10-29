-- Q-28 (owner decision): the cleaning service may undo a status it set itself, so the reservation remembers who
-- set its current cleaning status. Existing rows: the author of the latest recorded change (NULL if none).
ALTER TABLE reservation
    ADD COLUMN status_changed_by_user_id BIGINT REFERENCES app_user (id) ON DELETE SET NULL;

UPDATE reservation r
SET status_changed_by_user_id = (
    SELECT c.changed_by FROM reservation_status_change c
    WHERE c.reservation_id = r.id
    ORDER BY c.id DESC
    LIMIT 1);
