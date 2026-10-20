package com.bookman.reservation;

import static com.bookman.reservation.CleaningStatus.DONE;
import static com.bookman.reservation.CleaningStatus.NOT_PLANNED;
import static com.bookman.reservation.CleaningStatus.TAKEN;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookman.apartment.Role;
import com.bookman.common.ApiException;
import com.bookman.common.ErrorCode;
import org.junit.jupiter.api.Test;

class CleaningStatusPolicyTest {

    @Test
    void REQ08_cleaningServiceMovesOnlyForward() {
        assertThatCode(() -> CleaningStatusPolicy.check(Role.CLEANING_SERVICE, NOT_PLANNED, TAKEN)).doesNotThrowAnyException();
        assertThatCode(() -> CleaningStatusPolicy.check(Role.CLEANING_SERVICE, TAKEN, DONE)).doesNotThrowAnyException();
        for (CleaningStatus[] move : new CleaningStatus[][] {
                {NOT_PLANNED, DONE}, {TAKEN, NOT_PLANNED}, {DONE, TAKEN}, {DONE, NOT_PLANNED}}) {
            assertThatThrownBy(() -> CleaningStatusPolicy.check(Role.CLEANING_SERVICE, move[0], move[1]))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertCode(e, ErrorCode.FORBIDDEN));
        }
    }

    @Test
    void REQ08_adminMaySetAnyStatus() {
        for (CleaningStatus from : CleaningStatus.values()) {
            for (CleaningStatus to : CleaningStatus.values()) {
                if (from != to) {
                    assertThatCode(() -> CleaningStatusPolicy.check(Role.ADMIN, from, to)).doesNotThrowAnyException();
                }
            }
        }
    }

    @Test
    void REQ08_unchangedStatusIsAConflict() {
        assertThatThrownBy(() -> CleaningStatusPolicy.check(Role.CLEANING_SERVICE, TAKEN, TAKEN))
                .isInstanceOfSatisfying(ApiException.class, e -> assertCode(e, ErrorCode.STATUS_CONFLICT));
    }

    private static void assertCode(ApiException e, ErrorCode code) {
        org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo(code);
    }
}
