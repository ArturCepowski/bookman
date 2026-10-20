package com.bookman.app.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bookman.app.databinding.ItemActionRowBinding;
import com.bookman.app.databinding.ItemSectionHeaderBinding;
import java.util.ArrayList;
import java.util.List;

/** Simple list of section headers and rows with an optional action button. */
public class ActionRowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final class Row {
        final boolean header;
        final String title;
        @Nullable final String subtitle;
        @Nullable final String actionLabel;
        @Nullable final Runnable action;

        private Row(boolean header, String title, @Nullable String subtitle, @Nullable String actionLabel,
                @Nullable Runnable action) {
            this.header = header;
            this.title = title;
            this.subtitle = subtitle;
            this.actionLabel = actionLabel;
            this.action = action;
        }

        public static Row header(String title) {
            return new Row(true, title, null, null, null);
        }

        public static Row item(String title, @Nullable String subtitle, @Nullable String actionLabel,
                @Nullable Runnable action) {
            return new Row(false, title, subtitle, actionLabel, action);
        }
    }

    private static final int HEADER = 0;
    private static final int ITEM = 1;

    private final List<Row> rows = new ArrayList<>();

    public void setRows(List<Row> newRows) {
        int old = rows.size();
        rows.clear();
        notifyItemRangeRemoved(0, old);
        rows.addAll(newRows);
        notifyItemRangeInserted(0, rows.size());
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).header ? HEADER : ITEM;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == HEADER) {
            return new HeaderHolder(ItemSectionHeaderBinding.inflate(inflater, parent, false));
        }
        return new ItemHolder(ItemActionRowBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).binding.title.setText(row.title);
            return;
        }
        ItemActionRowBinding b = ((ItemHolder) holder).binding;
        b.title.setText(row.title);
        b.subtitle.setVisibility(row.subtitle == null ? View.GONE : View.VISIBLE);
        b.subtitle.setText(row.subtitle);
        b.action.setVisibility(row.action == null ? View.GONE : View.VISIBLE);
        b.action.setText(row.actionLabel);
        b.action.setOnClickListener(row.action == null ? null : v -> row.action.run());
    }

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        final ItemSectionHeaderBinding binding;

        HeaderHolder(ItemSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static final class ItemHolder extends RecyclerView.ViewHolder {
        final ItemActionRowBinding binding;

        ItemHolder(ItemActionRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
