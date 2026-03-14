package com.example.phonestore.ui.orders;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonestore.R;
import com.example.phonestore.data.model.Order;
import com.example.phonestore.data.model.OrderStatus;
import com.example.phonestore.data.model.PaymentStatus;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.VH> {

    public interface Listener {
        void onClick(Order order);
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

        String total = h.itemView.getContext().getString(
                R.string.admin_price_currency,
                NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(o.tongTien)
        );
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"))
                .format(new Date(o.ngayTao));

        h.tvTitle.setText(adminMode && o.username != null
                ? h.itemView.getContext().getString(R.string.admin_order_code_with_user, o.id, o.username)
                : h.itemView.getContext().getString(R.string.admin_order_code, o.id));
        h.tvSub.setText(h.itemView.getContext().getString(
                R.string.admin_order_date_status,
                date,
                h.itemView.getContext().getString(
                        R.string.order_list_status_pair,
                        formatOrderStatus(h.itemView.getContext(), o.trangThaiDon),
                        formatPaymentStatus(h.itemView.getContext(), o.trangThaiThanhToan)
                )
        ));
        h.tvTotal.setText(total);

        h.itemView.setOnClickListener(v -> listener.onClick(o));

        h.btnUpdateStatus.setVisibility(View.GONE);
        h.btnUpdateStatus.setOnClickListener(null);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static String formatOrderStatus(Context context, String status) {
        if (status == null || status.trim().isEmpty()) return "-";
        if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)) {
            return context.getString(R.string.order_status_pending);
        }
        if (OrderStatus.STATUS_DANG_XU_LY.equals(status)) {
            return context.getString(R.string.order_status_processing);
        }
        if (OrderStatus.STATUS_DA_GIAO.equals(status)) {
            return context.getString(R.string.order_status_delivered);
        }
        if (OrderStatus.STATUS_DA_HUY.equals(status)) {
            return context.getString(R.string.order_status_cancelled);
        }
        return status.replace('_', ' ');
    }

    public static String formatPaymentStatus(Context context, String status) {
        if (status == null || status.trim().isEmpty()) return "-";
        if (PaymentStatus.STATUS_CHUA_THANH_TOAN.equals(status)) {
            return context.getString(R.string.payment_status_unpaid);
        }
        if (PaymentStatus.STATUS_CHO_THANH_TOAN.equals(status)) {
            return context.getString(R.string.payment_status_waiting);
        }
        if (PaymentStatus.STATUS_DA_THANH_TOAN.equals(status)) {
            return context.getString(R.string.payment_status_paid);
        }
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
