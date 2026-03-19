package com.example.phonestore.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "SESSION";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_REMEMBER_ME = "rememberMe";
    private static final String KEY_REMEMBERED_USERNAME = "rememberedUsername";

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void save(long userId, String username, String role) {
        sp.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_ROLE, role)
                .apply();
    }

    public boolean isLoggedIn() {
        return sp.getString(KEY_USERNAME, null) != null;
    }

    public long getUserId() {
        return sp.getLong(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return sp.getString(KEY_USERNAME, null);
    }

    public String getRole() {
        return sp.getString(KEY_ROLE, null);
    }

    public void saveRememberedLogin(boolean rememberMe, String username) {
        SharedPreferences.Editor editor = sp.edit();
        if (rememberMe && username != null && !username.trim().isEmpty()) {
            editor.putBoolean(KEY_REMEMBER_ME, true);
            editor.putString(KEY_REMEMBERED_USERNAME, username.trim());
        } else {
            editor.remove(KEY_REMEMBER_ME);
            editor.remove(KEY_REMEMBERED_USERNAME);
        }
        editor.apply();
    }

    public boolean shouldRememberLogin() {
        return sp.getBoolean(KEY_REMEMBER_ME, false);
    }

    public String getRememberedUsername() {
        return sp.getString(KEY_REMEMBERED_USERNAME, "");
    }

    public void clear() {
        sp.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USERNAME)
                .remove(KEY_ROLE)
                .apply();
    }
}
