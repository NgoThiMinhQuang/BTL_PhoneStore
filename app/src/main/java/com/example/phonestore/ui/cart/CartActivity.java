package com.example.phonestore.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.CartDao;
import com.example.phonestore.data.model.CartItem;
import com.example.phonestore.data.model.CheckoutInfo;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.checkout.CheckoutActivity;
import com.example.phonestore.ui.home.BaseHomeActivity;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CartActivity extends BaseHomeActivity {

    public static final String EXTRA_SELECTED_PRODUCT_IDS = "extra_selected_product_ids";
    public static final String EXTRA_DISCOUNT_CODE = "extra_discount_code";

    private CartDao cartDao;

    private CartAdapter adapter;
    private TextView tvTotal;
    private TextView tvSubtotal;
    private TextView tvShippingFee;
    private TextView tvDiscountAmount;
    private TextView tvDiscountMessage;
    private EditText edtDiscountCode;

    private final Set<Long> selectedProductIds = new HashSet<>();
    private boolean didInitSelectAll = false;
    private String appliedDiscountCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!session.isLoggedIn() || session.getUserId() <= 0) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        cartDao = new CartDao(this);

        tvTotal = findViewById(R.id.tvTotal);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvShippingFee = findViewById(R.id.tvShippingFee);
        tvDiscountAmount = findViewById(R.id.tvDiscountAmount);
        tvDiscountMessage = findViewById(R.id.tvDiscountMessage);
        edtDiscountCode = findViewById(R.id.edtDiscountCode);

        RecyclerView rv = findViewById(R.id.rvCart);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CartAdapter(new CartAdapter.Listener() {
            @Override
            public void onChangeQty(CartItem item, int newQty) {
                boolean ok = cartDao.updateQty(session.getUserId(), item.productId, newQty);
                if (!ok) Toast.makeText(CartActivity.this, R.string.exceed_stock, Toast.LENGTH_SHORT).show();
                reload();
            }

            @Override
            public void onRemove(CartItem item) {
                cartDao.deleteItem(session.getUserId(), item.productId);
                selectedProductIds.remove(item.productId);
                reload();
            }

            @Override
            public void onToggleSelection(CartItem item, boolean isSelected) {
                if (isSelected) {
                    selectedProductIds.add(item.productId);
                } else {
                    selectedProductIds.remove(item.productId);
                }
                updateSelectedTotal();
            }
        });
        rv.setAdapter(adapter);

        MaterialButton btnApplyDiscount = findViewById(R.id.btnApplyDiscount);
        btnApplyDiscount.setOnClickListener(v -> applyDiscountCode());

        MaterialButton btnBuy = findViewById(R.id.btnBuyNow);
        btnBuy.setOnClickListener(v -> openCheckout());

        reload();
    }

    @Override
    protected int contentLayoutRes() {
        return R.layout.activity_cart;
    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_customer;
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.cart_nav_title);
    }

    @Override
    protected int selectedBottomNavItemId() {
        return R.id.nav_cart;
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    private void reload() {
        ArrayList<CartItem> list = cartDao.getCartItems(session.getUserId());

        Set<Long> availableIds = new HashSet<>();
        for (CartItem item : list) availableIds.add(item.productId);
        selectedProductIds.retainAll(availableIds);

        if (!didInitSelectAll) {
            selectedProductIds.addAll(availableIds);
            didInitSelectAll = true;
        }

        adapter.setData(list);
        adapter.setSelectedProductIds(selectedProductIds);
        updateSelectedTotal();
    }

    private void applyDiscountCode() {
        String rawCode = edtDiscountCode.getText().toString();
        int subtotal = cartDao.getTotalByProductIds(session.getUserId(), new ArrayList<>(selectedProductIds));
        String normalizedCode = CheckoutInfo.normalizeDiscountCode(rawCode);
        int discount = CheckoutInfo.calculateDiscount(rawCode, subtotal);

        if (normalizedCode == null) {
            appliedDiscountCode = null;
            tvDiscountMessage.setVisibility(android.view.View.GONE);
            updateSelectedTotal();
            return;
        }

        if (discount <= 0) {
            appliedDiscountCode = null;
            tvDiscountMessage.setVisibility(android.view.View.VISIBLE);
            tvDiscountMessage.setText(R.string.discount_invalid_message);
            updateSelectedTotal();
            return;
        }

        appliedDiscountCode = normalizedCode;
        edtDiscountCode.setText(normalizedCode);
        edtDiscountCode.setSelection(normalizedCode.length());
        tvDiscountMessage.setVisibility(android.view.View.VISIBLE);
        tvDiscountMessage.setText(getString(R.string.discount_applied_message, normalizedCode, formatMoney(discount)));
        updateSelectedTotal();
    }

    private void updateSelectedTotal() {
        int subtotal = cartDao.getTotalByProductIds(session.getUserId(), new ArrayList<>(selectedProductIds));
        int discount = CheckoutInfo.calculateDiscount(appliedDiscountCode, subtotal);
        int shippingFee = subtotal > 0 ? CheckoutInfo.SHIPPING_FEE : 0;
        int finalTotal = Math.max(0, subtotal + shippingFee - discount);

        tvSubtotal.setText(formatMoney(subtotal));
        tvShippingFee.setText(formatMoney(shippingFee));
        tvDiscountAmount.setText(formatMoney(discount));
        tvTotal.setText(getString(R.string.order_total_summary, formatMoney(finalTotal)));
    }

    private String formatMoney(int amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + "đ";
    }

    private void openCheckout() {
        if (selectedProductIds.isEmpty()) {
            Toast.makeText(this, R.string.cart_select_at_least_one, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(this, CheckoutActivity.class);
        i.putExtra(EXTRA_SELECTED_PRODUCT_IDS, toLongArray(selectedProductIds));
        i.putExtra(EXTRA_DISCOUNT_CODE, appliedDiscountCode);
        startActivity(i);
    }

    private long[] toLongArray(Set<Long> ids) {
        long[] arr = new long[ids.size()];
        int i = 0;
        for (Long id : ids) {
            arr[i++] = id;
        }
        return arr;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cartDao != null) {
            reload();
        }
    }
}
