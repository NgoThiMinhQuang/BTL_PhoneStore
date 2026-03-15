package com.example.phonestore.ui.products;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.ProductDao;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.ui.home.BaseHomeActivity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

public class ProductsActivity extends BaseHomeActivity {

    public static final String EXTRA_BRAND = "extra_brand";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_QUERY = "extra_query";

    private static final String SORT_DEFAULT = "default";
    private static final String SORT_PRICE_ASC = "price_asc";
    private static final String SORT_PRICE_DESC = "price_desc";
    private static final String SORT_NAME_ASC = "name_asc";
    private static final String SORT_DISCOUNT_DESC = "discount_desc";

    private ProductDao productDao;
    private ProductAdapter adapter;

    private EditText edtSearch;
    private TextView tvResultCount;
    private LinearLayout layoutBrandFilters;
    private View layoutEmpty;
    private Spinner spSort;
    private Spinner spPrice;
    private Spinner spOs;
    private Spinner spRom;
    private TextView chipInStock;
    private TextView chipDiscounted;

    private final ProductDao.ProductFilter filter = new ProductDao.ProductFilter();
    private final ArrayList<String> brands = new ArrayList<>();
    private String toolbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        filter.brand = trimToNull(getIntent().getStringExtra(EXTRA_BRAND));
        String title = trimToNull(getIntent().getStringExtra(EXTRA_TITLE));
        toolbarTitle = title != null ? title : filter.brand;

        super.onCreate(savedInstanceState);

        productDao = new ProductDao(this);

        RecyclerView rv = findViewById(R.id.rvProducts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter();
        rv.setAdapter(adapter);

        edtSearch = findViewById(R.id.edtSearch);
        tvResultCount = findViewById(R.id.tvResultCount);
        layoutBrandFilters = findViewById(R.id.layoutBrandFilters);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        spSort = findViewById(R.id.spSort);
        spPrice = findViewById(R.id.spPrice);
        spOs = findViewById(R.id.spOs);
        spRom = findViewById(R.id.spRom);
        chipInStock = findViewById(R.id.chipInStock);
        chipDiscounted = findViewById(R.id.chipDiscounted);

        String initKey = getIntent().getStringExtra(EXTRA_QUERY);
        if (initKey == null) initKey = "";
        initKey = initKey.trim();
        filter.keyword = trimToNull(initKey);
        filter.sortMode = SORT_DEFAULT;

        edtSearch.setText(initKey);
        edtSearch.setSelection(edtSearch.getText().length());

        setupBrandFilters();
        setupSpinners();
        setupToggles();
        chipInStock.setTextColor(getColor(R.color.text_primary));
        chipDiscounted.setTextColor(getColor(R.color.text_primary));
        bindSearch();
        loadData();
    }

    @Override
    protected int contentLayoutRes() {
        return R.layout.activity_products;
    }

    @Override
    protected int bottomMenuRes() {
        return R.menu.menu_bottom_customer;
    }

    @Override
    protected String screenTitle() {
        return toolbarTitle == null ? getString(R.string.nav_products_label) : getString(R.string.products_title_with_context, toolbarTitle);
    }

    @Override
    protected int selectedBottomNavItemId() {
        return R.id.nav_products;
    }

    @Override
    protected boolean shouldShowToolbarActions() {
        return false;
    }

    private void bindSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                filter.keyword = trimToNull(s.toString());
                loadData();
            }
        });
    }

    private void setupBrandFilters() {
        LinkedHashSet<String> uniqueBrands = new LinkedHashSet<>();
        for (Product product : productDao.layTatCa()) {
            String brand = trimToNull(product.hang);
            if (brand != null) uniqueBrands.add(brand);
        }
        brands.clear();
        brands.add(getString(R.string.filter_all));
        brands.addAll(uniqueBrands);
        renderBrandFilters();
    }

    private void renderBrandFilters() {
        layoutBrandFilters.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        String allLabel = getString(R.string.filter_all);
        for (String brand : brands) {
            TextView chip = (TextView) inflater.inflate(R.layout.item_filter_chip, layoutBrandFilters, false);
            chip.setText(brand);
            boolean isAll = allLabel.equals(brand);
            boolean selected = isAll ? filter.brand == null : brand.equalsIgnoreCase(filter.brand == null ? "" : filter.brand);
            chip.setSelected(selected);
            chip.setTextColor(getColor(selected ? R.color.red_primary : R.color.text_primary));
            chip.setOnClickListener(v -> {
                filter.brand = isAll ? null : brand;
                renderBrandFilters();
                loadData();
            });
            layoutBrandFilters.addView(chip);
        }
    }

    private void setupSpinners() {
        bindSpinner(spSort,
                new String[]{"Mặc định", "Giá thấp đến cao", "Giá cao đến thấp", "Tên A-Z", "Giảm giá cao"},
                position -> {
                    if (position == 1) filter.sortMode = SORT_PRICE_ASC;
                    else if (position == 2) filter.sortMode = SORT_PRICE_DESC;
                    else if (position == 3) filter.sortMode = SORT_NAME_ASC;
                    else if (position == 4) filter.sortMode = SORT_DISCOUNT_DESC;
                    else filter.sortMode = SORT_DEFAULT;
                    loadData();
                });

        bindSpinner(spPrice,
                new String[]{"Tất cả giá", "Dưới 10 triệu", "10 - 20 triệu", "20 - 30 triệu", "Trên 30 triệu"},
                position -> {
                    filter.minPrice = 0;
                    filter.maxPrice = 0;
                    if (position == 1) {
                        filter.maxPrice = 10_000_000;
                    } else if (position == 2) {
                        filter.minPrice = 10_000_000;
                        filter.maxPrice = 20_000_000;
                    } else if (position == 3) {
                        filter.minPrice = 20_000_000;
                        filter.maxPrice = 30_000_000;
                    } else if (position == 4) {
                        filter.minPrice = 30_000_000;
                    }
                    loadData();
                });

        bindSpinner(spOs,
                new String[]{"Tất cả HĐH", "iOS", "Android"},
                position -> {
                    filter.operatingSystem = position == 1 ? "iOS" : position == 2 ? "Android" : null;
                    loadData();
                });

        bindSpinner(spRom,
                new String[]{"Tất cả ROM", "<= 128GB", "256GB", "512GB", ">= 1TB"},
                position -> {
                    filter.minRomGb = 0;
                    filter.maxRomGb = 0;
                    if (position == 1) {
                        filter.maxRomGb = 128;
                    } else if (position == 2) {
                        filter.minRomGb = 256;
                        filter.maxRomGb = 256;
                    } else if (position == 3) {
                        filter.minRomGb = 512;
                        filter.maxRomGb = 512;
                    } else if (position == 4) {
                        filter.minRomGb = 1024;
                    }
                    loadData();
                });
    }

    private void bindSpinner(Spinner spinner, String[] items, OnSpinnerChanged listener) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                listener.onChanged(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupToggles() {
        chipInStock.setOnClickListener(v -> {
            chipInStock.setSelected(!chipInStock.isSelected());
            chipInStock.setTextColor(getColor(chipInStock.isSelected() ? R.color.red_primary : R.color.text_primary));
            filter.onlyInStock = chipInStock.isSelected();
            loadData();
        });

        chipDiscounted.setOnClickListener(v -> {
            chipDiscounted.setSelected(!chipDiscounted.isSelected());
            chipDiscounted.setTextColor(getColor(chipDiscounted.isSelected() ? R.color.red_primary : R.color.text_primary));
            filter.onlyDiscounted = chipDiscounted.isSelected();
            loadData();
        });
    }

    private void loadData() {
        ArrayList<Product> products = productDao.locSanPham(filter);
        adapter.setData(products);
        tvResultCount.setText(getString(R.string.products_found_count, products.size()));
        layoutEmpty.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private interface OnSpinnerChanged {
        void onChanged(int position);
    }
}
