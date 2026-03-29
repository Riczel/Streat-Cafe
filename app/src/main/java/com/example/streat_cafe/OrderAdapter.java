package com.example.streat_cafe;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private final Context context;
    private final List<JsonObject> orderList;
    private final OnOrderUpdateListener listener;

    public interface OnOrderUpdateListener {
        void onOrderRefresh();
    }

    public OrderAdapter(Context context, List<JsonObject> orderList, OnOrderUpdateListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        JsonObject order = orderList.get(position);
        
        String displayId = order.has("orderNumber") ? order.get("orderNumber").getAsString() : "Unknown";
        double totalValue = order.has("total") ? order.get("total").getAsDouble() : 0.0;
        String status = order.has("status") ? order.get("status").getAsString().toLowerCase() : "pending";
        String date = order.has("createdAt") ? order.get("createdAt").getAsString().substring(0, 10) : "Today";
        String realId = order.has("_id") ? order.get("_id").getAsString() : "";

        String statusLabel;
        switch (status) {
            case "pending": statusLabel = "PENDING"; break;
            case "approved": statusLabel = "PREPARING"; break;
            case "out_for_delivery": statusLabel = "OUT FOR DELIVERY"; break;
            case "completed": statusLabel = "DELIVERED"; break;
            case "cancelled": statusLabel = "CANCELLED"; break;
            case "rejected": statusLabel = "REJECTED"; break;
            default: statusLabel = status.toUpperCase(); break;
        }

        holder.tvOrderId.setText(String.format("Order #%s", displayId));
        holder.tvOrderTotal.setText(String.format(Locale.getDefault(), "Total: ₱ %.2f", totalValue));
        holder.tvStatus.setText(statusLabel);
        holder.tvOrderDate.setText(date);

        if (holder.tvDeliveryMethod != null && order.has("deliveryMethod")) {
            String method = order.get("deliveryMethod").getAsString();
            holder.tvDeliveryMethod.setText(String.format("%s%s", method.substring(0, 1).toUpperCase(), method.substring(1)));
        }
        if (holder.tvPaymentMethod != null && order.has("paymentMethod")) {
            holder.tvPaymentMethod.setText(String.format("Payment: %s", order.get("paymentMethod").getAsString().toUpperCase()));
        }

        holder.btnCancel.setVisibility(View.GONE);
        holder.btnReceive.setVisibility(View.GONE);
        holder.btnReorder.setVisibility(View.GONE);
        holder.btnUnavailable.setVisibility(View.GONE);

        if (status.equals("pending") || status.equals("approved")) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            boolean isCancellable = status.equals("pending");
            holder.btnCancel.setEnabled(isCancellable);
            holder.btnCancel.setAlpha(isCancellable ? 1.0f : 0.5f);
            holder.btnCancel.setOnClickListener(v -> showCancelConfirmation(realId));
        } else if (status.equals("out_for_delivery")) {
            holder.btnReceive.setVisibility(View.VISIBLE);
            holder.btnReceive.setOnClickListener(v -> updateStatus(realId, "completed"));
        } else if (status.equals("completed") || status.equals("cancelled")) {
            if (status.equals("completed")) {
                holder.btnReorder.setVisibility(View.VISIBLE);
                holder.btnReorder.setOnClickListener(v -> reorderItems(order.getAsJsonArray("items")));
            } else {
                holder.btnUnavailable.setVisibility(View.VISIBLE);
            }
        }

        // Click on item to show details
        holder.itemView.setOnClickListener(v -> showOrderDetailsDialog(order));
    }

    private void showOrderDetailsDialog(JsonObject order) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_order_details, null);
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.CustomDialog)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvOrderIdDetail = dialogView.findViewById(R.id.tvDetailOrderId);
        LinearLayout llItemsContainer = dialogView.findViewById(R.id.llItemsContainer);
        TextView tvTotalDetail = dialogView.findViewById(R.id.tvDetailTotal);
        LinearLayout sectionFeedback = dialogView.findViewById(R.id.sectionFeedback);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etComment = dialogView.findViewById(R.id.etFeedbackComment);
        Button btnSubmitFeedback = dialogView.findViewById(R.id.btnSubmitFeedback);
        Button btnClose = dialogView.findViewById(R.id.btnDetailClose);

        String orderNumber = order.has("orderNumber") ? order.get("orderNumber").getAsString() : "---";
        tvOrderIdDetail.setText(String.format("#SC-%s", orderNumber));
        tvTotalDetail.setText(String.format(Locale.getDefault(), "₱ %.2f", order.get("total").getAsDouble()));

        // Populate Items
        if (order.has("items") && order.get("items").isJsonArray()) {
            JsonArray items = order.getAsJsonArray("items");
            for (JsonElement itemElement : items) {
                JsonObject item = itemElement.getAsJsonObject();
                View itemView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, null);
                TextView text1 = itemView.findViewById(android.R.id.text1);
                TextView text2 = itemView.findViewById(android.R.id.text2);

                String name = item.get("productName").getAsString();
                int qty = item.get("quantity").getAsInt();
                double price = item.get("price").getAsDouble();
                
                text1.setText(String.format(Locale.getDefault(), "%dx %s", qty, name));
                text1.setTextColor(ContextCompat.getColor(context, R.color.dark_brown));
                text2.setText(String.format(Locale.getDefault(), "₱ %.2f", price));
                
                llItemsContainer.addView(itemView);
            }
        }

        // Feedback visibility - only for completed orders
        String status = order.has("status") ? order.get("status").getAsString().toLowerCase() : "";
        if (status.equals("completed")) {
            sectionFeedback.setVisibility(View.VISIBLE);
        } else {
            sectionFeedback.setVisibility(View.GONE);
        }

        btnSubmitFeedback.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String comment = etComment.getText().toString().trim();
            if (rating == 0) {
                Toast.makeText(context, "Please provide a rating", Toast.LENGTH_SHORT).show();
                return;
            }
            submitFeedback(order.get("_id").getAsString(), rating, comment, dialog);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void submitFeedback(String orderId, float rating, String comment, AlertDialog dialog) {
        SharedPreferences sp = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String token = sp.getString("authToken", "");

        JsonObject feedbackData = new JsonObject();
        feedbackData.addProperty("orderId", orderId);
        feedbackData.addProperty("rating", rating);
        feedbackData.addProperty("comment", comment);

        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);
        api.submitFeedback("Bearer " + token, feedbackData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(context, "Failed to submit feedback", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCancelConfirmation(String orderId) {
        new AlertDialog.Builder(context)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order?")
                .setPositiveButton("Yes", (dialog, which) -> updateStatus(orderId, "cancelled"))
                .setNegativeButton("No", null)
                .show();
    }

    private void updateStatus(String orderId, String newStatus) {
        SharedPreferences sp = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String token = sp.getString("authToken", "");

        JsonObject statusData = new JsonObject();
        statusData.addProperty("orderId", orderId);
        statusData.addProperty("status", newStatus);

        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);
        api.updateOrderStatus("Bearer " + token, statusData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && listener != null) {
                    listener.onOrderRefresh();
                }
            }
            @Override public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {}
        });
    }

    private void reorderItems(JsonArray items) {
        SharedPreferences sp = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String token = sp.getString("authToken", "");
        JsonObject cartData = new JsonObject();
        cartData.add("items", items);
        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);
        api.saveCart("Bearer " + token, cartData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) Toast.makeText(context, "Added to cart!", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {}
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderItems, tvOrderTotal, tvStatus, tvOrderDate, tvDeliveryMethod, tvPaymentMethod;
        Button btnCancel, btnReceive, btnReorder, btnUnavailable;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderItems = itemView.findViewById(R.id.tvOrderItems);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvDeliveryMethod = itemView.findViewById(R.id.tvDeliveryMethod);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            btnCancel = itemView.findViewById(R.id.btnCancelOrder);
            btnReceive = itemView.findViewById(R.id.btnReceiveOrder);
            btnReorder = itemView.findViewById(R.id.btnReorder);
            btnUnavailable = itemView.findViewById(R.id.btnUnavailable);
        }
    }
}
