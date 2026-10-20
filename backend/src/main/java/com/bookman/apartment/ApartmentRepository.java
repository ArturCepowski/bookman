package com.bookman.apartment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    @Query("select a.id from Apartment a order by a.id")
    List<Long> findAllIds();
}
