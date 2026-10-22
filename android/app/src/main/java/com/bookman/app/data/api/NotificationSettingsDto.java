package com.bookman.app.data.api;

/** "HH:MM" or null (= disabled). Gson omits nulls, which the backend reads as disabled. */
public class NotificationSettingsDto {
    public String notPlannedTime;
    public String notDoneTime;

    public NotificationSettingsDto() {
    }

    public NotificationSettingsDto(String notPlannedTime, String notDoneTime) {
        this.notPlannedTime = notPlannedTime;
        this.notDoneTime = notDoneTime;
    }
}
