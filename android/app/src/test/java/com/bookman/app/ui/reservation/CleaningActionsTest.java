package com.bookman.app.ui.reservation;

import static org.junit.Assert.assertEquals;

import com.bookman.app.data.apartment.Role;
import com.bookman.app.data.reservation.CleaningStatus;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class CleaningActionsTest {

    @Test
    public void REQ08_cleaningServiceOnlyMovesForward() {
        assertEquals(Collections.singletonList(CleaningStatus.TAKEN),
                CleaningActions.targets(Role.CLEANING_SERVICE, CleaningStatus.NOT_PLANNED));
        assertEquals(Collections.singletonList(CleaningStatus.DONE),
                CleaningActions.targets(Role.CLEANING_SERVICE, CleaningStatus.TAKEN));
        assertEquals(Collections.emptyList(), CleaningActions.targets(Role.CLEANING_SERVICE, CleaningStatus.DONE));
    }

    @Test
    public void REQ08_adminCanChooseAnyOtherStatus() {
        assertEquals(Arrays.asList(CleaningStatus.NOT_PLANNED, CleaningStatus.TAKEN),
                CleaningActions.targets(Role.ADMIN, CleaningStatus.DONE));
        assertEquals(Arrays.asList(CleaningStatus.TAKEN, CleaningStatus.DONE),
                CleaningActions.targets(Role.ADMIN, CleaningStatus.NOT_PLANNED));
    }
}
