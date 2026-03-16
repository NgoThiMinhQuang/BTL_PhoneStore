package com.example.phonestore.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.example.phonestore.data.db.DBHelper;
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
        return checkout(userId, info);
    }

    public long checkout(long userId, CheckoutInfo info) {
        ArrayList<CartItem> items = cartDao.getCartItems(userId);
        return checkoutWithItems(userId, info, items, false, null);
    }

    public long checkout(long userId, CheckoutInfo info, List<Long> selectedCartItemIds) {
        ArrayList<CartItem> items = cartDao.getCartItemsByIds(userId, selectedCartItemIds);
        return checkoutWithItems(userId, info, items, true, selectedCartItemIds);
    }

    public long checkoutSingleProduct(long userId, long productId, int quantity, CheckoutInfo info) {
        return checkoutSingleProduct(userId, productId, quantity, "", "", info);
    }

    public long checkoutSingleProduct(long userId, long productId, int quantity, String storage, String color, CheckoutInfo info) {
        CartItem item = buildSingleProductCartItem(productId, quantity, storage, color);
        if (item == null) {
            setLastCheckoutError("Không tìm thấy sản phẩm");
            return -1;
        }

        ArrayList<CartItem> items = new ArrayList<>();
        items.add(item);
        return checkoutWithItems(userId, info, items, false, null);
    }

    public int previewSingleProductTotal(long productId, int quantity) {
        CartItem item = buildSingleProductCartItem(productId, quantity, "", "");
        return item == null ? 0 : item.thanhTien();
    }

    private long checkoutWithItems(long userId, CheckoutInfo info, ArrayList<CartItem> items,
                                   boolean selectedOnly, List<Long> selectedCartItemIds) {
        clearLastCheckoutError();

        String checkoutError = validateCheckoutInfo(info);
        if (checkoutError != null) {
            setLastCheckoutError(checkoutError);
            return -1;
        }

        if (items == null || items.isEmpty()) {
            setLastCheckoutError("Giỏ hàng trống hoặc sản phẩm đã bị bỏ chọn");
            return -1;
        }

        int subtotal = 0;
        for (CartItem it : items) {
            if (it.productId <= 0) {
                setLastCheckoutError("Sản phẩm không hợp lệ");
                return -1;
            }
            if (it.soLuong <= 0) {
                setLastCheckoutError("Số lượng sản phẩm không hợp lệ");
                return -1;
            }
            if (it.soLuong > it.tonKho) {
                setLastCheckoutError("Sản phẩm " + it.tenSanPham + " không đủ tồn kho");
                return -1;
            }
            subtotal += it.thanhTien();
        }

        int shippingFee = subtotal > 0 ? CheckoutInfo.SHIPPING_FEE : 0;
        String discountCode = CheckoutInfo.normalizeDiscountCode(info.discountCode);
        int discountAmount = CheckoutInfo.calculateDiscount(discountCode, subtotal);
        int total = Math.max(0, subtotal + shippingFee - discountAmount);
        boolean bankTransfer = CheckoutInfo.PAYMENT_BANK_TRANSFER.equals(info.paymentMethod);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues orderValues = new ContentValues();
            orderValues.put(DBHelper.COL_O_USER_ID, userId);
            orderValues.put(DBHelper.COL_O_TOTAL, total);
            orderValues.put(DBHelper.COL_O_CREATED, System.currentTimeMillis());
            orderValues.put(DBHelper.COL_O_ORDER_STATUS, OrderStatus.STATUS_CHO_XAC_NHAN);
            orderValues.put(DBHelper.COL_O_PAYMENT_STATUS,
                    bankTransfer ? PaymentStatus.STATUS_CHO_THANH_TOAN : PaymentStatus.STATUS_CHUA_THANH_TOAN);
            orderValues.put(DBHelper.COL_O_RECEIVER, normalizeText(info.receiverName));
            orderValues.put(DBHelper.COL_O_PHONE, normalizeText(info.receiverPhone));
            orderValues.put(DBHelper.COL_O_ADDRESS, normalizeText(info.receiverAddress));
            orderValues.put(DBHelper.COL_O_PAY_METHOD, info.paymentMethod);
            orderValues.put(DBHelper.COL_O_NOTE, normalizeOptionalText(info.note));
            orderValues.put(DBHelper.COL_O_SUBTOTAL, subtotal);
            orderValues.put(DBHelper.COL_O_SHIPPING_FEE, shippingFee);
            orderValues.put(DBHelper.COL_O_DISCOUNT_CODE, normalizeOptionalText(discountCode));
            orderValues.put(DBHelper.COL_O_DISCOUNT_AMOUNT, discountAmount);

            long orderId = db.insert(DBHelper.TBL_ORDERS, null, orderValues);
            if (orderId == -1) {
                setLastCheckoutError("Không tạo được đơn hàng");
                return -1;
            }

            for (CartItem item : items) {
                if (!reserveStockForItem(db, item)) {
                    setLastCheckoutError("Không thể giữ tồn kho cho sản phẩm " + item.tenSanPham);
                    return -1;
                }

                ContentValues orderItemValues = new ContentValues();
                orderItemValues.put(DBHelper.COL_OI_ORDER_ID, orderId);
                orderItemValues.put(DBHelper.COL_OI_PRODUCT_ID, item.productId);
                orderItemValues.put(DBHelper.COL_OI_NAME, item.tenSanPham);
                orderItemValues.put(DBHelper.COL_OI_PRICE, item.gia);
                orderItemValues.put(DBHelper.COL_OI_DISCOUNT, item.giamGia);
                orderItemValues.put(DBHelper.COL_OI_QTY, item.soLuong);
                orderItemValues.put(DBHelper.COL_OI_AMOUNT, item.thanhTien());
                orderItemValues.put(DBHelper.COL_OI_STORAGE, normalizeOptionalText(item.dungLuong));
                orderItemValues.put(DBHelper.COL_OI_COLOR, normalizeOptionalText(item.mauSac));
                if (db.insert(DBHelper.TBL_ORDER_ITEMS, null, orderItemValues) == -1) {
                    setLastCheckoutError("Không tạo được chi tiết đơn hàng");
                    return -1;
                }

                historyDao.insert(
                        db,
                        item.productId,
                        item.tenSanPham,
                        InventoryHistoryDao.ACTION_EXPORT,
                        item.soLuong,
                        REFERENCE_TYPE_ORDER,
                        orderId,
                        bankTransfer ? "Giữ hàng cho đơn chuyển khoản" : "Giữ hàng cho đơn COD"
                );
            }

            if (selectedOnly) {
                deleteCartItemsInTransaction(db, userId, selectedCartItemIds);
            } else {
                clearCartInTransaction(db, userId);
            }

            db.setTransactionSuccessful();
            clearLastCheckoutError();
            return orderId;
        } catch (Exception e) {
            String msg = e.getMessage();
            setLastCheckoutError("Lỗi đặt hàng: " + (msg == null ? "không xác định" : msg));
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    public Order getOrderById(long orderId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(buildOrderSelect(null) +
                        " WHERE o." + DBHelper.COL_ID + "=? LIMIT 1",
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
                buildOrderSelect(null) +
                        " WHERE o." + DBHelper.COL_O_USER_ID + "=?" +
                        " ORDER BY o." + DBHelper.COL_ID + " DESC",
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
                buildOrderSelect("u." + DBHelper.COL_USERNAME + " AS " + DBHelper.COL_USERNAME) +
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
        return getOrderItemsInTransaction(db, orderId);
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

            if (OrderStatus.STATUS_DA_HUY.equals(newStatus) && !restoreStockForOrder(db, orderId, getCancelRestockNote(order))) {
                return false;
            }

            ContentValues values = new ContentValues();
            values.put(DBHelper.COL_O_ORDER_STATUS, newStatus);
            if (OrderStatus.STATUS_DA_GIAO.equals(newStatus)
                    && isCod(order)
                    && !PaymentStatus.STATUS_DA_THANH_TOAN.equals(order.trangThaiThanhToan)) {
                values.put(DBHelper.COL_O_PAYMENT_STATUS, PaymentStatus.STATUS_DA_THANH_TOAN);
            }

            boolean updated = db.update(
                    DBHelper.TBL_ORDERS,
                    values,
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

            ContentValues values = new ContentValues();
            values.put(DBHelper.COL_O_PAYMENT_STATUS, newStatus);
            boolean updated = db.update(
                    DBHelper.TBL_ORDERS,
                    values,
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
                        " WHERE " + DBHelper.COL_O_ORDER_STATUS + "=? AND " + DBHelper.COL_O_PAYMENT_STATUS + "=?",
                new String[]{OrderStatus.STATUS_DA_GIAO, PaymentStatus.STATUS_DA_THANH_TOAN}
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
        public long productId;
        public String name;
        public int qty;

        public ProductSale(long productId, String name, int qty) {
            this.productId = productId;
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

    public static class MonthCount {
        public int month;
        public int count;

        public MonthCount(int month, int count) {
            this.month = month;
            this.count = count;
        }
    }

    public static class ReportMetrics {
        public int totalOrders;
        public int deliveredPaidOrders;
        public int waitingPaymentOrders;
        public int cancelledOrders;
        public int recognizedRevenue;
        public int averageDeliveredOrderValue;
        public int deliveredUnits;
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
                        "AND " + DBHelper.COL_O_PAYMENT_STATUS + "=? " +
                        "GROUP BY m ORDER BY m",
                new String[]{String.valueOf(year), OrderStatus.STATUS_DA_GIAO, PaymentStatus.STATUS_DA_THANH_TOAN}
        );

        while (c.moveToNext()) {
            list.add(new MonthRevenue(c.isNull(0) ? 0 : c.getInt(0), c.isNull(1) ? 0 : c.getInt(1)));
        }
        c.close();
        return list;
    }

    public ArrayList<ProductSale> getTopSanPhamBanChay(int limit) {
        ArrayList<ProductSale> list = new ArrayList<>();
        if (limit <= 0) return list;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT oi." + DBHelper.COL_OI_PRODUCT_ID + ", MAX(oi." + DBHelper.COL_OI_NAME + "), SUM(oi." + DBHelper.COL_OI_QTY + ") AS qty " +
                        "FROM " + DBHelper.TBL_ORDER_ITEMS + " oi " +
                        "JOIN " + DBHelper.TBL_ORDERS + " o ON o." + DBHelper.COL_ID + " = oi." + DBHelper.COL_OI_ORDER_ID + " " +
                        "WHERE o." + DBHelper.COL_O_ORDER_STATUS + "=? AND o." + DBHelper.COL_O_PAYMENT_STATUS + "=? " +
                        "GROUP BY oi." + DBHelper.COL_OI_PRODUCT_ID + " " +
                        "ORDER BY qty DESC, MAX(oi." + DBHelper.COL_OI_NAME + ") ASC " +
                        "LIMIT " + limit,
                new String[]{OrderStatus.STATUS_DA_GIAO, PaymentStatus.STATUS_DA_THANH_TOAN}
        );

        while (c.moveToNext()) {
            list.add(new ProductSale(c.getLong(0), c.getString(1), c.isNull(2) ? 0 : c.getInt(2)));
        }
        c.close();
        return list;
    }

    public ArrayList<StatusCount> getSoDonTheoTrangThai() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<StatusCount> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_O_ORDER_STATUS + ", COUNT(*) AS cnt FROM " + DBHelper.TBL_ORDERS +
                        " GROUP BY " + DBHelper.COL_O_ORDER_STATUS +
                        " ORDER BY CASE " + DBHelper.COL_O_ORDER_STATUS +
                        " WHEN '" + OrderStatus.STATUS_CHO_XAC_NHAN + "' THEN 1" +
                        " WHEN '" + OrderStatus.STATUS_DANG_XU_LY + "' THEN 2" +
                        " WHEN '" + OrderStatus.STATUS_DA_GIAO + "' THEN 3" +
                        " WHEN '" + OrderStatus.STATUS_DA_HUY + "' THEN 4" +
                        " ELSE 5 END",
                null
        );

        while (c.moveToNext()) {
            list.add(new StatusCount(c.getString(0), c.isNull(1) ? 0 : c.getInt(1)));
        }
        c.close();
        return list;
    }

    public ArrayList<StatusCount> getSoDonTheoTrangThaiThanhToan() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<StatusCount> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_O_PAYMENT_STATUS + ", COUNT(*) AS cnt FROM " + DBHelper.TBL_ORDERS +
                        " GROUP BY " + DBHelper.COL_O_PAYMENT_STATUS +
                        " ORDER BY CASE " + DBHelper.COL_O_PAYMENT_STATUS +
                        " WHEN '" + PaymentStatus.STATUS_CHO_THANH_TOAN + "' THEN 1" +
                        " WHEN '" + PaymentStatus.STATUS_CHUA_THANH_TOAN + "' THEN 2" +
                        " WHEN '" + PaymentStatus.STATUS_DA_THANH_TOAN + "' THEN 3" +
                        " ELSE 4 END",
                null
        );

        while (c.moveToNext()) {
            list.add(new StatusCount(c.getString(0), c.isNull(1) ? 0 : c.getInt(1)));
        }
        c.close();
        return list;
    }

    public ArrayList<MonthCount> getSoDonTheoThang(int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<MonthCount> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT " +
                        "CAST(strftime('%m', datetime(" + DBHelper.COL_O_CREATED + "/1000, 'unixepoch')) AS INTEGER) AS m, " +
                        "COUNT(*) AS total " +
                        "FROM " + DBHelper.TBL_ORDERS + " " +
                        "WHERE strftime('%Y', datetime(" + DBHelper.COL_O_CREATED + "/1000, 'unixepoch')) = ? " +
                        "GROUP BY m ORDER BY m",
                new String[]{String.valueOf(year)}
        );
        while (c.moveToNext()) {
            list.add(new MonthCount(c.isNull(0) ? 0 : c.getInt(0), c.isNull(1) ? 0 : c.getInt(1)));
        }
        c.close();
        return list;
    }

    public ReportMetrics getReportMetrics() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ReportMetrics metrics = new ReportMetrics();
        Cursor c = db.rawQuery(
                "SELECT " +
                        "COUNT(*) AS total_orders, " +
                        "SUM(CASE WHEN " + DBHelper.COL_O_ORDER_STATUS + "=? AND " + DBHelper.COL_O_PAYMENT_STATUS + "=? THEN 1 ELSE 0 END) AS delivered_paid_orders, " +
                        "SUM(CASE WHEN " + DBHelper.COL_O_PAYMENT_STATUS + "=? AND " + DBHelper.COL_O_ORDER_STATUS + "<>? THEN 1 ELSE 0 END) AS waiting_payment_orders, " +
                        "SUM(CASE WHEN " + DBHelper.COL_O_ORDER_STATUS + "=? THEN 1 ELSE 0 END) AS cancelled_orders, " +
                        "SUM(CASE WHEN " + DBHelper.COL_O_ORDER_STATUS + "=? AND " + DBHelper.COL_O_PAYMENT_STATUS + "=? THEN " + DBHelper.COL_O_TOTAL + " ELSE 0 END) AS recognized_revenue, " +
                        "IFNULL((SELECT SUM(oi." + DBHelper.COL_OI_QTY + ") FROM " + DBHelper.TBL_ORDER_ITEMS + " oi " +
                        "JOIN " + DBHelper.TBL_ORDERS + " o2 ON o2." + DBHelper.COL_ID + " = oi." + DBHelper.COL_OI_ORDER_ID + " " +
                        "WHERE o2." + DBHelper.COL_O_ORDER_STATUS + "='" + OrderStatus.STATUS_DA_GIAO + "' AND o2." + DBHelper.COL_O_PAYMENT_STATUS + "='" + PaymentStatus.STATUS_DA_THANH_TOAN + "'), 0) AS delivered_units " +
                        "FROM " + DBHelper.TBL_ORDERS,
                new String[]{
                        OrderStatus.STATUS_DA_GIAO,
                        PaymentStatus.STATUS_DA_THANH_TOAN,
                        PaymentStatus.STATUS_CHO_THANH_TOAN,
                        OrderStatus.STATUS_DA_HUY,
                        OrderStatus.STATUS_DA_HUY,
                        OrderStatus.STATUS_DA_GIAO,
                        PaymentStatus.STATUS_DA_THANH_TOAN
                }
        );

        if (c.moveToFirst()) {
            metrics.totalOrders = c.isNull(0) ? 0 : c.getInt(0);
            metrics.deliveredPaidOrders = c.isNull(1) ? 0 : c.getInt(1);
            metrics.waitingPaymentOrders = c.isNull(2) ? 0 : c.getInt(2);
            metrics.cancelledOrders = c.isNull(3) ? 0 : c.getInt(3);
            metrics.recognizedRevenue = c.isNull(4) ? 0 : c.getInt(4);
            metrics.deliveredUnits = c.isNull(5) ? 0 : c.getInt(5);
            metrics.averageDeliveredOrderValue = metrics.deliveredPaidOrders == 0
                    ? 0
                    : metrics.recognizedRevenue / metrics.deliveredPaidOrders;
        }
        c.close();
        return metrics;
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
                return isCod(order) || PaymentStatus.STATUS_DA_THANH_TOAN.equals(order.trangThaiThanhToan);
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
        if (TextUtils.equals(order.trangThaiThanhToan, newStatus) || isFinalOrderStatus(order.trangThaiDon)) {
            return false;
        }
        if (!isBankTransfer(order)) {
            return false;
        }
        return PaymentStatus.STATUS_DA_THANH_TOAN.equals(newStatus)
                && PaymentStatus.STATUS_CHO_THANH_TOAN.equals(order.trangThaiThanhToan);
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

    private String buildOrderSelect(String extraColumns) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ")
                .append("o.").append(DBHelper.COL_ID).append(",")
                .append("o.").append(DBHelper.COL_O_USER_ID).append(",")
                .append("o.").append(DBHelper.COL_O_TOTAL).append(",")
                .append("o.").append(DBHelper.COL_O_SUBTOTAL).append(",")
                .append("o.").append(DBHelper.COL_O_SHIPPING_FEE).append(",")
                .append("o.").append(DBHelper.COL_O_DISCOUNT_CODE).append(",")
                .append("o.").append(DBHelper.COL_O_DISCOUNT_AMOUNT).append(",")
                .append("o.").append(DBHelper.COL_O_CREATED).append(",")
                .append("o.").append(DBHelper.COL_O_ORDER_STATUS).append(",")
                .append("o.").append(DBHelper.COL_O_PAYMENT_STATUS).append(",")
                .append("o.").append(DBHelper.COL_O_RECEIVER).append(",")
                .append("o.").append(DBHelper.COL_O_PHONE).append(",")
                .append("o.").append(DBHelper.COL_O_ADDRESS).append(",")
                .append("o.").append(DBHelper.COL_O_PAY_METHOD).append(",")
                .append("o.").append(DBHelper.COL_O_NOTE);
        if (!TextUtils.isEmpty(extraColumns)) {
            builder.append(",").append(extraColumns);
        }
        builder.append(" FROM ").append(DBHelper.TBL_ORDERS).append(" o");
        return builder.toString();
    }

    private Order getOrderByIdInTransaction(SQLiteDatabase db, long orderId) {
        Cursor c = db.rawQuery(
                buildOrderSelect(null) + " WHERE o." + DBHelper.COL_ID + "=? LIMIT 1",
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
        order.tamTinh = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_O_SUBTOTAL));
        order.phiVanChuyen = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_O_SHIPPING_FEE));
        order.maGiamGia = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_O_DISCOUNT_CODE));
        order.tienGiam = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_O_DISCOUNT_AMOUNT));
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

    private CartItem buildSingleProductCartItem(long productId, int quantity, String storage, String color) {
        Product product = productDao.getById(productId);
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
        item.dungLuong = normalizeOptionalText(storage);
        item.mauSac = normalizeOptionalText(color);
        return item;
    }

    private boolean reserveStockForItem(SQLiteDatabase db, CartItem item) {
        int currentStock = getCurrentStock(db, item.productId);
        if (currentStock < item.soLuong) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_P_STOCK, currentStock - item.soLuong);
        return db.update(
                DBHelper.TBL_PRODUCTS,
                values,
                DBHelper.COL_ID + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1 AND " + DBHelper.COL_P_STOCK + " >= ?",
                new String[]{String.valueOf(item.productId), String.valueOf(item.soLuong)}
        ) > 0;
    }

    private int getCurrentStock(SQLiteDatabase db, long productId) {
        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_P_STOCK +
                        " FROM " + DBHelper.TBL_PRODUCTS +
                        " WHERE " + DBHelper.COL_ID + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1 LIMIT 1",
                new String[]{String.valueOf(productId)}
        );
        int stock = 0;
        if (c.moveToFirst()) {
            stock = c.getInt(0);
        }
        c.close();
        return stock;
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
                        DBHelper.COL_OI_AMOUNT + "," +
                        DBHelper.COL_OI_STORAGE + "," +
                        DBHelper.COL_OI_COLOR +
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
            it.dungLuong = c.getString(8);
            it.mauSac = c.getString(9);
            list.add(it);
        }
        c.close();
        return list;
    }

    private boolean restoreStockForOrder(SQLiteDatabase db, long orderId, String note) {
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
                    note
            );
        }
        return true;
    }

    private String getCancelRestockNote(Order order) {
        if (isBankTransfer(order) && PaymentStatus.STATUS_DA_THANH_TOAN.equals(order.trangThaiThanhToan)) {
            return "Hoàn kho do hủy đơn chuyển khoản đã xác nhận thanh toán";
        }
        if (isBankTransfer(order)) {
            return "Hoàn kho do hủy đơn chuyển khoản đang giữ hàng";
        }
        return "Hoàn kho do hủy đơn COD";
    }

    private void clearCartInTransaction(SQLiteDatabase db, long userId) {
        db.delete(
                DBHelper.TBL_CART,
                DBHelper.COL_C_USER_ID + "=?",
                new String[]{String.valueOf(userId)}
        );
    }

    private void deleteCartItemsInTransaction(SQLiteDatabase db, long userId, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) return;

        StringBuilder inClause = new StringBuilder();
        String[] args = new String[cartItemIds.size() + 1];
        args[0] = String.valueOf(userId);

        for (int i = 0; i < cartItemIds.size(); i++) {
            if (i > 0) inClause.append(",");
            inClause.append("?");
            args[i + 1] = String.valueOf(cartItemIds.get(i));
        }

        db.delete(
                DBHelper.TBL_CART,
                DBHelper.COL_C_USER_ID + "=? AND " + DBHelper.COL_ID + " IN (" + inClause + ")",
                args
        );
    }

    private boolean isCod(Order order) {
        return order != null && CheckoutInfo.PAYMENT_COD.equals(order.phuongThucThanhToan);
    }

    private boolean isBankTransfer(Order order) {
        return order != null && CheckoutInfo.PAYMENT_BANK_TRANSFER.equals(order.phuongThucThanhToan);
    }
}
