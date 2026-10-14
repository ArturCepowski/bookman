package com.bookman.app.ui.apartment;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Event;
import com.bookman.app.data.apartment.ApartmentRepository;
import com.bookman.app.data.auth.AuthRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.Collections;
import javax.inject.Inject;

/** REQ-03 main screen. */
@HiltViewModel
public class ApartmentListViewModel extends ViewModel {

    private final ApartmentRepository apartments;
    private final AuthRepository auth;
    private final MutableLiveData<ApartmentListState> state =
            new MutableLiveData<>(new ApartmentListState(false, Collections.emptyList(), false));
    private final MutableLiveData<Event<AppError>> errors = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> loggedOut = new MutableLiveData<>();

    @Inject
    public ApartmentListViewModel(ApartmentRepository apartments, AuthRepository auth) {
        this.apartments = apartments;
        this.auth = auth;
    }

    public LiveData<ApartmentListState> state() {
        return state;
    }

    public LiveData<Event<AppError>> errors() {
        return errors;
    }

    public LiveData<Event<Boolean>> loggedOut() {
        return loggedOut;
    }

    public void refresh() {
        ApartmentListState before = state.getValue();
        state.setValue(new ApartmentListState(true, before.items, before.loaded));
        apartments.list(result -> {
            if (result.isSuccess()) {
                state.postValue(new ApartmentListState(false, result.value(), true));
            } else {
                state.postValue(new ApartmentListState(false, before.items, before.loaded));
                errors.postValue(new Event<>(result.error()));
            }
        });
    }

    public void logout() {
        auth.logout();
        loggedOut.setValue(new Event<>(true));
    }
}
