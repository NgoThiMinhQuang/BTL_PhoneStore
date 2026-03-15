package com.example.phonestore.ui.home;

import android.content.Context;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.InventoryHistoryDao;
import com.example.phonestore.data.model.InventoryHistoryEntry;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.utils.InventoryPolicy;

import java.util.ArrayList;
import java.util.HashMap;

final class InventoryDataHelper {

    private InventoryDataHelper() {
    }

    static HashMap<Long, int[]> buildHistoryTotals(ArrayList<InventoryHistoryEntry> histories) {
        HashMap<Long, int[]> totalsByProduct = new HashMap<>();
        if (histories == null) {
            return totalsByProduct;
        }
        for (InventoryHistoryEntry entry : histories) {
            int[] totals = totalsByProduct.get(entry.productId);
            if (totals == null) {
                totals = new int[]{0, 0};
                totalsByProduct.put(entry.productId, totals);
            }
            if (InventoryHistoryDao.ACTION_IMPORT.equals(entry.actionType)
                    || InventoryHistoryDao.ACTION_CANCEL_RETURN.equals(entry.actionType)) {
                totals[0] += Math.max(0, entry.quantity);
            } else if (InventoryHistoryDao.ACTION_EXPORT.equals(entry.actionType)) {
                totals[1] += Math.max(0, entry.quantity);
            }
        }
        return totalsByProduct;
    }

    static InventoryManagementItem toInventoryItem(Context context, Product product, HashMap<Long, int[]> totalsByProduct) {
        int[] totals = totalsByProduct == null ? null : totalsByProduct.get(product.maSanPham);
        return new InventoryManagementItem(
                product.maSanPham,
                normalizeName(context, product.tenSanPham),
                normalizeBrand(context, product.hang),
                product.tenAnh,
                Math.max(0, product.tonKho),
                InventoryPolicy.LOW_STOCK_THRESHOLD,
                totals == null ? 0 : totals[0],
                totals == null ? 0 : totals[1],
                InventoryPolicy.resolveStatus(product.tonKho)
        );
    }

    static String normalizeName(Context context, String name) {
        return name == null || name.trim().isEmpty()
                ? context.getString(R.string.admin_product_unknown_name)
                : name.trim();
    }

    static String normalizeBrand(Context context, String brand) {
        return brand == null || brand.trim().isEmpty()
                ? context.getString(R.string.admin_product_unknown_brand)
                : brand.trim();
    }
}
