package com.example.streat_cafe;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecentOrders extends AppCompatActivity implements OrderAdapter.OnOrderUpdateListener {

    private LinearLayout emptyState;
    private RecyclerView rvRecentOrders;
    private OrderAdapter adapter;
    private List<JsonObject> recentOrderList = new ArrayList<>();
    private ApiInterface apiInterface;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recent_orders);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        apiInterface = RetrofitClient.getClient().create(ApiInterface.class);
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", "");

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        emptyState = findViewById(R.id.emptyState);
        rvRecentOrders = findViewById(R.id.rvRecentOrders);

        rvRecentOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(this, recentOrderList, this);
        rvRecentOrders.setAdapter(adapter);

        loadRecentOrders();
    }

    @Override
    public void onOrderRefresh() {
        loadRecentOrders();
    }

    private void loadRecentOrders() {
        if (authToken == null || authToken.isEmpty()) {
            showEmptyState();
            return;
        }

        // Calling the specific "Recent Orders" endpoint used by your website
        apiInterface.getRecentOrders("Bearer " + authToken).enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonArray ordersArray = response.body();
                    recentOrderList.clear();
                    
                    for (JsonElement element : ordersArray) {
                        JsonObject order = element.getAsJsonObject();
                        // The backend route already filters for completed/cancelled, 
                        // but we check again for safety and case-insensitivity
                        String status = order.has("status") ? order.get("status").getAsString() : "";
                        if (status.equalsIgnoreCase("completed") || status.equalsIgnoreCase("cancelled")) {
                            recentOrderList.add(order);
                        }
                    }
                    
                    if (recentOrderList.isEmpty()) {
                        showEmptyState();
                    } else {
                        showOrdersList();
                    }
                } else {
                    Log.e("RecentOrders", "Error: " + response.code());
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                Log.e("RecentOrders", "Failure: " + t.getMessage());
                showEmptyState();
                Toast.makeText(RecentOrders.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState() {
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        if (rvRecentOrders != null) rvRecentOrders.setVisibility(View.GONE);
    }

    private void showOrdersList() {
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        if (rvRecentOrders != null) rvRecentOrders.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
    }
}
