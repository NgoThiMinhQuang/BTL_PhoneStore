package com.example.phonestore.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.CartDao;
import com.example.phonestore.data.model.CartItem;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.checkout.CheckoutActivity;
import com.example.phonestore.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CartActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_PRODUCT_IDS = "extra_selected_product_ids";

    private SessionManager session;
    private CartDao cartDao;

    private CartAdapter adapter;
    private TextView tvTotal;

    private final Set<Long> selectedProductIds = new HashSet<>();
    private boolean didInitSelectAll = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        cartDao = new CartDao(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.cart_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvTotal = findViewById(R.id.tvTotal);

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

        MaterialButton btnBuy = findViewById(R.id.btnBuyNow);
        btnBuy.setOnClickListener(v -> openCheckout());

        reload();
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

    private void updateSelectedTotal() {
        int total = cartDao.getTotalByProductIds(session.getUserId(), new ArrayList<>(selectedProductIds));
        String totalText = NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(total) + "đ";
        tvTotal.setText(getString(R.string.checkout_total, totalText));
    }

    private void openCheckout() {
        if (selectedProductIds.isEmpty()) {
            Toast.makeText(this, R.string.cart_select_at_least_one, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(this, CheckoutActivity.class);
        i.putExtra(EXTRA_SELECTED_PRODUCT_IDS, toLongArray(selectedProductIds));
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
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
