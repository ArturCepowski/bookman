package com.bookman.app.ui.reservation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
    public void REQ08_privateIsFrozenForCleaningService() {
        assertEquals(Collections.emptyList(), CleaningActions.targets(Role.CLEANING_SERVICE, CleaningStatus.PRIVATE));
    }

    @Test
    public void REQ08_adminCanChooseAnyOtherStatus() {
        assertEquals(Arrays.asList(CleaningStatus.NOT_PLANNED, CleaningStatus.TAKEN, CleaningStatus.PRIVATE),
                CleaningActions.targets(Role.ADMIN, CleaningStatus.DONE));
        assertEquals(Arrays.asList(CleaningStatus.NOT_PLANNED, CleaningStatus.TAKEN, CleaningStatus.DONE),
                CleaningActions.targets(Role.ADMIN, CleaningStatus.PRIVATE));
    }

    @Test
    public void REQ08_cleaningServiceMayUndoOnlyItsOwnChange() {
        assertEquals(CleaningStatus.NOT_PLANNED, CleaningActions.undo(Role.CLEANING_SERVICE, CleaningStatus.TAKEN, true));
        assertEquals(CleaningStatus.TAKEN, CleaningActions.undo(Role.CLEANING_SERVICE, CleaningStatus.DONE, true));
        assertNull(CleaningActions.undo(Role.CLEANING_SERVICE, CleaningStatus.DONE, false));
        assertNull(CleaningActions.undo(Role.CLEANING_SERVICE, CleaningStatus.NOT_PLANNED, true));
        assertNull(CleaningActions.undo(Role.CLEANING_SERVICE, CleaningStatus.PRIVATE, true));
        // The admin picks any status from the list instead.
        assertNull(CleaningActions.undo(Role.ADMIN, CleaningStatus.DONE, true));
    }
}
