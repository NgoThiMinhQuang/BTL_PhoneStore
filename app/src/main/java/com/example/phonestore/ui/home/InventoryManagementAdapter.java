package com.example.phonestore.ui.home;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Locale;

public class InventoryManagementAdapter extends RecyclerView.Adapter<InventoryManagementAdapter.VH> {

    interface Listener {
        void onCreateReceipt(InventoryManagementItem item);
    }

    private final ArrayList<InventoryManagementItem> data = new ArrayList<>();
    private final boolean alertMode;
    private final Listener listener;

    InventoryManagementAdapter() {
        this(false, null);
    }

    InventoryManagementAdapter(Listener listener) {
        this(true, listener);
    }

    private InventoryManagementAdapter(boolean alertMode, Listener listener) {
        this.alertMode = alertMode;
        this.listener = listener;
    }

    void setData(ArrayList<InventoryManagementItem> list) {
        data.clear();
        if (list != null) {
            data.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = alertMode ? R.layout.item_inventory_alert : R.layout.item_inventory_management;
        return new VH(LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        InventoryManagementItem item = data.get(position);
        Context context = holder.itemView.getContext();

        holder.tvName.setText(item.productName);
        holder.tvBrand.setText(item.brand);
        holder.tvCurrentStockValue.setText(context.getString(R.string.admin_inventory_value_units, item.currentStock));
        holder.tvMinimumStockValue.setText(context.getString(R.string.admin_inventory_value_units, item.minimumStock));
        bindStatus(context, holder, item.status, item.currentStock);

        if (alertMode) {
            if (holder.ivThumb != null) {
                holder.ivThumb.setImageResource(resolveProductImage(context, item));
            }
            if (holder.btnCreateReceipt != null) {
                holder.btnCreateReceipt.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCreateReceipt(item);
                    }
                });
            }
        } else {
            if (holder.tvTotalImportValue != null) {
                holder.tvTotalImportValue.setText(context.getString(R.string.admin_inventory_value_units, item.totalImport));
            }
            if (holder.tvTotalExportValue != null) {
                holder.tvTotalExportValue.setText(context.getString(R.string.admin_inventory_value_units, item.totalExport));
            }
        }
    }

    private void bindStatus(Context context, VH holder, String status, int currentStock) {
        int textRes = R.string.admin_product_status_in_stock;
        int textColorRes = R.color.admin_product_status_ok;
        int bgColorRes = R.color.admin_product_status_ok_bg;
        int stockColorRes = R.color.admin_text_primary;

        if (InventoryManagementItem.STATUS_LOW_STOCK.equals(status)) {
            textRes = R.string.admin_product_status_low_stock;
            textColorRes = R.color.admin_product_status_low;
            bgColorRes = R.color.admin_product_status_low_bg;
            stockColorRes = R.color.admin_product_status_low;
        } else if (InventoryManagementItem.STATUS_OUT_OF_STOCK.equals(status)) {
            textRes = R.string.admin_product_status_out_of_stock;
            textColorRes = R.color.admin_product_status_empty;
            bgColorRes = R.color.admin_product_status_empty_bg;
            stockColorRes = R.color.admin_product_status_empty;
        }

        holder.tvStatus.setText(textRes);
        if (alertMode) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        } else {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, textColorRes));
            Drawable background = holder.tvStatus.getBackground();
            if (background instanceof GradientDrawable) {
                ((GradientDrawable) background.mutate()).setColor(ContextCompat.getColor(context, bgColorRes));
            }
        }
        holder.tvCurrentStockValue.setTextColor(ContextCompat.getColor(context, stockColorRes));
        if (alertMode) {
            holder.tvCurrentStockValue.setContentDescription(context.getString(R.string.admin_inventory_current_stock_value, currentStock));
        }
    }

    private int resolveProductImage(Context context, InventoryManagementItem item) {
        int imageRes = findImageRes(context, item.imageName);
        if (imageRes != 0) return imageRes;

        String name = item.productName == null ? "" : item.productName.toLowerCase(Locale.ROOT);
        String brand = item.brand == null ? "" : item.brand.toLowerCase(Locale.ROOT);

        if (name.contains("iphone") || brand.contains("apple")) return R.drawable.ip_15;
        if (name.contains("s24") || brand.contains("samsung")) return findExistingSamsungImage(context);
        if (brand.contains("xiaomi")) return android.R.drawable.ic_menu_gallery;

        return android.R.drawable.ic_menu_gallery;
    }

    private int findImageRes(Context context, String imageName) {
        if (imageName == null || imageName.trim().isEmpty()) return 0;

        String imageKey = imageName.trim();
        int resId = context.getResources().getIdentifier(imageKey, "drawable", context.getPackageName());
        if (resId != 0) return resId;

        return context.getResources().getIdentifier(imageKey, "mipmap", context.getPackageName());
    }

    private int findExistingSamsungImage(Context context) {
        int imageRes = findImageRes(context, "ss_s24_ultra");
        if (imageRes != 0) return imageRes;
        imageRes = findImageRes(context, "ss_s24_utra");
        if (imageRes != 0) return imageRes;
        return android.R.drawable.ic_menu_gallery;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvBrand;
        TextView tvStatus;
        TextView tvCurrentStockValue;
        TextView tvMinimumStockValue;
        TextView tvTotalImportValue;
        TextView tvTotalExportValue;
        ImageView ivThumb;
        MaterialButton btnCreateReceipt;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvInventoryProductName);
            tvBrand = itemView.findViewById(R.id.tvInventoryBrand);
            tvStatus = itemView.findViewById(R.id.tvInventoryStatus);
            tvCurrentStockValue = itemView.findViewById(R.id.tvCurrentStockValue);
            tvMinimumStockValue = itemView.findViewById(R.id.tvMinimumStockValue);
            tvTotalImportValue = itemView.findViewById(R.id.tvTotalImportValue);
            tvTotalExportValue = itemView.findViewById(R.id.tvTotalExportValue);
            ivThumb = itemView.findViewById(R.id.ivInventoryThumb);
            btnCreateReceipt = itemView.findViewById(R.id.btnCreateReceipt);
        }
    }
}
