package com.example.phonestore.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.Product;

import java.util.ArrayList;

public class ProductDao {

    public static class ProductFilter {
        public String keyword;
        public String brand;
        public String operatingSystem;
        public String sortMode;
        public int minPrice;
        public int maxPrice;
        public int minRomGb;
        public int maxRomGb;
        public boolean onlyInStock;
        public boolean onlyDiscounted;
        public boolean includeInactive;
    }

    private final DBHelper dbHelper;

    public ProductDao(Context ctx) {
        dbHelper = new DBHelper(ctx);
    }

    public ArrayList<Product> layTatCa() {
        return queryProducts(DBHelper.COL_IS_ACTIVE + "=1", null, null);
    }

    public ArrayList<Product> layTatCaChoAdmin() {
        return queryProducts(null, null, null);
    }

    public ArrayList<Product> layTheoHang(String hang) {
        return queryProducts(DBHelper.COL_IS_ACTIVE + "=1 AND " + DBHelper.COL_P_BRAND + "=?", new String[]{hang}, null);
    }

    public ArrayList<Product> timKiem(String tuKhoa) {
        String k = "%" + tuKhoa + "%";
        return queryProducts(DBHelper.COL_IS_ACTIVE + "=1 AND (" + DBHelper.COL_P_NAME + " LIKE ? OR " + DBHelper.COL_P_BRAND + " LIKE ?)", new String[]{k, k}, null);
    }

    public ArrayList<Product> timKiemChoAdmin(String tuKhoa) {
        String k = "%" + tuKhoa + "%";
        return queryProducts("(" + DBHelper.COL_P_NAME + " LIKE ? OR " + DBHelper.COL_P_BRAND + " LIKE ?)", new String[]{k, k}, null);
    }

    public ArrayList<Product> timKiemTheoHang(String hang, String tuKhoa) {
        String k = "%" + tuKhoa + "%";
        return queryProducts(DBHelper.COL_IS_ACTIVE + "=1 AND " + DBHelper.COL_P_BRAND + "=? AND " + DBHelper.COL_P_NAME + " LIKE ?", new String[]{hang, k}, null);
    }

    public ArrayList<Product> locSanPham(ProductFilter filter) {
        ProductFilter safeFilter = filter == null ? new ProductFilter() : filter;
        ArrayList<String> args = new ArrayList<>();
        ArrayList<String> conditions = new ArrayList<>();

        if (!safeFilter.includeInactive) {
            conditions.add(DBHelper.COL_IS_ACTIVE + "=1");
        }

        String keyword = trimToNull(safeFilter.keyword);
        if (keyword != null) {
            String likeKeyword = "%" + keyword + "%";
            conditions.add("(" + DBHelper.COL_P_NAME + " LIKE ? OR " + DBHelper.COL_P_BRAND + " LIKE ?)");
            args.add(likeKeyword);
            args.add(likeKeyword);
        }

        String brand = trimToNull(safeFilter.brand);
        if (brand != null) {
            conditions.add(DBHelper.COL_P_BRAND + "=?");
            args.add(brand);
        }

        String operatingSystem = trimToNull(safeFilter.operatingSystem);
        if (operatingSystem != null) {
            conditions.add(DBHelper.COL_P_OS + "=?");
            args.add(operatingSystem);
        }

        if (safeFilter.minPrice > 0) {
            conditions.add(DBHelper.COL_P_PRICE + ">=?");
            args.add(String.valueOf(safeFilter.minPrice));
        }

        if (safeFilter.maxPrice > 0) {
            conditions.add(DBHelper.COL_P_PRICE + "<=?");
            args.add(String.valueOf(safeFilter.maxPrice));
        }

        if (safeFilter.minRomGb > 0) {
            conditions.add(DBHelper.COL_P_ROM_GB + ">=?");
            args.add(String.valueOf(safeFilter.minRomGb));
        }

        if (safeFilter.maxRomGb > 0) {
            conditions.add(DBHelper.COL_P_ROM_GB + "<=?");
            args.add(String.valueOf(safeFilter.maxRomGb));
        }

        if (safeFilter.onlyInStock) {
            conditions.add(DBHelper.COL_P_STOCK + ">0");
        }

        if (safeFilter.onlyDiscounted) {
            conditions.add(DBHelper.COL_P_DISCOUNT + ">0");
        }

        String whereClause = conditions.isEmpty() ? null : TextUtils.join(" AND ", conditions);
        return queryProducts(whereClause, args.isEmpty() ? null : args.toArray(new String[0]), resolveOrderBy(safeFilter.sortMode));
    }

    public Product getById(long id) {
        return getById(id, false);
    }

    public Product getById(long id, boolean includeInactive) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT * FROM " + DBHelper.TBL_PRODUCTS + " WHERE " + DBHelper.COL_ID + "=?";
        if (!includeInactive) {
            sql += " AND " + DBHelper.COL_IS_ACTIVE + "=1";
        }
        sql += " LIMIT 1";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(id)});
        Product p = null;
        if (c.moveToFirst()) p = docSanPham(c);
        c.close();
        return p;
    }

    public long insert(Product p) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(DBHelper.TBL_PRODUCTS, null, toValues(p));
    }

    public boolean update(Product p) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update(DBHelper.TBL_PRODUCTS, toValues(p),
                DBHelper.COL_ID + "=?", new String[]{String.valueOf(p.maSanPham)}) > 0;
    }

    public boolean delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_IS_ACTIVE, 0);
        return db.update(DBHelper.TBL_PRODUCTS, v,
                DBHelper.COL_ID + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1",
                new String[]{String.valueOf(id)}) > 0;
    }

    public boolean giamTonKho(SQLiteDatabase db, long productId, int qty) {
        return db.update(
                DBHelper.TBL_PRODUCTS,
                buildStockDecreaseValues(db, productId, qty),
                DBHelper.COL_ID + "=? AND " + DBHelper.COL_IS_ACTIVE + "=1 AND " + DBHelper.COL_P_STOCK + " >= ?",
                new String[]{String.valueOf(productId), String.valueOf(qty)}
        ) > 0;
    }

    private ContentValues buildStockDecreaseValues(SQLiteDatabase db, long productId, int qty) {
        Product product = getById(productId, true);
        ContentValues values = new ContentValues();
        if (product == null) {
            values.put(DBHelper.COL_P_STOCK, 0);
            return values;
        }
        values.put(DBHelper.COL_P_STOCK, Math.max(0, product.tonKho - qty));
        return values;
    }

    private ArrayList<Product> queryProducts(String whereClause, String[] args, String orderBy) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM " + DBHelper.TBL_PRODUCTS;
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        sql += " ORDER BY " + (orderBy == null || orderBy.trim().isEmpty() ? DBHelper.COL_ID + " DESC" : orderBy);
        Cursor c = db.rawQuery(sql, args);
        while (c.moveToNext()) list.add(docSanPham(c));
        c.close();
        return list;
    }

    private String resolveOrderBy(String sortMode) {
        String safeMode = trimToNull(sortMode);
        if (safeMode == null || "default".equals(safeMode)) {
            return DBHelper.COL_ID + " DESC";
        }
        if ("price_asc".equals(safeMode)) {
            return DBHelper.COL_P_PRICE + " ASC, " + DBHelper.COL_ID + " DESC";
        }
        if ("price_desc".equals(safeMode)) {
            return DBHelper.COL_P_PRICE + " DESC, " + DBHelper.COL_ID + " DESC";
        }
        if ("name_asc".equals(safeMode)) {
            return DBHelper.COL_P_NAME + " COLLATE NOCASE ASC, " + DBHelper.COL_ID + " DESC";
        }
        if ("discount_desc".equals(safeMode)) {
            return DBHelper.COL_P_DISCOUNT + " DESC, " + DBHelper.COL_P_PRICE + " DESC";
        }
        return DBHelper.COL_ID + " DESC";
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ContentValues toValues(Product p) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COL_P_NAME, p.tenSanPham);
        v.put(DBHelper.COL_P_BRAND, p.hang);
        v.put(DBHelper.COL_P_PRICE, p.gia);
        v.put(DBHelper.COL_P_STOCK, p.tonKho);
        v.put(DBHelper.COL_P_DISCOUNT, p.giamGia);
        v.put(DBHelper.COL_P_DESC, p.moTa);
        v.put(DBHelper.COL_P_IMAGE, p.tenAnh);
        v.put(DBHelper.COL_P_OS, p.heDieuHanh);
        v.put(DBHelper.COL_P_ROM_GB, p.romGb);
        v.put(DBHelper.COL_P_RAM_GB, p.ramGb);
        v.put(DBHelper.COL_P_CHIPSET, p.chipset);
        v.put(DBHelper.COL_P_SCREEN, p.manHinh);
        v.put(DBHelper.COL_P_CAMERA, p.camera);
        v.put(DBHelper.COL_P_BATTERY, p.pinMah);
        v.put(DBHelper.COL_P_COLORS, p.mauSac);
        v.put(DBHelper.COL_IS_ACTIVE, p.isActive ? 1 : 0);
        return v;
    }

    private Product docSanPham(Cursor c) {
        Product p = new Product();
        p.maSanPham = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_ID));
        p.tenSanPham = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_P_NAME));
        p.hang = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_P_BRAND));
        p.gia = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_P_PRICE));
        p.tonKho = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_P_STOCK));
        p.giamGia = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_P_DISCOUNT));
        p.moTa = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_P_DESC));
        p.tenAnh = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_P_IMAGE));
        p.heDieuHanh = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_P_OS));
        p.romGb = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_P_ROM_GB));
        p.ramGb = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_P_RAM_GB));
        p.chipset = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_P_CHIPSET));
        p.manHinh = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_P_SCREEN));
        p.camera = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_P_CAMERA));
        p.pinMah = c.getInt(c.getColumnIndexOrThrow(DBHelper.COL_P_BATTERY));
        p.mauSac = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_P_COLORS));
        int activeIndex = c.getColumnIndex(DBHelper.COL_IS_ACTIVE);
        p.isActive = activeIndex < 0 || c.getInt(activeIndex) == 1;
        return p;
    }
}
