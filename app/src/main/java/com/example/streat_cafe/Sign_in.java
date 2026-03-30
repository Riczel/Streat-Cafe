package com.example.streat_cafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Sign_in extends AppCompatActivity {

    private TextInputEditText emailField, passwordField;
    private Button loginButton, googleSignInButton;
    private TextView dontHaveAccount;
    private ApiInterface apiInterface;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        apiInterface = RetrofitClient.getClient().create(ApiInterface.class);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(getString(R.string.google_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailField = findViewById(R.id.etEmail);
        passwordField = findViewById(R.id.etPassword);
        loginButton = findViewById(R.id.btnLogin);
        googleSignInButton = findViewById(R.id.btnGoogleSignIn);
        dontHaveAccount = findViewById(R.id.tvDontHaveAccount);

        loginButton.setOnClickListener(v -> performLogin());
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        if (dontHaveAccount != null) {
            dontHaveAccount.setOnClickListener(v -> startActivity(new Intent(Sign_in.this, Sign_up.class)));
        }
    }

    private void signInWithGoogle() {
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
        body.addProperty("type", "login");

        googleSignInButton.setEnabled(false);
        apiInterface.loginWithGoogle(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                googleSignInButton.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("SignIn", "Response from Backend: " + response.body().toString());
                    handleSuccessResponse(response.body());
                } else {
                    Toast.makeText(Sign_in.this, "Server Authentication Failed", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                googleSignInButton.setEnabled(true);
                Toast.makeText(Sign_in.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSuccessResponse(JsonObject body) {
        boolean isNew = false; 
        
        if (body.has("isNewUser")) {
            isNew = body.get("isNewUser").getAsBoolean();

        }

        if (isNew) {
            Toast.makeText(Sign_in.this, "Email not registered. Please sign up first.", Toast.LENGTH_LONG).show();
            mGoogleSignInClient.signOut();
            return;
        }

        String token = body.has("token") ? body.get("token").getAsString() : "";
        String email = body.has("user") ? body.getAsJsonObject("user").get("email").getAsString() : "";
        String role = body.has("role") ? body.get("role").getAsString() : "user";

        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        sharedPreferences.edit()
                .putString("userEmail", email)
                .putString("authToken", token)
                .putString("userRole", role)
                .apply();

        Toast.makeText(Sign_in.this, "Login Successful!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(Sign_in.this, User_Dashboard.class));
        finishAffinity();
    }

    private void performLogin() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (emailField != null) emailField.setError("Valid email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            if (passwordField != null) passwordField.setError("Password is required");
            return;
        }

        JsonObject credentials = new JsonObject();
        credentials.addProperty("email", email);
        credentials.addProperty("password", password);

        loginButton.setEnabled(false);
        apiInterface.loginUser(credentials).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                loginButton.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    handleSuccessResponse(response.body());
                } else {
                    Toast.makeText(Sign_in.this, "Login failed", Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                loginButton.setEnabled(true);
                Toast.makeText(Sign_in.this, "Network Error", Toast.LENGTH_LONG).show();
            }
        });
    }
}
