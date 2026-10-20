package com.bookman.app.ui.member;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bookman.app.R;
import com.bookman.app.data.apartment.ApartmentSummary;
import com.bookman.app.data.member.MyInvitation;
import com.bookman.app.databinding.FragmentInvitationsBinding;
import com.bookman.app.databinding.ItemInvitationBinding;
import com.bookman.app.ui.common.UiErrors;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

/** REQ-09: pending invitations of the logged-in user. */
@AndroidEntryPoint
public class InvitationsFragment extends Fragment {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private FragmentInvitationsBinding binding;
    private InvitationsViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentInvitationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(InvitationsViewModel.class);

        viewModel.invitations().observe(getViewLifecycleOwner(), this::show);
        viewModel.loading().observe(getViewLifecycleOwner(), loading ->
                binding.progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
        viewModel.errors().observe(getViewLifecycleOwner(), e -> UiErrors.show(this, e.consume()));
        viewModel.accepted().observe(getViewLifecycleOwner(), event -> {
            ApartmentSummary apartment = event.consume();
            if (apartment != null) {
                Snackbar.make(binding.getRoot(), getString(R.string.accepted_message, apartment.name),
                        Snackbar.LENGTH_LONG).show();
            }
        });
        if (savedInstanceState == null) {
            viewModel.load();
        }
    }

    private void show(List<MyInvitation> invitations) {
        binding.items.removeAllViews();
        binding.emptyState.setVisibility(invitations.isEmpty() ? View.VISIBLE : View.GONE);
        for (MyInvitation invitation : invitations) {
            ItemInvitationBinding item = ItemInvitationBinding.inflate(getLayoutInflater(), binding.items, true);
            item.apartmentName.setText(invitation.apartmentName);
            String validUntil = DATE.format(invitation.expiresAt.atZone(ZoneId.systemDefault()));
            item.details.setText(invitation.invitedBy == null
                    ? getString(R.string.invitation_valid_until, validUntil)
                    : getString(R.string.invitation_from, invitation.invitedBy, validUntil));
            item.accept.setOnClickListener(v -> viewModel.accept(invitation.id));
            item.decline.setOnClickListener(v -> viewModel.decline(invitation.id));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
