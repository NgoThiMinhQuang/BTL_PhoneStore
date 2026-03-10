package com.example.phonestore.ui.home;

import android.os.Bundle;

import com.example.phonestore.R;

public class CustomerHomeActivity extends BaseHomeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_customer;
    }

    @Override
    protected String screenTitle() {
        return "Trang chủ";
    }

    @Override
    protected boolean shouldSetupHomeInteractions() {
        return true;
    }
}