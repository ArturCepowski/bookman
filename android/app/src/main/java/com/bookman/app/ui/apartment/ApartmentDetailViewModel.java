package com.bookman.app.ui.apartment;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Event;
import com.bookman.app.data.apartment.ApartmentDetails;
import com.bookman.app.data.apartment.ApartmentRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class ApartmentDetailViewModel extends ViewModel {

    private final ApartmentRepository repository;
    private final long apartmentId;
    private final MutableLiveData<ApartmentDetails> details = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<AppError>> errors = new MutableLiveData<>();

    @Inject
    public ApartmentDetailViewModel(SavedStateHandle args, ApartmentRepository repository) {
        this.repository = repository;
        Long id = args.get(ApartmentFormViewModel.ARG_APARTMENT_ID);
        if (id == null) {
            throw new IllegalArgumentException("apartmentId argument is required");
        }
        this.apartmentId = id;
    }

    public long apartmentId() {
        return apartmentId;
    }

    public LiveData<ApartmentDetails> details() {
        return details;
    }

    public LiveData<Boolean> loading() {
        return loading;
    }

    public LiveData<Event<AppError>> errors() {
        return errors;
    }

    /** REQ-05: editing is offered only to the apartment's ADMIN (delete is on the reservation screen). */
    public boolean canManage() {
        ApartmentDetails current = details.getValue();
        return current != null && current.canManage();
    }

    public void refresh() {
        loading.setValue(true);
        repository.get(apartmentId, result -> {
            loading.postValue(false);
            if (result.isSuccess()) {
                details.postValue(result.value());
            } else {
                errors.postValue(new Event<>(result.error()));
            }
        });
    }
}
