package com.bookman.security;

/** Principal stored in the security context for requests carrying a valid JWT. */
public record AuthenticatedUser(long userId) {
}
