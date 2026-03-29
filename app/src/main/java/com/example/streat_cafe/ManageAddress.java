package com.example.streat_cafe;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ManageAddress extends AppCompatActivity {

    private LinearLayout addressListSection, addressFormSection, addressList;
    private EditText etLabel, etProvince, etMunicipality, etDetails;
    private Button btnAddNewAddress, btnSaveAddress, btnCancel;
    private DatabaseHelper db;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_address);

        db = new DatabaseHelper(this);
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        userEmail = sharedPreferences.getString("userEmail", "");

        addressListSection = findViewById(R.id.addressListSection);
        addressFormSection = findViewById(R.id.addressFormSection);
        addressList = findViewById(R.id.addressList);
        
        etLabel = findViewById(R.id.etLabel);
        etProvince = findViewById(R.id.etProvince);
        etMunicipality = findViewById(R.id.etMunicipality);
        etDetails = findViewById(R.id.etDetails);
        
        btnAddNewAddress = findViewById(R.id.btnAddNewAddress);
        btnSaveAddress = findViewById(R.id.btnSaveAddress);
        btnCancel = findViewById(R.id.btnCancel);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadAddresses();

        btnAddNewAddress.setOnClickListener(v -> {
            addressListSection.setVisibility(View.GONE);
            addressFormSection.setVisibility(View.VISIBLE);
        });

        btnCancel.setOnClickListener(v -> {
            addressFormSection.setVisibility(View.GONE);
            addressListSection.setVisibility(View.VISIBLE);
            clearForm();
        });

        btnSaveAddress.setOnClickListener(v -> saveAddress());
    }

    private void loadAddresses() {
        addressList.removeAllViews();
        Cursor cursor = db.getAddresses(userEmail);
        
        LayoutInflater inflater = LayoutInflater.from(this);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                View itemView = inflater.inflate(R.layout.item_address, addressList, false);
                
                TextView tvLabel = itemView.findViewById(R.id.tvAddressLabel);
                TextView tvDetails = itemView.findViewById(R.id.tvFullAddress);

                String label = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ADDR_LABEL));
                String province = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ADDR_PROVINCE));
                String municipality = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ADDR_MUNICIPALITY));
                String details = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ADDR_DETAILS));

                tvLabel.setText(label);
                tvDetails.setText(municipality + ", " + province + " (" + details + ")");

                addressList.addView(itemView);
            }
            cursor.close();
        }
    }

    private void saveAddress() {
        String label = etLabel.getText().toString().trim();
        String province = etProvince.getText().toString().trim();
        String municipality = etMunicipality.getText().toString().trim();
        String details = etDetails.getText().toString().trim();

        if (label.isEmpty() || province.isEmpty() || municipality.isEmpty() || details.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isInserted = db.insertAddress(userEmail, label, province, municipality, details);
        if (isInserted) {
            Toast.makeText(this, "Address saved!", Toast.LENGTH_SHORT).show();
            addressFormSection.setVisibility(View.GONE);
            addressListSection.setVisibility(View.VISIBLE);
            clearForm();
            loadAddresses();
        } else {
            Toast.makeText(this, "Failed to save address", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearForm() {
        etLabel.setText("");
        etProvince.setText("");
        etMunicipality.setText("");
        etDetails.setText("");
    }
}
