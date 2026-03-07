package com.example.phonestore.ui.orders;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.Order;
import com.example.phonestore.data.model.OrderStatus;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.VH> {

    private static final String[] ADMIN_STATUSES = new String[]{
            OrderStatus.STATUS_CHO_XAC_NHAN,
            OrderStatus.STATUS_DA_THANH_TOAN,
            OrderStatus.STATUS_DANG_XU_LY,
            OrderStatus.STATUS_DA_GIAO,
            OrderStatus.STATUS_DA_HUY
    };

    public interface Listener {
        void onClick(Order order);
        void onUpdateStatus(Order order, String newStatus);
    }

    private final ArrayList<Order> data = new ArrayList<>();
    private final Listener listener;
    private boolean adminMode = false;

    public OrdersAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(ArrayList<Order> list, boolean adminMode) {
        data.clear();
        data.addAll(list);
        this.adminMode = adminMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Order o = data.get(position);

        String total = NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(o.tongTien) + "đ";
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"))
                .format(new Date(o.ngayTao));

        h.tvTitle.setText("Đơn #" + o.id + (adminMode && o.username != null ? " - " + o.username : ""));
        h.tvSub.setText(date + "  |  " + dinhDangTrangThai(o.trangThai));
        h.tvTotal.setText(total);

        h.itemView.setOnClickListener(v -> listener.onClick(o));

        h.btnUpdateStatus.setVisibility(adminMode ? View.VISIBLE : View.GONE);
        h.btnUpdateStatus.setOnClickListener(v -> hienThiHopThoaiCapNhatTrangThai(v.getContext(), o));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private void hienThiHopThoaiCapNhatTrangThai(android.content.Context context, Order order) {
        int checked = 0;
        for (int i = 0; i < ADMIN_STATUSES.length; i++) {
            if (ADMIN_STATUSES[i].equals(order.trangThai)) {
                checked = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.update_status)
                .setSingleChoiceItems(ADMIN_STATUSES, checked, (dialog, which) -> {
                    String selected = ADMIN_STATUSES[which];
                    dialog.dismiss();
                    listener.onUpdateStatus(order, selected);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String dinhDangTrangThai(String status) {
        if (status == null || status.trim().isEmpty()) return "-";
        return status.replace('_', ' ');
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSub, tvTotal;
        MaterialButton btnUpdateStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSub = itemView.findViewById(R.id.tvSub);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
        }
    }
}
