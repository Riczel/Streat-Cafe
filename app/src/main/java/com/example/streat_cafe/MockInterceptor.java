package com.example.streat_cafe;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MockInterceptor implements Interceptor {

    private static final List<JsonObject> mockOrders = new ArrayList<>();
    private static JsonObject mockCart;
    private static final List<JsonObject> mockProducts = new ArrayList<>();
    private static final Gson gson = new Gson();

    static {
        String initialCartJson = "{\"cart\":[{\"sizePrice\":0,\"productName\":\"Streat CC Oreo\",\"size\":\"Large\",\"quantity\":1,\"price\":95,\"image\":\"https://storage.googleapis.com/streat-cafe/product-images/sc_p1.jpg\",\"_id\":\"69bf694ce3b7a51eea9a5936\",\"addons\":[]}]}";
        mockCart = new JsonParser().parse(initialCartJson).getAsJsonObject();
        
        // Initialize some mock products if they don't exist
        mockProducts.add(createMockProduct("69bf694ce3b7a51eea9a5936", "Streat CC Oreo", "Creamy cheesecake with Oreo bits", 95, "cheesecake", "https://storage.googleapis.com/streat-cafe/product-images/sc_p1.jpg", true));
        mockProducts.add(createMockProduct("69bf694ce3b7a51eea9a5937", "Iced Coffee", "Freshly brewed iced coffee", 75, "coffee", "https://storage.googleapis.com/streat-cafe/product-images/coffee.jpg", true));
    }

    private static JsonObject createMockProduct(String id, String name, String desc, double price, String cat, String img, boolean available) {
        JsonObject p = new JsonObject();
        p.addProperty("_id", id);
        p.addProperty("productName", name);
        p.addProperty("productDescription", desc);
        p.addProperty("price", price);
        p.addProperty("category", cat);
        p.addProperty("image", img);
        p.addProperty("available", available);
        p.add("sizes", new JsonArray());
        p.add("addons", new JsonArray());
        p.add("sugarLevel", new JsonArray());
        return p;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String uri = chain.request().url().uri().toString();
        String method = chain.request().method();

        if (uri.contains("api/placeOrder") && method.equalsIgnoreCase("POST")) {
            return mockPlaceOrder(chain);
        } else if (uri.contains("api/getOrders") && method.equalsIgnoreCase("GET")) {
            return mockGetOrders(chain);
        } else if (uri.contains("api/updateOrderStatus") && method.equalsIgnoreCase("POST")) {
            return mockUpdateOrderStatus(chain);
        } else if (uri.contains("api/submitFeedback") && method.equalsIgnoreCase("POST")) {
            return createSuccessResponse(chain, "Feedback submitted successfully");
        } else if (uri.contains("api/getCart") && method.equalsIgnoreCase("GET")) {
            return mockGetCart(chain);
        } else if (uri.contains("api/saveCart") && method.equalsIgnoreCase("POST")) {
            return mockSaveCart(chain);
        } else if (uri.contains("api/getProducts") && method.equalsIgnoreCase("GET")) {
            return mockGetProducts(chain);
        } else if (uri.contains("api/updateProductAvailability") && method.equalsIgnoreCase("POST")) {
            return mockUpdateProductAvailability(chain);
        }

        return chain.proceed(chain.request());
    }

    private Response mockGetProducts(Chain chain) {
        return createResponse(chain, gson.toJson(mockProducts));
    }

    private Response mockUpdateProductAvailability(Chain chain) throws IOException {
        okio.Buffer buffer = new okio.Buffer();
        if (chain.request().body() != null) {
            chain.request().body().writeTo(buffer);
        }
        String requestBody = buffer.readUtf8();
        JsonObject data = gson.fromJson(requestBody, JsonObject.class);
        
        String productId = data.has("productId") ? data.get("productId").getAsString() : "";
        boolean available = data.has("available") && data.get("available").getAsBoolean();

        boolean found = false;
        for (JsonObject p : mockProducts) {
            if (p.get("_id").getAsString().equals(productId)) {
                p.addProperty("available", available);
                found = true;
                break;
            }
        }

        JsonObject resp = new JsonObject();
        if (found) {
            resp.addProperty("message", "Product availability updated successfully");
            return createResponse(chain, resp.toString());
        } else {
            resp.addProperty("message", "Product not found");
            return new Response.Builder()
                    .code(404)
                    .message("Not Found")
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .body(ResponseBody.create(MediaType.parse("application/json"), resp.toString()))
                    .addHeader("content-type", "application/json")
                    .build();
        }
    }

    private Response mockPlaceOrder(Chain chain) throws IOException {
        okio.Buffer buffer = new okio.Buffer();
        if (chain.request().body() != null) {
            chain.request().body().writeTo(buffer);
        }
        String requestBody = buffer.readUtf8();
        JsonObject orderData = gson.fromJson(requestBody, JsonObject.class);

        orderData.addProperty("_id", UUID.randomUUID().toString());
        if (!orderData.has("status")) {
            orderData.addProperty("status", "Pending");
        }
        orderData.addProperty("orderDate", new java.util.Date().toString());

        mockOrders.add(orderData);

        mockCart = new JsonObject();
        mockCart.add("cart", new JsonArray());

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("success", true);
        responseJson.addProperty("message", "Order placed successfully");
        responseJson.add("order", orderData);

        return createResponse(chain, responseJson.toString());
    }

    private Response mockGetOrders(Chain chain) {
        JsonObject responseJson = new JsonObject();
        JsonArray ordersArray = new JsonArray();
        for (JsonObject order : mockOrders) {
            ordersArray.add(order);
        }
        responseJson.add("orders", ordersArray);
        responseJson.addProperty("success", true);

        return createResponse(chain, responseJson.toString());
    }

    private Response mockUpdateOrderStatus(Chain chain) throws IOException {
        okio.Buffer buffer = new okio.Buffer();
        if (chain.request().body() != null) {
            chain.request().body().writeTo(buffer);
        }
        String requestBody = buffer.readUtf8();
        JsonObject statusData = gson.fromJson(requestBody, JsonObject.class);

        String orderId = statusData.has("orderId") ? statusData.get("orderId").getAsString() : "";
        String newStatus = statusData.has("status") ? statusData.get("status").getAsString() : "";

        boolean found = false;
        for (JsonObject order : mockOrders) {
            if (order.has("_id") && order.get("_id").getAsString().equals(orderId)) {
                order.addProperty("status", newStatus);
                found = true;
                break;
            }
        }

        JsonObject responseJson = new JsonObject();
        if (found) {
            responseJson.addProperty("success", true);
            responseJson.addProperty("message", "Status updated to " + newStatus);
        } else {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "Order not found");
        }

        return createResponse(chain, responseJson.toString());
    }

    private Response mockGetCart(Chain chain) {
        return createResponse(chain, mockCart.toString());
    }

    private Response mockSaveCart(Chain chain) throws IOException {
        okio.Buffer buffer = new okio.Buffer();
        if (chain.request().body() != null) {
            chain.request().body().writeTo(buffer);
        }
        String requestBody = buffer.readUtf8();
        mockCart = gson.fromJson(requestBody, JsonObject.class);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("success", true);
        responseJson.addProperty("message", "Cart saved successfully");
        return createResponse(chain, responseJson.toString());
    }

    private Response createSuccessResponse(Chain chain, String message) {
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("success", true);
        responseJson.addProperty("message", message);
        return createResponse(chain, responseJson.toString());
    }

    private Response createResponse(Chain chain, String json) {
        return new Response.Builder()
                .code(200)
                .message("OK")
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .body(ResponseBody.create(MediaType.parse("application/json"), json))
                .addHeader("content-type", "application/json")
                .build();
    }
}
