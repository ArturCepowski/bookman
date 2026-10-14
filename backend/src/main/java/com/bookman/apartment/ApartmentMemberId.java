package com.bookman.apartment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record ApartmentMemberId(
        @Column(name = "apartment_id") Long apartmentId,
        @Column(name = "user_id") Long userId) implements Serializable {
}
