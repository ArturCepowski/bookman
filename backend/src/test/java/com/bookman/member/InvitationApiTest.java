package com.bookman.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookman.support.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

class InvitationApiTest extends IntegrationTest {

    private static final String ICAL = "https://ical.booking.com/v1/export?t=invitations";

    @Autowired InvitationRepository invitations;
    @Autowired JdbcTemplate jdbc;

    @Test
    void REQ09_invitedUserSeesInvitationAndAcceptingGrantsCleaningServiceRole() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        long apartment = createApartment(admin, ICAL);

        invite(admin, apartment, "Cleaner@Example.com")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("cleaner@example.com"));

        long invitation = bodyOf(call(cleaner, get("/api/invitations"), null)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].apartmentName").value("Sunny flat"))
                .andExpect(jsonPath("$[0].invitedBy").value("admin@example.com")))
                .get(0).get("id").asLong();

        call(cleaner, post("/api/invitations/" + invitation + "/accept"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(apartment))
                .andExpect(jsonPath("$.role").value("CLEANING_SERVICE"));

        call(cleaner, get("/api/apartments"), null)
                .andExpect(jsonPath("$[0].id").value(apartment))
                .andExpect(jsonPath("$[0].role").value("CLEANING_SERVICE"));
        call(cleaner, get("/api/invitations"), null).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void REQ09_invitationAppearsAfterRegisteringWithTheInvitedEmail() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        long apartment = createApartment(admin, ICAL);
        invite(admin, apartment, "newcomer@example.com").andExpect(status().isCreated());

        TestUser newcomer = registerUser("NEWCOMER@example.com");

        call(newcomer, get("/api/invitations"), null).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void REQ09_invitedUserCanDecline() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        long apartment = createApartment(admin, ICAL);
        long invitation = idOf(invite(admin, apartment, "cleaner@example.com"));

        call(cleaner, post("/api/invitations/" + invitation + "/decline"), null).andExpect(status().isNoContent());

        call(cleaner, get("/api/apartments"), null).andExpect(jsonPath("$.length()").value(0));
        call(cleaner, post("/api/invitations/" + invitation + "/accept"), null).andExpect(status().isNotFound());
        assertThat(invitations.findById(invitation).orElseThrow().getStatus()).isEqualTo(InvitationStatus.DECLINED);
    }

    @Test
    void REQ09_invitationExpiresAfter14Days() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        long apartment = createApartment(admin, ICAL);
        Instant before = Instant.now();
        long invitation = idOf(invite(admin, apartment, "cleaner@example.com"));

        Instant expiresAt = invitations.findById(invitation).orElseThrow().getExpiresAt();
        assertThat(Duration.between(before, expiresAt)).isBetween(Duration.ofDays(14).minusMinutes(1), Duration.ofDays(14).plusMinutes(1));

        jdbc.update("update invitation set expires_at = now() - interval '1 minute' where id = ?", invitation);

        call(cleaner, get("/api/invitations"), null).andExpect(jsonPath("$.length()").value(0));
        call(cleaner, post("/api/invitations/" + invitation + "/accept"), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITATION_EXPIRED"));
        call(admin, get("/api/apartments/" + apartment + "/members"), null)
                .andExpect(jsonPath("$.invitations.length()").value(0));
        invite(admin, apartment, "cleaner@example.com").andExpect(status().isCreated());
    }

    @Test
    void REQ09_invitingSomeoneWhoAlreadyHasARoleIsAnError() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        long apartment = createApartment(admin, ICAL);
        addCleaningService(apartment, cleaner);

        invite(admin, apartment, "CLEANER@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_MEMBER"));
        invite(admin, apartment, "admin@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_MEMBER"));
    }

    @Test
    void REQ09_secondPendingInvitationForSameEmailIsAnError() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        long apartment = createApartment(admin, ICAL);
        invite(admin, apartment, "cleaner@example.com").andExpect(status().isCreated());

        invite(admin, apartment, "cleaner@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITATION_PENDING"));
    }

    @Test
    void REQ09_onlyAdminCanInvite() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        TestUser cleaner = registerUser("cleaner@example.com");
        TestUser stranger = registerUser("stranger@example.com");
        long apartment = createApartment(admin, ICAL);
        addCleaningService(apartment, cleaner);

        invite(cleaner, apartment, "friend@example.com").andExpect(status().isForbidden());
        invite(stranger, apartment, "friend@example.com").andExpect(status().isForbidden());
    }

    @Test
    void REQ09_nobodyElseCanAcceptSomeonesInvitation() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        TestUser other = registerUser("other@example.com");
        long apartment = createApartment(admin, ICAL);
        long invitation = idOf(invite(admin, apartment, "cleaner@example.com"));

        call(other, post("/api/invitations/" + invitation + "/accept"), null).andExpect(status().isNotFound());
        call(other, get("/api/apartments"), null).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void REQ05_deletingApartmentRemovesPendingInvitations() throws Exception {
        TestUser admin = registerUser("admin@example.com");
        long apartment = createApartment(admin, ICAL);
        long invitation = idOf(invite(admin, apartment, "cleaner@example.com"));

        call(admin, delete("/api/apartments/" + apartment), null).andExpect(status().isNoContent());

        assertThat(invitations.findById(invitation)).isEmpty();
    }

    private ResultActions invite(TestUser user, long apartment, String email) throws Exception {
        return call(user, post("/api/apartments/" + apartment + "/invitations"), Map.of("email", email));
    }

    private long idOf(ResultActions result) throws Exception {
        return bodyOf(result).get("id").asLong();
    }
}
