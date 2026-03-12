package com.example.phonestore.ui.home;

public class InventoryManagementItem {

    static final String STATUS_IN_STOCK = "IN_STOCK";
    static final String STATUS_LOW_STOCK = "LOW_STOCK";
    static final String STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";

    final long productId;
    final String productName;
    final String brand;
    final int currentStock;
    final int minimumStock;
    final int totalImport;
    final int totalExport;
    final String status;

    InventoryManagementItem(long productId,
                            String productName,
                            String brand,
                            int currentStock,
                            int minimumStock,
                            int totalImport,
                            int totalExport,
                            String status) {
        this.productId = productId;
        this.productName = productName;
        this.brand = brand;
        this.currentStock = currentStock;
        this.minimumStock = minimumStock;
        this.totalImport = totalImport;
        this.totalExport = totalExport;
        this.status = status;
    }
}
