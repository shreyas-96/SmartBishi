package com.example.routewisecollection.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.routewisecollection.R;
import com.example.routewisecollection.adapters.CustomerReportAdapter;
import com.example.routewisecollection.models.CustomerReportModel;
import com.example.routewisecollection.utils.Constants;
import com.example.routewisecollection.utils.LoginManager;
import com.example.routewisecollection.utils.PDFGenerator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerReportActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private EditText etSearchCustomer;
    private TextView tvTotalCustomers, tvNoCustomers, tvTotalAmount;
    private RecyclerView rvCustomerReports;
    private CardView btnGeneratePDF;
    private ProgressBar progressBar;

    private List<CustomerReportModel> customerReports = new ArrayList<>();
    private List<CustomerReportModel> filteredReports = new ArrayList<>();

    private CustomerReportAdapter adapter;

    private String agentId;
    private DatabaseReference customersRef, transactionsRef;
    private double totalAmount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_report);
        
        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Customer Report");
        }

        initializeViews();
        setupFirebaseReferences();
        setupRecyclerView();
        setupSearchFilter();
        setupClickListeners();
        checkPermissions();

        loadData();
    }

    private void initializeViews() {
        etSearchCustomer = findViewById(R.id.etSearchCustomer);
        tvTotalCustomers = findViewById(R.id.tvTotalCustomers);
        tvNoCustomers = findViewById(R.id.tvNoCustomers);
        rvCustomerReports = findViewById(R.id.rvCustomerReports);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupFirebaseReferences() {
        // Get agent ID from LoginManager
        LoginManager loginManager = new LoginManager(this);
        agentId = loginManager.getAgentMobile();

        customersRef = FirebaseDatabase.getInstance()
                .getReference("agents").child(agentId).child("customers");
        transactionsRef = FirebaseDatabase.getInstance()
                .getReference("agents").child(agentId).child("transactions");
    }

    private void setupRecyclerView() {
        adapter = new CustomerReportAdapter(filteredReports, this);
        rvCustomerReports.setLayoutManager(new LinearLayoutManager(this));
        rvCustomerReports.setAdapter(adapter);
    }

    private void setupSearchFilter() {
        etSearchCustomer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterReports(s.toString());
            }
        });
    }

    private void setupClickListeners() {
        btnGeneratePDF.setOnClickListener(v -> generatePDF());
    }

    private void loadData() {
        // Load customers then transactions to build report list
        customersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot customerSnap) {
                Map<String, CustomerReportModel> customerMap = new HashMap<>();

                // First, create customer objects
                for (DataSnapshot cSnap : customerSnap.getChildren()) {
                    String name = cSnap.child("name").getValue(String.class);
                    String phone = cSnap.getKey(); // Phone is key
                    String address = cSnap.child("address").getValue(String.class);
                    String accountNumber = cSnap.child("accountNumber").getValue(String.class);

                    CustomerReportModel crm = new CustomerReportModel(
                            name, phone, address, 0, "", 0.0);
                    crm.setAccountNumber(accountNumber != null ? accountNumber : "N/A");
                    customerMap.put(phone, crm);
                }

                // Then load transactions and calculate balances
                loadTransactionsForCustomers(customerMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerReportActivity.this, 
                    "Failed to load customers: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadTransactionsForCustomers(Map<String, CustomerReportModel> customerMap) {
        transactionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot transactionSnap) {
                // Process all transactions for each customer
                for (DataSnapshot custTransSnap : transactionSnap.getChildren()) {
                    String customerId = custTransSnap.getKey(); // phone number
                    CustomerReportModel crm = customerMap.get(customerId);
                    
                    if (crm != null) {
                        processCustomerTransactions(crm, custTransSnap);
                    }
                }

                // Update UI with processed data
                customerReports = new ArrayList<>(customerMap.values());
                filteredReports.clear();
                filteredReports.addAll(customerReports);
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerReportActivity.this, 
                    "Failed to load transactions: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processCustomerTransactions(CustomerReportModel crm, DataSnapshot custTransSnap) {
        List<Date> dates = new ArrayList<>();
        int totalEntries = 0;
        double totalDeposits = 0.0;
        double totalWithdrawals = 0.0;

        for (DataSnapshot transSnap : custTransSnap.getChildren()) {
            String type = transSnap.child("type").getValue(String.class);
            Double amount = transSnap.child("amount").getValue(Double.class);
            String dateStr = transSnap.child("date").getValue(String.class);

            if (amount != null && type != null) {
                totalEntries++;
                
                if ("deposit".equals(type)) {
                    totalDeposits += amount;
                } else if ("withdrawal".equals(type)) {
                    totalWithdrawals += amount;
                }
            }

            // Collect dates for date range
            if (dateStr != null) {
                try {
                    dates.add(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr));
                } catch (ParseException ignored) {
                    // Ignore parsing errors
                }
            }
        }

        // Set calculated values
        crm.setTotalEntries(totalEntries);
        crm.setTotalAmount(totalDeposits - totalWithdrawals); // Net balance

        // Set date range
        if (!dates.isEmpty()) {
            Collections.sort(dates);
            SimpleDateFormat sdfOut = new SimpleDateFormat("dd MMM", Locale.getDefault());
            String dateRange = sdfOut.format(dates.get(0)) + " - " + 
                              sdfOut.format(dates.get(dates.size() - 1));
            crm.setDateRange(dateRange);
        } else {
            crm.setDateRange("No transactions");
        }
    }

    private void filterReports(String query) {
        String lowerQuery = query.toLowerCase();
        filteredReports.clear();

        if (query.isEmpty()) {
            filteredReports.addAll(customerReports);
        } else {
            for (CustomerReportModel cr : customerReports) {
                if ((cr.getName() != null && cr.getName().toLowerCase().contains(lowerQuery)) ||
                    (cr.getPhoneNumber() != null && cr.getPhoneNumber().contains(query))) {
                    filteredReports.add(cr);
                }
            }
        }
        updateUI();
    }

    private void updateUI() {
        tvTotalCustomers.setText(String.valueOf(filteredReports.size()));

        // Calculate and display total amount
        totalAmount = 0.0;
        for (CustomerReportModel customer : filteredReports) {
            totalAmount += customer.getTotalAmount();
        }
        tvTotalAmount.setText(String.format(Locale.getDefault(), "₹%.2f", totalAmount));

        if (filteredReports.isEmpty()) {
            rvCustomerReports.setVisibility(View.GONE);
            tvNoCustomers.setVisibility(View.VISIBLE);
        } else {
            rvCustomerReports.setVisibility(View.VISIBLE);
            tvNoCustomers.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    private void generatePDF() {
        if (!checkPermissions()) {
            return;
        }

        if (filteredReports.isEmpty()) {
            Toast.makeText(this, "No customer data available to generate PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress();
        
        new android.os.Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            String companyName = prefs.getString(Constants.PREF_COMPANY_NAME, Constants.DEFAULT_COMPANY_NAME);
            
            // Get agent name and mobile from LoginManager (backend/database)
            LoginManager loginManager = new LoginManager(this);
            String agentName = loginManager.getAgentName();
            String agentMobile = loginManager.getAgentMobile();
            
            String pdfPath = PDFGenerator.generateCustomerReportPDF(
                this, filteredReports, totalAmount, agentName, agentMobile, companyName
            );
            
            hideProgress();
            
            if (pdfPath != null) {
                Toast.makeText(this, "PDF generated successfully!", Toast.LENGTH_SHORT).show();
            }
        }, 1000);
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. You can now generate PDFs.", 
                              Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied. Cannot generate PDFs.", 
                              Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showProgress() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        btnGeneratePDF.setEnabled(false);
    }

    private void hideProgress() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        btnGeneratePDF.setEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}