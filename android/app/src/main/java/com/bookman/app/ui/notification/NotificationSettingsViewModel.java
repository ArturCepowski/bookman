package com.bookman.app.ui.notification;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Event;
import com.bookman.app.common.Result;
import com.bookman.app.data.apartment.ApartmentRepository;
import com.bookman.app.data.apartment.NotificationSettings;
import com.bookman.app.ui.apartment.ApartmentFormViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalTime;
import javax.inject.Inject;

/** REQ-11 / REQ-12 settings screen (admin only; the backend enforces it, REQ-05). */
@HiltViewModel
public class NotificationSettingsViewModel extends ViewModel {

    /** Only a starting point for the time picker when a reminder is switched on. */
    static final LocalTime SUGGESTED_NOT_PLANNED = LocalTime.of(18, 0);
    static final LocalTime SUGGESTED_NOT_DONE = LocalTime.of(10, 0);

    private final ApartmentRepository repository;
    private final long apartmentId;
    private final MutableLiveData<NotificationSettings> settings = new MutableLiveData<>(new NotificationSettings(null, null));
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<AppError>> errors = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> saved = new MutableLiveData<>();

    @Inject
    public NotificationSettingsViewModel(SavedStateHandle args, ApartmentRepository repository) {
        this.repository = repository;
        Long id = args.get(ApartmentFormViewModel.ARG_APARTMENT_ID);
        if (id == null) {
            throw new IllegalArgumentException("apartmentId argument is required");
        }
        this.apartmentId = id;
        loading.setValue(true);
        repository.notificationSettings(apartmentId, this::onSettings);
    }

    public LiveData<NotificationSettings> settings() {
        return settings;
    }

    public LiveData<Boolean> loading() {
        return loading;
    }

    public LiveData<Event<AppError>> errors() {
        return errors;
    }

    public LiveData<Event<Boolean>> saved() {
        return saved;
    }

    public void setNotPlannedEnabled(boolean enabled) {
        NotificationSettings s = current();
        LocalTime time = enabled ? (s.notPlannedTime != null ? s.notPlannedTime : SUGGESTED_NOT_PLANNED) : null;
        settings.setValue(new NotificationSettings(time, s.notDoneTime));
    }

    public void setNotDoneEnabled(boolean enabled) {
        NotificationSettings s = current();
        LocalTime time = enabled ? (s.notDoneTime != null ? s.notDoneTime : SUGGESTED_NOT_DONE) : null;
        settings.setValue(new NotificationSettings(s.notPlannedTime, time));
    }

    public void setNotPlannedTime(LocalTime time) {
        settings.setValue(new NotificationSettings(time, current().notDoneTime));
    }

    public void setNotDoneTime(LocalTime time) {
        settings.setValue(new NotificationSettings(current().notPlannedTime, time));
    }

    public void save() {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        loading.setValue(true);
        repository.updateNotificationSettings(apartmentId, current(), result -> {
            onSettings(result);
            if (result.isSuccess()) {
                saved.postValue(new Event<>(true));
            }
        });
    }

    private void onSettings(Result<NotificationSettings> result) {
        loading.postValue(false);
        if (result.isSuccess()) {
            settings.postValue(result.value());
        } else {
            errors.postValue(new Event<>(result.error()));
        }
    }

    private NotificationSettings current() {
        NotificationSettings s = settings.getValue();
        return s == null ? new NotificationSettings(null, null) : s;
    }

    @Nullable
    static String label(@Nullable LocalTime time) {
        return time == null ? null : String.format(java.util.Locale.ROOT, "%02d:%02d", time.getHour(), time.getMinute());
    }
}
