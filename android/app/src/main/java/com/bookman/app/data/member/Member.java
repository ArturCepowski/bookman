package com.bookman.app.data.member;

import com.bookman.app.data.apartment.Role;

public final class Member {

    public final long userId;
    public final String email;
    public final Role role;

    public Member(long userId, String email, Role role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }
}
