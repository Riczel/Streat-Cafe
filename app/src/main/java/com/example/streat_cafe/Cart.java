package com.example.streat_cafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Cart extends AppCompatActivity {

    private LinearLayout emptyState, cartItemsList, summarySection;
    private NestedScrollView cartContent;
    private TextView tvSubtotal, tvTotal;
    private ApiInterface apiInterface;
    private String authToken;
    private double currentSubtotal = 0;
    private List<JsonObject> currentCartItems = new ArrayList<>();
    private final double SHIPPING_FEE = 10.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);

        apiInterface = RetrofitClient.getClient().create(ApiInterface.class);
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", "");

        emptyState = findViewById(R.id.emptyState);
        cartItemsList = findViewById(R.id.cartItemsList);
        cartContent = findViewById(R.id.cartContent);
        summarySection = findViewById(R.id.summarySection);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTotal = findViewById(R.id.tvTotal);
        Button btnOrderNow = findViewById(R.id.btnOrderNow);
        Button btnCheckout = findViewById(R.id.btnCheckout);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnOrderNow.setOnClickListener(v -> navigateTo(Menu.class));
        btnCheckout.setOnClickListener(v -> {
            Intent intent = new Intent(Cart.this, Checkout.class);
            intent.putExtra("subtotal", currentSubtotal);
            startActivity(intent);
        });

        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartItems();
    }

    private void setupNavigation() {
        if (findViewById(R.id.navHome) != null) findViewById(R.id.navHome).setOnClickListener(v -> navigateTo(User_Dashboard.class));
        if (findViewById(R.id.navMenu) != null) findViewById(R.id.navMenu).setOnClickListener(v -> navigateTo(Menu.class));
        if (findViewById(R.id.navDiscover) != null) findViewById(R.id.navDiscover).setOnClickListener(v -> navigateTo(Discover.class));
        if (findViewById(R.id.navOrders) != null) findViewById(R.id.navOrders).setOnClickListener(v -> navigateTo(Orders.class));
        if (findViewById(R.id.navProfile) != null) findViewById(R.id.navProfile).setOnClickListener(v -> navigateTo(User_Profile.class));
    }

    private void navigateTo(Class<?> targetClass) {
        if (this.getClass().equals(targetClass)) return;
        Intent intent = new Intent(this, targetClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void loadCartItems() {
        if (TextUtils.isEmpty(authToken)) {
            showEmptyState();
            return;
        }

        apiInterface.getCart("Bearer " + authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null && response.body().has("cart")) {
                    JsonArray cartArray = response.body().getAsJsonArray("cart");
                    currentCartItems.clear();
                    for (JsonElement element : cartArray) {
                        currentCartItems.add(element.getAsJsonObject());
                    }
                    displayCartItems(currentCartItems);
                } else {
                    showEmptyState();
                }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {
                showEmptyState();
            }
        });
    }

    private void displayCartItems(List<JsonObject> items) {
        cartItemsList.removeAllViews();
        if (items.isEmpty()) {
            showEmptyState();
            return;
        }

        emptyState.setVisibility(View.GONE);
        cartContent.setVisibility(View.VISIBLE);
        summarySection.setVisibility(View.VISIBLE);
        
        currentSubtotal = 0;
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            JsonObject item = items.get(i);
            
            String name = item.has("productName") ? item.get("productName").getAsString() : "Unknown";
            String category = item.has("category") ? item.get("category").getAsString() : "";
            
            // In your sample data, 'price' is the calculated unit price * quantity or just unit price.
            // Menu.java saves 'price' as the calculated total for that line item.
            double itemLineTotal = item.has("price") ? item.get("price").getAsDouble() : 0.0;
            int qty = item.has("quantity") ? item.get("quantity").getAsInt() : 1;
            String imageSource = item.has("image") ? item.get("image").getAsString() : "";
            String size = item.has("size") ? item.get("size").getAsString() : "Standard";
            
            String sugar = "";
            if (item.has("sugarLevel")) sugar = item.get("sugarLevel").getAsString();
            
            StringBuilder detailsBuilder = new StringBuilder(size);
            if (!TextUtils.isEmpty(sugar) && !sugar.equals("N/A") && !sugar.isEmpty()) {
                detailsBuilder.append(" • ").append(sugar).append(" Sweet");
            }
            
            if (item.has("addons") && item.get("addons").isJsonArray()) {
                JsonArray addons = item.getAsJsonArray("addons");
                for (JsonElement a : addons) {
                    if (a.isJsonObject()) {
                        detailsBuilder.append(" • ").append(a.getAsJsonObject().get("name").getAsString());
                    } else {
                        detailsBuilder.append(" • ").append(a.getAsString());
                    }
                }
            }

            currentSubtotal += itemLineTotal;

            View itemView = inflater.inflate(R.layout.item_cart, cartItemsList, false);
            
            TextView tvName = itemView.findViewById(R.id.tvProductName);
            TextView tvCategory = itemView.findViewById(R.id.tvCategory);
            TextView tvQty = itemView.findViewById(R.id.tvQty);
            TextView tvDetails = itemView.findViewById(R.id.tvDetails);
            TextView tvPrice = itemView.findViewById(R.id.tvPrice);
            ImageView ivProduct = itemView.findViewById(R.id.ivProduct);
            View btnPlus = itemView.findViewById(R.id.btnPlus);
            View btnMinus = itemView.findViewById(R.id.btnMinus);
            View btnDelete = itemView.findViewById(R.id.btnDelete);

            tvName.setText(name);
            tvCategory.setText(category);
            tvQty.setText(String.valueOf(qty));
            tvPrice.setText(String.format(Locale.getDefault(), "₱ %.2f", itemLineTotal));
            tvDetails.setText(detailsBuilder.toString());
            
            if (!TextUtils.isEmpty(imageSource)) {
                if (imageSource.startsWith("http")) {
                    Glide.with(this).load(imageSource).placeholder(R.drawable.coffee_removebg_preview).into(ivProduct);
                } else {
                    int resId = getResources().getIdentifier(imageSource, "drawable", getPackageName());
                    ivProduct.setImageResource(resId != 0 ? resId : R.drawable.coffee_removebg_preview);
                }
            } else {
                ivProduct.setImageResource(R.drawable.coffee_removebg_preview);
            }

            btnPlus.setOnClickListener(v -> updateQuantity(index, qty + 1));
            btnMinus.setOnClickListener(v -> {
                if (qty > 1) updateQuantity(index, qty - 1);
                else deleteItem(index);
            });
            btnDelete.setOnClickListener(v -> deleteItem(index));

            cartItemsList.addView(itemView);
        }
        updateTotals();
    }

    private void updateQuantity(int index, int newQty) {
        JsonObject item = currentCartItems.get(index);
        int oldQty = item.get("quantity").getAsInt();
        double oldPrice = item.get("price").getAsDouble();
        double unitPrice = oldPrice / oldQty;
        
        item.addProperty("quantity", newQty);
        item.addProperty("price", unitPrice * newQty);
        
        saveUpdatedCart();
    }

    private void deleteItem(int index) {
        currentCartItems.remove(index);
        saveUpdatedCart();
    }

    private void saveUpdatedCart() {
        JsonArray cartArray = new JsonArray();
        for (JsonObject item : currentCartItems) cartArray.add(item);
        JsonObject payload = new JsonObject();
        payload.add("cart", cartArray);

        apiInterface.saveCart("Bearer " + authToken, payload).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    displayCartItems(currentCartItems);
                } else {
                    Toast.makeText(Cart.this, "Failed to update cart", Toast.LENGTH_SHORT).show();
                    loadCartItems();
                }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(Cart.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTotals() {
        tvSubtotal.setText(String.format(Locale.getDefault(), "₱ %.2f", currentSubtotal));
        tvTotal.setText(String.format(Locale.getDefault(), "₱ %.2f", currentSubtotal + SHIPPING_FEE));
    }

    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        cartContent.setVisibility(View.GONE);
        summarySection.setVisibility(View.GONE);
    }
}
