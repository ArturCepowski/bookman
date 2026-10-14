package com.bookman.apartment;

import com.bookman.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ApartmentService service;

    public ApartmentController(ApartmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApartmentSummary> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.listForUser(user.userId());
    }

    @GetMapping("/{id}")
    public ApartmentDetails get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        return service.get(id, user.userId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApartmentDetails register(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ApartmentRequest request) {
        return service.register(user.userId(), request);
    }

    @PutMapping("/{id}")
    public ApartmentDetails update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
            @Valid @RequestBody ApartmentRequest request) {
        return service.update(id, user.userId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        service.delete(id, user.userId());
    }
}
