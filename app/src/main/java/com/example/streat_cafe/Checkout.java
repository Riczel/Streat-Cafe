package com.example.streat_cafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Checkout extends AppCompatActivity {

    private View sectionAddress;
    private LinearLayout gcashDetailsContainer, layoutUploadPlaceholder, layoutImagePreview;
    private TextView tvAddressTitle, tvAddressDetails, tvSelectedAddress, tvDeliveryAddressLabel;
    private EditText etFullName, etContactNumber, etReferenceNumber;
    private TextView tvSummarySubtotal, tvSummaryShipping, tvSummaryTotal;
    private Button btnPlaceOrder;
    private RadioButton btnDelivery, btnPickup;
    private RadioGroup rgDeliveryMode;
    private RadioButton btnCod, btnGcash;
    private LinearLayout btnUploadProof;
    private ImageView ivProofPreview, btnBack;
    private TextView tvUploadStatus, btnChangePhoto, btnRemovePhoto;
    private View btnAddAddress;
    private TextView tvCodLabel;
    private View layoutShippingFee;

    private View layoutAddAddressPlaceholder;
    private View layoutAlternativeDisplay;
    private TextView tvAlternativeDetails;
    private String alternativeAddress = "";
    private boolean isAlternativeSelected = false;

    private ApiInterface apiInterface;
    private String authToken;
    private double finalTotal;
    private List<JsonObject> cartItems = new ArrayList<>();
    private JsonObject userProfile;
    
    private String selectedPaymentMethod = "Cash on Delivery";
    private String selectedDeliveryMode = "Delivery";
    private Uri selectedImageUri;

    private String userHomeAddress = "";
    private JsonObject selectedHomeAddressObj;
    private JsonObject selectedAltAddressObj;
    
    private static final String STORE_ADDRESS = "Purok Apitong St. Harap ng flying V, General Mamerto Natividad, Philippines, 3125";
    private static final double DELIVERY_FEE = 10.0;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        ivProofPreview.setImageBitmap(bitmap);
                        
                        layoutUploadPlaceholder.setVisibility(View.GONE);
                        layoutImagePreview.setVisibility(View.VISIBLE);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_checkout);

        apiInterface = RetrofitClient.getClient().create(ApiInterface.class);
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", "");

        etFullName = findViewById(R.id.etFullName);
        etContactNumber = findViewById(R.id.etContactNumber);
        btnBack = findViewById(R.id.btnBack);
        
        rgDeliveryMode = findViewById(R.id.rgDeliveryMode);
        btnDelivery = findViewById(R.id.btnDelivery);
        btnPickup = findViewById(R.id.btnPickup);
        
        sectionAddress = findViewById(R.id.sectionAddress);
        tvAddressTitle = findViewById(R.id.tvAddressTitle);
        tvAddressDetails = findViewById(R.id.tvAddressDetails);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        tvDeliveryAddressLabel = findViewById(R.id.tvDeliveryAddressLabel);

        btnCod = findViewById(R.id.rbCod);
        btnGcash = findViewById(R.id.rbGcash);
        tvCodLabel = findViewById(R.id.rbCod);
        
        gcashDetailsContainer = findViewById(R.id.gcashDetailsContainer);
        etReferenceNumber = findViewById(R.id.etReferenceNumber);
        btnUploadProof = findViewById(R.id.btnUploadProof);
        layoutUploadPlaceholder = findViewById(R.id.layoutUploadPlaceholder);
        layoutImagePreview = findViewById(R.id.layoutImagePreview);
        ivProofPreview = findViewById(R.id.ivProofPreview);
        tvUploadStatus = findViewById(R.id.tvUploadStatus);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto);
        
        tvSummarySubtotal = findViewById(R.id.tvSummarySubtotal);
        tvSummaryShipping = findViewById(R.id.tvSummaryShipping);
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        layoutShippingFee = findViewById(R.id.layoutShippingFee);

        btnAddAddress = findViewById(R.id.btnAddAddress);
        layoutAddAddressPlaceholder = findViewById(R.id.layoutAddAddressPlaceholder);
        layoutAlternativeDisplay = findViewById(R.id.layoutAlternativeDisplay);
        tvAlternativeDetails = findViewById(R.id.tvAlternativeDetails);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        updatePaymentUI();
        updateDeliveryUI();

        if (rgDeliveryMode != null) {
            rgDeliveryMode.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.btnDelivery) {
                    selectedDeliveryMode = "Delivery";
                } else if (checkedId == R.id.btnPickup) {
                    selectedDeliveryMode = "Pickup";
                }
                updateDeliveryUI();
            });
        }

        if (btnCod != null) {
            btnCod.setOnClickListener(v -> {
                selectedPaymentMethod = (selectedDeliveryMode.equals("Pickup")) ? "Pay in Cash" : "Cash on Delivery";
                if (gcashDetailsContainer != null) gcashDetailsContainer.setVisibility(View.GONE);
                updatePaymentUI();
            });
        }
        if (btnGcash != null) {
            btnGcash.setOnClickListener(v -> {
                selectedPaymentMethod = "Gcash";
                if (gcashDetailsContainer != null) gcashDetailsContainer.setVisibility(View.VISIBLE);
                updatePaymentUI();
            });
        }

        if (btnUploadProof != null) {
            btnUploadProof.setOnClickListener(v -> {
                if (selectedImageUri == null) openGallery();
            });
        }

        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> openGallery());
        }

        if (btnRemovePhoto != null) {
            btnRemovePhoto.setOnClickListener(v -> {
                selectedImageUri = null;
                ivProofPreview.setImageDrawable(null);
                layoutImagePreview.setVisibility(View.GONE);
                layoutUploadPlaceholder.setVisibility(View.VISIBLE);
            });
        }

        double subtotal = getIntent().getDoubleExtra("subtotal", 0.0);
        if (tvSummarySubtotal != null) tvSummarySubtotal.setText(String.format(Locale.getDefault(), "₱ %.2f", subtotal));

        fetchUserProfile();
        fetchCartItems();

        if (sectionAddress != null) {
            sectionAddress.setOnClickListener(v -> {
                isAlternativeSelected = false;
                updateAddressSelectionUI();
            });
        }
        
        if (btnAddAddress != null) {
            btnAddAddress.setOnClickListener(v -> {
                 if (alternativeAddress.isEmpty()) {
                     Intent intent = new Intent(this, User_Profile.class);
                     startActivity(intent);
                 } else {
                     isAlternativeSelected = true;
                     updateAddressSelectionUI();
                 }
            });
        }

        if (btnPlaceOrder != null) {
            btnPlaceOrder.setOnClickListener(v -> {
                if (etFullName != null && etFullName.getText().toString().trim().isEmpty()) {
                    Toast.makeText(this, "Please enter full name", Toast.LENGTH_SHORT).show();
                    return;
                }
                String currentAddress = tvSelectedAddress != null ? tvSelectedAddress.getText().toString() : "";
                if (selectedDeliveryMode.equals("Delivery") && (currentAddress.isEmpty() || currentAddress.equals("No address saved"))) {
                    Toast.makeText(this, "Please select a delivery address", Toast.LENGTH_LONG).show();
                    return;
                }
                if (selectedPaymentMethod.equals("Gcash")) {
                    if (etReferenceNumber != null && etReferenceNumber.getText().toString().trim().isEmpty()) {
                        Toast.makeText(this, "Please enter Gcash reference number", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (selectedImageUri == null) {
                        Toast.makeText(this, "Please upload proof of payment", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                placeOrder();
            });
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void updateDeliveryUI() {
        if (btnDelivery == null || btnPickup == null) return;
        
        double subtotal = getIntent().getDoubleExtra("subtotal", 0.0);
        double shipping;

        if (selectedDeliveryMode.equals("Delivery")) {
            shipping = DELIVERY_FEE;
            if (layoutShippingFee != null) layoutShippingFee.setVisibility(View.VISIBLE);
            
            if (tvAddressTitle != null) tvAddressTitle.setText("HOME");
            if (tvAddressDetails != null) tvAddressDetails.setText(userHomeAddress.isEmpty() ? "No address saved" : userHomeAddress);
            
            if (tvDeliveryAddressLabel != null) tvDeliveryAddressLabel.setVisibility(View.VISIBLE);
            if (tvSelectedAddress != null) {
                tvSelectedAddress.setVisibility(View.VISIBLE);
                updateAddressSelectionUI();
            }
            if (btnAddAddress != null) btnAddAddress.setVisibility(View.VISIBLE);
            
            if (tvCodLabel != null) tvCodLabel.setText("Cash on Delivery");
            if (selectedPaymentMethod.equals("Pay in Cash")) selectedPaymentMethod = "Cash on Delivery";
            
        } else {
            shipping = 0.0;
            if (layoutShippingFee != null) layoutShippingFee.setVisibility(View.GONE);
            
            if (tvAddressTitle != null) tvAddressTitle.setText("STORE ADDRESS");
            if (tvAddressDetails != null) tvAddressDetails.setText(STORE_ADDRESS);
            
            if (tvDeliveryAddressLabel != null) tvDeliveryAddressLabel.setVisibility(View.GONE);
            if (tvSelectedAddress != null) {
                tvSelectedAddress.setVisibility(View.GONE);
                tvSelectedAddress.setText(STORE_ADDRESS);
            }
            if (btnAddAddress != null) btnAddAddress.setVisibility(View.GONE);
            
            if (tvCodLabel != null) tvCodLabel.setText("Pay in Cash");
            if (selectedPaymentMethod.equals("Cash on Delivery")) selectedPaymentMethod = "Pay in Cash";
        }
        
        finalTotal = subtotal + shipping;
        if (tvSummaryShipping != null) tvSummaryShipping.setText(String.format(Locale.getDefault(), "₱ %.2f", shipping));
        if (tvSummaryTotal != null) tvSummaryTotal.setText(String.format(Locale.getDefault(), "₱ %.2f", finalTotal));
        
        updatePaymentUI();
    }

    private void updateAddressSelectionUI() {
        if (sectionAddress != null) sectionAddress.setBackgroundTintList(null);
        if (btnAddAddress != null) btnAddAddress.setBackgroundTintList(null);

        if (isAlternativeSelected && !alternativeAddress.isEmpty()) {
            if (sectionAddress != null) sectionAddress.setBackgroundResource(R.drawable.bg_address_unselected);
            if (btnAddAddress != null) btnAddAddress.setBackgroundResource(R.drawable.bg_address_selected);
            if (tvSelectedAddress != null) tvSelectedAddress.setText(alternativeAddress);
        } else {
            if (sectionAddress != null) sectionAddress.setBackgroundResource(R.drawable.bg_address_selected);
            if (btnAddAddress != null) btnAddAddress.setBackgroundResource(R.drawable.bg_address_unselected);
            if (tvSelectedAddress != null) tvSelectedAddress.setText(userHomeAddress);
        }
    }

    private void fetchUserProfile() {
        if (authToken == null || authToken.isEmpty()) return;
        apiInterface.getProfile("Bearer " + authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userProfile = response.body();
                    
                    String fName = userProfile.has("firstName") ? userProfile.get("firstName").getAsString() : "";
                    String lName = userProfile.has("lastName") ? userProfile.get("lastName").getAsString() : "";
                    String phone = userProfile.has("phoneNumber") ? userProfile.get("phoneNumber").getAsString() : "";
                    
                    if (etFullName != null) etFullName.setText(fName + " " + lName);
                    if (etContactNumber != null) etContactNumber.setText(phone);

                    loadInitialAddresses();
                }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("Checkout", "Profile Fetch Error", t);
            }
        });
    }

    private void loadInitialAddresses() {
        if (userProfile != null && userProfile.has("addresses") && userProfile.get("addresses").isJsonArray()) {
            JsonArray addrArray = userProfile.getAsJsonArray("addresses");
            if (addrArray.size() > 0) {
                JsonObject primary = null;
                JsonObject alt = null;
                
                for (JsonElement e : addrArray) {
                    JsonObject o = e.getAsJsonObject();
                    if (o.has("isDefault") && o.get("isDefault").getAsBoolean()) {
                        primary = o;
                    } else if (alt == null) {
                        alt = o;
                    }
                }
                
                if (primary == null) primary = addrArray.get(0).getAsJsonObject();
                selectedHomeAddressObj = primary;
                userHomeAddress = formatAddress(primary);
                
                if (alt != null) {
                    selectedAltAddressObj = alt;
                    alternativeAddress = formatAddress(alt);
                    showAlternativeUI(alternativeAddress);
                } else {
                    selectedAltAddressObj = null;
                    alternativeAddress = "";
                    hideAlternativeUI();
                }
                
                updateDeliveryUI();
                return;
            }
        }
        selectedHomeAddressObj = null;
        selectedAltAddressObj = null;
        userHomeAddress = "";
        alternativeAddress = "";
        hideAlternativeUI();
        updateDeliveryUI();
    }

    private String formatAddress(JsonObject addr) {
        String street = addr.has("street") ? addr.get("street").getAsString() : "";
        String brgy = addr.has("barangay") ? addr.get("barangay").getAsString() : "";
        String muni = addr.has("municipality") ? addr.get("municipality").getAsString() : "";
        String prov = addr.has("province") ? addr.get("province").getAsString() : "";
        return street + ", " + brgy + ", " + muni + ", " + prov;
    }

    private void showAlternativeUI(String address) {
        if (layoutAddAddressPlaceholder != null) layoutAddAddressPlaceholder.setVisibility(View.GONE);
        if (layoutAlternativeDisplay != null) layoutAlternativeDisplay.setVisibility(View.VISIBLE);
        if (tvAlternativeDetails != null) tvAlternativeDetails.setText(address);
    }

    private void hideAlternativeUI() {
        if (layoutAddAddressPlaceholder != null) layoutAddAddressPlaceholder.setVisibility(View.VISIBLE);
        if (layoutAlternativeDisplay != null) layoutAlternativeDisplay.setVisibility(View.GONE);
    }

    private void updatePaymentUI() {
        if (btnCod == null || btnGcash == null) return;
        
        boolean isCod = selectedPaymentMethod.equalsIgnoreCase("Cash on Delivery") || 
                        selectedPaymentMethod.equalsIgnoreCase("Pay in Cash");
        
        btnCod.setChecked(isCod);
        btnGcash.setChecked(!isCod);
        
        if (isCod) {
            btnCod.setBackgroundResource(R.drawable.bg_status_selected);
            btnGcash.setBackgroundResource(R.drawable.bg_status_unselected);
            if (gcashDetailsContainer != null) gcashDetailsContainer.setVisibility(View.GONE);
        } else {
            btnCod.setBackgroundResource(R.drawable.bg_status_unselected);
            btnGcash.setBackgroundResource(R.drawable.bg_status_selected);
            if (gcashDetailsContainer != null) gcashDetailsContainer.setVisibility(View.VISIBLE);
        }
    }

    private void fetchCartItems() {
        if (authToken == null || authToken.isEmpty()) return;
        apiInterface.getCart("Bearer " + authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null && response.body().has("cart")) {
                    JsonArray cartArray = response.body().getAsJsonArray("cart");
                    cartItems.clear();
                    for (JsonElement e : cartArray) cartItems.add(e.getAsJsonObject());
                }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private String encodeImageToBase64(Uri imageUri) {
        if (imageUri == null) return null;
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void placeOrder() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        double subtotal = getIntent().getDoubleExtra("subtotal", 0.0);
        JsonObject orderData = new JsonObject();
        
        JsonArray itemsArray = new JsonArray();
        for (JsonObject item : cartItems) itemsArray.add(item);
        orderData.add("items", itemsArray);
        
        orderData.addProperty("subtotal", subtotal);
        orderData.addProperty("total", finalTotal);
        
        String deliveryMethod = selectedDeliveryMode.toLowerCase();
        if (deliveryMethod.contains("pickup")) deliveryMethod = "pickup";
        else deliveryMethod = "delivery";
        orderData.addProperty("deliveryMethod", deliveryMethod);

        if (deliveryMethod.equals("delivery")) {
            JsonObject addressPayload = new JsonObject();
            JsonObject selectedObj = isAlternativeSelected ? selectedAltAddressObj : selectedHomeAddressObj;
            
            if (selectedObj != null) {
                addressPayload.addProperty("label", selectedObj.has("label") ? selectedObj.get("label").getAsString() : (isAlternativeSelected ? "ALT" : "HOME"));
                addressPayload.addProperty("street", selectedObj.has("street") ? selectedObj.get("street").getAsString() : "");
                addressPayload.addProperty("barangay", selectedObj.has("barangay") ? selectedObj.get("barangay").getAsString() : "");
                addressPayload.addProperty("municipality", selectedObj.has("municipality") ? selectedObj.get("municipality").getAsString() : "General Mamerto Natividad");
                addressPayload.addProperty("province", selectedObj.has("province") ? selectedObj.get("province").getAsString() : "Nueva Ecija");
            }
            orderData.add("deliveryAddress", addressPayload);
        }

        String paymentMethod = selectedPaymentMethod.toLowerCase();
        if (paymentMethod.contains("gcash")) paymentMethod = "gcash";
        else paymentMethod = "cod";
        orderData.addProperty("paymentMethod", paymentMethod);

        if (paymentMethod.equals("gcash")) {
            JsonObject gcashDetails = new JsonObject();
            gcashDetails.addProperty("referenceNumber", etReferenceNumber.getText().toString().trim());
            String base64Image = encodeImageToBase64(selectedImageUri);
            if (base64Image != null) {
                gcashDetails.addProperty("proof", base64Image);
            }
            orderData.add("gcashDetails", gcashDetails);
        }

        Log.d("Checkout", "Placing order with payload: " + orderData.toString());

        apiInterface.placeOrder("Bearer " + authToken, orderData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String serverOrderNum = response.body().has("orderNumber") ? response.body().get("orderNumber").getAsString() : null;
                    clearCartAndShowSuccess(serverOrderNum);
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "No error message";
                        Log.e("Checkout", "Order failed: " + response.code() + " - " + errorMsg);
                        Toast.makeText(Checkout.this, "Server Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("Checkout", "Network error", t);
                Toast.makeText(Checkout.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearCartAndShowSuccess(String orderNum) {
        JsonObject emptyPayload = new JsonObject();
        emptyPayload.add("cart", new JsonArray());
        
        apiInterface.saveCart("Bearer " + authToken, emptyPayload).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                showOrderConfirmedDialog(orderNum);
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {
                showOrderConfirmedDialog(orderNum);
            }
        });
    }

    private void showOrderConfirmedDialog(String orderNum) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_order_confirmed, null);
        builder.setView(dialogView);

        TextView tvOrderID = dialogView.findViewById(R.id.confOrderID);
        TextView tvTotal = dialogView.findViewById(R.id.confTotal);
        TextView tvPayment = dialogView.findViewById(R.id.confPayment);
        TextView tvItems = dialogView.findViewById(R.id.confItems);
        TextView tvAddress = dialogView.findViewById(R.id.confAddress);
        Button btnBackHome = dialogView.findViewById(R.id.btnBackHome);

        // Use the real order number from server if available
        String displayID = (orderNum != null) ? "#" + orderNum : "#SC-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        if (tvOrderID != null) tvOrderID.setText(displayID);

        if (tvTotal != null) tvTotal.setText(String.format(Locale.getDefault(), "₱ %.2f", finalTotal));
        if (tvPayment != null) tvPayment.setText(selectedPaymentMethod);
        if (tvAddress != null) tvAddress.setText(tvSelectedAddress != null ? tvSelectedAddress.getText().toString() : "");

        StringBuilder itemsSummary = new StringBuilder();
        for (JsonObject item : cartItems) {
            String name = item.has("productName") ? item.get("productName").getAsString() : "Unknown";
            int qty = item.has("quantity") ? item.get("quantity").getAsInt() : 1;
            itemsSummary.append(name).append(" x").append(qty).append("\n");
        }
        if (tvItems != null) tvItems.setText(itemsSummary.toString().trim());

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnBackHome != null) {
            btnBackHome.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(Checkout.this, Orders.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }

        dialog.setCancelable(false);
        dialog.show();
    }
}
