package com.example.phonestore.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.CartItem;
import com.example.phonestore.data.model.CheckoutInfo;
import com.example.phonestore.data.model.Order;
import com.example.phonestore.data.model.OrderItem;
import com.example.phonestore.data.model.OrderStatus;

import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    private final DBHelper dbHelper;
    private final CartDao cartDao;
    private final InventoryHistoryDao historyDao;
    private String lastCheckoutError;

    public OrderDao(Context ctx) {
        dbHelper = new DBHelper(ctx);
        cartDao = new CartDao(ctx);
        historyDao = new InventoryHistoryDao(ctx);
    }

    public String getLastCheckoutError() {
        return lastCheckoutError;
    }

    private void setLastCheckoutError(String error) {
        lastCheckoutError = error;
    }

    private void clearLastCheckoutError() {
        lastCheckoutError = null;
    }

    // GIỮ LẠI để code cũ không vỡ (checkout nhanh như hiện tại)
    public long checkout(long userId) {
        CheckoutInfo info = new CheckoutInfo();
        info.paymentMethod = CheckoutInfo.PAYMENT_BANK_TRANSFER;
        info.receiverName = null;
        info.receiverPhone = null;
        info.receiverAddress = null;
        info.note = null;
        return checkout(userId, info);
    }

    // NEW: checkout có thông tin nhận hàng + phương thức TT
    public long checkout(long userId, CheckoutInfo info) {
        ArrayList<CartItem> items = cartDao.getCartItems(userId);
        return checkoutWithItems(userId, info, items, false, null);
    }

    public long checkout(long userId, CheckoutInfo info, List<Long> selectedProductIds) {
        ArrayList<CartItem> items = cartDao.getCartItemsByProductIds(userId, selectedProductIds);
        return checkoutWithItems(userId, info, items, true, selectedProductIds);
    }

    private long checkoutWithItems(long userId, CheckoutInfo info, ArrayList<CartItem> items,
                                   boolean selectedOnly, List<Long> selectedProductIds) {
        clearLastCheckoutError();

        if (items.isEmpty()) {
            setLastCheckoutError("Giỏ hàng trống hoặc sản phẩm đã bị bỏ chọn");
            return -1;
        }

        for (CartItem it : items) {
            if (it.soLuong > it.tonKho) {
                setLastCheckoutError("Sản phẩm " + it.tenSanPham + " không đủ tồn kho");
                return -1;
            }
        }

        int total = 0;
        for (CartItem it : items) total += it.thanhTien();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues o = new ContentValues();
            o.put(DBHelper.COL_O_USER_ID, userId);
            o.put(DBHelper.COL_O_TOTAL, total);
            o.put(DBHelper.COL_O_CREATED, System.currentTimeMillis());

            String status = CheckoutInfo.PAYMENT_BANK_TRANSFER.equals(info.paymentMethod)
                    ? OrderStatus.STATUS_DA_THANH_TOAN
                    : OrderStatus.STATUS_CHO_XAC_NHAN;
            o.put(DBHelper.COL_O_STATUS, status);

            o.put(DBHelper.COL_O_RECEIVER, info.receiverName);
            o.put(DBHelper.COL_O_PHONE, info.receiverPhone);
            o.put(DBHelper.COL_O_ADDRESS, info.receiverAddress);
            o.put(DBHelper.COL_O_PAY_METHOD, info.paymentMethod);
            o.put(DBHelper.COL_O_NOTE, info.note);

            long orderId = db.insert(DBHelper.TBL_ORDERS, null, o);
            if (orderId == -1) {
                setLastCheckoutError("Không tạo được đơn hàng");
                return -1;
            }

            for (CartItem it : items) {
                ContentValues oi = new ContentValues();
                oi.put(DBHelper.COL_OI_ORDER_ID, orderId);
                oi.put(DBHelper.COL_OI_PRODUCT_ID, it.productId);
                oi.put(DBHelper.COL_OI_NAME, it.tenSanPham);
                oi.put(DBHelper.COL_OI_PRICE, it.gia);
                oi.put(DBHelper.COL_OI_DISCOUNT, it.giamGia);
                oi.put(DBHelper.COL_OI_QTY, it.soLuong);
                oi.put(DBHelper.COL_OI_AMOUNT, it.thanhTien());
                if (db.insert(DBHelper.TBL_ORDER_ITEMS, null, oi) == -1) {
                    setLastCheckoutError("Không tạo được chi tiết đơn hàng");
                    return -1;
                }

                ContentValues p = new ContentValues();
                p.put(DBHelper.COL_P_STOCK, it.tonKho - it.soLuong);
                int updated = db.update(
                        DBHelper.TBL_PRODUCTS,
                        p,
                        DBHelper.COL_ID + "=? AND " + DBHelper.COL_P_STOCK + " >= ?",
                        new String[]{String.valueOf(it.productId), String.valueOf(it.soLuong)}
                );
                if (updated <= 0) {
                    setLastCheckoutError("Không thể trừ tồn kho cho sản phẩm " + it.tenSanPham);
                    return -1;
                }

                it.tonKho = it.tonKho - it.soLuong;
                historyDao.insert(
                        db,
                        it.productId,
                        it.tenSanPham,
                        InventoryHistoryDao.ACTION_EXPORT,
                        it.soLuong,
                        "ORDER",
                        orderId,
                        status
                );
            }

            if (selectedOnly) {
                deleteCartItemsInTransaction(db, userId, selectedProductIds);
            } else {
                clearCartInTransaction(db, userId);
            }

            clearLastCheckoutError();

            db.setTransactionSuccessful();
            return orderId;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("foreign key")) {
                setLastCheckoutError("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại");
            } else {
                setLastCheckoutError("Lỗi đặt hàng: " + (msg == null ? "không xác định" : msg));
            }
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    // NEW: đọc hóa đơn (để show nhận hàng)
    public Order getOrderById(long orderId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_ID + "," +
                        DBHelper.COL_O_USER_ID + "," +
                        DBHelper.COL_O_TOTAL + "," +
                        DBHelper.COL_O_CREATED + "," +
                        DBHelper.COL_O_STATUS + "," +
                        DBHelper.COL_O_RECEIVER + "," +
                        DBHelper.COL_O_PHONE + "," +
                        DBHelper.COL_O_ADDRESS + "," +
                        DBHelper.COL_O_PAY_METHOD + "," +
                        DBHelper.COL_O_NOTE +
                        " FROM " + DBHelper.TBL_ORDERS +
                        " WHERE " + DBHelper.COL_ID + "=? LIMIT 1",
                new String[]{String.valueOf(orderId)}
        );

        Order o = null;
        if (c.moveToFirst()) {
            o = new Order();
            o.id = c.getLong(0);
            o.userId = c.getLong(1);
            o.tongTien = c.getInt(2);
            o.ngayTao = c.getLong(3);
            o.trangThai = c.getString(4);
            o.nguoiNhan = c.getString(5);
            o.sdtNhan = c.getString(6);
            o.diaChiNhan = c.getString(7);
            o.phuongThucThanhToan = c.getString(8);
            o.ghiChu = c.getString(9);
        }
        c.close();
        return o;
    }

    public ArrayList<Order> getOrdersByUser(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Order> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_ID + "," +
                        DBHelper.COL_O_USER_ID + "," +
                        DBHelper.COL_O_TOTAL + "," +
                        DBHelper.COL_O_CREATED + "," +
                        DBHelper.COL_O_STATUS +
                        " FROM " + DBHelper.TBL_ORDERS +
                        " WHERE " + DBHelper.COL_O_USER_ID + "=? " +
                        " ORDER BY " + DBHelper.COL_ID + " DESC",
                new String[]{String.valueOf(userId)}
        );

        while (c.moveToNext()) {
            Order o = new Order();
            o.id = c.getLong(0);
            o.userId = c.getLong(1);
            o.tongTien = c.getInt(2);
            o.ngayTao = c.getLong(3);
            o.trangThai = c.getString(4);
            list.add(o);
        }
        c.close();
        return list;
    }

    // admin xem tất cả: join username
    public ArrayList<Order> getAllOrders() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Order> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT o." + DBHelper.COL_ID + "," +
                        " o." + DBHelper.COL_O_USER_ID + "," +
                        " u." + DBHelper.COL_USERNAME + "," +
                        " o." + DBHelper.COL_O_TOTAL + "," +
                        " o." + DBHelper.COL_O_CREATED + "," +
                        " o." + DBHelper.COL_O_STATUS +
                        " FROM " + DBHelper.TBL_ORDERS + " o" +
                        " JOIN " + DBHelper.TBL_USERS + " u ON u." + DBHelper.COL_ID + " = o." + DBHelper.COL_O_USER_ID +
                        " ORDER BY o." + DBHelper.COL_ID + " DESC",
                null
        );

        while (c.moveToNext()) {
            Order o = new Order();
            o.id = c.getLong(0);
            o.userId = c.getLong(1);
            o.username = c.getString(2);
            o.tongTien = c.getInt(3);
            o.ngayTao = c.getLong(4);
            o.trangThai = c.getString(5);
            list.add(o);
        }
        c.close();
        return list;
    }

    public ArrayList<OrderItem> getOrderItems(long orderId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<OrderItem> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_ID + "," +
                        DBHelper.COL_OI_ORDER_ID + "," +
                        DBHelper.COL_OI_PRODUCT_ID + "," +
                        DBHelper.COL_OI_NAME + "," +
                        DBHelper.COL_OI_PRICE + "," +
                        DBHelper.COL_OI_DISCOUNT + "," +
                        DBHelper.COL_OI_QTY + "," +
                        DBHelper.COL_OI_AMOUNT +
                        " FROM " + DBHelper.TBL_ORDER_ITEMS +
                        " WHERE " + DBHelper.COL_OI_ORDER_ID + "=? " +
                        " ORDER BY " + DBHelper.COL_ID + " DESC",
                new String[]{String.valueOf(orderId)}
        );

        while (c.moveToNext()) {
            OrderItem it = new OrderItem();
            it.id = c.getLong(0);
            it.orderId = c.getLong(1);
            it.productId = c.getLong(2);
            it.tenSanPham = c.getString(3);
            it.donGia = c.getInt(4);
            it.giamGia = c.getInt(5);
            it.soLuong = c.getInt(6);
            it.thanhTien = c.getInt(7);
            list.add(it);
        }
        c.close();
        return list;
    }

    public boolean updateStatus(long orderId, String status) {
        if (!isValidStatus(status)) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_O_STATUS, status);

        return db.update(
                DBHelper.TBL_ORDERS,
                v,
                DBHelper.COL_ID + "=?",
                new String[]{String.valueOf(orderId)}
        ) > 0;
    }

    // report đơn giản
    public int getTongDoanhThu() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(" + DBHelper.COL_O_TOTAL + ") FROM " + DBHelper.TBL_ORDERS, null);
        int sum = 0;
        if (c.moveToFirst() && !c.isNull(0)) sum = c.getInt(0);
        c.close();
        return sum;
    }

    public int getSoDonHang() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TBL_ORDERS, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // ====== REPORT MODELS ======
    public static class MonthRevenue {
        public int month;   // 1..12
        public int revenue; // VND

        public MonthRevenue(int month, int revenue) {
            this.month = month;
            this.revenue = revenue;
        }
    }

    public static class ProductSale {
        public String name;
        public int qty;

        public ProductSale(String name, int qty) {
            this.name = name;
            this.qty = qty;
        }
    }

    public static class StatusCount {
        public String status;
        public int count;

        public StatusCount(String status, int count) {
            this.status = status;
            this.count = count;
        }
    }

    // ====== REPORT QUERIES ======

    // Doanh thu theo tháng trong 1 năm (dựa trên COL_O_CREATED = millis) :contentReference[oaicite:8]{index=8}
    public ArrayList<MonthRevenue> getDoanhThuTheoThang(int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<MonthRevenue> list = new ArrayList<>();

        // sqlite: datetime(millis/1000,'unixepoch')
        Cursor c = db.rawQuery(
                "SELECT " +
                        "CAST(strftime('%m', datetime(" + DBHelper.COL_O_CREATED + "/1000, 'unixepoch')) AS INTEGER) AS m, " +
                        "SUM(" + DBHelper.COL_O_TOTAL + ") AS total " +
                        "FROM " + DBHelper.TBL_ORDERS + " " +
                        "WHERE strftime('%Y', datetime(" + DBHelper.COL_O_CREATED + "/1000, 'unixepoch')) = ? " +
                        "GROUP BY m " +
                        "ORDER BY m",
                new String[]{String.valueOf(year)}
        );

        while (c.moveToNext()) {
            int month = c.isNull(0) ? 0 : c.getInt(0);
            int total = c.isNull(1) ? 0 : c.getInt(1);
            list.add(new MonthRevenue(month, total));
        }
        c.close();
        return list;
    }

    // Top sản phẩm bán chạy (dựa trên hóa đơn chi tiết)
    public ArrayList<ProductSale> getTopSanPhamBanChay(int limit) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<ProductSale> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_OI_NAME + ", SUM(" + DBHelper.COL_OI_QTY + ") AS qty " +
                        "FROM " + DBHelper.TBL_ORDER_ITEMS + " " +
                        "GROUP BY " + DBHelper.COL_OI_NAME + " " +
                        "ORDER BY qty DESC " +
                        "LIMIT " + limit,
                null
        );

        while (c.moveToNext()) {
            String name = c.getString(0);
            int qty = c.isNull(1) ? 0 : c.getInt(1);
            list.add(new ProductSale(name, qty));
        }
        c.close();
        return list;
    }

    // Đếm đơn theo trạng thái
    public ArrayList<StatusCount> getSoDonTheoTrangThai() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<StatusCount> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_O_STATUS + ", COUNT(*) AS cnt " +
                        "FROM " + DBHelper.TBL_ORDERS + " " +
                        "GROUP BY " + DBHelper.COL_O_STATUS,
                null
        );

        while (c.moveToNext()) {
            String status = c.getString(0);
            int cnt = c.isNull(1) ? 0 : c.getInt(1);
            list.add(new StatusCount(status, cnt));
        }
        c.close();
        return list;
    }

    private boolean isValidStatus(String status) {
        return OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)
                || OrderStatus.STATUS_DA_THANH_TOAN.equals(status)
                || OrderStatus.STATUS_DANG_XU_LY.equals(status)
                || OrderStatus.STATUS_DA_GIAO.equals(status)
                || OrderStatus.STATUS_DA_HUY.equals(status);
    }

    private void clearCartInTransaction(SQLiteDatabase db, long userId) {
        db.delete(
                DBHelper.TBL_CART,
                DBHelper.COL_C_USER_ID + "=?",
                new String[]{String.valueOf(userId)}
        );
    }

    private void deleteCartItemsInTransaction(SQLiteDatabase db, long userId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;

        StringBuilder inClause = new StringBuilder();
        String[] args = new String[productIds.size() + 1];
        args[0] = String.valueOf(userId);

        for (int i = 0; i < productIds.size(); i++) {
            if (i > 0) inClause.append(",");
            inClause.append("?");
            args[i + 1] = String.valueOf(productIds.get(i));
        }

        db.delete(
                DBHelper.TBL_CART,
                DBHelper.COL_C_USER_ID + "=? AND " + DBHelper.COL_C_PRODUCT_ID + " IN (" + inClause + ")",
                args
        );
    }
}
