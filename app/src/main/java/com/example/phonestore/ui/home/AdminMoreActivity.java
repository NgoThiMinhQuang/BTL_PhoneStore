package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.phonestore.R;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.ui.admin.AdminCustomersActivity;
import com.example.phonestore.ui.admin.AdminReportsActivity;
import com.example.phonestore.ui.auth.WelcomeActivity;

public class AdminMoreActivity extends BaseHomeActivity {

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
        return R.layout.content_admin_more;
    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_admin;
    }

    @Override
    protected int selectedBottomNavItemId() {
        return R.id.nav_admin_more;
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.admin_more);
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected void onShellReady() {
        TextView tvAdminUsername = findViewById(R.id.tvAdminUsername);
        TextView tvAdminRole = findViewById(R.id.tvAdminRole);

        tvAdminUsername.setText(session.getUsername() == null ? "Admin" : session.getUsername());
        tvAdminRole.setText(getString(R.string.admin_role_access, session.getRole() == null ? DBHelper.ROLE_ADMIN : session.getRole()));

        findViewById(R.id.btnOpenCustomers).setOnClickListener(v -> startActivity(new Intent(this, AdminCustomersActivity.class)));
        findViewById(R.id.btnOpenReports).setOnClickListener(v -> startActivity(new Intent(this, AdminReportsActivity.class)));
        findViewById(R.id.btnAdminLogout).setOnClickListener(v -> showLogoutConfirmDialog());
    }
}
