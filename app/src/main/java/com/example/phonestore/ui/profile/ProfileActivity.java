package com.example.phonestore.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.OrderDao;
import com.example.phonestore.data.dao.UserDao;
import com.example.phonestore.data.model.Order;
import com.example.phonestore.data.model.OrderStatus;
import com.example.phonestore.data.model.User;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.home.BaseHomeActivity;
import com.example.phonestore.ui.orders.OrdersActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ProfileActivity extends BaseHomeActivity {

    private UserDao userDao;
    private OrderDao orderDao;

    private TextView tvFullName;
    private TextView tvUsername;
    private TextView tvOrdersCount;
    private TextView tvProcessingCount;
    private TextView tvSpendTotal;
    private View itemOrdersOverview;
    private View itemEditProfile;
    private View itemChangePassword;
    private View itemLogout;

    private long userId = -1;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        userId = session.getUserId();
        if (userId == -1) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        userDao = new UserDao(this);
        orderDao = new OrderDao(this);

        bindViews();
        bindActions();
        loadProfile();
    }

    @Override
    protected int shellLayoutRes() {
        return isAdminSession() ? R.layout.activity_home_bottom_admin : super.shellLayoutRes();
    }

    @Override
    protected int contentLayoutRes() {
        return R.layout.activity_profile;
    }

    @Override
    protected int bottomMenuRes() {
        return isAdminSession() ? R.menu.menu_bottom_admin : R.menu.menu_bottom_customer;
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.profile);
    }

    @Override
    protected int selectedBottomNavItemId() {
        return isAdminSession() ? View.NO_ID : R.id.nav_profile;
    }

    @Override
    protected boolean shouldShowBackButton() {
        return isAdminSession();
    }

    @Override
    protected boolean shouldUseAdminBackButtonStyling() {
        return isAdminSession();
    }

    @Override
    protected boolean shouldShowBottomNavigation() {
        return !isAdminSession();
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userDao != null && orderDao != null && userId > 0) {
            loadProfile();
        }
    }

    private void bindViews() {
        tvFullName = findViewById(R.id.tvFullName);
        tvUsername = findViewById(R.id.tvUsername);
        tvOrdersCount = findViewById(R.id.tvOrdersCount);
        tvProcessingCount = findViewById(R.id.tvProcessingCount);
        tvSpendTotal = findViewById(R.id.tvSpendTotal);
        itemOrdersOverview = findViewById(R.id.itemOrdersOverview);
        itemEditProfile = findViewById(R.id.itemEditProfile);
        itemChangePassword = findViewById(R.id.itemChangePassword);
        itemLogout = findViewById(R.id.itemLogout);
    }

    private void bindActions() {
        itemOrdersOverview.setOnClickListener(v -> startActivity(new Intent(this, OrdersActivity.class)));
        itemEditProfile.setOnClickListener(v -> showEditProfileDialog());
        itemChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        itemLogout.setOnClickListener(v -> confirmLogout());
    }

    private void loadProfile() {
        User user = userDao.getById(userId);
        if (user == null) {
            Toast.makeText(this, R.string.profile_user_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUser = user;

        tvFullName.setText(valueOrFallback(user.fullname, user.username));
        tvUsername.setText(getString(R.string.profile_username_handle, user.username));

        ArrayList<Order> orders = orderDao.getOrdersByUser(userId);
        int processingCount = 0;
        int deliveredSpend = 0;
        for (Order order : orders) {
            if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(order.trangThaiDon)
                    || OrderStatus.STATUS_DANG_XU_LY.equals(order.trangThaiDon)) {
                processingCount++;
            }
            if (OrderStatus.STATUS_DA_GIAO.equals(order.trangThaiDon)) {
                deliveredSpend += order.tongTien;
            }
        }

        tvOrdersCount.setText(String.valueOf(orders.size()));
        tvProcessingCount.setText(String.valueOf(processingCount));
        tvSpendTotal.setText(formatCompactMoney(deliveredSpend));
    }

    private void showEditProfileDialog() {
        EditText edtFullName = createInput(
                getString(R.string.profile_fullname_hint),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );
        edtFullName.setText(currentUser == null ? "" : valueOrFallback(currentUser.fullname, ""));
        edtFullName.setSelection(edtFullName.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_edit_dialog_title)
                .setView(wrapInput(edtFullName))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> saveFullName(edtFullName.getText().toString().trim()))
                .show();
    }

    private void showChangePasswordDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        container.setPadding(padding, padding, padding, dp(4));

        EditText edtOldPass = createInput(
                getString(R.string.profile_old_password_hint),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        EditText edtNewPass = createInput(
                getString(R.string.profile_new_password_hint),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        EditText edtConfirm = createInput(
                getString(R.string.profile_confirm_password_hint),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        addInput(container, edtOldPass, 0);
        addInput(container, edtNewPass, 12);
        addInput(container, edtConfirm, 12);

        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_change_password_title)
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> savePassword(
                        edtOldPass.getText().toString().trim(),
                        edtNewPass.getText().toString().trim(),
                        edtConfirm.getText().toString().trim()
                ))
                .show();
    }

    private void saveFullName(String fullname) {
        if (TextUtils.isEmpty(fullname)) {
            Toast.makeText(this, R.string.profile_name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean ok = userDao.updateFullName(userId, fullname);
        if (!ok) {
            Toast.makeText(this, R.string.profile_update_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.profile_update_success, Toast.LENGTH_SHORT).show();
        loadProfile();
    }

    private void savePassword(String oldPass, String newPass, String confirm) {
        if (TextUtils.isEmpty(oldPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, R.string.profile_password_incomplete, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPass.equals(confirm)) {
            Toast.makeText(this, R.string.profile_password_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPass.length() < 6) {
            Toast.makeText(this, "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPass.equals(oldPass)) {
            Toast.makeText(this, "Mật khẩu mới phải khác mật khẩu cũ", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean ok = userDao.changePassword(userId, oldPass, newPass);
        if (!ok) {
            Toast.makeText(this, R.string.profile_old_password_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.profile_update_success, Toast.LENGTH_SHORT).show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    session.clear();
                    Intent intent = new Intent(this, WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private EditText createInput(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(inputType);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setBackgroundResource(R.drawable.bg_edittext);
        return input;
    }

    private LinearLayout wrapInput(EditText input) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        container.setPadding(padding, padding, padding, dp(4));
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return container;
    }

    private void addInput(LinearLayout container, EditText input, int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(topMarginDp);
        container.addView(input, params);
    }

    private String formatCompactMoney(int amount) {
        if (amount >= 1_000_000) {
            int millions = amount / 1_000_000;
            return millions + "M";
        }
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount);
    }

    private String valueOrFallback(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

}
