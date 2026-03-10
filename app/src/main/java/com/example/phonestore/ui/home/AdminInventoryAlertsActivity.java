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
import com.example.phonestore.data.dao.ProductDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.ui.auth.WelcomeActivity;

import java.util.ArrayList;

public class AdminInventoryAlertsActivity extends BaseHomeActivity {

    private ProductDao productDao;
    private ReceiptAdapter adapter;
    private String currentFilter = "ALL";

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
        return getString(R.string.admin_alerts_title);
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected void onShellReady() {
        ((TextView) findViewById(R.id.tvScreenTitle)).setText(R.string.admin_alerts_title);
        ((TextView) findViewById(R.id.tvScreenSummary)).setText(R.string.admin_alerts_summary);
        View cardPrimary = findViewById(R.id.cardPrimaryKpi);
        View cardSecondary = findViewById(R.id.cardSecondaryKpi);
        ((TextView) cardPrimary.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_kpi_out_of_stock);
        ((TextView) cardSecondary.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_kpi_low_stock);

        RecyclerView rv = findViewById(R.id.rvInventoryList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReceiptAdapter(receipt -> {});
        rv.setAdapter(adapter);

        findViewById(R.id.btnPrimaryAction).setVisibility(View.GONE);
        setupFilters();
        EditText edtSearch = findViewById(R.id.edtWarehouseSearch);
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { loadData(s.toString().trim()); }
        });
        loadData("");
    }

    private void loadData(String keyword) {
        ArrayList<Product> products = productDao.layTatCa();
        ArrayList<com.example.phonestore.data.model.Receipt> alertRows = new ArrayList<>();
        int out = 0;
        int low = 0;
        for (Product product : products) {
            if (product.tonKho == 0) out++;
            if (product.tonKho > 0 && product.tonKho <= 5) low++;

            String name = product.tenSanPham == null ? "" : product.tenSanPham.toLowerCase();
            String brand = product.hang == null ? "" : product.hang.toLowerCase();
            if (keyword != null && !keyword.isEmpty() && !name.contains(keyword.toLowerCase()) && !brand.contains(keyword.toLowerCase())) {
                continue;
            }
            if (!"ALL".equals(currentFilter) && !"OUT".equals(currentFilter) && product.tonKho == 0) {
                continue;
            }
            if (!"ALL".equals(currentFilter) && !"LOW".equals(currentFilter) && product.tonKho > 0 && product.tonKho <= 5) {
                continue;
            }
            if (product.tonKho == 0) {
                com.example.phonestore.data.model.Receipt fake = new com.example.phonestore.data.model.Receipt();
                fake.id = product.maSanPham;
                fake.supplierName = product.tenSanPham;
                fake.totalQuantity = product.tonKho;
                fake.note = "Hết hàng • " + (product.hang == null ? "" : product.hang);
                fake.totalAmount = 0;
                alertRows.add(fake);
                continue;
            }
            if (product.tonKho <= 5) {
                com.example.phonestore.data.model.Receipt fake = new com.example.phonestore.data.model.Receipt();
                fake.id = product.maSanPham;
                fake.supplierName = product.tenSanPham;
                fake.totalQuantity = product.tonKho;
                fake.note = "Sắp hết • " + (product.hang == null ? "" : product.hang);
                fake.totalAmount = 0;
                alertRows.add(fake);
            }
        }
        adapter.setData(alertRows);
        View cardPrimary = findViewById(R.id.cardPrimaryKpi);
        View cardSecondary = findViewById(R.id.cardSecondaryKpi);
        ((TextView) cardPrimary.findViewById(R.id.tvKpiValue)).setText(String.valueOf(out));
        ((TextView) cardSecondary.findViewById(R.id.tvKpiValue)).setText(String.valueOf(low));
    }

    private void setupFilters() {
        LinearLayout container = findViewById(R.id.layoutWarehouseFilters);
        container.removeAllViews();
        addFilterChip(container, getString(R.string.filter_all), "ALL");
        addFilterChip(container, getString(R.string.filter_out_of_stock), "OUT");
        addFilterChip(container, getString(R.string.filter_low_stock), "LOW");
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
}
