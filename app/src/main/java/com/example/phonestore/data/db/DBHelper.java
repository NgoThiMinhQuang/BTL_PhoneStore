package com.example.phonestore.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.phonestore.data.model.Receipt;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "phonestore.db";
    public static final int DB_VERSION = 10;

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

    // ===== SUPPLIERS / RECEIPTS / INVENTORY HISTORY =====
    public static final String TBL_SUPPLIERS = "nha_cung_cap";
    public static final String COL_S_NAME = "ten_nha_cung_cap";
    public static final String COL_S_BRAND = "hang";
    public static final String COL_S_PHONE = "so_dien_thoai";
    public static final String COL_S_ADDRESS = "dia_chi";

    public static final String TBL_RECEIPTS = "phieu_nhap";
    public static final String COL_R_SUPPLIER_ID = "nha_cung_cap_id";
    public static final String COL_R_TOTAL_QTY = "tong_so_luong";
    public static final String COL_R_TOTAL_AMOUNT = "tong_tien";
    public static final String COL_R_CREATED = "ngay_tao";
    public static final String COL_R_NOTE = "ghi_chu";
    public static final String COL_R_STATUS = "trang_thai";
    public static final String COL_R_CREATED_BY = "nguoi_tao";

    public static final String TBL_RECEIPT_ITEMS = "phieu_nhap_ct";
    public static final String COL_RI_RECEIPT_ID = "phieu_nhap_id";
    public static final String COL_RI_PRODUCT_ID = "san_pham_id";
    public static final String COL_RI_PRODUCT_NAME = "ten_san_pham";
    public static final String COL_RI_QTY = "so_luong";
    public static final String COL_RI_UNIT_COST = "don_gia_nhap";
    public static final String COL_RI_AMOUNT = "thanh_tien";

    public static final String TBL_INVENTORY_HISTORY = "lich_su_kho";
    public static final String COL_H_PRODUCT_ID = "san_pham_id";
    public static final String COL_H_PRODUCT_NAME = "ten_san_pham";
    public static final String COL_H_ACTION = "loai_bien_dong";
    public static final String COL_H_QTY = "so_luong";
    public static final String COL_H_REF_TYPE = "loai_tham_chieu";
    public static final String COL_H_REF_ID = "tham_chieu_id";
    public static final String COL_H_NOTE = "ghi_chu";
    public static final String COL_H_CREATED = "ngay_tao";

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
        long iphone15Id = seedProduct(db, "iPhone 15 Pro Max", "Apple", 28990000, 10, 10, "Flagship, pin trâu, camera mạnh", "ip_15");
        long samsungS24Id = seedProduct(db, "Samsung S24 Ultra", "Samsung", 24490000, 8, 20, "Zoom xa, màn đẹp", "ss_s24_utra");
        long xiaomi14Id = seedProduct(db, "Xiaomi 14", "Xiaomi", 15990000, 15, 0, "Hiệu năng/giá tốt", "ic_iphone15");

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

        // 6) nhà cung cấp
        db.execSQL("CREATE TABLE " + TBL_SUPPLIERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_S_NAME + " TEXT NOT NULL, " +
                COL_S_BRAND + " TEXT, " +
                COL_S_PHONE + " TEXT, " +
                COL_S_ADDRESS + " TEXT" +
                ");");

        // seed nhà cung cấp mẫu
        long appleSupplierId = seedSupplier(db, "Apple Vietnam Supply", "Apple", "0900000001", "TP.HCM");
        long samsungSupplierId = seedSupplier(db, "Samsung Partner VN", "Samsung", "0900000002", "Hà Nội");
        long xiaomiSupplierId = seedSupplier(db, "Xiaomi Distribution", "Xiaomi", "0900000003", "Đà Nẵng");

        // 7) phiếu nhập
        db.execSQL("CREATE TABLE " + TBL_RECEIPTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_R_SUPPLIER_ID + " INTEGER NOT NULL, " +
                COL_R_TOTAL_QTY + " INTEGER NOT NULL, " +
                COL_R_TOTAL_AMOUNT + " INTEGER NOT NULL, " +
                COL_R_CREATED + " INTEGER NOT NULL, " +
                COL_R_NOTE + " TEXT, " +
                COL_R_STATUS + " TEXT NOT NULL DEFAULT '" + Receipt.STATUS_DRAFT + "', " +
                COL_R_CREATED_BY + " TEXT, " +
                "FOREIGN KEY(" + COL_R_SUPPLIER_ID + ") REFERENCES " + TBL_SUPPLIERS + "(" + COL_ID + ")" +
                ");");

        // 8) chi tiết phiếu nhập
        db.execSQL("CREATE TABLE " + TBL_RECEIPT_ITEMS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_RI_RECEIPT_ID + " INTEGER NOT NULL, " +
                COL_RI_PRODUCT_ID + " INTEGER NOT NULL, " +
                COL_RI_PRODUCT_NAME + " TEXT NOT NULL, " +
                COL_RI_QTY + " INTEGER NOT NULL, " +
                COL_RI_UNIT_COST + " INTEGER NOT NULL, " +
                COL_RI_AMOUNT + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + COL_RI_RECEIPT_ID + ") REFERENCES " + TBL_RECEIPTS + "(" + COL_ID + ") ON DELETE CASCADE, " +
                "FOREIGN KEY(" + COL_RI_PRODUCT_ID + ") REFERENCES " + TBL_PRODUCTS + "(" + COL_ID + ")" +
                ");");

        // 9) lịch sử kho
        db.execSQL("CREATE TABLE " + TBL_INVENTORY_HISTORY + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_H_PRODUCT_ID + " INTEGER NOT NULL, " +
                COL_H_PRODUCT_NAME + " TEXT NOT NULL, " +
                COL_H_ACTION + " TEXT NOT NULL, " +
                COL_H_QTY + " INTEGER NOT NULL, " +
                COL_H_REF_TYPE + " TEXT, " +
                COL_H_REF_ID + " INTEGER, " +
                COL_H_NOTE + " TEXT, " +
                COL_H_CREATED + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + COL_H_PRODUCT_ID + ") REFERENCES " + TBL_PRODUCTS + "(" + COL_ID + ")" +
                ");");

        seedInitialReceipt(db, appleSupplierId, iphone15Id, "iPhone 15 Pro Max", 10, 24000000, "Nhập lô đầu tiên", Receipt.STATUS_COMPLETED, "Admin");
        seedInitialReceipt(db, samsungSupplierId, samsungS24Id, "Samsung S24 Ultra", 8, 20000000, "Bổ sung kho quý I", Receipt.STATUS_COMPLETED, "Admin");
        seedInitialReceipt(db, xiaomiSupplierId, xiaomi14Id, "Xiaomi 14", 15, 12000000, "Chờ xác nhận nhập kho", Receipt.STATUS_DRAFT, "Admin");
    }

    private long seedProduct(SQLiteDatabase db,
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
        return db.insert(TBL_PRODUCTS, null, v);
    }

    private long seedSupplier(SQLiteDatabase db, String name, String brand, String phone, String address) {
        ContentValues v = new ContentValues();
        v.put(COL_S_NAME, name);
        v.put(COL_S_BRAND, brand);
        v.put(COL_S_PHONE, phone);
        v.put(COL_S_ADDRESS, address);
        return db.insert(TBL_SUPPLIERS, null, v);
    }

    private void seedInitialReceipt(SQLiteDatabase db,
                                    long supplierId,
                                    long productId,
                                    String productName,
                                    int quantity,
                                    int unitCost,
                                    String note,
                                    String status,
                                    String creatorName) {
        long createdAt = System.currentTimeMillis();
        int totalAmount = quantity * unitCost;

        ContentValues receipt = new ContentValues();
        receipt.put(COL_R_SUPPLIER_ID, supplierId);
        receipt.put(COL_R_TOTAL_QTY, quantity);
        receipt.put(COL_R_TOTAL_AMOUNT, totalAmount);
        receipt.put(COL_R_CREATED, createdAt);
        receipt.put(COL_R_NOTE, note);
        receipt.put(COL_R_STATUS, status);
        receipt.put(COL_R_CREATED_BY, creatorName);
        long receiptId = db.insert(TBL_RECEIPTS, null, receipt);

        ContentValues receiptItem = new ContentValues();
        receiptItem.put(COL_RI_RECEIPT_ID, receiptId);
        receiptItem.put(COL_RI_PRODUCT_ID, productId);
        receiptItem.put(COL_RI_PRODUCT_NAME, productName);
        receiptItem.put(COL_RI_QTY, quantity);
        receiptItem.put(COL_RI_UNIT_COST, unitCost);
        receiptItem.put(COL_RI_AMOUNT, totalAmount);
        db.insert(TBL_RECEIPT_ITEMS, null, receiptItem);

        if (!Receipt.STATUS_COMPLETED.equals(status)) {
            return;
        }

        ContentValues history = new ContentValues();
        history.put(COL_H_PRODUCT_ID, productId);
        history.put(COL_H_PRODUCT_NAME, productName);
        history.put(COL_H_ACTION, "IMPORT");
        history.put(COL_H_QTY, quantity);
        history.put(COL_H_REF_TYPE, "RECEIPT");
        history.put(COL_H_REF_ID, receiptId);
        history.put(COL_H_NOTE, note);
        history.put(COL_H_CREATED, createdAt);
        db.insert(TBL_INVENTORY_HISTORY, null, history);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TBL_INVENTORY_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_RECEIPT_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_RECEIPTS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_SUPPLIERS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_ORDER_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_ORDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_CART);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_USERS);
        onCreate(db);
    }
}