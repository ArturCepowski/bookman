package com.bookman.app.ui.apartment;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bookman.app.data.apartment.ApartmentSummary;
import com.bookman.app.databinding.ItemApartmentBinding;
import com.bookman.app.ui.common.RoleLabels;

class ApartmentAdapter extends ListAdapter<ApartmentSummary, ApartmentAdapter.Holder> {

    private final Consumer<ApartmentSummary> onClick;

    ApartmentAdapter(Consumer<ApartmentSummary> onClick) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull ApartmentSummary a, @NonNull ApartmentSummary b) {
                return a.id == b.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull ApartmentSummary a, @NonNull ApartmentSummary b) {
                return a.name.equals(b.name) && a.role.equals(b.role);
            }
        });
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemApartmentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ApartmentSummary item = getItem(position);
        holder.binding.name.setText(item.name);
        holder.binding.role.setText(RoleLabels.of(item.role));
        holder.binding.getRoot().setOnClickListener(v -> onClick.accept(item));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemApartmentBinding binding;

        Holder(ItemApartmentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
