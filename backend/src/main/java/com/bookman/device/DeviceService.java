package com.bookman.device;

import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {

    private final DeviceTokenRepository devices;
    private final Clock clock;

    public DeviceService(DeviceTokenRepository devices, Clock clock) {
        this.devices = devices;
        this.clock = clock;
    }

    /** Called after login and whenever FCM rotates the token. A phone receives only its current user's reminders. */
    @Transactional
    public void register(long userId, String token) {
        devices.findByToken(token).ifPresentOrElse(
                existing -> existing.assignTo(userId, clock.instant()),
                () -> devices.save(new DeviceToken(userId, token, clock.instant())));
    }

    /** Called on logout, so the next person using the phone does not get this user's reminders. */
    @Transactional
    public void unregister(long userId, String token) {
        devices.deleteForUser(token, userId);
    }

    @Transactional(readOnly = true)
    public List<String> tokensOf(long userId) {
        return devices.findByUserId(userId).stream().map(DeviceToken::getToken).toList();
    }

    /** FCM said the token is no longer valid (app uninstalled, data cleared). */
    @Transactional
    public void forget(String token) {
        devices.deleteByToken(token);
    }
}
