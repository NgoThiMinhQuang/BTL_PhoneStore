package com.example.phonestore.ui.home;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.ReceiptDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Receipt;
import com.example.phonestore.data.model.ReceiptItem;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class AdminReceiptDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECEIPT_ID = "extra_receipt_id";

    private ReceiptDao receiptDao;
    private long receiptId;
    private MaterialButton btnDeleteReceipt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_detail);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setTitleMarginStart(Math.round(getResources().getDisplayMetrics().density * 12));
        toolbar.setTitleMarginEnd(0);
        toolbar.setTitleMarginTop(0);
        toolbar.setTitleMarginBottom(0);
        toolbar.setTitle(R.string.receipt_detail_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        receiptId = getIntent().getLongExtra(EXTRA_RECEIPT_ID, -1);
        receiptDao = new ReceiptDao(this);

        if (receiptId == -1) {
            Toast.makeText(this, R.string.receipt_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.btnPrintReceipt).setOnClickListener(v ->
                Toast.makeText(this, R.string.receipt_print_placeholder, Toast.LENGTH_SHORT).show()
        );
        btnDeleteReceipt = findViewById(R.id.btnDeleteReceipt);
        btnDeleteReceipt.setOnClickListener(v -> confirmDelete());

        loadReceipt();
    }

    private void loadReceipt() {
        Receipt receipt = receiptDao.getReceiptById(receiptId);
        if (receipt == null) {
            Toast.makeText(this, R.string.receipt_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ArrayList<ReceiptItem> items = receiptDao.getReceiptItems(receiptId);
        bindHeader(receipt);
        bindItems(items);
        bindSummary(receipt, items);
        bindDeleteRule(receipt);
    }

    private void bindHeader(Receipt receipt) {
        ((TextView) findViewById(R.id.tvReceiptCode)).setText(receipt.getDisplayCode());
        ((TextView) findViewById(R.id.tvReceiptDate)).setText(ReceiptUiFormatter.formatDate(receipt.createdAt));
        ((TextView) findViewById(R.id.tvReceiptSupplier)).setText(valueOrDash(receipt.supplierName));
        ((TextView) findViewById(R.id.tvReceiptCreator)).setText(valueOrDash(receipt.creatorName));
        ((TextView) findViewById(R.id.tvReceiptNote)).setText(getString(
                R.string.receipt_note_value,
                receipt.note == null || receipt.note.trim().isEmpty()
                        ? getString(R.string.receipt_note_empty)
                        : receipt.note.trim()
        ));
        ReceiptUiFormatter.applyStatusBadge((TextView) findViewById(R.id.tvReceiptStatus), receipt.status);
    }

    private void bindItems(ArrayList<ReceiptItem> items) {
        RecyclerView rvReceiptItems = findViewById(R.id.rvReceiptItems);
        rvReceiptItems.setLayoutManager(new LinearLayoutManager(this));
        ReceiptLineAdapter adapter = new ReceiptLineAdapter(false, null);
        adapter.setData(items);
        rvReceiptItems.setAdapter(adapter);
        ((TextView) findViewById(R.id.tvItemsSummary)).setText(getString(R.string.receipt_items_count, items.size()));
    }

    private void bindSummary(Receipt receipt, ArrayList<ReceiptItem> items) {
        int totalQuantity = 0;
        int totalAmount = 0;
        for (ReceiptItem item : items) {
            totalQuantity += item.quantity;
            totalAmount += item.amount;
        }

        ((TextView) findViewById(R.id.tvTotalQuantity)).setText(getString(R.string.receipt_quantity_value, totalQuantity));
        ((TextView) findViewById(R.id.tvTotalAmount)).setText(ReceiptUiFormatter.formatCurrency(this, totalAmount > 0 ? totalAmount : receipt.totalAmount));
    }

    private void confirmDelete() {
        Receipt receipt = receiptDao.getReceiptById(receiptId);
        if (receipt != null && receipt.isCompleted()) {
            Toast.makeText(this, R.string.receipt_delete_completed_blocked, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.receipt_delete_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteReceipt())
                .show();
    }

    private void deleteReceipt() {
        boolean ok = receiptDao.deleteReceipt(receiptId);
        if (!ok) {
            Toast.makeText(this, R.string.receipt_delete_completed_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.receipt_deleted, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void bindDeleteRule(Receipt receipt) {
        if (btnDeleteReceipt == null) {
            return;
        }
        boolean allowDelete = receipt != null && receipt.isDraft();
        btnDeleteReceipt.setEnabled(allowDelete);
        btnDeleteReceipt.setAlpha(allowDelete ? 1f : 0.5f);
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
