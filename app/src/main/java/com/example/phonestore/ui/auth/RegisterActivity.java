package com.example.phonestore.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtUsername;
    private EditText edtFullName;
    private EditText edtPassword;
    private EditText edtConfirm;
    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edtUsername = findViewById(R.id.edtUsername);
        edtFullName = findViewById(R.id.edtFullName);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirm = findViewById(R.id.edtConfirm);
        MaterialButton btnRegister = findViewById(R.id.btnRegister);
        TextView tvGoLogin = findViewById(R.id.tvGoLogin);

        userDao = new UserDao(this);

        btnRegister.setOnClickListener(v -> doRegister());
        tvGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void doRegister() {
        String usernameInput = edtUsername.getText().toString().trim();
        String username = usernameInput.toLowerCase(Locale.ROOT);
        String fullname = edtFullName.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirm = edtConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(fullname)
                || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, R.string.auth_register_required_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidGmail(username)) {
            Toast.makeText(this, R.string.auth_register_gmail_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, R.string.auth_password_min_length, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, R.string.err_password_not_match, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean ok = userDao.registerCustomer(fullname, username, password);
        if (!ok) {
            Toast.makeText(this, R.string.auth_register_exists_or_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.auth_register_success, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private boolean isValidGmail(String value) {
        return !TextUtils.isEmpty(value)
                && Patterns.EMAIL_ADDRESS.matcher(value).matches()
                && value.endsWith("@gmail.com");
    }
}
