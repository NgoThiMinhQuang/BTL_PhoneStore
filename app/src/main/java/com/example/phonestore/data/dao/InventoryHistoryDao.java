package com.example.phonestore.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.InventoryHistoryEntry;

import java.util.ArrayList;

public class InventoryHistoryDao {

    public static final String ACTION_IMPORT = "IMPORT";
    public static final String ACTION_EXPORT = "EXPORT";
    public static final String ACTION_CANCEL_RETURN = "CANCEL_RETURN";

    private final DBHelper dbHelper;

    public InventoryHistoryDao(Context ctx) {
        dbHelper = new DBHelper(ctx);
    }

    public void insert(SQLiteDatabase db,
                       long productId,
                       String productName,
                       String action,
                       int quantity,
                       String referenceType,
                       long referenceId,
                       String note) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_H_PRODUCT_ID, productId);
        v.put(DBHelper.COL_H_PRODUCT_NAME, productName);
        v.put(DBHelper.COL_H_ACTION, action);
        v.put(DBHelper.COL_H_QTY, quantity);
        v.put(DBHelper.COL_H_REF_TYPE, referenceType);
        v.put(DBHelper.COL_H_REF_ID, referenceId);
        v.put(DBHelper.COL_H_NOTE, note);
        v.put(DBHelper.COL_H_CREATED, System.currentTimeMillis());
        db.insert(DBHelper.TBL_INVENTORY_HISTORY, null, v);
    }

    public ArrayList<InventoryHistoryEntry> getRecent(int limit) {
        return getRecent(limit, null);
    }

    public ArrayList<InventoryHistoryEntry> getAll() {
        return getRecent(Integer.MAX_VALUE, null);
    }

    public ArrayList<InventoryHistoryEntry> getRecent(int limit, String keyword) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<InventoryHistoryEntry> list = new ArrayList<>();
        Cursor c = db.rawQuery(buildQuery(keyword, limit), buildArgs(keyword));
        while (c.moveToNext()) {
            list.add(read(c));
        }
        c.close();
        return list;
    }

    private String buildQuery(String keyword, int limit) {
        String base = "SELECT h.*, CASE" +
                " WHEN h." + DBHelper.COL_H_REF_TYPE + "='RECEIPT' THEN IFNULL(r." + DBHelper.COL_R_CREATED_BY + ",'Admin')" +
                " WHEN h." + DBHelper.COL_H_REF_TYPE + "='ORDER' THEN 'System'" +
                " ELSE 'System' END AS actor_name" +
                " FROM " + DBHelper.TBL_INVENTORY_HISTORY + " h" +
                " LEFT JOIN " + DBHelper.TBL_RECEIPTS + " r ON h." + DBHelper.COL_H_REF_TYPE + "='RECEIPT' AND r." + DBHelper.COL_ID + " = h." + DBHelper.COL_H_REF_ID;

        if (keyword != null && !keyword.trim().isEmpty()) {
            return base +
                    " WHERE h." + DBHelper.COL_H_PRODUCT_NAME + " LIKE ? OR IFNULL(h." + DBHelper.COL_H_NOTE + ",'') LIKE ?" +
                    " ORDER BY h." + DBHelper.COL_ID + " DESC LIMIT " + limit;
        }

        return base + " ORDER BY h." + DBHelper.COL_ID + " DESC LIMIT " + limit;
    }

    private String[] buildArgs(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        String k = "%" + keyword.trim() + "%";
        return new String[]{k, k};
    }

    private InventoryHistoryEntry read(Cursor c) {
        InventoryHistoryEntry e = new InventoryHistoryEntry();
        e.id = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_ID));
        e.productId = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_H_PRODUCT_ID));
        e.productName = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_H_PRODUCT_NAME));
        e.actionType = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_H_ACTION));
        e.quantity = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_H_QTY));
        e.referenceType = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_H_REF_TYPE));
        e.referenceId = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_H_REF_ID));
        e.note = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_H_NOTE));
        e.createdAt = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_H_CREATED));
        int actorIndex = c.getColumnIndex("actor_name");
        if (actorIndex >= 0) {
            e.actorName = c.getString(actorIndex);
        }
        return e;
    }
}
