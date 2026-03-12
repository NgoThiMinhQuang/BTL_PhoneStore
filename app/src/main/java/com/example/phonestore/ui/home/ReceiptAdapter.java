package com.example.phonestore.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.Receipt;

import java.util.ArrayList;

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
        Receipt receipt = data.get(position);
        h.tvReceiptCode.setText(receipt.getDisplayCode());
        h.tvReceiptDate.setText(ReceiptUiFormatter.formatDate(receipt.createdAt));
        h.tvReceiptSupplier.setText(h.itemView.getContext().getString(R.string.receipt_supplier_name_value, valueOrDash(receipt.supplierName)));
        h.tvReceiptCreator.setText(h.itemView.getContext().getString(R.string.receipt_creator_name_value, valueOrDash(receipt.creatorName)));
        if (receipt.note == null || receipt.note.trim().isEmpty()) {
            h.tvReceiptNote.setText(h.itemView.getContext().getString(R.string.receipt_note_value, h.itemView.getContext().getString(R.string.receipt_note_empty)));
        } else {
            h.tvReceiptNote.setText(h.itemView.getContext().getString(R.string.receipt_note_value, receipt.note.trim()));
        }
        h.tvReceiptQuantity.setText(h.itemView.getContext().getString(R.string.receipt_quantity_value, receipt.totalQuantity));
        h.tvReceiptAmount.setText(ReceiptUiFormatter.formatCurrency(h.itemView.getContext(), receipt.totalAmount));
        ReceiptUiFormatter.applyStatusBadge(h.tvReceiptStatus, receipt.status);
        h.itemView.setOnClickListener(v -> listener.onClick(receipt));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvReceiptCode;
        final TextView tvReceiptDate;
        final TextView tvReceiptStatus;
        final TextView tvReceiptSupplier;
        final TextView tvReceiptCreator;
        final TextView tvReceiptNote;
        final TextView tvReceiptQuantity;
        final TextView tvReceiptAmount;

        VH(@NonNull View itemView) {
            super(itemView);
            tvReceiptCode = itemView.findViewById(R.id.tvReceiptCode);
            tvReceiptDate = itemView.findViewById(R.id.tvReceiptDate);
            tvReceiptStatus = itemView.findViewById(R.id.tvReceiptStatus);
            tvReceiptSupplier = itemView.findViewById(R.id.tvReceiptSupplier);
            tvReceiptCreator = itemView.findViewById(R.id.tvReceiptCreator);
            tvReceiptNote = itemView.findViewById(R.id.tvReceiptNote);
            tvReceiptQuantity = itemView.findViewById(R.id.tvReceiptQuantity);
            tvReceiptAmount = itemView.findViewById(R.id.tvReceiptAmount);
        }
    }
}
