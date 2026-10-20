package com.bookman.reservation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationStatusChangeRepository extends JpaRepository<ReservationStatusChange, Long> {

    List<ReservationStatusChange> findByReservationIdOrderByIdAsc(long reservationId);
}
