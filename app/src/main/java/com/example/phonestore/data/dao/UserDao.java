package com.example.phonestore.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.phonestore.data.db.DBHelper;
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

        return db.insert(DBHelper.TBL_USERS, null, v) != -1;
    }

    public User login(String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_ID + "," + DBHelper.COL_FULLNAME + "," +
                        DBHelper.COL_USERNAME + "," + DBHelper.COL_ROLE +
                        " FROM " + DBHelper.TBL_USERS +
                        " WHERE " + DBHelper.COL_USERNAME + "=? AND " + DBHelper.COL_PASSWORD + "=? LIMIT 1",
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
                        DBHelper.COL_USERNAME + "," + DBHelper.COL_ROLE +
                        " FROM " + DBHelper.TBL_USERS +
                        " WHERE " + DBHelper.COL_ID + "=? LIMIT 1",
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
        return db.update(DBHelper.TBL_USERS, v, DBHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)}) > 0;
    }

    // Đổi mật khẩu: bắt nhập đúng mật khẩu cũ
    public boolean changePassword(long id, String oldPassword, String newPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_PASSWORD, newPassword);
        return db.update(
                DBHelper.TBL_USERS,
                v,
                DBHelper.COL_ID + "=? AND " + DBHelper.COL_PASSWORD + "=?",
                new String[]{String.valueOf(id), oldPassword}
        ) > 0;
    }

    public ArrayList<User> getCustomers(String keyword) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<User> list = new ArrayList<>();

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        Cursor c;

        if (hasKeyword) {
            String k = "%" + keyword.trim() + "%";
            c = db.rawQuery(
                    "SELECT " + DBHelper.COL_ID + "," + DBHelper.COL_FULLNAME + "," +
                            DBHelper.COL_USERNAME + "," + DBHelper.COL_ROLE +
                            " FROM " + DBHelper.TBL_USERS +
                            " WHERE " + DBHelper.COL_ROLE + "=?" +
                            " AND (IFNULL(" + DBHelper.COL_FULLNAME + ",'') LIKE ? OR " + DBHelper.COL_USERNAME + " LIKE ?)" +
                            " ORDER BY " + DBHelper.COL_ID + " DESC",
                    new String[]{DBHelper.ROLE_CUSTOMER, k, k}
            );
        } else {
            c = db.rawQuery(
                    "SELECT " + DBHelper.COL_ID + "," + DBHelper.COL_FULLNAME + "," +
                            DBHelper.COL_USERNAME + "," + DBHelper.COL_ROLE +
                            " FROM " + DBHelper.TBL_USERS +
                            " WHERE " + DBHelper.COL_ROLE + "=?" +
                            " ORDER BY " + DBHelper.COL_ID + " DESC",
                    new String[]{DBHelper.ROLE_CUSTOMER}
            );
        }

        while (c.moveToNext()) {
            list.add(docNguoiDung(c));
        }
        c.close();
        return list;
    }

    public int getSoKhachHang() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + DBHelper.TBL_USERS +
                        " WHERE " + DBHelper.COL_ROLE + "=?",
                new String[]{DBHelper.ROLE_CUSTOMER}
        );

        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    private User docNguoiDung(Cursor c) {
        return new User(
                c.getLong(0),
                c.getString(1),
                c.getString(2),
                c.getString(3)
        );
    }
    // NEW: Admin update customer (không cần mật khẩu cũ)
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
                DBHelper.COL_ID + "=? AND " + DBHelper.COL_ROLE + "=?",
                new String[]{String.valueOf(id), DBHelper.ROLE_CUSTOMER}
        ) > 0;
    }

    // NEW: Admin delete customer
    public boolean adminDeleteCustomer(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                DBHelper.TBL_USERS,
                DBHelper.COL_ID + "=? AND " + DBHelper.COL_ROLE + "=?",
                new String[]{String.valueOf(id), DBHelper.ROLE_CUSTOMER}
        ) > 0;
    }
}
