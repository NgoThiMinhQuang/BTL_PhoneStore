package com.example.phonestore.ui.products;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.ProductDao;

public class ProductsActivity extends AppCompatActivity {

    public static final String EXTRA_BRAND = "extra_brand";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_QUERY = "extra_query"; // NEW

    private ProductDao productDao;
    private ProductAdapter adapter;

    private String selectedBrand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        Toolbar toolbar = findViewById(R.id.toolbar);

        selectedBrand = getIntent().getStringExtra(EXTRA_BRAND);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        toolbar.setContentInsetsRelative(0, 0);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.setTitleMarginStart(Math.round(getResources().getDisplayMetrics().density * 12));
        toolbar.setTitleMarginEnd(0);
        toolbar.setTitleMarginTop(0);
        toolbar.setTitleMarginBottom(0);

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

        EditText edtSearch = findViewById(R.id.edtSearch);

        String initKey = getIntent().getStringExtra(EXTRA_QUERY);
        if (initKey == null) initKey = "";
        initKey = initKey.trim();

        edtSearch.setText(initKey);
        edtSearch.setSelection(edtSearch.getText().length());

        loadData(initKey);

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