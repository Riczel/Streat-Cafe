package com.example.streat_cafe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
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
import androidx.annotation.NonNull;
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
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

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

        // Apply Input Filters for Validation
        applyInputFilters();

        setupClickListeners();
        setupPickImageLauncher();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupBarangayDropdown();
        setupNavigation();
        
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadUserData);
        }
    }

    private void applyInputFilters() {
        // Filter for Name fields: Only allow letters and spaces
        InputFilter nameFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char charAt = source.charAt(i);
                if (!Character.isLetter(charAt) && !Character.isSpaceChar(charAt)) {
                    return "";
                }
            }
            return null;
        };

        etFirstName.setFilters(new InputFilter[]{nameFilter});
        etLastName.setFilters(new InputFilter[]{nameFilter});

        // Filter for Phone Number: Numeric only, limit to 11 digits
        etPhoneNumber.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11)});
        etPhoneNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    private void setupBarangayDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, barangays);
        etBarangay.setAdapter(adapter);
        etAltBarangay.setAdapter(adapter);
        
        etBarangay.setOnClickListener(v -> {
            if (etBarangay.isEnabled()) etBarangay.showDropDown();
        });
        etAltBarangay.setOnClickListener(v -> {
            if (etAltBarangay.isEnabled()) etAltBarangay.showDropDown();
        });
    }

    private void setupClickListeners() {
        btnEditPersonal.setOnClickListener(v -> enableEditing(true));
        btnEditAddress.setOnClickListener(v -> enableEditing(true));
        btnCancel.setOnClickListener(v -> {
            enableEditing(false);
            loadUserData();
        });
        btnSave.setOnClickListener(v -> saveProfileChanges(false));

        btnAddAddress.setOnClickListener(v -> {
            layoutAlternativeAddress.setVisibility(View.VISIBLE);
            btnAddAddress.setVisibility(View.GONE);
            clearAltFields();
            enableAltFields(true);
        });

        btnEditAlt.setOnClickListener(v -> enableAltFields(true));
        btnDeleteAlt.setOnClickListener(v -> deleteAlternativeAddress());
        btnCancelAlt.setOnClickListener(v -> {
            enableAltFields(false);
            if (!hasAltAddress) layoutAlternativeAddress.setVisibility(View.GONE);
            loadUserData();
        });
        btnSaveAlt.setOnClickListener(v -> saveProfileChanges(true));

        btnSignOut.setOnClickListener(v -> signOut());
        btnMyCart.setOnClickListener(v -> navigateTo(Cart.class));
        btnRecentOrders.setOnClickListener(v -> navigateTo(RecentOrders.class));

        btnChangePic.setOnClickListener(v -> {
            if (layoutPicOptions.getVisibility() == View.VISIBLE) {
                layoutPicOptions.setVisibility(View.GONE);
            } else {
                layoutPicOptions.setVisibility(View.VISIBLE);
            }
        });

        btnPicChange.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
            layoutPicOptions.setVisibility(View.GONE);
        });

        btnPicRemove.setOnClickListener(v -> {
            removeProfilePicture();
            layoutPicOptions.setVisibility(View.GONE);
        });
    }

    private void setupPickImageLauncher() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        if (selectedImage != null) {
                            uploadProfilePicture(selectedImage);
                        }
                    }
                }
        );
    }

    private void uploadProfilePicture(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.DEFAULT);

            JsonObject payload = new JsonObject();
            payload.addProperty("profilePicture", base64Image);

            if (authToken == null || authToken.isEmpty()) return;

            apiInterface.updateProfile("Bearer " + authToken, payload).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(User_Profile.this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                        loadUserData();
                    } else {
                        Toast.makeText(User_Profile.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Toast.makeText(User_Profile.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeProfilePicture() {
        JsonObject payload = new JsonObject();
        payload.addProperty("profilePicture", "");

        if (authToken == null || authToken.isEmpty()) return;

        apiInterface.updateProfile("Bearer " + authToken, payload).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(User_Profile.this, "Profile picture removed", Toast.LENGTH_SHORT).show();
                    loadUserData();
                } else {
                    Toast.makeText(User_Profile.this, "Failed to remove image", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(User_Profile.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void signOut() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent intent = new Intent(User_Profile.this, Sign_in.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
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
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String phone = etPhoneNumber.getText().toString().trim();

            if (firstName.isEmpty()) {
                etFirstName.setError("Required");
                isValid = false;
            } else if (!firstName.matches("[a-zA-Z\\s]+")) {
                etFirstName.setError("Only letters allowed");
                isValid = false;
            }

            if (lastName.isEmpty()) {
                etLastName.setError("Required");
                isValid = false;
            } else if (!lastName.matches("[a-zA-Z\\s]+")) {
                etLastName.setError("Only letters allowed");
                isValid = false;
            }

            if (phone.isEmpty()) {
                etPhoneNumber.setError("Required");
                isValid = false;
            } else if (phone.length() != 11) {
                etPhoneNumber.setError("Must be 11 digits");
                isValid = false;
            } else if (!phone.startsWith("09")) {
                etPhoneNumber.setError("Must start with 09");
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
            Toast.makeText(this, "Please fix the errors in the form", Toast.LENGTH_SHORT).show();
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
                        
                        // Handle backend error messages using version-safe parsing
                        if (errorBody.contains("message")) {
                            JsonObject errorJson = new JsonParser().parse(errorBody).getAsJsonObject();
                            String message = errorJson.get("message").getAsString();
                            Toast.makeText(User_Profile.this, "Error: " + message, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(User_Profile.this, "Update failed: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(User_Profile.this, "Update failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
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

                    String profilePicUrl = "";
                    if (user.has("profilePicture")) {
                        profilePicUrl = user.get("profilePicture").getAsString();
                    } else if (user.has("profilePic")) {
                        profilePicUrl = user.get("profilePic").getAsString();
                    }
                    currentProfilePicUrl = profilePicUrl;

                    tvProfileName.setText(String.format("%s %s", fName, lName));
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