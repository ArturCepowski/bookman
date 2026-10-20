package com.bookman.app.push;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.bookman.app.MainActivity;
import com.bookman.app.R;

/** The "reminders" notification channel; its id must match the backend's FCM channel_id. */
public final class ReminderNotifications {

    public static final String CHANNEL_ID = "reminders";

    private ReminderNotifications() {
    }

    public static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.notification_channel_reminders), NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.notification_channel_reminders_description));
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    public static boolean canNotify(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Shows a reminder that arrived while the app was in the foreground (FCM does not display those itself). */
    public static void show(Context context, String title, String body) {
        if (!canNotify(context)) {
            return;
        }
        PendingIntent open = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(open);
        try {
            NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), notification.build());
        } catch (SecurityException e) {
            // Permission revoked between the check and the call.
        }
    }
}
