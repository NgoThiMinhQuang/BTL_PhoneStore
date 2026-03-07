package com.example.phonestore.ui.orders;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.OrderDao;
import com.example.phonestore.data.model.OrderItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";

    private OrderDao orderDao;
    private TextView tvHeader;
    private OrderItemsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.order_detail_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        long orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1);
        if (orderId == -1) {
            Toast.makeText(this, R.string.order_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orderDao = new OrderDao(this);
        tvHeader = findViewById(R.id.tvHeader);

        RecyclerView rv = findViewById(R.id.rvOrderItems);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderItemsAdapter();
        rv.setAdapter(adapter);

        loadOrder(orderId);
    }

    private void loadOrder(long orderId) {
        ArrayList<OrderItem> items = orderDao.getOrderItems(orderId);
        adapter.setData(items);

        int total = 0;
        for (OrderItem it : items) total += it.thanhTien;
        String totalText = NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(total) + "đ";

        tvHeader.setText(getString(R.string.order_header, orderId, items.size(), totalText));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
