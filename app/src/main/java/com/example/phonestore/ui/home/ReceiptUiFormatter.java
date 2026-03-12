package com.example.phonestore.ui.home;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.phonestore.R;
import com.example.phonestore.data.model.Receipt;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class ReceiptUiFormatter {

    private static final Locale VIETNAMESE = new Locale("vi", "VN");

    private ReceiptUiFormatter() {
    }

    static String formatCurrency(Context context, int amount) {
        return context.getString(
                R.string.admin_price_currency,
                NumberFormat.getNumberInstance(VIETNAMESE).format(amount)
        );
    }

    static String formatDate(long timestamp) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", VIETNAMESE).format(new Date(timestamp));
    }

    static String formatStatus(Context context, String status) {
        if (Receipt.STATUS_COMPLETED.equals(status)) {
            return context.getString(R.string.receipt_status_completed);
        }
        return context.getString(R.string.receipt_status_draft);
    }

    static void applyStatusBadge(TextView view, String status) {
        Context context = view.getContext();
        int backgroundColor = Receipt.STATUS_COMPLETED.equals(status)
                ? R.color.admin_success_soft
                : R.color.admin_warning_soft;
        int textColor = Receipt.STATUS_COMPLETED.equals(status)
                ? R.color.admin_success
                : R.color.admin_warning;

        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(dp(context, 999));
        badge.setColor(ContextCompat.getColor(context, backgroundColor));
        badge.setStroke(dp(context, 1), ContextCompat.getColor(context, textColor));

        view.setBackground(badge);
        view.setText(formatStatus(context, status));
        view.setTextColor(ContextCompat.getColor(context, textColor));
    }

    private static int dp(Context context, int value) {
        return Math.round(context.getResources().getDisplayMetrics().density * value);
    }
}
