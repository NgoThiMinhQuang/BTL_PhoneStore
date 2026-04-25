package com.example.phonestore.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
        Supplier supplier = data.get(position);
        h.tvName.setText(supplier.name);
        h.tvCode.setText(h.itemView.getContext().getString(R.string.supplier_code_format, supplier.id));
        h.tvStatus.setText(R.string.supplier_status_active);
        bindOptionalText(h.layoutBrand, h.tvBrand, supplier.brand);
        bindOptionalText(h.layoutPhone, h.tvPhone, supplier.phone);
        bindOptionalText(h.layoutAddress, h.tvAddress, supplier.address);
        h.itemView.setOnClickListener(v -> listener.onClick(supplier));
        h.itemView.setOnLongClickListener(v -> {
            listener.onLongClick(supplier);
            return true;
        });
    }

    private void bindOptionalText(View container, TextView textView, String value) {
        String safeValue = value == null ? "" : value.trim();
        container.setVisibility(safeValue.isEmpty() ? View.GONE : View.VISIBLE);
        textView.setText(safeValue);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvCode, tvStatus, tvBrand, tvPhone, tvAddress;
        LinearLayout layoutBrand, layoutPhone, layoutAddress;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvBrand = itemView.findViewById(R.id.tvBrand);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            layoutBrand = itemView.findViewById(R.id.layoutBrand);
            layoutPhone = itemView.findViewById(R.id.layoutPhone);
            layoutAddress = itemView.findViewById(R.id.layoutAddress);
        }
    }
}
