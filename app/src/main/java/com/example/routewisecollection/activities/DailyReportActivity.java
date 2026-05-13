package com.example.routewisecollection.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.routewisecollection.R;
import com.example.routewisecollection.adapters.TransactionAdapter;
import com.example.routewisecollection.models.Deposit;
import com.example.routewisecollection.models.Transaction;
import com.example.routewisecollection.models.Withdraw;
import com.example.routewisecollection.utils.Constants;
import com.example.routewisecollection.utils.LoginManager;
import com.example.routewisecollection.utils.DatabaseSyncManager;
import com.example.routewisecollection.utils.PDFGenerator;
import com.example.routewisecollection.viewmodel.DepositViewModel;
import com.example.routewisecollection.viewmodel.WithdrawViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyReportActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private TextView tvSelectedDate, tvTotalEntries, tvTotalCollection, tvNoEntries;
    private CardView btnSelectDate, btnGeneratePDF;
    private RecyclerView rvDailyEntries;
    private ProgressBar progressBar;
    private TransactionAdapter adapter;

    private String selectedDate;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private String agentId;
    private List<Transaction> currentTransactions = new ArrayList<>();
    
    private DepositViewModel depositViewModel;
    private WithdrawViewModel withdrawViewModel;
    
    // Store raw lists to combine them
    private List<Deposit> dailyDeposits = new ArrayList<>();
    private List<Withdraw> dailyWithdrawals = new ArrayList<>();
    
    private double totalCollection = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_report);
        
        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Daily Report");
        }

        // Get agent mobile from LoginManager
        LoginManager loginManager = new LoginManager(this);
        agentId = loginManager.getAgentMobile();
        
        // Initialize ViewModels
        depositViewModel = new ViewModelProvider(this).get(DepositViewModel.class);
        withdrawViewModel = new ViewModelProvider(this).get(WithdrawViewModel.class);

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        checkPermissions();

        selectedDate = dateFormat.format(new Date());
        updateSelectedDateDisplay();
        fetchTodaysTransactions();
    }

    private void initializeViews() {
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvTotalEntries = findViewById(R.id.tvTotalEntries);
        tvTotalCollection = findViewById(R.id.tvTotalCollection);
        tvNoEntries = findViewById(R.id.tvNoEntries);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF);
        rvDailyEntries = findViewById(R.id.rvDailyEntries);
        progressBar = findViewById(R.id.progressBar);

        adapter = new TransactionAdapter(new ArrayList<>(), this);
    }

    private void setupRecyclerView() {
        rvDailyEntries.setLayoutManager(new LinearLayoutManager(this));
        rvDailyEntries.setAdapter(adapter);
        
        // Set transaction click listeners
        adapter.setOnTransactionClickListener(new TransactionAdapter.OnTransactionClickListener() {
            @Override
            public void onTransactionClick(Transaction transaction, int position) {
                // Handle transaction click if needed
            }

            @Override
            public void onDeleteClick(Transaction transaction, int position) {
                showDeleteConfirmationDialog(transaction, position);
            }
        });
    }

    private void setupClickListeners() {
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnGeneratePDF.setOnClickListener(v -> generatePDF());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        try {
            Date date = dateFormat.parse(selectedDate);
            calendar.setTime(date);
        } catch (Exception e) {
            // Use current date if parsing fails
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    selectedDate = dateFormat.format(selectedCalendar.getTime());
                    updateSelectedDateDisplay();
                    fetchTodaysTransactions();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void updateSelectedDateDisplay() {
        try {
            Date date = dateFormat.parse(selectedDate);
            String displayDate = displayDateFormat.format(date);

            String today = dateFormat.format(new Date());
            if (selectedDate.equals(today)) {
                tvSelectedDate.setText("Today (" + displayDate + ")");
            } else {
                tvSelectedDate.setText(displayDate);
            }
        } catch (Exception e) {
            tvSelectedDate.setText(selectedDate);
        }
    }

    // Fetch both deposits and withdrawals for selected date from Local DB
    private void fetchTodaysTransactions() {
        showProgress();
        
        // Reset lists
        dailyDeposits.clear();
        dailyWithdrawals.clear();
        
        // Remove existing observers if any (simplified by just re-observing, LiveData handles lifecycle)
        
        // 1. Fetch Deposits
        depositViewModel.getDepositsByDate(selectedDate).observe(this, deposits -> {
            dailyDeposits = deposits != null ? deposits : new ArrayList<>();
            combineAndShowTransactions();
        });
        
        // 2. Fetch Withdrawals
        withdrawViewModel.getWithdrawsByDate(selectedDate).observe(this, withdrawals -> {
            dailyWithdrawals = withdrawals != null ? withdrawals : new ArrayList<>();
            combineAndShowTransactions();
        });
    }
    
    private void combineAndShowTransactions() {
        currentTransactions.clear();
        
        // Process Deposits
        for (Deposit d : dailyDeposits) {
            Transaction t = new Transaction();
            t.setTransactionId(d.getTransactionId());
            t.setCustomerId(d.getCustomerId());
            t.setCustomerName(d.getCustomerName() != null ? d.getCustomerName() : "Unknown");
            t.setAccountNumber(d.getAccountNumber() != null ? d.getAccountNumber() : "N/A");
            t.setAmount(d.getAmount());
            t.setDate(d.getDate());
            t.setMode(d.getMode() != null ? d.getMode() : "cash");
            t.setTimestamp(d.getTimestamp());
            t.setType("deposit");
            t.setTypeDisplay("Deposit");
            t.setOnlineMobileNumber(d.getOnlineMobileNumber());
            currentTransactions.add(t);
        }
        
        // Process Withdrawals
        for (Withdraw w : dailyWithdrawals) {
            Transaction t = new Transaction();
            t.setTransactionId(w.getTransactionId());
            t.setCustomerId(w.getCustomerId());
            t.setCustomerName(w.getCustomerName() != null ? w.getCustomerName() : "Unknown");
            t.setAccountNumber(w.getAccountNumber() != null ? w.getAccountNumber() : "N/A");
            t.setAmount(w.getAmount());
            t.setDate(w.getWithdrawDate());
            t.setMode(w.getWithdrawMethod() != null ? w.getWithdrawMethod() : "CASH");
            t.setTimestamp(w.getTimestamp());
            t.setType("withdrawal");
            t.setTypeDisplay("Withdrawal");
            t.setOnlineMobileNumber(w.getOnlineMobileNumber());
            currentTransactions.add(t);
        }
        
        // Sort transactions by timestamp (latest first)
        Collections.sort(currentTransactions, (t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));
        
        hideProgress();
        updateUI(currentTransactions);
    }

    private void updateUI(List<Transaction> transactions) {
        Log.d("DailyReport", "Transactions fetched: " + (transactions == null ? 0 : transactions.size()));

        tvTotalEntries.setText(String.valueOf(transactions.size()));

        double totalDeposits = 0.0;
        double totalWithdrawals = 0.0;
        
        for (Transaction t : transactions) {
            if (t.isDeposit()) {
                totalDeposits += t.getAmount();
            } else if (t.isWithdraw()) {
                totalWithdrawals += t.getAmount();
            }
        }
        
        // Net collection = Deposits - Withdrawals
        double netCollection = totalDeposits - totalWithdrawals;
        
        // Show net collection (can be negative if withdrawals > deposits)
        tvTotalCollection.setText("₹" + String.format(Locale.getDefault(), "%.2f", netCollection));
        totalCollection = netCollection;

        if (transactions.isEmpty()) {
            rvDailyEntries.setVisibility(View.GONE);
            tvNoEntries.setVisibility(View.VISIBLE);
        } else {
            rvDailyEntries.setVisibility(View.VISIBLE);
            tvNoEntries.setVisibility(View.GONE);
            adapter.updateTransactions(transactions);
        }
    }

    private void showDeleteConfirmationDialog(Transaction transaction, int position) {
        String type = transaction.isDeposit() ? "deposit" : "withdrawal";
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this " + type + " entry?\n\nAccount: " +
                         transaction.getAccountNumber() + "\nAmount: ₹" +
                         String.format(Locale.getDefault(), "%.2f", transaction.getAmount()) + 
                         "\n\n(This will delete the local record.)")
                .setPositiveButton("Delete", (dialog, which) -> deleteTransaction(transaction, position))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteTransaction(Transaction transaction, int position) {
        DatabaseSyncManager syncManager = new DatabaseSyncManager(this, agentId);
        String customerPhone = transaction.getCustomerId();
        String transactionId = transaction.getTransactionId();
        
        if (transactionId == null) {
            Toast.makeText(this, "Cannot delete: Transaction ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseSyncManager.OnSyncCompleteListener listener = new DatabaseSyncManager.OnSyncCompleteListener() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(DailyReportActivity.this, "Entry deleted successfully!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(DailyReportActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        };

        if (transaction.isDeposit()) {
            syncManager.deleteTransaction(customerPhone, transactionId, listener);
        } else {
            syncManager.deleteWithdrawal(customerPhone, transactionId, listener);
        }
    }
    

    private void generatePDF() {
        if (!checkPermissions()) {
            return;
        }

        if (currentTransactions.isEmpty()) {
            Toast.makeText(this, "No data available to generate PDF", Toast.LENGTH_SHORT).show();
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
            
            String pdfPath = PDFGenerator.generateDailyReportPDF(
                this, currentTransactions, selectedDate, totalCollection, agentName, agentMobile, companyName
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