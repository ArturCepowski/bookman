package com.bookman.app.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.bookman.app.common.AppError;
import com.bookman.app.common.Result;
import com.bookman.app.fakes.FakeAuthRepository;
import org.junit.Rule;
import org.junit.Test;

public class HomeViewModelTest {

    @Rule public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private final FakeAuthRepository repository = new FakeAuthRepository();

    @Test
    public void REQ01_loggedInUserIsShown() {
        HomeViewModel vm = new HomeViewModel(repository);

        assertEquals("anna@example.com", vm.email().getValue());
    }

    @Test
    public void REQ01_expiredSessionLogsOut() {
        repository.loggedIn = true;
        repository.nextResult = Result.failure(AppError.SESSION_EXPIRED);

        HomeViewModel vm = new HomeViewModel(repository);

        assertFalse(repository.loggedIn);
        assertNotNull(vm.loggedOut().getValue().consume());
    }

    @Test
    public void REQ01_logoutEndsSession() {
        repository.loggedIn = true;
        HomeViewModel vm = new HomeViewModel(repository);

        vm.logout();

        assertFalse(repository.isLoggedIn());
        assertNotNull(vm.loggedOut().getValue().consume());
    }
}
