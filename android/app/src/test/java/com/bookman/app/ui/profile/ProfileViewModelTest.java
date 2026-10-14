package com.bookman.app.ui.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.bookman.app.fakes.FakeProfileRepository;
import com.bookman.app.ui.common.FieldError;
import org.junit.Rule;
import org.junit.Test;

public class ProfileViewModelTest {

    @Rule public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeProfileRepository repository = new FakeProfileRepository();

    @Test
    public void REQ02_profileIsLoaded() {
        ProfileViewModel vm = new ProfileViewModel(repository);

        assertEquals("anna@example.com", vm.profile().getValue().email());
        assertEquals("+48600100200", vm.profile().getValue().phoneNumber());
    }

    @Test
    public void REQ02_phoneAndLanguageAreSavedOnBackend() {
        ProfileViewModel vm = new ProfileViewModel(repository);

        vm.save("+48 700 200 300", "PL");

        assertEquals("+48700200300", repository.lastPhone);
        assertEquals("PL", repository.lastLanguage);
        assertEquals("PL", vm.profile().getValue().language());
        assertNotNull(vm.saved().getValue().consume());
        assertNull(vm.phoneError().getValue());
    }

    @Test
    public void REQ02_invalidPhoneIsNotSaved() {
        ProfileViewModel vm = new ProfileViewModel(repository);

        vm.save("700200300", "EN");

        assertEquals(0, repository.updates);
        assertEquals(FieldError.PHONE_INVALID, vm.phoneError().getValue());
    }
}
