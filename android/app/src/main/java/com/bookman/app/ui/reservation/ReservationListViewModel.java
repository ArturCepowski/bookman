package com.bookman.app.ui.reservation;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Event;
import com.bookman.app.data.apartment.ApartmentRepository;
import com.bookman.app.data.apartment.Role;
import com.bookman.app.data.reservation.CleaningStatus;
import com.bookman.app.data.reservation.Reservation;
import com.bookman.app.data.reservation.ReservationRepository;
import com.bookman.app.ui.apartment.ApartmentFormViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

/** REQ-07 reservation list of one apartment; pull to refresh imports first (REQ-06); status changes (REQ-08). */
@HiltViewModel
public class ReservationListViewModel extends ViewModel {

    private final ApartmentRepository apartments;
    private final ReservationRepository reservations;
    private final long apartmentId;
    private final MutableLiveData<ReservationListState> state = new MutableLiveData<>(ReservationListState.initial());
    private final MutableLiveData<Event<AppError>> errors = new MutableLiveData<>();
    // Callbacks arrive on a background thread; this is the latest state, independent of LiveData delivery.
    private volatile ReservationListState current = ReservationListState.initial();

    @Inject
    public ReservationListViewModel(SavedStateHandle args, ApartmentRepository apartments,
            ReservationRepository reservations) {
        this.apartments = apartments;
        this.reservations = reservations;
        Long id = args.get(ApartmentFormViewModel.ARG_APARTMENT_ID);
        if (id == null) {
            throw new IllegalArgumentException("apartmentId argument is required");
        }
        this.apartmentId = id;
    }

    public long apartmentId() {
        return apartmentId;
    }

    public LiveData<ReservationListState> state() {
        return state;
    }

    public LiveData<Event<AppError>> errors() {
        return errors;
    }

    /** Loads the caller's role and the current list (no import). */
    public void load() {
        publish(current.withLoading(true));
        apartments.get(apartmentId, result -> {
            if (!result.isSuccess()) {
                publish(current.withLoading(false));
                errors.postValue(new Event<>(result.error()));
                return;
            }
            publish(current.withApartment(result.value().role, result.value().name));
            reloadList();
        });
    }

    /** REQ-06 pull to refresh: import from Booking.com, then show the list even if the import failed. */
    public void refresh() {
        publish(current.withLoading(true));
        reservations.importNow(apartmentId, result -> {
            if (!result.isSuccess()) {
                errors.postValue(new Event<>(result.error()));
            }
            if (current.role == null) {
                load();
            } else {
                reloadList();
            }
        });
    }

    /** Status changes this user may make on the given reservation. */
    public List<CleaningStatus> actionsFor(Reservation reservation) {
        Role role = current.role;
        return role == null ? Collections.emptyList() : CleaningActions.targets(role, reservation.cleaningStatus);
    }

    public void changeStatus(long reservationId, CleaningStatus status) {
        publish(current.withLoading(true));
        reservations.changeCleaningStatus(apartmentId, reservationId, status, result -> {
            if (result.isSuccess()) {
                List<Reservation> updated = new ArrayList<>(current.items);
                for (int i = 0; i < updated.size(); i++) {
                    if (updated.get(i).id == reservationId) {
                        updated.set(i, result.value());
                    }
                }
                publish(current.withItems(updated));
                return;
            }
            errors.postValue(new Event<>(result.error()));
            // Someone else changed it, or it was cancelled meanwhile: show the real state.
            reloadList();
        });
    }

    private void reloadList() {
        reservations.list(apartmentId, result -> {
            if (result.isSuccess()) {
                publish(current.withItems(result.value()));
            } else {
                publish(current.withLoading(false));
                errors.postValue(new Event<>(result.error()));
            }
        });
    }

    private void publish(ReservationListState next) {
        current = next;
        state.postValue(next);
    }
}
