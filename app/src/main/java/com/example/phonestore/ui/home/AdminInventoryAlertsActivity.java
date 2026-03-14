package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.InventoryHistoryDao;
import com.example.phonestore.data.dao.ProductDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.InventoryHistoryEntry;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.ui.auth.WelcomeActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class AdminInventoryAlertsActivity extends BaseHomeActivity {

    private static final String FILTER_OUT = InventoryManagementItem.STATUS_OUT_OF_STOCK;
    private static final String FILTER_LOW = InventoryManagementItem.STATUS_LOW_STOCK;
    private static final int MINIMUM_STOCK = 20;

    private ProductDao productDao;
    private InventoryHistoryDao historyDao;
    private InventoryManagementAdapter adapter;

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
        historyDao = new InventoryHistoryDao(this);
    }

    @Override
    protected int shellLayoutRes() {
        return R.layout.activity_admin_inventory_alerts;
    }

    @Override
    protected int contentLayoutRes() {
        return 0;
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
        return getString(R.string.admin_alerts_title);
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected boolean shouldShowBackButton() {
        return true;
    }

    @Override
    protected boolean shouldUseAdminBackButtonStyling() {
        return true;
    }

    @Override
    protected void onShellReady() {
        RecyclerView rv = findViewById(R.id.rvInventoryAlerts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryManagementAdapter(item -> startActivity(new Intent(this, AdminReceiptEditorActivity.class)));
        rv.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        ArrayList<Product> products = productDao.layTatCa();
        HashMap<Long, int[]> totalsByProduct = buildHistoryTotals();
        ArrayList<InventoryManagementItem> items = new ArrayList<>();

        for (Product product : products) {
            String status = resolveStatus(product.tonKho);
            if (InventoryManagementItem.STATUS_IN_STOCK.equals(status)) {
                continue;
            }

            int[] totals = totalsByProduct.get(product.maSanPham);
            items.add(new InventoryManagementItem(
                    product.maSanPham,
                    normalizeName(product.tenSanPham),
                    normalizeBrand(product.hang),
                    product.tenAnh,
                    Math.max(0, product.tonKho),
                    MINIMUM_STOCK,
                    totals == null ? 0 : totals[0],
                    totals == null ? 0 : totals[1],
                    status
            ));
        }

        adapter.setData(items);
    }

    private HashMap<Long, int[]> buildHistoryTotals() {
        ArrayList<InventoryHistoryEntry> histories = historyDao.getAll();
        HashMap<Long, int[]> totalsByProduct = new HashMap<>();
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

    private String resolveStatus(int stock) {
        if (stock <= 0) {
            return FILTER_OUT;
        }
        if (stock <= MINIMUM_STOCK) {
            return FILTER_LOW;
        }
        return InventoryManagementItem.STATUS_IN_STOCK;
    }

    private String normalizeName(String name) {
        return name == null || name.trim().isEmpty() ? getString(R.string.admin_product_unknown_name) : name.trim();
    }

    private String normalizeBrand(String brand) {
        return brand == null || brand.trim().isEmpty() ? getString(R.string.admin_product_unknown_brand) : brand.trim();
    }
}
