package com.example.phonestore.data.model;

public class CartItem {
    public long id;           // id dòng gio_hang_ct
    public long productId;
    public String tenSanPham;
    public String hang;
    public int gia;
    public int giamGia;
    public int tonKho;
    public int soLuong;
    public String tenAnh;
    public String dungLuong;
    public String mauSac;

    public String uniqueKey() {
        String storage = dungLuong == null ? "" : dungLuong.trim();
        String color = mauSac == null ? "" : mauSac.trim();
        return productId + "|" + storage + "|" + color;
    }

    public int giaSauGiam() {
        int pct = Math.max(0, Math.min(100, giamGia));
        long discounted = (long) gia * (100 - pct) / 100;
        return (int) discounted;
    }

    public int thanhTien() {
        long amount = (long) giaSauGiam() * soLuong;
        return (int) amount;
    }
}