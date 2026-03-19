package com.example.phonestore.ui.orders;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
        int layoutRes = viewType == 1 ? R.layout.item_order_admin : R.layout.item_order_customer;
        View v = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Order o = data.get(position);
        Context context = h.itemView.getContext();

        String total = context.getString(
                R.string.admin_price_currency,
                NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(o.tongTien)
        );
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"))
                .format(new Date(o.ngayTao));
        String orderStatus = formatOrderStatus(context, o.trangThaiDon);
        String paymentStatus = formatPaymentStatus(context, o.trangThaiThanhToan);
        int itemCount = Math.max(0, o.itemCount);

        if (adminMode) {
            h.tvTitle.setText(context.getString(R.string.admin_order_code, o.id));
            h.tvSub.setText(context.getString(
                    R.string.order_admin_card_customer_line,
                    valueOrDash(o.nguoiNhan),
                    valueOrDash(o.sdtNhan)
            ));
            if (h.tvMeta != null) {
                h.tvMeta.setText(context.getString(
                        R.string.order_admin_card_meta,
                        date,
                        Math.max(1, itemCount)
                ));
            }
        } else {
            h.tvTitle.setText(context.getString(R.string.admin_order_code, o.id));
            h.tvSub.setText(context.getString(R.string.order_customer_card_items, Math.max(1, itemCount)));
            if (h.tvMeta != null) {
                h.tvMeta.setText(date);
            }
        }

        h.tvTotal.setText(total);
        bindChip(h.tvStatusChip, o.trangThaiDon, adminMode, true);
        bindChip(h.tvPaymentChip, o.trangThaiThanhToan, adminMode, false);

        h.itemView.setOnClickListener(v -> listener.onClick(o));

        if (h.btnUpdateStatus != null) {
            h.btnUpdateStatus.setVisibility(View.GONE);
            h.btnUpdateStatus.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        return adminMode ? 1 : 0;
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
        if (PaymentStatus.STATUS_HET_HAN_THANH_TOAN.equals(status)) {
            return context.getString(R.string.payment_status_expired);
        }
        return status.replace('_', ' ');
    }

    private void bindChip(TextView view, String status, boolean adminMode, boolean orderChip) {
        if (view == null) {
            return;
        }
        Context context = view.getContext();
        int backgroundColor = ContextCompat.getColor(context, getChipBackgroundColor(status, adminMode, orderChip));
        int accentColor = ContextCompat.getColor(context, getChipAccentColor(status, adminMode, orderChip));

        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(dp(context, 999));
        badge.setColor(backgroundColor);
        badge.setStroke(dp(context, 1), accentColor);

        view.setBackground(badge);
        view.setText(orderChip ? formatOrderStatus(context, status) : formatPaymentStatus(context, status));
        view.setTextColor(accentColor);
    }

    private int getChipBackgroundColor(String status, boolean adminMode, boolean orderChip) {
        if (orderChip) {
            if (OrderStatus.STATUS_DA_GIAO.equals(status)) return R.color.admin_success_soft;
            if (OrderStatus.STATUS_DA_HUY.equals(status)) return R.color.admin_danger_soft;
            if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)) return adminMode ? R.color.admin_warning_soft : R.color.panel_soft;
            if (OrderStatus.STATUS_DANG_XU_LY.equals(status)) return adminMode ? R.color.admin_surface_soft : R.color.panel_soft;
            return adminMode ? R.color.admin_surface_soft : R.color.panel_soft;
        }

        if (PaymentStatus.STATUS_DA_THANH_TOAN.equals(status)) return R.color.admin_success_soft;
        if (PaymentStatus.STATUS_HET_HAN_THANH_TOAN.equals(status)) return R.color.admin_danger_soft;
        if (PaymentStatus.STATUS_CHO_THANH_TOAN.equals(status)) return adminMode ? R.color.admin_warning_soft : R.color.panel_soft;
        return adminMode ? R.color.admin_surface_soft : R.color.panel_soft;
    }

    private int getChipAccentColor(String status, boolean adminMode, boolean orderChip) {
        if (orderChip) {
            if (OrderStatus.STATUS_DA_GIAO.equals(status)) return R.color.admin_success;
            if (OrderStatus.STATUS_DA_HUY.equals(status)) return R.color.admin_danger;
            if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)) return adminMode ? R.color.admin_warning : R.color.red_primary;
            if (OrderStatus.STATUS_DANG_XU_LY.equals(status)) return adminMode ? R.color.admin_primary : R.color.red_primary;
            return adminMode ? R.color.admin_primary : R.color.red_primary;
        }

        if (PaymentStatus.STATUS_DA_THANH_TOAN.equals(status)) return R.color.admin_success;
        if (PaymentStatus.STATUS_HET_HAN_THANH_TOAN.equals(status)) return R.color.admin_danger;
        if (PaymentStatus.STATUS_CHO_THANH_TOAN.equals(status)) return adminMode ? R.color.admin_warning : R.color.red_primary;
        return adminMode ? R.color.admin_primary : R.color.red_primary;
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private int dp(Context context, int value) {
        return Math.round(context.getResources().getDisplayMetrics().density * value);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSub, tvMeta, tvTotal, tvStatusChip, tvPaymentChip;
        MaterialButton btnUpdateStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSub = itemView.findViewById(R.id.tvSub);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvStatusChip = itemView.findViewById(R.id.tvStatusChip);
            tvPaymentChip = itemView.findViewById(R.id.tvPaymentChip);
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
        }
    }
}
