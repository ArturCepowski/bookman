package com.bookman.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookman.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Q-24: phones register their FCM token after login and remove it on logout. */
class DeviceApiTest extends IntegrationTest {

    @Autowired DeviceTokenRepository devices;

    @Test
    void REQ11_phoneRegistersAndUnregistersItsToken() throws Exception {
        TestUser anna = registerUser("anna@example.com");

        call(anna, post("/api/users/me/devices"), Map.of("token", "t-1")).andExpect(status().isNoContent());
        call(anna, post("/api/users/me/devices"), Map.of("token", "t-1")).andExpect(status().isNoContent());
        assertThat(devices.findByUserId(anna.id())).hasSize(1);

        call(anna, post("/api/users/me/devices/unregister"), Map.of("token", "t-1")).andExpect(status().isNoContent());
        assertThat(devices.findByUserId(anna.id())).isEmpty();
    }

    @Test
    void REQ11_phoneUsedByAnotherAccountGetsOnlyTheCurrentUsersReminders() throws Exception {
        TestUser anna = registerUser("anna@example.com");
        TestUser bob = registerUser("bob@example.com");
        call(anna, post("/api/users/me/devices"), Map.of("token", "shared-phone"));

        call(bob, post("/api/users/me/devices"), Map.of("token", "shared-phone")).andExpect(status().isNoContent());

        assertThat(devices.findByToken("shared-phone")).get().extracting(DeviceToken::getUserId).isEqualTo(bob.id());
        call(anna, post("/api/users/me/devices/unregister"), Map.of("token", "shared-phone"));
        assertThat(devices.findByToken("shared-phone")).as("anna cannot remove bob's registration").isPresent();
    }

    @Test
    void REQ11_registrationRequiresLoginAndToken() throws Exception {
        TestUser anna = registerUser("anna@example.com");
        mvc.perform(post("/api/users/me/devices").contentType("application/json").content("{\"token\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        call(anna, post("/api/users/me/devices"), Map.of("token", " ")).andExpect(status().isBadRequest());
    }
}
