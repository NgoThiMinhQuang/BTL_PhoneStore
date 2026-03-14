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
        return queryProducts(DBHelper.COL_IS_ACTIVE + "=1", null);
    }

    public ArrayList<Product> layTatCaChoAdmin() {
        return queryProducts(null, null);
    }

    public ArrayList<Product> layTheoHang(String hang) {
        return queryProducts(DBHelper.COL_IS_ACTIVE + "=1 AND " + DBHelper.COL_P_BRAND + "=?", new String[]{hang});
    }

    public ArrayList<Product> timKiem(String tuKhoa) {
        String k = "%" + tuKhoa + "%";
        return queryProducts(DBHelper.COL_IS_ACTIVE + "=1 AND (" + DBHelper.COL_P_NAME + " LIKE ? OR " + DBHelper.COL_P_BRAND + " LIKE ?)", new String[]{k, k});
    }

    public ArrayList<Product> timKiemChoAdmin(String tuKhoa) {
        String k = "%" + tuKhoa + "%";
        return queryProducts("(" + DBHelper.COL_P_NAME + " LIKE ? OR " + DBHelper.COL_P_BRAND + " LIKE ?)", new String[]{k, k});
    }

    public ArrayList<Product> timKiemTheoHang(String hang, String tuKhoa) {
        String k = "%" + tuKhoa + "%";
        return queryProducts(DBHelper.COL_IS_ACTIVE + "=1 AND " + DBHelper.COL_P_BRAND + "=? AND " + DBHelper.COL_P_NAME + " LIKE ?", new String[]{hang, k});
    }

    public Product getById(long id) {
        return getById(id, false);
    }

    public Product getById(long id, boolean includeInactive) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT * FROM " + DBHelper.TBL_PRODUCTS + " WHERE " + DBHelper.COL_ID + "=?";
        if (!includeInactive) {
            sql += " AND " + DBHelper.COL_IS_ACTIVE + "=1";
        }
        sql += " LIMIT 1";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(id)});
        Product p = null;
        if (c.moveToFirst()) p = docSanPham(c);
        c.close();
        return p;
    }

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
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_IS_ACTIVE, 0);
        return db.update(DBHelper.TBL_PRODUCTS, v,
                DBHelper.COL_ID + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1",
                new String[]{String.valueOf(id)}) > 0;
    }

    public boolean giamTonKho(SQLiteDatabase db, long productId, int qty) {
        db.execSQL(
                "UPDATE " + DBHelper.TBL_PRODUCTS +
                        " SET " + DBHelper.COL_P_STOCK + " = " + DBHelper.COL_P_STOCK + " - ?" +
                        " WHERE " + DBHelper.COL_ID + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1 AND " + DBHelper.COL_P_STOCK + " >= ?",
                new Object[]{qty, productId, qty}
        );
        return true;
    }

    private ArrayList<Product> queryProducts(String whereClause, String[] args) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM " + DBHelper.TBL_PRODUCTS;
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        sql += " ORDER BY " + DBHelper.COL_ID + " DESC";
        Cursor c = db.rawQuery(sql, args);
        while (c.moveToNext()) list.add(docSanPham(c));
        c.close();
        return list;
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
        v.put(DBHelper.COL_IS_ACTIVE, p.isActive ? 1 : 0);
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
        int activeIndex = c.getColumnIndex(DBHelper.COL_IS_ACTIVE);
        p.isActive = activeIndex < 0 || c.getInt(activeIndex) == 1;
        return p;
    }
}
