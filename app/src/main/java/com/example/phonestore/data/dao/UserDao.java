package com.example.phonestore.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.User;

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
            u = new User(
                    c.getLong(0),
                    c.getString(1),
                    c.getString(2),
                    c.getString(3)
            );
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
            u = new User(
                    c.getLong(0),
                    c.getString(1),
                    c.getString(2),
                    c.getString(3)
            );
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
}