package com.example.phonestore.ui.products;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.ProductDao;

public class ProductsActivity extends AppCompatActivity {

    public static final String EXTRA_BRAND = "extra_brand"; // dùng để query DB
    public static final String EXTRA_TITLE = "extra_title"; // dùng để hiển thị title

    private ProductDao productDao;
    private ProductAdapter adapter;

    private String selectedBrand; // null nếu "View all"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        Toolbar toolbar = findViewById(R.id.toolbar);

        // Nhận filter từ Home
        selectedBrand = getIntent().getStringExtra(EXTRA_BRAND);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        if (title != null && !title.trim().isEmpty()) {
            toolbar.setTitle("Sản phẩm - " + title);
        } else if (selectedBrand != null && !selectedBrand.trim().isEmpty()) {
            toolbar.setTitle("Sản phẩm - " + selectedBrand);
        } else {
            toolbar.setTitle("Sản phẩm");
        }

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        productDao = new ProductDao(this);

        RecyclerView rv = findViewById(R.id.rvProducts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter();
        rv.setAdapter(adapter);

        // load lần đầu
        loadData("");

        // search
        android.widget.EditText edtSearch = findViewById(R.id.edtSearch);
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                loadData(s.toString().trim());
            }
        });
    }

    private void loadData(String key) {
        boolean hasBrand = selectedBrand != null && !selectedBrand.trim().isEmpty();

        if (!hasBrand) {
            adapter.setData(key.isEmpty() ? productDao.layTatCa() : productDao.timKiem(key));
        } else {
            adapter.setData(key.isEmpty() ? productDao.layTheoHang(selectedBrand)
                    : productDao.timKiemTheoHang(selectedBrand, key));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}