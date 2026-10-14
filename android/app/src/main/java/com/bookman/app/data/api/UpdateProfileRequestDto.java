package com.bookman.app.data.api;

public class UpdateProfileRequestDto {
    public final String phoneNumber;
    public final String language;

    public UpdateProfileRequestDto(String phoneNumber, String language) {
        this.phoneNumber = phoneNumber;
        this.language = language;
    }
}
