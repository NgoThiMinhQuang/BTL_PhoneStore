package com.example.phonestore.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Product;

import java.util.ArrayList;

public class ProductDao {

    private final DBHelper dbHelper;

    public ProductDao(Context ctx) {
        dbHelper = new DBHelper(ctx);
    }

    public ArrayList<Product> layTatCa() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Product> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT * FROM " + DBHelper.TBL_PRODUCTS + " ORDER BY " + DBHelper.COL_ID + " DESC",
                null
        );
        while (c.moveToNext()) list.add(docSanPham(c));
        c.close();
        return list;
    }

    public ArrayList<Product> layTheoHang(String hang) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Product> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + DBHelper.TBL_PRODUCTS +
                        " WHERE " + DBHelper.COL_P_BRAND + "=? ORDER BY " + DBHelper.COL_ID + " DESC",
                new String[]{hang}
        );
        while (c.moveToNext()) list.add(docSanPham(c));
        c.close();
        return list;
    }

    public ArrayList<Product> timKiem(String tuKhoa) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Product> list = new ArrayList<>();
        String k = "%" + tuKhoa + "%";
        Cursor c = db.rawQuery(
                "SELECT * FROM " + DBHelper.TBL_PRODUCTS +
                        " WHERE " + DBHelper.COL_P_NAME + " LIKE ? OR " + DBHelper.COL_P_BRAND + " LIKE ?" +
                        " ORDER BY " + DBHelper.COL_ID + " DESC",
                new String[]{k, k}
        );
        while (c.moveToNext()) list.add(docSanPham(c));
        c.close();
        return list;
    }

    public ArrayList<Product> timKiemTheoHang(String hang, String tuKhoa) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Product> list = new ArrayList<>();
        String k = "%" + tuKhoa + "%";
        Cursor c = db.rawQuery(
                "SELECT * FROM " + DBHelper.TBL_PRODUCTS +
                        " WHERE " + DBHelper.COL_P_BRAND + "=? AND " + DBHelper.COL_P_NAME + " LIKE ?" +
                        " ORDER BY " + DBHelper.COL_ID + " DESC",
                new String[]{hang, k}
        );
        while (c.moveToNext()) list.add(docSanPham(c));
        c.close();
        return list;
    }

    public Product getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + DBHelper.TBL_PRODUCTS + " WHERE " + DBHelper.COL_ID + "=? LIMIT 1",
                new String[]{String.valueOf(id)}
        );
        Product p = null;
        if (c.moveToFirst()) p = docSanPham(c);
        c.close();
        return p;
    }

    // ===== CRUD cho ADMIN =====
    public long insert(Product p) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(DBHelper.TBL_PRODUCTS, null, toValues(p));
    }

    public boolean update(Product p) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update(DBHelper.TBL_PRODUCTS, toValues(p),
                DBHelper.COL_ID + "=?", new String[]{String.valueOf(p.maSanPham)}) > 0;
    }

    public boolean delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TBL_PRODUCTS, DBHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)}) > 0;
    }

    public boolean giamTonKho(SQLiteDatabase db, long productId, int qty) {
        // dùng câu lệnh an toàn: ton_kho = ton_kho - qty (không âm)
        db.execSQL(
                "UPDATE " + DBHelper.TBL_PRODUCTS +
                        " SET " + DBHelper.COL_P_STOCK + " = " + DBHelper.COL_P_STOCK + " - ?" +
                        " WHERE " + DBHelper.COL_ID + "=? AND " + DBHelper.COL_P_STOCK + " >= ?",
                new Object[]{qty, productId, qty}
        );
        return true;
    }

    private ContentValues toValues(Product p) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_P_NAME, p.tenSanPham);
        v.put(DBHelper.COL_P_BRAND, p.hang);
        v.put(DBHelper.COL_P_PRICE, p.gia);
        v.put(DBHelper.COL_P_STOCK, p.tonKho);
        v.put(DBHelper.COL_P_DISCOUNT, p.giamGia);
        v.put(DBHelper.COL_P_DESC, p.moTa);
        v.put(DBHelper.COL_P_IMAGE, p.tenAnh);
        return v;
    }

    private Product docSanPham(Cursor c) {
        Product p = new Product();
        p.maSanPham = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_ID));
        p.tenSanPham = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_P_NAME));
        p.hang = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_P_BRAND));
        p.gia = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_P_PRICE));
        p.tonKho = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_P_STOCK));
        p.giamGia = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_P_DISCOUNT));
        p.moTa = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_P_DESC));
        p.tenAnh = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_P_IMAGE));
        return p;
    }
}