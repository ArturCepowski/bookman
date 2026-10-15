package com.bookman.app.ui.common;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bookman.app.R;
import com.bookman.app.common.AppError;
import com.bookman.app.common.ErrorMessages;
import com.google.android.material.snackbar.Snackbar;

public final class UiErrors {

    private UiErrors() {
    }

    /** Shows the error; an expired session (REQ-01) sends the user back to the login screen. */
    public static void show(Fragment fragment, @Nullable AppError error) {
        if (error == null || fragment.getView() == null) {
            return;
        }
        Snackbar.make(fragment.requireView(), ErrorMessages.of(error), Snackbar.LENGTH_LONG).show();
        if (error == AppError.SESSION_EXPIRED) {
            NavHostFragment.findNavController(fragment).navigate(R.id.action_global_login);
        }
    }
}
