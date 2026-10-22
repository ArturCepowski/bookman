package com.bookman.user;

import com.bookman.auth.PhoneNumbers;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** REQ-02: e-mail is deliberately absent – it cannot be changed in v1. Phone is optional (Q-24); null removes it. */
public record UpdateProfileRequest(
        @Pattern(regexp = PhoneNumbers.E164, message = "must be in E.164 format, e.g. +48600100200")
        String phoneNumber,
        @NotNull Language language) {
}
