package com.bookman.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmsNotificationRepository extends JpaRepository<SmsNotification, Long> {

    /** Q-11: inserts nothing if this reminder was already planned (unique key) – returns 0 then. */
    @Modifying
    @Query(value = """
            INSERT INTO sms_notification (reservation_id, recipient_user_id, reminder_type, reminder_date,
                                          phone_number, message, status, attempts, next_attempt_at, created_at)
            VALUES (:reservationId, :recipientId, :type, :reminderDate, :phone, :message, 'PENDING', 0, :now, :now)
            ON CONFLICT ON CONSTRAINT ux_sms_notification_once DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("reservationId") long reservationId, @Param("recipientId") long recipientId,
            @Param("type") String type, @Param("reminderDate") LocalDate reminderDate, @Param("phone") String phone,
            @Param("message") String message, @Param("now") Instant now);

    /** Next due SMS, locked so that parallel dispatchers never send the same row. */
    @Query(value = """
            SELECT * FROM sms_notification
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY next_attempt_at, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<SmsNotification> lockNextDue(@Param("now") Instant now);
}
