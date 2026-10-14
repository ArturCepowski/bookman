package com.bookman.auth;

import com.bookman.user.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
