package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.ProductDao;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.ui.products.ProductAdapter;
import com.example.phonestore.ui.products.ProductsActivity;
import com.example.phonestore.ui.profile.ProfileActivity;

import java.util.ArrayList;
import java.util.Calendar;

public class CustomerHomeActivity extends BaseHomeActivity {

    private static final String SORT_DISCOUNT_DESC = "discount_desc";
    private static final int BANNER_INTERVAL_MS = 3000;

    private final int[] homeBanners = {
            R.drawable.drawable_anh_uu_dai,
            R.drawable.dr_banner4,
            R.drawable.dr_banner5
    };
    private int currentBannerIndex;
    private FrameLayout homeBannerContainer;
    private ImageView ivHomeBannerCurrent;
    private ImageView ivHomeBannerNext;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private final Runnable bannerTicker = new Runnable() {
        @Override
        public void run() {
            showNextBanner();
            bannerHandler.postDelayed(this, BANNER_INTERVAL_MS);
        }
    };

    private final Handler flashSaleHandler = new Handler(Looper.getMainLooper());
    private final Runnable flashSaleTicker = new Runnable() {
        @Override
        public void run() {
            updateFlashSaleCountdown();
            flashSaleHandler.postDelayed(this, 1000);
        }
    };

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

        setupHomeBanner();
        setupFeaturedSection();
        setupFlashSaleSection();
        setupSuggestedSection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startHomeBannerRotation();
        startFlashSaleCountdown();
    }

    @Override
    protected void onPause() {
        bannerHandler.removeCallbacks(bannerTicker);
        flashSaleHandler.removeCallbacks(flashSaleTicker);
        super.onPause();
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

    private void setupHomeBanner() {
        homeBannerContainer = findViewById(R.id.btnBannerBuyNow);
        ivHomeBannerCurrent = findViewById(R.id.ivHomeBannerCurrent);
        ivHomeBannerNext = findViewById(R.id.ivHomeBannerNext);
        if (ivHomeBannerCurrent == null || ivHomeBannerNext == null) return;

        currentBannerIndex = 0;
        ivHomeBannerCurrent.setImageResource(homeBanners[currentBannerIndex]);
        ivHomeBannerNext.setVisibility(View.INVISIBLE);
    }

    private void startHomeBannerRotation() {
        bannerHandler.removeCallbacks(bannerTicker);
        if (homeBannerContainer == null || ivHomeBannerCurrent == null || ivHomeBannerNext == null) return;
        bannerHandler.postDelayed(bannerTicker, BANNER_INTERVAL_MS);
    }

    private void showNextBanner() {
        if (homeBannerContainer == null || ivHomeBannerCurrent == null || ivHomeBannerNext == null) return;
        if (homeBannerContainer.getWidth() <= 0) return;

        int nextIndex = (currentBannerIndex + 1) % homeBanners.length;
        int bannerWidth = homeBannerContainer.getWidth();
        ivHomeBannerNext.setImageResource(homeBanners[nextIndex]);
        ivHomeBannerNext.setTranslationX(bannerWidth);
        ivHomeBannerNext.setVisibility(View.VISIBLE);
        ivHomeBannerCurrent.animate().translationX(-bannerWidth).setDuration(320).start();
        ivHomeBannerNext.animate()
                .translationX(0f)
                .setDuration(320)
                .withEndAction(() -> {
                    currentBannerIndex = nextIndex;
                    ivHomeBannerCurrent.setImageResource(homeBanners[currentBannerIndex]);
                    ivHomeBannerCurrent.setTranslationX(0f);
                    ivHomeBannerNext.setVisibility(View.INVISIBLE);
                    ivHomeBannerNext.setTranslationX(0f);
                })
                .start();
    }

    private void setupFeaturedSection() {
        RecyclerView rvFeatured = findViewById(R.id.rvFeaturedProducts);
        if (rvFeatured == null) return;

        rvFeatured.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        FeaturedHomeAdapter featuredAdapter = new FeaturedHomeAdapter();
        rvFeatured.setAdapter(featuredAdapter);

        ProductDao.ProductFilter filter = new ProductDao.ProductFilter();
        filter.sortMode = SORT_DISCOUNT_DESC;

        ArrayList<Product> featuredProducts = new ProductDao(this).locSanPham(filter);
        ArrayList<Product> previewProducts = new ArrayList<>();
        int limit = Math.min(5, featuredProducts.size());
        for (int i = 0; i < limit; i++) {
            previewProducts.add(featuredProducts.get(i));
        }
        featuredAdapter.setData(previewProducts);

        View emptyState = findViewById(R.id.tvFeaturedEmpty);
        if (emptyState != null) {
            emptyState.setVisibility(previewProducts.isEmpty() ? View.VISIBLE : View.GONE);
        }
        rvFeatured.setVisibility(previewProducts.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupFlashSaleSection() {
        RecyclerView rvFlashSale = findViewById(R.id.rvFlashSale);
        if (rvFlashSale == null) return;

        rvFlashSale.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ProductAdapter flashSaleAdapter = ProductAdapter.forFlashSale();
        rvFlashSale.setAdapter(flashSaleAdapter);

        ProductDao.ProductFilter filter = new ProductDao.ProductFilter();
        filter.onlyDiscounted = true;
        filter.sortMode = SORT_DISCOUNT_DESC;

        ArrayList<Product> discountedProducts = new ProductDao(this).locSanPham(filter);
        flashSaleAdapter.setData(discountedProducts);

        View emptyState = findViewById(R.id.layoutFlashSaleEmpty);
        if (emptyState != null) {
            emptyState.setVisibility(discountedProducts.isEmpty() ? View.VISIBLE : View.GONE);
        }
        rvFlashSale.setVisibility(discountedProducts.isEmpty() ? View.GONE : View.VISIBLE);

        View viewAllFlashSale = findViewById(R.id.btnViewAllFlashSale);
        if (viewAllFlashSale != null) {
            viewAllFlashSale.setOnClickListener(v -> openFlashSaleProducts());
        }
    }

    private void setupSuggestedSection() {
        RecyclerView rvSuggested = findViewById(R.id.rvSuggestedProducts);
        if (rvSuggested == null) return;

        rvSuggested.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ProductAdapter suggestedAdapter = ProductAdapter.forCompactCarousel();
        rvSuggested.setAdapter(suggestedAdapter);

        ProductDao.ProductFilter filter = new ProductDao.ProductFilter();
        filter.sortMode = SORT_DISCOUNT_DESC;

        ArrayList<Product> suggestedProducts = new ProductDao(this).locSanPham(filter);
        suggestedAdapter.setData(suggestedProducts);
    }

    private void openFlashSaleProducts() {
        Intent intent = new Intent(this, ProductsActivity.class);
        intent.putExtra(ProductsActivity.EXTRA_TITLE, "Sản phẩm");
        intent.putExtra(ProductsActivity.EXTRA_ONLY_DISCOUNTED, true);
        startActivity(intent);
    }

    private void startFlashSaleCountdown() {
        flashSaleHandler.removeCallbacks(flashSaleTicker);
        updateFlashSaleCountdown();
        flashSaleHandler.postDelayed(flashSaleTicker, 1000);
    }

    private void updateFlashSaleCountdown() {
        TextView tvCountdown = findViewById(R.id.tvFlashSaleCountdown);
        if (tvCountdown == null) return;

        Calendar now = Calendar.getInstance();
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        long diffMillis = Math.max(0L, endOfDay.getTimeInMillis() - now.getTimeInMillis());
        long totalSeconds = diffMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        tvCountdown.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
    }
}
