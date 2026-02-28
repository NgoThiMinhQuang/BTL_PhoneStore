package com.example.phonestore.ui.products;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.CartDao;
import com.example.phonestore.data.dao.ProductDao;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.utils.SessionManager;

import java.text.NumberFormat;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    // Adapter sẽ truyền ID sản phẩm qua extra này
    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private ImageView ivImage;
    private TextView tvName, tvBrand, tvPrice, tvStock, tvDesc, tvQty;

    private int soLuong = 1;
    private Product sanPham;

    private ProductDao productDao;
    private CartDao cartDao;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Product Detailed");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Bind view
        ivImage = findViewById(R.id.ivImage);
        tvName = findViewById(R.id.tvName);
        tvBrand = findViewById(R.id.tvBrand);
        tvPrice = findViewById(R.id.tvPrice);
        tvStock = findViewById(R.id.tvStock);
        tvDesc = findViewById(R.id.tvDesc);
        tvQty = findViewById(R.id.tvQty);

        productDao = new ProductDao(this);
        cartDao = new CartDao(this);
        session = new SessionManager(this);

        long productId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, -1);
        if (productId == -1) {
            Toast.makeText(this, "Không tìm thấy sản phẩm!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sanPham = productDao.getById(productId);
        if (sanPham == null) {
            Toast.makeText(this, "Sản phẩm không tồn tại!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        hienThiSanPham(sanPham);

        // Nút trừ
        findViewById(R.id.btnMinus).setOnClickListener(v -> {
            if (soLuong > 1) soLuong--;
            tvQty.setText(String.valueOf(soLuong));
        });

        // Nút cộng (không vượt quá tồn kho)
        findViewById(R.id.btnPlus).setOnClickListener(v -> {
            int max = Math.max(sanPham.tonKho, 1);
            if (soLuong < max) soLuong++;
            tvQty.setText(String.valueOf(soLuong));
        });

        // Add to cart
        findViewById(R.id.btnAddToCart).setOnClickListener(v -> {
            if (sanPham.tonKho <= 0) {
                Toast.makeText(this, "Sản phẩm đã hết hàng!", Toast.LENGTH_SHORT).show();
                return;
            }

            long userId = session.getUserId(); // repo có sẵn getUserId() :contentReference[oaicite:7]{index=7}
            if (userId == -1) {
                Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean ok = cartDao.addOrIncrease(userId, sanPham.maSanPham, soLuong);
            Toast.makeText(this, ok ? "Đã thêm vào giỏ hàng!" : "Thêm thất bại!", Toast.LENGTH_SHORT).show();
        });
    }

    private void hienThiSanPham(Product p) {
        // Title theo tên máy
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(p.tenSanPham);

        tvName.setText(p.tenSanPham == null ? "" : p.tenSanPham);
        tvBrand.setText("Hãng: " + (p.hang == null ? "" : p.hang));
        tvStock.setText("Còn: " + p.tonKho);
        tvDesc.setText(p.moTa == null ? "" : p.moTa);

        // Giá có giảm giá thì tính giá cuối
        int giaCuoi = p.gia;
        if (p.giamGia > 0) {
            giaCuoi = p.gia - (p.gia * p.giamGia / 100);
        }
        tvPrice.setText(formatVnd(giaCuoi));

        // Ảnh theo tên mipmap giống adapter (repo đang làm vậy) :contentReference[oaicite:8]{index=8}
        int resId = 0;
        if (p.tenAnh != null && !p.tenAnh.trim().isEmpty()) {
            resId = getResources().getIdentifier(p.tenAnh.trim(), "mipmap", getPackageName());
        }
        ivImage.setImageResource(resId != 0 ? resId : android.R.drawable.ic_menu_gallery);

        // Reset số lượng
        soLuong = 1;
        tvQty.setText(String.valueOf(soLuong));
    }

    private String formatVnd(int value) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(value) + "đ";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}