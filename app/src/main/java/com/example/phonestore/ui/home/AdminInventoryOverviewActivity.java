package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

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

public class AdminInventoryOverviewActivity extends BaseHomeActivity {

    private static final String FILTER_ALL = "ALL";
    private static final String FILTER_IN_STOCK = InventoryManagementItem.STATUS_IN_STOCK;
    private static final String FILTER_LOW_STOCK = InventoryManagementItem.STATUS_LOW_STOCK;
    private static final String FILTER_OUT_OF_STOCK = InventoryManagementItem.STATUS_OUT_OF_STOCK;
    private static final int MINIMUM_STOCK = 10;

    private ProductDao productDao;
    private InventoryHistoryDao historyDao;
    private InventoryManagementAdapter adapter;
    private String currentFilter = FILTER_ALL;

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
        return R.layout.activity_admin_inventory_list;
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
        return getString(R.string.admin_inventory_overview_title);
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
        RecyclerView rv = findViewById(R.id.rvInventoryList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryManagementAdapter();
        rv.setAdapter(adapter);

        findViewById(R.id.btnPrimaryAction).setVisibility(View.GONE);
        setupFilters();

        EditText edtSearch = findViewById(R.id.edtWarehouseSearch);
        edtSearch.setHint(R.string.admin_inventory_search_hint);
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { loadData(s.toString().trim()); }
        });

        loadData("");
    }

    private void loadData(String keyword) {
        ArrayList<Product> products = keyword == null || keyword.isEmpty()
                ? productDao.layTatCa()
                : productDao.timKiem(keyword);
        HashMap<Long, int[]> totalsByProduct = buildHistoryTotals();
        ArrayList<InventoryManagementItem> items = new ArrayList<>();

        for (Product product : products) {
            int[] totals = totalsByProduct.get(product.maSanPham);
            int totalImport = totals == null ? 0 : totals[0];
            int totalExport = totals == null ? 0 : totals[1];
            int currentStock = Math.max(0, product.tonKho);
            String status = resolveStatus(product.tonKho);
            if (!matchesFilter(status)) {
                continue;
            }
            items.add(new InventoryManagementItem(
                    product.maSanPham,
                    normalizeName(product.tenSanPham),
                    normalizeBrand(product.hang),
                    currentStock,
                    MINIMUM_STOCK,
                    totalImport,
                    totalExport,
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
        if (stock <= 0) return FILTER_OUT_OF_STOCK;
        if (stock <= MINIMUM_STOCK) return FILTER_LOW_STOCK;
        return FILTER_IN_STOCK;
    }

    private boolean matchesFilter(String status) {
        return FILTER_ALL.equals(currentFilter) || currentFilter.equals(status);
    }

    private void setupFilters() {
        LinearLayout container = findViewById(R.id.layoutWarehouseFilters);
        container.removeAllViews();
        addFilterChip(container, getString(R.string.filter_all), FILTER_ALL);
        addFilterChip(container, getString(R.string.admin_filter_in_stock), FILTER_IN_STOCK);
        addFilterChip(container, getString(R.string.filter_low_stock), FILTER_LOW_STOCK);
        addFilterChip(container, getString(R.string.filter_out_of_stock), FILTER_OUT_OF_STOCK);
    }

    private void addFilterChip(LinearLayout container, String label, String filter) {
        TextView chip = (TextView) LayoutInflater.from(this).inflate(R.layout.item_filter_chip, container, false);
        chip.setText(label);
        chip.setSelected(filter.equals(currentFilter));
        chip.setTextColor(getColor(filter.equals(currentFilter) ? android.R.color.white : R.color.admin_text_secondary));
        chip.setOnClickListener(v -> {
            currentFilter = filter;
            setupFilters();
            EditText edtSearch = findViewById(R.id.edtWarehouseSearch);
            loadData(edtSearch.getText().toString().trim());
        });
        container.addView(chip);
    }

    private String normalizeName(String name) {
        return name == null || name.trim().isEmpty()
                ? getString(R.string.admin_product_unknown_name)
                : name.trim();
    }

    private String normalizeBrand(String brand) {
        return brand == null || brand.trim().isEmpty()
                ? getString(R.string.admin_product_unknown_brand)
                : brand.trim();
    }
}
