package com.bookman.app.ui.member;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.fakes.FakeMemberRepository;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;

public class InvitationsViewModelTest {

    @Rule public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeMemberRepository repository = new FakeMemberRepository();

    @Test
    public void REQ09_listsMyInvitations() {
        repository.myInvitationsResult = Result.success(Collections.singletonList(FakeMemberRepository.invitation(1)));
        InvitationsViewModel vm = new InvitationsViewModel(repository);

        vm.load();

        assertEquals("Sunny flat", vm.invitations().getValue().get(0).apartmentName);
    }

    @Test
    public void REQ09_acceptingReportsTheApartmentAndReloads() {
        InvitationsViewModel vm = new InvitationsViewModel(repository);

        vm.accept(1);

        assertEquals("Sunny flat", vm.accepted().getValue().consume().name);
        assertEquals(Arrays.asList("accept:1", "mine"), repository.calls);
    }

    @Test
    public void REQ09_decliningReloads() {
        InvitationsViewModel vm = new InvitationsViewModel(repository);

        vm.decline(1);

        assertEquals(Arrays.asList("decline:1", "mine"), repository.calls);
        assertNull(vm.accepted().getValue());
    }

    @Test
    public void REQ09_expiredInvitationShowsError() {
        repository.acceptResult = Result.failure(AppError.INVITATION_EXPIRED);
        InvitationsViewModel vm = new InvitationsViewModel(repository);

        vm.accept(1);

        assertEquals(AppError.INVITATION_EXPIRED, vm.errors().getValue().consume());
        assertNull(vm.accepted().getValue());
    }
}
