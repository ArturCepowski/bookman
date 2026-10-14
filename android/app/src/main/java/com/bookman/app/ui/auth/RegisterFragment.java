package com.bookman.app.ui.auth;

import com.bookman.app.ui.common.FieldError;

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
import com.bookman.app.databinding.FragmentRegisterBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RegisterViewModel viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        binding.registerButton.setOnClickListener(v -> viewModel.register(
                text(binding.emailInput.getText()),
                text(binding.passwordInput.getText()),
                text(binding.phoneInput.getText())));

        viewModel.state().observe(getViewLifecycleOwner(), state -> {
            binding.progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
            binding.registerButton.setEnabled(!state.loading);
            showError(binding.emailLayout, state.emailError);
            showError(binding.passwordLayout, state.passwordError);
            showError(binding.phoneLayout, state.phoneError);
        });
        viewModel.errors().observe(getViewLifecycleOwner(), event -> {
            AppError error = event.consume();
            if (error == AppError.EMAIL_TAKEN) {
                binding.emailLayout.setError(getString(ErrorMessages.of(error)));
            } else if (error != null) {
                Snackbar.make(binding.getRoot(), ErrorMessages.of(error), Snackbar.LENGTH_LONG).show();
            }
        });
        viewModel.registered().observe(getViewLifecycleOwner(), event -> {
            if (event.consume() != null) {
                NavHostFragment.findNavController(this).navigate(R.id.action_register_to_apartments);
            }
        });
    }

    private void showError(TextInputLayout layout, @Nullable FieldError error) {
        layout.setError(error == null ? null : getString(error.message));
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
