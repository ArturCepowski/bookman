package com.bookman.reservation;

import static com.bookman.reservation.CleaningStatus.DONE;
import static com.bookman.reservation.CleaningStatus.NOT_PLANNED;
import static com.bookman.reservation.CleaningStatus.PRIVATE;
import static com.bookman.reservation.CleaningStatus.TAKEN;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookman.apartment.Role;
import com.bookman.common.ApiException;
import com.bookman.common.ErrorCode;
import org.junit.jupiter.api.Test;

class CleaningStatusPolicyTest {

    @Test
    void REQ08_cleaningServiceMovesForwardButNotBackOverSomeoneElsesChange() {
        assertThatCode(() -> CleaningStatusPolicy.check(Role.CLEANING_SERVICE, NOT_PLANNED, TAKEN, false)).doesNotThrowAnyException();
        assertThatCode(() -> CleaningStatusPolicy.check(Role.CLEANING_SERVICE, TAKEN, DONE, false)).doesNotThrowAnyException();
        for (CleaningStatus[] move : new CleaningStatus[][] {
                {NOT_PLANNED, DONE}, {TAKEN, NOT_PLANNED}, {DONE, TAKEN}, {DONE, NOT_PLANNED}}) {
            assertThatThrownBy(() -> CleaningStatusPolicy.check(Role.CLEANING_SERVICE, move[0], move[1], false))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertCode(e, ErrorCode.FORBIDDEN));
        }
    }

    @Test
    void REQ08_adminMaySetAnyStatus() {
        for (CleaningStatus from : CleaningStatus.values()) {
            for (CleaningStatus to : CleaningStatus.values()) {
                if (from != to) {
                    assertThatCode(() -> CleaningStatusPolicy.check(Role.ADMIN, from, to, false)).doesNotThrowAnyException();
                }
            }
        }
    }

    @Test
    void REQ08_onlyAdminSetsOrLeavesPrivate() {
        assertThatCode(() -> CleaningStatusPolicy.check(Role.ADMIN, NOT_PLANNED, PRIVATE, false)).doesNotThrowAnyException();
        assertThatCode(() -> CleaningStatusPolicy.check(Role.ADMIN, PRIVATE, NOT_PLANNED, false)).doesNotThrowAnyException();
        for (CleaningStatus from : new CleaningStatus[] {NOT_PLANNED, TAKEN, DONE}) {
            assertThatThrownBy(() -> CleaningStatusPolicy.check(Role.CLEANING_SERVICE, from, PRIVATE, false))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertCode(e, ErrorCode.FORBIDDEN));
        }
        for (CleaningStatus to : new CleaningStatus[] {NOT_PLANNED, TAKEN, DONE}) {
            assertThatThrownBy(() -> CleaningStatusPolicy.check(Role.CLEANING_SERVICE, PRIVATE, to, false))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertCode(e, ErrorCode.FORBIDDEN));
        }
    }

    @Test
    void REQ08_unchangedStatusIsAConflict() {
        assertThatThrownBy(() -> CleaningStatusPolicy.check(Role.CLEANING_SERVICE, TAKEN, TAKEN, false))
                .isInstanceOfSatisfying(ApiException.class, e -> assertCode(e, ErrorCode.STATUS_CONFLICT));
    }

    @Test
    void REQ08_cleaningServiceMayUndoOnlyAStatusItSetItself() {
        assertThatCode(() -> CleaningStatusPolicy.check(Role.CLEANING_SERVICE, TAKEN, NOT_PLANNED, true))
                .doesNotThrowAnyException();
        assertThatCode(() -> CleaningStatusPolicy.check(Role.CLEANING_SERVICE, DONE, TAKEN, true))
                .doesNotThrowAnyException();
        // Undo is one step back only, and PRIVATE stays frozen.
        for (CleaningStatus[] move : new CleaningStatus[][] {{DONE, NOT_PLANNED}, {PRIVATE, NOT_PLANNED}}) {
            assertThatThrownBy(() -> CleaningStatusPolicy.check(Role.CLEANING_SERVICE, move[0], move[1], true))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertCode(e, ErrorCode.FORBIDDEN));
        }
    }

    private static void assertCode(ApiException e, ErrorCode code) {
        org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo(code);
    }
}
