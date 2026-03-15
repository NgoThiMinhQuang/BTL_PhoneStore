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

    public boolean addOrIncrease(long userId, long productId, int qtyAdd) {
        return addOrIncrease(userId, productId, qtyAdd, "", "");
    }

    public boolean addOrIncrease(long userId, long productId, int qtyAdd, String storage, String color) {
        if (qtyAdd <= 0) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String normalizedStorage = normalizeVariantValue(storage);
        String normalizedColor = normalizeVariantValue(color);

        int stock = getStock(productId);
        if (stock <= 0) return false;

        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_ID + "," + DBHelper.COL_C_QTY +
                        " FROM " + DBHelper.TBL_CART +
                        " WHERE " + DBHelper.COL_C_USER_ID + "=? AND " + DBHelper.COL_C_PRODUCT_ID + "=? AND " +
                        DBHelper.COL_C_STORAGE + "=? AND " + DBHelper.COL_C_COLOR + "=? LIMIT 1",
                new String[]{
                        String.valueOf(userId),
                        String.valueOf(productId),
                        normalizedStorage,
                        normalizedColor
                }
        );

        boolean exists = c.moveToFirst();
        long cartItemId = exists ? c.getLong(0) : -1;
        int oldQty = exists ? c.getInt(1) : 0;
        c.close();

        int newQty = oldQty + qtyAdd;
        if (newQty > stock) return false;

        if (exists) {
            ContentValues v = new ContentValues();
            v.put(DBHelper.COL_C_QTY, newQty);
            return db.update(
                    DBHelper.TBL_CART,
                    v,
                    DBHelper.COL_ID + "=? AND " + DBHelper.COL_C_USER_ID + "=?",
                    new String[]{String.valueOf(cartItemId), String.valueOf(userId)}
            ) > 0;
        }

        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_C_USER_ID, userId);
        v.put(DBHelper.COL_C_PRODUCT_ID, productId);
        v.put(DBHelper.COL_C_QTY, qtyAdd);
        v.put(DBHelper.COL_C_STORAGE, normalizedStorage);
        v.put(DBHelper.COL_C_COLOR, normalizedColor);
        return db.insert(DBHelper.TBL_CART, null, v) != -1;
    }

    public ArrayList<CartItem> getCartItems(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<CartItem> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT c." + DBHelper.COL_ID + "," +
                        " p." + DBHelper.COL_ID + "," +
                        " p." + DBHelper.COL_P_NAME + "," +
                        " p." + DBHelper.COL_P_BRAND + "," +
                        " p." + DBHelper.COL_P_PRICE + "," +
                        " p." + DBHelper.COL_P_DISCOUNT + "," +
                        " p." + DBHelper.COL_P_STOCK + "," +
                        " p." + DBHelper.COL_P_IMAGE + "," +
                        " c." + DBHelper.COL_C_QTY + "," +
                        " c." + DBHelper.COL_C_STORAGE + "," +
                        " c." + DBHelper.COL_C_COLOR +
                        " FROM " + DBHelper.TBL_CART + " c" +
                        " JOIN " + DBHelper.TBL_PRODUCTS + " p ON p." + DBHelper.COL_ID + " = c." + DBHelper.COL_C_PRODUCT_ID +
                        " WHERE c." + DBHelper.COL_C_USER_ID + "=? AND p." + DBHelper.COL_IS_ACTIVE + "=1" +
                        " ORDER BY c." + DBHelper.COL_ID + " DESC",
                new String[]{String.valueOf(userId)}
        );

        while (c.moveToNext()) {
            list.add(readCartItem(c));
        }
        c.close();
        return list;
    }

    public int getTotal(long userId) {
        int total = 0;
        for (CartItem it : getCartItems(userId)) total += it.thanhTien();
        return total;
    }

    public ArrayList<CartItem> getCartItemsByIds(long userId, List<Long> cartItemIds) {
        ArrayList<CartItem> list = new ArrayList<>();
        if (cartItemIds == null || cartItemIds.isEmpty()) return list;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] args = buildArgs(userId, cartItemIds);

        Cursor c = db.rawQuery(
                "SELECT c." + DBHelper.COL_ID + "," +
                        " p." + DBHelper.COL_ID + "," +
                        " p." + DBHelper.COL_P_NAME + "," +
                        " p." + DBHelper.COL_P_BRAND + "," +
                        " p." + DBHelper.COL_P_PRICE + "," +
                        " p." + DBHelper.COL_P_DISCOUNT + "," +
                        " p." + DBHelper.COL_P_STOCK + "," +
                        " p." + DBHelper.COL_P_IMAGE + "," +
                        " c." + DBHelper.COL_C_QTY + "," +
                        " c." + DBHelper.COL_C_STORAGE + "," +
                        " c." + DBHelper.COL_C_COLOR +
                        " FROM " + DBHelper.TBL_CART + " c" +
                        " JOIN " + DBHelper.TBL_PRODUCTS + " p ON p." + DBHelper.COL_ID + " = c." + DBHelper.COL_C_PRODUCT_ID +
                        " WHERE c." + DBHelper.COL_C_USER_ID + "=? AND p." + DBHelper.COL_IS_ACTIVE + "=1" +
                        " AND c." + DBHelper.COL_ID + " IN (" + buildInClause(cartItemIds.size()) + ")" +
                        " ORDER BY c." + DBHelper.COL_ID + " DESC",
                args
        );

        while (c.moveToNext()) {
            list.add(readCartItem(c));
        }
        c.close();
        return list;
    }

    public int getTotalByIds(long userId, List<Long> cartItemIds) {
        int total = 0;
        for (CartItem it : getCartItemsByIds(userId, cartItemIds)) total += it.thanhTien();
        return total;
    }

    public void deleteItemsByIds(long userId, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String[] args = buildArgs(userId, cartItemIds);
        db.delete(
                DBHelper.TBL_CART,
                DBHelper.COL_C_USER_ID + "=? AND " + DBHelper.COL_ID + " IN (" + buildInClause(cartItemIds.size()) + ")",
                args
        );
    }

    public int getTotalQty(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COALESCE(SUM(c." + DBHelper.COL_C_QTY + "), 0)" +
                        " FROM " + DBHelper.TBL_CART + " c" +
                        " JOIN " + DBHelper.TBL_PRODUCTS + " p ON p." + DBHelper.COL_ID + " = c." + DBHelper.COL_C_PRODUCT_ID +
                        " WHERE c." + DBHelper.COL_C_USER_ID + "=? AND p." + DBHelper.COL_IS_ACTIVE + "=1",
                new String[]{String.valueOf(userId)}
        );
        int totalQty = 0;
        if (c.moveToFirst()) totalQty = c.getInt(0);
        c.close();
        return totalQty;
    }

    public boolean updateQtyById(long userId, long cartItemId, int newQty) {
        if (newQty <= 0) return deleteItemById(userId, cartItemId);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT c." + DBHelper.COL_C_PRODUCT_ID +
                        " FROM " + DBHelper.TBL_CART + " c" +
                        " JOIN " + DBHelper.TBL_PRODUCTS + " p ON p." + DBHelper.COL_ID + " = c." + DBHelper.COL_C_PRODUCT_ID +
                        " WHERE c." + DBHelper.COL_ID + "=? AND c." + DBHelper.COL_C_USER_ID + "=? AND p." + DBHelper.COL_IS_ACTIVE + "=1 LIMIT 1",
                new String[]{String.valueOf(cartItemId), String.valueOf(userId)}
        );
        long productId = -1;
        if (c.moveToFirst()) {
            productId = c.getLong(0);
        }
        c.close();
        if (productId <= 0) return false;

        int stock = getStock(productId);
        if (newQty > stock) return false;

        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_C_QTY, newQty);

        return writableDb.update(
                DBHelper.TBL_CART,
                v,
                DBHelper.COL_ID + "=? AND " + DBHelper.COL_C_USER_ID + "=?",
                new String[]{String.valueOf(cartItemId), String.valueOf(userId)}
        ) > 0;
    }

    public boolean deleteItemById(long userId, long cartItemId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                DBHelper.TBL_CART,
                DBHelper.COL_ID + "=? AND " + DBHelper.COL_C_USER_ID + "=?",
                new String[]{String.valueOf(cartItemId), String.valueOf(userId)}
        ) > 0;
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
                        " WHERE " + DBHelper.COL_ID + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1 LIMIT 1",
                new String[]{String.valueOf(productId)}
        );
        int stock = 0;
        if (c.moveToFirst()) stock = c.getInt(0);
        c.close();
        return stock;
    }

    private CartItem readCartItem(Cursor c) {
        CartItem it = new CartItem();
        it.id = c.getLong(0);
        it.productId = c.getLong(1);
        it.tenSanPham = c.getString(2);
        it.hang = c.getString(3);
        it.gia = c.getInt(4);
        it.giamGia = c.getInt(5);
        it.tonKho = c.getInt(6);
        it.tenAnh = c.getString(7);
        it.soLuong = c.getInt(8);
        it.dungLuong = c.getString(9);
        it.mauSac = c.getString(10);
        return it;
    }

    private String buildInClause(int size) {
        StringJoiner joiner = new StringJoiner(",");
        for (int i = 0; i < size; i++) joiner.add("?");
        return joiner.toString();
    }

    private String[] buildArgs(long userId, List<Long> ids) {
        String[] args = new String[ids.size() + 1];
        args[0] = String.valueOf(userId);
        for (int i = 0; i < ids.size(); i++) {
            args[i + 1] = String.valueOf(ids.get(i));
        }
        return args;
    }

    private String normalizeVariantValue(String value) {
        if (value == null) return "";
        return value.trim();
    }
}
