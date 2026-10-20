package com.bookman.apartment;

import com.bookman.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/apartments/{apartmentId}/notification-settings")
public class NotificationSettingsController {

    private final NotificationSettingsService service;

    public NotificationSettingsController(NotificationSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public NotificationSettings get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long apartmentId) {
        return service.get(apartmentId, user.userId());
    }

    @PutMapping
    public NotificationSettings update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long apartmentId,
            @Valid @RequestBody NotificationSettings settings) {
        return service.update(apartmentId, user.userId(), settings);
    }
}
