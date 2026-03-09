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

import java.util.ArrayList;

public class AdminProductsActivity extends AppCompatActivity {

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
        toolbar.setTitle("Quản lý sản phẩm");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

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
        tvResultCount.setText("Hiển thị " + resultCount + " sản phẩm");
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
                .setTitle(editing ? "Cập nhật sản phẩm" : "Thêm sản phẩm mới")
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
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
                Toast.makeText(this, "Thao tác thất bại", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, editing ? "Cập nhật sản phẩm thành công" : "Thêm sản phẩm thành công", Toast.LENGTH_SHORT).show();
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

        if (TextUtils.isEmpty(name)) return "Vui lòng nhập tên sản phẩm";
        if (TextUtils.isEmpty(priceStr)) return "Vui lòng nhập giá";
        if (TextUtils.isEmpty(stockStr)) return "Vui lòng nhập tồn kho";

        int price;
        int stock;
        int discount;

        try {
            price = Integer.parseInt(priceStr);
            stock = Integer.parseInt(stockStr);
            discount = TextUtils.isEmpty(discountStr) ? 0 : Integer.parseInt(discountStr);
        } catch (NumberFormatException e) {
            return "Dữ liệu số không hợp lệ";
        }

        if (price <= 0) return "Giá phải lớn hơn 0";
        if (stock < 0) return "Tồn kho không được âm";
        if (discount < 0 || discount > 100) return "Giảm giá chỉ từ 0 đến 100";

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
                .setTitle("Xóa sản phẩm")
                .setMessage("Bạn có chắc muốn xóa \"" + product.tenSanPham + "\" không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, w) -> {
                    boolean ok = productDao.delete(product.maSanPham);
                    if (ok) {
                        Toast.makeText(this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                        loadData();
                    } else {
                        Toast.makeText(this, "Thao tác thất bại", Toast.LENGTH_SHORT).show();
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