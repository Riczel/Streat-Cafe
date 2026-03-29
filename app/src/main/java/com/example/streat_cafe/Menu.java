package com.example.streat_cafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

public class Menu extends AppCompatActivity {

    private final List<Product> productList = new ArrayList<>();
    private final List<Product> filteredList = new ArrayList<>();
    private ProductAdapter adapter;
    private ApiInterface apiInterface;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String authToken;
    
    private String selectedSize = "Standard";
    private String selectedSweetness = "100%";
    private final List<Product.Addon> selectedAddons = new ArrayList<>();
    private double currentUnitPrice = 0;
    private String currentCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu);

        apiInterface = RetrofitClient.getClient().create(ApiInterface.class);
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", "");

        RecyclerView rvMenu = findViewById(R.id.rvMenu);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        EditText etSearch = findViewById(R.id.etSearch);
        ImageView ivCart = findViewById(R.id.ivCart);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (ivCart != null) ivCart.setOnClickListener(v -> startActivity(new Intent(this, Cart.class)));

        rvMenu.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(this, filteredList, this::onProductClick);
        rvMenu.setAdapter(adapter);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadProducts);
        }

        setupCategoryListeners();
        setupNavigation();
        loadProducts();

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterProducts(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void loadProducts() {
        if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        }
        
        apiInterface.getProducts().enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    productList.clear();
                    for (JsonObject obj : response.body()) {
                        productList.add(Product.fromJson(obj));
                    }
                    filterByCategory(currentCategory);

                    // Check for recommended product from Intent
                    String recommended = getIntent().getStringExtra("recommended_product");
                    if (recommended != null) {
                        getIntent().removeExtra("recommended_product"); // Consume it so it doesn't reopen on config change
                        for (Product p : productList) {
                            if (p.getProductName().equalsIgnoreCase(recommended)) {
                                onProductClick(p);
                                break;
                            }
                        }
                    }
                } else {
                    Toast.makeText(Menu.this, "Failed to load products", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(Menu.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCategoryListeners() {
        int[] ids = {R.id.btnCatAll, R.id.btnCatCoffee, R.id.btnCatCheesecake, R.id.btnCatMilkshake, 
                     R.id.btnCatMilktea, R.id.btnCatMocktail, R.id.btnCatPicaPica, R.id.btnCatNoodles, 
                     R.id.btnCatTakoyaki, R.id.btnCatFrappe};
        
        String[] cats = {"All", "coffee", "cheesecake", "milkshake", "milktea", "mocktail", "pica-pica", "noodles", "takoyaki", "frappe"};

        for (int i = 0; i < ids.length; i++) {
            final int index = i;
            TextView tv = findViewById(ids[i]);
            if (tv == null) continue;
            tv.setOnClickListener(v -> {
                for (int id : ids) {
                    TextView other = findViewById(id);
                    if (other != null) {
                        other.setBackgroundResource(R.drawable.bg_option_unselected);
                        other.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                    }
                }
                v.setBackgroundResource(R.drawable.bg_option_selected);
                ((TextView) v).setTextColor(ContextCompat.getColor(this, android.R.color.white));
                currentCategory = cats[index];
                filterByCategory(currentCategory);
            });
        }
    }

    private void filterByCategory(String cat) {
        filteredList.clear();
        for (Product p : productList) {
            if (cat.equalsIgnoreCase("All") || p.getCategory().equalsIgnoreCase(cat)) {
                filteredList.add(p);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void filterProducts(String query) {
        filteredList.clear();
        for (Product p : productList) {
            boolean matchesCat = currentCategory.equalsIgnoreCase("All") || p.getCategory().equalsIgnoreCase(currentCategory);
            boolean matchesSearch = p.getProductName().toLowerCase().contains(query.toLowerCase());
            if (matchesCat && matchesSearch) {
                filteredList.add(p);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void onProductClick(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dv = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_cart, null);
        builder.setView(dv);

        ImageView ivProduct = dv.findViewById(R.id.dialogProductImage);
        TextView tvName = dv.findViewById(R.id.dialogProductName);
        TextView tvPriceDisplay = dv.findViewById(R.id.dialogProductPriceDisplay);
        TextView tvDescription = dv.findViewById(R.id.dialogProductDescription);
        TextView tvQty = dv.findViewById(R.id.dialogTvQty);
        Button btnConfirm = dv.findViewById(R.id.dialogBtnConfirm);

        tvName.setText(product.getProductName());
        tvDescription.setText(product.getProductDescription());
        tvQty.setText("1");

        String img = product.getImage();
        if (!TextUtils.isEmpty(img)) {
            if (img.startsWith("http")) {
                Glide.with(this).load(img).placeholder(R.drawable.coffee_removebg_preview).into(ivProduct);
            } else {
                int res = getResources().getIdentifier(img, "drawable", getPackageName());
                ivProduct.setImageResource(res != 0 ? res : R.drawable.coffee_removebg_preview);
            }
        } else {
            ivProduct.setImageResource(R.drawable.coffee_removebg_preview);
        }

        selectedAddons.clear();
        selectedSweetness = product.getSugarLevels().isEmpty() ? "" : "100%";
        
        // Use logic: if Medium and Large exist, default to Medium
        if (!product.getSizes().isEmpty()) {
            boolean hasMedium = false;
            for (Product.Size s : product.getSizes()) {
                if (s.name.equalsIgnoreCase("Medium")) {
                    hasMedium = true;
                    break;
                }
            }
            selectedSize = hasMedium ? "Medium" : product.getSizes().get(0).name;
        } else {
            selectedSize = "Standard";
        }

        dv.findViewById(R.id.sectionSize).setVisibility(product.getSizes().isEmpty() ? View.GONE : View.VISIBLE);
        dv.findViewById(R.id.sectionSweetness).setVisibility(product.getSugarLevels().isEmpty() ? View.GONE : View.VISIBLE);
        dv.findViewById(R.id.sectionAddons).setVisibility(product.getAddons().isEmpty() ? View.GONE : View.VISIBLE);

        setupSizeButtons(dv, product);
        setupSweetnessListeners(dv);
        setupAddonButtons(dv, product);
        updatePriceDisplay(tvPriceDisplay, btnConfirm, product.getPrice(), 1, product);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dv.findViewById(R.id.dialogBtnPlus).setOnClickListener(v -> {
            int q = Integer.parseInt(tvQty.getText().toString()) + 1;
            tvQty.setText(String.valueOf(q));
            updatePriceDisplay(tvPriceDisplay, btnConfirm, product.getPrice(), q, product);
        });

        dv.findViewById(R.id.dialogBtnMinus).setOnClickListener(v -> {
            int q = Integer.parseInt(tvQty.getText().toString());
            if (q > 1) {
                q--;
                tvQty.setText(String.valueOf(q));
                updatePriceDisplay(tvPriceDisplay, btnConfirm, product.getPrice(), q, product);
            }
        });

        btnConfirm.setOnClickListener(v -> {
            if (authToken.isEmpty()) {
                Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
                return;
            }
            int q = Integer.parseInt(tvQty.getText().toString());
            saveItemToCart(product, q, dialog);
        });

        dv.findViewById(R.id.dialogBtnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupSizeButtons(View dv, Product product) {
        TextView sM = dv.findViewById(R.id.sizeMedium);
        TextView sL = dv.findViewById(R.id.sizeLarge);
        List<Product.Size> sizes = product.getSizes();
        if (sizes.isEmpty()) return;

        boolean hasM = false, hasL = false;
        for (Product.Size s : sizes) {
            if (s.name.equalsIgnoreCase("Medium")) hasM = true;
            if (s.name.equalsIgnoreCase("Large")) hasL = true;
        }
        sM.setVisibility(hasM ? View.VISIBLE : View.GONE);
        sL.setVisibility(hasL ? View.VISIBLE : View.GONE);

        if (selectedSize.equalsIgnoreCase("Medium")) updateSizeUI(sM, sL);
        else if (selectedSize.equalsIgnoreCase("Large")) updateSizeUI(sL, sM);

        sM.setOnClickListener(v -> { selectedSize = "Medium"; updateSizeUI(sM, sL); updatePriceDisplay(dv.findViewById(R.id.dialogProductPriceDisplay), dv.findViewById(R.id.dialogBtnConfirm), product.getPrice(), Integer.parseInt(((TextView)dv.findViewById(R.id.dialogTvQty)).getText().toString()), product); });
        sL.setOnClickListener(v -> { selectedSize = "Large"; updateSizeUI(sL, sM); updatePriceDisplay(dv.findViewById(R.id.dialogProductPriceDisplay), dv.findViewById(R.id.dialogBtnConfirm), product.getPrice(), Integer.parseInt(((TextView)dv.findViewById(R.id.dialogTvQty)).getText().toString()), product); });
    }

    private void updateSizeUI(TextView a, TextView i) {
        a.setBackgroundResource(R.drawable.bg_option_selected);
        a.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        if (i != null && i.getVisibility() == View.VISIBLE) {
            i.setBackgroundResource(R.drawable.bg_option_unselected);
            i.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }
    }

    private void setupSweetnessListeners(View dv) {
        int[] ids = {R.id.sweet0, R.id.sweet50, R.id.sweet75, R.id.sweet100};
        for (int id : ids) {
            TextView tv = dv.findViewById(id);
            if (tv == null) continue;
            tv.setOnClickListener(v -> {
                for (int oid : ids) {
                    TextView o = dv.findViewById(oid);
                    if (o != null) { o.setBackgroundResource(R.drawable.bg_option_unselected); o.setTextColor(ContextCompat.getColor(this, android.R.color.black)); }
                }
                v.setBackgroundResource(R.drawable.bg_option_selected);
                ((TextView) v).setTextColor(ContextCompat.getColor(this, android.R.color.white));
                selectedSweetness = ((TextView) v).getText().toString();
            });
        }
    }

    private void setupAddonButtons(View dv, Product product) {
        List<Product.Addon> addons = product.getAddons();
        int[] layouts = {R.id.addonCheesecake, R.id.addonPearl, R.id.addonNata, R.id.addonCoffeeJelly, R.id.addonFruitJelly};
        int[] icons = {R.id.iconCheesecake, R.id.iconPearl, R.id.iconNata, R.id.iconCoffeeJelly, R.id.iconFruitJelly};
        int[] names = {R.id.textCheesecake, R.id.textPearl, R.id.textNata, R.id.textCoffeeJelly, R.id.textFruitJelly};
        int[] prices = {R.id.priceCheesecake, R.id.pricePearl, R.id.priceNata, R.id.priceCoffeeJelly, R.id.priceFruitJelly};

        for (int i = 0; i < layouts.length; i++) {
            RelativeLayout l = dv.findViewById(layouts[i]);
            if (l == null) continue;
            if (i < addons.size()) {
                l.setVisibility(View.VISIBLE);
                Product.Addon a = addons.get(i);
                ((TextView) dv.findViewById(names[i])).setText(a.name);
                ((TextView) dv.findViewById(prices[i])).setText(String.format(Locale.getDefault(), "+₱ %.2f", a.price));
                ImageView ic = dv.findViewById(icons[i]);
                l.setOnClickListener(v -> {
                    boolean alreadySelected = false;
                    for (Product.Addon selected : selectedAddons) {
                        if (selected.name.equals(a.name)) {
                            alreadySelected = true;
                            break;
                        }
                    }

                    selectedAddons.clear();
                    for (int iconId : icons) {
                        ImageView otherIcon = dv.findViewById(iconId);
                        if (otherIcon != null) otherIcon.setImageResource(R.drawable.ic_radio_unselected);
                    }

                    if (!alreadySelected) {
                        selectedAddons.add(a);
                        ic.setImageResource(R.drawable.ic_radio_selected);
                    }
                    
                    updatePriceDisplay(dv.findViewById(R.id.dialogProductPriceDisplay), dv.findViewById(R.id.dialogBtnConfirm), product.getPrice(), Integer.parseInt(((TextView)dv.findViewById(R.id.dialogTvQty)).getText().toString()), product);
                });
            } else { l.setVisibility(View.GONE); }
        }
    }

    private void updatePriceDisplay(TextView tvP, Button btnC, double bP, int qty, Product product) {
        tvP.setText(String.format(Locale.getDefault(), "₱ %.2f", bP));

        double baseSizePrice = bP;
        if (selectedSize.equalsIgnoreCase("Large")) {
            baseSizePrice = bP + 10.0;
        } else {
            baseSizePrice = bP;
        }

        double totalUnitPrice = baseSizePrice;
        for (Product.Addon a : selectedAddons) totalUnitPrice += a.price;
        currentUnitPrice = totalUnitPrice;
        
        btnC.setText(String.format(Locale.getDefault(), "Add to Cart • ₱ %.2f", totalUnitPrice * qty));
    }

    private void saveItemToCart(Product product, int qty, AlertDialog dialog) {
        apiInterface.getCart("Bearer " + authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                JsonArray cart = (response.isSuccessful() && response.body() != null && response.body().has("cart")) ? response.body().getAsJsonArray("cart") : new JsonArray();
                
                double sPrice = 0;
                if (selectedSize.equalsIgnoreCase("Large")) {
                    sPrice = 10.0;
                }

                JsonObject newItem = new JsonObject();
                newItem.addProperty("productId", product.getId());
                newItem.addProperty("productName", product.getProductName());
                newItem.addProperty("basePrice", product.getPrice());
                newItem.addProperty("size", selectedSize);
                newItem.addProperty("sizePrice", sPrice);
                newItem.addProperty("sugarLevel", selectedSweetness);
                newItem.addProperty("quantity", qty);
                newItem.addProperty("price", currentUnitPrice * qty);
                newItem.addProperty("image", product.getImage());
                newItem.addProperty("category", product.getCategory());

                JsonArray adds = new JsonArray();
                for (Product.Addon a : selectedAddons) {
                    JsonObject o = new JsonObject();
                    o.addProperty("_id", a.id);
                    o.addProperty("name", a.name);
                    o.addProperty("price", a.price);
                    adds.add(o);
                }
                newItem.add("addons", adds);

                boolean merged = false;
                for (int i = 0; i < cart.size(); i++) {
                    JsonObject ex = cart.get(i).getAsJsonObject();
                    if (ex.get("productName").getAsString().equals(newItem.get("productName").getAsString()) &&
                        ex.get("size").getAsString().equals(newItem.get("size").getAsString()) &&
                        ex.get("sugarLevel").getAsString().equals(newItem.get("sugarLevel").getAsString()) &&
                        ex.get("addons").toString().equals(newItem.get("addons").toString())) {
                        ex.addProperty("quantity", ex.get("quantity").getAsInt() + qty);
                        ex.addProperty("price", ex.get("price").getAsDouble() + (currentUnitPrice * qty));
                        merged = true; break;
                    }
                }
                if (!merged) cart.add(newItem);

                JsonObject payload = new JsonObject();
                payload.add("cart", cart);
                apiInterface.saveCart("Bearer " + authToken, payload).enqueue(new Callback<JsonObject>() {
                    @Override public void onResponse(@NonNull Call<JsonObject> c, @NonNull Response<JsonObject> r) {
                        if (r.isSuccessful()) { 
                            dialog.dismiss(); 
                            Toast.makeText(Menu.this, "Added to cart!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(@NonNull Call<JsonObject> c, @NonNull Throwable t) {
                        Toast.makeText(Menu.this, "Failed to add to cart", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Toast.makeText(Menu.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v -> navigateTo(User_Dashboard.class));
        findViewById(R.id.navDiscover).setOnClickListener(v -> navigateTo(Discover.class));
        findViewById(R.id.navOrders).setOnClickListener(v -> navigateTo(Orders.class));
        findViewById(R.id.navProfile).setOnClickListener(v -> navigateTo(User_Profile.class));
    }

    private void navigateTo(Class<?> target) {
        if (this.getClass().equals(target)) return;
        Intent intent = new Intent(this, target);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}
