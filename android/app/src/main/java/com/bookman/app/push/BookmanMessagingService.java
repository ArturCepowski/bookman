package com.bookman.app.push;

import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

/** Receives FCM token changes and reminders shown while the app is open (Q-24). */
@AndroidEntryPoint
public class BookmanMessagingService extends FirebaseMessagingService {

    @Inject PushRegistrar registrar;

    @Override
    public void onNewToken(@NonNull String token) {
        registrar.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        RemoteMessage.Notification notification = message.getNotification();
        if (notification != null && notification.getTitle() != null && notification.getBody() != null) {
            ReminderNotifications.show(this, notification.getTitle(), notification.getBody());
        }
    }
}
