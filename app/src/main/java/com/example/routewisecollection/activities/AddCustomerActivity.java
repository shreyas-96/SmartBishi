package com.example.routewisecollection.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.example.routewisecollection.R;
import com.example.routewisecollection.models.Customer;
import com.example.routewisecollection.models.Route;
import com.example.routewisecollection.utils.LoginManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddCustomerActivity extends AppCompatActivity {

    private EditText etName, etPhone, etAccountNumber;
    private Spinner spinnerRoute, spinnerVillage;
    private Button btnSave, btnCancel;
    private ProgressBar progressBar;
    private TextView tvHeaderTitle, tvModeIndicator;

    private List<Route> routeList = new ArrayList<>();
    private List<String> villageList = new ArrayList<>();
    private DatabaseReference customersRef, routesRef;
    private String agentId;

    // Edit mode variables
    private boolean isEditMode = false;
    private String originalPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_customer);

        // Initialize views
        etName = findViewById(R.id.etCustomerName);
        etPhone = findViewById(R.id.etCustomerPhone);
        etAccountNumber = findViewById(R.id.etAccountNumber);
        spinnerRoute = findViewById(R.id.spinnerRoute);
        spinnerVillage = findViewById(R.id.spinnerVillage);
        btnSave = findViewById(R.id.btnSaveCustomer);
        btnCancel = findViewById(R.id.btnCancelCustomer);
        progressBar = findViewById(R.id.progressBar);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvModeIndicator = findViewById(R.id.tvModeIndicator);

        // Get current agent
        LoginManager loginManager = new LoginManager(this);
        agentId = loginManager.getAgentMobile();

        customersRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentId)
                .child("customers");

        routesRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentId)
                .child("agentInfo")
                .child("routes");

        loadRoutesFromFirebase();
        setupRouteSelectionListener();
        checkEditMode();
        setupClickListeners();
    }

    private void loadRoutesFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);

        routesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                routeList.clear();
                List<String> routeNames = new ArrayList<>();

                for (DataSnapshot routeSnapshot : snapshot.getChildren()) {
                    String routeKey = routeSnapshot.getKey();
                    String name = routeSnapshot.child("name").getValue(String.class);
                    String status = routeSnapshot.child("status").getValue(String.class);

                    boolean isActive = (status == null) || "active".equalsIgnoreCase(status);

                    if (name != null && isActive) {
                        Route route = new Route();
                        route.setRouteName(name);
                        route.setFirebaseKey(routeKey);

                        // Load villages for this route
                        List<String> villages = new ArrayList<>();
                        DataSnapshot villagesSnapshot = routeSnapshot.child("villages");
                        for (DataSnapshot villageSnapshot : villagesSnapshot.getChildren()) {
                            String village = villageSnapshot.getValue(String.class);
                            if (village != null) {
                                villages.add(village);
                            }
                        }
                        route.setVillages(villages);

                        routeList.add(route);
                        routeNames.add(name);
                    }
                }

                // Set up route spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AddCustomerActivity.this,
                        android.R.layout.simple_spinner_item, routeNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerRoute.setAdapter(adapter);

                progressBar.setVisibility(View.GONE);

                if (routeList.isEmpty()) {
                    Toast.makeText(AddCustomerActivity.this,
                            "No routes found for your login", Toast.LENGTH_SHORT).show();
                }

                // Load villages for first route if available
                if (!routeList.isEmpty()) {
                    loadVillagesForRoute(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddCustomerActivity.this,
                        "Error loading routes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRouteSelectionListener() {
        spinnerRoute.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadVillagesForRoute(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void loadVillagesForRoute(int routePosition) {
        if (routePosition >= 0 && routePosition < routeList.size()) {
            Route selectedRoute = routeList.get(routePosition);
            villageList.clear();

            if (selectedRoute.getVillages() != null && !selectedRoute.getVillages().isEmpty()) {
                villageList.addAll(selectedRoute.getVillages());
            } else {
                villageList.add("No villages available");
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, villageList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerVillage.setAdapter(adapter);
        }
    }

    private void checkEditMode() {
        // Check if we're in edit mode
        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);

        if (isEditMode) {
            // Update UI for edit mode
            tvHeaderTitle.setText("Edit Customer");
            tvModeIndicator.setText("Editing Mode");
            tvModeIndicator.setVisibility(View.VISIBLE);
            btnSave.setText("💾 Update Customer");

            // Get customer data from intent
            originalPhoneNumber = getIntent().getStringExtra("CUSTOMER_PHONE");
            String name = getIntent().getStringExtra("CUSTOMER_NAME");
            String village = getIntent().getStringExtra("CUSTOMER_VILLAGE");
            String routeName = getIntent().getStringExtra("CUSTOMER_ROUTE_NAME");
            String accountNumber = getIntent().getStringExtra("CUSTOMER_ACCOUNT");

            // Pre-fill the form
            etName.setText(name);
            etPhone.setText(originalPhoneNumber);
            etAccountNumber.setText(accountNumber);

            // Set the spinner to the correct route and village
            if (routeName != null && !routeName.isEmpty()) {
                for (int i = 0; i < routeList.size(); i++) {
                    if (routeList.get(i).getRouteName().equalsIgnoreCase(routeName)) {
                        spinnerRoute.setSelection(i);

                        // Now set village after route is selected
                        if (village != null && !village.isEmpty()) {
                            List<String> villages = routeList.get(i).getVillages();
                            if (villages != null) {
                                for (int j = 0; j < villages.size(); j++) {
                                    if (villages.get(j).equalsIgnoreCase(village)) {
                                        spinnerVillage.setSelection(j);
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }

            // Disable phone number editing in edit mode
            etPhone.setEnabled(false);
            etPhone.setAlpha(0.6f);
        } else {
            tvHeaderTitle.setText("Add New Customer");
            tvModeIndicator.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveCustomer());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveCustomer() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String accountNumber = etAccountNumber.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (routeList.isEmpty()) {
            Toast.makeText(this, "No routes available. Please add routes first.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected route and village
        int routePosition = spinnerRoute.getSelectedItemPosition();
        Route selectedRoute = routeList.get(routePosition);
        String routeName = selectedRoute.getRouteName();

        int villagePosition = spinnerVillage.getSelectedItemPosition();
        String village = villageList.get(villagePosition);

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        if (isEditMode) {
            // Update existing customer
            updateCustomer(name, phone, village, routeName, accountNumber);
        } else {
            // Add new customer
            addNewCustomer(name, phone, village, routeName, accountNumber);
        }
    }

    private void addNewCustomer(String name, String phone, String village, String routeName, String accountNumber) {
        double principal = 0.0; // Default principal amount
        double interestRate = 0.0; // No default interest rate
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        int routeId = 1; // Default route ID

        Customer customer = new Customer(name, phone, village, routeId, principal, interestRate, date, accountNumber, agentId);
        customer.setVillage(village);
        customer.setRouteName(routeName);
        customer.setCreatedDate(date);
        customer.setCreatedTime(time);

        // Save directly to Firebase using phone number as key
        customersRef.child(phone).setValue(customer)
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Customer added! Route: " + routeName + ", Village: " + village, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateCustomer(String name, String phone, String village, String routeName, String accountNumber) {
        // Get the existing customer data first to preserve other fields
        customersRef.child(originalPhoneNumber).get().addOnSuccessListener(snapshot -> {
            Customer customer = snapshot.getValue(Customer.class);
            if (customer != null) {
                // Update only the editable fields
                customer.setName(name);
                customer.setVillage(village);
                customer.setRouteName(routeName);
                customer.setAccountNumber(accountNumber);

                // Set updated date and time
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                customer.setUpdatedDate(date);
                customer.setUpdatedTime(time);

                // Save updated customer back to Firebase
                customersRef.child(originalPhoneNumber).setValue(customer)
                        .addOnSuccessListener(unused -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Customer updated! Route: " + routeName + ", Village: " + village, Toast.LENGTH_LONG).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            Toast.makeText(this, "Error updating: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(this, "Customer not found!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            Toast.makeText(this, "Error fetching customer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
