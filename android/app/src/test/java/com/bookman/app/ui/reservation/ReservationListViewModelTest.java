package com.bookman.app.ui.reservation;

import static com.bookman.app.fakes.FakeReservationRepository.reservation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.data.apartment.Role;
import com.bookman.app.data.reservation.CleaningStatus;
import com.bookman.app.fakes.FakeApartmentRepository;
import com.bookman.app.fakes.FakeReservationRepository;
import com.bookman.app.ui.apartment.ApartmentFormViewModel;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;

public class ReservationListViewModelTest {

    @Rule public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeApartmentRepository apartments = new FakeApartmentRepository();
    private final FakeReservationRepository reservations = new FakeReservationRepository();

    private ReservationListViewModel viewModel() {
        return new ReservationListViewModel(
                new SavedStateHandle(Collections.singletonMap(ApartmentFormViewModel.ARG_APARTMENT_ID, 5L)),
                apartments, reservations);
    }

    @Test
    public void REQ07_showsReservationsAndApartmentName() {
        reservations.listResult = Result.success(Arrays.asList(
                reservation(1, CleaningStatus.TAKEN, "cleaner@example.com"),
                reservation(2, CleaningStatus.NOT_PLANNED, null)));
        ReservationListViewModel vm = viewModel();

        vm.load();

        ReservationListState state = vm.state().getValue();
        assertEquals(2, state.items.size());
        assertEquals("cleaner@example.com", state.items.get(0).takenBy);
        assertEquals("Sunny flat", state.apartmentName);
        assertEquals(Role.ADMIN, state.role);
        assertFalse(state.loading);
        assertFalse(state.isEmpty());
    }

    @Test
    public void REQ07_emptyStateAfterLoadingNothing() {
        ReservationListViewModel vm = viewModel();

        vm.load();

        assertTrue(vm.state().getValue().isEmpty());
    }

    @Test
    public void REQ06_pullToRefreshImportsThenReloads() {
        ReservationListViewModel vm = viewModel();
        vm.load();
        reservations.calls.clear();

        vm.refresh();

        assertEquals(Arrays.asList("import", "list"), reservations.calls);
    }

    @Test
    public void REQ06_failedImportStillShowsLastKnownList() {
        reservations.listResult = Result.success(Collections.singletonList(reservation(1, CleaningStatus.NOT_PLANNED, null)));
        reservations.importResult = Result.failure(AppError.IMPORT_FAILED);
        ReservationListViewModel vm = viewModel();
        vm.load();

        vm.refresh();

        assertEquals(AppError.IMPORT_FAILED, vm.errors().getValue().consume());
        assertEquals(1, vm.state().getValue().items.size());
        assertFalse(vm.state().getValue().loading);
    }

    @Test
    public void REQ08_takingCleaningUpdatesTheRow() {
        apartments.detailsResult = Result.success(FakeApartmentRepository.details(Role.CLEANING_SERVICE));
        reservations.listResult = Result.success(Collections.singletonList(reservation(1, CleaningStatus.NOT_PLANNED, null)));
        reservations.changeResult = Result.success(reservation(1, CleaningStatus.TAKEN, "cleaner@example.com"));
        ReservationListViewModel vm = viewModel();
        vm.load();

        assertEquals(Collections.singletonList(CleaningStatus.TAKEN), vm.actionsFor(vm.state().getValue().items.get(0)));
        vm.changeStatus(1, CleaningStatus.TAKEN);

        assertEquals(CleaningStatus.TAKEN, reservations.lastRequestedStatus);
        assertEquals(CleaningStatus.TAKEN, vm.state().getValue().items.get(0).cleaningStatus);
        assertEquals("cleaner@example.com", vm.state().getValue().items.get(0).takenBy);
    }

    @Test
    public void REQ08_conflictShowsErrorAndReloads() {
        reservations.listResult = Result.success(Collections.singletonList(reservation(1, CleaningStatus.NOT_PLANNED, null)));
        reservations.changeResult = Result.failure(AppError.STATUS_CONFLICT);
        ReservationListViewModel vm = viewModel();
        vm.load();
        reservations.calls.clear();

        vm.changeStatus(1, CleaningStatus.TAKEN);

        assertEquals(AppError.STATUS_CONFLICT, vm.errors().getValue().consume());
        assertEquals(Arrays.asList("change", "list"), reservations.calls);
    }

    @Test
    public void REQ08_noActionsUntilRoleIsKnown() {
        ReservationListViewModel vm = viewModel();

        assertTrue(vm.actionsFor(reservation(1, CleaningStatus.NOT_PLANNED, null)).isEmpty());
    }

    @Test
    public void REQ05_REQ10_adminGetsMembersRemindersAndDeleteInTheApartmentMenu() {
        ReservationListViewModel vm = viewModel();
        assertFalse(vm.canManage());

        vm.load();

        assertTrue(vm.canManage());
    }

    @Test
    public void REQ05_REQ10_cleaningServiceGetsOnlyDetails() {
        apartments.detailsResult = Result.success(FakeApartmentRepository.details(Role.CLEANING_SERVICE));
        ReservationListViewModel vm = viewModel();
        vm.load();

        assertFalse(vm.canManage());
        vm.delete();

        assertNull(apartments.lastDeletedId);
    }

    @Test
    public void REQ05_adminDeletesTheApartmentFromTheReservationScreen() {
        ReservationListViewModel vm = viewModel();
        vm.load();

        vm.delete();

        assertEquals(Long.valueOf(5), apartments.lastDeletedId);
        assertNotNull(vm.deleted().getValue().consume());
        assertFalse(vm.state().getValue().loading);
    }

    @Test
    public void REQ05_backendRejectionOfDeleteIsShown() {
        ReservationListViewModel vm = viewModel();
        vm.load();
        apartments.deleteResult = Result.failure(AppError.FORBIDDEN);

        vm.delete();

        assertEquals(AppError.FORBIDDEN, vm.errors().getValue().consume());
        assertNull(vm.deleted().getValue());
    }
}
