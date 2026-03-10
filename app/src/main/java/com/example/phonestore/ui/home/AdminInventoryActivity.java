package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;

import com.example.phonestore.R;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.ui.auth.WelcomeActivity;

public class AdminInventoryActivity extends BaseHomeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        }
    }

    @Override
    protected int shellLayoutRes() {
        return R.layout.activity_home_bottom_admin;
    }

    @Override
    protected int contentLayoutRes() {
        return R.layout.content_admin_inventory_nav;
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
        return getString(R.string.admin_inventory);
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected void onShellReady() {
        findViewById(R.id.btnOpenInventoryOverview).setOnClickListener(v -> startActivity(new Intent(this, AdminInventoryOverviewActivity.class)));
        findViewById(R.id.btnOpenReceipts).setOnClickListener(v -> startActivity(new Intent(this, AdminReceiptsActivity.class)));
        findViewById(R.id.btnOpenSuppliers).setOnClickListener(v -> startActivity(new Intent(this, AdminSuppliersActivity.class)));
        findViewById(R.id.btnOpenAlerts).setOnClickListener(v -> startActivity(new Intent(this, AdminInventoryAlertsActivity.class)));
        findViewById(R.id.btnOpenHistory).setOnClickListener(v -> startActivity(new Intent(this, AdminInventoryHistoryActivity.class)));
    }
}
