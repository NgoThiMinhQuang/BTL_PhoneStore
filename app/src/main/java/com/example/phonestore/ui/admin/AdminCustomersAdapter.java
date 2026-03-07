package com.example.phonestore.ui.admin;

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

        h.tvName.setText((u.fullname == null || u.fullname.trim().isEmpty()) ? "-" : u.fullname);
        h.tvUsername.setText("@" + (u.username == null ? "" : u.username));
        h.tvRole.setText(u.role == null ? "" : u.role);

        h.btnEdit.setOnClickListener(v -> listener.onEdit(u));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(u));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvUsername, tvRole;
        MaterialButton btnEdit, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvRole = itemView.findViewById(R.id.tvRole);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}