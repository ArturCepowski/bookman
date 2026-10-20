package com.bookman.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookman.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MembersApiTest extends IntegrationTest {

    private static final String ICAL = "https://ical.booking.com/v1/export?t=members";

    @Test
    void REQ10_adminSeesMembersAndPendingInvitations() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        long apartment = createApartment(admin, ICAL);
        addCleaningService(apartment, cleaner);
        call(admin, post(invitationsUrl(apartment)), Map.of("email", "invited@example.com"));

        call(admin, get(membersUrl(apartment)), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.members[0].email").value("admin@example.com"))
                .andExpect(jsonPath("$.members[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.members[1].email").value("cleaner@example.com"))
                .andExpect(jsonPath("$.members[1].role").value("CLEANING_SERVICE"))
                .andExpect(jsonPath("$.invitations.length()").value(1))
                .andExpect(jsonPath("$.invitations[0].email").value("invited@example.com"));
    }

    @Test
    void REQ10_adminCanCancelInvitation() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        TestUser invited = registerUser("invited@example.com");
        long apartment = createApartment(admin, ICAL);
        long invitation = bodyOf(call(admin, post(invitationsUrl(apartment)), Map.of("email", "invited@example.com")))
                .get("id").asLong();

        call(admin, delete(invitationsUrl(apartment) + "/" + invitation), null).andExpect(status().isNoContent());

        call(admin, get(membersUrl(apartment)), null).andExpect(jsonPath("$.invitations.length()").value(0));
        call(invited, get("/api/invitations"), null).andExpect(jsonPath("$.length()").value(0));
        call(invited, post("/api/invitations/" + invitation + "/accept"), null).andExpect(status().isNotFound());
    }

    @Test
    void REQ10_adminCanRemoveCleaningServiceButNotTheAdmin() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        long apartment = createApartment(admin, ICAL);
        addCleaningService(apartment, cleaner);

        call(admin, delete(membersUrl(apartment) + "/" + cleaner.id()), null).andExpect(status().isNoContent());

        call(cleaner, get("/api/apartments"), null).andExpect(jsonPath("$.length()").value(0));
        call(cleaner, get("/api/apartments/" + apartment + "/reservations"), null).andExpect(status().isForbidden());
        call(admin, delete(membersUrl(apartment) + "/" + admin.id()), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANNOT_REMOVE_ADMIN"));
    }

    @Test
    void REQ10_onlyAdminManagesMembers() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        TestUser colleague = registerUser("colleague@example.com");
        long apartment = createApartment(admin, ICAL);
        addCleaningService(apartment, cleaner);
        addCleaningService(apartment, colleague);
        long invitation = bodyOf(call(admin, post(invitationsUrl(apartment)), Map.of("email", "x@example.com")))
                .get("id").asLong();

        call(cleaner, get(membersUrl(apartment)), null).andExpect(status().isForbidden());
        call(cleaner, delete(membersUrl(apartment) + "/" + colleague.id()), null).andExpect(status().isForbidden());
        call(cleaner, delete(invitationsUrl(apartment) + "/" + invitation), null).andExpect(status().isForbidden());
    }

    private static String membersUrl(long apartment) {
        return "/api/apartments/" + apartment + "/members";
    }

    private static String invitationsUrl(long apartment) {
        return "/api/apartments/" + apartment + "/invitations";
    }
}
