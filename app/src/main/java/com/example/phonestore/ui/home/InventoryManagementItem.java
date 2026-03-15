package com.example.phonestore.ui.home;

import com.example.phonestore.utils.InventoryPolicy;

public class InventoryManagementItem {

    static final String STATUS_IN_STOCK = InventoryPolicy.STATUS_IN_STOCK;
    static final String STATUS_LOW_STOCK = InventoryPolicy.STATUS_LOW_STOCK;
    static final String STATUS_OUT_OF_STOCK = InventoryPolicy.STATUS_OUT_OF_STOCK;

    final long productId;
    final String productName;
    final String brand;
    final String imageName;
    final int currentStock;
    final int minimumStock;
    final int totalImport;
    final int totalExport;
    final String status;

    InventoryManagementItem(long productId,
                            String productName,
                            String brand,
                            String imageName,
                            int currentStock,
                            int minimumStock,
                            int totalImport,
                            int totalExport,
                            String status) {
        this.productId = productId;
        this.productName = productName;
        this.brand = brand;
        this.imageName = imageName;
        this.currentStock = currentStock;
        this.minimumStock = minimumStock;
        this.totalImport = totalImport;
        this.totalExport = totalExport;
        this.status = status;
    }

    InventoryManagementItem(long productId,
                            String productName,
                            String brand,
                            int currentStock,
                            int minimumStock,
                            int totalImport,
                            int totalExport,
                            String status) {
        this(productId, productName, brand, null, currentStock, minimumStock, totalImport, totalExport, status);
    }
}
