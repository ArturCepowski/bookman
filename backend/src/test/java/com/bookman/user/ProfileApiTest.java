package com.bookman.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookman.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfileApiTest extends IntegrationTest {

    @Test
    void REQ02_userCanViewAndEditPhoneAndLanguage() throws Exception {
        TestUser anna = registerUser("anna@example.com");

        call(anna, put("/api/users/me"), Map.of("phoneNumber", "+48700200300", "language", "PL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+48700200300"))
                .andExpect(jsonPath("$.language").value("PL"));

        call(anna, get("/api/users/me"), null)
                .andExpect(jsonPath("$.phoneNumber").value("+48700200300"))
                .andExpect(jsonPath("$.language").value("PL"));
    }

    @Test
    void REQ02_emailCannotBeChanged() throws Exception {
        TestUser anna = registerUser("anna@example.com");

        call(anna, put("/api/users/me"),
                Map.of("email", "other@example.com", "phoneNumber", "+48700200300", "language", "EN"))
                .andExpect(status().isOk());

        call(anna, get("/api/users/me"), null).andExpect(jsonPath("$.email").value("anna@example.com"));
    }

    @Test
    void REQ02_invalidPhoneOrLanguageIsRejected() throws Exception {
        TestUser anna = registerUser("anna@example.com");

        call(anna, put("/api/users/me"), Map.of("phoneNumber", "700200300", "language", "EN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists());
        call(anna, put("/api/users/me"), Map.of("phoneNumber", "+48700200300", "language", "DE"))
                .andExpect(status().isBadRequest());
        call(anna, get("/api/users/me"), null).andExpect(jsonPath("$.phoneNumber").value("+48600100200"));
    }

    @Test
    void REQ02_phoneNumberCanBeRemoved() throws Exception {
        TestUser anna = registerUser("anna@example.com");

        call(anna, put("/api/users/me"), Map.of("language", "PL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.language").value("PL"));
    }
}
