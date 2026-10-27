package com.bookman.app.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.bookman.app.ui.common.FieldError;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.bookman.app.common.AppError;
import com.bookman.app.common.DeviceLanguage;
import com.bookman.app.common.Result;
import com.bookman.app.fakes.FakeAuthRepository;
import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;

public class RegisterViewModelTest {

    @Rule public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeAuthRepository repository = new FakeAuthRepository();

    @Test
    public void REQ01_successfulRegistrationNavigatesHomeWithNormalizedData() {
        RegisterViewModel vm = new RegisterViewModel(repository, new DeviceLanguage(Locale.forLanguageTag("pl-PL")));

        vm.register(" Anna@Example.com", "secret123", "+48 600 100 200");

        assertEquals("anna@example.com", repository.lastEmail);
        assertEquals("+48600100200", repository.lastPhone);
        assertEquals("PL", repository.lastLanguage);
        assertNotNull(vm.registered().getValue().consume());
        assertFalse(vm.state().getValue().loading);
    }

    @Test
    public void REQ01_invalidInputIsNotSentToBackend() {
        RegisterViewModel vm = new RegisterViewModel(repository, new DeviceLanguage(Locale.ENGLISH));

        vm.register("anna@example.com", "secret123", "600100200");

        assertEquals(0, repository.calls);
        assertEquals(FieldError.PHONE_INVALID, vm.state().getValue().phoneError);
        assertNull(vm.registered().getValue());
    }

    @Test
    public void REQ01_existingEmailShowsError() {
        repository.nextResult = Result.failure(AppError.EMAIL_TAKEN);
        RegisterViewModel vm = new RegisterViewModel(repository, new DeviceLanguage(Locale.ENGLISH));

        vm.register("anna@example.com", "secret123", "+48600100200");

        assertEquals(AppError.EMAIL_TAKEN, vm.errors().getValue().consume());
        assertNull(vm.registered().getValue());
    }

    @Test
    public void REQ01_registrationWithoutPhoneNumberIsSent() {
        RegisterViewModel vm = new RegisterViewModel(repository, new DeviceLanguage(Locale.ENGLISH));

        vm.register("anna@example.com", "secret123", "");

        assertEquals(1, repository.calls);
        assertNull(repository.lastPhone);
        assertNotNull(vm.registered().getValue().consume());
    }
}
