package com.bookman.app.data.apartment;

import androidx.annotation.Nullable;
import java.time.LocalTime;
import java.util.Objects;

/** REQ-11 / REQ-12 reminder hours in the apartment's time zone; null = disabled. */
public final class NotificationSettings {

    @Nullable public final LocalTime notPlannedTime;
    @Nullable public final LocalTime notDoneTime;

    public NotificationSettings(@Nullable LocalTime notPlannedTime, @Nullable LocalTime notDoneTime) {
        this.notPlannedTime = notPlannedTime;
        this.notDoneTime = notDoneTime;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NotificationSettings)) {
            return false;
        }
        NotificationSettings other = (NotificationSettings) o;
        return Objects.equals(notPlannedTime, other.notPlannedTime) && Objects.equals(notDoneTime, other.notDoneTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notPlannedTime, notDoneTime);
    }
}
