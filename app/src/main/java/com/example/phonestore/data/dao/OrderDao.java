package com.example.phonestore.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.CartItem;
import com.example.phonestore.data.model.CartItem;
import com.example.phonestore.data.model.CheckoutInfo;
import com.example.phonestore.data.model.Order;
import com.example.phonestore.data.model.OrderItem;
import com.example.phonestore.data.model.OrderStatus;
import com.example.phonestore.data.model.PaymentStatus;
import com.example.phonestore.data.model.Product;

import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    private static final String REFERENCE_TYPE_ORDER = "ORDER";

    private final DBHelper dbHelper;
    private final CartDao cartDao;
    private final ProductDao productDao;
    private final InventoryHistoryDao historyDao;
    private String lastCheckoutError;

    public OrderDao(Context ctx) {
        dbHelper = new DBHelper(ctx);
        cartDao = new CartDao(ctx);
        productDao = new ProductDao(ctx);
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

    public long checkout(long userId) {
        CheckoutInfo info = new CheckoutInfo();
        info.paymentMethod = CheckoutInfo.PAYMENT_BANK_TRANSFER;
        info.receiverName = null;
        info.receiverPhone = null;
        info.receiverAddress = null;
        info.note = null;
        return checkout(userId, info);
    }

    public long checkout(long userId, CheckoutInfo info) {
        ArrayList<CartItem> items = cartDao.getCartItems(userId);
        return checkoutWithItems(userId, info, items, false, null);
    }

    public long checkout(long userId, CheckoutInfo info, List<Long> selectedProductIds) {
        ArrayList<CartItem> items = cartDao.getCartItemsByProductIds(userId, selectedProductIds);
        return checkoutWithItems(userId, info, items, true, selectedProductIds);
    }

    public long checkoutSingleProduct(long userId, long productId, int quantity, CheckoutInfo info) {
        CartItem item = buildSingleProductCartItem(productId, quantity);
        if (item == null) {
            setLastCheckoutError("Không tìm thấy sản phẩm");
            return -1;
        }

        ArrayList<CartItem> items = new ArrayList<>();
        items.add(item);
        return checkoutWithItems(userId, info, items, false, null);
    }

    public int previewSingleProductTotal(long productId, int quantity) {
        CartItem item = buildSingleProductCartItem(productId, quantity);
        return item == null ? 0 : item.thanhTien();
    }

    private long checkoutWithItems(long userId, CheckoutInfo info, ArrayList<CartItem> items,
                                   boolean selectedOnly, List<Long> selectedProductIds) {
        clearLastCheckoutError();

        String checkoutError = validateCheckoutInfo(info);
        if (checkoutError != null) {
            setLastCheckoutError(checkoutError);
            return -1;
        }

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

        int subtotal = 0;
        for (CartItem it : items) subtotal += it.thanhTien();
        int shippingFee = Math.max(0, info.shippingFee);
        int discountAmount = Math.max(0, info.discountAmount);
        int total = Math.max(0, subtotal + shippingFee - discountAmount);

        String orderStatus = OrderStatus.STATUS_CHO_XAC_NHAN;
        String paymentStatus = CheckoutInfo.PAYMENT_BANK_TRANSFER.equals(info.paymentMethod)
                ? PaymentStatus.STATUS_CHO_THANH_TOAN
                : PaymentStatus.STATUS_CHUA_THANH_TOAN;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues o = new ContentValues();
            o.put(DBHelper.COL_O_USER_ID, userId);
            o.put(DBHelper.COL_O_TOTAL, total);
            o.put(DBHelper.COL_O_CREATED, System.currentTimeMillis());
            o.put(DBHelper.COL_O_ORDER_STATUS, orderStatus);
            o.put(DBHelper.COL_O_PAYMENT_STATUS, paymentStatus);
            o.put(DBHelper.COL_O_RECEIVER, normalizeText(info.receiverName));
            o.put(DBHelper.COL_O_PHONE, normalizeText(info.receiverPhone));
            o.put(DBHelper.COL_O_ADDRESS, normalizeText(info.receiverAddress));
            o.put(DBHelper.COL_O_PAY_METHOD, info.paymentMethod);
            o.put(DBHelper.COL_O_NOTE, normalizeOptionalText(info.note));

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

                if (!CheckoutInfo.PAYMENT_BANK_TRANSFER.equals(info.paymentMethod)) {
                    ContentValues p = new ContentValues();
                    p.put(DBHelper.COL_P_STOCK, it.tonKho - it.soLuong);
                    int updated = db.update(
                            DBHelper.TBL_PRODUCTS,
                            p,
                            DBHelper.COL_ID + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1 AND " + DBHelper.COL_P_STOCK + " >= ?",
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
                            REFERENCE_TYPE_ORDER,
                            orderId,
                            orderStatus
                    );
                }
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

    public Order getOrderById(long orderId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " +
                        DBHelper.COL_ID + "," +
                        DBHelper.COL_O_USER_ID + "," +
                        DBHelper.COL_O_TOTAL + "," +
                        DBHelper.COL_O_CREATED + "," +
                        DBHelper.COL_O_ORDER_STATUS + "," +
                        DBHelper.COL_O_PAYMENT_STATUS + "," +
                        DBHelper.COL_O_RECEIVER + "," +
                        DBHelper.COL_O_PHONE + "," +
                        DBHelper.COL_O_ADDRESS + "," +
                        DBHelper.COL_O_PAY_METHOD + "," +
                        DBHelper.COL_O_NOTE +
                        " FROM " + DBHelper.TBL_ORDERS +
                        " WHERE " + DBHelper.COL_ID + "=? LIMIT 1",
                new String[]{String.valueOf(orderId)}
        );

        Order order = null;
        if (c.moveToFirst()) {
            order = readOrder(c);
        }
        c.close();
        return order;
    }

    public ArrayList<Order> getOrdersByUser(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Order> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT " +
                        DBHelper.COL_ID + "," +
                        DBHelper.COL_O_USER_ID + "," +
                        DBHelper.COL_O_TOTAL + "," +
                        DBHelper.COL_O_CREATED + "," +
                        DBHelper.COL_O_ORDER_STATUS + "," +
                        DBHelper.COL_O_PAYMENT_STATUS + "," +
                        DBHelper.COL_O_RECEIVER + "," +
                        DBHelper.COL_O_PHONE + "," +
                        DBHelper.COL_O_ADDRESS + "," +
                        DBHelper.COL_O_PAY_METHOD + "," +
                        DBHelper.COL_O_NOTE +
                        " FROM " + DBHelper.TBL_ORDERS +
                        " WHERE " + DBHelper.COL_O_USER_ID + "=?" +
                        " ORDER BY " + DBHelper.COL_ID + " DESC",
                new String[]{String.valueOf(userId)}
        );

        while (c.moveToNext()) {
            list.add(readOrder(c));
        }
        c.close();
        return list;
    }

    public ArrayList<Order> getAllOrders() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Order> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT " +
                        "o." + DBHelper.COL_ID + "," +
                        "o." + DBHelper.COL_O_USER_ID + "," +
                        "u." + DBHelper.COL_USERNAME + "," +
                        "o." + DBHelper.COL_O_TOTAL + "," +
                        "o." + DBHelper.COL_O_CREATED + "," +
                        "o." + DBHelper.COL_O_ORDER_STATUS + "," +
                        "o." + DBHelper.COL_O_PAYMENT_STATUS + "," +
                        "o." + DBHelper.COL_O_RECEIVER + "," +
                        "o." + DBHelper.COL_O_PHONE + "," +
                        "o." + DBHelper.COL_O_ADDRESS + "," +
                        "o." + DBHelper.COL_O_PAY_METHOD + "," +
                        "o." + DBHelper.COL_O_NOTE +
                        " FROM " + DBHelper.TBL_ORDERS + " o" +
                        " JOIN " + DBHelper.TBL_USERS + " u ON u." + DBHelper.COL_ID + " = o." + DBHelper.COL_O_USER_ID +
                        " ORDER BY o." + DBHelper.COL_ID + " DESC",
                null
        );

        while (c.moveToNext()) {
            list.add(readOrder(c));
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
                        " WHERE " + DBHelper.COL_OI_ORDER_ID + "=?" +
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

    public boolean updateOrderStatus(long orderId, String newStatus) {
        if (!isValidOrderStatus(newStatus)) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Order order = getOrderByIdInTransaction(db, orderId);
            if (!canTransitionOrder(order, newStatus)) {
                return false;
            }

            if (OrderStatus.STATUS_DA_HUY.equals(newStatus) && !OrderStatus.STATUS_DA_HUY.equals(order.trangThaiDon)) {
                ArrayList<OrderItem> items = getOrderItemsInTransaction(db, orderId);
                for (OrderItem item : items) {
                    db.execSQL(
                            "UPDATE " + DBHelper.TBL_PRODUCTS +
                                    " SET " + DBHelper.COL_P_STOCK + " = " + DBHelper.COL_P_STOCK + " + ?" +
                                    " WHERE " + DBHelper.COL_ID + "=?",
                            new Object[]{item.soLuong, item.productId}
                    );
                    historyDao.insert(
                            db,
                            item.productId,
                            item.tenSanPham,
                            InventoryHistoryDao.ACTION_CANCEL_RETURN,
                            item.soLuong,
                            REFERENCE_TYPE_ORDER,
                            orderId,
                            "Hủy đơn hàng"
                    );
                }
            }

            ContentValues v = new ContentValues();
            v.put(DBHelper.COL_O_ORDER_STATUS, newStatus);
            if (OrderStatus.STATUS_DA_GIAO.equals(newStatus)
                    && CheckoutInfo.PAYMENT_COD.equals(order.phuongThucThanhToan)
                    && !PaymentStatus.STATUS_DA_THANH_TOAN.equals(order.trangThaiThanhToan)) {
                v.put(DBHelper.COL_O_PAYMENT_STATUS, PaymentStatus.STATUS_DA_THANH_TOAN);
            }

            boolean updated = db.update(
                    DBHelper.TBL_ORDERS,
                    v,
                    DBHelper.COL_ID + "=?",
                    new String[]{String.valueOf(orderId)}
            ) > 0;
            if (!updated) {
                return false;
            }

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public boolean updatePaymentStatus(long orderId, String newStatus) {
        if (!isValidPaymentStatus(newStatus)) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Order order = getOrderByIdInTransaction(db, orderId);
            if (!canTransitionPayment(order, newStatus)) {
                return false;
            }

            if (PaymentStatus.STATUS_DA_THANH_TOAN.equals(newStatus)
                    && CheckoutInfo.PAYMENT_BANK_TRANSFER.equals(order.phuongThucThanhToan)) {
                ArrayList<OrderItem> items = getOrderItemsInTransaction(db, orderId);
                for (OrderItem item : items) {
                    int updatedStock = db.update(
                            DBHelper.TBL_PRODUCTS,
                            buildStockDecreaseValue(db, item.productId, item.soLuong),
                            DBHelper.COL_ID + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1 AND " + DBHelper.COL_P_STOCK + " >= ?",
                            new String[]{String.valueOf(item.productId), String.valueOf(item.soLuong)}
                    );
                    if (updatedStock <= 0) {
                        return false;
                    }
                    historyDao.insert(
                            db,
                            item.productId,
                            item.tenSanPham,
                            InventoryHistoryDao.ACTION_EXPORT,
                            item.soLuong,
                            REFERENCE_TYPE_ORDER,
                            orderId,
                            "Thanh toán chuyển khoản đã xác nhận"
                    );
                }
            }

            ContentValues v = new ContentValues();
            v.put(DBHelper.COL_O_PAYMENT_STATUS, newStatus);
            boolean updated = db.update(
                    DBHelper.TBL_ORDERS,
                    v,
                    DBHelper.COL_ID + "=?",
                    new String[]{String.valueOf(orderId)}
            ) > 0;
            if (!updated) {
                return false;
            }

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public int getTongDoanhThu() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + DBHelper.COL_O_TOTAL + ") FROM " + DBHelper.TBL_ORDERS +
                        " WHERE " + DBHelper.COL_O_ORDER_STATUS + "=?",
                new String[]{OrderStatus.STATUS_DA_GIAO}
        );
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

    public static class MonthRevenue {
        public int month;
        public int revenue;

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

    public ArrayList<MonthRevenue> getDoanhThuTheoThang(int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<MonthRevenue> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT " +
                        "CAST(strftime('%m', datetime(" + DBHelper.COL_O_CREATED + "/1000, 'unixepoch')) AS INTEGER) AS m, " +
                        "SUM(" + DBHelper.COL_O_TOTAL + ") AS total " +
                        "FROM " + DBHelper.TBL_ORDERS + " " +
                        "WHERE strftime('%Y', datetime(" + DBHelper.COL_O_CREATED + "/1000, 'unixepoch')) = ? " +
                        "AND " + DBHelper.COL_O_ORDER_STATUS + "=? " +
                        "GROUP BY m " +
                        "ORDER BY m",
                new String[]{String.valueOf(year), OrderStatus.STATUS_DA_GIAO}
        );

        while (c.moveToNext()) {
            int month = c.isNull(0) ? 0 : c.getInt(0);
            int total = c.isNull(1) ? 0 : c.getInt(1);
            list.add(new MonthRevenue(month, total));
        }
        c.close();
        return list;
    }

    public ArrayList<ProductSale> getTopSanPhamBanChay(int limit) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<ProductSale> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT oi." + DBHelper.COL_OI_NAME + ", SUM(oi." + DBHelper.COL_OI_QTY + ") AS qty " +
                        "FROM " + DBHelper.TBL_ORDER_ITEMS + " oi " +
                        "JOIN " + DBHelper.TBL_ORDERS + " o ON o." + DBHelper.COL_ID + " = oi." + DBHelper.COL_OI_ORDER_ID + " " +
                        "WHERE o." + DBHelper.COL_O_ORDER_STATUS + "=? " +
                        "GROUP BY oi." + DBHelper.COL_OI_NAME + " " +
                        "ORDER BY qty DESC " +
                        "LIMIT " + limit,
                new String[]{OrderStatus.STATUS_DA_GIAO}
        );

        while (c.moveToNext()) {
            String name = c.getString(0);
            int qty = c.isNull(1) ? 0 : c.getInt(1);
            list.add(new ProductSale(name, qty));
        }
        c.close();
        return list;
    }

    public ArrayList<StatusCount> getSoDonTheoTrangThai() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<StatusCount> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_O_ORDER_STATUS + ", COUNT(*) AS cnt " +
                        "FROM " + DBHelper.TBL_ORDERS + " " +
                        "GROUP BY " + DBHelper.COL_O_ORDER_STATUS,
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

    public ArrayList<String> getAllowedNextOrderStatuses(Order order) {
        ArrayList<String> allowed = new ArrayList<>();
        if (canTransitionOrder(order, OrderStatus.STATUS_DANG_XU_LY)) {
            allowed.add(OrderStatus.STATUS_DANG_XU_LY);
        }
        if (canTransitionOrder(order, OrderStatus.STATUS_DA_GIAO)) {
            allowed.add(OrderStatus.STATUS_DA_GIAO);
        }
        if (canTransitionOrder(order, OrderStatus.STATUS_DA_HUY)) {
            allowed.add(OrderStatus.STATUS_DA_HUY);
        }
        return allowed;
    }

    public ArrayList<String> getAllowedNextPaymentStatuses(Order order) {
        ArrayList<String> allowed = new ArrayList<>();
        if (canTransitionPayment(order, PaymentStatus.STATUS_DA_THANH_TOAN)) {
            allowed.add(PaymentStatus.STATUS_DA_THANH_TOAN);
        }
        return allowed;
    }

    public boolean isFinalOrderStatus(String status) {
        return OrderStatus.STATUS_DA_GIAO.equals(status) || OrderStatus.STATUS_DA_HUY.equals(status);
    }

    public boolean canTransitionOrder(Order order, String newStatus) {
        if (order == null || !isValidOrderStatus(newStatus) || !isValidOrderStatus(order.trangThaiDon)) {
            return false;
        }
        if (TextUtils.equals(order.trangThaiDon, newStatus) || isFinalOrderStatus(order.trangThaiDon)) {
            return false;
        }

        if (OrderStatus.STATUS_CHO_XAC_NHAN.equals(order.trangThaiDon)) {
            if (OrderStatus.STATUS_DANG_XU_LY.equals(newStatus)) {
                return canMoveToProcessing(order);
            }
            return OrderStatus.STATUS_DA_HUY.equals(newStatus);
        }

        if (OrderStatus.STATUS_DANG_XU_LY.equals(order.trangThaiDon)) {
            return OrderStatus.STATUS_DA_GIAO.equals(newStatus)
                    || OrderStatus.STATUS_DA_HUY.equals(newStatus);
        }

        return false;
    }

    public boolean canTransitionPayment(Order order, String newStatus) {
        if (order == null || !isValidPaymentStatus(newStatus) || !isValidPaymentStatus(order.trangThaiThanhToan)) {
            return false;
        }
        if (TextUtils.equals(order.trangThaiThanhToan, newStatus)) {
            return false;
        }
        if (OrderStatus.STATUS_DA_HUY.equals(order.trangThaiDon)) {
            return false;
        }
        if (PaymentStatus.STATUS_DA_THANH_TOAN.equals(order.trangThaiThanhToan)) {
            return false;
        }
        return PaymentStatus.STATUS_DA_THANH_TOAN.equals(newStatus)
                && (PaymentStatus.STATUS_CHUA_THANH_TOAN.equals(order.trangThaiThanhToan)
                || PaymentStatus.STATUS_CHO_THANH_TOAN.equals(order.trangThaiThanhToan));
    }

    private boolean canMoveToProcessing(Order order) {
        if (!CheckoutInfo.PAYMENT_BANK_TRANSFER.equals(order.phuongThucThanhToan)) {
            return true;
        }
        return PaymentStatus.STATUS_DA_THANH_TOAN.equals(order.trangThaiThanhToan);
    }

    private boolean isValidOrderStatus(String status) {
        return OrderStatus.STATUS_CHO_XAC_NHAN.equals(status)
                || OrderStatus.STATUS_DANG_XU_LY.equals(status)
                || OrderStatus.STATUS_DA_GIAO.equals(status)
                || OrderStatus.STATUS_DA_HUY.equals(status);
    }

    private boolean isValidPaymentStatus(String status) {
        return PaymentStatus.STATUS_CHUA_THANH_TOAN.equals(status)
                || PaymentStatus.STATUS_CHO_THANH_TOAN.equals(status)
                || PaymentStatus.STATUS_DA_THANH_TOAN.equals(status);
    }

    private String validateCheckoutInfo(CheckoutInfo info) {
        if (info == null) {
            return "Thiếu thông tin đặt hàng";
        }
        if (TextUtils.isEmpty(normalizeText(info.receiverName))
                || TextUtils.isEmpty(normalizeText(info.receiverPhone))
                || TextUtils.isEmpty(normalizeText(info.receiverAddress))) {
            return "Vui lòng nhập đủ Người nhận / SĐT / Địa chỉ";
        }
        String phone = normalizeText(info.receiverPhone);
        if (!phone.matches("\\d{9,11}")) {
            return "Số điện thoại không hợp lệ";
        }
        if (!CheckoutInfo.PAYMENT_COD.equals(info.paymentMethod)
                && !CheckoutInfo.PAYMENT_BANK_TRANSFER.equals(info.paymentMethod)) {
            return "Phương thức thanh toán không hợp lệ";
        }
        return null;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptionalText(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? "" : normalized;
    }

    private Order getOrderByIdInTransaction(SQLiteDatabase db, long orderId) {
        Cursor c = db.rawQuery(
                "SELECT " +
                        DBHelper.COL_ID + "," +
                        DBHelper.COL_O_USER_ID + "," +
                        DBHelper.COL_O_TOTAL + "," +
                        DBHelper.COL_O_CREATED + "," +
                        DBHelper.COL_O_ORDER_STATUS + "," +
                        DBHelper.COL_O_PAYMENT_STATUS + "," +
                        DBHelper.COL_O_RECEIVER + "," +
                        DBHelper.COL_O_PHONE + "," +
                        DBHelper.COL_O_ADDRESS + "," +
                        DBHelper.COL_O_PAY_METHOD + "," +
                        DBHelper.COL_O_NOTE +
                        " FROM " + DBHelper.TBL_ORDERS +
                        " WHERE " + DBHelper.COL_ID + "=? LIMIT 1",
                new String[]{String.valueOf(orderId)}
        );
        Order order = null;
        if (c.moveToFirst()) {
            order = readOrder(c);
        }
        c.close();
        return order;
    }

    private Order readOrder(Cursor c) {
        Order order = new Order();
        order.id = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_ID));
        order.userId = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_O_USER_ID));
        order.tongTien = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_O_TOTAL));
        order.ngayTao = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_O_CREATED));
        order.trangThaiDon = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_O_ORDER_STATUS));
        order.trangThaiThanhToan = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_O_PAYMENT_STATUS));
        order.nguoiNhan = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_O_RECEIVER));
        order.sdtNhan = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_O_PHONE));
        order.diaChiNhan = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_O_ADDRESS));
        order.phuongThucThanhToan = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_O_PAY_METHOD));
        order.ghiChu = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_O_NOTE));

        int usernameIndex = c.getColumnIndex(DBHelper.COL_USERNAME);
        if (usernameIndex >= 0) {
            order.username = c.getString(usernameIndex);
        }
        return order;
    }

    private CartItem buildSingleProductCartItem(long productId, int quantity) {
        Product product = productDao.getById(productId, true);
        if (product == null || !product.isActive) {
            return null;
        }

        CartItem item = new CartItem();
        item.productId = product.maSanPham;
        item.tenSanPham = product.tenSanPham;
        item.hang = product.hang;
        item.gia = product.gia;
        item.giamGia = product.giamGia;
        item.tonKho = product.tonKho;
        item.soLuong = Math.max(1, quantity);
        item.tenAnh = product.tenAnh;
        return item;
    }

    private ContentValues buildStockDecreaseValue(SQLiteDatabase db, long productId, int qty) {
        ContentValues values = new ContentValues();
        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_P_STOCK +
                        " FROM " + DBHelper.TBL_PRODUCTS +
                        " WHERE " + DBHelper.COL_ID + "=? LIMIT 1",
                new String[]{String.valueOf(productId)}
        );
        int stock = 0;
        if (c.moveToFirst()) {
            stock = c.getInt(0);
        }
        c.close();
        values.put(DBHelper.COL_P_STOCK, Math.max(0, stock - qty));
        return values;
    }

    private ArrayList<OrderItem> getOrderItemsInTransaction(SQLiteDatabase db, long orderId) {
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
                        " WHERE " + DBHelper.COL_OI_ORDER_ID + "=?" +
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
