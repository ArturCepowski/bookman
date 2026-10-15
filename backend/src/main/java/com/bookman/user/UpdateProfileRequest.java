package com.bookman.user;

import com.bookman.auth.PhoneNumbers;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** REQ-02: e-mail is deliberately absent – it cannot be changed in v1. */
public record UpdateProfileRequest(
        @NotBlank @Pattern(regexp = PhoneNumbers.E164, message = "must be in E.164 format, e.g. +48600100200")
        String phoneNumber,
        @NotNull Language language) {
}
