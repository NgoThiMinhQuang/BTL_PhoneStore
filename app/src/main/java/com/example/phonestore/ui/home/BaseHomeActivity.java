package com.example.phonestore.ui.home;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.phonestore.data.dao.UserDao;
import com.example.phonestore.data.model.User;

import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
    private View homeSearchCard;
    private boolean suppressBottomNavNavigation;

    @LayoutRes
    protected int shellLayoutRes() {
        return R.layout.activity_home_bottom;
    }

    @LayoutRes
    protected int contentLayoutRes() {
        return R.layout.content_home;
    }

    @MenuRes
    protected abstract int bottomMenuRes();

    protected abstract String screenTitle();

    protected int selectedBottomNavItemId() {
        return R.id.nav_home;
    }

    protected boolean shouldSetupHomeInteractions() {
        return false;
    }

    protected boolean shouldShowToolbarActions() {
        return true;
    }

    @MenuRes
    protected int toolbarMenuRes() {
        return shouldShowToolbarActions() ? R.menu.menu_home_actions : 0;
    }

    protected boolean shouldShowBackButton() {
        return false;
    }

    protected boolean shouldUseAdminBackButtonStyling() {
        return false;
    }

    protected boolean shouldShowBottomNavigation() {
        return true;
    }

    protected boolean isBottomNavRootScreen() {
        return true;
    }

    protected void onShellReady() {
        // subclasses may override
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        setContentView(shellLayoutRes());

        inflateContentLayoutIfNeeded();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setContentInsetsAbsolute(0, 0);
        int titleMarginStart = Math.round(getResources().getDisplayMetrics().density * (shouldShowBackButton() ? 8 : 12));
        toolbar.setTitleMarginStart(titleMarginStart);
        toolbar.setTitleMarginEnd(0);
        toolbar.setTitleMarginTop(0);
        toolbar.setTitleMarginBottom(0);
        toolbar.setTitle(screenTitle());
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(shouldShowBackButton());
            if (shouldShowBackButton() || shouldUseAdminBackButtonStyling()) {
                Drawable upArrow = toolbar.getNavigationIcon();
                if (upArrow != null) {
                    upArrow.setTint(getColor(android.R.color.white));
                }
            }
        }

        setupBottomNavigation();
        updateShellNavigationVisibility();
        bindCustomerHomeHeader();

        if (shouldSetupHomeInteractions()) {
            setupHomeBrandClicks();
            setupHomeScrollSupport();
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        onShellReady();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncBottomNavigationSelection();
        updateShellNavigationVisibility();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        syncBottomNavigationSelection();
        updateShellNavigationVisibility();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int menuRes = toolbarMenuRes();
        if (menuRes == 0) return true;

        getMenuInflater().inflate(menuRes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

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
    public boolean onSupportNavigateUp() {
        if (shouldShowBackButton()) {
            finish();
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (handleHomeWheelScroll(event)) return true;
        return super.dispatchGenericMotionEvent(event);
    }

    protected void openBottomTab(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    protected void showLogoutConfirmDialog() {
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

    protected boolean isAdminSession() {
        return DBHelper.ROLE_ADMIN.equals(getSessionRole());
    }

    protected String getSessionRole() {
        if (session != null) {
            return session.getRole();
        }
        return new SessionManager(this).getRole();
    }

    private void inflateContentLayoutIfNeeded() {
        int contentLayout = contentLayoutRes();
        if (contentLayout == 0) return;

        ViewGroup container = findViewById(R.id.homeContentContainer);
        if (container == null) return;

        container.removeAllViews();
        LayoutInflater.from(this).inflate(contentLayout, container, true);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.getMenu().clear();
            bottomNav.inflateMenu(bottomMenuRes());
            bottomNav.setItemIconTintList(null);
            bottomNav.setOnItemSelectedListener(item -> suppressBottomNavNavigation || handleBottomNavigation(item.getItemId()));
            bottomNav.setOnItemReselectedListener(item -> {
                if (!suppressBottomNavNavigation) {
                    handleBottomNavigation(item.getItemId());
                }
            });
            syncBottomNavigationSelection();
            return;
        }

        int[] adminTabIds = new int[]{
                R.id.nav_home,
                R.id.nav_orders_admin,
                R.id.nav_products,
                R.id.nav_admin_inventory,
                R.id.nav_admin_customers,
                R.id.nav_admin_more
        };
        for (int tabId : adminTabIds) {
            View tab = findViewById(tabId);
            if (tab == null) continue;
            tab.setSelected(tabId == selectedBottomNavItemId());
            tab.setOnClickListener(v -> handleBottomNavigation(v.getId()));
        }
    }

    private void syncBottomNavigationSelection() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) {
            return;
        }
        suppressBottomNavNavigation = true;
        bottomNav.setSelectedItemId(selectedBottomNavItemId());
        suppressBottomNavNavigation = false;
    }

    private void updateShellNavigationVisibility() {
        int visibility = shouldShowBottomNavigation() ? View.VISIBLE : View.GONE;

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setVisibility(visibility);
        }

        View adminBottomBar = findViewById(R.id.adminBottomBar);
        if (adminBottomBar != null) {
            adminBottomBar.setVisibility(visibility);
        }
    }

    private void bindCustomerHomeHeader() {
        TextView tvHomeUserName = findViewById(R.id.tvHomeUserName);
        if (tvHomeUserName == null) return;

        TextView tvHomeGreeting = findViewById(R.id.tvHomeGreeting);
        if (tvHomeGreeting != null) {
            tvHomeGreeting.setText("Hello, Welcome");
        }

        String fallbackName = session == null ? "PhoneStore" : session.getUsername();
        String displayName = fallbackName;
        if (session != null && session.getUserId() > 0) {
            User user = new UserDao(this).getById(session.getUserId());
            if (user != null) {
                String fullname = user.fullname == null ? "" : user.fullname.trim();
                displayName = fullname.isEmpty() ? user.username : fullname;
            }
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "PhoneStore";
        }
        tvHomeUserName.setText(displayName);
    }

    private boolean handleBottomNavigation(int id) {
        if (id == selectedBottomNavItemId() && isBottomNavRootScreen()) {
            return true;
        }

        if (id == R.id.nav_home) {
            openHomeTab();
            return true;
        }

        if (id == R.id.nav_products) {
            if (isAdminSession()) {
                openBottomTab(new Intent(this, AdminProductsActivity.class));
            } else {
                openBottomTab(new Intent(this, ProductsActivity.class));
            }
            return true;
        }

        if (id == R.id.nav_cart) {
            if (!session.isLoggedIn()) {
                redirectToWelcome();
                return true;
            }
            openBottomTab(new Intent(this, CartActivity.class));
            return true;
        }

        if (id == R.id.nav_orders) {
            openBottomTab(new Intent(this, OrdersActivity.class));
            return true;
        }

        if (id == R.id.nav_orders_admin) {
            openBottomTab(new Intent(this, AdminOrdersActivity.class));
            return true;
        }

        if (id == R.id.nav_profile) {
            openBottomTab(new Intent(this, ProfileActivity.class));
            return true;
        }

        if (id == R.id.nav_admin_inventory) {
            openBottomTab(new Intent(this, AdminInventoryActivity.class));
            return true;
        }

        if (id == R.id.nav_admin_customers) {
            openBottomTab(new Intent(this, AdminCustomersActivity.class));
            return true;
        }

        if (id == R.id.nav_admin_more) {
            openBottomTab(new Intent(this, AdminReportsActivity.class));
            return true;
        }

        return false;
    }

    private void openHomeTab() {
        Intent intent = new Intent(this, isAdminSession() ? AdminHomeActivity.class : CustomerHomeActivity.class);
        openBottomTab(intent);
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
            String brandKey = String.valueOf(v.getTag()).trim();
            if (brandKey.isEmpty()) return;

            String title = brandKey;
            if ("Apple".equalsIgnoreCase(brandKey)) {
                title = "Apple";
            }

            openProducts(brandKey, title);
        };

        View tvSamsung = findViewById(R.id.tvBrandSamsung);
        if (tvSamsung != null) tvSamsung.setOnClickListener(brandClick);

        View tvIphone = findViewById(R.id.tvBrandIphone);
        if (tvIphone != null) tvIphone.setOnClickListener(brandClick);

        View tvXiaomi = findViewById(R.id.tvBrandXiaomi);
        if (tvXiaomi != null) tvXiaomi.setOnClickListener(brandClick);

        View tvOppo = findViewById(R.id.tvBrandOppo);
        if (tvOppo != null) tvOppo.setOnClickListener(brandClick);
    }

    private void openProducts(@Nullable String brandKey, @Nullable String title) {
        Intent i = new Intent(this, ProductsActivity.class);
        if (brandKey != null && !brandKey.isEmpty()) i.putExtra(ProductsActivity.EXTRA_BRAND, brandKey);
        if (title != null && !title.isEmpty()) i.putExtra(ProductsActivity.EXTRA_TITLE, title);
        startActivity(i);
    }

    private void openQuickCheckout() {
        openBottomTab(new Intent(this, ProductsActivity.class));
    }

    private void redirectToWelcome() {
        Toast.makeText(this, "Vui lòng đăng nhập hoặc đăng ký để tiếp tục", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, WelcomeActivity.class));
    }

    private void setupHomeScrollSupport() {
        homeScroll = findViewById(R.id.homeScroll);
        if (homeScroll == null) return;

        homeSearchCard = findViewById(R.id.cardHomeSearch);
        homeScroll.setFillViewport(true);
        homeScroll.setFocusableInTouchMode(true);
        homeScroll.requestFocus();
        homeScroll.setClipToOutline(true);

        View content = homeScroll.getChildAt(0);
        if (content != null) {
            content.setFocusableInTouchMode(true);
            content.requestFocus();
        }

        homeScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> updateHomeSearchOffset(scrollY));
        homeScroll.post(() -> updateHomeSearchOffset(homeScroll.getScrollY()));
        homeScroll.setOnGenericMotionListener((v, event) -> handleHomeWheelScroll(event));
    }

    private void updateHomeSearchOffset(int scrollY) {
        if (homeSearchCard == null) return;

        float density = getResources().getDisplayMetrics().density;
        homeSearchCard.setZ((scrollY == 0 ? 14f : 2f) * density);
        homeSearchCard.setTranslationY(-Math.min(scrollY, homeSearchCard.getBottom()));
    }

    private boolean handleHomeWheelScroll(MotionEvent event) {
        if (homeScroll == null) return false;
        if (event.getAction() != MotionEvent.ACTION_SCROLL) return false;
        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) == 0) return false;

        float vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
        if (vScroll == 0f) return false;

        int delta = (int) (vScroll * ViewConfiguration.get(this).getScaledVerticalScrollFactor());
        if (delta == 0) delta = vScroll > 0f ? 1 : -1;

        View child = homeScroll.getChildAt(0);
        if (child == null) return false;

        int targetY = homeScroll.getScrollY() - delta;
        int maxY = Math.max(0, child.getHeight() - homeScroll.getHeight());
        targetY = Math.max(0, Math.min(targetY, maxY));

        if (targetY == homeScroll.getScrollY()) return false;

        homeScroll.scrollTo(0, targetY);
        return true;
    }
}
