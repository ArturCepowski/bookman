package com.bookman.app.ui.apartment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.data.apartment.ApartmentSummary;
import com.bookman.app.data.apartment.Role;
import com.bookman.app.fakes.FakeApartmentRepository;
import com.bookman.app.fakes.FakeAuthRepository;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;

public class ApartmentListViewModelTest {

    @Rule public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeApartmentRepository apartments = new FakeApartmentRepository();
    private final FakeAuthRepository auth = new FakeAuthRepository();

    @Test
    public void REQ03_showsApartmentsWithRoles() {
        apartments.listResult = Result.success(Arrays.asList(
                new ApartmentSummary(1, "Sunny flat", Role.ADMIN),
                new ApartmentSummary(2, "Loft", Role.CLEANING_SERVICE)));
        ApartmentListViewModel vm = new ApartmentListViewModel(apartments, auth);

        vm.refresh();

        ApartmentListState state = vm.state().getValue();
        assertEquals(2, state.items.size());
        assertEquals(Role.CLEANING_SERVICE, state.items.get(1).role);
        assertFalse(state.isEmpty());
        assertFalse(state.loading);
    }

    @Test
    public void REQ03_emptyStateOnlyAfterSuccessfulEmptyLoad() {
        ApartmentListViewModel vm = new ApartmentListViewModel(apartments, auth);
        assertFalse("not before loading", vm.state().getValue().isEmpty());

        vm.refresh();

        assertTrue(vm.state().getValue().isEmpty());
    }

    @Test
    public void REQ03_failedLoadDoesNotShowEmptyState() {
        apartments.listResult = Result.failure(AppError.NETWORK);
        ApartmentListViewModel vm = new ApartmentListViewModel(apartments, auth);

        vm.refresh();

        assertFalse(vm.state().getValue().isEmpty());
        assertEquals(AppError.NETWORK, vm.errors().getValue().consume());
    }

    @Test
    public void REQ01_expiredSessionIsReported() {
        apartments.listResult = Result.failure(AppError.SESSION_EXPIRED);
        ApartmentListViewModel vm = new ApartmentListViewModel(apartments, auth);

        vm.refresh();

        assertEquals(AppError.SESSION_EXPIRED, vm.errors().getValue().consume());
    }

    @Test
    public void REQ01_logoutEndsSession() {
        auth.loggedIn = true;
        ApartmentListViewModel vm = new ApartmentListViewModel(apartments, auth);

        vm.logout();

        assertFalse(auth.isLoggedIn());
        assertNotNull(vm.loggedOut().getValue().consume());
    }
}
