package com.example.routewisecollection.activities;

import android.Manifest;
import android.app.AlertDialog;
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
import com.example.routewisecollection.adapters.DailySummaryAdapter;
import com.example.routewisecollection.models.Transaction;
import com.example.routewisecollection.models.Deposit;
import com.example.routewisecollection.models.Withdraw;
import com.example.routewisecollection.models.ReportData;
import com.example.routewisecollection.utils.Constants;
import com.example.routewisecollection.utils.LoginManager;
import com.example.routewisecollection.utils.PDFGenerator;
import com.example.routewisecollection.viewmodel.DepositViewModel;
import com.example.routewisecollection.viewmodel.WithdrawViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MonthlyReportActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private TextView tvSelectedMonth, tvTotalEntries, tvTotalCollection, tvTotalCustomers, tvNoData;
    private CardView btnSelectMonth, btnGeneratePDF;
    private RecyclerView rvDailySummary;
    private ProgressBar progressBar;

    private DailySummaryAdapter adapter;
    private ReportData.MonthlyReport currentReport;

    private int selectedMonth, selectedYear;
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    private String agentMobile;
    private DepositViewModel depositViewModel;
    private WithdrawViewModel withdrawViewModel;
    
    private List<Deposit> allDeposits = new ArrayList<>();
    private List<Withdraw> allWithdrawals = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_monthly_report);

            // Enable back button in action bar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Monthly Report");
            }

            // Initialize ViewModels
            depositViewModel = new ViewModelProvider(this).get(DepositViewModel.class);
            withdrawViewModel = new ViewModelProvider(this).get(WithdrawViewModel.class);

            initializeViews();
            setupClickListeners();
            setupRecyclerView();
            checkPermissions();

            // Set current month as default
            Calendar cal = Calendar.getInstance();
            selectedMonth = cal.get(Calendar.MONTH) + 1;
            selectedYear = cal.get(Calendar.YEAR);
            updateSelectedMonthDisplay();
            
            // Start observing data
            observeData();
            
        } catch (Exception e) {
            Log.e("MonthlyReport", "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading monthly report: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        tvTotalEntries = findViewById(R.id.tvTotalEntries);
        tvTotalCollection = findViewById(R.id.tvTotalCollection);
        tvTotalCustomers = findViewById(R.id.tvTotalCustomers);
        tvNoData = findViewById(R.id.tvNoData);
        btnSelectMonth = findViewById(R.id.btnSelectMonth);
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF);
        rvDailySummary = findViewById(R.id.rvDailySummary);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnSelectMonth.setOnClickListener(v -> showMonthYearPicker());
        btnGeneratePDF.setOnClickListener(v -> generatePDF());
    }

    private void setupRecyclerView() {
        adapter = new DailySummaryAdapter(new ArrayList<>());
        rvDailySummary.setLayoutManager(new LinearLayoutManager(this));
        rvDailySummary.setAdapter(adapter);
    }

    private void observeData() {
        progressBar.setVisibility(View.VISIBLE);
        
        depositViewModel.getAllDeposits().observe(this, deposits -> {
            allDeposits = deposits != null ? deposits : new ArrayList<>();
            processData();
        });
        
        withdrawViewModel.getAllWithdraws().observe(this, withdrawals -> {
            allWithdrawals = withdrawals != null ? withdrawals : new ArrayList<>();
            processData();
        });
    }

    private void processData() {
        String monthPrefix = String.format(Locale.getDefault(), "%04d-%02d", selectedYear, selectedMonth);
        List<Transaction> filteredTransactions = new ArrayList<>();
        
        // Filter Deposits
        for (Deposit d : allDeposits) {
            if (d.getDate() != null && d.getDate().startsWith(monthPrefix)) {
                Transaction t = new Transaction();
                t.setDate(d.getDate());
                t.setAmount(d.getAmount());
                t.setCustomerName(d.getCustomerName() != null ? d.getCustomerName() : "Unknown");
                t.setCustomerId(d.getCustomerId());
                t.setAccountNumber(d.getAccountNumber() != null ? d.getAccountNumber() : "N/A");
                t.setTimestamp(d.getTimestamp());
                t.setType("deposit");
                t.setTypeDisplay("Deposit");
                t.setOnlineMobileNumber(d.getOnlineMobileNumber());
                filteredTransactions.add(t);
            }
        }
        
        // Filter Withdrawals
        for (Withdraw w : allWithdrawals) {
            if (w.getWithdrawDate() != null && w.getWithdrawDate().startsWith(monthPrefix)) {
                Transaction t = new Transaction();
                t.setDate(w.getWithdrawDate());
                t.setAmount(w.getAmount());
                t.setCustomerName(w.getCustomerName() != null ? w.getCustomerName() : "Unknown");
                t.setCustomerId(String.valueOf(w.getCustomerId()));
                t.setAccountNumber(w.getAccountNumber() != null ? w.getAccountNumber() : "N/A");
                
                // Construct timestamp
                long ts = System.currentTimeMillis();
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    Date date = sdf.parse(w.getWithdrawDate() + " " + (w.getWithdrawTime() != null ? w.getWithdrawTime() : "00:00"));
                    if (date != null) ts = date.getTime();
                } catch (Exception ignored) {}
                
                t.setTimestamp(ts);
                t.setType("withdrawal");
                t.setTypeDisplay("Withdrawal");
                t.setOnlineMobileNumber(w.getOnlineMobileNumber());
                filteredTransactions.add(t);
            }
        }
        
        calculateMonthlySummary(filteredTransactions);
        progressBar.setVisibility(View.GONE);
        updateUI();
    }

    private void showMonthYearPicker() {
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Month and Year");

        String[] monthYearOptions = new String[24]; // Last 2 years
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);

        for (int i = 0; i < 24; i++) {
            int year = currentYear - (i / 12);
            int month = (cal.get(Calendar.MONTH) - (i % 12) + 12) % 12;
            if (month == 0) month = 12;
            monthYearOptions[i] = months[month - 1] + " " + year;
        }

        builder.setItems(monthYearOptions, (dialog, which) -> {
            int year = currentYear - (which / 12);
            int month = (cal.get(Calendar.MONTH) - (which % 12) + 12) % 12;
            if (month == 0) month = 12;

            selectedMonth = month;
            selectedYear = year;
            updateSelectedMonthDisplay();
            processData(); // Re-filter data
        });

        builder.show();
    }

    private void updateSelectedMonthDisplay() {
        Calendar cal = Calendar.getInstance();
        cal.set(selectedYear, selectedMonth - 1, 1);
        String displayMonth = monthYearFormat.format(cal.getTime());

        Calendar current = Calendar.getInstance();
        if (selectedMonth == (current.get(Calendar.MONTH) + 1) &&
            selectedYear == current.get(Calendar.YEAR)) {
            tvSelectedMonth.setText("Current Month (" + displayMonth + ")");
        } else {
            tvSelectedMonth.setText(displayMonth);
        }
    }

    private void calculateMonthlySummary(List<Transaction> transactions) {
        try {
            java.util.Map<String, ReportData.DailySummary> dailySummaryMap = new java.util.HashMap<>();
            java.util.Set<String> uniqueCustomers = new java.util.HashSet<>();
            
            double totalDeposits = 0.0;
            double totalWithdrawals = 0.0;
            int totalEntries = 0;
            
            for (Transaction transaction : transactions) {
                String date = transaction.getDate();
                double amount = transaction.getAmount();
                boolean isDeposit = transaction.isDeposit();
                
                totalEntries++;
                uniqueCustomers.add(transaction.getCustomerId());
                
                if (isDeposit) {
                    totalDeposits += amount;
                } else {
                    totalWithdrawals += amount;
                }
                
                if (!dailySummaryMap.containsKey(date)) {
                    dailySummaryMap.put(date, new ReportData.DailySummary(date, 0.0, 0));
                }
                
                ReportData.DailySummary summary = dailySummaryMap.get(date);
                if (isDeposit) {
                    summary.setTotalAmount(summary.getTotalAmount() + amount);
                } else {
                    summary.setTotalAmount(summary.getTotalAmount() - amount);
                }
                summary.setEntryCount(summary.getEntryCount() + 1);
            }
            
            List<ReportData.DailySummary> dailySummaries = new ArrayList<>(dailySummaryMap.values());
            java.util.Collections.sort(dailySummaries, (s1, s2) -> s2.getDate().compareTo(s1.getDate()));
            
            currentReport = new ReportData.MonthlyReport(
                String.format(Locale.getDefault(), "%02d", selectedMonth),
                String.valueOf(selectedYear),
                totalDeposits - totalWithdrawals,
                totalEntries,
                uniqueCustomers.size(),
                dailySummaries
            );
            currentReport.setAllTransactions(transactions);
                  
        } catch (Exception e) {
            Log.e("MonthlyReport", "Error calculating summary: " + e.getMessage(), e);
        }
    }

    private void updateUI() {
        if (currentReport != null) {
            tvTotalEntries.setText(String.valueOf(currentReport.getTotalEntries()));
            tvTotalCollection.setText("₹" + String.format(Locale.getDefault(), "%.2f", currentReport.getTotalCollection()));
            tvTotalCustomers.setText(String.valueOf(currentReport.getTotalCustomers()));

            if (currentReport.getDailySummaries().isEmpty()) {
                rvDailySummary.setVisibility(View.GONE);
                tvNoData.setVisibility(View.VISIBLE);
            } else {
                rvDailySummary.setVisibility(View.VISIBLE);
                tvNoData.setVisibility(View.GONE);
                adapter.updateSummaries(currentReport.getDailySummaries());
            }
        }
    }

    private void generatePDF() {
        if (!checkPermissions()) return;
        if (currentReport == null) {
            Toast.makeText(this, "No report data available", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress();
        new android.os.Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            String companyName = prefs.getString(Constants.PREF_COMPANY_NAME, Constants.DEFAULT_COMPANY_NAME);
            String selectedMonthText = tvSelectedMonth.getText().toString();
            
            LoginManager loginManager = new LoginManager(this);
            String pdfPath = PDFGenerator.generateMonthlyReportPDF(
                this, currentReport, selectedMonthText, loginManager.getAgentName(), loginManager.getAgentMobile(), companyName
            );
            
            hideProgress();
            if (pdfPath != null) Toast.makeText(this, "PDF generated successfully!", Toast.LENGTH_SHORT).show();
        }, 1000);
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgress() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnGeneratePDF.setEnabled(false);
    }

    private void hideProgress() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        btnGeneratePDF.setEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

