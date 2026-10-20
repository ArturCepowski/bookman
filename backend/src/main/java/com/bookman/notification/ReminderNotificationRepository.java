package com.bookman.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderNotificationRepository extends JpaRepository<ReminderNotification, Long> {

    /** Q-11: inserts nothing if this reminder was already planned (unique key) – returns 0 then. */
    @Modifying
    @Query(value = """
            INSERT INTO reminder_notification (reservation_id, recipient_user_id, reminder_type, reminder_date,
                                               title, body, status, attempts, next_attempt_at, created_at)
            VALUES (:reservationId, :recipientId, :type, :reminderDate, :title, :body, 'PENDING', 0, :now, :now)
            ON CONFLICT ON CONSTRAINT ux_reminder_notification_once DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("reservationId") long reservationId, @Param("recipientId") long recipientId,
            @Param("type") String type, @Param("reminderDate") LocalDate reminderDate, @Param("title") String title,
            @Param("body") String body, @Param("now") Instant now);

    /** Next due reminder, locked so that parallel dispatchers never send the same row. */
    @Query(value = """
            SELECT * FROM reminder_notification
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY next_attempt_at, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<ReminderNotification> lockNextDue(@Param("now") Instant now);
}
