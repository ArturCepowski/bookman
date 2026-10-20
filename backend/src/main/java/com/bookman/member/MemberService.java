package com.bookman.member;

import com.bookman.apartment.ApartmentAccess;
import com.bookman.apartment.ApartmentMember;
import com.bookman.apartment.ApartmentMemberId;
import com.bookman.apartment.ApartmentMemberRepository;
import com.bookman.apartment.ApartmentSummary;
import com.bookman.apartment.ApartmentRepository;
import com.bookman.apartment.Role;
import com.bookman.common.ApiException;
import com.bookman.common.ErrorCode;
import com.bookman.user.User;
import com.bookman.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** REQ-09 invitations and REQ-10 member management. Admin-only operations check the role on the backend (NFR-01). */
@Service
public class MemberService {

    private final ApartmentAccess access;
    private final ApartmentRepository apartments;
    private final ApartmentMemberRepository members;
    private final InvitationRepository invitations;
    private final UserRepository users;
    private final Clock clock;

    public MemberService(ApartmentAccess access, ApartmentRepository apartments, ApartmentMemberRepository members,
            InvitationRepository invitations, UserRepository users, Clock clock) {
        this.access = access;
        this.apartments = apartments;
        this.members = members;
        this.invitations = invitations;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MembersOverview overview(long apartmentId, long adminId) {
        access.requireAdmin(apartmentId, adminId);
        Instant now = clock.instant();
        List<PendingInvitationView> pending = invitations
                .findByApartmentIdAndStatusOrderByCreatedAtAsc(apartmentId, InvitationStatus.PENDING).stream()
                .filter(i -> i.isOpen(now))
                .map(PendingInvitationView::of)
                .toList();
        return new MembersOverview(members.findMembers(apartmentId), pending);
    }

    /** REQ-09: in-app invitation only, no e-mail is sent (Q-07). */
    @Transactional
    public PendingInvitationView invite(long apartmentId, long adminId, String rawEmail) {
        access.requireAdmin(apartmentId, adminId);
        String email = User.normalizeEmail(rawEmail);
        Instant now = clock.instant();

        boolean alreadyMember = users.findByEmail(email)
                .map(u -> members.existsById(new ApartmentMemberId(apartmentId, u.getId())))
                .orElse(false);
        if (alreadyMember) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.ALREADY_MEMBER, "This person already has a role here");
        }
        var existing = invitations.findByApartmentIdAndEmailAndStatus(apartmentId, email, InvitationStatus.PENDING);
        if (existing.isPresent()) {
            if (existing.get().isOpen(now)) {
                throw new ApiException(HttpStatus.CONFLICT, ErrorCode.INVITATION_PENDING, "Already invited");
            }
            // Frees the "one pending invitation" slot for a new invitation.
            existing.get().close(InvitationStatus.EXPIRED, now);
            invitations.flush();
        }
        try {
            return PendingInvitationView.of(invitations.saveAndFlush(new Invitation(apartmentId, email, adminId, now)));
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.INVITATION_PENDING, "Already invited");
        }
    }

    /** REQ-10 */
    @Transactional
    public void cancelInvitation(long apartmentId, long adminId, long invitationId) {
        access.requireAdmin(apartmentId, adminId);
        Invitation invitation = invitations.findByIdAndApartmentId(invitationId, apartmentId)
                .filter(i -> i.getStatus() == InvitationStatus.PENDING)
                .orElseThrow(MemberService::notFound);
        invitation.close(InvitationStatus.CANCELLED, clock.instant());
    }

    /** REQ-10: removes a CLEANING_SERVICE member; the admin cannot be removed (spec 6: single admin). */
    @Transactional
    public void removeMember(long apartmentId, long adminId, long userId) {
        access.requireAdmin(apartmentId, adminId);
        ApartmentMember member = members.findById(new ApartmentMemberId(apartmentId, userId))
                .orElseThrow(MemberService::notFound);
        if (member.getRole() != Role.CLEANING_SERVICE) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.CANNOT_REMOVE_ADMIN, "The admin cannot be removed");
        }
        members.delete(member);
    }

    /** REQ-09: visible after login or registration with the invited e-mail. */
    @Transactional(readOnly = true)
    public List<MyInvitationView> myInvitations(long userId) {
        return invitations.findOpenForEmail(emailOf(userId), clock.instant());
    }

    /** REQ-09: accepting = invitation closed + CLEANING_SERVICE role, in one transaction (Q-09). */
    @Transactional
    public ApartmentSummary accept(long userId, long invitationId) {
        Invitation invitation = openInvitationFor(userId, invitationId);
        Instant now = clock.instant();
        if (members.existsById(new ApartmentMemberId(invitation.getApartmentId(), userId))) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.ALREADY_MEMBER, "You already have a role here");
        }
        members.save(new ApartmentMember(invitation.getApartmentId(), userId, invitation.getRole(), now));
        invitation.close(InvitationStatus.ACCEPTED, now);
        return new ApartmentSummary(invitation.getApartmentId(),
                apartments.getReferenceById(invitation.getApartmentId()).getName(), invitation.getRole());
    }

    /** REQ-09 */
    @Transactional
    public void decline(long userId, long invitationId) {
        openInvitationFor(userId, invitationId).close(InvitationStatus.DECLINED, clock.instant());
    }

    private Invitation openInvitationFor(long userId, long invitationId) {
        Invitation invitation = invitations.findById(invitationId)
                .filter(i -> i.getEmail().equals(emailOf(userId)))
                .filter(i -> i.getStatus() == InvitationStatus.PENDING)
                .orElseThrow(MemberService::notFound);
        if (invitation.isExpired(clock.instant())) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.INVITATION_EXPIRED, "The invitation has expired");
        }
        return invitation;
    }

    private String emailOf(long userId) {
        return users.findById(userId)
                .map(User::getEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Unknown user"));
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Not found");
    }
}
