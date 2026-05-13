package com.example.routewisecollection.activities;

import android.Manifest;
import android.annotation.SuppressLint;
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
import com.example.routewisecollection.models.Transaction;
import com.example.routewisecollection.models.Withdraw;
import com.example.routewisecollection.utils.Constants;
import com.example.routewisecollection.utils.LoginManager;
import com.example.routewisecollection.utils.DatabaseSyncManager;
import com.example.routewisecollection.utils.PDFGenerator;
import com.example.routewisecollection.viewmodel.WithdrawViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WithdrawReportActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private TextView tvSelectedMonth, tvTotalWithdrawals, tvTotalEntries, tvNoEntries;
    private CardView btnSelectMonth, btnGeneratePDF;
    private RecyclerView rvWithdrawEntries;
    private ProgressBar progressBar;
    private TransactionAdapter adapter;

    private String selectedDate;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private boolean showAllDates = true; // Flag to show all dates or specific date

    private String agentId;
    private List<Transaction> currentWithdrawals = new ArrayList<>();
    
    private WithdrawViewModel withdrawViewModel;
    private List<Withdraw> rawWithdrawals = new ArrayList<>();
    
    private double totalWithdrawals = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw_report);
        
        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Withdraw Report");
        }

        // Get agent mobile from LoginManager
        LoginManager loginManager = new LoginManager(this);
        agentId = loginManager.getAgentMobile();
        
        // Initialize ViewModel
        withdrawViewModel = new ViewModelProvider(this).get(WithdrawViewModel.class);

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        checkPermissions();

        selectedDate = dateFormat.format(new Date());
        showAllDates = true; // Initially show all dates
        updateSelectedDateDisplay();
        fetchWithdrawals();
    }

    private void initializeViews() {
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        tvTotalWithdrawals = findViewById(R.id.tvTotalWithdrawals);
        tvTotalEntries = findViewById(R.id.tvTotalEntries);
        tvNoEntries = findViewById(R.id.tvNoEntries);
        btnSelectMonth = findViewById(R.id.btnSelectMonth);
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF);
        rvWithdrawEntries = findViewById(R.id.rvWithdrawEntries);
        progressBar = findViewById(R.id.progressBar);

        adapter = new TransactionAdapter(new ArrayList<>(), this);
    }

    private void setupRecyclerView() {
        rvWithdrawEntries.setLayoutManager(new LinearLayoutManager(this));
        rvWithdrawEntries.setAdapter(adapter);
        
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
        btnSelectMonth.setOnClickListener(v -> showDatePicker());
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

        // Create dialog with options
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Filter")
                .setItems(new String[]{"Show All Withdrawals", "Select Specific Date"}, (dialog, which) -> {
                    if (which == 0) {
                        // Show all withdrawals
                        showAllDates = true;
                        updateSelectedDateDisplay();
                        fetchWithdrawals();
                    } else {
                        // Show date picker for specific date
                        DatePickerDialog datePickerDialog = new DatePickerDialog(
                                this,
                                (view, year, month, dayOfMonth) -> {
                                    Calendar selectedCalendar = Calendar.getInstance();
                                    selectedCalendar.set(year, month, dayOfMonth);
                                    selectedDate = dateFormat.format(selectedCalendar.getTime());
                                    showAllDates = false;
                                    updateSelectedDateDisplay();
                                    fetchWithdrawals();
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                        );
                        datePickerDialog.show();
                    }
                })
                .show();
    }

    private void updateSelectedDateDisplay() {
        try {
            if (showAllDates) {
                tvSelectedMonth.setText("All Withdrawals");
            } else {
                Date date = dateFormat.parse(selectedDate);
                String displayDate = displayDateFormat.format(date);

                String currentDate = dateFormat.format(new Date());
                if (selectedDate.equals(currentDate)) {
                    tvSelectedMonth.setText("Today (" + displayDate + ")");
                } else {
                    tvSelectedMonth.setText(displayDate);
                }
            }
        } catch (Exception e) {
            tvSelectedMonth.setText(selectedDate);
        }
    }

    // Fetch withdrawals for selected date or all dates from Local DB
    private void fetchWithdrawals() {
        showProgress();
        currentWithdrawals.clear();
        rawWithdrawals.clear();
        
        // Reset observers by re-invoking
        if (showAllDates) {
            withdrawViewModel.getAllWithdraws().observe(this, this::processWithdrawals);
        } else {
            withdrawViewModel.getWithdrawsByDate(selectedDate).observe(this, this::processWithdrawals);
        }
    }
    
    private void processWithdrawals(List<Withdraw> withdrawals) {
        rawWithdrawals = withdrawals != null ? withdrawals : new ArrayList<>();
        currentWithdrawals.clear();
        
        for (Withdraw w : rawWithdrawals) {
            Transaction transaction = new Transaction();
            transaction.setTransactionId(w.getTransactionId());
            transaction.setCustomerId(w.getCustomerId());
            transaction.setCustomerName(w.getCustomerName() != null ? w.getCustomerName() : "Unknown");
            transaction.setAccountNumber(w.getAccountNumber() != null ? w.getAccountNumber() : "N/A");
            transaction.setAmount(w.getAmount());
            transaction.setDate(w.getWithdrawDate());
            transaction.setTime(w.getWithdrawTime() != null ? w.getWithdrawTime() : "00:00");
            transaction.setMode(w.getWithdrawMethod() != null ? w.getWithdrawMethod() : "CASH");
            transaction.setOnlineMobileNumber(w.getOnlineMobileNumber());
            transaction.setType("withdrawal");
            transaction.setTypeDisplay("Withdrawal");
            transaction.setTimestamp(w.getTimestamp());
            
            currentWithdrawals.add(transaction);
        }
        
        // Sort already happens in DAO, but for UI mapping purposes, we can double check or rely on List order.
        // DAO order: ORDER BY withdrawDate DESC, withdrawTime DESC
        // So the list should already be in correct order.
        
        hideProgress();
        updateUI(currentWithdrawals);
    }

    private void updateUI(List<Transaction> withdrawals) {
        Log.d("WithdrawReport", "Withdrawals fetched: " + (withdrawals == null ? 0 : withdrawals.size()));

        tvTotalEntries.setText(String.valueOf(withdrawals.size()));

        totalWithdrawals = 0;
        for (Transaction transaction : withdrawals) {
            totalWithdrawals += transaction.getAmount();
        }

        tvTotalWithdrawals.setText(String.format(Locale.getDefault(), "₹%.2f", totalWithdrawals));

        if (withdrawals.isEmpty()) {
            rvWithdrawEntries.setVisibility(View.GONE);
            tvNoEntries.setVisibility(View.VISIBLE);
            
            // Update message based on filter type
            if (showAllDates) {
                tvNoEntries.setText("No withdrawals found");
            } else {
                try {
                    Date date = dateFormat.parse(selectedDate);
                    String displayDate = displayDateFormat.format(date);
                    tvNoEntries.setText("No withdrawals found for " + displayDate);
                } catch (Exception e) {
                    tvNoEntries.setText("No withdrawals found for selected date");
                }
            }
        } else {
            rvWithdrawEntries.setVisibility(View.VISIBLE);
            tvNoEntries.setVisibility(View.GONE);
            adapter.updateTransactions(withdrawals);
        }
    }

    private void showDeleteConfirmationDialog(Transaction transaction, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Withdrawal")
                .setMessage("Are you sure you want to delete this withdrawal entry?\n\nAccount: " +
                         transaction.getAccountNumber() + "\nAmount: ₹" +
                         String.format(Locale.getDefault(), "%.2f", transaction.getAmount()) + 
                         "\n\n(This will delete the local record.)")
                .setPositiveButton("Delete", (dialog, which) -> deleteWithdrawal(transaction, position))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteWithdrawal(Transaction transaction, int position) {
        DatabaseSyncManager syncManager = new DatabaseSyncManager(this, agentId);
        String customerPhone = transaction.getCustomerId();
        String transactionId = transaction.getTransactionId();

        if (transactionId == null) {
            Toast.makeText(this, "Cannot delete: Transaction ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        syncManager.deleteWithdrawal(customerPhone, transactionId, new DatabaseSyncManager.OnSyncCompleteListener() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(WithdrawReportActivity.this, "Withdrawal deleted successfully!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(WithdrawReportActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void generatePDF() {
        if (!checkPermissions()) {
            return;
        }

        if (currentWithdrawals.isEmpty()) {
            Toast.makeText(this, "No withdrawal data available to generate PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress();
        
        new android.os.Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            String companyName = prefs.getString(Constants.PREF_COMPANY_NAME, Constants.DEFAULT_COMPANY_NAME);
            
            // Re-get selected month text if needed, or use selectedDate
            String reportTitle = showAllDates ? "All Withdrawals" : selectedDate;
            
            // Get agent name and mobile from LoginManager (backend/database)
            LoginManager loginManager = new LoginManager(this);
            String agentName = loginManager.getAgentName();
            String agentMobile = loginManager.getAgentMobile();
            
            String pdfPath = PDFGenerator.generateWithdrawReportPDF(
                this, currentWithdrawals, reportTitle, totalWithdrawals, agentName, agentMobile, companyName
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
