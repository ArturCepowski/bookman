package com.bookman.app.ui.reservation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.bookman.app.R;
import com.bookman.app.data.apartment.Role;
import com.bookman.app.data.reservation.CleaningStatus;
import com.bookman.app.data.reservation.Reservation;
import com.bookman.app.databinding.FragmentReservationListBinding;
import com.bookman.app.ui.apartment.ApartmentFormViewModel;
import com.bookman.app.ui.common.StatusLabels;
import com.bookman.app.ui.common.UiErrors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;

@AndroidEntryPoint
public class ReservationListFragment extends Fragment {

    private FragmentReservationListBinding binding;
    private ReservationListViewModel viewModel;
    @Nullable private Role shownRole;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentReservationListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        shownRole = null;
        viewModel = new ViewModelProvider(this).get(ReservationListViewModel.class);
        ReservationAdapter adapter = new ReservationAdapter(viewModel::actionsFor, this::onAction);
        binding.list.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(viewModel::refresh);

        viewModel.state().observe(getViewLifecycleOwner(), state -> {
            binding.swipeRefresh.setRefreshing(state.loading);
            boolean roleChanged = state.role != shownRole;
            shownRole = state.role;
            adapter.submitList(state.items, () -> {
                // Row buttons depend on the role; rebind unchanged rows once it is known.
                if (roleChanged) {
                    adapter.notifyItemRangeChanged(0, adapter.getItemCount());
                }
            });
            binding.emptyState.setVisibility(state.isEmpty() ? View.VISIBLE : View.GONE);
            ActionBar bar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
            if (bar != null && state.apartmentName != null) {
                bar.setTitle(state.apartmentName);
            }
            if (roleChanged) {
                // Admin-only menu entries depend on the role.
                requireActivity().invalidateMenu();
            }
        });
        viewModel.errors().observe(getViewLifecycleOwner(), e -> UiErrors.show(this, e.consume()));
        viewModel.deleted().observe(getViewLifecycleOwner(), event -> {
            if (event.consume() != null) {
                NavHostFragment.findNavController(this).popBackStack(R.id.apartmentListFragment, false);
            }
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.reservation_list, menu);
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                boolean admin = viewModel.canManage();
                menu.findItem(R.id.menu_members).setVisible(admin);
                menu.findItem(R.id.menu_reminders).setVisible(admin);
                menu.findItem(R.id.menu_delete).setVisible(admin);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_apartment_details) {
                    navigate(R.id.action_reservations_to_detail);
                    return true;
                }
                if (id == R.id.menu_members) {
                    navigate(R.id.action_reservations_to_members);
                    return true;
                }
                if (id == R.id.menu_reminders) {
                    navigate(R.id.action_reservations_to_reminders);
                    return true;
                }
                if (id == R.id.menu_delete) {
                    confirmDelete();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void navigate(int action) {
        Bundle args = new Bundle();
        args.putLong(ApartmentFormViewModel.ARG_APARTMENT_ID, viewModel.apartmentId());
        NavHostFragment.findNavController(this).navigate(action, args);
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_apartment_title)
                .setMessage(R.string.delete_apartment_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> viewModel.delete())
                .show();
    }

    private void onAction(Reservation reservation, List<CleaningStatus> targets) {
        if (targets.size() == 1) {
            viewModel.changeStatus(reservation.id, targets.get(0));
            return;
        }
        CharSequence[] labels = new CharSequence[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            labels[i] = getString(StatusLabels.of(targets.get(i)));
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.action_change_status)
                .setItems(labels, (dialog, which) -> viewModel.changeStatus(reservation.id, targets.get(which)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
