package com.bookman.app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.bookman.app.R;
import com.bookman.app.common.AppError;
import com.bookman.app.common.ErrorMessages;
import com.bookman.app.databinding.FragmentHomeBinding;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding.logoutButton.setOnClickListener(v -> viewModel.logout());
        viewModel.email().observe(getViewLifecycleOwner(), email ->
                binding.welcome.setText(getString(R.string.home_logged_in_as, email)));
        viewModel.errors().observe(getViewLifecycleOwner(), event -> {
            AppError error = event.consume();
            if (error != null) {
                Snackbar.make(binding.getRoot(), ErrorMessages.of(error), Snackbar.LENGTH_LONG).show();
            }
        });
        viewModel.loggedOut().observe(getViewLifecycleOwner(), event -> {
            if (event.consume() != null) {
                NavHostFragment.findNavController(this).navigate(R.id.action_home_to_login);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
