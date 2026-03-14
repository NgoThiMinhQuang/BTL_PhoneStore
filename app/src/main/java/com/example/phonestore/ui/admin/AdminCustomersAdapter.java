package com.example.phonestore.ui.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.User;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class AdminCustomersAdapter extends RecyclerView.Adapter<AdminCustomersAdapter.VH> {

    public interface Listener {
        void onEdit(User user);
        void onDelete(User user);
    }

    private final ArrayList<User> data = new ArrayList<>();
    private final Listener listener;

    public AdminCustomersAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(ArrayList<User> list) {
        data.clear();
        data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_customer, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        User u = data.get(position);

        Context context = h.itemView.getContext();
        h.tvName.setText((u.fullname == null || u.fullname.trim().isEmpty()) ? context.getString(R.string.admin_customer_name_fallback) : u.fullname);
        h.tvUsername.setText(context.getString(R.string.admin_customer_username, u.username == null ? "" : u.username));
        h.tvRole.setText(context.getString(R.string.admin_customer_role_customer));
        h.tvStatus.setText(u.isActive ? R.string.admin_customer_status_active : R.string.admin_customer_status_inactive);

        if (u.orderCount > 0) {
            h.tvOrderMetric.setText(context.getString(R.string.admin_customer_orders_metric, u.orderCount));
        } else {
            h.tvOrderMetric.setText(R.string.admin_customer_orders_empty);
        }

        if (u.deliveredSpend > 0) {
            h.tvSpendMetric.setText(context.getString(
                    R.string.admin_customer_spend_metric,
                    java.text.NumberFormat.getNumberInstance(new java.util.Locale("vi", "VN")).format(u.deliveredSpend)
            ));
        } else {
            h.tvSpendMetric.setText(R.string.admin_customer_spend_empty);
        }

        h.btnEdit.setOnClickListener(v -> listener.onEdit(u));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(u));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvUsername, tvRole, tvStatus, tvOrderMetric, tvSpendMetric;
        MaterialButton btnEdit, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvOrderMetric = itemView.findViewById(R.id.tvOrderMetric);
            tvSpendMetric = itemView.findViewById(R.id.tvSpendMetric);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}