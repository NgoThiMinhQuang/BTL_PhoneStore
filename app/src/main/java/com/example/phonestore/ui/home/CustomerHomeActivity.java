package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.phonestore.R;
import com.example.phonestore.ui.profile.ProfileActivity;

public class CustomerHomeActivity extends BaseHomeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View avatar = findViewById(R.id.cardAvatarProfile);
        if (avatar != null) {
            avatar.setOnClickListener(v -> openBottomTab(new Intent(this, ProfileActivity.class)));
        }

        View btnNotification = findViewById(R.id.btnHomeNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> Toast.makeText(this, "Hiện chưa có thông báo mới", Toast.LENGTH_SHORT).show());
        }

        View btnLogout = findViewById(R.id.btnHomeLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());
        }
    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_customer;
    }

    @Override
    protected String screenTitle() {
        return "";
    }

    @Override
    protected int shellLayoutRes() {
        return R.layout.activity_home_bottom_customer;
    }

    @Override
    protected boolean shouldSetupHomeInteractions() {
        return true;
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }
}