package com.bookman.app.ui.profile;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Event;
import com.bookman.app.common.Result;
import com.bookman.app.data.auth.User;
import com.bookman.app.data.profile.ProfileRepository;
import com.bookman.app.ui.common.FieldError;
import com.bookman.app.ui.common.PhoneNumbers;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/** REQ-02: view and edit phone number and preferred language; e-mail is read-only. */
@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final ProfileRepository repository;
    private final MutableLiveData<User> profile = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<FieldError> phoneError = new MutableLiveData<>();
    private final MutableLiveData<Event<AppError>> errors = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> saved = new MutableLiveData<>();

    @Inject
    public ProfileViewModel(ProfileRepository repository) {
        this.repository = repository;
        loading.setValue(true);
        repository.load(this::onProfile);
    }

    public LiveData<User> profile() {
        return profile;
    }

    public LiveData<Boolean> loading() {
        return loading;
    }

    public LiveData<FieldError> phoneError() {
        return phoneError;
    }

    public LiveData<Event<AppError>> errors() {
        return errors;
    }

    public LiveData<Event<Boolean>> saved() {
        return saved;
    }

    /** @param language backend code, "EN" or "PL" */
    public void save(@Nullable String phone, String language) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        // Q-24: an empty field removes the number.
        String normalized = PhoneNumbers.normalizeOptional(phone);
        if (!PhoneNumbers.isValidOptional(normalized)) {
            phoneError.setValue(FieldError.PHONE_INVALID);
            return;
        }
        phoneError.setValue(null);
        loading.setValue(true);
        repository.update(normalized, language, result -> {
            onProfile(result);
            if (result.isSuccess()) {
                saved.postValue(new Event<>(true));
            }
        });
    }

    private void onProfile(Result<User> result) {
        loading.postValue(false);
        if (result.isSuccess()) {
            profile.postValue(result.value());
        } else {
            errors.postValue(new Event<>(result.error()));
        }
    }
}
