package com.bookman.apartment;

import com.bookman.common.ApiException;
import org.springframework.stereotype.Component;

/**
 * NFR-01 / Q-09: every apartment-scoped operation resolves the caller's membership row.
 * No row (including a non-existent apartment) means 403, so apartment ids are not enumerable.
 */
@Component
public class ApartmentAccess {

    private final ApartmentMemberRepository members;

    public ApartmentAccess(ApartmentMemberRepository members) {
        this.members = members;
    }

    public Role requireMember(long apartmentId, long userId) {
        return members.findById(new ApartmentMemberId(apartmentId, userId))
                .map(ApartmentMember::getRole)
                .orElseThrow(ApiException::forbidden);
    }

    public void requireAdmin(long apartmentId, long userId) {
        if (requireMember(apartmentId, userId) != Role.ADMIN) {
            throw ApiException.forbidden();
        }
    }
}
