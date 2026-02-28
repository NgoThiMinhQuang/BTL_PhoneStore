package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.phonestore.R;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.profile.ProfileActivity;
import com.example.phonestore.ui.products.ProductsActivity;
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

        // handle drawer click
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

            if (id == R.id.nav_products) {
                openProducts(null, null);
                drawerLayout.closeDrawers();
                return true;
            }

            Toast.makeText(this, "Click: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            drawerLayout.closeDrawers();
            return true;
        });

        // ✅ NEW: click Explore Brands + View all
        setupHomeBrandClicks();
    }

    private void setupHomeBrandClicks() {
        // View all ở Popular/Brands
        TextView tvViewAllPopular = findViewById(R.id.tvViewAllPopular);
        if (tvViewAllPopular != null) tvViewAllPopular.setOnClickListener(v -> openProducts(null, null));

        TextView tvViewAllBrands = findViewById(R.id.tvViewAllBrands);
        if (tvViewAllBrands != null) tvViewAllBrands.setOnClickListener(v -> openProducts(null, null));

        // Click brand chip
        View.OnClickListener brandClick = v -> {
            if (!(v instanceof TextView)) return;
            TextView tv = (TextView) v;

            String title = tv.getText().toString().trim();      // Samsung / iPhone / Xiaomi / OPPO
            String brandKey = String.valueOf(tv.getTag()).trim(); // Samsung / Apple / Xiaomi / OPPO (để query DB)

            openProducts(brandKey, title);
        };

        TextView tvSamsung = findViewById(R.id.tvBrandSamsung);
        if (tvSamsung != null) tvSamsung.setOnClickListener(brandClick);

        TextView tvIphone = findViewById(R.id.tvBrandIphone);
        if (tvIphone != null) tvIphone.setOnClickListener(brandClick);

        TextView tvXiaomi = findViewById(R.id.tvBrandXiaomi);
        if (tvXiaomi != null) tvXiaomi.setOnClickListener(brandClick);

        TextView tvOppo = findViewById(R.id.tvBrandOppo);
        if (tvOppo != null) tvOppo.setOnClickListener(brandClick);
    }

    private void openProducts(@Nullable String brandKey, @Nullable String title) {
        Intent i = new Intent(this, ProductsActivity.class);
        if (brandKey != null && !brandKey.isEmpty()) i.putExtra(ProductsActivity.EXTRA_BRAND, brandKey);
        if (title != null && !title.isEmpty()) i.putExtra(ProductsActivity.EXTRA_TITLE, title);
        startActivity(i);
    }
}