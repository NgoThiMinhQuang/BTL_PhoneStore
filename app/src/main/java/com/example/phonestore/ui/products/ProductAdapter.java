package com.example.phonestore.ui.products;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    private final ArrayList<Product> data = new ArrayList<>();
    private final boolean flashSaleMode;
    private final boolean compactMode;
    private final boolean catalogCompactMode;
    private Context ctx;

    private int selectedPosition = RecyclerView.NO_POSITION;
    private int pressedPosition = RecyclerView.NO_POSITION;
    private int hoveredPosition = RecyclerView.NO_POSITION;

    public ProductAdapter() {
        this(false, false, false);
    }

    private ProductAdapter(boolean flashSaleMode, boolean compactMode, boolean catalogCompactMode) {
        this.flashSaleMode = flashSaleMode;
        this.compactMode = compactMode;
        this.catalogCompactMode = catalogCompactMode;
    }

    public static ProductAdapter forFlashSale() {
        return new ProductAdapter(true, true, false);
    }

    public static ProductAdapter forCompactCarousel() {
        return new ProductAdapter(false, true, true);
    }

    public static ProductAdapter forCatalogCompact() {
        return new ProductAdapter(false, false, true);
    }

    public void setData(ArrayList<Product> newData) {
        data.clear();
        data.addAll(newData);
        selectedPosition = RecyclerView.NO_POSITION;
        pressedPosition = RecyclerView.NO_POSITION;
        hoveredPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ctx = parent.getContext();
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = data.get(position);

        String ten = p.tenSanPham == null ? "" : p.tenSanPham.trim();
        String hang = p.hang == null ? "" : p.hang.trim();

        h.tvName.setText(ten.isEmpty() ? "Sản phẩm" : ten);

        if (flashSaleMode) {
            bindFlashSaleCard(h, p);
        } else if (catalogCompactMode) {
            bindCatalogCompactCard(h, p);
        } else {
            bindDefaultCard(h, p, hang);
        }

        ProductImageLoader.load(h.ivThumb, p.tenAnh, p.tenSanPham, p.hang);
        applyItemSizing(h);

        boolean isActive = position == selectedPosition || position == pressedPosition || position == hoveredPosition;
        h.viewInteractionOverlay.setVisibility(isActive ? View.VISIBLE : View.GONE);
        h.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx,
                isActive ? R.color.product_card_active_bg : R.color.product_card_bg));

        h.itemView.setOnHoverListener((v, event) -> {
            int adapterPos = h.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return false;

            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER || event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
                updateHoveredPosition(adapterPos);
            } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                if (hoveredPosition == adapterPos) updateHoveredPosition(RecyclerView.NO_POSITION);
            }
            return false;
        });

        h.itemView.setOnTouchListener((v, event) -> {
            int adapterPos = h.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return false;

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                updatePressedPosition(adapterPos);
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                if (pressedPosition == adapterPos) updatePressedPosition(RecyclerView.NO_POSITION);
            }
            return false;
        });

        h.itemView.setOnClickListener(v -> {
            int adapterPos = h.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            setSelectedPosition(adapterPos);

            Intent i = new Intent(ctx, ProductDetailActivity.class);
            i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, p.maSanPham);
            ctx.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private void bindDefaultCard(VH holder, Product p, String hang) {
        if (hang.isEmpty()) {
            holder.tvBrand.setVisibility(View.GONE);
        } else {
            holder.tvBrand.setVisibility(View.VISIBLE);
            holder.tvBrand.setText(holder.itemView.getContext().getString(R.string.product_meta_brand, hang));
        }

        holder.tvSpecs.setText(buildSpecsText(p));
        holder.tvPrice.setText(formatMoney(p.gia));
        holder.tvOriginalPrice.setPaintFlags(holder.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        holder.tvOriginalPrice.setVisibility(View.GONE);
        holder.tvMetaPrimary.setVisibility(View.GONE);
        holder.tvMetaSecondary.setVisibility(View.GONE);
        holder.layoutFlashMeta.setVisibility(View.GONE);
        holder.layoutLegacyMeta.setVisibility(View.GONE);

        if (p.giamGia > 0) {
            holder.tvDiscount.setVisibility(View.VISIBLE);
            holder.tvDiscount.setText("-" + p.giamGia + "%");
        } else {
            holder.tvDiscount.setVisibility(View.GONE);
        }

        bindStock(holder, p.tonKho);

        if (p.heDieuHanh == null || p.heDieuHanh.trim().isEmpty()) {
            holder.tvOs.setVisibility(View.GONE);
        } else {
            holder.tvOs.setVisibility(View.VISIBLE);
            holder.tvOs.setText(holder.itemView.getContext().getString(R.string.product_meta_os, p.heDieuHanh.trim()));
        }
    }

    private void bindFlashSaleCard(VH holder, Product p) {
        bindCompactCard(holder, p, true, true, true);
    }

    private void bindCatalogCompactCard(VH holder, Product p) {
        bindCompactCard(holder, p, false, true, true);
    }

    private void bindCompactCard(VH holder, Product p, boolean saleBadgeMode, boolean showMeta, boolean customerStyle) {
        holder.tvBrand.setVisibility(View.GONE);
        holder.layoutLegacyMeta.setVisibility(View.GONE);
        holder.tvOs.setVisibility(View.GONE);

        holder.tvSpecs.setText(buildFlashSaleSpecsText(p));
        holder.tvStock.setText(saleBadgeMode ? buildFlashSaleBadgeText(p) : buildCatalogCompactBadgeText(p));
        holder.tvStock.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.panel_soft)));
        holder.tvStock.setTextColor(ContextCompat.getColor(ctx, R.color.text_sub));
        holder.tvStock.setVisibility(customerStyle ? View.GONE : View.VISIBLE);

        holder.layoutFlashMeta.setVisibility(showMeta ? View.VISIBLE : View.GONE);
        if (showMeta) {
            holder.tvMetaPrimary.setVisibility(View.VISIBLE);
            holder.tvMetaPrimary.setText(buildRatingText(p));

            String soldText = buildSoldText(p);
            holder.tvMetaSecondary.setVisibility(View.VISIBLE);
            holder.tvMetaSecondary.setText(soldText.isEmpty() ? "(0 sold)" : soldText);
        } else {
            holder.tvMetaPrimary.setVisibility(View.GONE);
            holder.tvMetaSecondary.setVisibility(View.GONE);
            holder.tvMetaSecondary.setText("");
        }

        int salePrice = calculateSalePrice(p.gia, p.giamGia);
        holder.tvPrice.setText(formatMoney(salePrice));
        holder.tvOriginalPrice.setPaintFlags(holder.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        if (p.giamGia > 0) {
            holder.tvDiscount.setVisibility(View.VISIBLE);
            holder.tvDiscount.setText("-" + p.giamGia + "%");
            holder.tvOriginalPrice.setVisibility(View.VISIBLE);
            holder.tvOriginalPrice.setText(formatMoney(p.gia));
        } else {
            holder.tvDiscount.setVisibility(View.GONE);
            holder.tvOriginalPrice.setVisibility(customerStyle ? View.VISIBLE : View.GONE);
            holder.tvOriginalPrice.setText(customerStyle ? formatMoney(p.gia) : "");
        }
    }

    private void bindStock(VH holder, int stock) {
        String status = InventoryPolicy.resolveStatus(stock);
        holder.tvStock.setText(InventoryPolicy.getCustomerStockText(holder.itemView.getContext(), stock));
        holder.tvStock.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.panel_soft)));
        if (InventoryPolicy.STATUS_IN_STOCK.equals(status)) {
            holder.tvStock.setTextColor(ContextCompat.getColor(ctx, R.color.text_sub));
        } else {
            holder.tvStock.setTextColor(ContextCompat.getColor(ctx, R.color.red_primary));
        }
    }

    private String buildSpecsText(Product p) {
        ArrayList<String> parts = new ArrayList<>();
        if (p.romGb > 0) parts.add(p.romGb + "GB");
        if (p.ramGb > 0) parts.add("RAM " + p.ramGb + "GB");
        if (p.chipset != null && !p.chipset.trim().isEmpty()) parts.add(p.chipset.trim());
        if (parts.isEmpty()) {
            String moTa = p.moTa == null ? "" : p.moTa.trim();
            return moTa.isEmpty() ? "Chưa có mô tả" : moTa;
        }
        return android.text.TextUtils.join(" • ", parts);
    }

    private String buildFlashSaleSpecsText(Product p) {
        ArrayList<String> parts = new ArrayList<>();
        if (p.mauSac != null && !p.mauSac.trim().isEmpty()) parts.add(p.mauSac.trim());
        if (p.romGb > 0) parts.add(p.romGb + "GB");
        if (parts.isEmpty()) return buildSpecsText(p);
        return android.text.TextUtils.join(" • ", parts);
    }

    private String buildFlashSaleBadgeText(Product p) {
        String brand = p.hang == null ? "" : p.hang.trim();
        return brand.isEmpty() ? "Flash deal" : "Hãng " + brand;
    }

    private String buildCatalogCompactBadgeText(Product p) {
        int stock = p.tonKho;
        if (stock <= 0) return "Hết hàng";
        if (stock <= 10) return "Sắp hết: " + stock;
        return "Còn: " + stock;
    }

    private String buildRatingText(Product p) {
        float rating = p.danhGia > 0 ? p.danhGia : 4.8f;
        return "★ " + String.format(Locale.getDefault(), "%.1f", rating);
    }

    private String buildSoldText(Product p) {
        int sold = Math.max(p.daBan, 0);
        return "(" + sold + " sold)";
    }

    private void applyItemSizing(VH holder) {
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (!(params instanceof RecyclerView.LayoutParams)) return;

        RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) params;
        if (flashSaleMode) {
            layoutParams.width = dpToPx(248);
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else if (compactMode) {
            layoutParams.width = dpToPx(228);
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        holder.itemView.setLayoutParams(layoutParams);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
    }

    private int calculateSalePrice(int originalPrice, int discountPercent) {
        if (discountPercent <= 0) return originalPrice;
        return Math.max(0, originalPrice - (originalPrice * discountPercent / 100));
    }

    private String formatMoney(int amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + "đ";
    }

    private void setSelectedPosition(int newPos) {
        int oldPos = selectedPosition;
        selectedPosition = newPos;
        if (oldPos != RecyclerView.NO_POSITION) notifyItemChanged(oldPos);
        if (newPos != RecyclerView.NO_POSITION) notifyItemChanged(newPos);
    }

    private void updatePressedPosition(int newPos) {
        if (pressedPosition == newPos) return;
        int oldPos = pressedPosition;
        pressedPosition = newPos;
        if (oldPos != RecyclerView.NO_POSITION) notifyItemChanged(oldPos);
        if (newPos != RecyclerView.NO_POSITION) notifyItemChanged(newPos);
    }

    private void updateHoveredPosition(int newPos) {
        if (hoveredPosition == newPos) return;
        int oldPos = hoveredPosition;
        hoveredPosition = newPos;
        if (oldPos != RecyclerView.NO_POSITION) notifyItemChanged(oldPos);
        if (newPos != RecyclerView.NO_POSITION) notifyItemChanged(newPos);
    }


    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        ImageView ivThumb;
        View viewInteractionOverlay;
        View layoutFlashMeta, layoutLegacyMeta;
        TextView tvName, tvBrand, tvSpecs, tvDiscount, tvPrice, tvOriginalPrice, tvStock, tvOs, tvMetaPrimary, tvMetaSecondary;

        VH(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            ivThumb = itemView.findViewById(R.id.ivThumb);
            viewInteractionOverlay = itemView.findViewById(R.id.viewInteractionOverlay);
            layoutFlashMeta = itemView.findViewById(R.id.layoutFlashMeta);
            layoutLegacyMeta = itemView.findViewById(R.id.layoutLegacyMeta);
            tvName = itemView.findViewById(R.id.tvName);
            tvBrand = itemView.findViewById(R.id.tvBrand);
            tvSpecs = itemView.findViewById(R.id.tvSpecs);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            tvStock = itemView.findViewById(R.id.tvStock);
            tvOs = itemView.findViewById(R.id.tvOs);
            tvMetaPrimary = itemView.findViewById(R.id.tvMetaPrimary);
            tvMetaSecondary = itemView.findViewById(R.id.tvMetaSecondary);
        }
    }
}
