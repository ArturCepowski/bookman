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
import com.bookman.app.databinding.FragmentApartmentListBinding;
import com.bookman.app.ui.common.UiErrors;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ApartmentListFragment extends Fragment {

    private FragmentApartmentListBinding binding;
    private ApartmentListViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentApartmentListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(ApartmentListViewModel.class);
        ApartmentAdapter adapter = new ApartmentAdapter(item -> open(R.id.action_apartments_to_reservations, item.id));
        binding.list.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(viewModel::refresh);
        binding.addApartment.setOnClickListener(v ->
                open(R.id.action_apartments_to_form, ApartmentFormViewModel.NEW));

        viewModel.state().observe(getViewLifecycleOwner(), state -> {
            binding.swipeRefresh.setRefreshing(state.loading);
            adapter.submitList(state.items);
            binding.emptyState.setVisibility(state.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.errors().observe(getViewLifecycleOwner(), e -> UiErrors.show(this, e.consume()));
        viewModel.loggedOut().observe(getViewLifecycleOwner(), event -> {
            if (event.consume() != null) {
                NavHostFragment.findNavController(this).navigate(R.id.action_global_login);
            }
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.apartment_list, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_profile) {
                    NavHostFragment.findNavController(ApartmentListFragment.this)
                            .navigate(R.id.action_apartments_to_profile);
                    return true;
                }
                if (item.getItemId() == R.id.menu_logout) {
                    viewModel.logout();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload when coming back from register/edit/delete.
        viewModel.refresh();
    }

    private void open(int action, long apartmentId) {
        Bundle args = new Bundle();
        args.putLong(ApartmentFormViewModel.ARG_APARTMENT_ID, apartmentId);
        NavHostFragment.findNavController(this).navigate(action, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
