package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.ReceiptDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Receipt;
import com.example.phonestore.ui.auth.WelcomeActivity;

import java.util.ArrayList;
import java.util.Locale;

public class AdminReceiptsActivity extends BaseHomeActivity {

    private static final String FILTER_ALL = "ALL";
    private static final String FILTER_DRAFT = Receipt.STATUS_DRAFT;
    private static final String FILTER_COMPLETED = Receipt.STATUS_COMPLETED;

    private ReceiptDao receiptDao;
    private ReceiptAdapter adapter;
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
        receiptDao = new ReceiptDao(this);
    }

    @Override
    protected int shellLayoutRes() {
        return R.layout.activity_admin_receipts;
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
    protected boolean shouldShowBackButton() {
        return true;
    }

    @Override
    protected boolean shouldUseAdminBackButtonStyling() {
        return true;
    }

    @Override
    protected void onShellReady() {
        RecyclerView rvReceipts = findViewById(R.id.rvReceipts);
        rvReceipts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReceiptAdapter(receipt -> {
            Intent intent = new Intent(this, AdminReceiptDetailActivity.class);
            intent.putExtra(AdminReceiptDetailActivity.EXTRA_RECEIPT_ID, receipt.id);
            startActivity(intent);
        });
        rvReceipts.setAdapter(adapter);

        findViewById(R.id.btnAddReceipt).setOnClickListener(v -> startActivity(new Intent(this, AdminReceiptEditorActivity.class)));
        setupFilters();

        EditText edtSearch = findViewById(R.id.edtReceiptSearch);
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                loadData(s.toString().trim());
            }
        });

        loadData("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (receiptDao != null) {
            EditText edtSearch = findViewById(R.id.edtReceiptSearch);
            loadData(edtSearch == null ? "" : edtSearch.getText().toString().trim());
        }
    }

    private void loadData(String keyword) {
        ArrayList<Receipt> receipts = receiptDao.getRecentReceipts();
        ArrayList<Receipt> filtered = new ArrayList<>();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        for (Receipt receipt : receipts) {
            if (!matchesFilter(receipt)) {
                continue;
            }
            if (!matchesKeyword(receipt, normalizedKeyword)) {
                continue;
            }
            filtered.add(receipt);
        }

        adapter.setData(filtered);
    }

    private boolean matchesFilter(Receipt receipt) {
        if (FILTER_ALL.equals(currentFilter)) {
            return true;
        }
        return currentFilter.equals(receipt.status);
    }

    private boolean matchesKeyword(Receipt receipt, String keyword) {
        if (keyword.isEmpty()) {
            return true;
        }

        String code = receipt.getDisplayCode().toLowerCase(Locale.ROOT);
        String supplier = safeLower(receipt.supplierName);
        String note = safeLower(receipt.note);
        return code.contains(keyword) || supplier.contains(keyword) || note.contains(keyword);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void setupFilters() {
        LinearLayout container = findViewById(R.id.layoutReceiptFilters);
        container.removeAllViews();
        addFilterChip(container, getString(R.string.filter_all), FILTER_ALL);
        addFilterChip(container, getString(R.string.receipt_status_draft), FILTER_DRAFT);
        addFilterChip(container, getString(R.string.receipt_status_completed), FILTER_COMPLETED);
    }

    private void addFilterChip(LinearLayout container, String label, String filter) {
        TextView chip = (TextView) LayoutInflater.from(this).inflate(R.layout.item_filter_chip, container, false);
        chip.setText(label);
        chip.setSelected(filter.equals(currentFilter));
        chip.setTextColor(getColor(filter.equals(currentFilter) ? android.R.color.white : R.color.admin_text_secondary));
        chip.setOnClickListener(v -> {
            currentFilter = filter;
            setupFilters();
            EditText edtSearch = findViewById(R.id.edtReceiptSearch);
            loadData(edtSearch.getText().toString().trim());
        });
        container.addView(chip);
    }
}
