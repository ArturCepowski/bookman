package com.bookman.reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByApartmentId(long apartmentId);

    Optional<Reservation> findByIdAndApartmentId(long id, long apartmentId);

    /** REQ-07: current and future, not cancelled, by check-out date. */
    @Query("""
            select new com.bookman.reservation.ReservationView(r.id, r.checkIn, r.checkOut, r.cleaningStatus,
                case when r.cleaningStatus <> com.bookman.reservation.CleaningStatus.NOT_PLANNED then u.email end)
            from Reservation r left join com.bookman.user.User u on u.id = r.responsibleUserId
            where r.apartmentId = :apartmentId and r.cancelledAt is null and r.checkOut >= :today
            order by r.checkOut, r.checkIn, r.id
            """)
    List<ReservationView> findCurrent(@Param("apartmentId") long apartmentId, @Param("today") LocalDate today);

    @Query("""
            select new com.bookman.reservation.ReservationView(r.id, r.checkIn, r.checkOut, r.cleaningStatus,
                case when r.cleaningStatus <> com.bookman.reservation.CleaningStatus.NOT_PLANNED then u.email end)
            from Reservation r left join com.bookman.user.User u on u.id = r.responsibleUserId
            where r.id = :id
            """)
    ReservationView findView(@Param("id") long id);

    /** Q-13: one import per apartment at a time; false if another import holds the lock. */
    @Query(value = "select pg_try_advisory_xact_lock(:key)", nativeQuery = true)
    boolean tryLockForImport(@Param("key") long key);
}
