package com.example.streat_cafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class User_Dashboard extends AppCompatActivity {

    private TextView heyName;
    private LinearLayout navProfile, navDiscover, navMenu, navOrders, navHome;
    private ApiInterface apiInterface;
    private String authToken;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Weather Views
    private CardView cardWeather, cardNoAddress;
    private TextView tvWeatherTemp, tvWeatherMessage, tvRecommendedDrink;
    private ImageView ivWeatherIcon;

    // WeatherAPI.com details
    private static final String WEATHER_API_KEY = "81979e8387214be6b1f143510262703";
    private static final String WEATHER_BASE_URL = "https://api.weatherapi.com/v1/";
    private static final String TAG = "STREAT_WEATHER";

    public interface WeatherService {
        @GET("current.json")
        Call<JsonObject> getCurrentWeather(@Query("key") String apiKey, @Query("q") String query, @Query("aqi") String aqi);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_dashboard);
        
        apiInterface = RetrofitClient.getClient().create(ApiInterface.class);
        
        heyName = findViewById(R.id.tvHeyName);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        CardView cardLocation = findViewById(R.id.cardLocation);
        CardView cardFBPage = findViewById(R.id.cardFBPage);
        navProfile = findViewById(R.id.navProfile);
        navDiscover = findViewById(R.id.navDiscover);
        navMenu = findViewById(R.id.navMenu);
        navOrders = findViewById(R.id.navOrders);
        navHome = findViewById(R.id.navHome);

        // Weather View Initialization
        cardWeather = findViewById(R.id.cardWeather);
        cardNoAddress = findViewById(R.id.cardNoAddress);
        tvWeatherTemp = findViewById(R.id.tvWeatherTemp);
        tvWeatherMessage = findViewById(R.id.tvWeatherMessage);
        tvRecommendedDrink = findViewById(R.id.tvRecommendedDrink);
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
        View btnTryNow = findViewById(R.id.btnTryNow);
        View btnTryFeatures = findViewById(R.id.btnTryFeatures);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", "");

        if (!authToken.isEmpty()) {
            fetchUserProfile();
        } else {
            heyName.setText(getGreeting() + ", Guest");
            showNoAddressState();
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                if (!authToken.isEmpty()) {
                    fetchUserProfile();
                } else {
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }

        if (btnTryNow != null) {
            btnTryNow.setOnClickListener(v -> {
                Intent intent = new Intent(this, Menu.class);
                if (tvRecommendedDrink != null) {
                    intent.putExtra("recommended_product", tvRecommendedDrink.getText().toString());
                }
                startActivity(intent);
            });
        }
        
        if (btnTryFeatures != null) {
            btnTryFeatures.setOnClickListener(v -> navigateTo(User_Profile.class));
        }

        if (cardLocation != null) {
            cardLocation.setOnClickListener(v -> openUrl("https://maps.app.goo.gl/3NDXm5FzEJkKdnLMA"));
        }
        if (cardFBPage != null) {
            cardFBPage.setOnClickListener(v -> openUrl("https://www.facebook.com/profile.php?id=100087261280704"));
        }

        setupNavigation();
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 0 && hour < 12) {
            return "Good Morning";
        } else if (hour >= 12 && hour < 18) {
            return "Good Afternoon";
        } else {
            return "Good Evening";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!authToken.isEmpty()) {
            fetchUserProfile(); 
        }
    }

    private void fetchUserProfile() {
        apiInterface.getProfile("Bearer " + authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject profile = response.body();
                    String firstName = profile.has("firstName") ? profile.get("firstName").getAsString() : "User";
                    heyName.setText(getGreeting() + ", " + firstName);

                    String city = "";
                    String province = "";
                    
                    if (profile.has("addresses") && profile.get("addresses").isJsonArray()) {
                        JsonArray addresses = profile.getAsJsonArray("addresses");
                        for (JsonElement e : addresses) {
                            if (e.isJsonObject()) {
                                JsonObject addr = e.getAsJsonObject();
                                if (addr.has("municipality") && !addr.get("municipality").isJsonNull()) {
                                    city = addr.get("municipality").getAsString().trim();
                                    province = addr.has("province") ? addr.get("province").getAsString().trim() : "";
                                    if (addr.has("isDefault") && addr.get("isDefault").getAsBoolean()) break;
                                }
                            }
                        }
                    }
                    
                    if (city.isEmpty() && profile.has("municipality") && !profile.get("municipality").isJsonNull()) {
                        city = profile.get("municipality").getAsString().trim();
                        province = profile.has("province") ? profile.get("province").getAsString().trim() : "";
                    }

                    if (!city.isEmpty()) {
                        cardWeather.setVisibility(View.VISIBLE);
                        cardNoAddress.setVisibility(View.GONE);
                        fetchWeather(city, province);
                    } else {
                        showNoAddressState();
                    }
                } else {
                    showNoAddressState();
                }
            }
            @Override public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                showNoAddressState();
            }
        });
    }

    private void fetchWeather(String city, String province) {
        tvWeatherTemp.setText("--°C");
        tvWeatherMessage.setText("Finding weather for " + city + "...");
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(WEATHER_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherService service = retrofit.create(WeatherService.class);
        
        String query = city + (province.isEmpty() ? "" : ", " + province);

        service.getCurrentWeather(WEATHER_API_KEY, query, "no").enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processWeatherResponse(response.body());
                } else {
                    if (response.code() == 400 || response.code() == 404) {
                        if (!province.isEmpty() && !province.equals(city)) {
                            Log.d(TAG, "City not found, trying province: " + province);
                            fetchWeatherByProvince(service, province);
                        } else {
                            tvWeatherMessage.setText("Location not recognized");
                        }
                    } else if (response.code() == 401 || response.code() == 403) {
                        tvWeatherMessage.setText("API Key Error");
                    } else {
                        tvWeatherMessage.setText("Weather Data Unavailable");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                tvWeatherMessage.setText("Check your connection");
            }
        });
    }

    private void fetchWeatherByProvince(WeatherService service, String province) {
        service.getCurrentWeather(WEATHER_API_KEY, province, "no").enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processWeatherResponse(response.body());
                } else {
                    tvWeatherMessage.setText("Location not recognized");
                }
            }
            @Override public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) { 
                tvWeatherMessage.setText("Connection failed");
            }
        });
    }

    private void processWeatherResponse(JsonObject data) {
        if (data.has("current")) {
            JsonObject current = data.getAsJsonObject("current");
            double temp = current.get("temp_c").getAsDouble();
            String condition = "Clear";
            String iconUrl = "";
            
            if (current.has("condition")) {
                JsonObject condObj = current.getAsJsonObject("condition");
                condition = condObj.get("text").getAsString();
                iconUrl = condObj.get("icon").getAsString();
                if (iconUrl.startsWith("//")) {
                    iconUrl = "https:" + iconUrl;
                }
            }
            updateWeatherUI(temp, condition, iconUrl);
        }
    }

    private void updateWeatherUI(double temp, String condition, String iconUrl) {
        tvWeatherTemp.setText(String.format(Locale.getDefault(), "%.0f°C %s", temp, condition));
        
        if (iconUrl != null && !iconUrl.isEmpty() && ivWeatherIcon != null) {
            Glide.with(this).load(iconUrl).placeholder(R.drawable.weather_svgrepo_com).into(ivWeatherIcon);
        }

        String lowerCondition = condition.toLowerCase();
        if (temp >= 30) {
            tvWeatherMessage.setText("Too hot to handle? Cool off with something iced.");
            tvRecommendedDrink.setText("Iced Caramel Macchiato");
        } else if (temp >= 25) {
            tvWeatherMessage.setText("Beat the heat—grab a refreshing treat!");
            tvRecommendedDrink.setText("Chocofudge");
        } else if (lowerCondition.contains("rain") || lowerCondition.contains("drizzle") || lowerCondition.contains("storm") || lowerCondition.contains("showers")) {
            tvWeatherMessage.setText("Rain outside? Stay in with a hot bowl of comfort.");
            tvRecommendedDrink.setText("Korean Seafood Ramen");
        } else {
            tvWeatherMessage.setText("Nice weather! Enjoy your favorite drink ☕");
            tvRecommendedDrink.setText("Cappuccino");
        }
    }

    private void showNoAddressState() {
        cardWeather.setVisibility(View.GONE);
        cardNoAddress.setVisibility(View.VISIBLE);
    }

    private void setupNavigation() {
        if (navHome != null) navHome.setOnClickListener(v -> { /* Already here */ });
        if (navProfile != null) navProfile.setOnClickListener(v -> navigateTo(User_Profile.class));
        if (navDiscover != null) navDiscover.setOnClickListener(v -> navigateTo(Discover.class));
        if (navMenu != null) navMenu.setOnClickListener(v -> navigateTo(Menu.class));
        if (navOrders != null) navOrders.setOnClickListener(v -> navigateTo(Orders.class));
    }

    private void navigateTo(Class<?> targetClass) {
        if (this.getClass().equals(targetClass)) return;
        Intent intent = new Intent(this, targetClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}
