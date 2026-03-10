package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.InventoryHistoryDao;
import com.example.phonestore.data.dao.ProductDao;
import com.example.phonestore.data.dao.ReceiptDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.ui.admin.AdminProductsActivity;
import com.example.phonestore.ui.auth.WelcomeActivity;

import java.util.ArrayList;

public class AdminInventoryOverviewActivity extends BaseHomeActivity {

    private ProductDao productDao;
    private ReceiptDao receiptDao;
    private InventoryHistoryDao historyDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        productDao = new ProductDao(this);
        receiptDao = new ReceiptDao(this);
        historyDao = new InventoryHistoryDao(this);
    }

    @Override
    protected int shellLayoutRes() {
        return R.layout.activity_home_bottom_admin;
    }

    @Override
    protected int contentLayoutRes() {
        return R.layout.content_admin_inventory;
    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_admin;
    }

    @Override
    protected int selectedBottomNavItemId() {
        return R.id.nav_admin_inventory;
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.admin_inventory_overview);
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected void onShellReady() {
        if (productDao == null) return;

        ArrayList<Product> products = productDao.layTatCa();
        TextView tvInventoryProducts = findViewById(R.id.tvInventoryProducts);
        TextView tvInventoryUnits = findViewById(R.id.tvInventoryUnits);
        TextView tvInventoryLowStock = findViewById(R.id.tvInventoryLowStock);
        TextView tvInventoryOutOfStock = findViewById(R.id.tvInventoryOutOfStock);
        LinearLayout layoutInventoryLowStock = findViewById(R.id.layoutInventoryLowStock);

        int units = 0;
        int lowStock = 0;
        int outOfStock = 0;

        layoutInventoryLowStock.removeAllViews();

        for (Product product : products) {
            units += Math.max(0, product.tonKho);
            if (product.tonKho == 0) outOfStock++;
            if (product.tonKho > 0 && product.tonKho <= 5) {
                lowStock++;
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_admin_dashboard_entry, layoutInventoryLowStock, false);
                ((TextView) itemView.findViewById(R.id.tvTitle)).setText(product.tenSanPham);
                ((TextView) itemView.findViewById(R.id.tvSub)).setText(product.hang == null ? "" : product.hang);
                ((TextView) itemView.findViewById(R.id.tvMeta)).setText(getString(R.string.admin_stock_remaining, product.tonKho));
                itemView.setOnClickListener(v -> openBottomTab(new Intent(this, AdminProductsActivity.class)));
                layoutInventoryLowStock.addView(itemView);
            }
        }

        java.util.ArrayList<com.example.phonestore.data.model.Receipt> receipts = receiptDao.getRecentReceipts();
        java.util.ArrayList<com.example.phonestore.data.model.InventoryHistoryEntry> histories = historyDao.getRecent(10);

        TextView summaryReceipts = new TextView(this);
        summaryReceipts.setText(getString(R.string.inventory_overview_receipts, receipts.size()));
        summaryReceipts.setTextColor(getColor(R.color.admin_text_secondary));
        layoutInventoryLowStock.addView(summaryReceipts, 0);

        TextView summaryHistory = new TextView(this);
        summaryHistory.setText(getString(R.string.inventory_overview_history, histories.size()));
        summaryHistory.setTextColor(getColor(R.color.admin_text_secondary));
        layoutInventoryLowStock.addView(summaryHistory, 1);

        if (layoutInventoryLowStock.getChildCount() <= 2) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_admin_dashboard_entry, layoutInventoryLowStock, false);
            ((TextView) itemView.findViewById(R.id.tvTitle)).setText(getString(R.string.admin_no_low_stock));
            ((TextView) itemView.findViewById(R.id.tvSub)).setText("");
            ((TextView) itemView.findViewById(R.id.tvMeta)).setText(getString(R.string.admin_open_products));
            itemView.setOnClickListener(v -> openBottomTab(new Intent(this, AdminProductsActivity.class)));
            layoutInventoryLowStock.addView(itemView);
        }

        tvInventoryProducts.setText(String.valueOf(products.size()));
        tvInventoryUnits.setText(String.valueOf(units));
        tvInventoryLowStock.setText(String.valueOf(lowStock));
        tvInventoryOutOfStock.setText(String.valueOf(outOfStock));
    }
}
