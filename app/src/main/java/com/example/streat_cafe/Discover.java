package com.example.streat_cafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Discover extends AppCompatActivity {

    private LinearLayout navHome, navProfile, navMenu, navOrders, navDiscover;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_discover);

        // Retrieve token for any API-based content you might add to Discover later
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", "");

        navHome = findViewById(R.id.navHome);
        navProfile = findViewById(R.id.navProfile);
        navMenu = findViewById(R.id.navMenu);
        navOrders = findViewById(R.id.navOrders);
        navDiscover = findViewById(R.id.navDiscover);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupNavigation();
    }

    private void setupNavigation() {
        if (navHome != null) navHome.setOnClickListener(v -> navigateTo(User_Dashboard.class));
        if (navMenu != null) navMenu.setOnClickListener(v -> navigateTo(Menu.class));
        if (navOrders != null) navOrders.setOnClickListener(v -> navigateTo(Orders.class));
        if (navProfile != null) navProfile.setOnClickListener(v -> navigateTo(User_Profile.class));
        
        // Current tab
        if (navDiscover != null) {
            navDiscover.setOnClickListener(v -> {
                // Already on Discover
            });
        }
    }

    private void navigateTo(Class<?> targetClass) {
        if (this.getClass().equals(targetClass)) return;
        Intent intent = new Intent(this, targetClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}
