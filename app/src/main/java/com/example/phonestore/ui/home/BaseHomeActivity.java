package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.MenuRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.phonestore.R;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.profile.ProfileActivity;   // <-- THÊM DÒNG NÀY
import com.example.phonestore.utils.SessionManager;
import com.google.android.material.navigation.NavigationView;

public abstract class BaseHomeActivity extends AppCompatActivity {

    protected SessionManager session;
    private DrawerLayout drawerLayout;
    private NavigationView navView;

    @MenuRes
    protected abstract int drawerMenuRes();

    protected abstract String screenTitle();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_drawer);

        session = new SessionManager(this);

        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navigationView);

        // set title
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(screenTitle());
        setSupportActionBar(toolbar);

        // toggle hamburger
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // set menu by role
        Menu menu = navView.getMenu();
        menu.clear();
        navView.inflateMenu(drawerMenuRes());

        // set header info
        TextView tvName = navView.getHeaderView(0).findViewById(R.id.tvHeaderName);
        TextView tvSub = navView.getHeaderView(0).findViewById(R.id.tvHeaderSub);
        tvName.setText("PHONE STORE");
        tvSub.setText(session.getUsername() + " (" + session.getRole() + ")");

        // handle click
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_logout) {
                session.clear();
                startActivity(new Intent(this, WelcomeActivity.class));
                finish();
                return true;
            }

            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                drawerLayout.closeDrawers();
                return true;
            }

            // tạm thời: bạn chưa làm các màn con thì show Toast
            Toast.makeText(this, "Click: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            drawerLayout.closeDrawers();
            return true;
        });
    }
}