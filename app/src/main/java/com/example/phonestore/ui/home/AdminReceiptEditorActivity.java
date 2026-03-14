package com.example.phonestore.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
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
import com.example.phonestore.data.model.Product;
import com.example.phonestore.data.model.Receipt;
import com.example.phonestore.data.model.ReceiptItem;
import com.example.phonestore.data.model.Supplier;
import com.example.phonestore.utils.SessionManager;

import java.util.ArrayList;

public class AdminReceiptEditorActivity extends AppCompatActivity {

    private ReceiptDao receiptDao;
    private SupplierDao supplierDao;
    private ProductDao productDao;
    private ReceiptLineAdapter adapter;
    private ArrayList<Supplier> suppliers;
    private ArrayList<Product> products;
    private String creatorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_editor);

        receiptDao = new ReceiptDao(this);
        supplierDao = new SupplierDao(this);
        productDao = new ProductDao(this);
        suppliers = supplierDao.getAll(null);
        products = productDao.layTatCaChoAdmin();
        creatorName = resolveCreatorName();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setTitleMarginStart(Math.round(getResources().getDisplayMetrics().density * 12));
        toolbar.setTitleMarginEnd(0);
        toolbar.setTitleMarginTop(0);
        toolbar.setTitleMarginBottom(0);
        toolbar.setTitle(R.string.receipt_create_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bindHeader();
        setupSupplierSpinner();
        setupLinesRecycler();

        findViewById(R.id.btnAddReceiptLine).setOnClickListener(v -> showAddLineDialog());
        findViewById(R.id.btnSaveDraftReceipt).setOnClickListener(v -> saveReceipt(false));
        findViewById(R.id.btnConfirmReceipt).setOnClickListener(v -> saveReceipt(true));
        updateSummary();
    }

    private void bindHeader() {
        int nextId = receiptDao.countReceipts() + 1;
        ((TextView) findViewById(R.id.tvReceiptCode)).setText(Receipt.formatCode(nextId));
        ((TextView) findViewById(R.id.tvReceiptCreator)).setText(creatorName);
    }

    private void setupSupplierSpinner() {
        Spinner spSupplier = findViewById(R.id.spSupplier);
        ArrayAdapter<String> supplierAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        for (Supplier supplier : suppliers) {
            supplierAdapter.add(supplier.name);
        }
        spSupplier.setAdapter(supplierAdapter);
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

    private void showAddLineDialog() {
        if (products.isEmpty()) {
            Toast.makeText(this, R.string.receipt_no_products_available, Toast.LENGTH_SHORT).show();
            return;
        }

        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_receipt_line_form, null, false);
        Spinner spProduct = view.findViewById(R.id.spProduct);

        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        for (Product product : products) {
            productAdapter.add(product.tenSanPham);
        }
        spProduct.setAdapter(productAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.receipt_add_product)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Product product = products.get(spProduct.getSelectedItemPosition());
            ReceiptItem item = new ReceiptItem();
            item.productId = product.maSanPham;
            item.productName = product.tenSanPham;
            item.quantity = 1;
            item.unitCost = Math.max(product.gia, 1);
            item.recalculateAmount();
            adapter.addOrMergeItem(item);
            updateSummary();
            Toast.makeText(this, R.string.receipt_line_added, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }));
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

        Toast.makeText(this, confirmReceipt ? R.string.receipt_confirmed : R.string.receipt_saved_draft, Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean validateForm() {
        if (suppliers.isEmpty()) {
            Toast.makeText(this, R.string.receipt_supplier_required, Toast.LENGTH_SHORT).show();
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
