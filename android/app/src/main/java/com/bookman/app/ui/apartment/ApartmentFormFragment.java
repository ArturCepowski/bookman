package com.bookman.app.ui.apartment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.bookman.app.R;
import com.bookman.app.common.AppError;
import com.bookman.app.databinding.FragmentApartmentFormBinding;
import com.bookman.app.ui.common.FieldError;
import com.bookman.app.ui.common.UiErrors;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AndroidEntryPoint
public class ApartmentFormFragment extends Fragment {

    private FragmentApartmentFormBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentApartmentFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ApartmentFormViewModel viewModel = new ViewModelProvider(this).get(ApartmentFormViewModel.class);

        List<String> zones = new ArrayList<>(ZoneId.getAvailableZoneIds());
        Collections.sort(zones);
        binding.timeZoneInput.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, zones));
        if (savedInstanceState == null && !viewModel.isEdit()) {
            binding.timeZoneInput.setText(ApartmentForm.DEFAULT_TIME_ZONE, false);
        }
        binding.saveButton.setText(viewModel.isEdit() ? R.string.action_save : R.string.action_register_apartment);
        binding.saveButton.setOnClickListener(v -> viewModel.save(
                text(binding.nameInput.getText()),
                text(binding.bookingUrlInput.getText()),
                text(binding.icalUrlInput.getText()),
                text(binding.timeZoneInput.getText())));

        viewModel.existing().observe(getViewLifecycleOwner(), d -> {
            if (savedInstanceState != null) {
                return;
            }
            binding.nameInput.setText(d.name);
            binding.bookingUrlInput.setText(d.bookingUrl);
            binding.icalUrlInput.setText(d.icalUrl);
            binding.timeZoneInput.setText(d.timeZone, false);
        });
        viewModel.state().observe(getViewLifecycleOwner(), state -> {
            binding.progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
            binding.saveButton.setEnabled(!state.loading);
            showError(binding.nameLayout, state.nameError);
            showError(binding.bookingUrlLayout, state.bookingUrlError);
            showError(binding.icalUrlLayout, state.icalUrlError);
            showError(binding.timeZoneLayout, state.timeZoneError);
        });
        viewModel.errors().observe(getViewLifecycleOwner(), event -> {
            AppError error = event.consume();
            if (error == AppError.ICAL_UNREACHABLE || error == AppError.ICAL_NOT_A_CALENDAR) {
                binding.icalUrlLayout.setError(getString(error == AppError.ICAL_UNREACHABLE
                        ? R.string.error_ical_unreachable : R.string.error_ical_not_a_calendar));
            } else {
                UiErrors.show(this, error);
            }
        });
        viewModel.saved().observe(getViewLifecycleOwner(), event -> {
            Long id = event.consume();
            if (id == null) {
                return;
            }
            if (viewModel.isEdit()) {
                NavHostFragment.findNavController(this).popBackStack();
            } else {
                Bundle args = new Bundle();
                args.putLong(ApartmentFormViewModel.ARG_APARTMENT_ID, id);
                NavHostFragment.findNavController(this).navigate(R.id.action_form_to_reservations, args,
                        new NavOptions.Builder().setPopUpTo(R.id.apartmentFormFragment, true).build());
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
