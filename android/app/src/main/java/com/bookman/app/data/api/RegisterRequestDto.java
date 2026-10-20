package com.bookman.app.data.api;

public class RegisterRequestDto {
    public final String email;
    public final String password;
    public final String phoneNumber;
    public final String language;

    public RegisterRequestDto(String email, String password, String phoneNumber, String language) {
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.language = language;
    }
}
