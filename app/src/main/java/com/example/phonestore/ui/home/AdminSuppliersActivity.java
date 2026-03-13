package com.example.phonestore.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.ReceiptDao;
import com.example.phonestore.data.dao.SupplierDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Supplier;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;

public class AdminSuppliersActivity extends BaseHomeActivity {

    private SupplierDao supplierDao;
    private ReceiptDao receiptDao;
    private SupplierAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }
        supplierDao = new SupplierDao(this);
        receiptDao = new ReceiptDao(this);
    }

    @Override
    protected int shellLayoutRes() {
        return R.layout.activity_admin_suppliers;
    }

    @Override
    protected int contentLayoutRes() {
        return 0;
    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_admin;
    }

    @Override
    protected int selectedBottomNavItemId() {
        return R.id.nav_admin_inventory;
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.admin_suppliers_title);
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
        RecyclerView rvSuppliers = findViewById(R.id.rvSuppliers);
        rvSuppliers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SupplierAdapter(this::showSupplierDialog, this::confirmDeleteSupplier);
        rvSuppliers.setAdapter(adapter);

        findViewById(R.id.btnAddSupplier).setOnClickListener(v -> showSupplierDialog(null));

        EditText edtSearch = findViewById(R.id.edtSupplierSearch);
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                loadData(s.toString().trim());
            }
        });

        loadData("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (supplierDao != null) {
            loadData(currentKeyword());
        }
    }

    private void loadData(String keyword) {
        ArrayList<Supplier> suppliers = supplierDao.getAll(keyword);
        adapter.setData(suppliers);
    }

    private String currentKeyword() {
        EditText edtSearch = findViewById(R.id.edtSupplierSearch);
        return edtSearch == null ? "" : edtSearch.getText().toString().trim();
    }

    private void confirmDeleteSupplier(Supplier supplier) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_supplier_title)
                .setMessage(getString(R.string.supplier_delete_confirm, supplier.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    if (receiptDao.hasReceiptForSupplier(supplier.id)) {
                        Toast.makeText(this, R.string.supplier_in_use, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    boolean ok = supplierDao.delete(supplier.id);
                    if (!ok) {
                        Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(this, R.string.supplier_deleted, Toast.LENGTH_SHORT).show();
                    loadData(currentKeyword());
                })
                .show();
    }

    private void showSupplierDialog(Supplier oldSupplier) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_supplier_form, null, false);
        TextView tvTitle = view.findViewById(R.id.tvSupplierDialogTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvSupplierDialogSubtitle);
        EditText edtName = view.findViewById(R.id.edtSupplierName);
        EditText edtBrand = view.findViewById(R.id.edtSupplierBrand);
        EditText edtPhone = view.findViewById(R.id.edtSupplierPhone);
        EditText edtAddress = view.findViewById(R.id.edtSupplierAddress);
        View btnCancel = view.findViewById(R.id.btnCancelSupplier);
        View btnSave = view.findViewById(R.id.btnSaveSupplier);

        boolean editing = oldSupplier != null;
        tvTitle.setText(editing ? R.string.supplier_form_edit_title : R.string.supplier_form_add_title);
        tvSubtitle.setText(editing ? R.string.supplier_form_edit_subtitle : R.string.supplier_form_add_subtitle);

        if (editing) {
            edtName.setText(oldSupplier.name);
            edtBrand.setText(oldSupplier.brand);
            edtPhone.setText(oldSupplier.phone);
            edtAddress.setText(oldSupplier.address);
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);
        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) {
                return;
            }
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, R.string.err_supplier_name_required, Toast.LENGTH_SHORT).show();
                return;
            }

            Supplier supplier = editing ? oldSupplier : new Supplier();
            supplier.name = name;
            supplier.brand = edtBrand.getText().toString().trim();
            supplier.phone = edtPhone.getText().toString().trim();
            supplier.address = edtAddress.getText().toString().trim();

            boolean ok = editing ? supplierDao.update(supplier) : supplierDao.insert(supplier) != -1;
            if (!ok) {
                Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, editing ? R.string.supplier_updated : R.string.supplier_added, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            loadData(currentKeyword());
        });
        dialog.show();
    }
}
