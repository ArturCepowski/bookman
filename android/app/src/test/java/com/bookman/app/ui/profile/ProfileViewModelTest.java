package com.bookman.app.ui.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.bookman.app.fakes.FakeAppLanguage;
import com.bookman.app.fakes.FakeProfileRepository;
import com.bookman.app.ui.common.FieldError;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

public class ProfileViewModelTest {

    @Rule public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeAppLanguage appLanguage = new FakeAppLanguage();
    private final FakeProfileRepository repository = new FakeProfileRepository();

    @Test
    public void REQ02_profileIsLoaded() {
        ProfileViewModel vm = new ProfileViewModel(repository, appLanguage);

        assertEquals("anna@example.com", vm.profile().getValue().email());
        assertEquals("+48600100200", vm.profile().getValue().phoneNumber());
    }

    @Test
    public void REQ02_phoneAndLanguageAreSavedOnBackend() {
        ProfileViewModel vm = new ProfileViewModel(repository, appLanguage);

        vm.save("+48 700 200 300", "PL");

        assertEquals("+48700200300", repository.lastPhone);
        assertEquals("PL", repository.lastLanguage);
        assertEquals("PL", vm.profile().getValue().language());
        assertNotNull(vm.saved().getValue().consume());
        assertNull(vm.phoneError().getValue());
    }

    @Test
    public void REQ02_invalidPhoneIsNotSaved() {
        ProfileViewModel vm = new ProfileViewModel(repository, appLanguage);

        vm.save("700200300", "EN");

        assertEquals(0, repository.updates);
        assertEquals(FieldError.PHONE_INVALID, vm.phoneError().getValue());
    }

    @Test
    public void REQ02_phoneNumberCanBeRemoved() {
        ProfileViewModel vm = new ProfileViewModel(repository, appLanguage);

        vm.save("", "EN");

        assertEquals(1, repository.updates);
        assertNull(repository.lastPhone);
        assertNull(vm.phoneError().getValue());
        assertNull(vm.profile().getValue().phoneNumber());
    }

    @Test
    public void REQ14_languageChangeInSettingsAppliesImmediately() {
        ProfileViewModel vm = new ProfileViewModel(repository, appLanguage);

        vm.save("+48600100200", "PL");

        assertEquals("PL", repository.lastLanguage);
        assertEquals(List.of("PL"), appLanguage.applied);
    }

    @Test
    public void REQ14_languageIsNotChangedWhenSavingFails() {
        ProfileViewModel vm = new ProfileViewModel(repository, appLanguage);
        repository.updateResult = Result.failure(AppError.NETWORK);

        vm.save("+48600100200", "PL");

        assertTrue(appLanguage.applied.isEmpty());
        assertEquals(AppError.NETWORK, vm.errors().getValue().consume());
    }
}
