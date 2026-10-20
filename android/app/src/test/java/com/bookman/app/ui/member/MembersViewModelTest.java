package com.bookman.app.ui.member;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.data.apartment.Role;
import com.bookman.app.data.member.Member;
import com.bookman.app.data.member.MembersOverview;
import com.bookman.app.data.member.PendingInvitation;
import com.bookman.app.fakes.FakeMemberRepository;
import com.bookman.app.ui.apartment.ApartmentFormViewModel;
import com.bookman.app.ui.common.FieldError;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;

public class MembersViewModelTest {

    @Rule public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeMemberRepository repository = new FakeMemberRepository();

    private MembersViewModel viewModel() {
        return new MembersViewModel(
                new SavedStateHandle(Collections.singletonMap(ApartmentFormViewModel.ARG_APARTMENT_ID, 5L)), repository);
    }

    @Test
    public void REQ10_showsMembersAndPendingInvitations() {
        repository.overviewResult = Result.success(new MembersOverview(
                Arrays.asList(new Member(1, "admin@example.com", Role.ADMIN),
                        new Member(2, "cleaner@example.com", Role.CLEANING_SERVICE)),
                Collections.singletonList(new PendingInvitation(9, "new@example.com", Instant.parse("2026-11-03T10:00:00Z")))));
        MembersViewModel vm = viewModel();

        vm.load();

        MembersState state = vm.state().getValue();
        assertEquals(2, state.overview.members.size());
        assertEquals("new@example.com", state.overview.invitations.get(0).email);
        assertFalse(state.loading);
    }

    @Test
    public void REQ09_inviteNormalizesEmailAndReloads() {
        MembersViewModel vm = viewModel();

        assertTrue(vm.invite("  Cleaner@Example.COM "));

        assertEquals("cleaner@example.com", repository.lastInvitedEmail);
        assertEquals("cleaner@example.com", vm.invited().getValue().consume());
        assertEquals(Arrays.asList("invite", "overview"), repository.calls);
    }

    @Test
    public void REQ09_invalidEmailIsNotSent() {
        MembersViewModel vm = viewModel();

        assertFalse(vm.invite("not-an-email"));

        assertTrue(repository.calls.isEmpty());
        assertEquals(FieldError.EMAIL_INVALID, vm.state().getValue().inviteEmailError);
    }

    @Test
    public void REQ09_existingMemberErrorIsShown() {
        repository.inviteResult = Result.failure(AppError.ALREADY_MEMBER);
        MembersViewModel vm = viewModel();

        vm.invite("cleaner@example.com");

        assertEquals(AppError.ALREADY_MEMBER, vm.errors().getValue().consume());
    }

    @Test
    public void REQ10_cancelAndRemoveReloadTheList() {
        MembersViewModel vm = viewModel();

        vm.cancelInvitation(9);
        vm.removeMember(2);

        assertEquals(Arrays.asList("cancel:9", "overview", "remove:2", "overview"), repository.calls);
    }
}
