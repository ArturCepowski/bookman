package com.bookman.app.ui.member;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Event;
import com.bookman.app.common.Result;
import com.bookman.app.data.member.MemberRepository;
import com.bookman.app.data.member.MembersOverview;
import com.bookman.app.ui.apartment.ApartmentFormViewModel;
import com.bookman.app.ui.common.EmailAddresses;
import com.bookman.app.ui.common.FieldError;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/** REQ-09 (invite) and REQ-10 (members, pending invitations, cancel, remove) for the apartment's admin. */
@HiltViewModel
public class MembersViewModel extends ViewModel {

    private final MemberRepository repository;
    private final long apartmentId;
    private final MutableLiveData<MembersState> state =
            new MutableLiveData<>(new MembersState(false, MembersOverview.empty(), null));
    private final MutableLiveData<Event<AppError>> errors = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> invited = new MutableLiveData<>();
    private volatile MembersState current = state.getValue();

    @Inject
    public MembersViewModel(SavedStateHandle args, MemberRepository repository) {
        this.repository = repository;
        Long id = args.get(ApartmentFormViewModel.ARG_APARTMENT_ID);
        if (id == null) {
            throw new IllegalArgumentException("apartmentId argument is required");
        }
        this.apartmentId = id;
    }

    public LiveData<MembersState> state() {
        return state;
    }

    public LiveData<Event<AppError>> errors() {
        return errors;
    }

    /** Emits the invited e-mail once the invitation was created. */
    public LiveData<Event<String>> invited() {
        return invited;
    }

    public void load() {
        publish(current.withLoading(true));
        repository.overview(apartmentId, this::onOverview);
    }

    /** @return true if the input was valid and the request was sent (the dialog may close) */
    public boolean invite(String rawEmail) {
        String email = EmailAddresses.normalize(rawEmail);
        if (!EmailAddresses.isValid(email)) {
            publish(current.withInviteError(FieldError.EMAIL_INVALID));
            return false;
        }
        publish(current.withLoading(true));
        repository.invite(apartmentId, email, result -> {
            if (result.isSuccess()) {
                invited.postValue(new Event<>(email));
                load();
            } else {
                publish(current.withLoading(false));
                errors.postValue(new Event<>(result.error()));
            }
        });
        return true;
    }

    public void cancelInvitation(long invitationId) {
        publish(current.withLoading(true));
        repository.cancelInvitation(apartmentId, invitationId, this::afterChange);
    }

    public void removeMember(long userId) {
        publish(current.withLoading(true));
        repository.removeMember(apartmentId, userId, this::afterChange);
    }

    private void afterChange(Result<Boolean> result) {
        if (!result.isSuccess()) {
            errors.postValue(new Event<>(result.error()));
        }
        load();
    }

    private void onOverview(Result<MembersOverview> result) {
        if (result.isSuccess()) {
            publish(current.withOverview(result.value()));
        } else {
            publish(current.withLoading(false));
            errors.postValue(new Event<>(result.error()));
        }
    }

    private void publish(MembersState next) {
        current = next;
        state.postValue(next);
    }
}
