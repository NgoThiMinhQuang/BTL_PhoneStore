package com.example.phonestore.ui.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
        if (list != null) {
            data.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory_history, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        InventoryHistoryEntry e = data.get(position);
        Context context = h.itemView.getContext();
        boolean isImport = InventoryHistoryDao.ACTION_IMPORT.equals(e.actionType);

        h.tvTitle.setText(isImport ? R.string.inventory_history_import : R.string.inventory_history_export);
        h.tvDate.setText(new SimpleDateFormat("yyyy-MM-dd", new Locale("vi", "VN")).format(new Date(e.createdAt)));
        h.tvProduct.setText(boldLabel(context, "Sản phẩm", fallback(e.productName)));
        h.tvRef.setText(boldLabel(context, "Chứng từ", formatReference(context, e)));
        h.tvStockAfter.setText(boldLabel(context, "Tồn kho sau", String.valueOf(Math.max(0, e.stockAfter))));
        h.tvActor.setText(boldLabel(context, "Thực hiện", fallbackActor(context, e.actorName)));
        h.tvNote.setText(context.getString(R.string.inventory_history_note_label, fallbackNote(context, e.note)));

        h.tvQty.setText((isImport ? "+" : "-") + e.quantity);
        h.tvQty.setTextColor(ContextCompat.getColor(context, isImport ? R.color.admin_success : R.color.admin_dashboard_red));

        h.tvHistoryIcon.setText(isImport ? "↓" : "↑");
        h.tvHistoryIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, isImport ? R.color.admin_success : R.color.admin_dashboard_red)));
    }

    private CharSequence boldLabel(Context context, String label, String value) {
        return android.text.Html.fromHtml(label + ": <b>" + value + "</b>", android.text.Html.FROM_HTML_MODE_LEGACY);
    }

    private String formatReference(Context context, InventoryHistoryEntry entry) {
        if ("RECEIPT".equals(entry.referenceType)) {
            return context.getString(R.string.inventory_history_receipt_code, entry.referenceId);
        }
        if ("ORDER".equals(entry.referenceType)) {
            return context.getString(R.string.inventory_history_order_code, entry.referenceId);
        }
        return context.getString(R.string.inventory_history_unknown_code);
    }

    private String fallback(String value) {
        return value == null || value.trim().isEmpty() ? "---" : value.trim();
    }

    private String fallbackActor(Context context, String value) {
        return value == null || value.trim().isEmpty() ? context.getString(R.string.inventory_history_empty_actor) : value.trim();
    }

    private String fallbackNote(Context context, String value) {
        return value == null || value.trim().isEmpty() ? context.getString(R.string.inventory_history_empty_note) : value.trim();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvHistoryIcon;
        final TextView tvTitle;
        final TextView tvQty;
        final TextView tvDate;
        final TextView tvProduct;
        final TextView tvRef;
        final TextView tvStockAfter;
        final TextView tvActor;
        final TextView tvNote;

        VH(@NonNull View itemView) {
            super(itemView);
            tvHistoryIcon = itemView.findViewById(R.id.tvHistoryIcon);
            tvTitle = itemView.findViewById(R.id.tvHistoryTitle);
            tvQty = itemView.findViewById(R.id.tvHistoryQty);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            tvProduct = itemView.findViewById(R.id.tvHistoryProduct);
            tvRef = itemView.findViewById(R.id.tvHistoryRef);
            tvStockAfter = itemView.findViewById(R.id.tvHistoryStockAfter);
            tvActor = itemView.findViewById(R.id.tvHistoryActor);
            tvNote = itemView.findViewById(R.id.tvHistoryNote);
        }
    }
}
