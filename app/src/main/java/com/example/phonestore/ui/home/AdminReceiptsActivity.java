package com.example.phonestore.ui.home;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.ProductDao;
import com.example.phonestore.data.dao.ReceiptDao;
import com.example.phonestore.data.dao.SupplierDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.data.model.Receipt;
import com.example.phonestore.data.model.ReceiptItem;
import com.example.phonestore.data.model.Supplier;
import com.example.phonestore.ui.auth.WelcomeActivity;

import java.util.ArrayList;

public class AdminReceiptsActivity extends BaseHomeActivity {

    private ReceiptDao receiptDao;
    private SupplierDao supplierDao;
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
        receiptDao = new ReceiptDao(this);
        supplierDao = new SupplierDao(this);
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
        return getString(R.string.admin_receipts_title);
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected void onShellReady() {
        ((TextView) findViewById(R.id.tvScreenTitle)).setText(R.string.admin_receipts_title);
        ((TextView) findViewById(R.id.tvScreenSummary)).setText(R.string.admin_receipts_summary);
        View cardPrimary = findViewById(R.id.cardPrimaryKpi);
        View cardSecondary = findViewById(R.id.cardSecondaryKpi);
        ((TextView) cardPrimary.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_kpi_need_import);
        ((TextView) cardSecondary.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_kpi_suggested_qty);

        RecyclerView rv = findViewById(R.id.rvInventoryList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReceiptAdapter(receipt -> {
            Intent intent = new Intent(this, AdminReceiptDetailActivity.class);
            intent.putExtra(AdminReceiptDetailActivity.EXTRA_RECEIPT_ID, receipt.id);
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        ((com.google.android.material.button.MaterialButton) findViewById(R.id.btnPrimaryAction)).setText(R.string.receipt_create);
        findViewById(R.id.btnPrimaryAction).setOnClickListener(v -> startActivity(new Intent(this, AdminReceiptEditorActivity.class)));
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
        ArrayList<Receipt> receipts = receiptDao.getRecentReceipts();
        if (keyword != null && !keyword.isEmpty()) {
            ArrayList<Receipt> filtered = new ArrayList<>();
            String lower = keyword.toLowerCase();
            for (Receipt receipt : receipts) {
                String supplier = receipt.supplierName == null ? "" : receipt.supplierName.toLowerCase();
                String note = receipt.note == null ? "" : receipt.note.toLowerCase();
                if (supplier.contains(lower) || note.contains(lower) || String.valueOf(receipt.id).contains(lower)) {
                    filtered.add(receipt);
                }
            }
            receipts = filtered;
        }
        if (!"ALL".equals(currentFilter)) {
            ArrayList<Receipt> filtered = new ArrayList<>();
            for (int i = 0; i < receipts.size(); i++) {
                Receipt receipt = receipts.get(i);
                boolean hasNote = receipt.note != null && !receipt.note.trim().isEmpty();
                if ("RECENT".equals(currentFilter) && i < 5) filtered.add(receipt);
                if ("HAS_NOTE".equals(currentFilter) && hasNote) filtered.add(receipt);
                if ("NO_NOTE".equals(currentFilter) && !hasNote) filtered.add(receipt);
            }
            receipts = filtered;
        }
        ArrayList<Product> products = productDao.layTatCa();
        int lowStock = 0;
        int suggested = 0;
        for (Product product : products) {
            if (product.tonKho <= 5) {
                lowStock++;
                suggested += product.tonKho == 0 ? 12 : 8;
            }
        }
        adapter.setData(receipts);
        View cardPrimary = findViewById(R.id.cardPrimaryKpi);
        View cardSecondary = findViewById(R.id.cardSecondaryKpi);
        ((TextView) cardPrimary.findViewById(R.id.tvKpiValue)).setText(String.valueOf(lowStock));
        ((TextView) cardSecondary.findViewById(R.id.tvKpiValue)).setText(String.valueOf(suggested));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (receiptDao != null) loadData("");
    }

    private void setupFilters() {
        LinearLayout container = findViewById(R.id.layoutWarehouseFilters);
        container.removeAllViews();
        addFilterChip(container, getString(R.string.filter_all), "ALL");
        addFilterChip(container, getString(R.string.filter_recent_receipts), "RECENT");
        addFilterChip(container, getString(R.string.filter_has_note), "HAS_NOTE");
        addFilterChip(container, getString(R.string.filter_no_note), "NO_NOTE");
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
