package com.example.phonestore.ui.home;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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
import com.example.phonestore.data.model.ReceiptItem;
import com.example.phonestore.data.model.Supplier;

import java.util.ArrayList;

public class AdminReceiptEditorActivity extends AppCompatActivity {

    private ReceiptDao receiptDao;
    private SupplierDao supplierDao;
    private ProductDao productDao;
    private ReceiptLineAdapter adapter;
    private ArrayList<Supplier> suppliers;
    private ArrayList<Product> products;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_editor);

        receiptDao = new ReceiptDao(this);
        supplierDao = new SupplierDao(this);
        productDao = new ProductDao(this);
        suppliers = supplierDao.getAll(null);
        products = productDao.layTatCa();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.receipt_create_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Spinner spSupplier = findViewById(R.id.spSupplier);
        ArrayAdapter<String> supplierAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        for (Supplier supplier : suppliers) supplierAdapter.add(supplier.name);
        spSupplier.setAdapter(supplierAdapter);

        RecyclerView rv = findViewById(R.id.rvReceiptLines);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReceiptLineAdapter(position -> new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.receipt_line_remove_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> adapter.removeAt(position))
                .show());
        rv.setAdapter(adapter);

        findViewById(R.id.btnAddReceiptLine).setOnClickListener(v -> showAddLineDialog());
        findViewById(R.id.btnSaveReceipt).setOnClickListener(v -> saveReceipt());
    }

    private void showAddLineDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_receipt_line_form, null, false);
        Spinner spProduct = view.findViewById(R.id.spProduct);
        EditText edtQty = view.findViewById(R.id.edtReceiptQty);
        EditText edtCost = view.findViewById(R.id.edtReceiptCost);

        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        for (Product product : products) productAdapter.add(product.tenSanPham);
        spProduct.setAdapter(productAdapter);

        builder.setTitle(R.string.receipt_add_line)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (edtQty.getText().toString().trim().isEmpty() || Integer.parseInt(edtQty.getText().toString().trim()) <= 0) {
                Toast.makeText(this, R.string.receipt_quantity_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (edtCost.getText().toString().trim().isEmpty() || Integer.parseInt(edtCost.getText().toString().trim()) <= 0) {
                Toast.makeText(this, R.string.receipt_cost_required, Toast.LENGTH_SHORT).show();
                return;
            }
            Product product = products.get(spProduct.getSelectedItemPosition());
            ReceiptItem item = new ReceiptItem();
            item.productId = product.maSanPham;
            item.productName = product.tenSanPham;
            item.quantity = Integer.parseInt(edtQty.getText().toString().trim());
            item.unitCost = Integer.parseInt(edtCost.getText().toString().trim());
            item.amount = item.quantity * item.unitCost;
            adapter.addItem(item);
            Toast.makeText(this, R.string.receipt_line_added, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void saveReceipt() {
        if (adapter.getData().isEmpty()) {
            Toast.makeText(this, R.string.receipt_lines_required, Toast.LENGTH_SHORT).show();
            return;
        }
        Spinner spSupplier = findViewById(R.id.spSupplier);
        EditText edtReceiptNote = findViewById(R.id.edtReceiptNote);
        long id = receiptDao.createReceipt(
                suppliers.get(spSupplier.getSelectedItemPosition()).id,
                edtReceiptNote.getText().toString().trim(),
                adapter.getData()
        );
        if (id == -1) {
            Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.receipt_created, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
