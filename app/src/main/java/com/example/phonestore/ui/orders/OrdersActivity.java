package com.example.phonestore.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.OrderDao;
import com.example.phonestore.data.model.Order;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.home.BaseHomeActivity;

import java.util.ArrayList;

public class OrdersActivity extends BaseHomeActivity {

    public static final String EXTRA_SHOW_CHECKOUT_SUCCESS = "extra_show_checkout_success";
    public static final String EXTRA_CREATED_ORDER_ID = "extra_created_order_id";

    private OrderDao orderDao;
    private OrdersAdapter adapter;
    private TextView tvOrdersSummaryCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!session.isLoggedIn() || session.getUserId() <= 0) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        orderDao = new OrderDao(this);

        tvOrdersSummaryCount = findViewById(R.id.tvOrdersSummaryCount);

        RecyclerView rv = findViewById(R.id.rvOrders);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OrdersAdapter(order -> {
            Intent i = new Intent(OrdersActivity.this, OrderDetailActivity.class);
            i.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id);
            startActivity(i);
        });

        rv.setAdapter(adapter);

        loadOrders();
        showCheckoutSuccessIfNeeded(getIntent());
    }

    @Override
    protected int contentLayoutRes() {
        return R.layout.activity_orders;
    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_customer;
    }

    @Override
    protected int selectedBottomNavItemId() {
        return R.id.nav_orders;
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.orders_title_customer);
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    private void loadOrders() {
        orderDao.reconcileExpiredTransferOrders();
        ArrayList<Order> list = orderDao.getOrdersByUser(session.getUserId());
        adapter.setData(list, false);
        if (tvOrdersSummaryCount != null) {
            tvOrdersSummaryCount.setText(getString(R.string.orders_count_summary, list.size()));
        }
    }

    private void showCheckoutSuccessIfNeeded(Intent intent) {
        if (intent == null) return;

        boolean showSuccess = intent.getBooleanExtra(EXTRA_SHOW_CHECKOUT_SUCCESS, false);
        long orderId = intent.getLongExtra(EXTRA_CREATED_ORDER_ID, -1);

        if (!showSuccess) return;

        String message = orderId > 0
                ? getString(R.string.checkout_success_with_order_id, orderId)
                : getString(R.string.checkout_success);

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        intent.removeExtra(EXTRA_SHOW_CHECKOUT_SUCCESS);
        intent.removeExtra(EXTRA_CREATED_ORDER_ID);
        setIntent(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadOrders();
        showCheckoutSuccessIfNeeded(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (orderDao != null) {
            loadOrders();
        }
    }
}
