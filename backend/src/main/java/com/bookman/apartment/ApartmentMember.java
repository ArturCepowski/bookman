package com.bookman.apartment;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "apartment_member")
public class ApartmentMember {

    @EmbeddedId
    private ApartmentMemberId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ApartmentMember() {
    }

    public ApartmentMember(long apartmentId, long userId, Role role, Instant createdAt) {
        this.id = new ApartmentMemberId(apartmentId, userId);
        this.role = role;
        this.createdAt = createdAt;
    }

    public ApartmentMemberId getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }
}
