package com.bookman.member;

import com.bookman.apartment.ApartmentSummary;
import com.bookman.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MemberController {

    private final MemberService service;

    public MemberController(MemberService service) {
        this.service = service;
    }

    @GetMapping("/api/apartments/{apartmentId}/members")
    public MembersOverview overview(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long apartmentId) {
        return service.overview(apartmentId, user.userId());
    }

    @PostMapping("/api/apartments/{apartmentId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public PendingInvitationView invite(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long apartmentId, @Valid @RequestBody InviteRequest request) {
        return service.invite(apartmentId, user.userId(), request.email());
    }

    @DeleteMapping("/api/apartments/{apartmentId}/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelInvitation(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long apartmentId,
            @PathVariable long invitationId) {
        service.cancelInvitation(apartmentId, user.userId(), invitationId);
    }

    @DeleteMapping("/api/apartments/{apartmentId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long apartmentId,
            @PathVariable long userId) {
        service.removeMember(apartmentId, user.userId(), userId);
    }

    @GetMapping("/api/invitations")
    public List<MyInvitationView> myInvitations(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.myInvitations(user.userId());
    }

    @PostMapping("/api/invitations/{invitationId}/accept")
    public ApartmentSummary accept(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long invitationId) {
        return service.accept(user.userId(), invitationId);
    }

    @PostMapping("/api/invitations/{invitationId}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long invitationId) {
        service.decline(user.userId(), invitationId);
    }
}
