package com.example.routewisecollection.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.routewisecollection.R;
import com.example.routewisecollection.models.Customer;
import com.example.routewisecollection.models.Deposit;
import com.example.routewisecollection.utils.Constants;
import com.example.routewisecollection.utils.LoginManager;
import com.example.routewisecollection.utils.SmsUtils;
import com.example.routewisecollection.utils.WhatsAppUtils;
import com.example.routewisecollection.viewmodel.DepositViewModel;

import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DepositActivity extends AppCompatActivity {

    private CardView cardDepositOption, cardWithdrawOption;
    private Spinner spinnerCustomer;
    private EditText etDepositAmount;
    private EditText etSearchCustomer;
    private TextView tvCustomerDetails, tvInterestAmount, tvTotalAmount;
    private android.view.View btnCalculate, btnSaveDeposit, btnCancel;
    private List<Customer> customerList = new ArrayList<>();
    private List<Customer> displayedCustomers = new ArrayList<>();
    private ArrayAdapter<String> customerNamesAdapter;
    private Customer selectedCustomer;

    private DatabaseReference customersRef;
    private DatabaseReference transactionsRef;
    private String agentMobile;
    private String agentName;
    private RadioGroup radioGroupDepositType;
    private RadioButton radioCash, radioOnline;
    private com.google.android.material.textfield.TextInputLayout tilOnlineMobileNumber;
    private EditText etOnlineMobileNumber;
    private DepositViewModel depositViewModel;
    private static final int SMS_PERMISSION_REQUEST_CODE = 100;
    
    // Pre-selected customer from intent
    private String preSelectedCustomerPhone;
    private String preSelectedCustomerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposit);
        
        // Initialize ViewModel
        depositViewModel = new ViewModelProvider(this).get(DepositViewModel.class);

        // Initialize views
        cardDepositOption = findViewById(R.id.cardDepositOption);
        cardWithdrawOption = findViewById(R.id.cardWithdrawOption);
        spinnerCustomer = findViewById(R.id.spinnerCustomer);
        etSearchCustomer = findViewById(R.id.etSearchCustomer);
        etDepositAmount = findViewById(R.id.etDepositAmount);
        tvCustomerDetails = findViewById(R.id.tvCustomerDetails);
        tvInterestAmount = findViewById(R.id.tvInterestAmount);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnSaveDeposit = findViewById(R.id.btnSaveDepositLayout);
        btnCancel = findViewById(R.id.btnCancelDeposit);
        radioGroupDepositType = findViewById(R.id.radioGroupDepositType);
        radioCash = findViewById(R.id.radioCash);
        radioOnline = findViewById(R.id.radioOnline);
        tilOnlineMobileNumber = findViewById(R.id.tilOnlineMobileNumber);
        etOnlineMobileNumber = findViewById(R.id.etOnlineMobileNumber);

        // Get agent info from LoginManager session
        LoginManager loginManager = new LoginManager(this);
        agentMobile = loginManager.getAgentMobile();
        agentName = loginManager.getAgentName();
        
        // Get pre-selected customer from intent (if coming from customer click)
        Intent intent = getIntent();
        if (intent != null) {
            preSelectedCustomerPhone = intent.getStringExtra("CUSTOMER_PHONE");
            preSelectedCustomerName = intent.getStringExtra("CUSTOMER_NAME");
        }
        
        // Check and request SMS permission if needed
        if (!SmsUtils.isSmsPermissionGranted(this)) {
            SmsUtils.requestSmsPermission(this);
        }

        // Initialize Firebase references
        customersRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentMobile)
                .child("customers");

        transactionsRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentMobile)
                .child("transactions");

        setupDashboardClickListeners();
        fetchCustomersFromFirebase();
        setupClickListeners();
        setupSearch();
        setupDepositTypeListener();
    }

    private void setupDepositTypeListener() {
        radioGroupDepositType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioOnline) {
                tilOnlineMobileNumber.setVisibility(android.view.View.VISIBLE);
            } else {
                tilOnlineMobileNumber.setVisibility(android.view.View.GONE);
                etOnlineMobileNumber.setText(""); // Clear text when hidden
            }
        });
    }

    private void setupDashboardClickListeners() {
        // Deposit option - stays on current screen (no action needed)
        cardDepositOption.setOnClickListener(v -> Toast.makeText(this, "Deposit mode selected", Toast.LENGTH_SHORT).show());

        // Withdraw option - navigate to WithdrawActivity
        cardWithdrawOption.setOnClickListener(v -> {
            Intent intent = new Intent(DepositActivity.this, WithdrawActivity.class);
            startActivity(intent);
        });
    }

    private void fetchCustomersFromFirebase() {
        customersRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                customerList.clear();
                List<String> customerNames = new ArrayList<>();
                for (DataSnapshot customerSnapshot : snapshot.getChildren()) {
                    Customer customer = customerSnapshot.getValue(Customer.class);
                    if (customer != null) {
                        customerList.add(customer);
                        customerNames.add(customer.getName() + " - " + customer.getAccountNumber());
                    }
                }
                customerNamesAdapter = new ArrayAdapter<>(DepositActivity.this, android.R.layout.simple_spinner_item, customerNames);
                customerNamesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCustomer.setAdapter(customerNamesAdapter);

                if (!customerList.isEmpty()) {
                    // Check if we have a pre-selected customer from intent
                    if (preSelectedCustomerPhone != null && !preSelectedCustomerPhone.isEmpty()) {
                        // Find and select the customer by phone number
                        selectCustomerByPhone(preSelectedCustomerPhone);
                    } else {
                        // Default behavior - update with search query
                        updateCustomerSpinner(etSearchCustomer.getText() != null ? etSearchCustomer.getText().toString() : "");
                    }
                } else {
                    selectedCustomer = null;
                    tvCustomerDetails.setText("No customers found.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DepositActivity.this, "Failed to load customers: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch() {
        if (etSearchCustomer == null) return;
        etSearchCustomer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCustomerSpinner(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }
    
    /**
     * Select a customer in the spinner by their phone number
     * This is used when coming from customer list click
     */
    private void selectCustomerByPhone(String phoneNumber) {
        if (customerList == null || customerList.isEmpty() || phoneNumber == null) return;
        
        // Find the customer with matching phone number
        int customerIndex = -1;
        for (int i = 0; i < customerList.size(); i++) {
            Customer customer = customerList.get(i);
            if (customer.getPhoneNumber() != null && customer.getPhoneNumber().equals(phoneNumber)) {
                customerIndex = i;
                break;
            }
        }
        
        if (customerIndex != -1) {
            // Found the customer - set up displayedCustomers with this customer first
            displayedCustomers = new ArrayList<>(customerList);
            
            // Update adapter with all customers
            List<String> names = new ArrayList<>();
            for (Customer c : displayedCustomers) {
                names.add(c.getName() + " - " + c.getAccountNumber());
            }
            customerNamesAdapter.clear();
            customerNamesAdapter.addAll(names);
            customerNamesAdapter.notifyDataSetChanged();
            
            // Select the customer in the spinner
            spinnerCustomer.setSelection(customerIndex);
            selectedCustomer = customerList.get(customerIndex);
            displayCustomerDetails();
            
            // Show a toast to confirm selection
            Toast.makeText(this, "Selected: " + selectedCustomer.getName(), Toast.LENGTH_SHORT).show();
        } else {
            // Customer not found - use default behavior
            updateCustomerSpinner("");
        }
    }

    private void updateCustomerSpinner(String query) {
        if (customerList == null || customerList.isEmpty() || customerNamesAdapter == null) return;
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Customer> sorted = new ArrayList<>(customerList);
        if (!q.isEmpty()) {
            Collections.sort(sorted, (a, b) -> {
                int sa = scoreCustomer(a, q);
                int sb = scoreCustomer(b, q);
                return Integer.compare(sb, sa);
            });
        }
        displayedCustomers = sorted;
        List<String> names = new ArrayList<>();
        for (Customer c : sorted) {
            names.add(c.getName() + " - " + c.getAccountNumber());
        }
        customerNamesAdapter.clear();
        customerNamesAdapter.addAll(names);
        customerNamesAdapter.notifyDataSetChanged();
        if (!sorted.isEmpty()) {
            spinnerCustomer.setSelection(0);
            selectedCustomer = displayedCustomers.get(0);
            displayCustomerDetails();
        }
    }

    private int scoreCustomer(Customer c, String q) {
        String name = c.getName() != null ? c.getName().toLowerCase() : "";
        String acc = c.getAccountNumber() != null ? c.getAccountNumber().toLowerCase() : "";
        String ph = c.getPhoneNumber() != null ? c.getPhoneNumber().toLowerCase() : "";
        int score = 0;
        if (name.startsWith(q)) score += 100;
        else if (name.contains(q)) score += 60;
        if (acc.startsWith(q)) score += 90;
        else if (acc.contains(q)) score += 50;
        if (ph.startsWith(q)) score += 80;
        else if (ph.contains(q)) score += 40;
        return score;
    }

    private void setupClickListeners() {
        btnCalculate.setOnClickListener(v -> calculateInterest());
        btnSaveDeposit.setOnClickListener(v -> saveDeposit());
        btnCancel.setOnClickListener(v -> finish());

        spinnerCustomer.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position >= 0 && position < displayedCustomers.size()) {
                    selectedCustomer = displayedCustomers.get(position);
                    displayCustomerDetails();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedCustomer = null;
                tvCustomerDetails.setText("");
                tvInterestAmount.setText("");
                tvTotalAmount.setText("");
            }
        });
    }

    private void displayCustomerDetails() {
        if (selectedCustomer != null) {
            String details = "Name: " + selectedCustomer.getName() + "\n" +
                    "Account: " + selectedCustomer.getAccountNumber() + "\n" +
                    "Phone: " + selectedCustomer.getPhoneNumber();
            tvCustomerDetails.setText(details);
        }
    }

    private void calculateInterest() {
        String amountStr = etDepositAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter deposit amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCustomer == null) {
            Toast.makeText(this, "Please select a customer", Toast.LENGTH_SHORT).show();
            return;
        }

        double depositAmount = Double.parseDouble(amountStr);
        
        // No interest calculation needed - just show the entered amount
        // tvInterestAmount.setText("Interest: Rs. 0");
        // tvTotalAmount.setText("Total: Rs. " + String.format(Locale.getDefault(), "%.0f", depositAmount));
    }

    private void saveDeposit() {
        String amountStr = etDepositAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter deposit amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCustomer == null) {
            Toast.makeText(this, "Please select a customer", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected deposit type (mode)
        final String mode;
        String onlineMobileNumber = "";
        
        int selectedRadioButtonId = radioGroupDepositType.getCheckedRadioButtonId();
        if (selectedRadioButtonId == R.id.radioCash) {
            mode = "cash";
        } else if (selectedRadioButtonId == R.id.radioOnline) {
            mode = "online";
            onlineMobileNumber = etOnlineMobileNumber.getText().toString().trim();
            if (onlineMobileNumber.isEmpty()) {
                tilOnlineMobileNumber.setError("Please enter mobile number");
                return;
            } else {
                tilOnlineMobileNumber.setError(null);
            }
        } else {
            mode = "cash"; // default
        }

        double depositAmount = Double.parseDouble(amountStr);
        
        SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date now = new Date();
        String depositDate = dateSdf.format(now);
        
        // Generate timestamp and other required fields
        long timestamp = System.currentTimeMillis();
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        String createdAt = isoFormat.format(new Date(timestamp));
        String receiptNumber = generateReceiptNumber();

        // Save deposit transaction under /agents/{agentMobile}/transactions/{customerPhone}/unique_push_id
        DatabaseReference customerTransactionRef = transactionsRef.child(selectedCustomer.getPhoneNumber()).push();
        String transactionId = customerTransactionRef.getKey();
        
        // Ensure transactionId is not null (fallback if Firebase key generation fails)
        if (transactionId == null) {
            transactionId = "TXN" + System.currentTimeMillis();
        }

        // Create new Deposit object with new structure
        Deposit deposit = new Deposit(
            selectedCustomer.getAccountNumber(),
            depositAmount,
            createdAt,
            selectedCustomer.getPhoneNumber(), // Using phone as customerId
            selectedCustomer.getName(),
            depositDate,
            mode,
            receiptNumber,
            "", // remarks
            timestamp,
            transactionId,
            "deposit", // type
            onlineMobileNumber
        );

        // Check network availability
        boolean isOnline = com.example.routewisecollection.utils.NetworkUtils.isNetworkAvailable(this);
        
        if (isOnline) {
            // ONLINE MODE: Save to Firebase first, then local database
            Map<String, Object> newDepositData = createNewStructureData(
                selectedCustomer.getAccountNumber(),
                depositAmount,
                selectedCustomer.getPhoneNumber(),
                selectedCustomer.getName(),
                depositDate,
                mode,
                receiptNumber,
                timestamp,
                transactionId,
                onlineMobileNumber
            );

            // Save to Firebase
            customerTransactionRef.setValue(newDepositData)
                    .addOnSuccessListener(unused -> {
                        // Mark as synced since we successfully saved to Firebase
                        deposit.setSynced(true);
                        
                        // Also save to local Room database
                        depositViewModel.insertDeposit(deposit, new DepositViewModel.OnInsertListener() {
                            @Override
                            public void onSuccess(long id) {
                                runOnUiThread(() -> {
                                    Toast.makeText(DepositActivity.this, "Deposit saved successfully (Online)", Toast.LENGTH_SHORT).show();
                                    
                                    // Send SMS notification to customer
                                    sendDepositConfirmationSms(selectedCustomer, depositAmount, receiptNumber);
                                    
                                    finish();
                                });
                            }

                            @Override
                            public void onFailure(String error) {
                                runOnUiThread(() -> {
                                    Toast.makeText(DepositActivity.this, "Saved to Firebase but local save failed: " + error, Toast.LENGTH_SHORT).show();
                                    
                                    // Send SMS even if local save failed
                                    sendDepositConfirmationSms(selectedCustomer, depositAmount, receiptNumber);
                                    
                                    finish();
                                });
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        // Firebase failed even though we're online - save locally as unsynced
                        deposit.setSynced(false);
                        
                        depositViewModel.insertDeposit(deposit, new DepositViewModel.OnInsertListener() {
                            @Override
                            public void onSuccess(long id) {
                                runOnUiThread(() -> {
                                    Toast.makeText(DepositActivity.this, "Saved locally. Will sync when connection is stable.", Toast.LENGTH_LONG).show();
                                    finish();
                                });
                            }

                            @Override
                            public void onFailure(String error) {
                                runOnUiThread(() -> {
                                    Toast.makeText(DepositActivity.this, "Failed to save: " + error, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    });
        } else {
            // OFFLINE MODE: Save to local database only
            deposit.setSynced(false); // Mark as not synced
            
            depositViewModel.insertDeposit(deposit, new DepositViewModel.OnInsertListener() {
                @Override
                public void onSuccess(long id) {
                    runOnUiThread(() -> {
                        Toast.makeText(DepositActivity.this, "Saved locally (Offline). Will sync when internet is available.", Toast.LENGTH_LONG).show();
                        
                        // Don't send SMS when offline
                        // SMS will be attempted when synced to Firebase
                        
                        finish();
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(DepositActivity.this, "Failed to save locally: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private String generateReceiptNumber() {
        // Generate receipt number similar to the new format
        return "RCP" + System.currentTimeMillis();
    }
    
    /**
     * Create new structure data exactly as required
     */
    private Map<String, Object> createNewStructureData(String accountNumber, double amount, 
                                                      String customerId, String customerName, 
                                                      String date, String mode, String receiptNumber, 
                                                      long timestamp, String transactionId, String onlineMobileNumber) {
        Map<String, Object> newData = new HashMap<>();
        
        // Exact new structure as requested
        newData.put("accountNumber", accountNumber != null ? accountNumber : "");
        newData.put("amount", amount);
        
        // ISO format timestamp for createdAt
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        newData.put("createdAt", isoFormat.format(new Date(timestamp)));
        
        newData.put("customerId", customerId != null ? customerId : "");
        newData.put("customerName", customerName != null ? customerName : "");
        newData.put("date", date != null ? date : "");
        newData.put("paymentMethod", mode != null ? mode : "cash"); // Payment method (cash/online)
        newData.put("receiptNumber", receiptNumber != null ? receiptNumber : "");
        newData.put("remarks", ""); // Empty remarks
        newData.put("timestamp", timestamp);
        newData.put("transactionId", transactionId != null ? transactionId : "");
        newData.put("type", "deposit"); // Type field for transaction type
        newData.put("onlineMobileNumber", onlineMobileNumber != null ? onlineMobileNumber : "");
        
        return newData;
    }
    
    /**
     * Send SMS confirmation to customer after successful deposit
     */
    private void sendDepositConfirmationSms(Customer customer, double amount, String receiptNumber) {
        if (customer == null || customer.getPhoneNumber() == null || customer.getPhoneNumber().isEmpty()) {
            return;
        }
        
        // Check if SMS permission is granted
        if (!SmsUtils.isSmsPermissionGranted(this)) {
            Toast.makeText(this, "SMS permission not granted. Cannot send SMS.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Calculate pending amount (balance) for the customer
        calculatePendingAmountAndSendSms(customer, amount, receiptNumber);
    }
    
    /**
     * Calculate pending amount from Firebase and send SMS
     */
    private void calculatePendingAmountAndSendSms(Customer customer, double depositAmount, String receiptNumber) {
        DatabaseReference customerTransactionsRef = transactionsRef.child(customer.getPhoneNumber());
        
        customerTransactionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalDeposits = 0;
                double totalWithdrawals = 0;
                
                // Calculate total deposits and withdrawals
                for (DataSnapshot txnSnapshot : snapshot.getChildren()) {
                    String type = txnSnapshot.child("type").getValue(String.class);
                    Double amount = txnSnapshot.child("amount").getValue(Double.class);
                    
                    if (amount != null && type != null) {
                        if ("deposit".equals(type)) {
                            totalDeposits += amount;
                        } else if ("withdrawal".equals(type)) {
                            totalWithdrawals += amount;
                        }
                    }
                }
                
                // Calculate pending/remaining balance
                double pendingAmount = totalDeposits - totalWithdrawals;
                
                // Generate SMS message with pending amount and account number
                String companyName = agentName != null && !agentName.isEmpty() ? agentName : "BhishiGroup";
                String accountNumber = customer.getAccountNumber() != null && !customer.getAccountNumber().isEmpty() 
                    ? customer.getAccountNumber() 
                    : "N/A";
                
                String smsMessage = String.format(Locale.getDefault(),
                    "Dear %s,\n\n" +
                    "Your payment of Rs. %.2f has been received successfully.\n\n" +
                    "Account No: %s\n" +
                    "Total Amount: Rs. %.2f\n\n" +
                    "Thank you for your payment!\n\n" +
                    "- %s",
                    customer.getName(),
                    depositAmount,
                    accountNumber,
                    pendingAmount,
                    companyName
                );
                
                // Send SMS on UI thread
                runOnUiThread(() -> {
                    SmsUtils.sendSms(DepositActivity.this, customer.getPhoneNumber(), smsMessage);
                    
                    // Send WhatsApp message via webhook
                    WhatsAppUtils.sendDepositMessage(
                        DepositActivity.this,
                        customer.getPhoneNumber(),
                        customer.getName(),
                        depositAmount,
                        accountNumber,
                        pendingAmount,
                        companyName
                    );
                });
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // If balance calculation fails, send SMS without pending amount
                runOnUiThread(() -> {
                    String companyName = agentName != null && !agentName.isEmpty() ? agentName : "BhishiGroup";
                    String accountNumber = customer.getAccountNumber() != null && !customer.getAccountNumber().isEmpty() 
                        ? customer.getAccountNumber() 
                        : "N/A";
                    
                    String smsMessage = String.format(Locale.getDefault(),
                        "Dear %s,\n\n" +
                        "Your payment of Rs. %.2f has been received successfully.\n\n" +
                        "Account No: %s\n\n" +
                        "Thank you for your payment!\n\n" +
                        "- %s",
                        customer.getName(),
                        depositAmount,
                        accountNumber,
                        companyName
                    );
                    
                    SmsUtils.sendSms(DepositActivity.this, customer.getPhoneNumber(), smsMessage);
                    
                    // Send WhatsApp message even if balance calculation failed
                    // Use depositAmount as totalAmount since we couldn't calculate the balance
                    WhatsAppUtils.sendDepositMessage(
                        DepositActivity.this,
                        customer.getPhoneNumber(),
                        customer.getName(),
                        depositAmount,
                        accountNumber,
                        depositAmount, // Use deposit amount as fallback
                        companyName
                    );
                });
            }
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied. SMS notifications will not be sent.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public RadioButton getRadioCash() {
        return radioCash;
    }

    public void setRadioCash(RadioButton radioCash) {
        this.radioCash = radioCash;
    }
}
