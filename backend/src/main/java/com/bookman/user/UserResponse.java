package com.bookman.user;

public record UserResponse(Long id, String email, String phoneNumber, Language language) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getPhoneNumber(), user.getLanguage());
    }
}
