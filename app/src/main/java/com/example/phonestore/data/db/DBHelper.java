package com.example.phonestore.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "phonestore.db";
    public static final int DB_VERSION = 2;

    public static final String TBL_USERS = "users";
    public static final String COL_ID = "id";
    public static final String COL_FULLNAME = "fullname";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";
    public static final String COL_ROLE = "role";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sqlUsers = "CREATE TABLE " + TBL_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FULLNAME + " TEXT, " +
                COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                COL_PASSWORD + " TEXT NOT NULL, " +
                COL_ROLE + " TEXT NOT NULL" +
                ");";
        db.execSQL(sqlUsers);

        // Seed ADMIN mặc định
        ContentValues admin = new ContentValues();
        admin.put(COL_FULLNAME, "Administrator");
        admin.put(COL_USERNAME, "admin");
        admin.put(COL_PASSWORD, "admin123");
        admin.put(COL_ROLE, ROLE_ADMIN);
        db.insert(TBL_USERS, null, admin);

        // Seed CUSTOMER mặc định
        ContentValues customer = new ContentValues();
        customer.put(COL_FULLNAME, "Khachhang");
        customer.put(COL_USERNAME, "khachhang");
        customer.put(COL_PASSWORD, "khachhang123");
        customer.put(COL_ROLE, ROLE_CUSTOMER);
        db.insert(TBL_USERS, null, customer);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TBL_USERS);
        onCreate(db);
    }
}