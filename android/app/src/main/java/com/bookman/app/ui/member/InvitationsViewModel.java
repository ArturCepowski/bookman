package com.bookman.app.ui.member;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Event;
import com.bookman.app.data.apartment.ApartmentSummary;
import com.bookman.app.data.member.MemberRepository;
import com.bookman.app.data.member.MyInvitation;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

/** REQ-09: the logged-in user's pending invitations, accept or decline. */
@HiltViewModel
public class InvitationsViewModel extends ViewModel {

    private final MemberRepository repository;
    private final MutableLiveData<List<MyInvitation>> invitations = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<AppError>> errors = new MutableLiveData<>();
    private final MutableLiveData<Event<ApartmentSummary>> accepted = new MutableLiveData<>();

    @Inject
    public InvitationsViewModel(MemberRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<MyInvitation>> invitations() {
        return invitations;
    }

    public LiveData<Boolean> loading() {
        return loading;
    }

    public LiveData<Event<AppError>> errors() {
        return errors;
    }

    /** Emits the apartment that was joined. */
    public LiveData<Event<ApartmentSummary>> accepted() {
        return accepted;
    }

    public void load() {
        loading.setValue(true);
        repository.myInvitations(result -> {
            loading.postValue(false);
            if (result.isSuccess()) {
                invitations.postValue(result.value());
            } else {
                errors.postValue(new Event<>(result.error()));
            }
        });
    }

    public void accept(long invitationId) {
        loading.setValue(true);
        repository.accept(invitationId, result -> {
            if (result.isSuccess()) {
                accepted.postValue(new Event<>(result.value()));
            } else {
                errors.postValue(new Event<>(result.error()));
            }
            reload();
        });
    }

    public void decline(long invitationId) {
        loading.setValue(true);
        repository.decline(invitationId, result -> {
            if (!result.isSuccess()) {
                errors.postValue(new Event<>(result.error()));
            }
            reload();
        });
    }

    private void reload() {
        repository.myInvitations(result -> {
            loading.postValue(false);
            if (result.isSuccess()) {
                invitations.postValue(result.value());
            }
        });
    }
}
