package com.example.phonestore.ui.cart;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.CartItem;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

    public interface Listener {
        void onChangeQty(CartItem item, int newQty);
        void onRemove(CartItem item);
        void onToggleSelection(CartItem item, boolean isSelected);
    }

    private final ArrayList<CartItem> data = new ArrayList<>();
    private final Set<Long> selectedProductIds = new HashSet<>();
    private final Listener listener;
    private Context ctx;

    public CartAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(ArrayList<CartItem> list) {
        data.clear();
        data.addAll(list);
        notifyDataSetChanged();
    }

    public void setSelectedProductIds(Set<Long> ids) {
        selectedProductIds.clear();
        if (ids != null) selectedProductIds.addAll(ids);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ctx = parent.getContext();
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_cart, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CartItem it = data.get(position);

        h.tvName.setText(it.tenSanPham + (it.hang == null ? "" : " (" + it.hang + ")"));
        String giaText = NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(it.giaSauGiam()) + "đ";
        h.tvPrice.setText("Giá: " + giaText + (it.giamGia > 0 ? " (" + it.giamGia + "%)" : ""));
        h.tvQty.setText(String.valueOf(it.soLuong));

        int resId = resolveImageRes(it.tenAnh);
        h.ivThumb.setImageResource(resId != 0 ? resId : android.R.drawable.ic_menu_gallery);

        boolean isSelected = selectedProductIds.contains(it.productId);
        h.cbSelect.setOnCheckedChangeListener(null);
        h.cbSelect.setChecked(isSelected);
        bindSelectionStyle(h, isSelected);

        h.cbSelect.setOnCheckedChangeListener((buttonView, checked) -> {
            bindSelectionStyle(h, checked);
            listener.onToggleSelection(it, checked);
        });

        h.itemView.setOnClickListener(v -> {
            boolean checked = !h.cbSelect.isChecked();
            h.cbSelect.setChecked(checked);
        });

        h.btnMinus.setOnClickListener(v -> listener.onChangeQty(it, it.soLuong - 1));
        h.btnPlus.setOnClickListener(v -> listener.onChangeQty(it, it.soLuong + 1));
        h.btnDelete.setOnClickListener(v -> listener.onRemove(it));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private void bindSelectionStyle(VH h, boolean isSelected) {
        h.cardRoot.setCardBackgroundColor(ContextCompat.getColor(ctx,
                isSelected ? R.color.product_card_active_bg : R.color.panel_light));
        h.cardRoot.setStrokeColor(ContextCompat.getColor(ctx,
                isSelected ? R.color.red_primary : R.color.panel_stroke));
        h.cardRoot.setStrokeWidth((int) (ctx.getResources().getDisplayMetrics().density * (isSelected ? 2 : 1)));
    }

    private int resolveImageRes(String imageName) {
        if (imageName == null || imageName.trim().isEmpty()) return 0;

        String imageKey = imageName.trim();
        int resId = ctx.getResources().getIdentifier(imageKey, "drawable", ctx.getPackageName());
        if (resId != 0) return resId;

        return ctx.getResources().getIdentifier(imageKey, "mipmap", ctx.getPackageName());
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        CheckBox cbSelect;
        ImageView ivThumb, btnDelete;
        TextView tvName, tvPrice, tvQty;
        View btnMinus, btnPlus;

        VH(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            ivThumb = itemView.findViewById(R.id.ivThumb);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQty = itemView.findViewById(R.id.tvQty);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
