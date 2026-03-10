package com.example.phonestore.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.ReceiptDao;
import com.example.phonestore.data.model.Receipt;
import com.example.phonestore.data.model.ReceiptItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class AdminReceiptDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECEIPT_ID = "extra_receipt_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.receipt_detail_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        long receiptId = getIntent().getLongExtra(EXTRA_RECEIPT_ID, -1);
        ReceiptDao receiptDao = new ReceiptDao(this);
        ArrayList<Receipt> receipts = receiptDao.getRecentReceipts();
        Receipt target = null;
        for (Receipt receipt : receipts) if (receipt.id == receiptId) target = receipt;
        ArrayList<ReceiptItem> items = receiptDao.getReceiptItems(receiptId);

        TextView tvReceiptHeader = findViewById(R.id.tvReceiptHeader);
        TextView tvReceiptSummary = findViewById(R.id.tvReceiptSummary);
        long finalReceiptId = receiptId;
        if (target != null) {
            tvReceiptHeader.setText(getString(
                    R.string.receipt_header,
                    target.id,
                    target.supplierName,
                    getString(R.string.admin_price_currency, NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(target.totalAmount))
            ));
            StringBuilder summary = new StringBuilder();
            summary.append(getString(R.string.receipt_summary_qty, target.totalQuantity));
            if (target.note != null && !target.note.trim().isEmpty()) {
                summary.append("\n").append(getString(R.string.receipt_summary_note, target.note.trim()));
            }
            tvReceiptSummary.setText(summary.toString());
        }

        tvReceiptHeader.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete)
                    .setMessage(R.string.receipt_delete_confirm)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        boolean ok = receiptDao.deleteReceipt(finalReceiptId);
                        if (!ok) {
                            Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(this, R.string.receipt_deleted, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .show();
            return true;
        });

        RecyclerView rv = findViewById(R.id.rvReceiptItems);
        rv.setLayoutManager(new LinearLayoutManager(this));
        ReceiptLineAdapter adapter = new ReceiptLineAdapter();
        adapter.setData(items);
        rv.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
