package com.bookman.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookman.security.JwtProperties;
import com.bookman.security.JwtService;
import com.bookman.support.IntegrationTest;
import com.bookman.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AuthApiTest extends IntegrationTest {

    @Autowired UserRepository users;
    @Autowired JwtProperties jwtProperties;

    @Test
    void REQ01_registerReturnsTokenAndStoresUser() throws Exception {
        register("Anna@Example.com", "secret123", "+48600100200", "PL")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("anna@example.com"))
                .andExpect(jsonPath("$.user.phoneNumber").value("+48600100200"))
                .andExpect(jsonPath("$.user.language").value("PL"));

        assertThat(users.findByEmail("anna@example.com")).isPresent();
    }

    @Test
    void REQ01_duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        register("anna@example.com", "secret123", "+48600100200", "EN").andExpect(status().isCreated());

        register("ANNA@Example.COM", "other-pass", "+48600100201", "EN")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void REQ01_phoneNumberMustBeE164() throws Exception {
        for (String bad : new String[] {"600100200", "+0600100200", "+48 600 100 200", "+4860010020012345"}) {
            register("p" + bad.hashCode() + "@example.com", "secret123", bad, "EN")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists());
        }
    }

    @Test
    void REQ01_phoneNumberIsOptional() throws Exception {
        String body = json.writeValueAsString(Map.of(
                "email", "a@example.com", "password", "secret123", "language", "EN"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.phoneNumber").doesNotExist());
    }

    @Test
    void REQ01_invalidEmailAndShortPasswordAreRejected() throws Exception {
        register("not-an-email", "short", "+48600100200", "EN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void REQ01_loginWithCorrectCredentialsReturnsToken() throws Exception {
        register("anna@example.com", "secret123", "+48600100200", "EN");

        login("ANNA@example.com", "secret123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("anna@example.com"));
    }

    @Test
    void REQ01_loginWithWrongPasswordOrUnknownEmailFails() throws Exception {
        register("anna@example.com", "secret123", "+48600100200", "EN");

        login("anna@example.com", "wrong-pass")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login("nobody@example.com", "secret123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void REQ01_tokenKeepsUserLoggedIn() throws Exception {
        String token = token(register("anna@example.com", "secret123", "+48600100200", "EN"));

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("anna@example.com"));
    }

    @Test
    void REQ01_requestsWithoutValidTokenAreRejected() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void REQ01_expiredSessionIsRejected() throws Exception {
        String token = token(register("anna@example.com", "secret123", "+48600100200", "EN"));
        long userId = json.readTree(mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        Clock longAgo = Clock.fixed(Instant.now().minus(Duration.ofDays(2)), ZoneOffset.UTC);
        String expired = new JwtService(jwtProperties, longAgo).issueToken(userId);

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions register(String email, String password, String phone, String language) throws Exception {
        String body = json.writeValueAsString(Map.of(
                "email", email, "password", password, "phoneNumber", phone, "language", language));
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = json.writeValueAsString(Map.of("email", email, "password", password));
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String token(ResultActions result) throws Exception {
        JsonNode node = json.readTree(result.andReturn().getResponse().getContentAsString());
        return node.get("token").asText();
    }
}
