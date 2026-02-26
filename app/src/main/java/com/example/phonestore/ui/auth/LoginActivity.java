package com.example.phonestore.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonestore.R;
import com.example.phonestore.data.dao.UserDao;
import com.example.phonestore.data.db.DBHelper;
import com.example.phonestore.data.model.User;
import com.example.phonestore.ui.home.AdminHomeActivity;
import com.example.phonestore.ui.home.CustomerHomeActivity;
import com.example.phonestore.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsername;
    private EditText edtPassword;
    private UserDao userDao;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        TextView tvGoRegister = findViewById(R.id.tvGoRegister);

        userDao = new UserDao(this);
        session = new SessionManager(this);

        if (session.isLoggedIn()) {
            openHomeByRole(session.getRole());
            return;
        }

        btnLogin.setOnClickListener(v -> doLogin());
        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void doLogin() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ tài khoản và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = userDao.login(username, password);
        if (user == null) {
            Toast.makeText(this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        session.save(user.id, user.username, user.role);
        openHomeByRole(user.role);
    }

    private void openHomeByRole(String role) {
        Intent intent;

        if (DBHelper.ROLE_ADMIN.equals(role)) {
            intent = new Intent(this, AdminHomeActivity.class);
        } else if (DBHelper.ROLE_CUSTOMER.equals(role)) {
            intent = new Intent(this, CustomerHomeActivity.class);
        } else {
            session.clear();
            Toast.makeText(this, "Role không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(intent);
        finish();
    }
}
