package com.example.phonestore.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "phonestore.db";
    public static final int DB_VERSION = 7; // tăng version để upgrade DB

    // ===== USERS (SQLite: tiếng Việt không dấu) =====
    public static final String TBL_USERS = "nguoi_dung";
    public static final String COL_ID = "id";
    public static final String COL_FULLNAME = "ho_ten";
    public static final String COL_USERNAME = "ten_dang_nhap";
    public static final String COL_PASSWORD = "mat_khau";
    public static final String COL_ROLE = "vai_tro";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    // ===== PRODUCTS =====
    public static final String TBL_PRODUCTS = "san_pham";
    public static final String COL_P_NAME = "ten_san_pham";
    public static final String COL_P_BRAND = "hang";
    public static final String COL_P_PRICE = "gia";
    public static final String COL_P_STOCK = "ton_kho";
    public static final String COL_P_DISCOUNT = "giam_gia";
    public static final String COL_P_DESC = "mo_ta";
    public static final String COL_P_IMAGE = "ten_anh";

    // ===== CART =====
    public static final String TBL_CART = "gio_hang";
    public static final String COL_C_USER_ID = "nguoi_dung_id";
    public static final String COL_C_PRODUCT_ID = "san_pham_id";
    public static final String COL_C_QTY = "so_luong";

    // ===== ORDERS (hoa_don) =====
    public static final String TBL_ORDERS = "hoa_don";
    public static final String COL_O_USER_ID = "nguoi_dung_id";
    public static final String COL_O_TOTAL = "tong_tien";
    public static final String COL_O_CREATED = "ngay_tao";
    public static final String COL_O_STATUS = "trang_thai";

    // (NEW) SQLite cột tiếng Việt không dấu
    public static final String COL_O_RECEIVER = "nguoi_nhan";
    public static final String COL_O_PHONE = "sdt_nhan";
    public static final String COL_O_ADDRESS = "dia_chi_nhan";
    public static final String COL_O_PAY_METHOD = "phuong_thuc_thanh_toan";
    public static final String COL_O_NOTE = "ghi_chu";

    public static final String TBL_ORDER_ITEMS = "hoa_don_ct";
    public static final String COL_OI_ORDER_ID = "hoa_don_id";
    public static final String COL_OI_PRODUCT_ID = "san_pham_id";
    public static final String COL_OI_NAME = "ten_san_pham";
    public static final String COL_OI_PRICE = "don_gia";
    public static final String COL_OI_DISCOUNT = "giam_gia";
    public static final String COL_OI_QTY = "so_luong";
    public static final String COL_OI_AMOUNT = "thanh_tien";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // 1) người dùng
        db.execSQL("CREATE TABLE " + TBL_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FULLNAME + " TEXT, " +
                COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                COL_PASSWORD + " TEXT NOT NULL, " +
                COL_ROLE + " TEXT NOT NULL" +
                ");");

        // seed ADMIN
        ContentValues admin = new ContentValues();
        admin.put(COL_FULLNAME, "Administrator");
        admin.put(COL_USERNAME, "admin");
        admin.put(COL_PASSWORD, "admin123");
        admin.put(COL_ROLE, ROLE_ADMIN);
        db.insert(TBL_USERS, null, admin);

        // seed CUSTOMER
        ContentValues customer = new ContentValues();
        customer.put(COL_FULLNAME, "Khachhang");
        customer.put(COL_USERNAME, "khachhang");
        customer.put(COL_PASSWORD, "khachhang123");
        customer.put(COL_ROLE, ROLE_CUSTOMER);
        db.insert(TBL_USERS, null, customer);

        // 2) sản phẩm
        db.execSQL("CREATE TABLE " + TBL_PRODUCTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_P_NAME + " TEXT NOT NULL, " +
                COL_P_BRAND + " TEXT, " +
                COL_P_PRICE + " INTEGER NOT NULL, " +
                COL_P_STOCK + " INTEGER NOT NULL DEFAULT 0, " +
                COL_P_DISCOUNT + " INTEGER NOT NULL DEFAULT 0, " +
                COL_P_DESC + " TEXT, " +
                COL_P_IMAGE + " TEXT" +
                ");");

        // seed sản phẩm mẫu
        seedProduct(db, "iPhone 15 Pro Max", "Apple", 28990000, 10, 10, "Flagship, pin trâu, camera mạnh", "ip_15");
        seedProduct(db, "Samsung S24 Ultra", "Samsung", 24490000, 8, 20, "Zoom xa, màn đẹp", "ss_s24_utra");
        seedProduct(db, "Xiaomi 14", "Xiaomi", 15990000, 15, 0, "Hiệu năng/giá tốt", "ic_iphone15");

        // 3) giỏ hàng
        db.execSQL("CREATE TABLE " + TBL_CART + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_C_USER_ID + " INTEGER NOT NULL, " +
                COL_C_PRODUCT_ID + " INTEGER NOT NULL, " +
                COL_C_QTY + " INTEGER NOT NULL DEFAULT 1, " +
                "UNIQUE(" + COL_C_USER_ID + "," + COL_C_PRODUCT_ID + "), " +
                "FOREIGN KEY(" + COL_C_USER_ID + ") REFERENCES " + TBL_USERS + "(" + COL_ID + "), " +
                "FOREIGN KEY(" + COL_C_PRODUCT_ID + ") REFERENCES " + TBL_PRODUCTS + "(" + COL_ID + ")" +
                ");");

        // 4) hóa đơn (NEW: thêm cột nhận hàng + thanh toán)
        db.execSQL("CREATE TABLE " + TBL_ORDERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_O_USER_ID + " INTEGER NOT NULL, " +
                COL_O_TOTAL + " INTEGER NOT NULL, " +
                COL_O_CREATED + " INTEGER NOT NULL, " +
                COL_O_STATUS + " TEXT NOT NULL, " +
                COL_O_RECEIVER + " TEXT, " +
                COL_O_PHONE + " TEXT, " +
                COL_O_ADDRESS + " TEXT, " +
                COL_O_PAY_METHOD + " TEXT, " +
                COL_O_NOTE + " TEXT, " +
                "FOREIGN KEY(" + COL_O_USER_ID + ") REFERENCES " + TBL_USERS + "(" + COL_ID + ")" +
                ");");

        // 5) hóa đơn chi tiết
        db.execSQL("CREATE TABLE " + TBL_ORDER_ITEMS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_OI_ORDER_ID + " INTEGER NOT NULL, " +
                COL_OI_PRODUCT_ID + " INTEGER NOT NULL, " +
                COL_OI_NAME + " TEXT NOT NULL, " +
                COL_OI_PRICE + " INTEGER NOT NULL, " +
                COL_OI_DISCOUNT + " INTEGER NOT NULL DEFAULT 0, " +
                COL_OI_QTY + " INTEGER NOT NULL, " +
                COL_OI_AMOUNT + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + COL_OI_ORDER_ID + ") REFERENCES " + TBL_ORDERS + "(" + COL_ID + ") ON DELETE CASCADE" +
                ");");
    }

    private void seedProduct(SQLiteDatabase db,
                             String tenSanPham, String hang, int gia,
                             int tonKho, int giamGia, String moTa, String tenAnh) {
        ContentValues v = new ContentValues();
        v.put(COL_P_NAME, tenSanPham);
        v.put(COL_P_BRAND, hang);
        v.put(COL_P_PRICE, gia);
        v.put(COL_P_STOCK, tonKho);
        v.put(COL_P_DISCOUNT, giamGia);
        v.put(COL_P_DESC, moTa);
        v.put(COL_P_IMAGE, tenAnh);
        db.insert(TBL_PRODUCTS, null, v);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TBL_ORDER_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_ORDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_CART);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_USERS);
        onCreate(db);
    }
}