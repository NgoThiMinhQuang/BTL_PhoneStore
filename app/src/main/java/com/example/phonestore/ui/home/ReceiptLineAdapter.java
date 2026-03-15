package com.example.phonestore.ui.home;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.ReceiptItem;

import java.util.ArrayList;

public class ReceiptLineAdapter extends RecyclerView.Adapter<ReceiptLineAdapter.VH> {

    interface Listener {
        void onRemove(int position);
        void onItemChanged();
    }

    private final ArrayList<ReceiptItem> data = new ArrayList<>();
    private final Listener listener;
    private final boolean editable;

    ReceiptLineAdapter(boolean editable, Listener listener) {
        this.editable = editable;
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

    void addOrMergeItem(ReceiptItem item) {
        for (int i = 0; i < data.size(); i++) {
            ReceiptItem existing = data.get(i);
            if (existing.productId == item.productId) {
                existing.quantity += item.quantity;
                if (item.unitCost > 0) {
                    existing.unitCost = item.unitCost;
                }
                existing.recalculateAmount();
                notifyItemChanged(i);
                notifyChanged();
                return;
            }
        }
        item.recalculateAmount();
        data.add(item);
        notifyItemInserted(data.size() - 1);
        notifyChanged();
    }

    void removeAt(int position) {
        if (position < 0 || position >= data.size()) {
            return;
        }
        data.remove(position);
        notifyItemRemoved(position);
        notifyChanged();
    }

    int getTotalQuantity() {
        int total = 0;
        for (ReceiptItem item : data) {
            total += Math.max(0, item.quantity);
        }
        return total;
    }

    int getTotalAmount() {
        int total = 0;
        for (ReceiptItem item : data) {
            item.recalculateAmount();
            total += Math.max(0, item.amount);
        }
        return total;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receipt_line, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ReceiptItem item = data.get(position);
        if (holder.costWatcher != null) {
            holder.edtUnitCost.removeTextChangedListener(holder.costWatcher);
        }
        if (holder.quantityWatcher != null) {
            holder.edtQuantity.removeTextChangedListener(holder.quantityWatcher);
        }

        holder.tvProductName.setText(item.productName);
        holder.edtUnitCost.setText(String.valueOf(item.unitCost));
        holder.edtQuantity.setText(String.valueOf(item.quantity));
        holder.tvLineAmount.setText(holder.itemView.getContext().getString(
                R.string.receipt_line_amount_value,
                ReceiptUiFormatter.formatCurrency(holder.itemView.getContext(), item.amount)
        ));

        holder.btnDeleteLine.setVisibility(editable ? View.VISIBLE : View.GONE);
        holder.edtUnitCost.setEnabled(editable);
        holder.edtQuantity.setEnabled(editable);
        holder.edtUnitCost.setFocusable(editable);
        holder.edtQuantity.setFocusable(editable);
        holder.edtUnitCost.setFocusableInTouchMode(editable);
        holder.edtQuantity.setFocusableInTouchMode(editable);
        holder.edtUnitCost.setCursorVisible(editable);
        holder.edtQuantity.setCursorVisible(editable);

        bindWatchers(holder, item);
        holder.btnDeleteLine.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemove(holder.getBindingAdapterPosition());
            }
        });
    }

    private void bindWatchers(VH holder, ReceiptItem item) {
        if (holder.costWatcher != null) {
            holder.edtUnitCost.removeTextChangedListener(holder.costWatcher);
        }
        if (holder.quantityWatcher != null) {
            holder.edtQuantity.removeTextChangedListener(holder.quantityWatcher);
        }

        if (!editable) {
            holder.costWatcher = null;
            holder.quantityWatcher = null;
            return;
        }

        holder.costWatcher = new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                item.unitCost = parsePositiveInt(s);
                item.recalculateAmount();
                holder.tvLineAmount.setText(holder.itemView.getContext().getString(
                        R.string.receipt_line_amount_value,
                        ReceiptUiFormatter.formatCurrency(holder.itemView.getContext(), item.amount)
                ));
                notifyChanged();
            }
        };

        holder.quantityWatcher = new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                item.quantity = parsePositiveInt(s);
                item.recalculateAmount();
                holder.tvLineAmount.setText(holder.itemView.getContext().getString(
                        R.string.receipt_line_amount_value,
                        ReceiptUiFormatter.formatCurrency(holder.itemView.getContext(), item.amount)
                ));
                notifyChanged();
            }
        };

        holder.edtUnitCost.addTextChangedListener(holder.costWatcher);
        holder.edtQuantity.addTextChangedListener(holder.quantityWatcher);
    }

    private int parsePositiveInt(Editable value) {
        if (value == null) {
            return 0;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void notifyChanged() {
        if (listener != null) {
            listener.onItemChanged();
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvProductName;
        final TextView tvLineAmount;
        final EditText edtUnitCost;
        final EditText edtQuantity;
        final ImageView btnDeleteLine;
        TextWatcher costWatcher;
        TextWatcher quantityWatcher;

        VH(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvLineAmount = itemView.findViewById(R.id.tvLineAmount);
            edtUnitCost = itemView.findViewById(R.id.edtUnitCost);
            edtQuantity = itemView.findViewById(R.id.edtQuantity);
            btnDeleteLine = itemView.findViewById(R.id.btnDeleteLine);
        }
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
