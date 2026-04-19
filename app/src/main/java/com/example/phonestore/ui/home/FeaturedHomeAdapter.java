package com.example.phonestore.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.ui.products.ProductDetailActivity;
import com.example.phonestore.utils.ProductImageLoader;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class FeaturedHomeAdapter extends RecyclerView.Adapter<FeaturedHomeAdapter.VH> {

    private final ArrayList<Product> data = new ArrayList<>();
    private Context ctx;

    public void setData(ArrayList<Product> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ctx = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured_home, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = data.get(position);
        int discountedPrice = (int) ((long) p.gia * (100 - Math.max(0, p.giamGia)) / 100);

        h.tvName.setText(p.tenSanPham == null ? "Sản phẩm" : p.tenSanPham);
        h.tvVariant.setText(buildVariantText(p));
        h.tvRating.setText("★ " + formatRating(p.danhGia));
        h.tvSold.setText("(" + formatSold(p.daBan) + " sold)");
        h.tvPrice.setText(formatMoney(discountedPrice));
        h.tvOriginalPrice.setText(formatMoney(p.gia));
        h.tvOriginalPrice.setPaintFlags(h.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        if (p.giamGia > 0) {
            h.tvDiscount.setVisibility(View.VISIBLE);
            h.tvDiscount.setText("-" + p.giamGia + "%");
        } else {
            h.tvDiscount.setVisibility(View.GONE);
        }

        ProductImageLoader.load(h.ivThumb, p.tenAnh, p.tenSanPham, p.hang);

        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(ctx, ProductDetailActivity.class);
            i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, p.maSanPham);
            ctx.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private String buildVariantText(Product p) {
        ArrayList<String> parts = new ArrayList<>();
        if (p.mauSac != null && !p.mauSac.trim().isEmpty()) {
            String[] colors = p.mauSac.split(",");
            if (colors.length > 0 && !colors[0].trim().isEmpty()) {
                parts.add(colors[0].trim());
            }
        }
        if (p.romGb > 0) {
            parts.add(p.romGb + "GB");
        }
        if (parts.isEmpty()) {
            if (p.chipset != null && !p.chipset.trim().isEmpty()) {
                parts.add(p.chipset.trim());
            } else if (p.hang != null && !p.hang.trim().isEmpty()) {
                parts.add(p.hang.trim());
            }
        }
        return android.text.TextUtils.join(" • ", parts);
    }

    private String formatMoney(int value) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(value) + "đ";
    }

    private String formatRating(float rating) {
        return String.format(Locale.US, "%.1f", rating <= 0f ? 4.5f : rating);
    }

    private String formatSold(int sold) {
        if (sold >= 1000) {
            return String.format(Locale.US, "%.1fK", sold / 1000f);
        }
        return String.valueOf(Math.max(sold, 0));
    }


    static class VH extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvDiscount;
        TextView tvName;
        TextView tvVariant;
        TextView tvRating;
        TextView tvSold;
        TextView tvPrice;
        TextView tvOriginalPrice;

        VH(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivFeaturedThumb);
            tvDiscount = itemView.findViewById(R.id.tvFeaturedDiscount);
            tvName = itemView.findViewById(R.id.tvFeaturedName);
            tvVariant = itemView.findViewById(R.id.tvFeaturedVariant);
            tvRating = itemView.findViewById(R.id.tvFeaturedRating);
            tvSold = itemView.findViewById(R.id.tvFeaturedSold);
            tvPrice = itemView.findViewById(R.id.tvFeaturedPrice);
            tvOriginalPrice = itemView.findViewById(R.id.tvFeaturedOriginalPrice);
        }
    }
}
