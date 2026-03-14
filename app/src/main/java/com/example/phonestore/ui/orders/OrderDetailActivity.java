package com.example.phonestore.ui.orders;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.OrderDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.CheckoutInfo;
import com.example.phonestore.data.model.Order;
import com.example.phonestore.data.model.OrderItem;
import com.example.phonestore.data.model.OrderStatus;
import com.example.phonestore.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";

    private final ArrayList<String> availableStatuses = new ArrayList<>();

    private OrderDao orderDao;
    private OrderItemsAdapter adapter;
    private SessionManager session;
    private long orderId;
    private boolean adminMode;

    private ScrollView scrollViewOrderDetail;
    private TextView tvOrderCode;
    private TextView tvOrderDate;
    private TextView tvStatusBadge;
    private TextView tvCustomerName;
    private TextView tvCustomerPhone;
    private TextView tvCustomerAddress;
    private TextView tvCustomerNote;
    private TextView tvProductsSummary;
    private TextView tvOrderTotal;
    private TextView tvPaymentMethod;
    private TextView tvStatusCurrent;
    private Spinner spinnerStatus;
    private MaterialButton btnUpdateStatus;
    private View cardStatusUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.order_detail_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1);
        if (orderId == -1) {
            Toast.makeText(this, R.string.order_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        session = new SessionManager(this);
        orderDao = new OrderDao(this);
        adminMode = DBHelper.ROLE_ADMIN.equals(session.getRole());

        bindViews();
        setupRecyclerView();
        setupStatusSpinner();

        if (!adminMode) {
            cardStatusUpdate.setVisibility(View.GONE);
        }

        btnUpdateStatus.setOnClickListener(v -> updateOrderStatus());

        loadOrder(true);
    }

    private void bindViews() {
        scrollViewOrderDetail = findViewById(R.id.scrollViewOrderDetail);
        tvOrderCode = findViewById(R.id.tvOrderCode);
        tvOrderDate = findViewById(R.id.tvOrderDate);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone);
        tvCustomerAddress = findViewById(R.id.tvCustomerAddress);
        tvCustomerNote = findViewById(R.id.tvCustomerNote);
        tvProductsSummary = findViewById(R.id.tvProductsSummary);
        tvOrderTotal = findViewById(R.id.tvOrderTotal);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvStatusCurrent = findViewById(R.id.tvStatusCurrent);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        cardStatusUpdate = findViewById(R.id.cardStatusUpdate);
    }

    private void setupRecyclerView() {
        RecyclerView rvOrderItems = findViewById(R.id.rvOrderItems);
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderItemsAdapter();
        rvOrderItems.setAdapter(adapter);
    }

    private void setupStatusSpinner() {
        spinnerStatus.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        ));
    }

    private void loadOrder(boolean shouldScrollToStatus) {
        Order order = orderDao.getOrderById(orderId);
        if (order == null) {
            Toast.makeText(this, R.string.order_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!adminMode && order.userId != session.getUserId()) {
            Toast.makeText(this, R.string.order_access_denied, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ArrayList<OrderItem> items = orderDao.getOrderItems(orderId);
        adapter.setData(items);
        bindOrder(order, items);

        if (shouldScrollToStatus) {
            scrollToStatusSection();
        }
    }

    private void bindOrder(Order order, ArrayList<OrderItem> items) {
        int total = order.tongTien > 0 ? order.tongTien : calculateTotal(items);

        tvOrderCode.setText(getString(R.string.admin_order_code, order.id));
        tvOrderDate.setText(formatDate(order.ngayTao));
        updateStatusBadge(order.trangThai);

        tvCustomerName.setText(getString(R.string.order_customer_name) + " " + valueOrDash(order.nguoiNhan));
        tvCustomerPhone.setText(getString(R.string.order_customer_phone) + " " + valueOrDash(order.sdtNhan));
        tvCustomerAddress.setText(getString(R.string.order_customer_address) + " " + valueOrDash(order.diaChiNhan));

        if (order.ghiChu != null && !order.ghiChu.trim().isEmpty()) {
            tvCustomerNote.setVisibility(View.VISIBLE);
            tvCustomerNote.setText(getString(R.string.admin_shipping_note, order.ghiChu.trim()));
        } else {
            tvCustomerNote.setVisibility(View.GONE);
            tvCustomerNote.setText("");
        }

        tvProductsSummary.setText(getString(R.string.order_products_summary, items.size()));
        tvOrderTotal.setText(formatCurrency(total));
        tvPaymentMethod.setText(formatPaymentMethod(order.phuongThucThanhToan));
        tvStatusCurrent.setText(getString(R.string.order_status_current, formatStatus(order.trangThai)));
        bindStatusControls(order.trangThai);
    }

    private void updateOrderStatus() {
        int selectedIndex = spinnerStatus.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= availableStatuses.size()) {
            return;
        }

        boolean ok = orderDao.updateStatus(orderId, availableStatuses.get(selectedIndex));
        if (!ok) {
            Toast.makeText(this, R.string.order_status_update_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.order_status_updated, Toast.LENGTH_SHORT).show();
        loadOrder(true);
    }

    private void bindStatusControls(String currentStatus) {
        List<String> nextStatuses = orderDao.getAllowedNextStatuses(currentStatus);
        availableStatuses.clear();
        availableStatuses.addAll(nextStatuses);

        ArrayList<String> labels = new ArrayList<>();
        for (String status : nextStatuses) {
            labels.add(formatStatus(status));
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(spinnerAdapter);

        boolean finalStatus = orderDao.isFinalStatus(currentStatus);
        spinnerStatus.setEnabled(!finalStatus && !nextStatuses.isEmpty());
        btnUpdateStatus.setEnabled(!finalStatus && !nextStatuses.isEmpty());

        if (finalStatus) {
            btnUpdateStatus.setText(R.string.order_status_locked);
        } else {
            btnUpdateStatus.setText(R.string.update_status);
        }
    }

    private void scrollToStatusSection() {
        scrollViewOrderDetail.post(() -> scrollViewOrderDetail.smoothScrollTo(0, cardStatusUpdate.getTop()));
    }

    private void updateStatusBadge(String status) {
        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(dp(999));
        badge.setColor(ContextCompat.getColor(this, getStatusBackgroundColor(status)));
        badge.setStroke(dp(1), ContextCompat.getColor(this, getStatusAccentColor(status)));

        tvStatusBadge.setBackground(badge);
        tvStatusBadge.setText(formatStatus(status));
        tvStatusBadge.setTextColor(ContextCompat.getColor(this, getStatusAccentColor(status)));
    }

    private int getStatusBackgroundColor(String status) {
        if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)) return R.color.admin_warning_soft;
        if (OrderStatus.STATUS_CHO_THANH_TOAN.equals(status)) return R.color.admin_dashboard_blue_soft;
        if (OrderStatus.STATUS_DA_THANH_TOAN.equals(status)) return R.color.admin_surface_soft;
        if (OrderStatus.STATUS_DANG_XU_LY.equals(status)) return R.color.admin_surface_soft;
        if (OrderStatus.STATUS_DA_GIAO.equals(status)) return R.color.admin_success_soft;
        if (OrderStatus.STATUS_DA_HUY.equals(status)) return R.color.admin_danger_soft;
        return R.color.admin_surface_soft;
    }

    private int getStatusAccentColor(String status) {
        if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)) return R.color.admin_warning;
        if (OrderStatus.STATUS_CHO_THANH_TOAN.equals(status)) return R.color.admin_dashboard_blue;
        if (OrderStatus.STATUS_DA_THANH_TOAN.equals(status)) return R.color.admin_primary;
        if (OrderStatus.STATUS_DANG_XU_LY.equals(status)) return R.color.admin_primary;
        if (OrderStatus.STATUS_DA_GIAO.equals(status)) return R.color.admin_success;
        if (OrderStatus.STATUS_DA_HUY.equals(status)) return R.color.admin_danger;
        return R.color.admin_primary;
    }

    private int calculateTotal(ArrayList<OrderItem> items) {
        int total = 0;
        for (OrderItem item : items) {
            total += item.thanhTien;
        }
        return total;
    }

    private String formatCurrency(int amount) {
        return getString(
                R.string.admin_price_currency,
                NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount)
        );
    }

    private String formatDate(long timestamp) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"))
                .format(new Date(timestamp));
    }

    private String formatPaymentMethod(String paymentMethod) {
        if (CheckoutInfo.PAYMENT_BANK_TRANSFER.equals(paymentMethod)) {
            return getString(R.string.order_bank_transfer_full);
        }
        if (CheckoutInfo.PAYMENT_COD.equals(paymentMethod)) {
            return getString(R.string.payment_cod);
        }
        return valueOrDash(paymentMethod);
    }

    private String formatStatus(String status) {
        if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)) return getString(R.string.order_status_pending);
        if (OrderStatus.STATUS_CHO_THANH_TOAN.equals(status)) return getString(R.string.order_status_waiting_payment);
        if (OrderStatus.STATUS_DA_THANH_TOAN.equals(status)) return getString(R.string.order_status_paid);
        if (OrderStatus.STATUS_DANG_XU_LY.equals(status)) return getString(R.string.order_status_processing);
        if (OrderStatus.STATUS_DA_GIAO.equals(status)) return getString(R.string.order_status_delivered);
        if (OrderStatus.STATUS_DA_HUY.equals(status)) return getString(R.string.order_status_cancelled);
        if (status == null || status.trim().isEmpty()) return "-";
        return status.replace('_', ' ');
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
