package com.example.phonestore.ui.products;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.Product;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = data.get(position);

        String ten = p.tenSanPham == null ? "" : p.tenSanPham;
        String hang = p.hang == null ? "" : p.hang;

        h.tvName.setText(hang.isEmpty() ? ten : (ten + " (" + hang + ")"));
        h.tvDesc.setText(p.moTa == null ? "" : p.moTa);

        String giaText = NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(p.gia) + "đ";
        h.tvPrice.setText(giaText);

        h.tvStock.setText("Còn: " + p.tonKho);

        if (p.giamGia > 0) {
            h.tvDiscount.setVisibility(View.VISIBLE);
            h.tvDiscount.setText("Giảm: " + p.giamGia + "%");
        } else {
            h.tvDiscount.setVisibility(View.GONE);
        }

        // ảnh theo tên mipmap (nếu có)
        int resId = 0;
        if (p.tenAnh != null && !p.tenAnh.trim().isEmpty()) {
            resId = ctx.getResources().getIdentifier(p.tenAnh.trim(), "mipmap", ctx.getPackageName());
        }
        h.ivThumb.setImageResource(resId != 0 ? resId : android.R.drawable.ic_menu_gallery);

        h.itemView.setOnClickListener(v ->
                Toast.makeText(ctx, "Bạn chọn: " + ten, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvName, tvDesc, tvDiscount, tvPrice, tvStock;

        VH(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivThumb);
            tvName = itemView.findViewById(R.id.tvName);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStock = itemView.findViewById(R.id.tvStock);
        }
    }
}