package com.example.routewisecollection.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.routewisecollection.R;
import com.example.routewisecollection.models.Customer;
import com.example.routewisecollection.models.Withdraw;
import com.example.routewisecollection.utils.Constants;
import com.example.routewisecollection.utils.LoginManager;
import com.example.routewisecollection.utils.SmsUtils;
import com.example.routewisecollection.utils.WhatsAppUtils;
import com.example.routewisecollection.viewmodel.WithdrawViewModel;

import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WithdrawActivity extends AppCompatActivity {

    private Spinner spinnerCustomerWithdraw;
    private EditText etSearchCustomerWithdraw;
    private EditText etWithdrawAmount;
    private EditText etWithdrawNotes;
    private TextView tvCustomerDetailsWithdraw;
    private android.view.View btnSaveWithdraw, btnCancelWithdraw;
    private RadioGroup radioGroupWithdrawType;

    private final List<Customer> customerList = new ArrayList<>();
    private List<Customer> displayedCustomers = new ArrayList<>();
    private ArrayAdapter<String> customerNamesAdapter;
    private Customer selectedCustomer;

    private DatabaseReference customersRef;
    private DatabaseReference transactionsRef;
    private String agentName;
    private com.google.android.material.textfield.TextInputLayout tilOnlineMobileNumberWithdraw;
    private EditText etOnlineMobileNumberWithdraw;
    private WithdrawViewModel withdrawViewModel;
    private static final int SMS_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);
        
        // Initialize ViewModel
        withdrawViewModel = new ViewModelProvider(this).get(WithdrawViewModel.class);

        spinnerCustomerWithdraw = findViewById(R.id.spinnerCustomerWithdraw);
        etSearchCustomerWithdraw = findViewById(R.id.etSearchCustomerWithdraw);
        etWithdrawAmount = findViewById(R.id.etWithdrawAmount);
        etWithdrawNotes = findViewById(R.id.etWithdrawNotes);
        tvCustomerDetailsWithdraw = findViewById(R.id.tvCustomerDetailsWithdraw);
        btnSaveWithdraw = findViewById(R.id.btnSaveWithdraw);
        btnCancelWithdraw = findViewById(R.id.btnCancelWithdraw);
        radioGroupWithdrawType = findViewById(R.id.radioGroupWithdrawType);
        tilOnlineMobileNumberWithdraw = findViewById(R.id.tilOnlineMobileNumberWithdraw);
        etOnlineMobileNumberWithdraw = findViewById(R.id.etOnlineMobileNumberWithdraw);

        LoginManager loginManager = new LoginManager(this);
        String agentMobile = loginManager.getAgentMobile();
        agentName = loginManager.getAgentName();
        
        // Check and request SMS permission if needed
        if (!SmsUtils.isSmsPermissionGranted(this)) {
            SmsUtils.requestSmsPermission(this);
        }

        customersRef = FirebaseDatabase.getInstance()
                .getReference("agents").child(agentMobile).child("customers");
        DatabaseReference withdrawalsRef = FirebaseDatabase.getInstance()
                .getReference("agents").child(agentMobile).child("withdrawals");
        transactionsRef = FirebaseDatabase.getInstance()
                .getReference("agents").child(agentMobile).child("transactions");

        fetchCustomers();
        setupSearch();
        setupClicks();
        setupWithdrawTypeListener();
    }

    private void setupWithdrawTypeListener() {
        radioGroupWithdrawType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioOnlineWithdraw) {
                tilOnlineMobileNumberWithdraw.setVisibility(android.view.View.VISIBLE);
            } else {
                tilOnlineMobileNumberWithdraw.setVisibility(android.view.View.GONE);
                etOnlineMobileNumberWithdraw.setText(""); // Clear text when hidden
            }
        });
    }

    private void fetchCustomers() {
        customersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                customerList.clear();
                List<String> names = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Customer c = snap.getValue(Customer.class);
                    if (c != null) {
                        customerList.add(c);
                        names.add(c.getName() + " - " + c.getAccountNumber());
                    }
                }
                customerNamesAdapter = new ArrayAdapter<>(WithdrawActivity.this, android.R.layout.simple_spinner_item, names);
                customerNamesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCustomerWithdraw.setAdapter(customerNamesAdapter);

                updateCustomerSpinner(etSearchCustomerWithdraw.getText() != null ? etSearchCustomerWithdraw.getText().toString() : "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(WithdrawActivity.this, "Failed to load customers: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch() {
        etSearchCustomerWithdraw.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateCustomerSpinner(s != null ? s.toString() : ""); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClicks() {
        spinnerCustomerWithdraw.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position >= 0 && position < displayedCustomers.size()) {
                    selectedCustomer = displayedCustomers.get(position);
                    displayCustomerDetails();
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { selectedCustomer = null; tvCustomerDetailsWithdraw.setText(""); }
        });

        btnSaveWithdraw.setOnClickListener(v -> saveWithdraw());
        btnCancelWithdraw.setOnClickListener(v -> finish());
    }

    private void displayCustomerDetails() {
        if (selectedCustomer == null) return;
        String details = "Name: " + selectedCustomer.getName() + "\n" +
                "Account: " + selectedCustomer.getAccountNumber() + "\n" +
                "Mobile: " + selectedCustomer.getPhoneNumber();
        tvCustomerDetailsWithdraw.setText(details);
    }

    private void updateCustomerSpinner(String query) {
        if (customerList.isEmpty() || customerNamesAdapter == null) return;
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Customer> sorted = new ArrayList<>(customerList);
        if (!q.isEmpty()) {
            sorted.sort((a, b) -> Integer.compare(score(b, q), score(a, q)));
        }
        displayedCustomers = sorted;
        List<String> names = new ArrayList<>();
        for (Customer c : sorted) names.add(c.getName() + " - " + c.getAccountNumber());
        customerNamesAdapter.clear();
        customerNamesAdapter.addAll(names);
        customerNamesAdapter.notifyDataSetChanged();
        if (!sorted.isEmpty()) {
            spinnerCustomerWithdraw.setSelection(0);
            selectedCustomer = sorted.get(0);
            displayCustomerDetails();
        }
    }

    private int score(Customer c, String q) {
        String name = c.getName() != null ? c.getName().toLowerCase() : "";
        String acc = c.getAccountNumber() != null ? c.getAccountNumber().toLowerCase() : "";
        String ph = c.getPhoneNumber() != null ? c.getPhoneNumber().toLowerCase() : "";
        int s = 0;
        if (name.startsWith(q)) s += 100; else if (name.contains(q)) s += 60;
        if (acc.startsWith(q)) s += 90; else if (acc.contains(q)) s += 50;
        if (ph.startsWith(q)) s += 80; else if (ph.contains(q)) s += 40;
        return s;
    }

    private void saveWithdraw() {
        String amountStr = etWithdrawAmount.getText().toString().trim();
        if (amountStr.isEmpty()) { Toast.makeText(this, "Please enter withdrawal amount", Toast.LENGTH_SHORT).show(); return; }
        if (selectedCustomer == null) { Toast.makeText(this, "Please select a customer", Toast.LENGTH_SHORT).show(); return; }

        double originalAmount = Double.parseDouble(amountStr);
        if (originalAmount <= 0) { Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show(); return; }
        
        // Apply 5% penalty on withdrawal
        double penaltyPercentage = 5.0;
        double penalty = (originalAmount * penaltyPercentage) / 100.0;
        double finalAmount = originalAmount - penalty;
        boolean penaltyApplied = true;

        // Get selected withdraw type (mode)
        final String mode;
        String onlineMobileNumber = "";
        
        int selectedRadioButtonId = radioGroupWithdrawType.getCheckedRadioButtonId();
        if (selectedRadioButtonId == R.id.radioCashWithdraw) {
            mode = "cash";
        } else if (selectedRadioButtonId == R.id.radioOnlineWithdraw) {
            mode = "online";
            onlineMobileNumber = etOnlineMobileNumberWithdraw.getText().toString().trim();
             if (onlineMobileNumber.isEmpty()) {
                tilOnlineMobileNumberWithdraw.setError("Please enter mobile number");
                return;
            } else {
                tilOnlineMobileNumberWithdraw.setError(null);
            }
        } else {
            mode = "cash"; // default
        }

        SimpleDateFormat dateSdf = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
        SimpleDateFormat timeSdf = new SimpleDateFormat(Constants.TIME_FORMAT, Locale.getDefault());
        Date now = new Date();
        String date = dateSdf.format(now);
        String time = timeSdf.format(now);

        String receiptNumber = generateReceiptNumber();
        String notes = etWithdrawNotes.getText() != null ? etWithdrawNotes.getText().toString().trim() : "";
        if (notes.isEmpty()) {
            notes = String.format(Locale.getDefault(), 
                "Early withdrawal - Penalty: ₹%.2f (%.0f%% of withdrawal amount)", 
                penalty, penaltyPercentage);
        } else {
            notes = notes + " | Penalty: ₹" + String.format(Locale.getDefault(), "%.2f", penalty);
        }

        // Generate transaction details
        String transactionId = "WDRW" + System.currentTimeMillis();
        long timestamp = System.currentTimeMillis();
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        String createdAt = isoFormat.format(new Date(timestamp));

        // Create Withdraw object (store final amount after penalty)
        Withdraw withdrawal = new Withdraw(
                selectedCustomer.getPhoneNumber(),
                selectedCustomer.getAccountNumber(),
                finalAmount,
                date,
                time,
                receiptNumber,
                notes,
                selectedCustomer.getName(),
                mode.toUpperCase(), // Use selected mode
                onlineMobileNumber
        );
        withdrawal.setTimestamp(timestamp);
        withdrawal.setTransactionId(transactionId);
        
        // Check network availability
        boolean isOnline = com.example.routewisecollection.utils.NetworkUtils.isNetworkAvailable(this);
        
        if (isOnline) {
            // ONLINE MODE: Calculate balance and save to Firebase
            DatabaseReference customerTransactionRef = transactionsRef.child(selectedCustomer.getPhoneNumber()).push();
            String firebaseTransactionId = customerTransactionRef.getKey();
            
            if (firebaseTransactionId == null) {
                firebaseTransactionId = transactionId;
            }
            
            final String finalTransactionId = firebaseTransactionId;
            withdrawal.setTransactionId(finalTransactionId);
            
            // Calculate total balance before saving
            calculateTotalBalanceAndSave(selectedCustomer, finalAmount, originalAmount, penalty, penaltyApplied, 
                mode, date, time, receiptNumber, notes, timestamp, createdAt, finalTransactionId, customerTransactionRef, withdrawal, true, onlineMobileNumber);
        } else {
            // OFFLINE MODE: Save to local database only
            withdrawal.setSynced(false); // Mark as not synced
            
            withdrawViewModel.insertWithdraw(withdrawal, new WithdrawViewModel.OnInsertListener() {
                @Override
                public void onSuccess(long id) {
                    runOnUiThread(() -> {
                        String message = String.format(Locale.getDefault(),
                            "Saved locally (Offline)!\nOriginal: ₹%.2f\nPenalty (5%%): ₹%.2f\nFinal Amount: ₹%.2f\n\nWill sync when internet is available.",
                            originalAmount, penalty, finalAmount);
                        Toast.makeText(WithdrawActivity.this, message, Toast.LENGTH_LONG).show();
                        
                        // Don't send SMS when offline
                        
                        finish();
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(WithdrawActivity.this, "Failed to save locally: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }
    
    /**
     * Calculate total balance and save withdrawal transaction
     */
    private void calculateTotalBalanceAndSave(Customer customer, double finalAmount, double originalAmount, 
            double penalty, boolean penaltyApplied, String mode, String date, String time, 
            String receiptNumber, String notes, long timestamp, String createdAt, 
            String transactionId, DatabaseReference customerTransactionRef, Withdraw withdrawal, boolean isOnline, String onlineMobileNumber) {
        
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
                
                // Calculate total balance after this withdrawal
                double totalBalance = totalDeposits - (totalWithdrawals + finalAmount);
                
                // Create withdrawal data with penalty information
                Map<String, Object> withdrawalMap = new HashMap<>();
                withdrawalMap.put("accountNumber", customer.getAccountNumber() != null ? customer.getAccountNumber() : "");
                withdrawalMap.put("amount", finalAmount); // Amount after penalty deduction
                withdrawalMap.put("originalAmount", originalAmount); // Original requested amount
                withdrawalMap.put("penalty", penalty); // Penalty amount
                withdrawalMap.put("penaltyApplied", penaltyApplied); // Flag indicating penalty was applied
                withdrawalMap.put("createdAt", createdAt);
                withdrawalMap.put("customerId", customer.getPhoneNumber()); // Using phone as customerId
                withdrawalMap.put("customerName", customer.getName() != null ? customer.getName() : "");
                withdrawalMap.put("date", date);
                withdrawalMap.put("time", time); // Add time field
                withdrawalMap.put("paymentMethod", mode); // Payment method (cash/online)
                withdrawalMap.put("receiptNumber", receiptNumber != null ? receiptNumber : "");
                withdrawalMap.put("remarks", notes != null ? notes : ""); // Use remarks instead of notes
                withdrawalMap.put("timestamp", timestamp);
                withdrawalMap.put("totalBalance", totalBalance); // Total balance after withdrawal
                withdrawalMap.put("transactionId", transactionId);
                withdrawalMap.put("type", "withdrawal"); // Type field for transaction type
                withdrawalMap.put("onlineMobileNumber", onlineMobileNumber != null ? onlineMobileNumber : "");

                // Save to Firebase
                customerTransactionRef.setValue(withdrawalMap)
                        .addOnSuccessListener(unused -> {
                            // Mark as synced when successfully saved to Firebase
                            withdrawal.setSynced(isOnline);
                            
                            // Also save to local Room database
                            withdrawViewModel.insertWithdraw(withdrawal, new WithdrawViewModel.OnInsertListener() {
                                @Override
                                public void onSuccess(long id) {
                                    runOnUiThread(() -> {
                                        String message = String.format(Locale.getDefault(),
                                            "Withdrawal saved!\nOriginal: ₹%.2f\nPenalty (5%%): ₹%.2f\nFinal Amount: ₹%.2f",
                                            originalAmount, penalty, finalAmount);
                                        Toast.makeText(WithdrawActivity.this, message, Toast.LENGTH_LONG).show();
                                        
                                        // Send SMS notification to customer
                                        sendWithdrawalConfirmationSms(customer, finalAmount, originalAmount, penalty, receiptNumber);
                                        
                                        finish();
                                    });
                                }

                                @Override
                                public void onFailure(String error) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(WithdrawActivity.this, "Saved to Firebase but local save failed: " + error, Toast.LENGTH_SHORT).show();
                                        
                                        // Send SMS even if local save failed
                                        sendWithdrawalConfirmationSms(customer, finalAmount, originalAmount, penalty, receiptNumber);
                                        
                                        finish();
                                    });
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            // Mark as unsynced if Firebase save failed
                            withdrawal.setSynced(false);
                            
                            // Save to local database anyway
                            withdrawViewModel.insertWithdraw(withdrawal, new WithdrawViewModel.OnInsertListener() {
                                @Override
                                public void onSuccess(long id) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(WithdrawActivity.this, "Saved locally. Will sync when connection is stable.", Toast.LENGTH_LONG).show();
                                        finish();
                                    });
                                }

                                @Override
                                public void onFailure(String error) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(WithdrawActivity.this, "Failed to save: " + error, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                        });
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(WithdrawActivity.this, "Failed to calculate balance: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String generateReceiptNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        return Constants.RECEIPT_PREFIX + "W" + sdf.format(new Date());
    }
    
    /**
     * Send SMS confirmation to customer after successful withdrawal
     */
    private void sendWithdrawalConfirmationSms(Customer customer, double finalAmount, double originalAmount, double penalty, String receiptNumber) {
        if (customer == null || customer.getPhoneNumber() == null || customer.getPhoneNumber().isEmpty()) {
            return;
        }
        
        // Check if SMS permission is granted
        if (!SmsUtils.isSmsPermissionGranted(this)) {
            Toast.makeText(this, "SMS permission not granted. Cannot send SMS.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Calculate pending amount (balance) for the customer
        calculatePendingAmountAndSendSms(customer, finalAmount, originalAmount, penalty, receiptNumber);
    }
    
    /**
     * Calculate pending amount from Firebase and send SMS
     */
    private void calculatePendingAmountAndSendSms(Customer customer, double finalAmount, double originalAmount, double penalty, String receiptNumber) {
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
                    "Withdrawal Processed:\n" +
                    "Requested: Rs. %.2f\n" +
                    "Penalty (5%%): Rs. %.2f\n" +
                    "Final Amount: Rs. %.2f\n\n" +
                    "Account No: %s\n" +
                    "Remaining Balance: Rs. %.2f\n\n" +
                    "Thank you!\n\n" +
                    "- %s",
                    customer.getName(),
                    originalAmount,
                    penalty,
                    finalAmount,
                    accountNumber,
                    pendingAmount,
                    companyName
                );
                
                // Send SMS on UI thread
                runOnUiThread(() -> {
                    SmsUtils.sendSms(WithdrawActivity.this, customer.getPhoneNumber(), smsMessage);
                    
                    // Send WhatsApp message via webhook
                    WhatsAppUtils.sendWithdrawalMessage(
                        WithdrawActivity.this,
                        customer.getPhoneNumber(),
                        customer.getName(),
                        finalAmount,
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
                        "Withdrawal Processed:\n" +
                        "Requested: Rs. %.2f\n" +
                        "Penalty (5%%): Rs. %.2f\n" +
                        "Final Amount: Rs. %.2f\n\n" +
                        "Account No: %s\n\n" +
                        "Thank you!\n\n" +
                        "- %s",
                        customer.getName(),
                        originalAmount,
                        penalty,
                        finalAmount,
                        accountNumber,
                        companyName
                    );
                    
                    SmsUtils.sendSms(WithdrawActivity.this, customer.getPhoneNumber(), smsMessage);
                    
                    // Send WhatsApp message even if balance calculation failed
                    // Use finalAmount as totalAmount since we couldn't calculate the balance
                    WhatsAppUtils.sendWithdrawalMessage(
                        WithdrawActivity.this,
                        customer.getPhoneNumber(),
                        customer.getName(),
                        finalAmount,
                        accountNumber,
                        finalAmount, // Use final amount as fallback
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
}
