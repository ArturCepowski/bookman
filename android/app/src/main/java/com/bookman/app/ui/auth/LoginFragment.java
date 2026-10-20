package com.bookman.app.ui.auth;

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
import com.bookman.app.databinding.FragmentLoginBinding;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        LoginViewModel viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        binding.loginButton.setOnClickListener(v -> viewModel.login(
                text(binding.emailInput.getText()), text(binding.passwordInput.getText())));
        binding.goToRegister.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_login_to_register));

        viewModel.state().observe(getViewLifecycleOwner(), state -> {
            binding.progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
            binding.loginButton.setEnabled(!state.loading);
            binding.emailLayout.setError(state.emailError == null ? null : getString(state.emailError.message));
            binding.passwordLayout.setError(state.passwordError == null ? null : getString(state.passwordError.message));
        });
        viewModel.errors().observe(getViewLifecycleOwner(), event -> {
            AppError error = event.consume();
            if (error != null) {
                Snackbar.make(binding.getRoot(), ErrorMessages.of(error), Snackbar.LENGTH_LONG).show();
            }
        });
        viewModel.loggedIn().observe(getViewLifecycleOwner(), event -> {
            if (event.consume() != null) {
                NavHostFragment.findNavController(this).navigate(R.id.action_login_to_apartments);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static String text(@Nullable CharSequence s) {
        return s == null ? "" : s.toString();
    }
}
