package com.example.phonestore.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.phonestore.data.model.Receipt;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "phonestore.db";
    public static final int DB_VERSION = 14;

    // ===== USERS (SQLite: tiếng Việt không dấu) =====
    public static final String TBL_USERS = "nguoi_dung";
    public static final String COL_ID = "id";
    public static final String COL_FULLNAME = "ho_ten";
    public static final String COL_USERNAME = "ten_dang_nhap";
    public static final String COL_PASSWORD = "mat_khau";
    public static final String COL_ROLE = "vai_tro";
    public static final String COL_IS_ACTIVE = "is_active";

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
    public static final String COL_P_OS = "he_dieu_hanh";
    public static final String COL_P_ROM_GB = "rom_gb";
    public static final String COL_P_RAM_GB = "ram_gb";
    public static final String COL_P_CHIPSET = "chipset";
    public static final String COL_P_SCREEN = "man_hinh";
    public static final String COL_P_CAMERA = "camera";
    public static final String COL_P_BATTERY = "pin_mah";
    public static final String COL_P_COLORS = "mau_sac";

    // ===== CART =====
    public static final String TBL_CART = "gio_hang";
    public static final String COL_C_USER_ID = "nguoi_dung_id";
    public static final String COL_C_PRODUCT_ID = "san_pham_id";
    public static final String COL_C_QTY = "so_luong";
    public static final String COL_C_STORAGE = "dung_luong";
    public static final String COL_C_COLOR = "mau_sac_chon";

    // ===== ORDERS (hoa_don) =====
    public static final String TBL_ORDERS = "hoa_don";
    public static final String COL_O_USER_ID = "nguoi_dung_id";
    public static final String COL_O_TOTAL = "tong_tien";
    public static final String COL_O_CREATED = "ngay_tao";
    public static final String COL_O_ORDER_STATUS = "trang_thai_don";
    public static final String COL_O_PAYMENT_STATUS = "trang_thai_thanh_toan";

    public static final String COL_O_RECEIVER = "nguoi_nhan";
    public static final String COL_O_PHONE = "sdt_nhan";
    public static final String COL_O_ADDRESS = "dia_chi_nhan";
    public static final String COL_O_PAY_METHOD = "phuong_thuc_thanh_toan";
    public static final String COL_O_NOTE = "ghi_chu";
    public static final String COL_O_SUBTOTAL = "tam_tinh";
    public static final String COL_O_SHIPPING_FEE = "phi_van_chuyen";
    public static final String COL_O_DISCOUNT_CODE = "ma_giam_gia";
    public static final String COL_O_DISCOUNT_AMOUNT = "tien_giam";

    public static final String TBL_ORDER_ITEMS = "hoa_don_ct";
    public static final String COL_OI_ORDER_ID = "hoa_don_id";
    public static final String COL_OI_PRODUCT_ID = "san_pham_id";
    public static final String COL_OI_NAME = "ten_san_pham";
    public static final String COL_OI_PRICE = "don_gia";
    public static final String COL_OI_DISCOUNT = "giam_gia";
    public static final String COL_OI_QTY = "so_luong";
    public static final String COL_OI_AMOUNT = "thanh_tien";
    public static final String COL_OI_STORAGE = "dung_luong";
    public static final String COL_OI_COLOR = "mau_sac_chon";

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
        db.execSQL("CREATE TABLE " + TBL_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FULLNAME + " TEXT, " +
                COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                COL_PASSWORD + " TEXT NOT NULL, " +
                COL_ROLE + " TEXT NOT NULL, " +
                COL_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1" +
                ");");

        ContentValues admin = new ContentValues();
        admin.put(COL_FULLNAME, "Administrator");
        admin.put(COL_USERNAME, "admin");
        admin.put(COL_PASSWORD, "admin123");
        admin.put(COL_ROLE, ROLE_ADMIN);
        admin.put(COL_IS_ACTIVE, 1);
        db.insert(TBL_USERS, null, admin);

        ContentValues customer = new ContentValues();
        customer.put(COL_FULLNAME, "Khachhang");
        customer.put(COL_USERNAME, "khachhang");
        customer.put(COL_PASSWORD, "khachhang123");
        customer.put(COL_ROLE, ROLE_CUSTOMER);
        customer.put(COL_IS_ACTIVE, 1);
        db.insert(TBL_USERS, null, customer);

        db.execSQL("CREATE TABLE " + TBL_PRODUCTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_P_NAME + " TEXT NOT NULL, " +
                COL_P_BRAND + " TEXT, " +
                COL_P_PRICE + " INTEGER NOT NULL, " +
                COL_P_STOCK + " INTEGER NOT NULL DEFAULT 0, " +
                COL_P_DISCOUNT + " INTEGER NOT NULL DEFAULT 0, " +
                COL_P_DESC + " TEXT, " +
                COL_P_IMAGE + " TEXT, " +
                COL_P_OS + " TEXT, " +
                COL_P_ROM_GB + " INTEGER NOT NULL DEFAULT 0, " +
                COL_P_RAM_GB + " INTEGER NOT NULL DEFAULT 0, " +
                COL_P_CHIPSET + " TEXT, " +
                COL_P_SCREEN + " TEXT, " +
                COL_P_CAMERA + " TEXT, " +
                COL_P_BATTERY + " INTEGER NOT NULL DEFAULT 0, " +
                COL_P_COLORS + " TEXT, " +
                COL_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1" +
                ");");

        long iphone15Id = seedProduct(db, "iPhone 15 Pro Max", "Apple", 28990000, 10, 8,
                "Flagship màn lớn, camera mạnh và pin bền bỉ cho nhu cầu cao cấp.", "ip_15",
                "iOS", 256, 8, "Apple A17 Pro", "6.7\" Super Retina XDR", "48MP + 12MP + 12MP", 4422,
                "Titan tự nhiên,Đen titan,Trắng titan,Xanh titan");
        seedProduct(db, "iPhone 15", "Apple", 21990000, 12, 5,
                "Hiệu năng ổn định, camera đẹp và trải nghiệm iOS mượt mà hằng ngày.", "ip_15",
                "iOS", 128, 6, "Apple A16 Bionic", "6.1\" Super Retina XDR", "48MP + 12MP", 3349,
                "Đen,Xanh dương,Hồng,Vàng");
        seedProduct(db, "iPhone 14", "Apple", 16990000, 7, 12,
                "Thiết kế quen thuộc, hiệu năng tốt và phù hợp nhu cầu sử dụng lâu dài.", "ic_iphone15",
                "iOS", 128, 6, "Apple A15 Bionic", "6.1\" Super Retina XDR", "12MP + 12MP", 3279,
                "Đen,Trắng,Tím,Xanh");
        long samsungS24Id = seedProduct(db, "Samsung S24 Ultra", "Samsung", 24490000, 8, 10,
                "Màn hình lớn, camera zoom mạnh và bút S Pen cho người dùng chuyên sâu.", "ss_s24_utra",
                "Android", 512, 12, "Snapdragon 8 Gen 3", "6.8\" Dynamic AMOLED 2X", "200MP + 50MP + 12MP + 10MP", 5000,
                "Đen,Tím,Xám,Vàng");
        seedProduct(db, "Samsung Galaxy S24", "Samsung", 19990000, 9, 7,
                "Nhỏ gọn hơn dòng Ultra nhưng vẫn mạnh mẽ với AI và màn hình cao cấp.", "ss_s24_utra",
                "Android", 256, 8, "Exynos 2400", "6.2\" Dynamic AMOLED 2X", "50MP + 12MP + 10MP", 4000,
                "Đen,Xám,Tím,Vàng");
        seedProduct(db, "Samsung Galaxy A55", "Samsung", 9990000, 15, 15,
                "Điện thoại tầm trung cân bằng giữa thiết kế đẹp, pin khỏe và màn hình tốt.", "ss_s24_utra",
                "Android", 256, 8, "Exynos 1480", "6.6\" Super AMOLED", "50MP + 12MP + 5MP", 5000,
                "Xanh navy,Xanh nhạt,Tím");
        long xiaomi14Id = seedProduct(db, "Xiaomi 14", "Xiaomi", 15990000, 6, 6,
                "Hiệu năng cao, sạc nhanh và camera Leica nổi bật trong phân khúc.", "",
                "Android", 256, 12, "Snapdragon 8 Gen 3", "6.36\" AMOLED 120Hz", "50MP + 50MP + 50MP", 4610,
                "Đen,Trắng,Xanh lá");
        seedProduct(db, "Redmi Note 13 Pro", "Xiaomi", 8990000, 11, 10,
                "Màn hình đẹp, pin lớn và camera độ phân giải cao cho nhu cầu phổ thông nâng cao.", "",
                "Android", 256, 8, "Snapdragon 7s Gen 2", "6.67\" AMOLED 120Hz", "200MP + 8MP + 2MP", 5100,
                "Đen,Tím,Xanh dương");
        seedProduct(db, "OPPO Reno11", "OPPO", 10990000, 10, 8,
                "Thiết kế thời trang, camera chân dung đẹp và sạc nhanh tiện lợi.", "",
                "Android", 256, 12, "Dimensity 7050", "6.7\" AMOLED 120Hz", "50MP + 32MP + 8MP", 5000,
                "Bạc,Đen,Xanh ngọc");
        seedProduct(db, "vivo V30", "vivo", 11990000, 9, 9,
                "Mỏng nhẹ, pin lớn và hiệu năng ổn định cho giải trí lẫn công việc.", "",
                "Android", 256, 12, "Snapdragon 7 Gen 3", "6.78\" AMOLED 120Hz", "50MP + 50MP", 5000,
                "Đen,Tím,Xanh biển");

        db.execSQL("CREATE TABLE " + TBL_CART + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_C_USER_ID + " INTEGER NOT NULL, " +
                COL_C_PRODUCT_ID + " INTEGER NOT NULL, " +
                COL_C_QTY + " INTEGER NOT NULL DEFAULT 1, " +
                COL_C_STORAGE + " TEXT, " +
                COL_C_COLOR + " TEXT, " +
                "UNIQUE(" + COL_C_USER_ID + "," + COL_C_PRODUCT_ID + "," + COL_C_STORAGE + "," + COL_C_COLOR + "), " +
                "FOREIGN KEY(" + COL_C_USER_ID + ") REFERENCES " + TBL_USERS + "(" + COL_ID + "), " +
                "FOREIGN KEY(" + COL_C_PRODUCT_ID + ") REFERENCES " + TBL_PRODUCTS + "(" + COL_ID + ")" +
                ");");

        db.execSQL("CREATE TABLE " + TBL_ORDERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_O_USER_ID + " INTEGER NOT NULL, " +
                COL_O_TOTAL + " INTEGER NOT NULL, " +
                COL_O_CREATED + " INTEGER NOT NULL, " +
                COL_O_ORDER_STATUS + " TEXT NOT NULL, " +
                COL_O_PAYMENT_STATUS + " TEXT NOT NULL, " +
                COL_O_RECEIVER + " TEXT, " +
                COL_O_PHONE + " TEXT, " +
                COL_O_ADDRESS + " TEXT, " +
                COL_O_PAY_METHOD + " TEXT, " +
                COL_O_NOTE + " TEXT, " +
                COL_O_SUBTOTAL + " INTEGER NOT NULL DEFAULT 0, " +
                COL_O_SHIPPING_FEE + " INTEGER NOT NULL DEFAULT 0, " +
                COL_O_DISCOUNT_CODE + " TEXT, " +
                COL_O_DISCOUNT_AMOUNT + " INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(" + COL_O_USER_ID + ") REFERENCES " + TBL_USERS + "(" + COL_ID + ")" +
                ");");

        db.execSQL("CREATE TABLE " + TBL_ORDER_ITEMS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_OI_ORDER_ID + " INTEGER NOT NULL, " +
                COL_OI_PRODUCT_ID + " INTEGER NOT NULL, " +
                COL_OI_NAME + " TEXT NOT NULL, " +
                COL_OI_PRICE + " INTEGER NOT NULL, " +
                COL_OI_DISCOUNT + " INTEGER NOT NULL DEFAULT 0, " +
                COL_OI_QTY + " INTEGER NOT NULL, " +
                COL_OI_AMOUNT + " INTEGER NOT NULL, " +
                COL_OI_STORAGE + " TEXT, " +
                COL_OI_COLOR + " TEXT, " +
                "FOREIGN KEY(" + COL_OI_ORDER_ID + ") REFERENCES " + TBL_ORDERS + "(" + COL_ID + ") ON DELETE CASCADE" +
                ");");

        db.execSQL("CREATE TABLE " + TBL_SUPPLIERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_S_NAME + " TEXT NOT NULL, " +
                COL_S_BRAND + " TEXT, " +
                COL_S_PHONE + " TEXT, " +
                COL_S_ADDRESS + " TEXT" +
                ");");

        long appleSupplierId = seedSupplier(db, "Apple Vietnam Supply", "Apple", "0900000001", "TP.HCM");
        long samsungSupplierId = seedSupplier(db, "Samsung Partner VN", "Samsung", "0900000002", "Hà Nội");
        long xiaomiSupplierId = seedSupplier(db, "Xiaomi Distribution", "Xiaomi", "0900000003", "Đà Nẵng");

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
        seedInitialReceipt(db, xiaomiSupplierId, xiaomi14Id, "Xiaomi 14", 8, 12000000, "Chờ xác nhận bổ sung kho", Receipt.STATUS_DRAFT, "Admin");
    }

    private long seedProduct(SQLiteDatabase db,
                             String tenSanPham, String hang, int gia,
                             int tonKho, int giamGia, String moTa, String tenAnh,
                             String heDieuHanh, int romGb, int ramGb, String chipset,
                             String manHinh, String camera, int pinMah, String mauSac) {
        ContentValues v = new ContentValues();
        v.put(COL_P_NAME, tenSanPham);
        v.put(COL_P_BRAND, hang);
        v.put(COL_P_PRICE, gia);
        v.put(COL_P_STOCK, tonKho);
        v.put(COL_P_DISCOUNT, giamGia);
        v.put(COL_P_DESC, moTa);
        v.put(COL_P_IMAGE, tenAnh);
        v.put(COL_P_OS, heDieuHanh);
        v.put(COL_P_ROM_GB, romGb);
        v.put(COL_P_RAM_GB, ramGb);
        v.put(COL_P_CHIPSET, chipset);
        v.put(COL_P_SCREEN, manHinh);
        v.put(COL_P_CAMERA, camera);
        v.put(COL_P_BATTERY, pinMah);
        v.put(COL_P_COLORS, mauSac);
        v.put(COL_IS_ACTIVE, 1);
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
