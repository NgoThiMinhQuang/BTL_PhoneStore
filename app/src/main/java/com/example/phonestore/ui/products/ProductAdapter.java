package com.example.phonestore.ui.products;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    private final ArrayList<Product> data = new ArrayList<>();
    private Context ctx;

    private int selectedPosition = RecyclerView.NO_POSITION;
    private int pressedPosition = RecyclerView.NO_POSITION;
    private int hoveredPosition = RecyclerView.NO_POSITION;

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

        if (hang.isEmpty()) {
            h.tvBrand.setVisibility(View.GONE);
        } else {
            h.tvBrand.setVisibility(View.VISIBLE);
            h.tvBrand.setText(h.itemView.getContext().getString(R.string.product_meta_brand, hang));
        }

        h.tvSpecs.setText(buildSpecsText(p));

        String giaText = NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(p.gia) + "đ";
        h.tvPrice.setText(giaText);

        if (p.giamGia > 0) {
            h.tvDiscount.setVisibility(View.VISIBLE);
            h.tvDiscount.setText("-" + p.giamGia + "%");
        } else {
            h.tvDiscount.setVisibility(View.GONE);
        }

        bindStock(h, p.tonKho);

        if (p.heDieuHanh == null || p.heDieuHanh.trim().isEmpty()) {
            h.tvOs.setVisibility(View.GONE);
        } else {
            h.tvOs.setVisibility(View.VISIBLE);
            h.tvOs.setText(h.itemView.getContext().getString(R.string.product_meta_os, p.heDieuHanh.trim()));
        }

        int imageRes = resolveProductImage(p);
        h.ivThumb.setImageResource(imageRes != 0 ? imageRes : android.R.drawable.ic_menu_gallery);

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

    private int resolveProductImage(Product p) {
        int imageRes = findImageRes(p.tenAnh);
        if (imageRes != 0) return imageRes;

        String name = p.tenSanPham == null ? "" : p.tenSanPham.toLowerCase(Locale.ROOT);
        String brand = p.hang == null ? "" : p.hang.toLowerCase(Locale.ROOT);

        if (name.contains("iphone") || brand.contains("apple")) return R.drawable.ip_15;
        if (name.contains("s24") || brand.contains("samsung")) return findExistingSamsungImage();
        if (name.contains("iphone 14")) return findImageRes("ic_iphone15");

        return 0;
    }

    private int findImageRes(String imageName) {
        if (imageName == null || imageName.trim().isEmpty()) return 0;

        String imageKey = imageName.trim();
        int resId = ctx.getResources().getIdentifier(imageKey, "drawable", ctx.getPackageName());
        if (resId != 0) return resId;

        return ctx.getResources().getIdentifier(imageKey, "mipmap", ctx.getPackageName());
    }

    private int findExistingSamsungImage() {
        int imageRes = findImageRes("ss_s24_ultra");
        if (imageRes != 0) return imageRes;
        return findImageRes("ss_s24_utra");
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        ImageView ivThumb;
        View viewInteractionOverlay;
        TextView tvName, tvBrand, tvSpecs, tvDiscount, tvPrice, tvStock, tvOs;

        VH(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            ivThumb = itemView.findViewById(R.id.ivThumb);
            viewInteractionOverlay = itemView.findViewById(R.id.viewInteractionOverlay);
            tvName = itemView.findViewById(R.id.tvName);
            tvBrand = itemView.findViewById(R.id.tvBrand);
            tvSpecs = itemView.findViewById(R.id.tvSpecs);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStock = itemView.findViewById(R.id.tvStock);
            tvOs = itemView.findViewById(R.id.tvOs);
        }
    }
}
