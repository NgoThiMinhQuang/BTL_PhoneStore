package com.example.phonestore.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
    private MaterialButton btnConfirmReceipt;
    private Receipt currentReceipt;
    private ArrayList<ReceiptItem> currentItems = new ArrayList<>();
    private WebView printWebView;

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
        toolbar.setTitle(getString(R.string.receipt_detail_title));
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

        findViewById(R.id.btnPrintReceipt).setOnClickListener(v -> printReceipt());
        btnDeleteReceipt = findViewById(R.id.btnDeleteReceipt);
        btnDeleteReceipt.setOnClickListener(v -> confirmDelete());
        btnConfirmReceipt = findViewById(R.id.btnConfirmReceipt);
        btnConfirmReceipt.setOnClickListener(v -> confirmReceipt());

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
        currentReceipt = receipt;
        currentItems = items;
        bindHeader(receipt);
        bindItems(items);
        bindSummary(receipt, items);
        bindActions(receipt);
    }

    private void bindHeader(Receipt receipt) {
        String receiptCode = receipt == null ? getString(R.string.receipt_detail_title) : receipt.getDisplayCode();
        ((TextView) findViewById(R.id.tvReceiptCode)).setText(receiptCode);
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

    private void confirmReceipt() {
        Receipt receipt = receiptDao.getReceiptById(receiptId);
        if (receipt == null) {
            Toast.makeText(this, R.string.receipt_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!receipt.isDraft()) {
            Toast.makeText(this, R.string.receipt_confirm_completed_blocked, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.receipt_confirm)
                .setMessage(R.string.receipt_confirm_detail_prompt)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.receipt_confirm, (dialog, which) -> submitConfirmReceipt())
                .show();
    }

    private void submitConfirmReceipt() {
        boolean ok = receiptDao.confirmDraftReceipt(receiptId);
        if (!ok) {
            Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.receipt_confirmed, Toast.LENGTH_SHORT).show();
        loadReceipt();
    }

    private void bindActions(Receipt receipt) {
        boolean isDraft = receipt != null && receipt.isDraft();
        if (btnDeleteReceipt != null) {
            btnDeleteReceipt.setEnabled(isDraft);
            btnDeleteReceipt.setAlpha(isDraft ? 1f : 0.5f);
        }
        if (btnConfirmReceipt != null) {
            btnConfirmReceipt.setEnabled(isDraft);
            btnConfirmReceipt.setVisibility(isDraft ? View.VISIBLE : View.GONE);
        }
    }

    private void printReceipt() {
        if (currentReceipt == null) {
            Toast.makeText(this, R.string.receipt_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(this, R.string.receipt_print_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        String html = buildReceiptPrintHtml(currentReceipt, currentItems);
        String jobName = getString(R.string.receipt_print_job_name, currentReceipt.getDisplayCode());
        printWebView = new WebView(this);
        printWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(jobName);
                PrintAttributes attributes = new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build();
                printManager.print(jobName, adapter, attributes);
            }
        });
        printWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private String buildReceiptPrintHtml(Receipt receipt, ArrayList<ReceiptItem> items) {
        ArrayList<ReceiptItem> safeItems = items == null ? new ArrayList<>() : items;
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < safeItems.size(); i++) {
            ReceiptItem item = safeItems.get(i);
            rows.append("<tr>")
                    .append("<td class='center'>").append(i + 1).append("</td>")
                    .append("<td>").append(escapeHtml(valueOrDash(item.productName))).append("</td>")
                    .append("<td class='number'>").append(Math.max(0, item.quantity)).append("</td>")
                    .append("<td class='number'>").append(escapeHtml(ReceiptUiFormatter.formatCurrency(this, Math.max(0, item.unitCost)))).append("</td>")
                    .append("<td class='number'>").append(escapeHtml(ReceiptUiFormatter.formatCurrency(this, Math.max(0, item.amount)))).append("</td>")
                    .append("</tr>");
        }
        if (rows.length() == 0) {
            rows.append("<tr><td colspan='5' class='empty'>Không có sản phẩm</td></tr>");
        }

        String note = receipt.note == null || receipt.note.trim().isEmpty()
                ? getString(R.string.receipt_note_empty)
                : receipt.note.trim();
        int totalQuantity = calculateTotalQuantity(safeItems);
        int totalAmount = calculateTotalAmount(receipt, safeItems);

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                "<style>" +
                "@page{size:A4;margin:16mm;}" +
                "body{font-family:sans-serif;color:#111827;font-size:13px;line-height:1.45;}" +
                ".header{text-align:center;margin-bottom:20px;}" +
                ".store{font-size:14px;font-weight:700;color:#E11D48;}" +
                "h1{font-size:24px;margin:8px 0 4px;text-transform:uppercase;}" +
                ".code{font-size:15px;font-weight:700;}" +
                ".grid{display:grid;grid-template-columns:1fr 1fr;gap:10px 28px;margin:18px 0;}" +
                ".label{color:#6B7280;font-size:12px;}" +
                ".value{font-weight:700;margin-top:2px;}" +
                ".note{border:1px solid #FFE4E6;border-radius:10px;padding:10px;margin:12px 0 18px;}" +
                "table{width:100%;border-collapse:collapse;margin-top:10px;}" +
                "th{background:#FFF1F2;color:#9F1239;text-align:left;}" +
                "th,td{border:1px solid #FECFDA;padding:8px;vertical-align:top;}" +
                ".center{text-align:center;}" +
                ".number{text-align:right;white-space:nowrap;}" +
                ".empty{text-align:center;color:#6B7280;}" +
                ".summary{margin-top:18px;margin-left:auto;width:55%;}" +
                ".summary-row{display:flex;justify-content:space-between;border-bottom:1px solid #FFE4E6;padding:8px 0;}" +
                ".summary-row:last-child{border-bottom:0;font-size:16px;font-weight:700;color:#E11D48;}" +
                ".signatures{display:flex;justify-content:space-between;margin-top:46px;text-align:center;font-weight:700;}" +
                ".signature-note{font-weight:400;color:#6B7280;font-style:italic;margin-top:4px;}" +
                "</style></head><body>" +
                "<div class='header'>" +
                "<div class='store'>" + escapeHtml(getString(R.string.app_name)) + "</div>" +
                "<h1>" + escapeHtml(getString(R.string.receipt_print_title)) + "</h1>" +
                "<div class='code'>" + escapeHtml(receipt.getDisplayCode()) + "</div>" +
                "</div>" +
                "<div class='grid'>" +
                infoCell("Ngày tạo", ReceiptUiFormatter.formatDate(receipt.createdAt)) +
                infoCell("Trạng thái", ReceiptUiFormatter.formatStatus(this, receipt.status)) +
                infoCell("Nhà cung cấp", valueOrDash(receipt.supplierName)) +
                infoCell("Người tạo", valueOrDash(receipt.creatorName)) +
                "</div>" +
                "<div class='note'><div class='label'>Ghi chú</div><div class='value'>" + escapeHtml(note) + "</div></div>" +
                "<table><thead><tr>" +
                "<th class='center'>STT</th><th>Sản phẩm</th><th class='number'>Số lượng</th><th class='number'>Giá nhập</th><th class='number'>Thành tiền</th>" +
                "</tr></thead><tbody>" + rows + "</tbody></table>" +
                "<div class='summary'>" +
                "<div class='summary-row'><span>Tổng số lượng</span><strong>" + totalQuantity + " máy</strong></div>" +
                "<div class='summary-row'><span>Tổng chi phí</span><strong>" + escapeHtml(ReceiptUiFormatter.formatCurrency(this, totalAmount)) + "</strong></div>" +
                "</div>" +
                "<div class='signatures'>" +
                "<div>Người lập phiếu<div class='signature-note'>(Ký, ghi rõ họ tên)</div></div>" +
                "<div>Nhà cung cấp<div class='signature-note'>(Ký, ghi rõ họ tên)</div></div>" +
                "</div>" +
                "</body></html>";
    }

    private String infoCell(String label, String value) {
        return "<div><div class='label'>" + escapeHtml(label) + "</div><div class='value'>" + escapeHtml(valueOrDash(value)) + "</div></div>";
    }

    private int calculateTotalQuantity(ArrayList<ReceiptItem> items) {
        int total = 0;
        for (ReceiptItem item : items) {
            total += Math.max(0, item.quantity);
        }
        return total;
    }

    private int calculateTotalAmount(Receipt receipt, ArrayList<ReceiptItem> items) {
        int total = 0;
        for (ReceiptItem item : items) {
            total += Math.max(0, item.amount);
        }
        return total > 0 ? total : Math.max(0, receipt.totalAmount);
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
