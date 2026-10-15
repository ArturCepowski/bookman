package com.bookman.app.ui.apartment;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Event;
import com.bookman.app.common.Result;
import com.bookman.app.data.apartment.ApartmentDetails;
import com.bookman.app.data.apartment.ApartmentRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/** Register (REQ-04) or edit (REQ-05) an apartment; {@link #ARG_APARTMENT_ID} = -1 means "new". */
@HiltViewModel
public class ApartmentFormViewModel extends ViewModel {

    public static final String ARG_APARTMENT_ID = "apartmentId";
    public static final long NEW = -1L;

    private final ApartmentRepository repository;
    private final long apartmentId;
    private final MutableLiveData<ApartmentFormState> state = new MutableLiveData<>(ApartmentFormState.idle());
    private final MutableLiveData<ApartmentDetails> existing = new MutableLiveData<>();
    private final MutableLiveData<Event<AppError>> errors = new MutableLiveData<>();
    private final MutableLiveData<Event<Long>> saved = new MutableLiveData<>();

    @Inject
    public ApartmentFormViewModel(SavedStateHandle args, ApartmentRepository repository) {
        this.repository = repository;
        Long id = args.get(ARG_APARTMENT_ID);
        this.apartmentId = id == null ? NEW : id;
        if (isEdit()) {
            state.setValue(ApartmentFormState.loading());
            repository.get(apartmentId, result -> {
                state.postValue(ApartmentFormState.idle());
                if (result.isSuccess()) {
                    existing.postValue(result.value());
                } else {
                    errors.postValue(new Event<>(result.error()));
                }
            });
        }
    }

    public boolean isEdit() {
        return apartmentId != NEW;
    }

    public LiveData<ApartmentFormState> state() {
        return state;
    }

    /** Current values to pre-fill the form when editing. */
    public LiveData<ApartmentDetails> existing() {
        return existing;
    }

    public LiveData<Event<AppError>> errors() {
        return errors;
    }

    /** Emits the id of the saved apartment. */
    public LiveData<Event<Long>> saved() {
        return saved;
    }

    public void save(String name, String bookingUrl, String icalUrl, String timeZone) {
        ApartmentFormState current = state.getValue();
        if (current != null && current.loading) {
            return;
        }
        ApartmentForm form = ApartmentForm.of(name, bookingUrl, icalUrl, timeZone);
        if (!form.isValid()) {
            state.setValue(ApartmentFormState.invalid(form));
            return;
        }
        state.setValue(ApartmentFormState.loading());
        if (isEdit()) {
            repository.update(apartmentId, form.input, this::onSaved);
        } else {
            repository.create(form.input, this::onSaved);
        }
    }

    private void onSaved(Result<ApartmentDetails> result) {
        if (result.isSuccess()) {
            state.postValue(ApartmentFormState.idle());
            saved.postValue(new Event<>(result.value().id));
            return;
        }
        AppError error = result.error();
        boolean icalProblem = error == AppError.ICAL_UNREACHABLE || error == AppError.ICAL_NOT_A_CALENDAR;
        state.postValue(icalProblem ? ApartmentFormState.icalRejected() : ApartmentFormState.idle());
        errors.postValue(new Event<>(error));
    }
}
