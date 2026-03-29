package com.example.streat_cafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class User_Profile extends AppCompatActivity {

    private TextView tvProfileName, tvProfileEmail;
    private EditText etFirstName, etLastName, etPhoneNumber;
    private EditText etHouseNo, etMunicipality, etProvince;
    private AutoCompleteTextView etBarangay;
    private ImageView ivProfilePic;
    private FloatingActionButton btnChangePic;
    private LinearLayout layoutPicOptions;
    private TextView btnPicChange, btnPicRemove;

    private LinearLayout layoutAlternativeAddress;
    private EditText etAltHouseNo, etAltMunicipality, etAltProvince;
    private AutoCompleteTextView etAltBarangay;
    private TextView btnCancelAlt, btnSaveAlt, btnAddAddress;
    private LinearLayout layoutAltTopButtons, layoutAltBottomButtons;
    private TextView btnEditAlt, btnDeleteAlt;

    private LinearLayout btnSignOut, btnMyCart, btnRecentOrders, layoutEditControls;
    private TextView btnEditPersonal, btnEditAddress;
    private Button btnCancel, btnSave;
    
    private String authToken;
    private ApiInterface apiInterface;
    private boolean hasAltAddress = false;
    private boolean isEditingAlt = false;
    private boolean hasPrimaryAddress = false;
    private String currentProfilePicUrl = "";
    private SwipeRefreshLayout swipeRefreshLayout;

    private final String CITY_VALUE = "General Mamerto Natividad";
    private final String PROVINCE_VALUE = "Nueva Ecija";
    private final String TAG = "STREAT_DEBUG";

    private final String[] barangays = {
            "Balangkare Norte", "Balangkare Sur", "Balaring", "Belen", "Bravo",
            "Burol", "Kabulihan", "Mag-asawang Sampaloc", "Manarog", "Mataas na Kahoy",
            "Panacsac", "Picaleon", "Piñahan", "Platero", "Poblacion",
            "Pula", "Pulong Singkamas", "Sapang Bato", "Talabutab Norte", "Talabutab Sur"
    };

    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        apiInterface = RetrofitClient.getClient().create(ApiInterface.class);
        
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        authToken = sharedPreferences.getString("authToken", "");

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        ivProfilePic = findViewById(R.id.ivProfilePic);
        btnChangePic = findViewById(R.id.btnChangePic);
        layoutPicOptions = findViewById(R.id.layoutPicOptions);
        btnPicChange = findViewById(R.id.btnPicChange);
        btnPicRemove = findViewById(R.id.btnPicRemove);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        
        etHouseNo = findViewById(R.id.etHouseNo);
        etBarangay = findViewById(R.id.etBarangay);
        etMunicipality = findViewById(R.id.etMunicipality);
        etProvince = findViewById(R.id.etProvince);

        layoutAlternativeAddress = findViewById(R.id.layoutAlternativeAddress);
        etAltHouseNo = findViewById(R.id.etAltHouseNo);
        etAltBarangay = findViewById(R.id.etAltBarangay);
        etAltMunicipality = findViewById(R.id.etAltMunicipality);
        etAltProvince = findViewById(R.id.etAltProvince);
        btnCancelAlt = findViewById(R.id.btnCancelAlt);
        btnSaveAlt = findViewById(R.id.btnSaveAlt);
        btnAddAddress = findViewById(R.id.btnAddAddress);
        layoutAltTopButtons = findViewById(R.id.layoutAltTopButtons);
        layoutAltBottomButtons = findViewById(R.id.layoutAltBottomButtons);
        btnEditAlt = findViewById(R.id.btnEditAlt);
        btnDeleteAlt = findViewById(R.id.btnDeleteAlt);

        btnEditPersonal = findViewById(R.id.btnEditPersonal);
        btnEditAddress = findViewById(R.id.btnEditAddress);
        layoutEditControls = findViewById(R.id.layoutEditControls);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);

        btnSignOut = findViewById(R.id.btnSignOut);
        btnMyCart = findViewById(R.id.btnMyCart);
        btnRecentOrders = findViewById(R.id.btnRecentOrders);

        etMunicipality.setText(CITY_VALUE);
        etProvince.setText(PROVINCE_VALUE);
        etAltMunicipality.setText(CITY_VALUE);
        etAltProvince.setText(PROVINCE_VALUE);
        
        etMunicipality.setEnabled(false);
        etProvince.setEnabled(false);
        etAltMunicipality.setEnabled(false);
        etAltProvince.setEnabled(false);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, barangays);
        etBarangay.setAdapter(adapter);
        etAltBarangay.setAdapter(adapter);

        etBarangay.setOnClickListener(v -> {
            if (etBarangay.isEnabled()) etBarangay.showDropDown();
        });
        etAltBarangay.setOnClickListener(v -> {
            if (etAltBarangay.isEnabled()) etAltBarangay.showDropDown();
        });

        btnAddAddress.setOnClickListener(v -> {
            btnAddAddress.setVisibility(View.GONE);
            layoutAlternativeAddress.setVisibility(View.VISIBLE);
            enableAltFields(true);
        });

        btnCancelAlt.setOnClickListener(v -> {
            if (hasAltAddress) {
                enableAltFields(false);
                loadUserData();
            } else {
                layoutAlternativeAddress.setVisibility(View.GONE);
                updateAddAddressButtonVisibility();
                clearAltFields();
                isEditingAlt = false;
            }
        });

        btnSaveAlt.setOnClickListener(v -> saveProfileChanges(true));

        btnEditAlt.setOnClickListener(v -> enableAltFields(true));
        
        btnDeleteAlt.setOnClickListener(v -> deleteAlternativeAddress());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnEditPersonal.setOnClickListener(v -> enableEditing(true));
        btnEditAddress.setOnClickListener(v -> enableEditing(true));

        btnCancel.setOnClickListener(v -> {
            enableEditing(false);
            loadUserData(); 
        });

        btnSave.setOnClickListener(v -> saveProfileChanges(false));

        btnSignOut.setOnClickListener(v -> {
            // Sign out from Google
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
            GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(User_Profile.this, gso);
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
                startActivity(new Intent(User_Profile.this, Sign_in.class));
                finishAffinity();
            });
        });

        btnMyCart.setOnClickListener(v -> navigateTo(Cart.class));

        if (btnRecentOrders != null) {
            btnRecentOrders.setOnClickListener(v -> {
                Intent intent = new Intent(User_Profile.this, RecentOrders.class);
                startActivity(intent);
            });
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadUserData);
        }

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadProfilePicture(imageUri);
                        }
                    }
                    layoutPicOptions.setVisibility(View.GONE);
                }
        );

        btnChangePic.setOnClickListener(v -> {
            if (currentProfilePicUrl == null || currentProfilePicUrl.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickImageLauncher.launch(intent);
            } else {
                if (layoutPicOptions.getVisibility() == View.VISIBLE) {
                    layoutPicOptions.setVisibility(View.GONE);
                } else {
                    layoutPicOptions.setVisibility(View.VISIBLE);
                }
            }
        });

        btnPicChange.setOnClickListener(v -> {
            new AlertDialog.Builder(User_Profile.this)
                    .setTitle("Confirmation")
                    .setMessage("Are you want to change your profile?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        pickImageLauncher.launch(intent);
                    })
                    .setNegativeButton("No", (dialog, which) -> layoutPicOptions.setVisibility(View.GONE))
                    .show();
        });

        btnPicRemove.setOnClickListener(v -> {
            new AlertDialog.Builder(User_Profile.this)
                    .setTitle("Confirmation")
                    .setMessage("Are you want to delete you profile?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        removeProfilePicture();
                        layoutPicOptions.setVisibility(View.GONE);
                    })
                    .setNegativeButton("No", (dialog, which) -> layoutPicOptions.setVisibility(View.GONE))
                    .show();
        });

        setupNavigation();
        loadUserData();
    }

    private void removeProfilePicture() {
        if (authToken == null || authToken.isEmpty()) return;
        
        JsonObject payload = new JsonObject();
        payload.addProperty("profilePicture", " ");

        apiInterface.updateProfile("Bearer " + authToken, payload).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(User_Profile.this, "Profile picture deleted", Toast.LENGTH_SHORT).show();
                    ivProfilePic.setImageResource(R.drawable.istockphoto_1225790722_612x612_removebg_preview);
                    currentProfilePicUrl = "";
                    loadUserData();
                } else {
                    Toast.makeText(User_Profile.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(User_Profile.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadProfilePicture(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            

            int maxWidth = 400;
            int maxHeight = 400;
            if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight) {
                float scale = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
                bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(scale * bitmap.getWidth()), Math.round(scale * bitmap.getHeight()), true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            JsonObject payload = new JsonObject();
            payload.addProperty("profilePicture", "data:image/jpeg;base64," + base64Image);

            apiInterface.updateProfile("Bearer " + authToken, payload).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(User_Profile.this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                        // Update local UI immediately
                        Glide.with(User_Profile.this)
                                .load(imageBytes)
                                .placeholder(R.drawable.istockphoto_1225790722_612x612_removebg_preview)
                                .circleCrop()
                                .into(ivProfilePic);
                        currentProfilePicUrl = "has_image";
                        loadUserData();
                    } else {
                        Toast.makeText(User_Profile.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Toast.makeText(User_Profile.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearAltFields() {
        etAltHouseNo.setText("");
        etAltBarangay.setText("", false);
        etAltMunicipality.setText(CITY_VALUE);
        etAltProvince.setText(PROVINCE_VALUE);
    }

    private void enableAltFields(boolean enable) {
        isEditingAlt = enable;
        etAltHouseNo.setEnabled(enable);
        etAltBarangay.setEnabled(enable);
        etAltMunicipality.setEnabled(false);
        etAltProvince.setEnabled(false);
        
        if (enable) {
            layoutAltBottomButtons.setVisibility(View.VISIBLE);
            layoutAltTopButtons.setVisibility(View.GONE);
        } else {
            layoutAltBottomButtons.setVisibility(View.GONE);
            layoutAltTopButtons.setVisibility(View.VISIBLE);
        }
    }

    private void enableEditing(boolean enable) {
        etFirstName.setEnabled(enable);
        etLastName.setEnabled(enable);
        etPhoneNumber.setEnabled(enable);
        etHouseNo.setEnabled(enable);
        etBarangay.setEnabled(enable);
        
        etMunicipality.setEnabled(false);
        etProvince.setEnabled(false);
        
        layoutEditControls.setVisibility(enable ? View.VISIBLE : View.GONE);
        
        btnEditPersonal.setVisibility(enable ? View.INVISIBLE : View.VISIBLE);
        btnEditAddress.setVisibility(enable ? View.INVISIBLE : View.VISIBLE);
        
        updateAddAddressButtonVisibility();
    }

    private void updateAddAddressButtonVisibility() {
        if (!layoutEditControls.isShown() && hasPrimaryAddress && layoutAlternativeAddress.getVisibility() != View.VISIBLE) {
            btnAddAddress.setVisibility(View.VISIBLE);
        } else {
            btnAddAddress.setVisibility(View.GONE);
        }
    }

    private void deleteAlternativeAddress() {
        if (authToken == null || authToken.isEmpty()) return;

        apiInterface.deleteAddress("Bearer " + authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(User_Profile.this, "Alternative address deleted", Toast.LENGTH_SHORT).show();
                    hasAltAddress = false;
                    isEditingAlt = false;
                    layoutAlternativeAddress.setVisibility(View.GONE);
                    updateAddAddressButtonVisibility();
                    clearAltFields();
                    loadUserData();
                } else {
                    Toast.makeText(User_Profile.this, "Failed to delete: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(User_Profile.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateFields(boolean isAlt) {
        boolean isValid = true;
        
        if (!isAlt) {
            if (etFirstName.getText().toString().trim().isEmpty()) {
                etFirstName.setError("Required");
                isValid = false;
            }
            if (etLastName.getText().toString().trim().isEmpty()) {
                etLastName.setError("Required");
                isValid = false;
            }
            if (etPhoneNumber.getText().toString().trim().isEmpty()) {
                etPhoneNumber.setError("Required");
                isValid = false;
            }
            if (etHouseNo.getText().toString().trim().isEmpty()) {
                etHouseNo.setError("Required");
                isValid = false;
            }
            if (etBarangay.getText().toString().trim().isEmpty()) {
                etBarangay.setError("Required");
                isValid = false;
            }
        } else {
            String altStreet = etAltHouseNo.getText().toString().trim();
            String altBrgy = etAltBarangay.getText().toString().trim();
            
            if (altStreet.isEmpty()) {
                etAltHouseNo.setError("Required");
                isValid = false;
            }
            if (altBrgy.isEmpty()) {
                etAltBarangay.setError("Required");
                isValid = false;
            }

            if (!altStreet.isEmpty() && !altBrgy.isEmpty()) {
                String primaryStreet = etHouseNo.getText().toString().trim().toLowerCase();
                String primaryBrgy = etBarangay.getText().toString().trim().toLowerCase();
                String altKey = altStreet.toLowerCase() + altBrgy.toLowerCase();
                String primaryKey = primaryStreet + primaryBrgy;
                
                if (!primaryKey.isEmpty() && primaryKey.equals(altKey)) {
                    Toast.makeText(this, "Alternative address cannot be the same as primary", Toast.LENGTH_LONG).show();
                    isValid = false;
                }
            }
        }
        
        if (!isValid) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
        }
        
        return isValid;
    }

    private void saveProfileChanges(boolean isSavingAlt) {
        if (!validateFields(isSavingAlt)) return;

        if (authToken == null || authToken.isEmpty()) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObject updateData = new JsonObject();
        
        if (isSavingAlt) {
            JsonObject altAddr = new JsonObject();
            altAddr.addProperty("street", etAltHouseNo.getText().toString().trim());
            altAddr.addProperty("barangay", etAltBarangay.getText().toString().trim());
            altAddr.addProperty("municipality", CITY_VALUE);
            altAddr.addProperty("province", PROVINCE_VALUE);
            
            updateData.add("newAddress", altAddr);
        } else {
            updateData.addProperty("firstName", etFirstName.getText().toString().trim());
            updateData.addProperty("lastName", etLastName.getText().toString().trim());
            updateData.addProperty("phoneNumber", etPhoneNumber.getText().toString().trim());
            updateData.addProperty("street", etHouseNo.getText().toString().trim());
            updateData.addProperty("barangay", etBarangay.getText().toString().trim());
            updateData.addProperty("municipality", CITY_VALUE);
            updateData.addProperty("province", PROVINCE_VALUE);
        }

        Log.e(TAG, "Sending Targeted Payload: " + updateData.toString());

        apiInterface.updateProfile("Bearer " + authToken, updateData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Log.e(TAG, "Update Success! Response code: " + response.code());
                    if (response.body() != null) {
                        Log.e(TAG, "Response Body: " + response.body().toString());
                    }
                    Toast.makeText(User_Profile.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    
                    if (isSavingAlt) {
                        hasAltAddress = true;
                        enableAltFields(false); 
                    } else {
                        hasPrimaryAddress = true;
                        enableEditing(false);
                    }
                    loadUserData();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e(TAG, "Update failed. Error Response: " + response.code() + " - " + errorBody);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(User_Profile.this, "Update failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network Failure: " + t.getMessage());
                Toast.makeText(User_Profile.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    private void setupNavigation() {
        if (findViewById(R.id.navHome) != null) findViewById(R.id.navHome).setOnClickListener(v -> navigateTo(User_Dashboard.class));
        if (findViewById(R.id.navMenu) != null) findViewById(R.id.navMenu).setOnClickListener(v -> navigateTo(Menu.class));
        if (findViewById(R.id.navDiscover) != null) findViewById(R.id.navDiscover).setOnClickListener(v -> navigateTo(Discover.class));
        if (findViewById(R.id.navOrders) != null) findViewById(R.id.navOrders).setOnClickListener(v -> navigateTo(Orders.class));
        if (findViewById(R.id.navProfile) != null) findViewById(R.id.navProfile).setOnClickListener(v -> { /* Already here */ });
    }

    private void navigateTo(Class<?> targetClass) {
        if (this.getClass().equals(targetClass)) return;
        Intent intent = new Intent(this, targetClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void loadUserData() {
        if (authToken == null || authToken.isEmpty()) return;

        apiInterface.getProfile("Bearer " + authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject user = response.body();
                    Log.e(TAG, "Loaded User Data: " + user.toString());
                    
                    String fName = user.has("firstName") ? user.get("firstName").getAsString() : "";
                    String lName = user.has("lastName") ? user.get("lastName").getAsString() : "";
                    String phone = user.has("phoneNumber") ? user.get("phoneNumber").getAsString() : "";
                    String email = user.has("email") ? user.get("email").getAsString() : "";
                    
                    // Matching backend key "profilePicture"
                    String profilePicUrl = "";
                    if (user.has("profilePicture")) {
                        profilePicUrl = user.get("profilePicture").getAsString();
                    } else if (user.has("profilePic")) {
                        profilePicUrl = user.get("profilePic").getAsString();
                    }
                    currentProfilePicUrl = profilePicUrl;

                    tvProfileName.setText(fName + " " + lName);
                    tvProfileEmail.setText(email);
                    etFirstName.setText(fName);
                    etLastName.setText(lName);
                    etPhoneNumber.setText(phone);

                    if (!profilePicUrl.isEmpty()) {
                        Glide.with(User_Profile.this)
                                .load(profilePicUrl)
                                .placeholder(R.drawable.istockphoto_1225790722_612x612_removebg_preview)
                                .circleCrop()
                                .into(ivProfilePic);
                    } else {
                        ivProfilePic.setImageResource(R.drawable.istockphoto_1225790722_612x612_removebg_preview);
                    }

                    boolean serverHasPrimary = false;
                    boolean serverHasAlt = false;

                    if (user.has("addresses") && user.get("addresses").isJsonArray()) {
                        JsonArray addrArray = user.getAsJsonArray("addresses");

                        for (int i = 0; i < addrArray.size(); i++) {
                            JsonObject addr = addrArray.get(i).getAsJsonObject();
                            boolean isDefault = addr.has("isDefault") && addr.get("isDefault").getAsBoolean();

                            if (isDefault) {
                                serverHasPrimary = true;
                                etHouseNo.setText(addr.has("street") ? addr.get("street").getAsString() : "");
                                etBarangay.setText(addr.has("barangay") ? addr.get("barangay").getAsString() : "", false);
                            } else {
                                serverHasAlt = true;
                                etAltHouseNo.setText(addr.has("street") ? addr.get("street").getAsString() : "");
                                etAltBarangay.setText(addr.has("barangay") ? addr.get("barangay").getAsString() : "", false);
                            }
                        }
                    }

                    hasPrimaryAddress = serverHasPrimary;
                    boolean previousHasAlt = hasAltAddress;
                    hasAltAddress = serverHasAlt;

                    if (!serverHasPrimary) {
                        enableEditing(true);
                        btnAddAddress.setVisibility(View.GONE);
                        layoutAlternativeAddress.setVisibility(View.GONE);
                    } else {
                        if (serverHasAlt) {
                            layoutAlternativeAddress.setVisibility(View.VISIBLE);
                            btnAddAddress.setVisibility(View.GONE);
                            if (!isEditingAlt) enableAltFields(false);
                        } else {
                            if (isEditingAlt || previousHasAlt) {
                                layoutAlternativeAddress.setVisibility(View.VISIBLE);
                                if (!isEditingAlt) enableAltFields(false);
                            } else {
                                layoutAlternativeAddress.setVisibility(View.GONE);
                                updateAddAddressButtonVisibility();
                            }
                        }
                        
                        if (!layoutEditControls.isShown()) {
                            enableEditing(false);
                        }
                    }
                } else {
                    Log.e(TAG, "Load user data failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "Error Loading Profile: " + t.getMessage());
            }
        });
    }
}
