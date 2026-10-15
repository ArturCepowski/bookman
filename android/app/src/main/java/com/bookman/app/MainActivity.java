package com.bookman.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.bookman.app.data.auth.AuthRepository;
import com.bookman.app.databinding.ActivityMainBinding;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject AuthRepository authRepository;

    private NavController navController;

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
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
