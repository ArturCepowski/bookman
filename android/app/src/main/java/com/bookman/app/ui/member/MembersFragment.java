package com.bookman.app.ui.member;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bookman.app.R;
import com.bookman.app.data.apartment.Role;
import com.bookman.app.data.member.Member;
import com.bookman.app.data.member.MembersOverview;
import com.bookman.app.data.member.PendingInvitation;
import com.bookman.app.databinding.DialogInviteBinding;
import com.bookman.app.databinding.FragmentMembersBinding;
import com.bookman.app.ui.common.ActionRowAdapter;
import com.bookman.app.ui.common.ActionRowAdapter.Row;
import com.bookman.app.ui.common.RoleLabels;
import com.bookman.app.ui.common.UiErrors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

/** REQ-10 (admin only; the backend rejects everyone else with 403). */
@AndroidEntryPoint
public class MembersFragment extends Fragment {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private FragmentMembersBinding binding;
    private MembersViewModel viewModel;
    private final ActionRowAdapter adapter = new ActionRowAdapter();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMembersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(MembersViewModel.class);
        binding.list.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(viewModel::load);
        binding.invite.setOnClickListener(v -> showInviteDialog());

        viewModel.state().observe(getViewLifecycleOwner(), state -> {
            binding.swipeRefresh.setRefreshing(state.loading);
            adapter.setRows(rows(state.overview));
        });
        viewModel.errors().observe(getViewLifecycleOwner(), e -> UiErrors.show(this, e.consume()));
        viewModel.invited().observe(getViewLifecycleOwner(), event -> {
            String email = event.consume();
            if (email != null) {
                Snackbar.make(binding.getRoot(), getString(R.string.invited_message, email), Snackbar.LENGTH_LONG).show();
            }
        });
        if (savedInstanceState == null) {
            viewModel.load();
        }
    }

    private List<Row> rows(MembersOverview overview) {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.header(getString(R.string.members_header)));
        for (Member member : overview.members) {
            boolean removable = member.role == Role.CLEANING_SERVICE;
            rows.add(Row.item(member.email, getString(RoleLabels.of(member.role)),
                    removable ? getString(R.string.action_remove_member) : null,
                    removable ? () -> confirmRemove(member) : null));
        }
        if (!overview.invitations.isEmpty()) {
            rows.add(Row.header(getString(R.string.invitations_header)));
            for (PendingInvitation invitation : overview.invitations) {
                String validUntil = DATE.format(invitation.expiresAt.atZone(ZoneId.systemDefault()));
                rows.add(Row.item(invitation.email, getString(R.string.invitation_valid_until, validUntil),
                        getString(R.string.action_cancel_invitation),
                        () -> viewModel.cancelInvitation(invitation.id)));
            }
        }
        return rows;
    }

    private void confirmRemove(Member member) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.remove_member_title, member.email))
                .setMessage(R.string.remove_member_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_remove_member, (d, w) -> viewModel.removeMember(member.userId))
                .show();
    }

    private void showInviteDialog() {
        DialogInviteBinding dialogBinding = DialogInviteBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.invite_dialog_title)
                .setMessage(R.string.invite_dialog_message)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_send_invitation, null)
                .show();
        // Keep the dialog open while the e-mail is invalid.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            CharSequence text = dialogBinding.emailInput.getText();
            if (viewModel.invite(text == null ? "" : text.toString())) {
                dialog.dismiss();
            } else {
                dialogBinding.emailLayout.setError(getString(R.string.field_error_email));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
