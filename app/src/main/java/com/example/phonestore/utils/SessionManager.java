package com.example.phonestore.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        sp = ctx.getSharedPreferences("SESSION", Context.MODE_PRIVATE);
    }

    public void save(long userId, String username, String role) {
        sp.edit()
                .putLong("userId", userId)
                .putString("username", username)
                .putString("role", role)
                .apply();
    }

    public boolean isLoggedIn() {
        return sp.getString("username", null) != null;
    }

    public String getUsername() {
        return sp.getString("username", null);
    }

    public String getRole() {
        return sp.getString("role", null);
    }

    public void clear() {
        sp.edit().clear().apply();
    }
}