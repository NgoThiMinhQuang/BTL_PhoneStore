package com.example.phonestore.ui.home;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.SupplierDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Supplier;
import com.example.phonestore.ui.auth.WelcomeActivity;

public class AdminSuppliersActivity extends BaseHomeActivity {

    private SupplierDao supplierDao;
    private com.example.phonestore.data.dao.ReceiptDao receiptDao;
    private SupplierAdapter adapter;
    private String currentBrandFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }
        supplierDao = new SupplierDao(this);
        receiptDao = new com.example.phonestore.data.dao.ReceiptDao(this);
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
        return getString(R.string.admin_suppliers_title);
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected void onShellReady() {
        ((TextView) findViewById(R.id.tvScreenTitle)).setText(R.string.admin_suppliers_title);
        ((TextView) findViewById(R.id.tvScreenSummary)).setText(R.string.admin_suppliers_summary);
        View cardPrimary = findViewById(R.id.cardPrimaryKpi);
        View cardSecondary = findViewById(R.id.cardSecondaryKpi);
        ((TextView) cardPrimary.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_kpi_brand_count);
        ((TextView) cardSecondary.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_kpi_priority_import);

        RecyclerView rv = findViewById(R.id.rvInventoryList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SupplierAdapter(
                this::showSupplierDialog,
                this::confirmDeleteSupplier
        );
        rv.setAdapter(adapter);

        findViewById(R.id.btnPrimaryAction).setOnClickListener(v -> showSupplierDialog(null));
        ((com.google.android.material.button.MaterialButton) findViewById(R.id.btnPrimaryAction)).setText(R.string.add_supplier);
        setupBrandFilters(null);

        EditText edtSearch = findViewById(R.id.edtWarehouseSearch);
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { loadData(s.toString().trim()); }
        });

        loadData("");
    }

    private void loadData(String keyword) {
        java.util.ArrayList<Supplier> suppliers = supplierDao.getAll(keyword);
        if (!"ALL".equals(currentBrandFilter)) {
            java.util.ArrayList<Supplier> filtered = new java.util.ArrayList<>();
            for (Supplier supplier : suppliers) {
                String brand = supplier.brand == null ? "" : supplier.brand.trim();
                if (currentBrandFilter.equalsIgnoreCase(brand)) filtered.add(supplier);
            }
            suppliers = filtered;
        }
        int lowStockBrands = 0;
        java.util.HashSet<String> brands = new java.util.HashSet<>();
        for (Supplier supplier : suppliers) {
            if (supplier.brand != null && !supplier.brand.trim().isEmpty()) {
                brands.add(supplier.brand.trim());
            }
        }
        lowStockBrands = brands.size();
        adapter.setData(suppliers);
        setupBrandFilters(suppliers);
        View cardPrimary = findViewById(R.id.cardPrimaryKpi);
        View cardSecondary = findViewById(R.id.cardSecondaryKpi);
        ((TextView) cardPrimary.findViewById(R.id.tvKpiValue)).setText(String.valueOf(suppliers.size()));
        ((TextView) cardSecondary.findViewById(R.id.tvKpiValue)).setText(String.valueOf(lowStockBrands));
    }

    private void confirmDeleteSupplier(Supplier supplier) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_supplier_title)
                .setMessage(getString(R.string.supplier_delete_confirm, supplier.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    if (receiptDao.hasReceiptForSupplier(supplier.id)) {
                        Toast.makeText(this, R.string.supplier_in_use, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    boolean ok = supplierDao.delete(supplier.id);
                    if (!ok) {
                        Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(this, R.string.supplier_deleted, Toast.LENGTH_SHORT).show();
                    loadData("");
                })
                .show();
    }

    private void setupBrandFilters(java.util.ArrayList<Supplier> suppliers) {
        LinearLayout container = findViewById(R.id.layoutWarehouseFilters);
        container.removeAllViews();
        addBrandChip(container, getString(R.string.filter_brand_all), "ALL");
        java.util.LinkedHashSet<String> brands = new java.util.LinkedHashSet<>();
        java.util.ArrayList<Supplier> source = suppliers == null ? supplierDao.getAll(null) : suppliers;
        for (Supplier supplier : source) {
            if (supplier.brand != null && !supplier.brand.trim().isEmpty()) brands.add(supplier.brand.trim());
        }
        for (String brand : brands) {
            addBrandChip(container, brand, brand);
        }
    }

    private void addBrandChip(LinearLayout container, String label, String value) {
        TextView chip = (TextView) LayoutInflater.from(this).inflate(R.layout.item_filter_chip, container, false);
        chip.setText(label);
        chip.setSelected(value.equals(currentBrandFilter));
        chip.setTextColor(getColor(value.equals(currentBrandFilter) ? android.R.color.white : R.color.admin_text_secondary));
        chip.setOnClickListener(v -> {
            currentBrandFilter = value;
            setupBrandFilters(null);
            EditText edtSearch = findViewById(R.id.edtWarehouseSearch);
            loadData(edtSearch.getText().toString().trim());
        });
        container.addView(chip);
    }

    private void showSupplierDialog(Supplier oldSupplier) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_supplier_form, null, false);
        EditText edtName = view.findViewById(R.id.edtSupplierName);
        EditText edtBrand = view.findViewById(R.id.edtSupplierBrand);
        EditText edtPhone = view.findViewById(R.id.edtSupplierPhone);
        EditText edtAddress = view.findViewById(R.id.edtSupplierAddress);

        boolean editing = oldSupplier != null;
        if (editing) {
            edtName.setText(oldSupplier.name);
            edtBrand.setText(oldSupplier.brand);
            edtPhone.setText(oldSupplier.phone);
            edtAddress.setText(oldSupplier.address);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? R.string.edit_supplier : R.string.add_supplier)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, R.string.err_fullname_required, Toast.LENGTH_SHORT).show();
                return;
            }
            Supplier supplier = editing ? oldSupplier : new Supplier();
            supplier.name = name;
            supplier.brand = edtBrand.getText().toString().trim();
            supplier.phone = edtPhone.getText().toString().trim();
            supplier.address = edtAddress.getText().toString().trim();

            boolean ok = editing ? supplierDao.update(supplier) : supplierDao.insert(supplier) != -1;
            if (!ok) {
                Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, editing ? R.string.supplier_updated : R.string.supplier_added, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            loadData("");
        }));
        dialog.show();
    }
}
