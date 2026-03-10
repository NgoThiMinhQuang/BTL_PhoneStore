package com.example.phonestore.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.Supplier;

import java.util.ArrayList;

public class SupplierAdapter extends RecyclerView.Adapter<SupplierAdapter.VH> {

    interface Listener {
        void onClick(Supplier supplier);
        void onLongClick(Supplier supplier);
    }

    interface OnClick {
        void handle(Supplier supplier);
    }

    interface OnLongClick {
        void handle(Supplier supplier);
    }

    private final ArrayList<Supplier> data = new ArrayList<>();
    private final Listener listener;

    SupplierAdapter(Listener listener) {
        this.listener = listener;
    }

    SupplierAdapter(OnClick click, OnLongClick longClick) {
        this.listener = new Listener() {
            @Override
            public void onClick(Supplier supplier) {
                click.handle(supplier);
            }

            @Override
            public void onLongClick(Supplier supplier) {
                longClick.handle(supplier);
            }
        };
    }

    void setData(ArrayList<Supplier> list) {
        data.clear();
        data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_supplier, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Supplier s = data.get(position);
        h.tvName.setText(s.name);
        h.tvSub.setText((s.brand == null ? "" : s.brand) + (s.phone == null || s.phone.isEmpty() ? "" : " • " + s.phone));
        h.tvMeta.setText(s.address == null ? "" : s.address);
        h.itemView.setOnClickListener(v -> listener.onClick(s));
        h.itemView.setOnLongClickListener(v -> {
            listener.onLongClick(s);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSub, tvMeta;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvSub = itemView.findViewById(R.id.tvSub);
            tvMeta = itemView.findViewById(R.id.tvMeta);
        }
    }
}
