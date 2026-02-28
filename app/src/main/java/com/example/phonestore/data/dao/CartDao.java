package com.example.phonestore.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.phonestore.data.db.DBHelper;

public class CartDao {

    private final DBHelper dbHelper;

    public CartDao(Context ctx) {
        dbHelper = new DBHelper(ctx);
    }

    // Nếu sản phẩm đã có trong giỏ => cộng thêm số lượng
    // Nếu chưa có => insert mới
    public boolean addOrIncrease(long userId, long productId, int qtyAdd) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_C_QTY +
                        " FROM " + DBHelper.TBL_CART +
                        " WHERE " + DBHelper.COL_C_USER_ID + "=? AND " + DBHelper.COL_C_PRODUCT_ID + "=? LIMIT 1",
                new String[]{String.valueOf(userId), String.valueOf(productId)}
        );

        boolean exists = c.moveToFirst();
        int oldQty = exists ? c.getInt(0) : 0;
        c.close();

        if (exists) {
            ContentValues v = new ContentValues();
            v.put(DBHelper.COL_C_QTY, oldQty + qtyAdd);

            return db.update(
                    DBHelper.TBL_CART,
                    v,
                    DBHelper.COL_C_USER_ID + "=? AND " + DBHelper.COL_C_PRODUCT_ID + "=?",
                    new String[]{String.valueOf(userId), String.valueOf(productId)}
            ) > 0;
        } else {
            ContentValues v = new ContentValues();
            v.put(DBHelper.COL_C_USER_ID, userId);
            v.put(DBHelper.COL_C_PRODUCT_ID, productId);
            v.put(DBHelper.COL_C_QTY, qtyAdd);
            return db.insert(DBHelper.TBL_CART, null, v) != -1;
        }
    }
}