package com.bookman.member;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    List<Invitation> findByApartmentIdAndStatusOrderByCreatedAtAsc(long apartmentId, InvitationStatus status);

    Optional<Invitation> findByApartmentIdAndEmailAndStatus(long apartmentId, String email, InvitationStatus status);

    Optional<Invitation> findByIdAndApartmentId(long id, long apartmentId);

    /** REQ-09: pending invitations addressed to this e-mail, with apartment name and inviter. */
    @Query("""
            select new com.bookman.member.MyInvitationView(i.id, i.apartmentId, a.name, u.email, i.expiresAt)
            from Invitation i
            join com.bookman.apartment.Apartment a on a.id = i.apartmentId
            left join com.bookman.user.User u on u.id = i.invitedBy
            where i.email = :email and i.status = com.bookman.member.InvitationStatus.PENDING
              and i.expiresAt > :now
            order by i.createdAt
            """)
    List<MyInvitationView> findOpenForEmail(@Param("email") String email, @Param("now") Instant now);
}
