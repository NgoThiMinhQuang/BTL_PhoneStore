package com.example.phonestore.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.ReceiptItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ReceiptLineAdapter extends RecyclerView.Adapter<ReceiptLineAdapter.VH> {

    interface Listener {
        void onLongClick(int position);
    }

    private final ArrayList<ReceiptItem> data = new ArrayList<>();
    private final Listener listener;

    ReceiptLineAdapter() {
        this.listener = null;
    }

    ReceiptLineAdapter(Listener listener) {
        this.listener = listener;
    }

    void setData(ArrayList<ReceiptItem> list) {
        data.clear();
        data.addAll(list);
        notifyDataSetChanged();
    }

    ArrayList<ReceiptItem> getData() {
        return new ArrayList<>(data);
    }

    void addItem(ReceiptItem item) {
        data.add(item);
        notifyItemInserted(data.size() - 1);
    }

    void removeAt(int position) {
        if (position < 0 || position >= data.size()) return;
        data.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receipt_line, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ReceiptItem item = data.get(position);
        h.tvTitle.setText(h.itemView.getContext().getString(R.string.receipt_item_title, item.productName, item.quantity));
        h.tvMeta.setText(h.itemView.getContext().getString(
                R.string.receipt_item_meta,
                h.itemView.getContext().getString(R.string.admin_price_currency, NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(item.unitCost))
        ));
        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMeta;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
        }
    }
}
