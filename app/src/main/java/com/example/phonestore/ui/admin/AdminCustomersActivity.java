package com.example.phonestore.ui.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.UserDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.User;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

public class AdminCustomersActivity extends AppCompatActivity {

    private SessionManager session;
    private UserDao userDao;
    private AdminCustomersAdapter adapter;
    private EditText edtSearch;
    private TextView tvCustomersCount;
    private TextView tvCustomersFiltered;
    private TextView tvCustomerResultCount;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_customers);

        session = new SessionManager(this);
        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        userDao = new UserDao(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setTitleMarginStart(Math.round(getResources().getDisplayMetrics().density * 12));
        toolbar.setTitleMarginEnd(0);
        toolbar.setTitleMarginTop(0);
        toolbar.setTitleMarginBottom(0);
        toolbar.setTitle(R.string.admin_customers_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        edtSearch = findViewById(R.id.edtSearch);
        View cardCustomersCount = findViewById(R.id.cardCustomersCount);
        View cardCustomersFiltered = findViewById(R.id.cardCustomersFiltered);
        ((TextView) cardCustomersCount.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_customers_count);
        ((TextView) cardCustomersFiltered.findViewById(R.id.tvKpiLabel)).setText(R.string.admin_customers_search_results);
        tvCustomersCount = cardCustomersCount.findViewById(R.id.tvKpiValue);
        tvCustomersFiltered = cardCustomersFiltered.findViewById(R.id.tvKpiValue);
        tvCustomerResultCount = findViewById(R.id.tvCustomerResultCount);

        RecyclerView rv = findViewById(R.id.rvCustomers);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminCustomersAdapter(new AdminCustomersAdapter.Listener() {
            @Override
            public void onEdit(User user) {
                showCustomerDialog(user);
            }

            @Override
            public void onDelete(User user) {
                confirmDelete(user);
            }
        });
        rv.setAdapter(adapter);

        MaterialButton btnAdd = findViewById(R.id.btnAddCustomer);
        btnAdd.setOnClickListener(v -> showCustomerDialog(null));

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = AdminCustomersActivity.this::loadData;
                searchHandler.postDelayed(searchRunnable, 250);
            }
        });

        loadData();
    }

    private void loadData() {
        String key = edtSearch.getText().toString().trim();
        int totalCustomers = userDao.getSoKhachHang();
        java.util.ArrayList<User> filteredList = userDao.getCustomers(key);
        adapter.setData(filteredList);
        tvCustomersCount.setText(String.valueOf(totalCustomers));
        tvCustomersFiltered.setText(String.valueOf(filteredList.size()));
        tvCustomerResultCount.setText(getString(R.string.admin_customer_results, filteredList.size()));
    }

    private void showCustomerDialog(User oldUser) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_admin_customer_form, null, false);

        EditText edtFullName = view.findViewById(R.id.edtFullName);
        EditText edtUsername = view.findViewById(R.id.edtUsername);
        EditText edtPassword = view.findViewById(R.id.edtPassword);
        EditText edtConfirm = view.findViewById(R.id.edtConfirm);

        boolean editing = oldUser != null;
        if (editing) {
            edtFullName.setText(oldUser.fullname);
            edtUsername.setText(oldUser.username);
            edtUsername.setEnabled(false);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? R.string.edit_customer : R.string.add_customer)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String fullname = edtFullName.getText().toString().trim();
            String username = edtUsername.getText().toString().trim();
            String pass = edtPassword.getText().toString().trim();
            String confirm = edtConfirm.getText().toString().trim();

            if (TextUtils.isEmpty(fullname)) {
                Toast.makeText(this, R.string.err_fullname_required, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!editing) {
                if (TextUtils.isEmpty(username)) {
                    Toast.makeText(this, R.string.err_username_required, Toast.LENGTH_SHORT).show();
                    return;
                }

                String passError = validatePassword(pass, confirm, true);
                if (passError != null) {
                    Toast.makeText(this, passError, Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean ok = userDao.registerCustomer(fullname, username, pass);
                if (!ok) {
                    Toast.makeText(this, R.string.err_add_customer_failed, Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(this, R.string.customer_added, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadData();
                return;
            }

            String passError = validatePassword(pass, confirm, false);
            if (passError != null) {
                Toast.makeText(this, passError, Toast.LENGTH_SHORT).show();
                return;
            }

            boolean ok = userDao.adminUpdateCustomer(oldUser.id, fullname, pass.isEmpty() ? null : pass);
            if (!ok) {
                Toast.makeText(this, R.string.customer_update_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, R.string.customer_updated, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            loadData();
        }));

        dialog.show();
    }

    private String validatePassword(String pass, String confirm, boolean required) {
        if (required && TextUtils.isEmpty(pass)) return getString(R.string.err_password_required);
        if (required && TextUtils.isEmpty(confirm)) return getString(R.string.err_confirm_password_required);
        if (!required && pass.isEmpty() && confirm.isEmpty()) return null;
        if (!pass.equals(confirm)) return getString(R.string.err_password_not_match);
        return null;
    }

    private void confirmDelete(User user) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_customer_deactivate_title)
                .setMessage(getString(R.string.admin_customer_deactivate_confirm, user.username))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.admin_customer_deactivate_action, (d, w) -> {
                    boolean ok = userDao.adminDeleteCustomer(user.id);
                    if (!ok) {
                        Toast.makeText(this, R.string.err_delete_customer_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(this, R.string.customer_deleted, Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
