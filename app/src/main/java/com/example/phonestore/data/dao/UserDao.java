package com.example.phonestore.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.OrderStatus;
import com.example.phonestore.data.model.User;

import java.util.ArrayList;

public class UserDao {

    private final DBHelper dbHelper;

    public UserDao(Context ctx) {
        dbHelper = new DBHelper(ctx);
    }

    public boolean registerCustomer(String fullname, String username, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.rawQuery(
                "SELECT 1 FROM " + DBHelper.TBL_USERS + " WHERE " + DBHelper.COL_USERNAME + "=? LIMIT 1",
                new String[]{username}
        );
        boolean exists = c.moveToFirst();
        c.close();
        if (exists) return false;

        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_FULLNAME, fullname);
        v.put(DBHelper.COL_USERNAME, username);
        v.put(DBHelper.COL_PASSWORD, password);
        v.put(DBHelper.COL_ROLE, DBHelper.ROLE_CUSTOMER);
        v.put(DBHelper.COL_IS_ACTIVE, 1);

        return db.insert(DBHelper.TBL_USERS, null, v) != -1;
    }

    public User login(String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_ID + "," + DBHelper.COL_FULLNAME + "," +
                        DBHelper.COL_USERNAME + "," + DBHelper.COL_ROLE + "," + DBHelper.COL_IS_ACTIVE +
                        " FROM " + DBHelper.TBL_USERS +
                        " WHERE " + DBHelper.COL_USERNAME + "=? AND " + DBHelper.COL_PASSWORD + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1 LIMIT 1",
                new String[]{username, password}
        );

        User u = null;
        if (c.moveToFirst()) {
            u = docNguoiDung(c);
        }
        c.close();
        return u;
    }

    public User getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_ID + "," + DBHelper.COL_FULLNAME + "," +
                        DBHelper.COL_USERNAME + "," + DBHelper.COL_ROLE + "," + DBHelper.COL_IS_ACTIVE +
                        " FROM " + DBHelper.TBL_USERS +
                        " WHERE " + DBHelper.COL_ID + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1 LIMIT 1",
                new String[]{String.valueOf(id)}
        );

        User u = null;
        if (c.moveToFirst()) {
            u = docNguoiDung(c);
        }
        c.close();
        return u;
    }

    public boolean updateFullName(long id, String fullname) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_FULLNAME, fullname);
        return db.update(DBHelper.TBL_USERS, v, DBHelper.COL_ID + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1",
                new String[]{String.valueOf(id)}) > 0;
    }

    public boolean changePassword(long id, String oldPassword, String newPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_PASSWORD, newPassword);
        return db.update(
                DBHelper.TBL_USERS,
                v,
                DBHelper.COL_ID + "=? AND " + DBHelper.COL_PASSWORD + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1",
                new String[]{String.valueOf(id), oldPassword}
        ) > 0;
    }

    public ArrayList<User> getCustomers(String keyword) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<User> list = new ArrayList<>();
        Cursor c = db.rawQuery(buildCustomersQuery(keyword), buildCustomersArgs(keyword));
        while (c.moveToNext()) {
            list.add(docNguoiDungThongKe(c));
        }
        c.close();
        return list;
    }

    public int getSoKhachHang() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + DBHelper.TBL_USERS +
                        " WHERE " + DBHelper.COL_ROLE + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1",
                new String[]{DBHelper.ROLE_CUSTOMER}
        );

        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public boolean adminUpdateCustomer(long id, String fullname, String newPasswordOrNull) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_FULLNAME, fullname);

        if (newPasswordOrNull != null && !newPasswordOrNull.trim().isEmpty()) {
            v.put(DBHelper.COL_PASSWORD, newPasswordOrNull.trim());
        }

        return db.update(
                DBHelper.TBL_USERS,
                v,
                DBHelper.COL_ID + "=? AND " + DBHelper.COL_ROLE + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1",
                new String[]{String.valueOf(id), DBHelper.ROLE_CUSTOMER}
        ) > 0;
    }

    public boolean adminDeleteCustomer(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_IS_ACTIVE, 0);
        return db.update(
                DBHelper.TBL_USERS,
                v,
                DBHelper.COL_ID + "=? AND " + DBHelper.COL_ROLE + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1",
                new String[]{String.valueOf(id), DBHelper.ROLE_CUSTOMER}
        ) > 0;
    }

    private String buildCustomersQuery(String keyword) {
        String sql = "SELECT u." + DBHelper.COL_ID + "," +
                " u." + DBHelper.COL_FULLNAME + "," +
                " u." + DBHelper.COL_USERNAME + "," +
                " u." + DBHelper.COL_ROLE + "," +
                " u." + DBHelper.COL_IS_ACTIVE + "," +
                " COUNT(CASE WHEN o." + DBHelper.COL_ID + " IS NOT NULL THEN 1 END) AS order_count," +
                " COALESCE(SUM(CASE WHEN o." + DBHelper.COL_O_ORDER_STATUS + "='" + OrderStatus.STATUS_DA_GIAO + "' THEN o." + DBHelper.COL_O_TOTAL + " ELSE 0 END), 0) AS delivered_spend" +
                " FROM " + DBHelper.TBL_USERS + " u" +
                " LEFT JOIN " + DBHelper.TBL_ORDERS + " o ON o." + DBHelper.COL_O_USER_ID + " = u." + DBHelper.COL_ID +
                " WHERE u." + DBHelper.COL_ROLE + "=? AND u." + DBHelper.COL_IS_ACTIVE + "=1";

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql += " AND (IFNULL(u." + DBHelper.COL_FULLNAME + ",'') LIKE ? OR u." + DBHelper.COL_USERNAME + " LIKE ?)";
        }

        sql += " GROUP BY u." + DBHelper.COL_ID + ", u." + DBHelper.COL_FULLNAME + ", u." + DBHelper.COL_USERNAME + ", u." + DBHelper.COL_ROLE + ", u." + DBHelper.COL_IS_ACTIVE +
                " ORDER BY u." + DBHelper.COL_ID + " DESC";
        return sql;
    }

    private String[] buildCustomersArgs(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new String[]{DBHelper.ROLE_CUSTOMER};
        }
        String k = "%" + keyword.trim() + "%";
        return new String[]{DBHelper.ROLE_CUSTOMER, k, k};
    }

    private User docNguoiDung(Cursor c) {
        User user = new User(
                c.getLong(0),
                c.getString(1),
                c.getString(2),
                c.getString(3)
        );
        if (c.getColumnCount() > 4) {
            user.isActive = c.getInt(4) == 1;
        }
        return user;
    }

    private User docNguoiDungThongKe(Cursor c) {
        User user = docNguoiDung(c);
        int orderCountIndex = c.getColumnIndex("order_count");
        if (orderCountIndex >= 0) {
            user.orderCount = c.getInt(orderCountIndex);
        }
        int deliveredSpendIndex = c.getColumnIndex("delivered_spend");
        if (deliveredSpendIndex >= 0) {
            user.deliveredSpend = c.getInt(deliveredSpendIndex);
        }
        return user;
    }
}
