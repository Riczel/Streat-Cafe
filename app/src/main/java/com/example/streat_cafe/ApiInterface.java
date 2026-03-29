package com.example.streat_cafe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiInterface {

    @POST("api/signup")
    Call<JsonObject> registerUser(@Body JsonObject userDetails);

    @POST("api/login")
    Call<JsonObject> loginUser(@Body JsonObject credentials);

    @POST("api/auth/google/android")
    Call<JsonObject> loginWithGoogle(@Body JsonObject body);
    
    @GET("api/getProducts")
    Call<List<JsonObject>> getProducts();

    @GET("api/me")
    Call<JsonObject> getProfile(@Header("Authorization") String token);

    @GET("api/getCart")
    Call<JsonObject> getCart(@Header("Authorization") String token);

    @POST("api/saveCart")
    Call<JsonObject> saveCart(@Header("Authorization") String token, @Body JsonObject cartData);

    @POST("api/createProducts")
    Call<JsonObject> createProduct(@Body JsonObject productDetails);

    @POST("api/placeOrder")
    Call<JsonObject> placeOrder(@Header("Authorization") String token, @Body JsonObject orderData);

    @GET("api/getOrders")
    Call<JsonArray> getOrders(@Header("Authorization") String token);

    @GET("api/getRecentOrders")
    Call<JsonArray> getRecentOrders(@Header("Authorization") String token);

    @POST("api/updateOrderStatus")
    Call<JsonObject> updateOrderStatus(@Header("Authorization") String token, @Body JsonObject statusData);

    @POST("api/submitFeedback")
    Call<JsonObject> submitFeedback(@Header("Authorization") String token, @Body JsonObject feedbackData);

    @POST("api/updateProfile")
    Call<JsonObject> updateProfile(@Header("Authorization") String token, @Body JsonObject profileData);

    @DELETE("api/deleteAddress")
    Call<JsonObject> deleteAddress(@Header("Authorization") String token);

    @POST("api/updateProductAvailability")
    Call<JsonObject> updateProductAvailability(@Body JsonObject availabilityData);

}
