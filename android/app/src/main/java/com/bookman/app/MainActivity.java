package com.bookman.app;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.bookman.app.data.auth.AuthRepository;
import com.bookman.app.databinding.ActivityMainBinding;
import com.bookman.app.push.PushRegistrar;
import com.bookman.app.push.ReminderNotifications;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject AuthRepository authRepository;
    @Inject PushRegistrar pushRegistrar;

    // Q-24: reminders arrive as notifications; Android 13+ asks the user once.
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    private NavController navController;
    private boolean askedForNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        navController = host.getNavController();
        // REQ-01: a stored session skips the login screen until logout or expiry.
        // setGraph on every onCreate is required; after recreation the saved back stack takes precedence.
        NavGraph graph = navController.getNavInflater().inflate(R.navigation.nav_graph);
        graph.setStartDestination(authRepository.isLoggedIn() ? R.id.apartmentListFragment : R.id.loginFragment);
        navController.setGraph(graph);
        AppBarConfiguration appBar = new AppBarConfiguration.Builder(R.id.loginFragment, R.id.apartmentListFragment).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBar);

        // REQ-14: the activity context carries the in-app language, so the channel name follows a language change.
        ReminderNotifications.createChannel(this);
        if (authRepository.isLoggedIn()) {
            pushRegistrar.registerCurrentDevice();
        }
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.apartmentListFragment) {
                askForNotificationPermissionOnce();
            }
        });
    }

    private void askForNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderNotifications.canNotify(this)
                && !askedForNotifications) {
            askedForNotifications = true;
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
