package com.example.phonestore.utils;

import android.content.Context;
import android.content.res.ColorStateList;

import androidx.core.content.ContextCompat;

import com.example.phonestore.R;
import com.example.phonestore.data.model.Product;

public final class InventoryPolicy {

    public static final int LOW_STOCK_THRESHOLD = 10;
    public static final String STATUS_IN_STOCK = "IN_STOCK";
    public static final String STATUS_LOW_STOCK = "LOW_STOCK";
    public static final String STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";

    private InventoryPolicy() {
    }

    public static String resolveStatus(int stock) {
        if (stock <= 0) {
            return STATUS_OUT_OF_STOCK;
        }
        if (stock <= LOW_STOCK_THRESHOLD) {
            return STATUS_LOW_STOCK;
        }
        return STATUS_IN_STOCK;
    }

    public static boolean isLowStock(int stock) {
        return STATUS_LOW_STOCK.equals(resolveStatus(stock));
    }

    public static boolean isLowStock(Product product) {
        return product != null && isLowStock(product.tonKho);
    }

    public static boolean isOutOfStock(int stock) {
        return STATUS_OUT_OF_STOCK.equals(resolveStatus(stock));
    }

    public static boolean isInStock(int stock) {
        return STATUS_IN_STOCK.equals(resolveStatus(stock));
    }

    public static StatusAppearance getAppearance(Context context, int stock) {
        String status = resolveStatus(stock);
        if (STATUS_OUT_OF_STOCK.equals(status)) {
            return new StatusAppearance(
                    status,
                    context.getString(R.string.admin_product_status_out_of_stock),
                    ContextCompat.getColor(context, R.color.admin_product_status_empty),
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.admin_product_status_empty_bg)),
                    ContextCompat.getColor(context, R.color.admin_product_status_empty)
            );
        }
        if (STATUS_LOW_STOCK.equals(status)) {
            return new StatusAppearance(
                    status,
                    context.getString(R.string.admin_product_status_low_stock),
                    ContextCompat.getColor(context, R.color.admin_product_status_low),
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.admin_product_status_low_bg)),
                    ContextCompat.getColor(context, R.color.admin_product_status_low)
            );
        }
        return new StatusAppearance(
                status,
                context.getString(R.string.admin_product_status_in_stock),
                ContextCompat.getColor(context, R.color.admin_product_status_ok),
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.admin_product_status_ok_bg)),
                ContextCompat.getColor(context, R.color.admin_text_primary)
        );
    }

    public static String getCustomerStockText(Context context, int stock) {
        String status = resolveStatus(stock);
        if (STATUS_OUT_OF_STOCK.equals(status)) {
            return context.getString(R.string.product_stock_out);
        }
        if (STATUS_LOW_STOCK.equals(status)) {
            return context.getString(R.string.product_stock_low, Math.max(0, stock));
        }
        return context.getString(R.string.product_stock_ok, Math.max(0, stock));
    }

    public static int getLowStockThreshold() {
        return LOW_STOCK_THRESHOLD;
    }

    public static final class StatusAppearance {
        public final String status;
        public final String label;
        public final int labelColor;
        public final ColorStateList labelBackgroundTint;
        public final int stockColor;

        StatusAppearance(String status, String label, int labelColor, ColorStateList labelBackgroundTint, int stockColor) {
            this.status = status;
            this.label = label;
            this.labelColor = labelColor;
            this.labelBackgroundTint = labelBackgroundTint;
            this.stockColor = stockColor;
        }
    }
}
