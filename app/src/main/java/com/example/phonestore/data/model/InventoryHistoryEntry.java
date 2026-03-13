package com.example.phonestore.data.model;

public class InventoryHistoryEntry {
    public long id;
    public long productId;
    public String productName;
    public String actionType;
    public int quantity;
    public String referenceType;
    public long referenceId;
    public String note;
    public long createdAt;
    public int stockAfter;
    public String actorName;
}
