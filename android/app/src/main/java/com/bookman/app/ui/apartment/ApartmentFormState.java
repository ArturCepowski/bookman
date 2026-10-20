package com.bookman.app.ui.apartment;

import androidx.annotation.Nullable;
import com.bookman.app.ui.common.FieldError;

public final class ApartmentFormState {

    public final boolean loading;
    @Nullable public final FieldError nameError;
    @Nullable public final FieldError bookingUrlError;
    @Nullable public final FieldError icalUrlError;
    @Nullable public final FieldError timeZoneError;

    private ApartmentFormState(boolean loading, @Nullable FieldError nameError, @Nullable FieldError bookingUrlError,
            @Nullable FieldError icalUrlError, @Nullable FieldError timeZoneError) {
        this.loading = loading;
        this.nameError = nameError;
        this.bookingUrlError = bookingUrlError;
        this.icalUrlError = icalUrlError;
        this.timeZoneError = timeZoneError;
    }

    static ApartmentFormState idle() {
        return new ApartmentFormState(false, null, null, null, null);
    }

    static ApartmentFormState loading() {
        return new ApartmentFormState(true, null, null, null, null);
    }

    static ApartmentFormState invalid(ApartmentForm form) {
        return new ApartmentFormState(false, form.nameError, form.bookingUrlError, form.icalUrlError, form.timeZoneError);
    }

    static ApartmentFormState icalRejected() {
        return new ApartmentFormState(false, null, null, FieldError.ICAL_URL_INVALID, null);
    }
}
