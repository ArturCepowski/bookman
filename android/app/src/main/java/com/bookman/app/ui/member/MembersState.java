package com.bookman.app.ui.member;

import androidx.annotation.Nullable;
import com.bookman.app.data.member.MembersOverview;
import com.bookman.app.ui.common.FieldError;

public final class MembersState {

    public final boolean loading;
    public final MembersOverview overview;
    @Nullable public final FieldError inviteEmailError;

    MembersState(boolean loading, MembersOverview overview, @Nullable FieldError inviteEmailError) {
        this.loading = loading;
        this.overview = overview;
        this.inviteEmailError = inviteEmailError;
    }

    MembersState withLoading(boolean value) {
        return new MembersState(value, overview, null);
    }

    MembersState withOverview(MembersOverview value) {
        return new MembersState(false, value, null);
    }

    MembersState withInviteError(FieldError error) {
        return new MembersState(false, overview, error);
    }
}
