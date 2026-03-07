package com.example.phonestore.ui.products;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.CartDao;
import com.example.phonestore.data.dao.ProductDao;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.ui.auth.LoginActivity;
import com.example.phonestore.ui.cart.CartActivity;
import com.example.phonestore.ui.checkout.CheckoutActivity;
import com.example.phonestore.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private SessionManager session;
    private ProductDao productDao;
    private CartDao cartDao;

    private Product product;
    private int qty = 1;
    private TextView tvCartBadge;

    private String selectedStorage = "";
    private String selectedColor = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        session = new SessionManager(this);

        productDao = new ProductDao(this);
        cartDao = new CartDao(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Chi tiết sản phẩm");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        long id = getIntent().getLongExtra(EXTRA_PRODUCT_ID, -1);
        product = productDao.getById(id);
        if (product == null) {
            Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView iv = findViewById(R.id.ivImage);
        TextView tvName = findViewById(R.id.tvName);
        TextView tvBrand = findViewById(R.id.tvBrand);
        TextView tvPrice = findViewById(R.id.tvPrice);
        TextView tvPriceOriginal = findViewById(R.id.tvPriceOriginal);
        TextView tvDiscountBadge = findViewById(R.id.tvDiscountBadge);
        TextView tvStock = findViewById(R.id.tvStock);
        TextView tvDesc = findViewById(R.id.tvDesc);
        TextView tvQty = findViewById(R.id.tvQty);
        TextView tvRatingValue = findViewById(R.id.tvRatingValue);
        TextView tvRatingStars = findViewById(R.id.tvRatingStars);
        TextView tvRatingMeta = findViewById(R.id.tvRatingMeta);
        TextView tvSpecScreen = findViewById(R.id.tvSpecScreen);
        TextView tvSpecCamera = findViewById(R.id.tvSpecCamera);
        TextView tvSpecRam = findViewById(R.id.tvSpecRam);
        TextView tvSpecPin = findViewById(R.id.tvSpecPin);

        LinearLayout layoutStorageOptions = findViewById(R.id.layoutStorageOptions);
        LinearLayout layoutColorOptions = findViewById(R.id.layoutColorOptions);

        MaterialButton btnMinus = findViewById(R.id.btnMinus);
        MaterialButton btnPlus = findViewById(R.id.btnPlus);
        MaterialButton btnAdd = findViewById(R.id.btnAddToCart);
        MaterialButton btnBuyNow = findViewById(R.id.btnBuyNow);

        int resId = 0;
        if (product.tenAnh != null && !product.tenAnh.trim().isEmpty()) {
            String imageKey = product.tenAnh.trim();
            resId = getResources().getIdentifier(imageKey, "drawable", getPackageName());
            if (resId == 0) {
                resId = getResources().getIdentifier(imageKey, "mipmap", getPackageName());
            }
        }
        iv.setImageResource(resId != 0 ? resId : android.R.drawable.ic_menu_gallery);

        String brand = (product.hang == null || product.hang.trim().isEmpty()) ? "PhoneStore" : product.hang.trim();
        tvName.setText(product.tenSanPham);
        tvBrand.setText(brand);

        int discountPercent = Math.max(0, Math.min(100, product.giamGia));
        int originalPrice = Math.max(0, product.gia);
        int discountedPrice = originalPrice * (100 - discountPercent) / 100;

        tvPrice.setText(formatMoney(discountedPrice));
        tvPriceOriginal.setText("Giá gốc: " + formatMoney(originalPrice));
        tvPriceOriginal.setPaintFlags(tvPriceOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        tvDiscountBadge.setText("-" + discountPercent + "%");

        float rating = buildRating(product);
        int ratingCount = buildRatingCount(product);
        tvRatingValue.setText(String.format(Locale.US, "%.1f", rating));
        tvRatingStars.setText(buildStarText(rating));
        tvRatingMeta.setText("(" + ratingCount + " đánh giá)");

        tvStock.setText("Còn lại: " + product.tonKho + " sản phẩm");
        tvDesc.setText(product.moTa == null ? "" : product.moTa);

        populateOptionChips(layoutStorageOptions, buildStorageOptions(product), true);
        populateOptionChips(layoutColorOptions, buildColorOptions(product), false);

        String[] specs = buildSpecs(product);
        tvSpecScreen.setText("Màn hình: " + specs[0]);
        tvSpecCamera.setText("Camera: " + specs[1]);
        tvSpecRam.setText("RAM: " + specs[2]);
        tvSpecPin.setText("Pin: " + specs[3]);

        tvQty.setText(String.valueOf(qty));

        btnMinus.setOnClickListener(v -> {
            if (qty > 1) qty--;
            tvQty.setText(String.valueOf(qty));
        });

        btnPlus.setOnClickListener(v -> {
            if (qty < product.tonKho) qty++;
            tvQty.setText(String.valueOf(qty));
        });

        btnAdd.setOnClickListener(v -> {
            if (!ensureLoggedIn()) return;
            if (!addToCart()) return;
            refreshCartBadge();
            Toast.makeText(this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
        });

        btnBuyNow.setOnClickListener(v -> {
            if (!ensureLoggedIn()) return;
            if (!addToCart()) return;
            refreshCartBadge();

            Intent i = new Intent(this, CheckoutActivity.class);
            int selectedTotal = cartDao.getTotalByProductIds(
                    session.getUserId(),
                    new ArrayList<>(Collections.singletonList(product.maSanPham))
            );
            if (selectedTotal > 0) {
                i.putExtra(CartActivity.EXTRA_SELECTED_PRODUCT_IDS, new long[]{product.maSanPham});
            }
            startActivity(i);
        });
    }

    private String formatMoney(int value) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(value) + "đ";
    }

    private float buildRating(Product p) {
        String key = normalizedKey(p);
        if (key.contains("iphone") || key.contains("apple")) return 4.9f;
        if (key.contains("samsung") || key.contains("galaxy")) return 4.8f;
        if (key.contains("xiaomi") || key.contains("redmi")) return 4.7f;
        if (key.contains("oppo") || key.contains("vivo")) return 4.6f;
        return 4.5f;
    }

    private int buildRatingCount(Product p) {
        String key = normalizedKey(p);
        if (key.contains("iphone") || key.contains("apple")) return 328;
        if (key.contains("samsung") || key.contains("galaxy")) return 241;
        if (key.contains("xiaomi") || key.contains("redmi")) return 189;
        if (key.contains("oppo") || key.contains("vivo")) return 134;
        return 120;
    }

    private String buildStarText(float rating) {
        int fullStars = Math.round(rating);
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            stars.append(i < fullStars ? "★" : "☆");
        }
        return stars.toString();
    }

    private void populateOptionChips(LinearLayout container, List<String> options, boolean isStorage) {
        container.removeAllViews();
        for (int i = 0; i < options.size(); i++) {
            final String value = options.get(i);

            TextView chip = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (i < options.size() - 1) lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            chip.setBackgroundResource(R.drawable.bg_option_chip);
            chip.setTextColor(ContextCompat.getColorStateList(this, R.color.option_chip_text));
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setText(value);
            chip.setClickable(true);
            chip.setFocusable(true);

            chip.setOnClickListener(v -> {
                clearSelectedState(container);
                chip.setSelected(true);
                if (isStorage) {
                    selectedStorage = value;
                } else {
                    selectedColor = value;
                }
            });

            container.addView(chip);

            if (i == 0) {
                chip.setSelected(true);
                if (isStorage) {
                    selectedStorage = value;
                } else {
                    selectedColor = value;
                }
            }
        }
    }

    private void clearSelectedState(LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            container.getChildAt(i).setSelected(false);
        }
    }

    private List<String> buildStorageOptions(Product p) {
        String key = normalizedKey(p);
        if (key.contains("iphone") || key.contains("apple")) {
            return Arrays.asList("128GB", "256GB", "512GB");
        }
        if (key.contains("samsung") || key.contains("galaxy")) {
            return Arrays.asList("256GB", "512GB", "1TB");
        }
        if (key.contains("xiaomi") || key.contains("redmi")) {
            return Arrays.asList("128GB", "256GB", "512GB");
        }
        if (key.contains("oppo") || key.contains("vivo")) {
            return Arrays.asList("128GB", "256GB");
        }
        return Arrays.asList("128GB", "256GB", "512GB");
    }

    private List<String> buildColorOptions(Product p) {
        String key = normalizedKey(p);
        if (key.contains("iphone") || key.contains("apple")) {
            return Arrays.asList("Titan tự nhiên", "Đen", "Trắng", "Xanh");
        }
        if (key.contains("samsung") || key.contains("galaxy")) {
            return Arrays.asList("Đen", "Xám", "Tím", "Xanh");
        }
        if (key.contains("xiaomi") || key.contains("redmi")) {
            return Arrays.asList("Đen", "Bạc", "Xanh dương");
        }
        if (key.contains("oppo") || key.contains("vivo")) {
            return Arrays.asList("Đen", "Tím", "Vàng");
        }
        return Arrays.asList("Đen", "Bạc", "Xanh");
    }

    private String[] buildSpecs(Product p) {
        String key = normalizedKey(p);
        if (key.contains("iphone") || key.contains("apple")) {
            return new String[]{"Super Retina XDR", "48MP", "8GB", "~29 giờ phát video"};
        }
        if (key.contains("samsung") || key.contains("galaxy")) {
            return new String[]{"Dynamic AMOLED 2X", "200MP", "12GB", "5.000mAh"};
        }
        if (key.contains("xiaomi") || key.contains("redmi")) {
            return new String[]{"AMOLED 120Hz", "108MP", "8GB", "5.000mAh"};
        }
        if (key.contains("oppo") || key.contains("vivo")) {
            return new String[]{"AMOLED FHD+", "50MP", "8GB", "4.800mAh"};
        }
        return new String[]{"OLED FHD+", "50MP", "8GB", "4.500mAh"};
    }

    private String normalizedKey(Product p) {
        String name = p.tenSanPham == null ? "" : p.tenSanPham;
        String brand = p.hang == null ? "" : p.hang;
        return (name + " " + brand).toLowerCase(Locale.ROOT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private boolean ensureLoggedIn() {
        if (session.isLoggedIn()) return true;
        startActivity(new Intent(this, LoginActivity.class));
        Toast.makeText(this, "Vui lòng đăng nhập/đăng ký để tiếp tục", Toast.LENGTH_SHORT).show();
        return false;
    }

    private boolean addToCart() {
        if (product.tonKho <= 0) {
            Toast.makeText(this, "Sản phẩm đã hết hàng", Toast.LENGTH_SHORT).show();
            return false;
        }
        boolean ok = cartDao.addOrIncrease(session.getUserId(), product.maSanPham, qty);
        if (!ok) {
            Toast.makeText(this, "Không thể thêm (vượt tồn kho)", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_detail_actions, menu);

        MenuItem cartItem = menu.findItem(R.id.action_cart);
        View actionView = cartItem.getActionView();
        if (actionView != null) {
            tvCartBadge = actionView.findViewById(R.id.tvCartBadge);
            actionView.setOnClickListener(v -> openCartFromToolbar());
            refreshCartBadge();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_cart) {
            openCartFromToolbar();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCartBadge();
    }

    private void openCartFromToolbar() {
        if (!ensureLoggedIn()) return;
        startActivity(new Intent(this, CartActivity.class));
    }

    private void refreshCartBadge() {
        if (tvCartBadge == null) return;
        if (!session.isLoggedIn()) {
            tvCartBadge.setVisibility(View.GONE);
            return;
        }

        int totalQty = cartDao.getTotalQty(session.getUserId());
        if (totalQty <= 0) {
            tvCartBadge.setVisibility(View.GONE);
            return;
        }

        tvCartBadge.setText(totalQty > 99 ? "99+" : String.valueOf(totalQty));
        tvCartBadge.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
