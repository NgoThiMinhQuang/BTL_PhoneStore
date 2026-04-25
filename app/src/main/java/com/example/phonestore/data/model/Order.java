package com.example.phonestore.data.model;

public class Order {
    public static final String REFUND_STATUS_NONE = "";
    public static final String REFUND_STATUS_CHO_HOAN_TIEN = "CHO_HOAN_TIEN";
    public static final String REFUND_STATUS_DA_HOAN_TIEN = "DA_HOAN_TIEN";

    public long id;
    public long userId;
    public String username; // admin xem tất cả
    public int tongTien;
    public int tamTinh;
    public int phiVanChuyen;
    public int itemCount;
    public String maGiamGia;
    public int tienGiam;
    public long ngayTao;
    public String trangThaiDon;
    public String trangThaiThanhToan;

    public String nguoiNhan;
    public String sdtNhan;
    public String diaChiNhan;
    public String phuongThucThanhToan;
    public String ghiChu;
    public long paymentDeadline;
    public long expiredAt;
    public long cancelledAt;
    public String cancelReason;
    public String refundStatus;
    public long refundedAt;
    public String refundNote;

    public boolean hasPaymentDeadline() {
        return paymentDeadline > 0;
    }

}
