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
import com.example.phonestore.data.model.Receipt;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDao {

    private static final String REFERENCE_TYPE_ORDER = "ORDER";
    private static final long BANK_TRANSFER_PAYMENT_WINDOW_MS = 30L * 60L * 1000L;
    private static final String CANCEL_REASON_EXPIRED_TRANSFER = "Quá hạn thanh toán chuyển khoản";
    private static final String CANCEL_REASON_CANCELLED_TRANSFER_PAID = "Hủy đơn chuyển khoản đã thanh toán";
    private static final String CANCEL_REASON_CANCELLED_TRANSFER_PENDING = "Hủy đơn chuyển khoản chưa thanh toán";
    private static final String CANCEL_REASON_CANCELLED_COD = "Hủy đơn COD";

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
            orderValues.put(DBHelper.COL_O_ORDER_STATUS, OrderStatus.STATUS_CHO_XAC_NHAN);
            orderValues.put(DBHelper.COL_O_PAYMENT_STATUS,
                    bankTransfer ? PaymentStatus.STATUS_CHO_THANH_TOAN : PaymentStatus.STATUS_CHUA_THANH_TOAN);
            orderValues.put(DBHelper.COL_O_RECEIVER, normalizeText(info.receiverName));
            orderValues.put(DBHelper.COL_O_PHONE, normalizeText(info.receiverPhone));
            orderValues.put(DBHelper.COL_O_ADDRESS, normalizeText(info.receiverAddress));
            orderValues.put(DBHelper.COL_O_PAY_METHOD, info.paymentMethod);
            long createdAt = System.currentTimeMillis();
            long paymentDeadline = bankTransfer ? createdAt + BANK_TRANSFER_PAYMENT_WINDOW_MS : 0L;
            orderValues.put(DBHelper.COL_O_CREATED, createdAt);
            orderValues.put(DBHelper.COL_O_NOTE, normalizeOptionalText(info.note));
            orderValues.put(DBHelper.COL_O_SUBTOTAL, subtotal);
            orderValues.put(DBHelper.COL_O_SHIPPING_FEE, shippingFee);
            orderValues.put(DBHelper.COL_O_DISCOUNT_CODE, normalizeOptionalText(discountCode));
            orderValues.put(DBHelper.COL_O_DISCOUNT_AMOUNT, discountAmount);
            orderValues.put(DBHelper.COL_O_PAYMENT_DEADLINE, paymentDeadline);
            orderValues.put(DBHelper.COL_O_EXPIRED_AT, 0L);
            orderValues.put(DBHelper.COL_O_CANCELLED_AT, 0L);
            orderValues.put(DBHelper.COL_O_CANCEL_REASON, "");
            orderValues.put(DBHelper.COL_O_REFUND_STATUS, Order.REFUND_STATUS_NONE);
            orderValues.put(DBHelper.COL_O_REFUNDED_AT, 0L);
            orderValues.put(DBHelper.COL_O_REFUND_NOTE, "");

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

            ContentValues values = buildOrderStatusUpdateValues(order, newStatus);
            boolean updated = db.update(
                    DBHelper.TBL_ORDERS,
                    values,
                    DBHelper.COL_ID + "=?",
                    new String[]{String.valueOf(orderId)}
            ) > 0;
            if (!updated) {
                return false;
            }

            if (OrderStatus.STATUS_DA_HUY.equals(newStatus)
                    && !handleCancelledOrderStockAfterUpdate(db, orderId, values, order)) {
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
            if (PaymentStatus.STATUS_DA_THANH_TOAN.equals(newStatus)) {
                values.put(DBHelper.COL_O_PAYMENT_DEADLINE, 0L);
                values.put(DBHelper.COL_O_EXPIRED_AT, 0L);
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
        public int recognizedProfit;
        public int averageDeliveredOrderValue;
        public int deliveredUnits;
        public int recentSixMonthRevenue;
        public int recentSixMonthProfit;
        public int recentSixMonthDeliveredPaidOrders;
        public int recentSixMonthAverageOrderValue;
    }

    public static class RecentMonthReport {
        public int year;
        public int month;
        public String label;
        public int revenue;
        public int profit;
        public int deliveredPaidOrders;

        public RecentMonthReport(int year, int month, String label, int revenue, int profit, int deliveredPaidOrders) {
            this.year = year;
            this.month = month;
            this.label = label;
            this.revenue = revenue;
            this.profit = profit;
            this.deliveredPaidOrders = deliveredPaidOrders;
        }
    }

    public static class ReportDashboardData {
        public ReportMetrics metrics;
        public ArrayList<MonthRevenue> revenueByMonth;
        public ArrayList<RecentMonthReport> recentSixMonths;
    }

    private static class OrderProfitSnapshot {
        long orderId;
        long createdAt;
        int orderTotal;
        double estimatedCost;
    }

    private static class MonthProfitAccumulator {
        int year;
        int month;
        String label;
        int revenue;
        double profit;
        int deliveredPaidOrders;
    }

    public ReportDashboardData getReportDashboardData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ReportDashboardData data = new ReportDashboardData();
        data.revenueByMonth = getDoanhThuTheoThang(db, Calendar.getInstance().get(Calendar.YEAR));
        data.recentSixMonths = getRecentSixMonthReports(db);
        data.metrics = buildReportMetrics(db, data.recentSixMonths);
        return data;
    }

    public ArrayList<MonthRevenue> getDoanhThuTheoThang(int year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return getDoanhThuTheoThang(db, year);
    }

    private ArrayList<MonthRevenue> getDoanhThuTheoThang(SQLiteDatabase db, int year) {
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
        return getReportDashboardData().metrics;
    }

    private ReportMetrics buildReportMetrics(SQLiteDatabase db, ArrayList<RecentMonthReport> recentSixMonths) {
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
        }
        c.close();

        metrics.recognizedProfit = calculateRecognizedProfit(db);
        metrics.averageDeliveredOrderValue = metrics.deliveredPaidOrders == 0
                ? 0
                : metrics.recognizedRevenue / metrics.deliveredPaidOrders;
        metrics.recentSixMonthRevenue = sumRecentRevenue(recentSixMonths);
        metrics.recentSixMonthProfit = sumRecentProfit(recentSixMonths);
        metrics.recentSixMonthDeliveredPaidOrders = sumRecentDeliveredPaidOrders(recentSixMonths);
        metrics.recentSixMonthAverageOrderValue = metrics.recentSixMonthDeliveredPaidOrders == 0
                ? 0
                : metrics.recentSixMonthRevenue / metrics.recentSixMonthDeliveredPaidOrders;
        return metrics;
    }

    private int calculateRecognizedProfit(SQLiteDatabase db) {
        double totalProfit = 0d;
        for (OrderProfitSnapshot snapshot : getDeliveredPaidOrderSnapshots(db)) {
            totalProfit += snapshot.orderTotal - snapshot.estimatedCost;
        }
        return safeRoundCurrency(totalProfit);
    }

    private ArrayList<RecentMonthReport> getRecentSixMonthReports(SQLiteDatabase db) {
        ArrayList<RecentMonthReport> reports = new ArrayList<>();
        LinkedHashMap<String, MonthProfitAccumulator> buckets = initRecentSixMonthBuckets();
        for (OrderProfitSnapshot snapshot : getDeliveredPaidOrderSnapshots(db)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(snapshot.createdAt);
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            String key = getMonthBucketKey(year, month);
            MonthProfitAccumulator bucket = buckets.get(key);
            if (bucket == null) {
                continue;
            }
            bucket.revenue += snapshot.orderTotal;
            bucket.profit += snapshot.orderTotal - snapshot.estimatedCost;
            bucket.deliveredPaidOrders++;
        }

        for (MonthProfitAccumulator bucket : buckets.values()) {
            reports.add(new RecentMonthReport(
                    bucket.year,
                    bucket.month,
                    bucket.label,
                    bucket.revenue,
                    safeRoundCurrency(bucket.profit),
                    bucket.deliveredPaidOrders
            ));
        }
        return reports;
    }

    private LinkedHashMap<String, MonthProfitAccumulator> initRecentSixMonthBuckets() {
        LinkedHashMap<String, MonthProfitAccumulator> buckets = new LinkedHashMap<>();
        Calendar cursor = Calendar.getInstance();
        cursor.set(Calendar.DAY_OF_MONTH, 1);
        cursor.set(Calendar.HOUR_OF_DAY, 0);
        cursor.set(Calendar.MINUTE, 0);
        cursor.set(Calendar.SECOND, 0);
        cursor.set(Calendar.MILLISECOND, 0);
        cursor.add(Calendar.MONTH, -5);
        for (int i = 0; i < 6; i++) {
            MonthProfitAccumulator bucket = new MonthProfitAccumulator();
            bucket.year = cursor.get(Calendar.YEAR);
            bucket.month = cursor.get(Calendar.MONTH) + 1;
            bucket.label = String.format(Locale.US, "%02d/%d", bucket.month, bucket.year);
            buckets.put(getMonthBucketKey(bucket.year, bucket.month), bucket);
            cursor.add(Calendar.MONTH, 1);
        }
        return buckets;
    }

    private ArrayList<OrderProfitSnapshot> getDeliveredPaidOrderSnapshots(SQLiteDatabase db) {
        ArrayList<OrderProfitSnapshot> snapshots = new ArrayList<>();
        Map<Long, Double> weightedAverageCostByProduct = getWeightedAverageCostByProduct(db);
        Cursor c = db.rawQuery(
                "SELECT o." + DBHelper.COL_ID + ", o." + DBHelper.COL_O_CREATED + ", o." + DBHelper.COL_O_TOTAL + ", " +
                        "oi." + DBHelper.COL_OI_PRODUCT_ID + ", oi." + DBHelper.COL_OI_QTY + " " +
                        "FROM " + DBHelper.TBL_ORDERS + " o " +
                        "JOIN " + DBHelper.TBL_ORDER_ITEMS + " oi ON oi." + DBHelper.COL_OI_ORDER_ID + " = o." + DBHelper.COL_ID + " " +
                        "WHERE o." + DBHelper.COL_O_ORDER_STATUS + "=? AND o." + DBHelper.COL_O_PAYMENT_STATUS + "=? " +
                        "ORDER BY o." + DBHelper.COL_O_CREATED + " ASC, o." + DBHelper.COL_ID + " ASC, oi." + DBHelper.COL_ID + " ASC",
                new String[]{OrderStatus.STATUS_DA_GIAO, PaymentStatus.STATUS_DA_THANH_TOAN}
        );

        OrderProfitSnapshot current = null;
        long currentOrderId = -1L;
        while (c.moveToNext()) {
            long orderId = c.getLong(0);
            if (current == null || currentOrderId != orderId) {
                if (current != null) {
                    snapshots.add(current);
                }
                current = new OrderProfitSnapshot();
                current.orderId = orderId;
                current.createdAt = c.getLong(1);
                current.orderTotal = c.getInt(2);
                current.estimatedCost = 0d;
                currentOrderId = orderId;
            }
            long productId = c.getLong(3);
            int quantity = c.getInt(4);
            double unitCost = 0d;
            Double weightedCost = weightedAverageCostByProduct.get(productId);
            if (weightedCost != null) {
                unitCost = weightedCost;
            }
            current.estimatedCost += unitCost * quantity;
        }
        c.close();
        if (current != null) {
            snapshots.add(current);
        }
        return snapshots;
    }

    private Map<Long, Double> getWeightedAverageCostByProduct(SQLiteDatabase db) {
        LinkedHashMap<Long, Double> costs = new LinkedHashMap<>();
        Cursor c = db.rawQuery(
                "SELECT ri." + DBHelper.COL_RI_PRODUCT_ID + ", " +
                        "SUM(ri." + DBHelper.COL_RI_QTY + " * ri." + DBHelper.COL_RI_UNIT_COST + ") * 1.0 / NULLIF(SUM(ri." + DBHelper.COL_RI_QTY + "), 0) AS weighted_cost " +
                        "FROM " + DBHelper.TBL_RECEIPT_ITEMS + " ri " +
                        "JOIN " + DBHelper.TBL_RECEIPTS + " r ON r." + DBHelper.COL_ID + " = ri." + DBHelper.COL_RI_RECEIPT_ID + " " +
                        "WHERE r." + DBHelper.COL_R_STATUS + "=? " +
                        "GROUP BY ri." + DBHelper.COL_RI_PRODUCT_ID,
                new String[]{Receipt.STATUS_COMPLETED}
        );
        while (c.moveToNext()) {
            costs.put(c.getLong(0), c.isNull(1) ? 0d : c.getDouble(1));
        }
        c.close();
        return costs;
    }

    private int sumRecentRevenue(ArrayList<RecentMonthReport> recentSixMonths) {
        int total = 0;
        for (RecentMonthReport item : recentSixMonths) {
            total += Math.max(0, item.revenue);
        }
        return total;
    }

    private int sumRecentProfit(ArrayList<RecentMonthReport> recentSixMonths) {
        int total = 0;
        for (RecentMonthReport item : recentSixMonths) {
            total += item.profit;
        }
        return total;
    }

    private int sumRecentDeliveredPaidOrders(ArrayList<RecentMonthReport> recentSixMonths) {
        int total = 0;
        for (RecentMonthReport item : recentSixMonths) {
            total += Math.max(0, item.deliveredPaidOrders);
        }
        return total;
    }

    private String getMonthBucketKey(int year, int month) {
        return year + "-" + month;
    }

    private int safeRoundCurrency(double amount) {
        return (int) Math.round(amount);
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

    public int reconcileExpiredTransferOrders() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int reconciled = reconcileExpiredTransferOrders(db, System.currentTimeMillis());
            db.setTransactionSuccessful();
            return reconciled;
        } finally {
            db.endTransaction();
        }
    }

    private int reconcileExpiredTransferOrders(SQLiteDatabase db, long now) {
        ArrayList<Long> expiredOrderIds = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT " + DBHelper.COL_ID +
                        " FROM " + DBHelper.TBL_ORDERS +
                        " WHERE " + DBHelper.COL_O_PAY_METHOD + "=?" +
                        " AND " + DBHelper.COL_O_ORDER_STATUS + "=?" +
                        " AND " + DBHelper.COL_O_PAYMENT_STATUS + "=?" +
                        " AND " + DBHelper.COL_O_PAYMENT_DEADLINE + ">0" +
                        " AND " + DBHelper.COL_O_PAYMENT_DEADLINE + "<=?" +
                        " AND " + DBHelper.COL_O_EXPIRED_AT + "=0",
                new String[]{
                        CheckoutInfo.PAYMENT_BANK_TRANSFER,
                        OrderStatus.STATUS_CHO_XAC_NHAN,
                        PaymentStatus.STATUS_CHO_THANH_TOAN,
                        String.valueOf(now)
                }
        );
        while (c.moveToNext()) {
            expiredOrderIds.add(c.getLong(0));
        }
        c.close();

        int reconciled = 0;
        for (Long orderId : expiredOrderIds) {
            Order order = getOrderByIdInTransaction(db, orderId == null ? -1L : orderId);
            if (order == null || !shouldExpireTransferOrder(order, now)) {
                continue;
            }

            ContentValues values = new ContentValues();
            values.put(DBHelper.COL_O_ORDER_STATUS, OrderStatus.STATUS_DA_HUY);
            values.put(DBHelper.COL_O_PAYMENT_STATUS, PaymentStatus.STATUS_HET_HAN_THANH_TOAN);
            values.put(DBHelper.COL_O_EXPIRED_AT, now);
            values.put(DBHelper.COL_O_CANCELLED_AT, now);
            values.put(DBHelper.COL_O_CANCEL_REASON, CANCEL_REASON_EXPIRED_TRANSFER);
            values.put(DBHelper.COL_O_PAYMENT_DEADLINE, 0L);
            values.put(DBHelper.COL_O_REFUND_STATUS, Order.REFUND_STATUS_NONE);
            values.put(DBHelper.COL_O_REFUNDED_AT, 0L);
            values.put(DBHelper.COL_O_REFUND_NOTE, "");

            boolean updated = db.update(
                    DBHelper.TBL_ORDERS,
                    values,
                    DBHelper.COL_ID + "=?",
                    new String[]{String.valueOf(order.id)}
            ) > 0;
            if (!updated) {
                continue;
            }
            if (restoreStockForOrder(db, order.id, getCancelRestockNote(order, true))) {
                reconciled++;
            }
        }
        return reconciled;
    }

    private boolean shouldExpireTransferOrder(Order order, long now) {
        return order != null
                && isBankTransfer(order)
                && OrderStatus.STATUS_CHO_XAC_NHAN.equals(order.trangThaiDon)
                && PaymentStatus.STATUS_CHO_THANH_TOAN.equals(order.trangThaiThanhToan)
                && order.paymentDeadline > 0
                && order.paymentDeadline <= now
                && order.expiredAt <= 0;
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
                || PaymentStatus.STATUS_DA_THANH_TOAN.equals(status)
                || PaymentStatus.STATUS_HET_HAN_THANH_TOAN.equals(status);
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
        if (!phone.matches("0\\d{9}")) {
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

    private ContentValues buildOrderStatusUpdateValues(Order order, String newStatus) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_O_ORDER_STATUS, newStatus);
        if (OrderStatus.STATUS_DA_GIAO.equals(newStatus)) {
            values.put(DBHelper.COL_O_PAYMENT_DEADLINE, 0L);
            values.put(DBHelper.COL_O_EXPIRED_AT, 0L);
            if (isCod(order) && !PaymentStatus.STATUS_DA_THANH_TOAN.equals(order.trangThaiThanhToan)) {
                values.put(DBHelper.COL_O_PAYMENT_STATUS, PaymentStatus.STATUS_DA_THANH_TOAN);
            }
        }
        if (OrderStatus.STATUS_DA_HUY.equals(newStatus)) {
            long cancelledAt = System.currentTimeMillis();
            values.put(DBHelper.COL_O_CANCELLED_AT, cancelledAt);
            values.put(DBHelper.COL_O_PAYMENT_DEADLINE, 0L);
            values.put(DBHelper.COL_O_EXPIRED_AT, order.expiredAt);
            values.put(DBHelper.COL_O_CANCEL_REASON, getCancelReason(order));
            if (isBankTransfer(order) && PaymentStatus.STATUS_DA_THANH_TOAN.equals(order.trangThaiThanhToan)) {
                values.put(DBHelper.COL_O_REFUND_STATUS, Order.REFUND_STATUS_CHO_HOAN_TIEN);
                values.put(DBHelper.COL_O_REFUND_NOTE, buildRefundNote(order));
                values.put(DBHelper.COL_O_REFUNDED_AT, 0L);
            } else {
                values.put(DBHelper.COL_O_REFUND_STATUS, Order.REFUND_STATUS_NONE);
                values.put(DBHelper.COL_O_REFUND_NOTE, "");
                values.put(DBHelper.COL_O_REFUNDED_AT, 0L);
            }
        }
        return values;
    }

    private boolean handleCancelledOrderStockAfterUpdate(SQLiteDatabase db, long orderId, ContentValues values, Order order) {
        boolean alreadyCancelledBefore = order != null && OrderStatus.STATUS_DA_HUY.equals(order.trangThaiDon);
        boolean alreadyExpiredBefore = order != null && order.expiredAt > 0;
        if (alreadyCancelledBefore || alreadyExpiredBefore) {
            return true;
        }
        return restoreStockForOrder(db, orderId, getCancelRestockNote(order, false));
    }

    private String getCancelReason(Order order) {
        if (isBankTransfer(order) && PaymentStatus.STATUS_DA_THANH_TOAN.equals(order.trangThaiThanhToan)) {
            return CANCEL_REASON_CANCELLED_TRANSFER_PAID;
        }
        if (isBankTransfer(order)) {
            return CANCEL_REASON_CANCELLED_TRANSFER_PENDING;
        }
        return CANCEL_REASON_CANCELLED_COD;
    }

    private String buildRefundNote(Order order) {
        String base = getCancelReason(order);
        if (order == null || order.ghiChu == null || order.ghiChu.trim().isEmpty()) {
            return base;
        }
        return base + ". Ghi chú đơn: " + order.ghiChu.trim();
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
                .append("o.").append(DBHelper.COL_O_NOTE).append(",")
                .append("o.").append(DBHelper.COL_O_PAYMENT_DEADLINE).append(",")
                .append("o.").append(DBHelper.COL_O_EXPIRED_AT).append(",")
                .append("o.").append(DBHelper.COL_O_CANCELLED_AT).append(",")
                .append("o.").append(DBHelper.COL_O_CANCEL_REASON).append(",")
                .append("o.").append(DBHelper.COL_O_REFUND_STATUS).append(",")
                .append("o.").append(DBHelper.COL_O_REFUNDED_AT).append(",")
                .append("o.").append(DBHelper.COL_O_REFUND_NOTE).append(",")
                .append("(SELECT IFNULL(SUM(").append(DBHelper.COL_OI_QTY).append("), 0) FROM ")
                .append(DBHelper.TBL_ORDER_ITEMS)
                .append(" oi WHERE oi.").append(DBHelper.COL_OI_ORDER_ID).append(" = o.").append(DBHelper.COL_ID)
                .append(") AS item_count");
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
        order.paymentDeadline = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_O_PAYMENT_DEADLINE));
        order.expiredAt = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_O_EXPIRED_AT));
        order.cancelledAt = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_O_CANCELLED_AT));
        order.cancelReason = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_O_CANCEL_REASON));
        order.refundStatus = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_O_REFUND_STATUS));
        order.refundedAt = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_O_REFUNDED_AT));
        order.refundNote = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_O_REFUND_NOTE));

        int itemCountIndex = c.getColumnIndex("item_count");
        if (itemCountIndex >= 0) {
            order.itemCount = c.getInt(itemCountIndex);
        }

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
        if (hasRestockHistory(db, orderId)) {
            return true;
        }
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

    private boolean hasRestockHistory(SQLiteDatabase db, long orderId) {
        Cursor c = db.rawQuery(
                "SELECT 1 FROM " + DBHelper.TBL_INVENTORY_HISTORY +
                        " WHERE " + DBHelper.COL_H_REF_TYPE + "=?" +
                        " AND " + DBHelper.COL_H_REF_ID + "=?" +
                        " AND " + DBHelper.COL_H_ACTION + "=? LIMIT 1",
                new String[]{REFERENCE_TYPE_ORDER, String.valueOf(orderId), InventoryHistoryDao.ACTION_CANCEL_RETURN}
        );
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    private String getCancelRestockNote(Order order, boolean expiredTransfer) {
        if (expiredTransfer) {
            return "Hoàn kho do đơn chuyển khoản hết hạn thanh toán";
        }
        if (isBankTransfer(order) && PaymentStatus.STATUS_DA_THANH_TOAN.equals(order.trangThaiThanhToan)) {
            return "Hoàn kho do hủy đơn chuyển khoản đã thanh toán, chờ hoàn tiền";
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
