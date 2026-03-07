package com.example.phonestore.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.ProductDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import android.content.Intent;

import java.util.ArrayList;

public class AdminProductsActivity extends AppCompatActivity {

    private SessionManager session;
    private ProductDao productDao;
    private AdminProductAdapter adapter;
    private EditText edtSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_products);

        session = new SessionManager(this);
        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        productDao = new ProductDao(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.admin_products_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        edtSearch = findViewById(R.id.edtSearch);

        RecyclerView rv = findViewById(R.id.rvProducts);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminProductAdapter(new AdminProductAdapter.Listener() {
            @Override
            public void onEdit(Product product) {
                showProductFormDialog(product);
            }

            @Override
            public void onDelete(Product product) {
                confirmDelete(product);
            }
        });
        rv.setAdapter(adapter);

        MaterialButton btnAdd = findViewById(R.id.btnAddProduct);
        btnAdd.setOnClickListener(v -> showProductFormDialog(null));

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                loadData();
            }
        });

        loadData();
    }

    private void loadData() {
        String key = edtSearch.getText().toString().trim();
        ArrayList<Product> list = key.isEmpty() ? productDao.layTatCa() : productDao.timKiem(key);
        adapter.setData(list);
    }

    private void showProductFormDialog(Product oldProduct) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_admin_product_form, null, false);

        EditText edtName = view.findViewById(R.id.edtName);
        EditText edtBrand = view.findViewById(R.id.edtBrand);
        EditText edtPrice = view.findViewById(R.id.edtPrice);
        EditText edtStock = view.findViewById(R.id.edtStock);
        EditText edtDiscount = view.findViewById(R.id.edtDiscount);
        EditText edtDesc = view.findViewById(R.id.edtDesc);
        EditText edtImage = view.findViewById(R.id.edtImage);

        boolean editing = oldProduct != null;
        if (editing) {
            edtName.setText(oldProduct.tenSanPham);
            edtBrand.setText(oldProduct.hang);
            edtPrice.setText(String.valueOf(oldProduct.gia));
            edtStock.setText(String.valueOf(oldProduct.tonKho));
            edtDiscount.setText(String.valueOf(oldProduct.giamGia));
            edtDesc.setText(oldProduct.moTa);
            edtImage.setText(oldProduct.tenAnh);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? R.string.edit_product : R.string.add_product)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Product p = editing ? oldProduct : new Product();
            String error = fillAndValidateProductFromForm(p, edtName, edtBrand, edtPrice, edtStock, edtDiscount, edtDesc, edtImage);
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }

            boolean ok;
            if (editing) {
                ok = productDao.update(p);
            } else {
                ok = productDao.insert(p) != -1;
            }

            if (!ok) {
                Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, editing ? R.string.product_updated : R.string.product_added, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            loadData();
        }));

        dialog.show();
    }

    private String fillAndValidateProductFromForm(Product p,
                                                  EditText edtName,
                                                  EditText edtBrand,
                                                  EditText edtPrice,
                                                  EditText edtStock,
                                                  EditText edtDiscount,
                                                  EditText edtDesc,
                                                  EditText edtImage) {
        String name = edtName.getText().toString().trim();
        String brand = edtBrand.getText().toString().trim();
        String priceStr = edtPrice.getText().toString().trim();
        String stockStr = edtStock.getText().toString().trim();
        String discountStr = edtDiscount.getText().toString().trim();

        if (TextUtils.isEmpty(name)) return getString(R.string.err_name_required);
        if (TextUtils.isEmpty(priceStr)) return getString(R.string.err_price_required);
        if (TextUtils.isEmpty(stockStr)) return getString(R.string.err_stock_required);

        int price;
        int stock;
        int discount;

        try {
            price = Integer.parseInt(priceStr);
            stock = Integer.parseInt(stockStr);
            discount = TextUtils.isEmpty(discountStr) ? 0 : Integer.parseInt(discountStr);
        } catch (NumberFormatException e) {
            return getString(R.string.err_number_format);
        }

        if (price <= 0) return getString(R.string.err_price_invalid);
        if (stock < 0) return getString(R.string.err_stock_invalid);
        if (discount < 0 || discount > 100) return getString(R.string.err_discount_invalid);

        p.tenSanPham = name;
        p.hang = brand;
        p.gia = price;
        p.tonKho = stock;
        p.giamGia = discount;
        p.moTa = edtDesc.getText().toString().trim();
        p.tenAnh = edtImage.getText().toString().trim();
        return null;
    }

    private void confirmDelete(Product product) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_product)
                .setMessage(getString(R.string.delete_product_confirm, product.tenSanPham))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    boolean ok = productDao.delete(product.maSanPham);
                    if (ok) {
                        Toast.makeText(this, R.string.product_deleted, Toast.LENGTH_SHORT).show();
                        loadData();
                    } else {
                        Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
