package com.bookman.app.ui.reservation;

import androidx.annotation.Nullable;
import com.bookman.app.data.apartment.Role;
import com.bookman.app.data.reservation.Reservation;
import java.util.Collections;
import java.util.List;

public final class ReservationListState {

    public final boolean loading;
    public final List<Reservation> items;
    public final boolean loaded;
    /** Caller's role in the apartment; null until loaded. */
    @Nullable public final Role role;
    @Nullable public final String apartmentName;

    ReservationListState(boolean loading, List<Reservation> items, boolean loaded, @Nullable Role role,
            @Nullable String apartmentName) {
        this.loading = loading;
        this.items = Collections.unmodifiableList(items);
        this.loaded = loaded;
        this.role = role;
        this.apartmentName = apartmentName;
    }

    static ReservationListState initial() {
        return new ReservationListState(false, Collections.emptyList(), false, null, null);
    }

    ReservationListState withLoading(boolean value) {
        return new ReservationListState(value, items, loaded, role, apartmentName);
    }

    ReservationListState withItems(List<Reservation> value) {
        return new ReservationListState(false, value, true, role, apartmentName);
    }

    ReservationListState withApartment(Role value, String name) {
        return new ReservationListState(loading, items, loaded, value, name);
    }

    public boolean isEmpty() {
        return loaded && !loading && items.isEmpty();
    }
}
