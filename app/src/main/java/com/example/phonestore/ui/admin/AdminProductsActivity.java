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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.phonestore.utils.ProductImageLoader;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.ProductDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.ui.home.BaseHomeActivity;
import com.example.phonestore.utils.InventoryPolicy;
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
    protected boolean shouldShowBackButton() {
        return true;
    }

    @Override
    protected boolean shouldUseAdminBackButtonStyling() {
        return true;
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
        ArrayList<Product> allProducts = filterActiveProducts(productDao.layTatCaChoAdmin());
        String key = edtSearch.getText().toString().trim();
        ArrayList<Product> filteredProducts = key.isEmpty() ? allProducts : filterActiveProducts(productDao.timKiemChoAdmin(key));

        adapter.setData(filteredProducts);
        updateDashboard(allProducts, filteredProducts);
        updateEmptyState(filteredProducts);
    }

    private ArrayList<Product> filterActiveProducts(ArrayList<Product> products) {
        ArrayList<Product> activeProducts = new ArrayList<>();
        if (products == null) {
            return activeProducts;
        }
        for (Product product : products) {
            if (product != null && product.isActive) {
                activeProducts.add(product);
            }
        }
        return activeProducts;
    }

    private void bindImagePreview(ImageView ivImagePreview,
                                  TextView tvImagePreviewHint,
                                  EditText edtImage,
                                  EditText edtName,
                                  EditText edtBrand) {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                refreshImagePreview(ivImagePreview, tvImagePreviewHint, edtImage, edtName, edtBrand);
            }
        };

        edtImage.addTextChangedListener(watcher);
        edtName.addTextChangedListener(watcher);
        edtBrand.addTextChangedListener(watcher);
        refreshImagePreview(ivImagePreview, tvImagePreviewHint, edtImage, edtName, edtBrand);
    }

    private void refreshImagePreview(ImageView ivImagePreview,
                                     TextView tvImagePreviewHint,
                                     EditText edtImage,
                                     EditText edtName,
                                     EditText edtBrand) {
        String imageRef = edtImage.getText().toString().trim();
        String productName = edtName.getText().toString().trim();
        String brand = edtBrand.getText().toString().trim();

        ProductImageLoader.load(ivImagePreview, imageRef, productName, brand);

        if (imageRef.isEmpty()) {
            tvImagePreviewHint.setText("Chưa nhập ảnh, đang dùng ảnh mặc định/fallback");
            return;
        }
        if (!ProductImageLoader.isValidImageInput(imageRef)) {
            tvImagePreviewHint.setText("URL ảnh không hợp lệ, đang dùng ảnh fallback");
            return;
        }
        if (imageRef.regionMatches(true, 0, "http://", 0, 7)
                || imageRef.regionMatches(true, 0, "https://", 0, 8)) {
            tvImagePreviewHint.setText("Đang thử tải ảnh từ URL bạn nhập");
            return;
        }
        tvImagePreviewHint.setText("Đang preview theo tên resource nội bộ");
    }

    private void updateDashboard(ArrayList<Product> allProducts, ArrayList<Product> filteredProducts) {
        int total = allProducts == null ? 0 : allProducts.size();
        int inStock = 0;
        int lowStock = 0;

        if (allProducts != null) {
            for (Product p : allProducts) {
                if (p == null || !p.isActive) continue;
                if (!InventoryPolicy.isOutOfStock(p.tonKho)) inStock++;
                if (InventoryPolicy.isLowStock(p)) lowStock++;
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
        EditText edtImage = view.findViewById(R.id.edtImage);
        ImageView ivImagePreview = view.findViewById(R.id.ivImagePreview);
        TextView tvImagePreviewHint = view.findViewById(R.id.tvImagePreviewHint);
        EditText edtPrice = view.findViewById(R.id.edtPrice);
        EditText edtDiscount = view.findViewById(R.id.edtDiscount);
        EditText edtOs = view.findViewById(R.id.edtOs);
        EditText edtRom = view.findViewById(R.id.edtRom);
        EditText edtRam = view.findViewById(R.id.edtRam);
        EditText edtBattery = view.findViewById(R.id.edtBattery);
        EditText edtChipset = view.findViewById(R.id.edtChipset);
        EditText edtScreen = view.findViewById(R.id.edtScreen);
        EditText edtCamera = view.findViewById(R.id.edtCamera);
        EditText edtColors = view.findViewById(R.id.edtColors);
        EditText edtDesc = view.findViewById(R.id.edtDesc);
        boolean editing = oldProduct != null;
        if (editing) {
            edtName.setText(oldProduct.tenSanPham);
            edtBrand.setText(oldProduct.hang);
            edtImage.setText(oldProduct.tenAnh);
            edtPrice.setText(String.valueOf(oldProduct.gia));
            edtDiscount.setText(String.valueOf(oldProduct.giamGia));
            edtOs.setText(oldProduct.heDieuHanh);
            edtRom.setText(oldProduct.romGb > 0 ? String.valueOf(oldProduct.romGb) : "");
            edtRam.setText(oldProduct.ramGb > 0 ? String.valueOf(oldProduct.ramGb) : "");
            edtBattery.setText(oldProduct.pinMah > 0 ? String.valueOf(oldProduct.pinMah) : "");
            edtChipset.setText(oldProduct.chipset);
            edtScreen.setText(oldProduct.manHinh);
            edtCamera.setText(oldProduct.camera);
            edtColors.setText(oldProduct.mauSac);
            edtDesc.setText(oldProduct.moTa);
        }

        bindImagePreview(ivImagePreview, tvImagePreviewHint, edtImage, edtName, edtBrand);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? R.string.admin_product_dialog_edit : R.string.admin_product_dialog_add)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Product p = editing ? oldProduct : new Product();
            String error = fillAndValidateProductFromForm(
                    p, editing, edtName, edtBrand, edtImage, edtPrice, edtDiscount,
                    edtOs, edtRom, edtRam, edtBattery, edtChipset, edtScreen, edtCamera,
                    edtColors, edtDesc
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
                                                  boolean editing,
                                                  EditText edtName,
                                                  EditText edtBrand,
                                                  EditText edtImage,
                                                  EditText edtPrice,
                                                  EditText edtDiscount,
                                                  EditText edtOs,
                                                  EditText edtRom,
                                                  EditText edtRam,
                                                  EditText edtBattery,
                                                  EditText edtChipset,
                                                  EditText edtScreen,
                                                  EditText edtCamera,
                                                  EditText edtColors,
                                                  EditText edtDesc) {

        String name = edtName.getText().toString().trim();
        String brand = edtBrand.getText().toString().trim();
        String image = edtImage.getText().toString().trim();
        String priceStr = edtPrice.getText().toString().trim();
        String discountStr = edtDiscount.getText().toString().trim();
        String os = edtOs.getText().toString().trim();
        String romStr = edtRom.getText().toString().trim();
        String ramStr = edtRam.getText().toString().trim();
        String batteryStr = edtBattery.getText().toString().trim();
        String chipset = edtChipset.getText().toString().trim();
        String screen = edtScreen.getText().toString().trim();
        String camera = edtCamera.getText().toString().trim();
        String colors = edtColors.getText().toString().trim();

        if (TextUtils.isEmpty(name)) return getString(R.string.err_name_required);
        if (!ProductImageLoader.isValidImageInput(image)) return "URL ảnh không hợp lệ";
        if (TextUtils.isEmpty(priceStr)) return getString(R.string.err_price_required);
        if (TextUtils.isEmpty(os)) return "Vui lòng nhập hệ điều hành";
        if (TextUtils.isEmpty(romStr)) return "Vui lòng nhập dung lượng ROM";
        if (TextUtils.isEmpty(ramStr)) return "Vui lòng nhập dung lượng RAM";
        if (TextUtils.isEmpty(batteryStr)) return "Vui lòng nhập dung lượng pin";
        if (TextUtils.isEmpty(chipset)) return "Vui lòng nhập chipset";
        if (TextUtils.isEmpty(screen)) return "Vui lòng nhập màn hình";
        if (TextUtils.isEmpty(camera)) return "Vui lòng nhập camera";
        if (TextUtils.isEmpty(colors)) return "Vui lòng nhập màu sắc";

        int price;
        int discount;
        int rom;
        int ram;
        int battery;

        try {
            price = Integer.parseInt(priceStr);
            discount = TextUtils.isEmpty(discountStr) ? 0 : Integer.parseInt(discountStr);
            rom = Integer.parseInt(romStr);
            ram = Integer.parseInt(ramStr);
            battery = Integer.parseInt(batteryStr);
        } catch (NumberFormatException e) {
            return getString(R.string.err_number_format);
        }

        if (price <= 0) return getString(R.string.err_price_invalid);
        if (discount < 0 || discount >= 100) return getString(R.string.err_discount_invalid);
        if (rom <= 0) return "ROM phải lớn hơn 0";
        if (ram <= 0) return "RAM phải lớn hơn 0";
        if (battery <= 0) return "Pin phải lớn hơn 0";

        p.tenSanPham = name;
        p.hang = brand;
        p.tenAnh = image;
        p.gia = price;
        p.tonKho = editing ? Math.max(0, p.tonKho) : 0;
        p.giamGia = discount;
        p.heDieuHanh = os;
        p.romGb = rom;
        p.ramGb = ram;
        p.pinMah = battery;
        p.chipset = chipset;
        p.manHinh = screen;
        p.camera = camera;
        p.mauSac = colors;
        p.moTa = edtDesc.getText().toString().trim();
        if (!editing) {
            p.isActive = true;
        }

        return null;
    }

    private void confirmDelete(Product product) {
        ProductDao.ProductDeactivationCheck check = productDao.checkDeactivationEligibility(product.maSanPham);
        if (!check.canDeactivate()) {
            Toast.makeText(this, buildDeactivateBlockedMessage(check), Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_deactivate_product_title)
                .setMessage(getString(R.string.admin_deactivate_product_message, product.tenSanPham))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.admin_deactivate_product_action, (d, w) -> {
                    boolean ok = productDao.delete(product.maSanPham);
                    if (ok) {
                        Toast.makeText(this, R.string.product_deleted, Toast.LENGTH_SHORT).show();
                        loadData();
                    } else {
                        Toast.makeText(this, buildDeactivateBlockedMessage(productDao.checkDeactivationEligibility(product.maSanPham)), Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private String buildDeactivateBlockedMessage(ProductDao.ProductDeactivationCheck check) {
        if (check == null || check.product == null) {
            return getString(R.string.action_failed);
        }
        if (check.alreadyInactive) {
            return getString(R.string.admin_product_deactivate_blocked_inactive);
        }
        if (check.currentStock > 0) {
            return getString(R.string.admin_product_deactivate_blocked_stock, check.currentStock);
        }
        if (check.activeCartCount > 0) {
            return getString(R.string.admin_product_deactivate_blocked_cart, check.activeCartCount);
        }
        if (check.openOrderCount > 0) {
            return getString(R.string.admin_product_deactivate_blocked_order, check.openOrderCount);
        }
        return getString(R.string.action_failed);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (productDao != null && adapter != null) {
            loadData();
        }
    }
}
