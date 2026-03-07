package com.example.phonestore.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        data.addAll(list);
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

        h.tvName.setText(p.tenSanPham == null ? "" : p.tenSanPham);
        h.tvBrand.setText(p.hang == null ? "-" : p.hang);

        String gia = NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(p.gia) + "đ";
        h.tvPrice.setText(gia);

        h.tvStock.setText("Tồn kho: " + p.tonKho);
        h.tvDiscount.setText("Giảm giá: " + p.giamGia + "%");

        h.btnEdit.setOnClickListener(v -> listener.onEdit(p));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(p));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvBrand, tvPrice, tvStock, tvDiscount;
        MaterialButton btnEdit, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvBrand = itemView.findViewById(R.id.tvBrand);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStock = itemView.findViewById(R.id.tvStock);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
