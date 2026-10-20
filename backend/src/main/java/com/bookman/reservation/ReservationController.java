package com.bookman.reservation;

import com.bookman.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/apartments/{apartmentId}/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReservationView> list(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long apartmentId) {
        return service.listCurrent(apartmentId, user.userId());
    }

    @PostMapping("/import")
    public ImportResult importNow(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long apartmentId) {
        return service.importNow(apartmentId, user.userId());
    }

    @PutMapping("/{reservationId}/cleaning-status")
    public ReservationView changeCleaningStatus(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long apartmentId, @PathVariable long reservationId,
            @Valid @RequestBody CleaningStatusRequest request) {
        return service.changeCleaningStatus(apartmentId, reservationId, user.userId(), request.status());
    }
}
