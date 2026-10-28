package com.bookman.app.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bookman.app.common.AppError;
import com.bookman.app.common.AppLanguage;
import com.bookman.app.common.DeviceLanguage;
import com.bookman.app.common.Event;
import com.bookman.app.data.auth.AuthRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class RegisterViewModel extends ViewModel {

    private final AuthRepository repository;
    private final DeviceLanguage deviceLanguage;
    private final AppLanguage appLanguage;
    private final MutableLiveData<AuthFormState> state = new MutableLiveData<>(AuthFormState.idle());
    private final MutableLiveData<Event<AppError>> errors = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> registered = new MutableLiveData<>();

    @Inject
    public RegisterViewModel(AuthRepository repository, DeviceLanguage deviceLanguage, AppLanguage appLanguage) {
        this.repository = repository;
        this.deviceLanguage = deviceLanguage;
        this.appLanguage = appLanguage;
    }

    public LiveData<AuthFormState> state() {
        return state;
    }

    public LiveData<Event<AppError>> errors() {
        return errors;
    }

    public LiveData<Event<Boolean>> registered() {
        return registered;
    }

    public void register(String email, String password, String phone) {
        AuthFormState current = state.getValue();
        if (current != null && current.loading) {
            return;
        }
        RegistrationForm form = RegistrationForm.of(email, password, phone);
        if (!form.isValid()) {
            state.setValue(new AuthFormState(false, form.emailError, form.passwordError, form.phoneError));
            return;
        }
        state.setValue(AuthFormState.loading());
        repository.register(form.email, form.password, form.phoneNumber, deviceLanguage.code(), result -> {
            state.postValue(AuthFormState.idle());
            if (result.isSuccess()) {
                // REQ-14: from now on the profile language decides (initially the device language).
                appLanguage.apply(result.value().language());
                registered.postValue(new Event<>(true));
            } else {
                errors.postValue(new Event<>(result.error()));
            }
        });
    }
}
