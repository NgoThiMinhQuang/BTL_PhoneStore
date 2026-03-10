package com.example.phonestore.ui.orders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.OrderItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.VH> {

    private final ArrayList<OrderItem> data = new ArrayList<>();

    public void setData(ArrayList<OrderItem> list) {
        data.clear();
        data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        OrderItem it = data.get(position);

        String gia = h.itemView.getContext().getString(
                R.string.admin_price_currency,
                NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(it.donGia)
        );
        String tt = h.itemView.getContext().getString(
                R.string.admin_price_currency,
                NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(it.thanhTien)
        );

        h.tvName.setText(it.tenSanPham);
        h.tvSub.setText(h.itemView.getContext().getString(R.string.admin_order_item_sub, gia, it.soLuong, it.giamGia));
        h.tvAmount.setText(tt);
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSub, tvAmount;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvSub = itemView.findViewById(R.id.tvSub);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}