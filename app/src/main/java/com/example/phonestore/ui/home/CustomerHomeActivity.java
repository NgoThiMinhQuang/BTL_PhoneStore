package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;

import com.example.phonestore.R;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.ui.auth.WelcomeActivity;

public class CustomerHomeActivity extends BaseHomeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // chặn nếu không phải customer
        if (!session.isLoggedIn() || !DBHelper.ROLE_CUSTOMER.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        }
    }

    @Override
    protected int drawerMenuRes() {
        return R.menu.menu_drawer_customer;
    }

    @Override
    protected String screenTitle() {
        return "Home";
    }
}