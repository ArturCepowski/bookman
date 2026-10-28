package com.bookman.app.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.fakes.FakeAppLanguage;
import com.bookman.app.fakes.FakeAuthRepository;
import com.bookman.app.data.auth.User;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

public class LoginViewModelTest {

    @Rule public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeAppLanguage appLanguage = new FakeAppLanguage();
    private final FakeAuthRepository repository = new FakeAuthRepository();

    @Test
    public void REQ01_successfulLoginNavigatesHome() {
        LoginViewModel vm = new LoginViewModel(repository, appLanguage);

        vm.login("ANNA@example.com ", "secret123");

        assertEquals("anna@example.com", repository.lastEmail);
        assertNotNull(vm.loggedIn().getValue().consume());
    }

    @Test
    public void REQ01_wrongCredentialsShowError() {
        repository.nextResult = Result.failure(AppError.INVALID_CREDENTIALS);
        LoginViewModel vm = new LoginViewModel(repository, appLanguage);

        vm.login("anna@example.com", "wrong");

        assertEquals(AppError.INVALID_CREDENTIALS, vm.errors().getValue().consume());
        assertNull(vm.loggedIn().getValue());
    }

    @Test
    public void REQ01_emptyFieldsAreRejectedLocally() {
        LoginViewModel vm = new LoginViewModel(repository, appLanguage);

        vm.login(" ", "");

        assertEquals(0, repository.calls);
        assertNotNull(vm.state().getValue().emailError);
        assertNotNull(vm.state().getValue().passwordError);
    }

    @Test
    public void REQ14_profileLanguageIsAppliedAfterLogin() {
        repository.nextResult = Result.success(new User(1, "anna@example.com", null, "PL"));
        LoginViewModel vm = new LoginViewModel(repository, appLanguage);

        vm.login("anna@example.com", "secret123");

        assertEquals(List.of("PL"), appLanguage.applied);
    }

    @Test
    public void REQ14_failedLoginKeepsTheLanguage() {
        repository.nextResult = Result.failure(AppError.INVALID_CREDENTIALS);
        LoginViewModel vm = new LoginViewModel(repository, appLanguage);

        vm.login("anna@example.com", "wrong");

        assertTrue(appLanguage.applied.isEmpty());
    }
}
