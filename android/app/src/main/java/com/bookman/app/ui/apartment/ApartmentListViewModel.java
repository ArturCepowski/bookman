package com.bookman.app.ui.apartment;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Event;
import com.bookman.app.data.apartment.ApartmentRepository;
import com.bookman.app.data.auth.AuthRepository;
import com.bookman.app.data.member.MemberRepository;
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
    private final MutableLiveData<Integer> pendingInvitations = new MutableLiveData<>(0);
    private final MemberRepository members;

    @Inject
    public ApartmentListViewModel(ApartmentRepository apartments, AuthRepository auth, MemberRepository members) {
        this.apartments = apartments;
        this.auth = auth;
        this.members = members;
    }

    /** REQ-09: how many invitations wait for this user (shown as a banner). */
    public LiveData<Integer> pendingInvitations() {
        return pendingInvitations;
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
        members.myInvitations(result -> {
            if (result.isSuccess()) {
                pendingInvitations.postValue(result.value().size());
            }
        });
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
