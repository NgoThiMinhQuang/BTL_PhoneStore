package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.InventoryHistoryDao;
import com.example.phonestore.data.dao.ProductDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.utils.InventoryPolicy;

import java.util.ArrayList;
import java.util.HashMap;

public class AdminInventoryAlertsActivity extends BaseHomeActivity {

    private ProductDao productDao;
    private InventoryHistoryDao historyDao;
    private InventoryManagementAdapter adapter;
    private TextView tvAlertsHeaderCount;

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
        tvAlertsHeaderCount = findViewById(R.id.tvAlertsHeaderCount);

        RecyclerView rv = findViewById(R.id.rvInventoryAlerts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryManagementAdapter(item -> startActivity(new Intent(this, AdminReceiptEditorActivity.class)));
        rv.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        ArrayList<Product> products = productDao.layTatCa();
        HashMap<Long, int[]> totalsByProduct = InventoryDataHelper.buildHistoryTotals(historyDao.getAll());
        ArrayList<InventoryManagementItem> items = new ArrayList<>();

        for (Product product : products) {
            if (InventoryPolicy.isInStock(product.tonKho)) {
                continue;
            }
            items.add(InventoryDataHelper.toInventoryItem(this, product, totalsByProduct));
        }

        updateHeader(items.size());
        adapter.setData(items);
    }

    private void updateHeader(int count) {
        if (tvAlertsHeaderCount == null) return;
        if (count <= 0) {
            tvAlertsHeaderCount.setText(R.string.admin_alerts_header_empty);
            return;
        }
        tvAlertsHeaderCount.setText(getString(R.string.admin_alerts_summary_count, count));
    }
}
