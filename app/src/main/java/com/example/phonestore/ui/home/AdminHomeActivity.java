package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;

import com.example.phonestore.R;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.ui.auth.WelcomeActivity;

public class AdminHomeActivity extends BaseHomeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // chặn nếu không phải admin
        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        }
    }

    @Override
    protected int drawerMenuRes() {
        return R.menu.menu_drawer_admin;
    }

    @Override
    protected String screenTitle() {
        return "Home";
    }
}