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

    private SessionManager session;
    private OrderDao orderDao;
    private OrdersAdapter adapter;
    private boolean adminMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        orderDao = new OrderDao(this);
        adminMode = DBHelper.ROLE_ADMIN.equals(session.getRole());

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(adminMode ? "Đơn hàng" : "Đơn hàng của tôi");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView rv = findViewById(R.id.rvOrders);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OrdersAdapter(new OrdersAdapter.Listener() {
            @Override
            public void onClick(Order order) {
                Intent i = new Intent(OrdersActivity.this, OrderDetailActivity.class);
                i.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id);
                startActivity(i);
            }

            @Override
            public void onUpdateStatus(Order order, String newStatus) {
                if (!adminMode) return;

                boolean ok = orderDao.updateStatus(order.id, newStatus);
                if (!ok) {
                    Toast.makeText(OrdersActivity.this, "Cập nhật trạng thái thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(OrdersActivity.this, "Đã cập nhật trạng thái đơn hàng", Toast.LENGTH_SHORT).show();
                loadOrders();
            }
        });
        rv.setAdapter(adapter);

        loadOrders();
    }

    private void loadOrders() {
        ArrayList<Order> list = adminMode
                ? orderDao.getAllOrders()
                : orderDao.getOrdersByUser(session.getUserId());

        adapter.setData(list, adminMode);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
