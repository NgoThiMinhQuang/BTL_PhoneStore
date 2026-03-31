package com.example.phonestore.ui.checkout;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.CartDao;
import com.example.phonestore.data.dao.OrderDao;
import com.example.phonestore.data.model.CheckoutInfo;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.cart.CartActivity;
import com.example.phonestore.ui.home.BaseHomeActivity;
import com.example.phonestore.ui.orders.OrdersActivity;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class CheckoutActivity extends BaseHomeActivity {

    public static final String EXTRA_BUY_NOW_PRODUCT_ID = "extra_buy_now_product_id";
    public static final String EXTRA_BUY_NOW_QTY = "extra_buy_now_qty";
    public static final String EXTRA_BUY_NOW_STORAGE = "extra_buy_now_storage";
    public static final String EXTRA_BUY_NOW_COLOR = "extra_buy_now_color";

    private CartDao cartDao;
    private OrderDao orderDao;

    private TextView tvTotal;
    private TextView tvSubtotal;
    private TextView tvShippingFee;
    private TextView tvDiscountAmount;
    private TextView tvDiscountMessage;
    private EditText edtName, edtPhone, edtAddress, edtNote, edtDiscountCode;
    private RadioButton rbCod, rbBank;

    private final ArrayList<Long> selectedCartItemIds = new ArrayList<>();
    private long buyNowProductId = -1;
    private int buyNowQty = 1;
    private String buyNowStorage = "";
    private String buyNowColor = "";
    private String appliedDiscountCode;
    private int subtotal;
    private int discountAmount;
    private int shippingFee;
    private int totalAmount;

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
        orderDao = new OrderDao(this);

        tvTotal = findViewById(R.id.tvTotal);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvShippingFee = findViewById(R.id.tvShippingFee);
        tvDiscountAmount = findViewById(R.id.tvDiscountAmount);
        tvDiscountMessage = findViewById(R.id.tvDiscountMessage);
        edtName = findViewById(R.id.edtNguoiNhan);
        edtPhone = findViewById(R.id.edtSdtNhan);
        edtAddress = findViewById(R.id.edtDiaChiNhan);
        edtNote = findViewById(R.id.edtGhiChu);
        edtDiscountCode = findViewById(R.id.edtDiscountCode);
        rbCod = findViewById(R.id.rbCod);
        rbBank = findViewById(R.id.rbChuyenKhoan);

        long[] selectedIds = getIntent().getLongArrayExtra(CartActivity.EXTRA_SELECTED_CART_ITEM_IDS);
        if (selectedIds != null) {
            for (long id : selectedIds) {
                selectedCartItemIds.add(id);
            }
        }

        buyNowProductId = getIntent().getLongExtra(EXTRA_BUY_NOW_PRODUCT_ID, -1);
        buyNowQty = Math.max(1, getIntent().getIntExtra(EXTRA_BUY_NOW_QTY, 1));
        buyNowStorage = normalizeVariant(getIntent().getStringExtra(EXTRA_BUY_NOW_STORAGE));
        buyNowColor = normalizeVariant(getIntent().getStringExtra(EXTRA_BUY_NOW_COLOR));

        appliedDiscountCode = CheckoutInfo.normalizeDiscountCode(getIntent().getStringExtra(CartActivity.EXTRA_DISCOUNT_CODE));
        if (appliedDiscountCode != null) {
            edtDiscountCode.setText(appliedDiscountCode);
        }

        orderDao.reconcileExpiredTransferOrders();
        recalculateSummary();
        if (subtotal <= 0) {
            Toast.makeText(this, R.string.empty_cart, Toast.LENGTH_SHORT).show();
            Intent cartIntent = new Intent(this, CartActivity.class);
            cartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(cartIntent);
            finish();
            return;
        }

        MaterialButton btnApplyDiscount = findViewById(R.id.btnApplyDiscount);
        btnApplyDiscount.setOnClickListener(v -> {
            appliedDiscountCode = CheckoutInfo.normalizeDiscountCode(edtDiscountCode.getText().toString());
            recalculateSummary();
        });

        MaterialButton btnConfirm = findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(v -> submitCheckout());
    }

    @Override
    protected int contentLayoutRes() {
        return R.layout.activity_checkout;
    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_customer;
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.checkout_title);
    }

    @Override
    protected int selectedBottomNavItemId() {
        return R.id.nav_cart;
    }

    @Override
    protected boolean shouldShowBackButton() {
        return true;
    }

    @Override
    protected boolean isBottomNavRootScreen() {
        return false;
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    private void recalculateSummary() {
        if (buyNowProductId > 0) {
            subtotal = orderDao.previewSingleProductTotal(buyNowProductId, buyNowQty);
        } else {
            subtotal = selectedCartItemIds.isEmpty()
                    ? cartDao.getTotal(session.getUserId())
                    : cartDao.getTotalByIds(session.getUserId(), selectedCartItemIds);
        }
        shippingFee = subtotal > 0 ? CheckoutInfo.SHIPPING_FEE : 0;
        discountAmount = CheckoutInfo.calculateDiscount(appliedDiscountCode, subtotal);
        totalAmount = Math.max(0, subtotal + shippingFee - discountAmount);

        tvSubtotal.setText(formatMoney(subtotal));
        tvShippingFee.setText(formatMoney(shippingFee));
        tvDiscountAmount.setText(formatMoney(discountAmount));
        tvTotal.setText(getString(R.string.order_total_summary, formatMoney(totalAmount)));

        if (appliedDiscountCode == null) {
            tvDiscountMessage.setVisibility(android.view.View.GONE);
        } else if (discountAmount > 0) {
            tvDiscountMessage.setVisibility(android.view.View.VISIBLE);
            tvDiscountMessage.setText(getString(R.string.discount_applied_message, appliedDiscountCode, formatMoney(discountAmount)));
        } else {
            tvDiscountMessage.setVisibility(android.view.View.VISIBLE);
            tvDiscountMessage.setText(R.string.discount_invalid_message);
        }
    }

    private void submitCheckout() {
        String name = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String note = edtNote.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(address)) {
            Toast.makeText(this, R.string.checkout_required_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.matches("0\\d{9}")) {
            Toast.makeText(this, R.string.invalid_phone, Toast.LENGTH_SHORT).show();
            return;
        }

        String method = rbBank.isChecked()
                ? CheckoutInfo.PAYMENT_BANK_TRANSFER
                : CheckoutInfo.PAYMENT_COD;

        CheckoutInfo info = new CheckoutInfo();
        info.receiverName = name;
        info.receiverPhone = phone;
        info.receiverAddress = address;
        info.note = note;
        info.paymentMethod = method;
        info.discountCode = appliedDiscountCode;
        info.discountAmount = discountAmount;
        info.shippingFee = shippingFee;
        info.subtotal = subtotal;
        info.totalAmount = totalAmount;

        if (CheckoutInfo.PAYMENT_BANK_TRANSFER.equals(method)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.bank_transfer)
                    .setMessage(R.string.bank_transfer_confirm)
                    .setNegativeButton(R.string.abort, null)
                    .setPositiveButton(R.string.bank_transfer_create_order, (dialog, which) -> createOrder(info))
                    .show();
        } else {
            createOrder(info);
        }
    }

    private void createOrder(CheckoutInfo info) {
        long orderId;
        if (buyNowProductId > 0) {
            orderId = orderDao.checkoutSingleProduct(
                    session.getUserId(),
                    buyNowProductId,
                    buyNowQty,
                    buyNowStorage,
                    buyNowColor,
                    info
            );
        } else if (selectedCartItemIds.isEmpty()) {
            orderId = orderDao.checkout(session.getUserId(), info);
        } else {
            orderId = orderDao.checkout(session.getUserId(), info, selectedCartItemIds);
        }

        if (orderId == -1) {
            String error = orderDao.getLastCheckoutError();
            if (error == null || error.trim().isEmpty()) {
                error = getString(R.string.checkout_failed);
            }
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            return;
        }

        Intent ordersIntent = new Intent(this, OrdersActivity.class);
        ordersIntent.putExtra(OrdersActivity.EXTRA_SHOW_CHECKOUT_SUCCESS, true);
        ordersIntent.putExtra(OrdersActivity.EXTRA_CREATED_ORDER_ID, orderId);
        ordersIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(ordersIntent);
        finish();
        overridePendingTransition(0, 0);
    }

    private String formatMoney(int amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + "đ";
    }

    private String normalizeVariant(String value) {
        return value == null ? "" : value.trim();
    }
}
