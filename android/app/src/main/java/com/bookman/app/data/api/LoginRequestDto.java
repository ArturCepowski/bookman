package com.bookman.app.data.api;

public class LoginRequestDto {
    public final String email;
    public final String password;

    public LoginRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
