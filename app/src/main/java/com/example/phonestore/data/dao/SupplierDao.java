package com.example.phonestore.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Supplier;

import java.util.ArrayList;

public class SupplierDao {

    private final DBHelper dbHelper;

    public SupplierDao(Context ctx) {
        dbHelper = new DBHelper(ctx);
    }

    public ArrayList<Supplier> getAll(String keyword) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Supplier> list = new ArrayList<>();
        Cursor c;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = "%" + keyword.trim() + "%";
            c = db.rawQuery(
                    "SELECT * FROM " + DBHelper.TBL_SUPPLIERS +
                            " WHERE " + DBHelper.COL_S_NAME + " LIKE ? OR " + DBHelper.COL_S_BRAND + " LIKE ?" +
                            " ORDER BY " + DBHelper.COL_ID + " DESC",
                    new String[]{k, k}
            );
        } else {
            c = db.rawQuery(
                    "SELECT * FROM " + DBHelper.TBL_SUPPLIERS + " ORDER BY " + DBHelper.COL_ID + " DESC",
                    null
            );
        }
        while (c.moveToNext()) list.add(read(c));
        c.close();
        return list;
    }

    public Supplier getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + DBHelper.TBL_SUPPLIERS + " WHERE " + DBHelper.COL_ID + "=? LIMIT 1",
                new String[]{String.valueOf(id)}
        );
        Supplier supplier = null;
        if (c.moveToFirst()) supplier = read(c);
        c.close();
        return supplier;
    }

    public long insert(Supplier supplier) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(DBHelper.TBL_SUPPLIERS, null, toValues(supplier));
    }

    public boolean update(Supplier supplier) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update(
                DBHelper.TBL_SUPPLIERS,
                toValues(supplier),
                DBHelper.COL_ID + "=?",
                new String[]{String.valueOf(supplier.id)}
        ) > 0;
    }

    public boolean delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TBL_SUPPLIERS, DBHelper.COL_ID + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public int countAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TBL_SUPPLIERS, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    private ContentValues toValues(Supplier supplier) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_S_NAME, supplier.name);
        v.put(DBHelper.COL_S_BRAND, supplier.brand);
        v.put(DBHelper.COL_S_PHONE, supplier.phone);
        v.put(DBHelper.COL_S_ADDRESS, supplier.address);
        return v;
    }

    private Supplier read(Cursor c) {
        Supplier supplier = new Supplier();
        supplier.id = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_ID));
        supplier.name = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_S_NAME));
        supplier.brand = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_S_BRAND));
        supplier.phone = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_S_PHONE));
        supplier.address = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_S_ADDRESS));
        return supplier;
    }
}
