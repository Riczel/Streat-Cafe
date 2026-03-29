package com.example.streat_cafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Sign_up extends AppCompatActivity {

    private EditText emailField, firstNameField, lastNameField, phoneField, passwordField, confirmPasswordField;
    private Button signUpButton, googleSignUpButton;
    private TextView alreadyHaveAccount;
    private ApiInterface apiInterface;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        apiInterface = RetrofitClient.getClient().create(ApiInterface.class);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(getString(R.string.google_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        emailField = findViewById(R.id.editTextTextEmailAddress);
        firstNameField = findViewById(R.id.editTextText);
        lastNameField = findViewById(R.id.editTextText2);
        phoneField = findViewById(R.id.editTextPhone);
        passwordField = findViewById(R.id.editTextTextPassword);
        confirmPasswordField = findViewById(R.id.editTextTextPassword2);
        signUpButton = findViewById(R.id.button3);
        googleSignUpButton = findViewById(R.id.btnGoogleSignUp);
        alreadyHaveAccount = findViewById(R.id.tvAlreadyHaveAccount);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        signUpButton.setOnClickListener(v -> validateAndSignUp());
        googleSignUpButton.setOnClickListener(v -> signUpWithGoogle());
        alreadyHaveAccount.setOnClickListener(v -> {
            startActivity(new Intent(Sign_up.this, Sign_in.class));
            finish(); 
        });
    }

    private void signUpWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String authCode = account.getServerAuthCode();
            if (authCode != null) {
                sendCodeToBackend(authCode);
            }
        } catch (ApiException e) {
            Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendCodeToBackend(String code) {
        JsonObject body = new JsonObject();
        body.addProperty("code", code);

        googleSignUpButton.setEnabled(false);
        apiInterface.loginWithGoogle(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                googleSignUpButton.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    handleSuccessResponse(response.body(), response.code());
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                googleSignUpButton.setEnabled(true);
                Toast.makeText(Sign_up.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSuccessResponse(JsonObject body, int statusCode) {
        // Improved detection: Check isNewUser, newUser, response code, and message
        boolean isNew = false;
        if (body.has("isNewUser")) {
            isNew = body.get("isNewUser").getAsBoolean();
        } else if (body.has("newUser")) {
            isNew = body.get("newUser").getAsBoolean();
        } else if (statusCode == 201) {
            isNew = true;
        } else if (body.has("message")) {
            String msg = body.get("message").getAsString().toLowerCase();
            if (msg.contains("created") || msg.contains("registered") || msg.contains("sign up")) {
                isNew = true;
            } else if (msg.contains("login") || msg.contains("welcome back")) {
                isNew = false;
            } else {
                // Default to true for sign-up flow if ambiguous
                isNew = true; 
            }
        } else {
            // Default to true if no other indicators found
            isNew = true;
        }
        
        if (!isNew) {
            Toast.makeText(this, "This account already exists. Please sign in instead.", Toast.LENGTH_LONG).show();
            mGoogleSignInClient.signOut();
            return;
        }

        // Proceed only for new accounts
        String token = body.has("token") ? body.get("token").getAsString() : "";
        String email = body.has("user") ? body.getAsJsonObject("user").get("email").getAsString() : "";
        
        saveSession(email, token, body.has("role") ? body.get("role").getAsString() : "user");

        Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, User_Dashboard.class));
        finishAffinity();
    }

    private void handleErrorResponse(Response<JsonObject> response) {
        String errorMsg = "Process Failed";
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                JsonObject errorObj = new JsonParser().parse(errorBody.string()).getAsJsonObject();
                if (errorObj.has("message")) errorMsg = errorObj.get("message").getAsString();
            }
        } catch (Exception ignored) {}

        if (errorMsg.toLowerCase().contains("already exist") || response.code() == 409) {
            Toast.makeText(this, "This email is already registered. Please sign in.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        }
    }

    private void validateAndSignUp() {
        String email = emailField.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Valid email required");
            return;
        }
        
        // Manual form submission
        JsonObject userDetails = new JsonObject();
        userDetails.addProperty("email", email);
        userDetails.addProperty("firstName", firstNameField.getText().toString().trim());
        userDetails.addProperty("lastName", lastNameField.getText().toString().trim());
        userDetails.addProperty("phoneNumber", phoneField.getText().toString().trim());
        userDetails.addProperty("password", passwordField.getText().toString().trim());

        apiInterface.registerUser(userDetails).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(Sign_up.this, "Account Created! Please Sign In.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Sign_up.this, Sign_in.class));
                    finish();
                } else {
                    handleErrorResponse(response);
                }
            }
            @Override public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Toast.makeText(Sign_up.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSession(String email, String token, String role) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        sharedPreferences.edit()
                .putString("userEmail", email)
                .putString("authToken", token)
                .putString("userRole", role)
                .apply();
    }
}
