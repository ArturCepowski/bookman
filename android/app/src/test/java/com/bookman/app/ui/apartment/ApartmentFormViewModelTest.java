package com.bookman.app.ui.apartment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.fakes.FakeApartmentRepository;
import com.bookman.app.ui.common.FieldError;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;

public class ApartmentFormViewModelTest {

    @Rule public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private static final String LISTING = "https://www.booking.com/hotel/pl/sunny.html";
    private static final String ICAL = "https://ical.booking.com/v1/export?t=abc";

    private final FakeApartmentRepository repository = new FakeApartmentRepository();

    @Test
    public void REQ04_registerSendsFormAndReportsNewId() {
        ApartmentFormViewModel vm = new ApartmentFormViewModel(new SavedStateHandle(), repository);

        vm.save(" Sunny flat ", LISTING, ICAL, "");

        assertFalse(vm.isEdit());
        assertEquals(1, repository.creates);
        assertEquals("Sunny flat", repository.lastInput.name);
        assertEquals("Europe/Warsaw", repository.lastInput.timeZone);
        assertEquals(Long.valueOf(5), vm.saved().getValue().consume());
    }

    @Test
    public void REQ04_invalidFormIsNotSent() {
        ApartmentFormViewModel vm = new ApartmentFormViewModel(new SavedStateHandle(), repository);

        vm.save("", LISTING, "https://example.com/cal.ics", "");

        assertEquals(0, repository.creates);
        assertEquals(FieldError.NAME_INVALID, vm.state().getValue().nameError);
        assertEquals(FieldError.ICAL_URL_INVALID, vm.state().getValue().icalUrlError);
    }

    @Test
    public void REQ04_unreachableIcalIsShownOnTheIcalField() {
        repository.detailsResult = Result.failure(AppError.ICAL_UNREACHABLE);
        ApartmentFormViewModel vm = new ApartmentFormViewModel(new SavedStateHandle(), repository);

        vm.save("Sunny flat", LISTING, ICAL, "");

        assertEquals(FieldError.ICAL_URL_INVALID, vm.state().getValue().icalUrlError);
        assertEquals(AppError.ICAL_UNREACHABLE, vm.errors().getValue().consume());
        assertNull(vm.saved().getValue());
    }

    @Test
    public void REQ05_editPrefillsAndUpdatesExistingApartment() {
        ApartmentFormViewModel vm = new ApartmentFormViewModel(
                new SavedStateHandle(Collections.singletonMap(ApartmentFormViewModel.ARG_APARTMENT_ID, 5L)),
                repository);

        assertTrue(vm.isEdit());
        assertEquals("Sunny flat", vm.existing().getValue().name);

        vm.save("Renamed", LISTING, ICAL, "Europe/Berlin");

        assertEquals(Long.valueOf(5), repository.lastUpdatedId);
        assertEquals("Renamed", repository.lastInput.name);
        assertEquals(0, repository.creates);
    }
}
