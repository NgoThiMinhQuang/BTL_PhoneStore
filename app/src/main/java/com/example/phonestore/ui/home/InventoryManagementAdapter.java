package com.example.phonestore.ui.home;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;

import java.util.ArrayList;

public class InventoryManagementAdapter extends RecyclerView.Adapter<InventoryManagementAdapter.VH> {

    private final ArrayList<InventoryManagementItem> data = new ArrayList<>();

    void setData(ArrayList<InventoryManagementItem> list) {
        data.clear();
        data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory_management, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        InventoryManagementItem item = data.get(position);
        holder.tvName.setText(item.productName);
        holder.tvBrand.setText(item.brand);
        holder.tvCurrentStockValue.setText(holder.itemView.getContext().getString(R.string.admin_inventory_value_units, item.currentStock));
        holder.tvMinimumStockValue.setText(holder.itemView.getContext().getString(R.string.admin_inventory_value_units, item.minimumStock));
        holder.tvTotalImportValue.setText(holder.itemView.getContext().getString(R.string.admin_inventory_value_units, item.totalImport));
        holder.tvTotalExportValue.setText(holder.itemView.getContext().getString(R.string.admin_inventory_value_units, item.totalExport));
        bindStatus(holder.tvStatus, item.status);
    }

    private void bindStatus(TextView view, String status) {
        int textColor = R.color.admin_product_status_ok;
        int backgroundColor = R.color.admin_product_status_ok_bg;
        int labelRes = R.string.admin_product_status_in_stock;

        if (InventoryManagementItem.STATUS_LOW_STOCK.equals(status)) {
            textColor = R.color.admin_product_status_low;
            backgroundColor = R.color.admin_product_status_low_bg;
            labelRes = R.string.admin_product_status_low_stock;
        } else if (InventoryManagementItem.STATUS_OUT_OF_STOCK.equals(status)) {
            textColor = R.color.admin_product_status_empty;
            backgroundColor = R.color.admin_product_status_empty_bg;
            labelRes = R.string.admin_product_status_out_of_stock;
        }

        view.setText(labelRes);
        view.setTextColor(view.getContext().getColor(textColor));
        GradientDrawable background = (GradientDrawable) view.getBackground().mutate();
        background.setColor(view.getContext().getColor(backgroundColor));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvBrand;
        final TextView tvStatus;
        final TextView tvCurrentStockValue;
        final TextView tvMinimumStockValue;
        final TextView tvTotalImportValue;
        final TextView tvTotalExportValue;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvInventoryProductName);
            tvBrand = itemView.findViewById(R.id.tvInventoryBrand);
            tvStatus = itemView.findViewById(R.id.tvInventoryStatus);
            tvCurrentStockValue = itemView.findViewById(R.id.tvCurrentStockValue);
            tvMinimumStockValue = itemView.findViewById(R.id.tvMinimumStockValue);
            tvTotalImportValue = itemView.findViewById(R.id.tvTotalImportValue);
            tvTotalExportValue = itemView.findViewById(R.id.tvTotalExportValue);
        }
    }
}
