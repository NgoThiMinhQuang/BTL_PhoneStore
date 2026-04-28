package com.example.phonestore.utils;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.phonestore.R;

import java.util.Locale;

public final class ProductImageLoader {

    private ProductImageLoader() {
    }

    public static void load(ImageView imageView, String imageRef, String productName, String brand) {
        Context context = imageView.getContext();
        int fallbackRes = resolveFallbackImage(context, imageRef, productName, brand);
        if (isLoadableUri(imageRef)) {
            Glide.with(imageView)
                    .load(imageRef.trim())
                    .placeholder(fallbackRes)
                    .error(fallbackRes)
                    .into(imageView);
            return;
        }
        imageView.setImageResource(fallbackRes);
    }

    public static int resolveFallbackImage(Context context, String imageRef, String productName, String brand) {
        int imageRes = findImageRes(context, imageRef);
        if (imageRes != 0) {
            return imageRes;
        }

        String name = productName == null ? "" : productName.toLowerCase(Locale.ROOT);
        String normalizedBrand = brand == null ? "" : brand.toLowerCase(Locale.ROOT);

        if (name.contains("iphone") || brandContains(normalizedBrand, "apple")) {
            return R.drawable.ip_15;
        }
        if (name.contains("s24") || brandContains(normalizedBrand, "samsung")) {
            int samsungRes = findImageRes(context, "ss_s24_ultra");
            if (samsungRes != 0) {
                return samsungRes;
            }
            samsungRes = findImageRes(context, "ss_s24_utra");
            if (samsungRes != 0) {
                return samsungRes;
            }
        }
        if (name.contains("iphone 14")) {
            int iphone14Res = findImageRes(context, "ic_iphone15");
            if (iphone14Res != 0) {
                return iphone14Res;
            }
        }

        return android.R.drawable.ic_menu_gallery;
    }

    public static boolean isValidImageInput(String imageRef) {
        if (imageRef == null) {
            return true;
        }
        String trimmed = imageRef.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        if (!looksLikeUri(trimmed)) {
            return true;
        }
        Uri uri = Uri.parse(trimmed);
        String scheme = uri.getScheme();
        if ("content".equalsIgnoreCase(scheme)) {
            return true;
        }
        String host = uri.getHost();
        return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && host != null
                && !host.trim().isEmpty()
                && !trimmed.contains(" ");
    }

    private static boolean isLoadableUri(String imageRef) {
        if (!isValidImageInput(imageRef)) return false;
        String trimmed = imageRef == null ? "" : imageRef.trim();
        return looksLikeUrl(trimmed) || trimmed.regionMatches(true, 0, "content://", 0, 10);
    }

    private static boolean looksLikeUri(String value) {
        return looksLikeUrl(value) || value.regionMatches(true, 0, "content://", 0, 10);
    }

    private static boolean looksLikeUrl(String value) {
        return value.regionMatches(true, 0, "http://", 0, 7)
                || value.regionMatches(true, 0, "https://", 0, 8);
    }

    private static boolean brandContains(String brand, String keyword) {
        return brand.contains(keyword);
    }

    private static int findImageRes(Context context, String imageName) {
        if (imageName == null || imageName.trim().isEmpty()) {
            return 0;
        }

        String imageKey = imageName.trim();
        int resId = context.getResources().getIdentifier(imageKey, "drawable", context.getPackageName());
        if (resId != 0) {
            return resId;
        }
        return context.getResources().getIdentifier(imageKey, "mipmap", context.getPackageName());
    }
}
