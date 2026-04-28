package com.example.phonestore.ui.admin;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.utils.InventoryPolicy;
import com.example.phonestore.utils.ProductImageLoader;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.VH> {

    public interface Listener {
        void onEdit(Product product);
        void onToggleSale(Product product);
        void onDelete(Product product);
    }

    private final ArrayList<Product> data = new ArrayList<>();
    private final Listener listener;
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    public AdminProductAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(ArrayList<Product> list) {
        data.clear();
        if (list != null) {
            data.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = data.get(position);
        Context context = h.itemView.getContext();

        String name = p.tenSanPham == null || p.tenSanPham.trim().isEmpty()
                ? context.getString(R.string.admin_product_unknown_name)
                : p.tenSanPham.trim();
        String brand = p.hang == null || p.hang.trim().isEmpty()
                ? context.getString(R.string.admin_product_unknown_brand)
                : p.hang.trim();
        String desc = p.moTa == null || p.moTa.trim().isEmpty()
                ? context.getString(R.string.admin_product_empty_desc)
                : p.moTa.trim();

        h.tvName.setText(name);
        h.tvBrand.setText(brand);
        h.tvId.setText(context.getString(R.string.admin_product_id, p.maSanPham));
        h.tvDesc.setText(desc);
        h.tvPrice.setText(context.getString(R.string.admin_price_currency, currencyFormat.format(p.gia)));
        h.tvStock.setText(context.getString(R.string.admin_product_stock_short, Math.max(0, p.tonKho)));
        ProductImageLoader.load(h.ivThumb, p.tenAnh, p.tenSanPham, p.hang);

        if (p.giamGia > 0) {
            h.tvDiscount.setVisibility(View.VISIBLE);
            h.tvDiscount.setText(context.getString(R.string.admin_product_discount_short, p.giamGia));
        } else {
            h.tvDiscount.setVisibility(View.GONE);
        }

        bindStatus(context, h, p);
        bindSaleAction(context, h, p);

        h.btnEdit.setOnClickListener(v -> listener.onEdit(p));
        h.btnToggleSale.setOnClickListener(v -> listener.onToggleSale(p));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(p));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private void bindStatus(Context context, VH h, Product product) {
        if (!product.isActive) {
            h.tvStatus.setText(R.string.admin_product_status_stopped);
            h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.admin_text_secondary));
            h.tvStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.admin_product_status_stopped_bg)));
            h.tvStock.setTextColor(ContextCompat.getColor(context, R.color.admin_text_secondary));
            return;
        }

        InventoryPolicy.StatusAppearance appearance = InventoryPolicy.getAppearance(context, product.tonKho);
        h.tvStatus.setText(appearance.label);
        h.tvStatus.setTextColor(appearance.labelColor);
        h.tvStatus.setBackgroundTintList(appearance.labelBackgroundTint);
        h.tvStock.setTextColor(appearance.stockColor);
    }

    private void bindSaleAction(Context context, VH h, Product product) {
        if (product.isActive) {
            h.btnToggleSale.setContentDescription(context.getString(R.string.admin_stop_selling_product_action));
            h.btnToggleSale.setIconResource(R.drawable.ic_stop_selling);
            h.btnToggleSale.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.admin_warning)));
            return;
        }

        h.btnToggleSale.setContentDescription(context.getString(R.string.admin_resume_selling_product_action));
        h.btnToggleSale.setIconResource(R.drawable.ic_resume_selling);
        h.btnToggleSale.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.admin_success)));
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvName, tvBrand, tvId, tvDesc, tvPrice, tvStock, tvDiscount, tvStatus;
        MaterialButton btnEdit, btnToggleSale, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivThumb);
            tvName = itemView.findViewById(R.id.tvName);
            tvBrand = itemView.findViewById(R.id.tvBrand);
            tvId = itemView.findViewById(R.id.tvId);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStock = itemView.findViewById(R.id.tvStock);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnToggleSale = itemView.findViewById(R.id.btnToggleSale);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
