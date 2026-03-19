package com.example.phonestore.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.example.phonestore.utils.SessionManager;

import java.util.ArrayList;

public class AdminReceiptEditorActivity extends AppCompatActivity {

    private ReceiptDao receiptDao;
    private SupplierDao supplierDao;
    private ProductDao productDao;
    private ReceiptLineAdapter adapter;
    private ArrayList<Supplier> suppliers;
    private ArrayList<Product> availableProducts = new ArrayList<>();
    private String creatorName;
    private TextView tvSupplierBrandHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_editor);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new android.content.Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        receiptDao = new ReceiptDao(this);
        supplierDao = new SupplierDao(this);
        productDao = new ProductDao(this);
        suppliers = supplierDao.getAll(null);
        creatorName = resolveCreatorName();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setTitleMarginStart(Math.round(getResources().getDisplayMetrics().density * 12));
        toolbar.setTitleMarginEnd(0);
        toolbar.setTitleMarginTop(0);
        toolbar.setTitleMarginBottom(0);
        toolbar.setTitle(getString(R.string.receipt_create_title));
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvSupplierBrandHint = findViewById(R.id.tvSupplierBrandHint);
        bindHeader();
        setupSupplierSpinner();
        setupLinesRecycler();

        findViewById(R.id.btnAddReceiptLine).setOnClickListener(v -> showAddLineDialog());
        findViewById(R.id.btnSaveDraftReceipt).setOnClickListener(v -> saveReceipt(false));
        findViewById(R.id.btnConfirmReceipt).setOnClickListener(v -> saveReceipt(true));
        updateSummary();
    }

    private void bindHeader() {
        ((TextView) findViewById(R.id.tvReceiptCode)).setText(getString(R.string.receipt_create_title));
        ((TextView) findViewById(R.id.tvReceiptCreator)).setText(creatorName);
    }

    private void setupSupplierSpinner() {
        Spinner spSupplier = findViewById(R.id.spSupplier);
        ArrayAdapter<String> supplierAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected_admin, new ArrayList<>());
        supplierAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown_admin);
        for (Supplier supplier : suppliers) {
            supplierAdapter.add(supplier.name);
        }
        spSupplier.setAdapter(supplierAdapter);
        spSupplier.setOnItemSelectedListener(new SimpleItemSelectedListener(this::refreshAvailableProducts));
        refreshAvailableProducts();
    }

    private void setupLinesRecycler() {
        RecyclerView rv = findViewById(R.id.rvReceiptLines);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReceiptLineAdapter(true, new ReceiptLineAdapter.Listener() {
            @Override
            public void onRemove(int position) {
                new AlertDialog.Builder(AdminReceiptEditorActivity.this)
                        .setTitle(R.string.delete)
                        .setMessage(R.string.receipt_line_remove_confirm)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.delete, (dialog, which) -> {
                            adapter.removeAt(position);
                            updateSummary();
                        })
                        .show();
            }

            @Override
            public void onItemChanged() {
                updateSummary();
            }
        });
        rv.setAdapter(adapter);
    }

    private void refreshAvailableProducts() {
        Supplier supplier = getSelectedSupplier();
        String brand = normalizeBrand(supplier == null ? null : supplier.brand);
        if (brand == null) {
            availableProducts.clear();
            if (tvSupplierBrandHint != null) {
                tvSupplierBrandHint.setText(R.string.receipt_supplier_brand_required);
                tvSupplierBrandHint.setVisibility(View.VISIBLE);
            }
            return;
        }

        availableProducts = productDao.laySanPhamDangBanTheoHang(brand);
        if (tvSupplierBrandHint != null) {
            tvSupplierBrandHint.setText(getString(R.string.receipt_supplier_brand_hint, brand));
            tvSupplierBrandHint.setVisibility(View.VISIBLE);
        }
    }

    private void showAddLineDialog() {
        Supplier supplier = getSelectedSupplier();
        String brand = normalizeBrand(supplier == null ? null : supplier.brand);
        if (brand == null) {
            Toast.makeText(this, R.string.receipt_supplier_brand_required, Toast.LENGTH_SHORT).show();
            return;
        }

        View view = getLayoutInflater().inflate(R.layout.dialog_receipt_line_form, null, false);
        Spinner spProduct = view.findViewById(R.id.spProduct);
        EditText edtQuantity = view.findViewById(R.id.edtReceiptLineQuantity);
        EditText edtUnitCost = view.findViewById(R.id.edtReceiptLineUnitCost);
        TextView tvEmptyMessage = view.findViewById(R.id.tvReceiptLineEmptyMessage);

        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_selected_admin, new ArrayList<>());
        productAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown_admin);
        for (Product product : availableProducts) {
            productAdapter.add(product.tenSanPham);
        }
        spProduct.setAdapter(productAdapter);

        boolean hasProducts = !availableProducts.isEmpty();
        tvEmptyMessage.setVisibility(hasProducts ? View.GONE : View.VISIBLE);
        if (!hasProducts) {
            tvEmptyMessage.setText(getString(R.string.receipt_no_products_for_supplier_brand, brand));
            spProduct.setVisibility(View.GONE);
            edtQuantity.setVisibility(View.GONE);
            edtUnitCost.setVisibility(View.GONE);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.receipt_add_product)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add, null)
                .create();

        dialog.setOnShowListener(d -> {
            if (!hasProducts) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                return;
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int selectedIndex = spProduct.getSelectedItemPosition();
                if (selectedIndex < 0 || selectedIndex >= availableProducts.size()) {
                    Toast.makeText(this, R.string.receipt_product_required, Toast.LENGTH_SHORT).show();
                    return;
                }

                int quantity = parsePositiveInt(edtQuantity.getText().toString().trim());
                int unitCost = parsePositiveInt(edtUnitCost.getText().toString().trim());
                if (quantity <= 0) {
                    Toast.makeText(this, R.string.receipt_quantity_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (unitCost <= 0) {
                    Toast.makeText(this, R.string.receipt_cost_required, Toast.LENGTH_SHORT).show();
                    return;
                }

                Product product = availableProducts.get(selectedIndex);
                ReceiptItem item = new ReceiptItem();
                item.productId = product.maSanPham;
                item.productName = product.tenSanPham;
                item.quantity = quantity;
                item.unitCost = unitCost;
                item.recalculateAmount();
                adapter.addOrMergeItem(item);
                updateSummary();
                Toast.makeText(this, R.string.receipt_line_added, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void saveReceipt(boolean confirmReceipt) {
        if (!validateForm()) {
            return;
        }

        Spinner spSupplier = findViewById(R.id.spSupplier);
        EditText edtReceiptNote = findViewById(R.id.edtReceiptNote);
        Supplier supplier = suppliers.get(spSupplier.getSelectedItemPosition());
        ArrayList<ReceiptItem> items = adapter.getData();
        long id = confirmReceipt
                ? receiptDao.createConfirmedReceipt(supplier.id, edtReceiptNote.getText().toString().trim(), creatorName, items)
                : receiptDao.saveDraftReceipt(supplier.id, edtReceiptNote.getText().toString().trim(), creatorName, items);

        if (id == -1) {
            Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        String receiptCode = Receipt.formatCode(id);
        Toast.makeText(
                this,
                getString(confirmReceipt ? R.string.receipt_confirmed_with_code : R.string.receipt_saved_draft_with_code, receiptCode),
                Toast.LENGTH_SHORT
        ).show();
        setResult(RESULT_OK);
        finish();
    }

    private boolean validateForm() {
        if (suppliers.isEmpty()) {
            Toast.makeText(this, R.string.receipt_supplier_required, Toast.LENGTH_SHORT).show();
            return false;
        }
        Supplier supplier = getSelectedSupplier();
        if (normalizeBrand(supplier == null ? null : supplier.brand) == null) {
            Toast.makeText(this, R.string.receipt_supplier_brand_required, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (adapter.getData().isEmpty()) {
            Toast.makeText(this, R.string.receipt_lines_required, Toast.LENGTH_SHORT).show();
            return false;
        }
        for (ReceiptItem item : adapter.getData()) {
            if (item.quantity <= 0) {
                Toast.makeText(this, R.string.receipt_quantity_required, Toast.LENGTH_SHORT).show();
                return false;
            }
            if (item.unitCost <= 0) {
                Toast.makeText(this, R.string.receipt_cost_required, Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!isAllowedProductForSupplier(item.productId)) {
                Toast.makeText(this, R.string.receipt_line_supplier_brand_mismatch, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void updateSummary() {
        int itemCount = adapter.getData().size();
        ((TextView) findViewById(R.id.tvReceiptLinesSummary)).setText(
                itemCount == 0
                        ? getString(R.string.receipt_line_empty)
                        : getString(R.string.receipt_items_count, itemCount)
        );
        ((TextView) findViewById(R.id.tvTotalQuantity)).setText(getString(R.string.receipt_quantity_value, adapter.getTotalQuantity()));
        ((TextView) findViewById(R.id.tvTotalAmount)).setText(ReceiptUiFormatter.formatCurrency(this, adapter.getTotalAmount()));
    }

    private Supplier getSelectedSupplier() {
        Spinner spSupplier = findViewById(R.id.spSupplier);
        int position = spSupplier == null ? 0 : spSupplier.getSelectedItemPosition();
        if (position < 0 || position >= suppliers.size()) {
            return null;
        }
        return suppliers.get(position);
    }

    private String normalizeBrand(String brand) {
        if (brand == null) {
            return null;
        }
        String trimmed = brand.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int parsePositiveInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private boolean isAllowedProductForSupplier(long productId) {
        for (Product product : availableProducts) {
            if (product.maSanPham == productId) {
                return true;
            }
        }
        return false;
    }

    private String resolveCreatorName() {
        String username = new SessionManager(this).getUsername();
        if (username == null || username.trim().isEmpty()) {
            return getString(R.string.receipt_creator_default);
        }
        return username.trim();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
