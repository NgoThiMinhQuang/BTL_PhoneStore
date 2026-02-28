package com.example.phonestore.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "phonestore.db";
    public static final int DB_VERSION = 4; // tăng để upgrade (trước đang là 3) :contentReference[oaicite:10]{index=10}

    // users
    public static final String TBL_USERS = "users";
    public static final String COL_ID = "id";
    public static final String COL_FULLNAME = "fullname";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";
    public static final String COL_ROLE = "role";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    // products
    public static final String TBL_PRODUCTS = "products";
    public static final String COL_P_NAME = "name";
    public static final String COL_P_BRAND = "brand";
    public static final String COL_P_PRICE = "price";
    public static final String COL_P_STOCK = "stock";
    public static final String COL_P_DISCOUNT = "discount";
    public static final String COL_P_DESC = "description";
    public static final String COL_P_IMAGE = "image_name"; // tên ảnh trong mipmap (tuỳ chọn)

    // cart_items (giỏ hàng)
    public static final String TBL_CART = "cart_items";
    public static final String COL_C_USER_ID = "user_id";
    public static final String COL_C_PRODUCT_ID = "product_id";
    public static final String COL_C_QTY = "qty";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // 1) users
        String sqlUsers = "CREATE TABLE " + TBL_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FULLNAME + " TEXT, " +
                COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                COL_PASSWORD + " TEXT NOT NULL, " +
                COL_ROLE + " TEXT NOT NULL" +
                ");";
        db.execSQL(sqlUsers);

        // Seed ADMIN
        ContentValues admin = new ContentValues();
        admin.put(COL_FULLNAME, "Administrator");
        admin.put(COL_USERNAME, "admin");
        admin.put(COL_PASSWORD, "admin123");
        admin.put(COL_ROLE, ROLE_ADMIN);
        db.insert(TBL_USERS, null, admin);

        // Seed CUSTOMER
        ContentValues customer = new ContentValues();
        customer.put(COL_FULLNAME, "Khachhang");
        customer.put(COL_USERNAME, "khachhang");
        customer.put(COL_PASSWORD, "khachhang123");
        customer.put(COL_ROLE, ROLE_CUSTOMER);
        db.insert(TBL_USERS, null, customer);

        // 2) products
        String sqlProducts = "CREATE TABLE " + TBL_PRODUCTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_P_NAME + " TEXT NOT NULL, " +
                COL_P_BRAND + " TEXT, " +
                COL_P_PRICE + " INTEGER NOT NULL, " +
                COL_P_STOCK + " INTEGER NOT NULL DEFAULT 0, " +
                COL_P_DISCOUNT + " INTEGER NOT NULL DEFAULT 0, " +
                COL_P_DESC + " TEXT, " +
                COL_P_IMAGE + " TEXT" +
                ");";
        db.execSQL(sqlProducts);

        // Seed sản phẩm mẫu (để test danh sách)
        themSanPhamMau(db, "iPhone 15 Pro Max", "Apple", 28990000, 10, 10,
                "Flagship, pin trau, camera manh", null);
        themSanPhamMau(db, "Samsung S24 Ultra", "Samsung", 24490000, 8, 20,
                "Zoom xa, man dep", null);
        themSanPhamMau(db, "Xiaomi 14", "Xiaomi", 15990000, 15, 0,
                "Hieu nang/gia tot", null);

        // 3) cart_items
        String sqlCart = "CREATE TABLE " + TBL_CART + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_C_USER_ID + " INTEGER NOT NULL, " +
                COL_C_PRODUCT_ID + " INTEGER NOT NULL, " +
                COL_C_QTY + " INTEGER NOT NULL DEFAULT 1, " +
                "UNIQUE(" + COL_C_USER_ID + "," + COL_C_PRODUCT_ID + ")" +
                ");";
        db.execSQL(sqlCart);
    }

    // Hàm seed (tham số tiếng Việt không dấu)
    private void themSanPhamMau(SQLiteDatabase db,
                                String tenSanPham,
                                String hang,
                                int gia,
                                int tonKho,
                                int giamGia,
                                String moTa,
                                String tenAnh) {

        ContentValues giaTri = new ContentValues();
        giaTri.put(COL_P_NAME, tenSanPham);
        giaTri.put(COL_P_BRAND, hang);
        giaTri.put(COL_P_PRICE, gia);
        giaTri.put(COL_P_STOCK, tonKho);
        giaTri.put(COL_P_DISCOUNT, giamGia);
        giaTri.put(COL_P_DESC, moTa);
        giaTri.put(COL_P_IMAGE, tenAnh);

        db.insert(TBL_PRODUCTS, null, giaTri);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop theo thứ tự để chắc chắn
        db.execSQL("DROP TABLE IF EXISTS " + TBL_CART);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_USERS);
        onCreate(db);
    }
}