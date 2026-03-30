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
        body.addProperty("type", "register");

        googleSignUpButton.setEnabled(false);
        apiInterface.loginWithGoogle(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                googleSignUpButton.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("SignUp", "Response: " + response.body().toString());
                    handleSuccessResponse(response.body());
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

    private void handleSuccessResponse(JsonObject body) {
        boolean isNew = true; 
        
        if (body.has("isNewUser")) {
            isNew = body.get("isNewUser").getAsBoolean();
        } else if (body.has("newUser")) {
            isNew = body.get("newUser").getAsBoolean();
        } else if (body.has("isNew")) {
            isNew = body.get("isNew").getAsBoolean();
        }

        if (!isNew) {
            Toast.makeText(this, "This account already exists. Please sign in instead.", Toast.LENGTH_LONG).show();
            mGoogleSignInClient.signOut();
            return;
        }

        Toast.makeText(this, "Account Created Successfully! Please Sign In.", Toast.LENGTH_SHORT).show();
        mGoogleSignInClient.signOut();
        startActivity(new Intent(this, Sign_in.class));
        finish();
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
        String firstName = firstNameField.getText().toString().trim();
        String lastName = lastNameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Valid email required");
            return;
        }

        if (TextUtils.isEmpty(firstName)) {
            firstNameField.setError("First name is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Password is required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordField.setError("Passwords do not match");
            return;
        }
        
        JsonObject userDetails = new JsonObject();
        userDetails.addProperty("email", email);
        userDetails.addProperty("firstName", firstName);
        userDetails.addProperty("lastName", lastName);
        userDetails.addProperty("phoneNumber", phoneField.getText().toString().trim());
        userDetails.addProperty("password", password);

        signUpButton.setEnabled(false);
        apiInterface.registerUser(userDetails).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                signUpButton.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(Sign_up.this, "Account Created! Please Sign In.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Sign_up.this, Sign_in.class));
                    finish();
                } else {
                    handleErrorResponse(response);
                }
            }
            @Override public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                signUpButton.setEnabled(true);
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
