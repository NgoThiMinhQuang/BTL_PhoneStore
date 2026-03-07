package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.phonestore.R;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.ui.admin.AdminCustomersActivity;
import com.example.phonestore.ui.admin.AdminProductsActivity;
import com.example.phonestore.ui.admin.AdminReportsActivity;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.cart.CartActivity;
import com.example.phonestore.ui.checkout.CheckoutActivity;
import com.example.phonestore.ui.orders.OrdersActivity;
import com.example.phonestore.ui.products.ProductsActivity;
import com.example.phonestore.ui.profile.ProfileActivity;
import com.example.phonestore.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseHomeActivity extends AppCompatActivity {

    protected SessionManager session;
    private ScrollView homeScroll;

    @MenuRes
    protected abstract int bottomMenuRes();

    protected abstract String screenTitle();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_bottom);

        session = new SessionManager(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(screenTitle());
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(bottomMenuRes());

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            }

            if (id == R.id.nav_products) {
                if (DBHelper.ROLE_ADMIN.equals(session.getRole())) {
                    Intent i = new Intent(this, AdminProductsActivity.class);
                    openBottomTab(i);
                } else {
                    Intent i = new Intent(this, ProductsActivity.class);
                    openBottomTab(i);
                }
                return true;
            }

            if (id == R.id.nav_cart) {
                if (!session.isLoggedIn()) {
                    redirectToWelcome();
                    return true;
                }
                Intent i = new Intent(this, CartActivity.class);
                openBottomTab(i);
                return true;
            }

            if (id == R.id.nav_orders) {
                Intent i = new Intent(this, OrdersActivity.class);
                openBottomTab(i);
                return true;
            }

            if (id == R.id.nav_orders_admin) {
                Intent i = new Intent(this, OrdersActivity.class);
                i.putExtra(OrdersActivity.EXTRA_ADMIN_MODE, true);
                openBottomTab(i);
                return true;
            }

            if (id == R.id.nav_profile) {
                Intent i = new Intent(this, ProfileActivity.class);
                openBottomTab(i);
                return true;
            }

            if (id == R.id.nav_admin_customers) {
                Intent i = new Intent(this, AdminCustomersActivity.class);
                openBottomTab(i);
                return true;
            }

            if (id == R.id.nav_admin_reports) {
                Intent i = new Intent(this, AdminReportsActivity.class);
                openBottomTab(i);
                return true;
            }

            return false;
        });

        bottomNav.setOnItemReselectedListener(item -> {
            // no-op
        });
        bottomNav.setSelectedItemId(R.id.nav_home);

        setupHomeBrandClicks();
        setupHomeScrollSupport();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }

        if (id == R.id.nav_logout) {
            showLogoutConfirmDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (handleHomeWheelScroll(event)) return true;
        return super.dispatchGenericMotionEvent(event);
    }

    private void openBottomTab(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    session.clear();
                    startActivity(new Intent(this, WelcomeActivity.class));
                    finish();
                })
                .show();
    }

    private void setupHomeBrandClicks() {
        TextView tvViewAllPopular = findViewById(R.id.tvViewAllPopular);
        if (tvViewAllPopular != null) tvViewAllPopular.setOnClickListener(v -> openProducts(null, null));

        View btnBannerBuyNow = findViewById(R.id.btnBannerBuyNow);
        if (btnBannerBuyNow != null) {
            btnBannerBuyNow.setOnClickListener(v -> openQuickCheckout());
        }

        TextView tvViewAllBrands = findViewById(R.id.tvViewAllBrands);
        if (tvViewAllBrands != null) tvViewAllBrands.setOnClickListener(v -> openProducts(null, null));

        View.OnClickListener brandClick = v -> {
            if (!(v instanceof TextView)) return;
            TextView tv = (TextView) v;

            String title = tv.getText().toString().trim();
            String brandKey = String.valueOf(tv.getTag()).trim();

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

    private void openQuickCheckout() {
        if (!session.isLoggedIn()) {
            redirectToWelcome();
            return;
        }
        startActivity(new Intent(this, CheckoutActivity.class));
    }

    private void redirectToWelcome() {
        Toast.makeText(this, "Vui lòng đăng nhập hoặc đăng ký để tiếp tục", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, WelcomeActivity.class));
    }

    private void setupHomeScrollSupport() {
        homeScroll = findViewById(R.id.homeScroll);
        if (homeScroll == null) return;

        homeScroll.setFillViewport(true);
        homeScroll.setFocusableInTouchMode(true);
        homeScroll.requestFocus();

        View content = homeScroll.getChildAt(0);
        if (content != null) {
            content.setFocusableInTouchMode(true);
            content.requestFocus();
        }

        homeScroll.setOnGenericMotionListener((v, event) -> handleHomeWheelScroll(event));
    }

    private boolean handleHomeWheelScroll(MotionEvent event) {
        if (homeScroll == null) return false;
        if (event.getAction() != MotionEvent.ACTION_SCROLL) return false;
        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) == 0) return false;

        float vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
        if (vScroll == 0f) return false;

        int delta = (int) (vScroll * ViewConfiguration.get(this).getScaledVerticalScrollFactor());
        if (delta == 0) delta = vScroll > 0f ? 1 : -1;

        int targetY = homeScroll.getScrollY() - delta;
        int maxY = Math.max(0, homeScroll.getChildAt(0).getHeight() - homeScroll.getHeight());
        targetY = Math.max(0, Math.min(targetY, maxY));

        if (targetY == homeScroll.getScrollY()) return false;

        homeScroll.scrollTo(0, targetY);
        return true;
    }
}
