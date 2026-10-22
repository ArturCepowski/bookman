package com.bookman.auth;

import com.bookman.user.Language;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        // REQ-01 / Q-15 (confirmed): password 8-72 chars (72 = bcrypt input limit).
        @NotBlank @Size(min = 8, max = 72) String password,
        // Q-24 (owner decision): optional; when given it must be E.164.
        @Pattern(regexp = PhoneNumbers.E164, message = "must be in E.164 format, e.g. +48600100200")
        String phoneNumber,
        // REQ-14: the app sends its current (system-derived) language so reminder texts match from day one.
        @NotNull Language language) {
}
