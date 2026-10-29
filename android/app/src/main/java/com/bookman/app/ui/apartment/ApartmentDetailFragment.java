package com.bookman.app.ui.apartment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.bookman.app.R;
import com.bookman.app.databinding.FragmentApartmentDetailBinding;
import com.bookman.app.ui.common.RoleLabels;
import com.bookman.app.ui.common.UiErrors;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ApartmentDetailFragment extends Fragment {

    private FragmentApartmentDetailBinding binding;
    private ApartmentDetailViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentApartmentDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(ApartmentDetailViewModel.class);

        viewModel.details().observe(getViewLifecycleOwner(), d -> {
            binding.name.setText(d.name);
            binding.role.setText(RoleLabels.of(d.role));
            binding.bookingUrl.setText(d.bookingUrl);
            binding.timeZone.setText(d.timeZone);
            boolean showIcal = d.icalUrl != null;
            binding.icalLabel.setVisibility(showIcal ? View.VISIBLE : View.GONE);
            binding.icalUrl.setVisibility(showIcal ? View.VISIBLE : View.GONE);
            binding.icalUrl.setText(showIcal ? d.icalUrl : "");
            requireActivity().invalidateMenu();
        });
        viewModel.loading().observe(getViewLifecycleOwner(), loading ->
                binding.progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
        viewModel.errors().observe(getViewLifecycleOwner(), e -> UiErrors.show(this, e.consume()));

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.apartment_detail, menu);
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                // Members, reminders and delete are in the menu of the reservation screen.
                menu.findItem(R.id.menu_edit).setVisible(viewModel.canManage());
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_edit) {
                    Bundle args = new Bundle();
                    args.putLong(ApartmentFormViewModel.ARG_APARTMENT_ID, viewModel.apartmentId());
                    NavHostFragment.findNavController(ApartmentDetailFragment.this)
                            .navigate(R.id.action_detail_to_form, args);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
