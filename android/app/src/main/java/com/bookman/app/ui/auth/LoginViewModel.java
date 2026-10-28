package com.bookman.app.ui.auth;

import com.bookman.app.ui.common.FieldError;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bookman.app.common.AppError;
import com.bookman.app.common.AppLanguage;
import com.bookman.app.common.Event;
import com.bookman.app.data.auth.AuthRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final AuthRepository repository;
    private final AppLanguage appLanguage;
    private final MutableLiveData<AuthFormState> state = new MutableLiveData<>(AuthFormState.idle());
    private final MutableLiveData<Event<AppError>> errors = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> loggedIn = new MutableLiveData<>();

    @Inject
    public LoginViewModel(AuthRepository repository, AppLanguage appLanguage) {
        this.repository = repository;
        this.appLanguage = appLanguage;
    }

    public LiveData<AuthFormState> state() {
        return state;
    }

    public LiveData<Event<AppError>> errors() {
        return errors;
    }

    public LiveData<Event<Boolean>> loggedIn() {
        return loggedIn;
    }

    public void login(String email, String password) {
        AuthFormState current = state.getValue();
        if (current != null && current.loading) {
            return;
        }
        String normalizedEmail = RegistrationForm.normalizeEmail(email);
        String pass = password == null ? "" : password;
        FieldError emailError = normalizedEmail.isEmpty() ? FieldError.EMAIL_INVALID : null;
        FieldError passwordError = pass.isEmpty() ? FieldError.REQUIRED : null;
        if (emailError != null || passwordError != null) {
            state.setValue(new AuthFormState(false, emailError, passwordError, null));
            return;
        }
        state.setValue(AuthFormState.loading());
        repository.login(normalizedEmail, pass, result -> {
            state.postValue(AuthFormState.idle());
            if (result.isSuccess()) {
                // REQ-14: the language stored in the profile wins over the device language after login.
                appLanguage.apply(result.value().language());
                loggedIn.postValue(new Event<>(true));
            } else {
                errors.postValue(new Event<>(result.error()));
            }
        });
    }
}
