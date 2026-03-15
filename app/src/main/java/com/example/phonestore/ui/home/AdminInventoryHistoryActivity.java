package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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

public class AdminInventoryHistoryActivity extends BaseHomeActivity {

    private static final String FILTER_ALL = "ALL";

    private InventoryHistoryDao historyDao;
    private ProductDao productDao;
    private InventoryHistoryAdapter adapter;
    private Spinner spProduct;
    private Spinner spType;
    private ArrayList<Product> products = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }
        historyDao = new InventoryHistoryDao(this);
        productDao = new ProductDao(this);
    }

    @Override
    protected int shellLayoutRes() {
        return R.layout.activity_admin_inventory_history;
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
        return getString(R.string.admin_history_title);
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
        spProduct = findViewById(R.id.spHistoryProduct);
        spType = findViewById(R.id.spHistoryType);

        RecyclerView rv = findViewById(R.id.rvInventoryHistory);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryHistoryAdapter();
        rv.setAdapter(adapter);

        setupFilters();
        loadData();
    }

    private void setupFilters() {
        products = productDao.layTatCaChoAdmin();

        ArrayList<String> productLabels = new ArrayList<>();
        productLabels.add(getString(R.string.inventory_history_all_products));
        for (Product product : products) {
            productLabels.add(product.tenSanPham);
        }

        ArrayList<String> typeLabels = new ArrayList<>();
        typeLabels.add(getString(R.string.inventory_history_all_types));
        typeLabels.add(getString(R.string.inventory_history_import));
        typeLabels.add(getString(R.string.inventory_history_export));
        typeLabels.add(getString(R.string.inventory_history_cancel_return));

        spProduct.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, productLabels));
        spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, typeLabels));

        spProduct.setOnItemSelectedListener(new SimpleItemSelectedListener(this::loadData));
        spType.setOnItemSelectedListener(new SimpleItemSelectedListener(this::loadData));
    }

    private void loadData() {
        ArrayList<InventoryHistoryEntry> list = historyDao.getAll();
        enrichStockAfter(list);

        ArrayList<InventoryHistoryEntry> filtered = new ArrayList<>();
        long selectedProductId = getSelectedProductId();
        String selectedType = getSelectedType();

        for (InventoryHistoryEntry entry : list) {
            if (selectedProductId != -1 && entry.productId != selectedProductId) {
                continue;
            }
            if (!FILTER_ALL.equals(selectedType) && !selectedType.equals(entry.actionType)) {
                continue;
            }
            filtered.add(entry);
        }

        adapter.setData(filtered);
    }

    private void enrichStockAfter(ArrayList<InventoryHistoryEntry> list) {
        HashMap<Long, Integer> runningStock = productDao.getCurrentStockMap(true);
        for (InventoryHistoryEntry entry : list) {
            int currentStock = runningStock.containsKey(entry.productId) ? runningStock.get(entry.productId) : 0;
            entry.stockAfter = Math.max(0, currentStock);
            runningStock.put(entry.productId, reverseEntryEffect(entry, currentStock));
        }
    }

    private int reverseEntryEffect(InventoryHistoryEntry entry, int currentStock) {
        int quantity = Math.max(0, entry.quantity);
        if (InventoryHistoryDao.ACTION_IMPORT.equals(entry.actionType)
                || InventoryHistoryDao.ACTION_CANCEL_RETURN.equals(entry.actionType)) {
            return Math.max(0, currentStock - quantity);
        }
        if (InventoryHistoryDao.ACTION_EXPORT.equals(entry.actionType)) {
            return Math.max(0, currentStock + quantity);
        }
        return Math.max(0, currentStock);
    }

    private long getSelectedProductId() {
        int position = spProduct == null ? 0 : spProduct.getSelectedItemPosition();
        if (position <= 0 || position > products.size()) {
            return -1;
        }
        return products.get(position - 1).maSanPham;
    }

    private String getSelectedType() {
        int position = spType == null ? 0 : spType.getSelectedItemPosition();
        if (position == 1) {
            return InventoryHistoryDao.ACTION_IMPORT;
        }
        if (position == 2) {
            return InventoryHistoryDao.ACTION_EXPORT;
        }
        if (position == 3) {
            return InventoryHistoryDao.ACTION_CANCEL_RETURN;
        }
        return FILTER_ALL;
    }
}
