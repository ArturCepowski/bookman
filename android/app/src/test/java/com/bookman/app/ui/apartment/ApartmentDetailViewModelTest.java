package com.bookman.app.ui.apartment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.bookman.app.common.Result;
import com.bookman.app.data.apartment.Role;
import com.bookman.app.fakes.FakeApartmentRepository;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;

public class ApartmentDetailViewModelTest {

    @Rule public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeApartmentRepository repository = new FakeApartmentRepository();

    private ApartmentDetailViewModel viewModel() {
        return new ApartmentDetailViewModel(
                new SavedStateHandle(Collections.singletonMap(ApartmentFormViewModel.ARG_APARTMENT_ID, 5L)),
                repository);
    }

    @Test
    public void REQ05_onlyAdminCanEdit() {
        ApartmentDetailViewModel vm = viewModel();
        vm.refresh();
        assertTrue(vm.canManage());

        repository.detailsResult = Result.success(FakeApartmentRepository.details(Role.CLEANING_SERVICE));
        vm.refresh();
        assertFalse(vm.canManage());
    }

    @Test
    public void NFR03_cleaningServiceDetailsHaveNoIcalUrl() {
        repository.detailsResult = Result.success(FakeApartmentRepository.details(Role.CLEANING_SERVICE));
        ApartmentDetailViewModel vm = viewModel();
        vm.refresh();

        assertNull(vm.details().getValue().icalUrl);
    }
}
