package com.example.phonestore.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.Receipt;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.VH> {

    interface Listener {
        void onClick(Receipt receipt);
    }

    private final ArrayList<Receipt> data = new ArrayList<>();
    private final Listener listener;

    ReceiptAdapter(Listener listener) {
        this.listener = listener;
    }

    void setData(ArrayList<Receipt> list) {
        data.clear();
        data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receipt, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Receipt r = data.get(position);
        h.tvTitle.setText("Phiếu nhập #" + r.id + " • " + r.supplierName);
        h.tvSub.setText("SL " + r.totalQuantity + (r.note == null || r.note.isEmpty() ? "" : " • " + r.note));
        h.tvMeta.setText(NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(r.totalAmount) + "đ");
        h.itemView.setOnClickListener(v -> listener.onClick(r));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSub, tvMeta;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSub = itemView.findViewById(R.id.tvSub);
            tvMeta = itemView.findViewById(R.id.tvMeta);
        }
    }
}
