package com.example.phonestore.ui.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.ProductDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.home.BaseHomeActivity;
import com.example.phonestore.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class AdminProductsActivity extends BaseHomeActivity {

    private SessionManager session;
    private ProductDao productDao;
    private AdminProductAdapter adapter;

    private EditText edtSearch;
    private RecyclerView rvProducts;
    private View layoutEmpty;
    private TextView tvTotalProducts;
    private TextView tvInStockProducts;
    private TextView tvLowStockProducts;
    private TextView tvResultCount;

    @Override
    protected int shellLayoutRes() {
        return R.layout.activity_home_bottom_admin;
    }

    @Override
    protected int contentLayoutRes() {
        return R.layout.activity_admin_products;
    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_admin;
    }

    @Override
    protected int selectedBottomNavItemId() {
        return R.id.nav_products;
    }

    @Override
    protected String screenTitle() {
        return getString(R.string.admin_products_title);
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (!session.isLoggedIn() || !DBHelper.ROLE_ADMIN.equals(session.getRole())) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        productDao = new ProductDao(this);

        edtSearch = findViewById(R.id.edtSearch);
        rvProducts = findViewById(R.id.rvProducts);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvTotalProducts = findViewById(R.id.tvTotalProducts);
        tvInStockProducts = findViewById(R.id.tvInStockProducts);
        tvLowStockProducts = findViewById(R.id.tvLowStockProducts);
        tvResultCount = findViewById(R.id.tvResultCount);

        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        rvProducts.setHasFixedSize(true);

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
        rvProducts.setAdapter(adapter);

        MaterialButton btnAdd = findViewById(R.id.btnAddProduct);
        btnAdd.setOnClickListener(v -> showProductFormDialog(null));

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                loadData();
            }
        });

        loadData();
    }

    private void loadData() {
        ArrayList<Product> allProducts = productDao.layTatCa();
        String key = edtSearch.getText().toString().trim();
        ArrayList<Product> filteredProducts = key.isEmpty() ? allProducts : productDao.timKiem(key);

        adapter.setData(filteredProducts);
        updateDashboard(allProducts, filteredProducts);
        updateEmptyState(filteredProducts);
    }

    private void updateDashboard(ArrayList<Product> allProducts, ArrayList<Product> filteredProducts) {
        int total = allProducts == null ? 0 : allProducts.size();
        int inStock = 0;
        int lowStock = 0;

        if (allProducts != null) {
            for (Product p : allProducts) {
                if (p == null) continue;
                if (p.tonKho > 0) inStock++;
                if (p.tonKho > 0 && p.tonKho <= 5) lowStock++;
            }
        }

        tvTotalProducts.setText(String.valueOf(total));
        tvInStockProducts.setText(String.valueOf(inStock));
        tvLowStockProducts.setText(String.valueOf(lowStock));

        int resultCount = filteredProducts == null ? 0 : filteredProducts.size();
        tvResultCount.setText(getString(R.string.admin_product_results, resultCount));
    }

    private void updateEmptyState(ArrayList<Product> list) {
        boolean isEmpty = list == null || list.isEmpty();
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvProducts.setVisibility(isEmpty ? View.INVISIBLE : View.VISIBLE);
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
                .setTitle(editing ? R.string.admin_product_dialog_edit : R.string.admin_product_dialog_add)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Product p = editing ? oldProduct : new Product();
            String error = fillAndValidateProductFromForm(
                    p, edtName, edtBrand, edtPrice, edtStock, edtDiscount, edtDesc, edtImage
            );

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

            Toast.makeText(this, editing ? R.string.admin_product_updated_success : R.string.admin_product_added_success, Toast.LENGTH_SHORT).show();
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
                .setTitle(R.string.admin_delete_product_title)
                .setMessage(getString(R.string.admin_delete_product_message, product.tenSanPham))
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
    protected void onResume() {
        super.onResume();
        if (productDao != null && adapter != null) {
            loadData();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return false;
    }
}