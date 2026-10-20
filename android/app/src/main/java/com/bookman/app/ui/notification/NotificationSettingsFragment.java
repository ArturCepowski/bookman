package com.bookman.app.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bookman.app.R;
import com.bookman.app.databinding.FragmentNotificationSettingsBinding;
import com.bookman.app.ui.common.UiErrors;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.LocalTime;

/** REQ-11 / REQ-12 reminder hours (admin). */
@AndroidEntryPoint
public class NotificationSettingsFragment extends Fragment {

    private FragmentNotificationSettingsBinding binding;
    private NotificationSettingsViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(NotificationSettingsViewModel.class);

        binding.notPlannedSwitch.setOnClickListener(v -> viewModel.setNotPlannedEnabled(binding.notPlannedSwitch.isChecked()));
        binding.notDoneSwitch.setOnClickListener(v -> viewModel.setNotDoneEnabled(binding.notDoneSwitch.isChecked()));
        binding.notPlannedTime.setOnClickListener(v ->
                pickTime(viewModel.settings().getValue().notPlannedTime, viewModel::setNotPlannedTime));
        binding.notDoneTime.setOnClickListener(v ->
                pickTime(viewModel.settings().getValue().notDoneTime, viewModel::setNotDoneTime));
        binding.saveButton.setOnClickListener(v -> viewModel.save());

        viewModel.settings().observe(getViewLifecycleOwner(), s -> {
            binding.notPlannedSwitch.setChecked(s.notPlannedTime != null);
            binding.notPlannedTime.setEnabled(s.notPlannedTime != null);
            binding.notPlannedTime.setText(s.notPlannedTime == null
                    ? getString(R.string.reminder_off) : NotificationSettingsViewModel.label(s.notPlannedTime));
            binding.notDoneSwitch.setChecked(s.notDoneTime != null);
            binding.notDoneTime.setEnabled(s.notDoneTime != null);
            binding.notDoneTime.setText(s.notDoneTime == null
                    ? getString(R.string.reminder_off) : NotificationSettingsViewModel.label(s.notDoneTime));
        });
        viewModel.loading().observe(getViewLifecycleOwner(), loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.saveButton.setEnabled(!busy);
        });
        viewModel.errors().observe(getViewLifecycleOwner(), e -> UiErrors.show(this, e.consume()));
        viewModel.saved().observe(getViewLifecycleOwner(), event -> {
            if (event.consume() != null) {
                Snackbar.make(binding.getRoot(), R.string.reminders_saved, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void pickTime(@Nullable LocalTime current, Consumer<LocalTime> onPicked) {
        LocalTime start = current == null ? LocalTime.of(12, 0) : current;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(start.getHour())
                .setMinute(start.getMinute())
                .build();
        picker.addOnPositiveButtonClickListener(v -> onPicked.accept(LocalTime.of(picker.getHour(), picker.getMinute())));
        picker.show(getChildFragmentManager(), "time");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
