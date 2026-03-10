package com.example.phonestore.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Product;
import com.example.phonestore.data.model.Receipt;
import com.example.phonestore.data.model.ReceiptItem;

import java.util.ArrayList;

public class ReceiptDao {

    private final DBHelper dbHelper;
    private final InventoryHistoryDao historyDao;

    public ReceiptDao(Context ctx) {
        dbHelper = new DBHelper(ctx);
        historyDao = new InventoryHistoryDao(ctx);
    }

    public long createReceipt(long supplierId, String note, ArrayList<ReceiptItem> items) {
        if (items == null || items.isEmpty()) return -1;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int totalQty = 0;
            int totalAmount = 0;
            for (ReceiptItem item : items) {
                totalQty += item.quantity;
                totalAmount += item.amount;
            }

            ContentValues receiptValues = new ContentValues();
            receiptValues.put(DBHelper.COL_R_SUPPLIER_ID, supplierId);
            receiptValues.put(DBHelper.COL_R_TOTAL_QTY, totalQty);
            receiptValues.put(DBHelper.COL_R_TOTAL_AMOUNT, totalAmount);
            receiptValues.put(DBHelper.COL_R_CREATED, System.currentTimeMillis());
            receiptValues.put(DBHelper.COL_R_NOTE, note);
            long receiptId = db.insert(DBHelper.TBL_RECEIPTS, null, receiptValues);
            if (receiptId == -1) return -1;

            for (ReceiptItem item : items) {
                ContentValues itemValues = new ContentValues();
                itemValues.put(DBHelper.COL_RI_RECEIPT_ID, receiptId);
                itemValues.put(DBHelper.COL_RI_PRODUCT_ID, item.productId);
                itemValues.put(DBHelper.COL_RI_PRODUCT_NAME, item.productName);
                itemValues.put(DBHelper.COL_RI_QTY, item.quantity);
                itemValues.put(DBHelper.COL_RI_UNIT_COST, item.unitCost);
                itemValues.put(DBHelper.COL_RI_AMOUNT, item.amount);
                if (db.insert(DBHelper.TBL_RECEIPT_ITEMS, null, itemValues) == -1) return -1;

                db.execSQL(
                        "UPDATE " + DBHelper.TBL_PRODUCTS +
                                " SET " + DBHelper.COL_P_STOCK + " = " + DBHelper.COL_P_STOCK + " + ?" +
                                " WHERE " + DBHelper.COL_ID + "=?",
                        new Object[]{item.quantity, item.productId}
                );

                historyDao.insert(
                        db,
                        item.productId,
                        item.productName,
                        InventoryHistoryDao.ACTION_IMPORT,
                        item.quantity,
                        "RECEIPT",
                        receiptId,
                        note
                );
            }

            db.setTransactionSuccessful();
            return receiptId;
        } finally {
            db.endTransaction();
        }
    }

    public ArrayList<Receipt> getRecentReceipts() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Receipt> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT r." + DBHelper.COL_ID + "," +
                        " r." + DBHelper.COL_R_SUPPLIER_ID + "," +
                        " s." + DBHelper.COL_S_NAME + "," +
                        " r." + DBHelper.COL_R_TOTAL_QTY + "," +
                        " r." + DBHelper.COL_R_TOTAL_AMOUNT + "," +
                        " r." + DBHelper.COL_R_CREATED + "," +
                        " r." + DBHelper.COL_R_NOTE +
                        " FROM " + DBHelper.TBL_RECEIPTS + " r" +
                        " JOIN " + DBHelper.TBL_SUPPLIERS + " s ON s." + DBHelper.COL_ID + " = r." + DBHelper.COL_R_SUPPLIER_ID +
                        " ORDER BY r." + DBHelper.COL_ID + " DESC",
                null
        );
        while (c.moveToNext()) list.add(readReceipt(c));
        c.close();
        return list;
    }

    public ArrayList<ReceiptItem> getReceiptItems(long receiptId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<ReceiptItem> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + DBHelper.TBL_RECEIPT_ITEMS +
                        " WHERE " + DBHelper.COL_RI_RECEIPT_ID + "=? ORDER BY " + DBHelper.COL_ID + " DESC",
                new String[]{String.valueOf(receiptId)}
        );
        while (c.moveToNext()) list.add(readItem(c));
        c.close();
        return list;
    }

    public boolean deleteReceipt(long receiptId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ArrayList<ReceiptItem> items = getReceiptItems(receiptId);
            for (ReceiptItem item : items) {
                db.execSQL(
                        "UPDATE " + DBHelper.TBL_PRODUCTS +
                                " SET " + DBHelper.COL_P_STOCK + " = MAX(0, " + DBHelper.COL_P_STOCK + " - ?)" +
                                " WHERE " + DBHelper.COL_ID + "=?",
                        new Object[]{item.quantity, item.productId}
                );
            }
            db.delete(DBHelper.TBL_INVENTORY_HISTORY, DBHelper.COL_H_REF_TYPE + "=? AND " + DBHelper.COL_H_REF_ID + "=?", new String[]{"RECEIPT", String.valueOf(receiptId)});
            int deleted = db.delete(DBHelper.TBL_RECEIPTS, DBHelper.COL_ID + "=?", new String[]{String.valueOf(receiptId)});
            db.setTransactionSuccessful();
            return deleted > 0;
        } finally {
            db.endTransaction();
        }
    }

    public boolean hasReceiptForSupplier(long supplierId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT 1 FROM " + DBHelper.TBL_RECEIPTS + " WHERE " + DBHelper.COL_R_SUPPLIER_ID + "=? LIMIT 1",
                new String[]{String.valueOf(supplierId)}
        );
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    public int countReceipts() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TBL_RECEIPTS, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    private Receipt readReceipt(Cursor c) {
        Receipt receipt = new Receipt();
        receipt.id = c.getLong(0);
        receipt.supplierId = c.getLong(1);
        receipt.supplierName = c.getString(2);
        receipt.totalQuantity = c.getInt(3);
        receipt.totalAmount = c.getInt(4);
        receipt.createdAt = c.getLong(5);
        receipt.note = c.getString(6);
        return receipt;
    }

    private ReceiptItem readItem(Cursor c) {
        ReceiptItem item = new ReceiptItem();
        item.id = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_ID));
        item.receiptId = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_RI_RECEIPT_ID));
        item.productId = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_RI_PRODUCT_ID));
        item.productName = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_RI_PRODUCT_NAME));
        item.quantity = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_RI_QTY));
        item.unitCost = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_RI_UNIT_COST));
        item.amount = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_RI_AMOUNT));
        return item;
    }
}
