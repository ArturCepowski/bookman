package com.bookman.apartment;

import java.time.Clock;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApartmentService {

    private final ApartmentRepository apartments;
    private final ApartmentMemberRepository members;
    private final ApartmentAccess access;
    private final ApartmentValidator validator;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ApartmentService(ApartmentRepository apartments, ApartmentMemberRepository members, ApartmentAccess access,
            ApartmentValidator validator, ApplicationEventPublisher events, Clock clock) {
        this.apartments = apartments;
        this.members = members;
        this.access = access;
        this.validator = validator;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ApartmentSummary> listForUser(long userId) {
        return members.findSummariesForUser(userId);
    }

    @Transactional(readOnly = true)
    public ApartmentDetails get(long apartmentId, long userId) {
        Role role = access.requireMember(apartmentId, userId);
        return ApartmentDetails.of(apartments.getReferenceById(apartmentId), role);
    }

    /** REQ-04: any user may register an apartment and becomes its ADMIN. */
    @Transactional
    public ApartmentDetails register(long userId, ApartmentRequest request) {
        ApartmentData data = validator.validate(request, null);
        Apartment apartment = apartments.save(new Apartment(data, clock.instant()));
        members.save(new ApartmentMember(apartment.getId(), userId, Role.ADMIN, clock.instant()));
        events.publishEvent(new ApartmentRegisteredEvent(apartment.getId()));
        return ApartmentDetails.of(apartment, Role.ADMIN);
    }

    /** REQ-05: admin only. */
    @Transactional
    public ApartmentDetails update(long apartmentId, long userId, ApartmentRequest request) {
        access.requireAdmin(apartmentId, userId);
        Apartment apartment = apartments.getReferenceById(apartmentId);
        apartment.apply(validator.validate(request, apartment.getIcalUrl()));
        return ApartmentDetails.of(apartment, Role.ADMIN);
    }

    /** REQ-05: admin only; members (and later reservations, invitations) go via ON DELETE CASCADE. */
    @Transactional
    public void delete(long apartmentId, long userId) {
        access.requireAdmin(apartmentId, userId);
        apartments.deleteById(apartmentId);
    }
}
