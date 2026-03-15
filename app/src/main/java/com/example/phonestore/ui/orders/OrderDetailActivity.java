package com.example.phonestore.ui.orders;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.phonestore.data.model.PaymentStatus;
import com.example.phonestore.ui.home.BaseHomeActivity;
import com.example.phonestore.ui.home.AdminOrdersActivity;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends BaseHomeActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";

    private final ArrayList<String> availableOrderStatuses = new ArrayList<>();
    private final ArrayList<String> availablePaymentStatuses = new ArrayList<>();

    private OrderDao orderDao;
    private OrderItemsAdapter adapter;
    private long orderId;

    private ScrollView scrollViewOrderDetail;
    private TextView tvOrderCode;
    private TextView tvOrderDate;
    private TextView tvStatusBadge;
    private TextView tvCustomerName;
    private TextView tvCustomerPhone;
    private TextView tvCustomerAddress;
    private TextView tvCustomerNote;
    private TextView tvProductsSummary;
    private TextView tvOrderSubtotal;
    private TextView tvOrderShippingFee;
    private TextView tvOrderDiscount;
    private TextView tvOrderTotal;
    private TextView tvPaymentMethod;
    private TextView tvPaymentStatusOverview;
    private TextView tvOrderStatusCurrent;
    private TextView tvPaymentStatusCurrent;
    private TextView tvActionHeadline;
    private TextView tvActionSummary;
    private TextView tvStatusCompletedTitle;
    private TextView tvStatusCompletedMessage;
    private TextView tvOrderActionHint;
    private TextView tvPaymentActionHint;
    private Spinner spinnerOrderStatus;
    private Spinner spinnerPaymentStatus;
    private MaterialButton btnUpdateOrderStatus;
    private MaterialButton btnUpdatePaymentStatus;
    private View cardStatusUpdate;
    private View cardStatusCompleted;
    private View viewStatusDivider;
    private LinearLayout layoutOrderStatusAction;
    private LinearLayout layoutPaymentStatusAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1);
        if (orderId == -1) {
            Toast.makeText(this, R.string.order_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orderDao = new OrderDao(this);

        bindViews();
        setupRecyclerView();
        setupSpinners();

        if (!isAdminDetail()) {
            cardStatusUpdate.setVisibility(View.GONE);
        }

        btnUpdateOrderStatus.setOnClickListener(v -> updateOrderStatus());
        btnUpdatePaymentStatus.setOnClickListener(v -> updatePaymentStatus());

        loadOrder(true);
    }

    @Override
    protected int shellLayoutRes() {
        return isAdminDetail() ? R.layout.activity_home_bottom_admin : super.shellLayoutRes();
    }

    @Override
    protected int contentLayoutRes() {
        return R.layout.activity_order_detail;
    }

    @Override
    protected int bottomMenuRes() {
        return isAdminDetail() ? R.menu.menu_bottom_admin : R.menu.menu_bottom_customer;
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.order_detail_title);
    }

    @Override
    protected int selectedBottomNavItemId() {
        return isAdminDetail() ? R.id.nav_orders_admin : R.id.nav_orders;
    }

    @Override
    protected boolean shouldShowBackButton() {
        return true;
    }

    @Override
    protected boolean shouldUseAdminBackButtonStyling() {
        return isAdminDetail();
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected boolean isBottomNavRootScreen() {
        return false;
    }

    @Override
    public void onBackPressed() {
        if (isAdminDetail() && isTaskRoot()) {
            startActivity(new Intent(this, AdminOrdersActivity.class));
            finish();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (isAdminDetail() && isTaskRoot()) {
            startActivity(new Intent(this, AdminOrdersActivity.class));
            finish();
            return true;
        }
        return super.onSupportNavigateUp();
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
        tvOrderSubtotal = findViewById(R.id.tvOrderSubtotal);
        tvOrderShippingFee = findViewById(R.id.tvOrderShippingFee);
        tvOrderDiscount = findViewById(R.id.tvOrderDiscount);
        tvOrderTotal = findViewById(R.id.tvOrderTotal);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvPaymentStatusOverview = findViewById(R.id.tvPaymentStatusOverview);
        tvOrderStatusCurrent = findViewById(R.id.tvOrderStatusCurrent);
        tvPaymentStatusCurrent = findViewById(R.id.tvPaymentStatusCurrent);
        tvActionHeadline = findViewById(R.id.tvActionHeadline);
        tvActionSummary = findViewById(R.id.tvActionSummary);
        tvStatusCompletedTitle = findViewById(R.id.tvStatusCompletedTitle);
        tvStatusCompletedMessage = findViewById(R.id.tvStatusCompletedMessage);
        tvOrderActionHint = findViewById(R.id.tvOrderActionHint);
        tvPaymentActionHint = findViewById(R.id.tvPaymentActionHint);
        spinnerOrderStatus = findViewById(R.id.spinnerOrderStatus);
        spinnerPaymentStatus = findViewById(R.id.spinnerPaymentStatus);
        btnUpdateOrderStatus = findViewById(R.id.btnUpdateOrderStatus);
        btnUpdatePaymentStatus = findViewById(R.id.btnUpdatePaymentStatus);
        cardStatusUpdate = findViewById(R.id.cardStatusUpdate);
        cardStatusCompleted = findViewById(R.id.cardStatusCompleted);
        viewStatusDivider = findViewById(R.id.viewStatusDivider);
        layoutOrderStatusAction = findViewById(R.id.layoutOrderStatusAction);
        layoutPaymentStatusAction = findViewById(R.id.layoutPaymentStatusAction);
    }

    private void setupRecyclerView() {
        RecyclerView rvOrderItems = findViewById(R.id.rvOrderItems);
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderItemsAdapter();
        rvOrderItems.setAdapter(adapter);
    }

    private void setupSpinners() {
        spinnerOrderStatus.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        ));
        spinnerPaymentStatus.setAdapter(new ArrayAdapter<>(
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

        if (!isAdminDetail() && order.userId != session.getUserId()) {
            Toast.makeText(this, R.string.order_access_denied, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ArrayList<OrderItem> items = orderDao.getOrderItems(orderId);
        adapter.setData(items);
        bindOrder(order, items);

        if (shouldScrollToStatus && isAdminDetail() && cardStatusUpdate.getVisibility() == View.VISIBLE) {
            scrollToStatusSection();
        }
    }

    private void bindOrder(Order order, ArrayList<OrderItem> items) {
        int subtotal = order.tamTinh > 0 ? order.tamTinh : calculateTotal(items);
        int shippingFee = Math.max(0, order.phiVanChuyen);
        int discountAmount = Math.max(0, order.tienGiam);
        int total = order.tongTien > 0 ? order.tongTien : Math.max(0, subtotal + shippingFee - discountAmount);

        tvOrderCode.setText(getString(R.string.admin_order_code, order.id));
        tvOrderDate.setText(formatDate(order.ngayTao));
        updateStatusBadge(order.trangThaiDon);

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
        tvOrderSubtotal.setText(formatCurrency(subtotal));
        tvOrderShippingFee.setText(formatCurrency(shippingFee));
        tvOrderDiscount.setText(getString(R.string.order_discount_value, formatCurrency(discountAmount), valueOrDash(order.maGiamGia)));
        tvOrderTotal.setText(formatCurrency(total));
        tvPaymentMethod.setText(formatPaymentMethod(order.phuongThucThanhToan));
        tvPaymentStatusOverview.setText(getString(
                R.string.order_payment_overview,
                OrdersAdapter.formatPaymentStatus(this, order.trangThaiThanhToan)
        ));
        tvOrderStatusCurrent.setText(getString(R.string.order_status_current, OrdersAdapter.formatOrderStatus(this, order.trangThaiDon)));
        tvPaymentStatusCurrent.setText(getString(R.string.payment_status_current, OrdersAdapter.formatPaymentStatus(this, order.trangThaiThanhToan)));
        bindStatusControls(order);
    }

    private void updateOrderStatus() {
        int selectedIndex = spinnerOrderStatus.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= availableOrderStatuses.size()) {
            return;
        }

        boolean ok = orderDao.updateOrderStatus(orderId, availableOrderStatuses.get(selectedIndex));
        if (!ok) {
            Toast.makeText(this, R.string.order_status_update_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.order_status_updated, Toast.LENGTH_SHORT).show();
        loadOrder(true);
    }

    private void updatePaymentStatus() {
        int selectedIndex = spinnerPaymentStatus.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= availablePaymentStatuses.size()) {
            return;
        }

        boolean ok = orderDao.updatePaymentStatus(orderId, availablePaymentStatuses.get(selectedIndex));
        if (!ok) {
            Toast.makeText(this, R.string.payment_status_update_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.payment_status_updated, Toast.LENGTH_SHORT).show();
        loadOrder(true);
    }

    private void bindStatusControls(Order order) {
        if (!isAdminDetail()) {
            cardStatusUpdate.setVisibility(View.GONE);
            return;
        }

        cardStatusUpdate.setVisibility(View.VISIBLE);
        availableOrderStatuses.clear();
        availablePaymentStatuses.clear();
        availableOrderStatuses.addAll(orderDao.getAllowedNextOrderStatuses(order));
        availablePaymentStatuses.addAll(orderDao.getAllowedNextPaymentStatuses(order));

        boolean finalState = orderDao.isFinalOrderStatus(order.trangThaiDon);
        boolean hasOrderActions = !availableOrderStatuses.isEmpty();
        boolean hasPaymentActions = !availablePaymentStatuses.isEmpty();

        bindActionHeader(order, finalState, hasOrderActions, hasPaymentActions);
        bindOrderStatusControls(order, hasOrderActions);
        bindPaymentStatusControls(order, hasPaymentActions);
        bindCompletedState(order, finalState);

        layoutOrderStatusAction.setVisibility(!finalState && hasOrderActions ? View.VISIBLE : View.GONE);
        layoutPaymentStatusAction.setVisibility(!finalState && hasPaymentActions ? View.VISIBLE : View.GONE);
        viewStatusDivider.setVisibility(!finalState && hasOrderActions && hasPaymentActions ? View.VISIBLE : View.GONE);
    }

    private void bindActionHeader(Order order, boolean finalState, boolean hasOrderActions, boolean hasPaymentActions) {
        if (finalState) {
            tvActionHeadline.setText(OrderStatus.STATUS_DA_GIAO.equals(order.trangThaiDon)
                    ? R.string.order_admin_completed_title
                    : R.string.order_admin_cancelled_title);
            tvActionSummary.setText(OrderStatus.STATUS_DA_GIAO.equals(order.trangThaiDon)
                    ? R.string.order_admin_completed_summary
                    : R.string.order_admin_cancelled_summary);
            return;
        }

        if (isBankTransfer(order) && PaymentStatus.STATUS_CHO_THANH_TOAN.equals(order.trangThaiThanhToan)) {
            tvActionHeadline.setText(R.string.order_admin_waiting_payment_title);
            tvActionSummary.setText(R.string.order_admin_waiting_payment_summary);
            return;
        }

        if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(order.trangThaiDon)) {
            tvActionHeadline.setText(R.string.order_admin_pending_title);
            tvActionSummary.setText(isCod(order)
                    ? R.string.order_admin_pending_cod_summary
                    : R.string.order_admin_pending_bank_summary);
            return;
        }

        if (OrderStatus.STATUS_DANG_XU_LY.equals(order.trangThaiDon)) {
            tvActionHeadline.setText(R.string.order_admin_processing_title);
            tvActionSummary.setText(R.string.order_admin_processing_summary);
            return;
        }

        if (!hasOrderActions && !hasPaymentActions) {
            tvActionHeadline.setText(R.string.order_admin_read_only_title);
            tvActionSummary.setText(R.string.order_admin_read_only_summary);
        }
    }

    private void bindCompletedState(Order order, boolean finalState) {
        if (!finalState) {
            cardStatusCompleted.setVisibility(View.GONE);
            return;
        }

        cardStatusCompleted.setVisibility(View.VISIBLE);
        if (OrderStatus.STATUS_DA_GIAO.equals(order.trangThaiDon)) {
            tvStatusCompletedTitle.setText(R.string.order_admin_completed_card_title);
            tvStatusCompletedMessage.setText(getString(
                    R.string.order_admin_completed_card_message,
                    OrdersAdapter.formatPaymentStatus(this, order.trangThaiThanhToan)
            ));
        } else {
            tvStatusCompletedTitle.setText(R.string.order_admin_cancelled_card_title);
            tvStatusCompletedMessage.setText(isBankTransfer(order)
                    ? getString(R.string.order_admin_cancelled_bank_card_message,
                    OrdersAdapter.formatPaymentStatus(this, order.trangThaiThanhToan))
                    : getString(R.string.order_admin_cancelled_cod_card_message,
                    OrdersAdapter.formatPaymentStatus(this, order.trangThaiThanhToan)));
        }
    }

    private void bindOrderStatusControls(Order order, boolean enabled) {
        ArrayList<String> labels = new ArrayList<>();
        for (String status : availableOrderStatuses) {
            labels.add(OrdersAdapter.formatOrderStatus(this, status));
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrderStatus.setAdapter(spinnerAdapter);

        spinnerOrderStatus.setEnabled(enabled);
        btnUpdateOrderStatus.setEnabled(enabled);
        btnUpdateOrderStatus.setText(R.string.update_order_status);
        tvOrderActionHint.setText(getOrderActionHint(order, enabled));
    }

    private void bindPaymentStatusControls(Order order, boolean enabled) {
        ArrayList<String> labels = new ArrayList<>();
        for (String status : availablePaymentStatuses) {
            labels.add(OrdersAdapter.formatPaymentStatus(this, status));
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentStatus.setAdapter(spinnerAdapter);

        spinnerPaymentStatus.setEnabled(enabled);
        btnUpdatePaymentStatus.setEnabled(enabled);
        btnUpdatePaymentStatus.setText(R.string.update_payment_status);
        tvPaymentActionHint.setText(getPaymentActionHint(order, enabled));
    }

    private String getOrderActionHint(Order order, boolean enabled) {
        if (enabled) {
            if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(order.trangThaiDon)) {
                return getString(isCod(order)
                        ? R.string.order_admin_order_hint_cod_pending
                        : R.string.order_admin_order_hint_bank_pending);
            }
            if (OrderStatus.STATUS_DANG_XU_LY.equals(order.trangThaiDon)) {
                return getString(R.string.order_admin_order_hint_processing);
            }
        }

        if (orderDao.isFinalOrderStatus(order.trangThaiDon)) {
            return getString(R.string.order_admin_order_hint_final);
        }
        if (isBankTransfer(order) && PaymentStatus.STATUS_CHO_THANH_TOAN.equals(order.trangThaiThanhToan)) {
            return getString(R.string.order_admin_order_hint_wait_payment);
        }
        return getString(R.string.order_admin_order_hint_read_only);
    }

    private String getPaymentActionHint(Order order, boolean enabled) {
        if (enabled) {
            return getString(R.string.order_admin_payment_hint_ready);
        }
        if (orderDao.isFinalOrderStatus(order.trangThaiDon)) {
            return getString(R.string.order_admin_payment_hint_final);
        }
        if (isCod(order)) {
            return getString(R.string.order_admin_payment_hint_cod);
        }
        if (PaymentStatus.STATUS_DA_THANH_TOAN.equals(order.trangThaiThanhToan)) {
            return getString(R.string.order_admin_payment_hint_paid);
        }
        return getString(R.string.order_admin_payment_hint_waiting);
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
        tvStatusBadge.setText(OrdersAdapter.formatOrderStatus(this, status));
        tvStatusBadge.setTextColor(ContextCompat.getColor(this, getStatusAccentColor(status)));
    }

    private int getStatusBackgroundColor(String status) {
        if (isAdminDetail()) {
            if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)) return R.color.admin_warning_soft;
            if (OrderStatus.STATUS_DANG_XU_LY.equals(status)) return R.color.admin_surface_soft;
            if (OrderStatus.STATUS_DA_GIAO.equals(status)) return R.color.admin_success_soft;
            if (OrderStatus.STATUS_DA_HUY.equals(status)) return R.color.admin_danger_soft;
            return R.color.admin_surface_soft;
        }

        if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)) return R.color.panel_soft;
        if (OrderStatus.STATUS_DANG_XU_LY.equals(status)) return R.color.panel_soft;
        if (OrderStatus.STATUS_DA_GIAO.equals(status)) return R.color.admin_success_soft;
        if (OrderStatus.STATUS_DA_HUY.equals(status)) return R.color.admin_danger_soft;
        return R.color.panel_soft;
    }

    private int getStatusAccentColor(String status) {
        if (isAdminDetail()) {
            if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)) return R.color.admin_warning;
            if (OrderStatus.STATUS_DANG_XU_LY.equals(status)) return R.color.admin_primary;
            if (OrderStatus.STATUS_DA_GIAO.equals(status)) return R.color.admin_success;
            if (OrderStatus.STATUS_DA_HUY.equals(status)) return R.color.admin_danger;
            return R.color.admin_primary;
        }

        if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)) return R.color.red_primary;
        if (OrderStatus.STATUS_DANG_XU_LY.equals(status)) return R.color.red_primary;
        if (OrderStatus.STATUS_DA_GIAO.equals(status)) return R.color.admin_success;
        if (OrderStatus.STATUS_DA_HUY.equals(status)) return R.color.admin_danger;
        return R.color.red_primary;
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

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private boolean isAdminDetail() {
        return DBHelper.ROLE_ADMIN.equals(getSessionRole());
    }

    private boolean isCod(Order order) {
        return order != null && CheckoutInfo.PAYMENT_COD.equals(order.phuongThucThanhToan);
    }

    private boolean isBankTransfer(Order order) {
        return order != null && CheckoutInfo.PAYMENT_BANK_TRANSFER.equals(order.phuongThucThanhToan);
    }
}
