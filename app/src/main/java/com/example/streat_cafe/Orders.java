package com.example.streat_cafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Orders extends AppCompatActivity implements OrderAdapter.OnOrderUpdateListener {

    private Button btnOrderNow;
    private LinearLayout emptyState;
    private RecyclerView rvOrders;
    private OrderAdapter adapter;
    private List<JsonObject> allOrders = new ArrayList<>();
    private List<JsonObject> filteredOrders = new ArrayList<>();
    private ApiInterface apiInterface;
    private String authToken;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_orders);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left, 
                         insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 
                         insets.getInsets(WindowInsetsCompat.Type.systemBars()).right, 
                         insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        apiInterface = RetrofitClient.getClient().create(ApiInterface.class);
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", "");

        emptyState = findViewById(R.id.emptyState);
        rvOrders = findViewById(R.id.rvOrders);
        btnOrderNow = findViewById(R.id.btnOrderNow);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(this, filteredOrders, this);
        rvOrders.setAdapter(adapter);

        if (btnOrderNow != null) {
            btnOrderNow.setOnClickListener(v -> navigateTo(Menu.class));
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadOrders);
        }

        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    @Override
    public void onOrderRefresh() {
        loadOrders();
    }

    private void filterOrders() {
        filteredOrders.clear();
        for (JsonObject order : allOrders) {
            String status = order.has("status") ? order.get("status").getAsString().toLowerCase() : "pending";
            if (!status.equals("completed") && !status.equals("cancelled")) {
                filteredOrders.add(order);
            }
        }

        if (filteredOrders.isEmpty()) {
            showEmptyState();
        } else {
            showOrdersList();
        }
    }

    private void loadOrders() {
        if (authToken == null || authToken.isEmpty()) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            showEmptyState();
            return;
        }

        Log.d("Orders", "Calling GET /api/getOrders...");
        apiInterface.getOrders("Bearer " + authToken).enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    JsonArray ordersArray = response.body();
                    Log.d("Orders", "Loaded " + ordersArray.size() + " orders: " + ordersArray.toString());
                    allOrders.clear();
                    for (JsonElement element : ordersArray) {
                        allOrders.add(element.getAsJsonObject());
                    }
                    filterOrders();
                } else {
                    Log.e("Orders", "Error response: " + response.code());
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                Log.e("Orders", "API Failure: " + t.getMessage());
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        if (rvOrders != null) rvOrders.setVisibility(View.GONE);
    }

    private void showOrdersList() {
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        if (rvOrders != null) rvOrders.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    private void setupNavigation() {
        if (findViewById(R.id.navHome) != null) findViewById(R.id.navHome).setOnClickListener(v -> navigateTo(User_Dashboard.class));
        if (findViewById(R.id.navMenu) != null) findViewById(R.id.navMenu).setOnClickListener(v -> navigateTo(Menu.class));
        if (findViewById(R.id.navDiscover) != null) findViewById(R.id.navDiscover).setOnClickListener(v -> navigateTo(Discover.class));
        if (findViewById(R.id.navProfile) != null) findViewById(R.id.navProfile).setOnClickListener(v -> navigateTo(User_Profile.class));
    }

    private void navigateTo(Class<?> targetClass) {
        if (this.getClass().equals(targetClass)) return;
        Intent intent = new Intent(this, targetClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}
