package com.example.phonestore.ui.checkout;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.CartDao;
import com.example.phonestore.data.dao.OrderDao;
import com.example.phonestore.data.model.CheckoutInfo;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.cart.CartActivity;
import com.example.phonestore.ui.orders.OrdersActivity;
import com.example.phonestore.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private SessionManager session;
    private CartDao cartDao;
    private OrderDao orderDao;

    private TextView tvTotal;
    private EditText edtName, edtPhone, edtAddress, edtNote;
    private RadioButton rbCod, rbBank;

    private final ArrayList<Long> selectedProductIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getUserId() <= 0) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        cartDao = new CartDao(this);
        orderDao = new OrderDao(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.checkout_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvTotal = findViewById(R.id.tvTotal);
        edtName = findViewById(R.id.edtNguoiNhan);
        edtPhone = findViewById(R.id.edtSdtNhan);
        edtAddress = findViewById(R.id.edtDiaChiNhan);
        edtNote = findViewById(R.id.edtGhiChu);
        rbCod = findViewById(R.id.rbCod);
        rbBank = findViewById(R.id.rbChuyenKhoan);

        long[] selectedIds = getIntent().getLongArrayExtra(CartActivity.EXTRA_SELECTED_PRODUCT_IDS);
        if (selectedIds != null) {
            for (long id : selectedIds) {
                selectedProductIds.add(id);
            }
        }

        int total = selectedProductIds.isEmpty()
                ? cartDao.getTotal(session.getUserId())
                : cartDao.getTotalByProductIds(session.getUserId(), selectedProductIds);

        String totalText = NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(total) + "đ";
        tvTotal.setText(getString(R.string.checkout_total, totalText));

        if (total <= 0) {
            Toast.makeText(this, R.string.empty_cart, Toast.LENGTH_SHORT).show();
            Intent ordersIntent = new Intent(this, OrdersActivity.class);
            ordersIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(ordersIntent);
            finish();
            return;
        }

        MaterialButton btnConfirm = findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(v -> submitCheckout());
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

        if (phone.length() < 9 || phone.length() > 11) {
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

        if (CheckoutInfo.PAYMENT_BANK_TRANSFER.equals(method)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.bank_transfer)
                    .setMessage(R.string.bank_transfer_confirm)
                    .setNegativeButton(R.string.abort, null)
                    .setPositiveButton(R.string.success, (dialog, which) -> createOrder(info))
                    .show();
        } else {
            createOrder(info);
        }
    }

    private void createOrder(CheckoutInfo info) {
        long orderId = selectedProductIds.isEmpty()
                ? orderDao.checkout(session.getUserId(), info)
                : orderDao.checkout(session.getUserId(), info, selectedProductIds);

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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}