package com.example.phonestore.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.CartItem;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class CartDao {

    private final DBHelper dbHelper;

    public CartDao(Context ctx) {
        dbHelper = new DBHelper(ctx);
    }

    // thêm vào giỏ, có check tồn kho
    public boolean addOrIncrease(long userId, long productId, int qtyAdd) {
        if (qtyAdd <= 0) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int stock = getStock(productId);
        if (stock <= 0) return false;

        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_C_QTY +
                        " FROM " + DBHelper.TBL_CART +
                        " WHERE " + DBHelper.COL_C_USER_ID + "=? AND " + DBHelper.COL_C_PRODUCT_ID + "=? LIMIT 1",
                new String[]{String.valueOf(userId), String.valueOf(productId)}
        );

        boolean exists = c.moveToFirst();
        int oldQty = exists ? c.getInt(0) : 0;
        c.close();

        int newQty = oldQty + qtyAdd;
        if (newQty > stock) return false;

        if (exists) {
            ContentValues v = new ContentValues();
            v.put(DBHelper.COL_C_QTY, newQty);
            return db.update(DBHelper.TBL_CART, v,
                    DBHelper.COL_C_USER_ID + "=? AND " + DBHelper.COL_C_PRODUCT_ID + "=?",
                    new String[]{String.valueOf(userId), String.valueOf(productId)}) > 0;
        } else {
            ContentValues v = new ContentValues();
            v.put(DBHelper.COL_C_USER_ID, userId);
            v.put(DBHelper.COL_C_PRODUCT_ID, productId);
            v.put(DBHelper.COL_C_QTY, qtyAdd);
            return db.insert(DBHelper.TBL_CART, null, v) != -1;
        }
    }

    public ArrayList<CartItem> getCartItems(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<CartItem> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT p." + DBHelper.COL_ID + "," +
                        " p." + DBHelper.COL_P_NAME + "," +
                        " p." + DBHelper.COL_P_BRAND + "," +
                        " p." + DBHelper.COL_P_PRICE + "," +
                        " p." + DBHelper.COL_P_DISCOUNT + "," +
                        " p." + DBHelper.COL_P_STOCK + "," +
                        " p." + DBHelper.COL_P_IMAGE + "," +
                        " c." + DBHelper.COL_C_QTY +
                        " FROM " + DBHelper.TBL_CART + " c" +
                        " JOIN " + DBHelper.TBL_PRODUCTS + " p ON p." + DBHelper.COL_ID + " = c." + DBHelper.COL_C_PRODUCT_ID +
                        " WHERE c." + DBHelper.COL_C_USER_ID + "=? " +
                        " ORDER BY c." + DBHelper.COL_ID + " DESC",
                new String[]{String.valueOf(userId)}
        );

        while (c.moveToNext()) {
            CartItem it = new CartItem();
            it.productId = c.getLong(0);
            it.tenSanPham = c.getString(1);
            it.hang = c.getString(2);
            it.gia = c.getInt(3);
            it.giamGia = c.getInt(4);
            it.tonKho = c.getInt(5);
            it.tenAnh = c.getString(6);
            it.soLuong = c.getInt(7);
            list.add(it);
        }
        c.close();
        return list;
    }

    public int getTotal(long userId) {
        int total = 0;
        for (CartItem it : getCartItems(userId)) total += it.thanhTien();
        return total;
    }

    public ArrayList<CartItem> getCartItemsByProductIds(long userId, List<Long> productIds) {
        ArrayList<CartItem> list = new ArrayList<>();
        if (productIds == null || productIds.isEmpty()) return list;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] args = buildArgs(userId, productIds);

        Cursor c = db.rawQuery(
                "SELECT p." + DBHelper.COL_ID + "," +
                        " p." + DBHelper.COL_P_NAME + "," +
                        " p." + DBHelper.COL_P_BRAND + "," +
                        " p." + DBHelper.COL_P_PRICE + "," +
                        " p." + DBHelper.COL_P_DISCOUNT + "," +
                        " p." + DBHelper.COL_P_STOCK + "," +
                        " p." + DBHelper.COL_P_IMAGE + "," +
                        " c." + DBHelper.COL_C_QTY +
                        " FROM " + DBHelper.TBL_CART + " c" +
                        " JOIN " + DBHelper.TBL_PRODUCTS + " p ON p." + DBHelper.COL_ID + " = c." + DBHelper.COL_C_PRODUCT_ID +
                        " WHERE c." + DBHelper.COL_C_USER_ID + "=?" +
                        " AND c." + DBHelper.COL_C_PRODUCT_ID + " IN (" + buildInClause(productIds.size()) + ")" +
                        " ORDER BY c." + DBHelper.COL_ID + " DESC",
                args
        );

        while (c.moveToNext()) {
            CartItem it = new CartItem();
            it.productId = c.getLong(0);
            it.tenSanPham = c.getString(1);
            it.hang = c.getString(2);
            it.gia = c.getInt(3);
            it.giamGia = c.getInt(4);
            it.tonKho = c.getInt(5);
            it.tenAnh = c.getString(6);
            it.soLuong = c.getInt(7);
            list.add(it);
        }
        c.close();
        return list;
    }

    public int getTotalByProductIds(long userId, List<Long> productIds) {
        int total = 0;
        for (CartItem it : getCartItemsByProductIds(userId, productIds)) total += it.thanhTien();
        return total;
    }

    public void deleteItems(long userId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String[] args = buildArgs(userId, productIds);
        db.delete(
                DBHelper.TBL_CART,
                DBHelper.COL_C_USER_ID + "=? AND " + DBHelper.COL_C_PRODUCT_ID + " IN (" + buildInClause(productIds.size()) + ")",
                args
        );
    }

    public int getTotalQty(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COALESCE(SUM(" + DBHelper.COL_C_QTY + "), 0)" +
                        " FROM " + DBHelper.TBL_CART +
                        " WHERE " + DBHelper.COL_C_USER_ID + "=?",
                new String[]{String.valueOf(userId)}
        );
        int totalQty = 0;
        if (c.moveToFirst()) totalQty = c.getInt(0);
        c.close();
        return totalQty;
    }

    public boolean updateQty(long userId, long productId, int newQty) {
        if (newQty <= 0) return deleteItem(userId, productId);

        int stock = getStock(productId);
        if (newQty > stock) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_C_QTY, newQty);

        return db.update(DBHelper.TBL_CART, v,
                DBHelper.COL_C_USER_ID + "=? AND " + DBHelper.COL_C_PRODUCT_ID + "=?",
                new String[]{String.valueOf(userId), String.valueOf(productId)}) > 0;
    }

    public boolean deleteItem(long userId, long productId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DBHelper.TBL_CART,
                DBHelper.COL_C_USER_ID + "=? AND " + DBHelper.COL_C_PRODUCT_ID + "=?",
                new String[]{String.valueOf(userId), String.valueOf(productId)}) > 0;
    }

    public void clear(long userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DBHelper.TBL_CART, DBHelper.COL_C_USER_ID + "=?",
                new String[]{String.valueOf(userId)});
    }

    private int getStock(long productId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_P_STOCK +
                        " FROM " + DBHelper.TBL_PRODUCTS +
                        " WHERE " + DBHelper.COL_ID + "=? LIMIT 1",
                new String[]{String.valueOf(productId)}
        );
        int stock = 0;
        if (c.moveToFirst()) stock = c.getInt(0);
        c.close();
        return stock;
    }

    private String buildInClause(int size) {
        StringJoiner joiner = new StringJoiner(",");
        for (int i = 0; i < size; i++) joiner.add("?");
        return joiner.toString();
    }

    private String[] buildArgs(long userId, List<Long> productIds) {
        String[] args = new String[productIds.size() + 1];
        args[0] = String.valueOf(userId);
        for (int i = 0; i < productIds.size(); i++) {
            args[i + 1] = String.valueOf(productIds.get(i));
        }
        return args;
    }
}