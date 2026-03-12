package com.example.phonestore.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.OrderDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Order;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.utils.SessionManager;

import java.util.ArrayList;

public class OrdersActivity extends AppCompatActivity {

    public static final String EXTRA_ADMIN_MODE = "extra_admin_mode";
    public static final String EXTRA_SHOW_CHECKOUT_SUCCESS = "extra_show_checkout_success";
    public static final String EXTRA_CREATED_ORDER_ID = "extra_created_order_id";

    private SessionManager session;
    private OrderDao orderDao;
    private OrdersAdapter adapter;
    private boolean adminMode;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        session = new SessionManager(this);

        if (!session.isLoggedIn() || session.getUserId() <= 0) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        orderDao = new OrderDao(this);
        adminMode = resolveAdminMode(getIntent());

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        updateToolbarTitle();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

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

    private boolean resolveAdminMode(Intent intent) {
        return intent != null && intent.getBooleanExtra(
                EXTRA_ADMIN_MODE,
                DBHelper.ROLE_ADMIN.equals(session.getRole())
        );
    }

    private void updateToolbarTitle() {
        if (toolbar != null) {
            toolbar.setTitle(getString(adminMode ? R.string.orders_title_admin : R.string.orders_title_customer));
        }
    }

    private void loadOrders() {
        ArrayList<Order> list = adminMode
                ? orderDao.getAllOrders()
                : orderDao.getOrdersByUser(session.getUserId());

        adapter.setData(list, adminMode);
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

        adminMode = resolveAdminMode(intent);
        updateToolbarTitle();
        loadOrders();
        showCheckoutSuccessIfNeeded(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
