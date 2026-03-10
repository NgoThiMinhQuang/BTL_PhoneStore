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
import com.example.phonestore.data.dao.InventoryHistoryDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.InventoryHistoryEntry;
import com.example.phonestore.ui.auth.WelcomeActivity;

import java.util.ArrayList;

public class AdminInventoryHistoryActivity extends BaseHomeActivity {

    private InventoryHistoryDao historyDao;
    private InventoryHistoryAdapter adapter;
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
        historyDao = new InventoryHistoryDao(this);
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
        return getString(R.string.admin_history_title);
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected void onShellReady() {
        ((TextView) findViewById(R.id.tvScreenTitle)).setText(R.string.admin_history_title);
        ((TextView) findViewById(R.id.tvScreenSummary)).setText(R.string.admin_history_summary);
        View cardPrimary = findViewById(R.id.cardPrimaryKpi);
        View cardSecondary = findViewById(R.id.cardSecondaryKpi);
        ((TextView) cardPrimary.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_kpi_best_seller);
        ((TextView) cardSecondary.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_kpi_tracked_lines);

        RecyclerView rv = findViewById(R.id.rvInventoryList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryHistoryAdapter();
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
        ArrayList<InventoryHistoryEntry> list = historyDao.getRecent(20, keyword);
        if (!"ALL".equals(currentFilter)) {
            ArrayList<InventoryHistoryEntry> filtered = new ArrayList<>();
            for (InventoryHistoryEntry entry : list) {
                if (currentFilter.equals(entry.actionType)) filtered.add(entry);
            }
            list = filtered;
        }
        adapter.setData(list);
        View cardPrimary = findViewById(R.id.cardPrimaryKpi);
        View cardSecondary = findViewById(R.id.cardSecondaryKpi);
        int importCount = 0;
        for (InventoryHistoryEntry e : list) if (InventoryHistoryDao.ACTION_IMPORT.equals(e.actionType)) importCount++;
        ((TextView) cardPrimary.findViewById(R.id.tvKpiValue)).setText(String.valueOf(importCount));
        ((TextView) cardSecondary.findViewById(R.id.tvKpiValue)).setText(String.valueOf(list.size()));
    }

    private void setupFilters() {
        LinearLayout container = findViewById(R.id.layoutWarehouseFilters);
        container.removeAllViews();
        addFilterChip(container, getString(R.string.filter_all), "ALL");
        addFilterChip(container, getString(R.string.filter_import), InventoryHistoryDao.ACTION_IMPORT);
        addFilterChip(container, getString(R.string.filter_export), InventoryHistoryDao.ACTION_EXPORT);
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
