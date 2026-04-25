package com.example.phonestore.data.model;

public class CheckoutInfo {

    public static final String PAYMENT_COD = "COD";
    public static final String PAYMENT_BANK_TRANSFER = "CHUYEN_KHOAN";
    public static final String DISCOUNT_CODE_PHONESTORE10 = "PHONESTORE10";
    public static final int SHIPPING_FEE = 30000;
    public static final int PROMO_MIN_SUBTOTAL = 5_000_000;

    public String receiverName;
    public String receiverPhone;
    public String receiverAddress;
    public String paymentMethod;
    public String note;
    public String discountCode;
    public int discountAmount;
    public int shippingFee = SHIPPING_FEE;
    public int subtotal;
    public int totalAmount;

    public static String normalizeDiscountCode(String rawCode) {
        if (rawCode == null) return null;
        String normalized = rawCode.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    public static int calculateDiscount(String rawCode, int subtotal) {
        String code = normalizeDiscountCode(rawCode);
        if (code == null || subtotal < PROMO_MIN_SUBTOTAL) {
            return 0;
        }
        if (DISCOUNT_CODE_PHONESTORE10.equals(code)) {
            return subtotal / 10;
        }
        return 0;
    }

}
