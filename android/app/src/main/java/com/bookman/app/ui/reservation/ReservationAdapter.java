package com.bookman.app.ui.reservation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bookman.app.R;
import com.bookman.app.data.reservation.CleaningStatus;
import com.bookman.app.data.reservation.Reservation;
import com.bookman.app.databinding.ItemReservationBinding;
import com.bookman.app.ui.common.Pills;
import com.bookman.app.ui.common.StatusLabels;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.function.Function;

class ReservationAdapter extends ListAdapter<Reservation, ReservationAdapter.Holder> {

    interface OnAction {
        void onAction(Reservation reservation, List<CleaningStatus> targets);
    }

    interface OnUndo {
        void onUndo(Reservation reservation, CleaningStatus target);
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private final Function<Reservation, List<CleaningStatus>> actions;
    private final OnAction onAction;
    private final Function<Reservation, CleaningStatus> undo;
    private final OnUndo onUndo;

    ReservationAdapter(Function<Reservation, List<CleaningStatus>> actions, OnAction onAction,
            Function<Reservation, CleaningStatus> undo, OnUndo onUndo) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull Reservation a, @NonNull Reservation b) {
                return a.id == b.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull Reservation a, @NonNull Reservation b) {
                return a.equals(b);
            }
        });
        this.actions = actions;
        this.onAction = onAction;
        this.undo = undo;
        this.onUndo = onUndo;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemReservationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Reservation item = getItem(position);
        ItemReservationBinding b = holder.binding;
        b.dates.setText(b.getRoot().getContext().getString(R.string.reservation_dates,
                DATE.format(item.checkIn), DATE.format(item.checkOut)));
        Pills.status(b.status, item.cleaningStatus);
        b.takenBy.setVisibility(item.takenBy == null ? View.GONE : View.VISIBLE);
        if (item.takenBy != null) {
            b.takenBy.setText(b.getRoot().getContext().getString(R.string.reservation_taken_by, item.takenBy));
        }

        List<CleaningStatus> targets = actions.apply(item);
        b.action.setVisibility(targets.isEmpty() ? View.GONE : View.VISIBLE);
        if (!targets.isEmpty()) {
            b.action.setText(targets.size() == 1
                    ? StatusLabels.actionTo(targets.get(0))
                    : R.string.action_change_status);
            b.action.setOnClickListener(v -> onAction.onAction(item, targets));
        }
        CleaningStatus back = undo.apply(item);
        b.undo.setVisibility(back == null ? View.GONE : View.VISIBLE);
        b.undo.setOnClickListener(back == null ? null : v -> onUndo.onUndo(item, back));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemReservationBinding binding;

        Holder(ItemReservationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
