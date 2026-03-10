package com.example.phonestore.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.InventoryHistoryDao;
import com.example.phonestore.data.model.InventoryHistoryEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class InventoryHistoryAdapter extends RecyclerView.Adapter<InventoryHistoryAdapter.VH> {

    private final ArrayList<InventoryHistoryEntry> data = new ArrayList<>();

    void setData(ArrayList<InventoryHistoryEntry> list) {
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
        InventoryHistoryEntry e = data.get(position);
        String action = InventoryHistoryDao.ACTION_IMPORT.equals(e.actionType)
                ? h.itemView.getContext().getString(R.string.inventory_history_import)
                : h.itemView.getContext().getString(R.string.inventory_history_export);
        String ref = h.itemView.getContext().getString(R.string.inventory_history_ref, e.referenceType == null ? "-" : e.referenceType, e.referenceId);
        String time = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN")).format(new Date(e.createdAt));
        h.tvTitle.setText(e.productName);
        h.tvSub.setText(action + " • " + ref + " • " + time);
        h.tvMeta.setText(h.itemView.getContext().getString(R.string.inventory_history_qty, e.quantity) + (e.note == null || e.note.isEmpty() ? "" : " • " + e.note));
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
