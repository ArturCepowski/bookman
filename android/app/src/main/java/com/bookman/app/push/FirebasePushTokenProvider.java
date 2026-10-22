package com.bookman.app.push;

import android.content.Context;
import android.util.Log;
import androidx.core.util.Consumer;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

public class FirebasePushTokenProvider implements PushTokenProvider {

    private static final String TAG = "Push";

    private final Context context;

    public FirebasePushTokenProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void currentToken(Consumer<String> callback) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            // Built without google-services.json: the app works, only reminders are not delivered.
            Log.i(TAG, "Firebase is not configured; push notifications are disabled in this build");
            callback.accept(null);
            return;
        }
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task ->
                callback.accept(task.isSuccessful() ? task.getResult() : null));
    }
}
