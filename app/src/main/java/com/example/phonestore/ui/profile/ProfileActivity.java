package com.example.phonestore.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.UserDao;
import com.example.phonestore.data.model.User;
import com.example.phonestore.ui.auth.WelcomeActivity;
import com.example.phonestore.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import android.content.Intent;
import androidx.appcompat.widget.Toolbar;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager session;
    private UserDao userDao;

    private TextView tvUsername, tvRole;
    private EditText edtFullName, edtOldPass, edtNewPass, edtConfirm;
    private MaterialButton btnSave;

    private long userId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = new SessionManager(this);
        userDao = new UserDao(this);

        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        userId = session.getUserId();
        if (userId == -1) {
            session.clear();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Profile");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvUsername = findViewById(R.id.tvUsername);
        tvRole = findViewById(R.id.tvRole);
        edtFullName = findViewById(R.id.edtFullName);
        edtOldPass = findViewById(R.id.edtOldPass);
        edtNewPass = findViewById(R.id.edtNewPass);
        edtConfirm = findViewById(R.id.edtConfirm);
        btnSave = findViewById(R.id.btnSave);

        loadUser();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadUser() {
        User u = userDao.getById(userId);
        if (u == null) {
            Toast.makeText(this, "Không tìm thấy user", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        tvUsername.setText(u.username);
        tvRole.setText(u.role);
        edtFullName.setText(u.fullname);
    }

    private void saveProfile() {
        String fullname = edtFullName.getText().toString().trim();
        if (TextUtils.isEmpty(fullname)) {
            Toast.makeText(this, "Họ tên không được trống", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean okName = userDao.updateFullName(userId, fullname);

        // Nếu user không nhập mật khẩu mới => chỉ cập nhật họ tên
        String oldPass = edtOldPass.getText().toString().trim();
        String newPass = edtNewPass.getText().toString().trim();
        String confirm = edtConfirm.getText().toString().trim();

        boolean okPass = true;
        if (!TextUtils.isEmpty(newPass) || !TextUtils.isEmpty(confirm) || !TextUtils.isEmpty(oldPass)) {
            if (TextUtils.isEmpty(oldPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirm)) {
                Toast.makeText(this, "Đổi mật khẩu: nhập đủ mật khẩu cũ/mới/nhập lại", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirm)) {
                Toast.makeText(this, "Mật khẩu mới nhập lại không khớp", Toast.LENGTH_SHORT).show();
                return;
            }
            okPass = userDao.changePassword(userId, oldPass, newPass);
            if (!okPass) {
                Toast.makeText(this, "Mật khẩu cũ không đúng", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (okName && okPass) {
            Toast.makeText(this, "Cập nhật profile thành công", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
        }
    }
}