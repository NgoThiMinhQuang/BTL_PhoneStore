package com.example.phonestore.data.model;

public class ReceiptItem {
    public long id;
    public long receiptId;
    public long productId;
    public String productName;
    public int quantity;
    public int unitCost;
    public int amount;

    public void recalculateAmount() {
        int safeQuantity = Math.max(quantity, 0);
        int safeUnitCost = Math.max(unitCost, 0);
        amount = safeQuantity * safeUnitCost;
    }
}
