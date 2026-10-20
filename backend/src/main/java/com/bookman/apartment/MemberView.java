package com.bookman.apartment;

/** REQ-10 row: a user with a role in the apartment. */
public record MemberView(Long userId, String email, Role role) {
}
