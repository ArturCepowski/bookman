package com.bookman.app.data.api;

public class MyInvitationDto {
    public long id;
    public long apartmentId;
    public String apartmentName;
    public String invitedBy;
    /** ISO-8601 instant */
    public String expiresAt;
}
