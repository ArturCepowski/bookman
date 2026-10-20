package com.bookman.app.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bookman.app.R;
import com.bookman.app.databinding.FragmentProfileBinding;
import com.bookman.app.ui.common.UiErrors;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ProfileViewModel viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        binding.saveButton.setOnClickListener(v -> viewModel.save(
                binding.phoneInput.getText() == null ? "" : binding.phoneInput.getText().toString(),
                binding.languagePolish.isChecked() ? "PL" : "EN"));

        viewModel.profile().observe(getViewLifecycleOwner(), user -> {
            binding.email.setText(user.email());
            binding.phoneInput.setText(user.phoneNumber());
            binding.languageGroup.check("PL".equals(user.language()) ? R.id.languagePolish : R.id.languageEnglish);
        });
        viewModel.loading().observe(getViewLifecycleOwner(), loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.saveButton.setEnabled(!busy);
        });
        viewModel.phoneError().observe(getViewLifecycleOwner(), error ->
                binding.phoneLayout.setError(error == null ? null : getString(error.message)));
        viewModel.errors().observe(getViewLifecycleOwner(), e -> UiErrors.show(this, e.consume()));
        viewModel.saved().observe(getViewLifecycleOwner(), event -> {
            if (event.consume() != null) {
                Snackbar.make(binding.getRoot(), R.string.profile_saved, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
