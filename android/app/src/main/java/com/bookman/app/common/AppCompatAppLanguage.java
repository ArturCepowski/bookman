package com.bookman.app.common;

import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Per-app language via AppCompat: Android 13+ stores it in the system settings, older versions through the
 * AppLocalesMetadataHolderService (autoStoreLocales) declared in the manifest.
 */
public class AppCompatAppLanguage implements AppLanguage {

    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public void apply(String code) {
        LocaleListCompat wanted = LocaleListCompat.forLanguageTags(AppLanguage.tagFor(code));
        // Callers run on background threads; changing locales recreates activities, so do it on the main thread.
        main.post(() -> {
            if (!wanted.equals(AppCompatDelegate.getApplicationLocales())) {
                AppCompatDelegate.setApplicationLocales(wanted);
            }
        });
    }
}
