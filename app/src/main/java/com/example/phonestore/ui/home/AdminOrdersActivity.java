package com.example.phonestore.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.OrderDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Order;
import com.example.phonestore.data.model.OrderStatus;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.orders.OrderDetailActivity;
import com.example.phonestore.ui.orders.OrdersAdapter;

import java.util.ArrayList;

public class AdminOrdersActivity extends BaseHomeActivity {

    private static final String FILTER_PENDING = OrderStatus.STATUS_CHO_XAC_NHAN;
    private static final String FILTER_PROCESSING = OrderStatus.STATUS_DANG_XU_LY;

    private OrderDao orderDao;
    private OrdersAdapter adapter;
    private String currentFilter;
    private String currentKeyword = "";
    private TextView tvOrdersCount;
    private TextView tvOrdersPending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        orderDao = new OrderDao(this);
    }

    @Override
    protected int shellLayoutRes() {
        return R.layout.activity_home_bottom_admin;
    }

    @Override
    protected int contentLayoutRes() {
        return R.layout.activity_admin_orders;
    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_admin;
    }

    @Override
    protected int selectedBottomNavItemId() {
        return R.id.nav_orders_admin;
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.admin_orders);
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected boolean shouldShowBackButton() {
        return true;
    }

    @Override
    protected boolean shouldUseAdminBackButtonStyling() {
        return true;
    }

    @Override
    protected void onShellReady() {
        View cardOrdersCount = findViewById(R.id.cardOrdersCount);
        View cardOrdersPending = findViewById(R.id.cardOrdersPending);
        ((TextView) cardOrdersCount.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_orders_count);
        ((TextView) cardOrdersPending.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_orders_pending);
        tvOrdersCount = cardOrdersCount.findViewById(R.id.tvKpiValue);
        tvOrdersPending = cardOrdersPending.findViewById(R.id.tvKpiValue);

        RecyclerView rvOrders = findViewById(R.id.rvOrders);
        if (rvOrders.getAdapter() == null) {
            rvOrders.setLayoutManager(new LinearLayoutManager(this));
            adapter = new OrdersAdapter(order -> {
                Intent intent = new Intent(AdminOrdersActivity.this, OrderDetailActivity.class);
                intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id);
                startActivity(intent);
            });
            rvOrders.setAdapter(adapter);
        }

        if (currentFilter == null) {
            currentFilter = FILTER_PENDING;
        }

        bindChip(findViewById(R.id.chipAllOrders), null);
        bindChip(findViewById(R.id.chipPendingOrders), FILTER_PENDING);
        bindChip(findViewById(R.id.chipProcessingOrders), FILTER_PROCESSING);
        bindChip(findViewById(R.id.chipCompletedOrders), OrderStatus.STATUS_DA_GIAO);
        bindChip(findViewById(R.id.chipCancelledOrders), OrderStatus.STATUS_DA_HUY);

        EditText edtSearchOrders = findViewById(R.id.edtSearchOrders);
        if (edtSearchOrders != null) {
            edtSearchOrders.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    currentKeyword = s == null ? "" : s.toString().trim();
                    loadOrders();
                }
            });
        }

        updateChipStates();
        loadOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (orderDao != null) loadOrders();
    }

    private void bindChip(TextView chip, String filter) {
        chip.setOnClickListener(v -> {
            currentFilter = filter;
            updateChipStates();
            loadOrders();
        });
    }

    private void updateChipStates() {
        updateChip(findViewById(R.id.chipAllOrders), currentFilter == null);
        updateChip(findViewById(R.id.chipPendingOrders), FILTER_PENDING.equals(currentFilter));
        updateChip(findViewById(R.id.chipProcessingOrders), FILTER_PROCESSING.equals(currentFilter));
        updateChip(findViewById(R.id.chipCompletedOrders), OrderStatus.STATUS_DA_GIAO.equals(currentFilter));
        updateChip(findViewById(R.id.chipCancelledOrders), OrderStatus.STATUS_DA_HUY.equals(currentFilter));
    }

    private void updateChip(TextView chip, boolean selected) {
        chip.setSelected(selected);
        chip.setTextColor(ContextCompat.getColor(this, selected ? android.R.color.white : R.color.admin_text_secondary));
    }

    private void loadOrders() {
        if (adapter == null || orderDao == null) return;

        orderDao.reconcileExpiredTransferOrders();
        ArrayList<Order> allOrders = orderDao.getAllOrders();
        ArrayList<Order> filteredOrders = new ArrayList<>();
        int pendingCount = 0;

        for (Order order : allOrders) {
            if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(order.trangThaiDon)) {
                pendingCount++;
            }
            if (matchesFilter(order) && matchesKeyword(order)) {
                filteredOrders.add(order);
            }
        }

        tvOrdersCount.setText(String.valueOf(filteredOrders.size()));
        tvOrdersPending.setText(String.valueOf(pendingCount));
        adapter.setData(filteredOrders, true);
    }

    private boolean matchesFilter(Order order) {
        if (currentFilter == null) return true;
        return currentFilter.equals(order.trangThaiDon);
    }

    private boolean matchesKeyword(Order order) {
        if (currentKeyword == null || currentKeyword.trim().isEmpty()) {
            return true;
        }
        String keyword = currentKeyword.trim().toLowerCase();
        return String.valueOf(order.id).contains(keyword)
                || safeLower(order.nguoiNhan).contains(keyword)
                || safeLower(order.sdtNhan).contains(keyword)
                || safeLower(order.username).contains(keyword);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
