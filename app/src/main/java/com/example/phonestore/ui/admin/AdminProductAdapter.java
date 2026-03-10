package com.example.phonestore.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.Product;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.VH> {

    public interface Listener {
        void onEdit(Product product);
        void onDelete(Product product);
    }

    private final ArrayList<Product> data = new ArrayList<>();
    private final Listener listener;

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

        h.tvName.setText(p.tenSanPham == null || p.tenSanPham.trim().isEmpty()
                ? h.itemView.getContext().getString(R.string.admin_product_unknown_name)
                : p.tenSanPham);

        String brand = (p.hang == null || p.hang.trim().isEmpty())
                ? h.itemView.getContext().getString(R.string.admin_product_unknown_brand)
                : p.hang.trim();
        h.tvBrand.setText(h.itemView.getContext().getString(R.string.admin_product_brand_label, brand));

        h.tvId.setText(h.itemView.getContext().getString(R.string.admin_product_id, p.maSanPham));

        String desc = (p.moTa == null || p.moTa.trim().isEmpty())
                ? h.itemView.getContext().getString(R.string.admin_product_empty_desc)
                : p.moTa.trim();
        h.tvDesc.setText(desc);

        String gia = h.itemView.getContext().getString(
                R.string.admin_price_currency,
                NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(p.gia)
        );
        h.tvPrice.setText(gia);

        h.tvStock.setText(h.itemView.getContext().getString(R.string.admin_stock_label, p.tonKho));
        h.tvDiscount.setText(h.itemView.getContext().getString(R.string.admin_discount_label, p.giamGia));

        if (p.tonKho <= 0) {
            h.tvStock.setText(R.string.admin_stock_empty);
            h.tvStock.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.red_dark));
        } else if (p.tonKho <= 5) {
            h.tvStock.setText(h.itemView.getContext().getString(R.string.admin_stock_low, p.tonKho));
            h.tvStock.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.red_primary));
        } else {
            h.tvStock.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.text_primary));
        }

        if (p.giamGia > 0) {
            h.tvDiscount.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.red_primary));
        } else {
            h.tvDiscount.setText(R.string.admin_discount_none);
            h.tvDiscount.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.text_sub));
        }

        h.btnEdit.setOnClickListener(v -> listener.onEdit(p));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(p));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvBrand, tvId, tvDesc, tvPrice, tvStock, tvDiscount;
        MaterialButton btnEdit, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvBrand = itemView.findViewById(R.id.tvBrand);
            tvId = itemView.findViewById(R.id.tvId);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStock = itemView.findViewById(R.id.tvStock);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}