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

    public int giaSauGiam() {
        int pct = Math.max(0, Math.min(100, giamGia));
        return gia * (100 - pct) / 100;
    }

    public int thanhTien() {
        return giaSauGiam() * soLuong;
    }
}