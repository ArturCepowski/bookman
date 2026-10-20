package com.bookman.app.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Event;
import com.bookman.app.data.auth.AuthRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/** Placeholder home for Stage 1; Stage 2 replaces it with the apartment list (REQ-03). */
@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final MutableLiveData<Event<AppError>> errors = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> loggedOut = new MutableLiveData<>();

    @Inject
    public HomeViewModel(AuthRepository repository) {
        this.repository = repository;
        refresh();
    }

    public LiveData<String> email() {
        return email;
    }

    public LiveData<Event<AppError>> errors() {
        return errors;
    }

    public LiveData<Event<Boolean>> loggedOut() {
        return loggedOut;
    }

    public void refresh() {
        repository.loadCurrentUser(result -> {
            if (result.isSuccess()) {
                email.postValue(result.value().email());
            } else if (result.error() == AppError.SESSION_EXPIRED) {
                repository.logout();
                errors.postValue(new Event<>(AppError.SESSION_EXPIRED));
                loggedOut.postValue(new Event<>(true));
            } else {
                errors.postValue(new Event<>(result.error()));
            }
        });
    }

    public void logout() {
        repository.logout();
        loggedOut.setValue(new Event<>(true));
    }
}
