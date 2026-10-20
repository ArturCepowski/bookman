package com.bookman.app.ui.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.data.apartment.NotificationSettings;
import com.bookman.app.fakes.FakeApartmentRepository;
import com.bookman.app.ui.apartment.ApartmentFormViewModel;
import java.time.LocalTime;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;

public class NotificationSettingsViewModelTest {

    @Rule public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeApartmentRepository repository = new FakeApartmentRepository();

    private NotificationSettingsViewModel viewModel() {
        return new NotificationSettingsViewModel(
                new SavedStateHandle(Collections.singletonMap(ApartmentFormViewModel.ARG_APARTMENT_ID, 5L)), repository);
    }

    @Test
    public void REQ11_currentSettingsAreLoaded() {
        repository.settingsResult = Result.success(new NotificationSettings(LocalTime.of(18, 30), null));

        NotificationSettingsViewModel vm = viewModel();

        assertEquals(LocalTime.of(18, 30), vm.settings().getValue().notPlannedTime);
        assertNull(vm.settings().getValue().notDoneTime);
    }

    @Test
    public void REQ11_enablingAndChoosingHourIsSaved() {
        NotificationSettingsViewModel vm = viewModel();

        vm.setNotPlannedEnabled(true);
        vm.setNotPlannedTime(LocalTime.of(19, 15));
        vm.save();

        assertEquals(new NotificationSettings(LocalTime.of(19, 15), null), repository.lastSavedSettings);
        assertNotNull(vm.saved().getValue().consume());
    }

    @Test
    public void REQ12_reminderCanBeDisabled() {
        repository.settingsResult = Result.success(new NotificationSettings(LocalTime.of(18, 0), LocalTime.of(10, 0)));
        NotificationSettingsViewModel vm = viewModel();

        vm.setNotDoneEnabled(false);
        vm.save();

        assertEquals(new NotificationSettings(LocalTime.of(18, 0), null), repository.lastSavedSettings);
    }

    @Test
    public void REQ12_switchingOnSuggestsAnHourThatCanBeChanged() {
        NotificationSettingsViewModel vm = viewModel();

        vm.setNotDoneEnabled(true);

        assertEquals(NotificationSettingsViewModel.SUGGESTED_NOT_DONE, vm.settings().getValue().notDoneTime);
        assertEquals("10:00", NotificationSettingsViewModel.label(vm.settings().getValue().notDoneTime));
    }

    @Test
    public void REQ05_nonAdminRejectionIsShown() {
        repository.settingsResult = Result.failure(AppError.FORBIDDEN);

        NotificationSettingsViewModel vm = viewModel();

        assertEquals(AppError.FORBIDDEN, vm.errors().getValue().consume());
    }
}
