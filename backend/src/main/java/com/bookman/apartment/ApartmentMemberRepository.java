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
}
