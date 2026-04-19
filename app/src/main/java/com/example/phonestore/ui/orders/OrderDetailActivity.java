package com.example.phonestore.ui.orders;

import android.app.AlertDialog;
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
import androidx.core.view.ViewCompat;
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
    private final ArrayList<String> allOrderStatuses = new ArrayList<>();
    private final ArrayList<String> allPaymentStatuses = new ArrayList<>();

    private OrderDao orderDao;
    private OrderItemsAdapter adapter;
    private long orderId;

    private ScrollView scrollViewOrderDetail;
    private TextView tvOrderCode;
    private TextView tvOrderDate;
    private TextView tvStatusBadge;
    private TextView tvPaymentBadge;
    private TextView tvOrderMeta;
    private TextView tvTimelinePendingDate;
    private TextView tvTimelinePendingSummary;
    private TextView tvTimelineProcessingDate;
    private TextView tvTimelineProcessingSummary;
    private TextView tvTimelineDeliveredDate;
    private TextView tvTimelineDeliveredSummary;
    private TextView tvTimelineCancelledDate;
    private TextView tvTimelineCancelledSummary;
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
    private TextView tvPaymentLifecycleInfo;
    private TextView tvRefundInfo;
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
    private MaterialButton btnUpdateStatus;
    private View cardStatusUpdate;
    private View cardStatusCompleted;
    private View cardCancelledStatus;
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

        btnUpdateStatus.setOnClickListener(v -> confirmUpdateStatuses());

        allOrderStatuses.clear();
        allOrderStatuses.add(OrderStatus.STATUS_CHO_XAC_NHAN);
        allOrderStatuses.add(OrderStatus.STATUS_DANG_XU_LY);
        allOrderStatuses.add(OrderStatus.STATUS_DA_GIAO);
        allOrderStatuses.add(OrderStatus.STATUS_DA_HUY);

        allPaymentStatuses.clear();
        allPaymentStatuses.add(PaymentStatus.STATUS_CHUA_THANH_TOAN);
        allPaymentStatuses.add(PaymentStatus.STATUS_CHO_THANH_TOAN);
        allPaymentStatuses.add(PaymentStatus.STATUS_DA_THANH_TOAN);
        allPaymentStatuses.add(PaymentStatus.STATUS_HET_HAN_THANH_TOAN);

        loadOrder(true);
    }

    @Override
    protected int shellLayoutRes() {
        return isAdminDetail() ? R.layout.activity_home_bottom_admin : super.shellLayoutRes();
    }

    @Override
    protected int contentLayoutRes() {
        return isAdminDetail() ? R.layout.activity_order_detail_admin : R.layout.activity_order_detail_customer;
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

    @Override
    protected void onResume() {
        super.onResume();
        if (orderDao != null) {
            loadOrder(false);
        }
    }

    private void bindViews() {
        scrollViewOrderDetail = findViewById(R.id.scrollViewOrderDetail);
        tvOrderCode = findViewById(R.id.tvOrderCode);
        tvOrderDate = findViewById(R.id.tvOrderDate);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvPaymentBadge = findViewById(R.id.tvPaymentBadge);
        tvOrderMeta = findViewById(R.id.tvOrderMeta);
        tvTimelinePendingDate = findViewById(R.id.tvTimelinePendingDate);
        tvTimelinePendingSummary = findViewById(R.id.tvTimelinePendingSummary);
        tvTimelineProcessingDate = findViewById(R.id.tvTimelineProcessingDate);
        tvTimelineProcessingSummary = findViewById(R.id.tvTimelineProcessingSummary);
        tvTimelineDeliveredDate = findViewById(R.id.tvTimelineDeliveredDate);
        tvTimelineDeliveredSummary = findViewById(R.id.tvTimelineDeliveredSummary);
        tvTimelineCancelledDate = findViewById(R.id.tvTimelineCancelledDate);
        tvTimelineCancelledSummary = findViewById(R.id.tvTimelineCancelledSummary);
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
        tvPaymentLifecycleInfo = findViewById(R.id.tvPaymentLifecycleInfo);
        tvRefundInfo = findViewById(R.id.tvRefundInfo);
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
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        cardStatusUpdate = findViewById(R.id.cardStatusUpdate);
        cardStatusCompleted = findViewById(R.id.cardStatusCompleted);
        cardCancelledStatus = findViewById(R.id.cardCancelledStatus);
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
        if (spinnerOrderStatus == null || spinnerPaymentStatus == null) {
            return;
        }
        int selectedLayout = isAdminDetail() ? R.layout.item_spinner_selected_admin : android.R.layout.simple_spinner_item;
        int dropdownLayout = isAdminDetail() ? R.layout.item_spinner_dropdown_admin : android.R.layout.simple_spinner_dropdown_item;

        ArrayAdapter<String> orderAdapter = new ArrayAdapter<>(
                this,
                selectedLayout,
                new ArrayList<>()
        );
        orderAdapter.setDropDownViewResource(dropdownLayout);
        spinnerOrderStatus.setAdapter(orderAdapter);

        ArrayAdapter<String> paymentAdapter = new ArrayAdapter<>(
                this,
                selectedLayout,
                new ArrayList<>()
        );
        paymentAdapter.setDropDownViewResource(dropdownLayout);
        spinnerPaymentStatus.setAdapter(paymentAdapter);
    }

    private void loadOrder(boolean shouldScrollToStatus) {
        orderDao.reconcileExpiredTransferOrders();
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
        if (tvOrderMeta != null) {
            tvOrderMeta.setText(buildOrderHeaderSummary(items));
        }
        updateStatusBadge(order.trangThaiDon);
        updatePaymentBadge(order.trangThaiThanhToan);
        bindTimeline(order);

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
        bindPaymentLifecycleInfo(order);
        bindRefundInfo(order);
        tvOrderStatusCurrent.setText(getString(R.string.order_status_current, OrdersAdapter.formatOrderStatus(this, order.trangThaiDon)));
        tvPaymentStatusCurrent.setText(getString(R.string.payment_status_current, OrdersAdapter.formatPaymentStatus(this, order.trangThaiThanhToan)));
        bindStatusControls(order);
    }

    private void confirmUpdateStatuses() {
        int orderIndex = spinnerOrderStatus.getSelectedItemPosition();
        int paymentIndex = spinnerPaymentStatus.getSelectedItemPosition();

        final String nextOrderStatus = (orderIndex >= 0 && orderIndex < availableOrderStatuses.size())
                ? availableOrderStatuses.get(orderIndex) : null;
        final String nextPaymentStatus = (paymentIndex >= 0 && paymentIndex < availablePaymentStatuses.size())
                ? availablePaymentStatuses.get(paymentIndex) : null;

        if (nextOrderStatus == null && nextPaymentStatus == null) {
            return;
        }

        StringBuilder message = new StringBuilder("Bạn có chắc chắn muốn thay đổi");
        if (nextOrderStatus != null) {
            message.append("\n- Trạng thái đơn hàng: ")
                    .append(OrdersAdapter.formatOrderStatus(this, nextOrderStatus));
        }
        if (nextPaymentStatus != null) {
            message.append("\n- Trạng thái thanh toán: ")
                    .append(OrdersAdapter.formatPaymentStatus(this, nextPaymentStatus));
        }
        message.append("\nkhông?");

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận cập nhật")
                .setMessage(message.toString())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.update_order_status, (dialog, which) -> applyStatusUpdates(nextOrderStatus, nextPaymentStatus))
                .show();
    }

    private void applyStatusUpdates(String nextOrderStatus, String nextPaymentStatus) {
        if (nextOrderStatus != null) {
            boolean ok = orderDao.updateOrderStatus(orderId, nextOrderStatus);
            if (!ok) {
                Toast.makeText(this, R.string.order_status_update_failed, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (nextPaymentStatus != null) {
            boolean ok = orderDao.updatePaymentStatus(orderId, nextPaymentStatus);
            if (!ok) {
                Toast.makeText(this, R.string.payment_status_update_failed, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Toast.makeText(this, R.string.order_status_updated, Toast.LENGTH_SHORT).show();
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
        availableOrderStatuses.addAll(allOrderStatuses);
        availablePaymentStatuses.addAll(allPaymentStatuses);

        boolean finalState = orderDao.isFinalOrderStatus(order.trangThaiDon);
        boolean hasOrderActions = true;
        boolean hasPaymentActions = true;

        bindActionHeader(order, finalState, hasOrderActions, hasPaymentActions);
        bindOrderStatusControls(order, hasOrderActions);
        bindPaymentStatusControls(order, hasPaymentActions);
        bindCompletedState(order, finalState);

        boolean showActions = !finalState && (hasOrderActions || hasPaymentActions);
        layoutOrderStatusAction.setVisibility(!finalState ? View.VISIBLE : View.GONE);
        layoutPaymentStatusAction.setVisibility(!finalState ? View.VISIBLE : View.GONE);
        if (btnUpdateStatus != null) {
            btnUpdateStatus.setVisibility(showActions ? View.VISIBLE : View.GONE);
            btnUpdateStatus.setEnabled(showActions);
        }
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

        int selectedLayout = isAdminDetail() ? R.layout.item_spinner_selected_admin : android.R.layout.simple_spinner_item;
        int dropdownLayout = isAdminDetail() ? R.layout.item_spinner_dropdown_admin : android.R.layout.simple_spinner_dropdown_item;
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                selectedLayout,
                labels
        );
        spinnerAdapter.setDropDownViewResource(dropdownLayout);
        spinnerOrderStatus.setAdapter(spinnerAdapter);
        int currentIndex = availableOrderStatuses.indexOf(order.trangThaiDon);
        if (currentIndex >= 0) {
            spinnerOrderStatus.setSelection(currentIndex, false);
        }

        spinnerOrderStatus.setEnabled(enabled);
        tvOrderActionHint.setText(getOrderActionHint(order, enabled));
    }

    private void bindPaymentStatusControls(Order order, boolean enabled) {
        ArrayList<String> labels = new ArrayList<>();
        for (String status : availablePaymentStatuses) {
            labels.add(OrdersAdapter.formatPaymentStatus(this, status));
        }

        int selectedLayout = isAdminDetail() ? R.layout.item_spinner_selected_admin : android.R.layout.simple_spinner_item;
        int dropdownLayout = isAdminDetail() ? R.layout.item_spinner_dropdown_admin : android.R.layout.simple_spinner_dropdown_item;
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                selectedLayout,
                labels
        );
        spinnerAdapter.setDropDownViewResource(dropdownLayout);
        spinnerPaymentStatus.setAdapter(spinnerAdapter);
        int currentIndex = availablePaymentStatuses.indexOf(order.trangThaiThanhToan);
        if (currentIndex >= 0) {
            spinnerPaymentStatus.setSelection(currentIndex, false);
        }

        spinnerPaymentStatus.setEnabled(enabled);
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
        applyBadge(tvStatusBadge, OrdersAdapter.formatOrderStatus(this, status), getStatusBackgroundColor(status), getStatusAccentColor(status));
    }

    private void updatePaymentBadge(String status) {
        if (tvPaymentBadge == null) {
            return;
        }
        int backgroundColor;
        int accentColor;
        if (PaymentStatus.STATUS_DA_THANH_TOAN.equals(status)) {
            backgroundColor = R.color.admin_success_soft;
            accentColor = R.color.admin_success;
        } else if (PaymentStatus.STATUS_HET_HAN_THANH_TOAN.equals(status)) {
            backgroundColor = R.color.admin_danger_soft;
            accentColor = R.color.admin_danger;
        } else if (PaymentStatus.STATUS_CHO_THANH_TOAN.equals(status)) {
            backgroundColor = isAdminDetail() ? R.color.admin_warning_soft : R.color.panel_soft;
            accentColor = isAdminDetail() ? R.color.admin_warning : R.color.red_primary;
        } else {
            backgroundColor = isAdminDetail() ? R.color.admin_surface_soft : R.color.panel_soft;
            accentColor = isAdminDetail() ? R.color.admin_primary : R.color.red_primary;
        }
        applyBadge(tvPaymentBadge, OrdersAdapter.formatPaymentStatus(this, status), backgroundColor, accentColor);
    }

    private void applyBadge(TextView view, String text, int backgroundColorRes, int accentColorRes) {
        if (view == null) {
            return;
        }
        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(dp(999));
        badge.setColor(ContextCompat.getColor(this, backgroundColorRes));
        badge.setStroke(dp(1), ContextCompat.getColor(this, accentColorRes));

        view.setBackground(badge);
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(this, accentColorRes));
    }

    private int getStatusBackgroundColor(String status) {
        if (isAdminDetail()) {
            if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)) return R.color.admin_warning_soft;
            if (OrderStatus.STATUS_DANG_XU_LY.equals(status)) return R.color.admin_surface_soft;
            if (OrderStatus.STATUS_DA_GIAO.equals(status)) return R.color.admin_success_soft;
            if (OrderStatus.STATUS_DA_HUY.equals(status)) return R.color.admin_danger_soft;
            return R.color.admin_surface_soft;
        }

        if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)) return R.color.admin_warning_soft;
        if (OrderStatus.STATUS_DANG_XU_LY.equals(status)) return R.color.admin_dashboard_purple_soft;
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

        if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)) return R.color.admin_warning;
        if (OrderStatus.STATUS_DANG_XU_LY.equals(status)) return R.color.admin_dashboard_purple;
        if (OrderStatus.STATUS_DA_GIAO.equals(status)) return R.color.admin_success;
        if (OrderStatus.STATUS_DA_HUY.equals(status)) return R.color.admin_danger;
        return R.color.red_primary;
    }

    private void bindTimeline(Order order) {
        bindTimelineStep(
                R.id.viewTimelinePendingDot,
                OrderStatus.STATUS_CHO_XAC_NHAN.equals(order.trangThaiDon)
                        || OrderStatus.STATUS_DANG_XU_LY.equals(order.trangThaiDon)
                        || OrderStatus.STATUS_DA_GIAO.equals(order.trangThaiDon)
                        || OrderStatus.STATUS_DA_HUY.equals(order.trangThaiDon),
                false,
                tvTimelinePendingDate,
                tvTimelinePendingSummary,
                formatDate(order.ngayTao),
                getString(R.string.order_status_pending_desc)
        );

        boolean processingReached = OrderStatus.STATUS_DANG_XU_LY.equals(order.trangThaiDon)
                || OrderStatus.STATUS_DA_GIAO.equals(order.trangThaiDon);
        bindTimelineStep(
                R.id.viewTimelineProcessingDot,
                processingReached,
                OrderStatus.STATUS_DANG_XU_LY.equals(order.trangThaiDon),
                tvTimelineProcessingDate,
                tvTimelineProcessingSummary,
                processingReached ? getString(R.string.order_timeline_status_value, OrdersAdapter.formatOrderStatus(this, OrderStatus.STATUS_DANG_XU_LY)) : getString(R.string.order_timeline_not_available),
                getString(R.string.order_status_processing_desc)
        );

        boolean deliveredReached = OrderStatus.STATUS_DA_GIAO.equals(order.trangThaiDon);
        bindTimelineStep(
                R.id.viewTimelineDeliveredDot,
                deliveredReached,
                deliveredReached,
                tvTimelineDeliveredDate,
                tvTimelineDeliveredSummary,
                deliveredReached ? getString(R.string.order_timeline_status_value, OrdersAdapter.formatOrderStatus(this, order.trangThaiDon)) : getString(R.string.order_timeline_not_available),
                getString(R.string.order_status_delivered_desc)
        );

        boolean cancelledReached = OrderStatus.STATUS_DA_HUY.equals(order.trangThaiDon);
        String cancelledDate = order.cancelledAt > 0 ? formatDate(order.cancelledAt) : getString(R.string.order_timeline_status_value, OrdersAdapter.formatOrderStatus(this, order.trangThaiDon));
        if (cardCancelledStatus != null) {
            cardCancelledStatus.setVisibility(cancelledReached ? View.VISIBLE : View.GONE);
        }
        bindTimelineStep(
                R.id.viewTimelineCancelledDot,
                cancelledReached,
                cancelledReached,
                tvTimelineCancelledDate,
                tvTimelineCancelledSummary,
                cancelledReached ? cancelledDate : getString(R.string.order_timeline_not_available),
                getString(R.string.order_status_cancelled_desc)
        );
    }

    private void bindTimelineStep(int dotId,
                                  boolean completed,
                                  boolean current,
                                  TextView dateView,
                                  TextView summaryView,
                                  String dateText,
                                  String summaryText) {
        View dot = findViewById(dotId);
        if (dot != null) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            int fill = completed ? (isAdminDetail() ? R.color.admin_primary : R.color.red_primary) : (isAdminDetail() ? R.color.admin_surface_soft : R.color.panel_soft);
            int stroke = completed ? (isAdminDetail() ? R.color.admin_primary : R.color.red_primary) : (isAdminDetail() ? R.color.admin_stroke : R.color.panel_stroke);
            drawable.setColor(ContextCompat.getColor(this, fill));
            drawable.setStroke(dp(1), ContextCompat.getColor(this, stroke));
            ViewCompat.setBackground(dot, drawable);
            dot.setAlpha(completed || current ? 1f : 0.65f);
        }
        if (dateView != null) {
            dateView.setText(dateText);
            dateView.setTextColor(ContextCompat.getColor(this, completed || current
                    ? (isAdminDetail() ? R.color.admin_text_primary : R.color.text_primary)
                    : (isAdminDetail() ? R.color.admin_text_secondary : R.color.text_sub)));
        }
        if (summaryView != null) {
            summaryView.setVisibility(View.GONE);
            summaryView.setText("");
        }
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

    private String buildOrderHeaderSummary(ArrayList<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return getString(R.string.order_products_summary, 0);
        }

        StringBuilder builder = new StringBuilder();
        int limit = Math.min(2, items.size());
        for (int i = 0; i < limit; i++) {
            OrderItem item = items.get(i);
            if (i > 0) builder.append("\n");
            builder.append(item.tenSanPham).append(" x").append(item.soLuong);
        }
        if (items.size() > limit) {
            builder.append("\n+").append(items.size() - limit).append(" sản phẩm khác");
        }
        return builder.toString();
    }

    private void bindPaymentLifecycleInfo(Order order) {
        if (tvPaymentLifecycleInfo == null) {
            return;
        }
        String lifecycleInfo = buildPaymentLifecycleInfo(order);
        if (lifecycleInfo == null) {
            tvPaymentLifecycleInfo.setVisibility(View.GONE);
            tvPaymentLifecycleInfo.setText("");
            return;
        }
        tvPaymentLifecycleInfo.setVisibility(View.VISIBLE);
        tvPaymentLifecycleInfo.setText(lifecycleInfo);
    }

    private void bindRefundInfo(Order order) {
        if (tvRefundInfo == null) {
            return;
        }
        String refundInfo = buildRefundInfo(order);
        if (refundInfo == null) {
            tvRefundInfo.setVisibility(View.GONE);
            tvRefundInfo.setText("");
            return;
        }
        tvRefundInfo.setVisibility(View.VISIBLE);
        tvRefundInfo.setText(refundInfo);
    }

    private String buildPaymentLifecycleInfo(Order order) {
        if (order == null || !isBankTransfer(order)) {
            return null;
        }
        if (PaymentStatus.STATUS_CHO_THANH_TOAN.equals(order.trangThaiThanhToan) && order.hasPaymentDeadline()) {
            return getString(R.string.order_transfer_deadline_info, formatDate(order.paymentDeadline));
        }
        if (PaymentStatus.STATUS_HET_HAN_THANH_TOAN.equals(order.trangThaiThanhToan) && order.expiredAt > 0) {
            return getString(R.string.order_transfer_expired_info, formatDate(order.expiredAt));
        }
        if (order.cancelledAt > 0 && order.cancelReason != null && !order.cancelReason.trim().isEmpty()) {
            return getString(R.string.order_cancel_reason_info, order.cancelReason.trim(), formatDate(order.cancelledAt));
        }
        return null;
    }

    private String buildRefundInfo(Order order) {
        if (order == null || order.refundStatus == null || order.refundStatus.trim().isEmpty()) {
            return null;
        }
        if (Order.REFUND_STATUS_CHO_HOAN_TIEN.equals(order.refundStatus)) {
            String note = order.refundNote == null || order.refundNote.trim().isEmpty()
                    ? getString(R.string.order_refund_pending_short)
                    : order.refundNote.trim();
            return getString(R.string.order_refund_pending_info, note);
        }
        if (Order.REFUND_STATUS_DA_HOAN_TIEN.equals(order.refundStatus)) {
            return getString(R.string.order_refund_done_info, formatDate(order.refundedAt));
        }
        return null;
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) {
            return "-";
        }
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
