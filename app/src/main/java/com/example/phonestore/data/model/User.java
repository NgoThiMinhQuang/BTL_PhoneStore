package com.example.phonestore.data.model;

public class User {
    public long id;
    public String fullname;
    public String username;
    public String role;
    public boolean isActive = true;
    public int orderCount;
    public int deliveredSpend;

    public User(long id, String fullname, String username, String role) {
        this.id = id;
        this.fullname = fullname;
        this.username = username;
        this.role = role;
    }
}
