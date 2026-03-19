package com.example.phonestore.data.model;

import java.util.Locale;

public class Receipt {
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public long id;
    public long supplierId;
    public String supplierName;
    public int totalQuantity;
    public int totalAmount;
    public int lineCount;
    public long createdAt;
    public String note;
    public String status;
    public String creatorName;

    public String getDisplayCode() {
        return formatCode(id);
    }

    public boolean isDraft() {
        return STATUS_DRAFT.equals(status);
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    public static String formatCode(long id) {
        return String.format(Locale.US, "P%04d", id);
    }
}
