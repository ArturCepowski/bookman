package com.bookman.apartment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApartmentMemberRepository extends JpaRepository<ApartmentMember, ApartmentMemberId> {

    @Query("""
            select new com.bookman.apartment.ApartmentSummary(a.id, a.name, m.role)
            from ApartmentMember m, Apartment a
            where a.id = m.id.apartmentId and m.id.userId = :userId
            order by lower(a.name), a.id
            """)
    List<ApartmentSummary> findSummariesForUser(@Param("userId") long userId);

    /** REQ-10: members of one apartment with their e-mails, admin first. */
    @Query("""
            select new com.bookman.apartment.MemberView(u.id, u.email, m.role)
            from ApartmentMember m, com.bookman.user.User u
            where u.id = m.id.userId and m.id.apartmentId = :apartmentId
            order by case when m.role = com.bookman.apartment.Role.ADMIN then 0 else 1 end, u.email
            """)
    List<MemberView> findMembers(@Param("apartmentId") long apartmentId);

    /** REQ-11/12 recipients: every ADMIN and CLEANING_SERVICE user of the apartment. */
    @Query("""
            select u from ApartmentMember m, com.bookman.user.User u
            where u.id = m.id.userId and m.id.apartmentId = :apartmentId
            order by u.id
            """)
    List<com.bookman.user.User> findMemberUsers(@Param("apartmentId") long apartmentId);
}
