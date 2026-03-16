package com.example.phonestore.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonestore.R;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        startFloatingAnimations();

        findViewById(R.id.btnGoLogin).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        findViewById(R.id.btnGoRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void startFloatingAnimations() {
        applyFloatingAnimation(findViewById(R.id.bubbleColors), R.anim.bubble_float_slow, 0L);
        applyFloatingAnimation(findViewById(R.id.bubbleRepair), R.anim.bubble_float_medium, 180L);
        applyFloatingAnimation(findViewById(R.id.bubbleSale), R.anim.bubble_float_fast, 320L);
        applyFloatingAnimation(findViewById(R.id.bubbleDelivery), R.anim.bubble_float_medium, 520L);
        applyFloatingAnimation(findViewById(R.id.bubbleSupport), R.anim.bubble_float_slow, 700L);
        applyFloatingAnimation(findViewById(R.id.dotTopLeft), R.anim.bubble_float_fast, 140L);
        applyFloatingAnimation(findViewById(R.id.dotTopRight), R.anim.bubble_float_medium, 360L);
        applyFloatingAnimation(findViewById(R.id.dotMidLeft), R.anim.bubble_float_slow, 540L);
        applyFloatingAnimation(findViewById(R.id.dotMidRight), R.anim.bubble_float_fast, 780L);
    }

    private void applyFloatingAnimation(View view, int animRes, long startOffset) {
        if (view == null) {
            return;
        }
        Animation animation = AnimationUtils.loadAnimation(this, animRes);
        animation.setStartOffset(startOffset);
        view.startAnimation(animation);
    }
}
