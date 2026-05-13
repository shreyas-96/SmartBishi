package com.example.routewisecollection.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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

public class WeeklyReportActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private Button btnSelectWeek;
    private TextView tvWeekPeriod, tvTotalCollection, tvTotalEntries;
    private CardView summaryCard, dailyBreakdownCard;
    private LinearLayout actionButtons;
    private RecyclerView recyclerViewDailySummary;
    private Button btnSaveReport, btnGeneratePDF;
    private ProgressBar progressBar;

    private DailySummaryAdapter dailySummaryAdapter;
    private Calendar selectedWeekStart;
    private ReportData.WeeklyReport currentWeeklyReport;
    
    private DepositViewModel depositViewModel;
    private WithdrawViewModel withdrawViewModel;
    
    private List<Deposit> allDeposits = new ArrayList<>();
    private List<Withdraw> allWithdrawals = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_report);

        initializeViews();
        
        // Initialize ViewModels
        depositViewModel = new ViewModelProvider(this).get(DepositViewModel.class);
        withdrawViewModel = new ViewModelProvider(this).get(WithdrawViewModel.class);
        
        setupClickListeners();
        setupRecyclerView();
        checkPermissions();
        
        // Start observing data
        observeData();
    }

    private void initializeViews() {
        btnSelectWeek = findViewById(R.id.btnSelectWeek);
        tvWeekPeriod = findViewById(R.id.tvWeekPeriod);
        tvTotalCollection = findViewById(R.id.tvTotalCollection);
        tvTotalEntries = findViewById(R.id.tvTotalEntries);
        summaryCard = findViewById(R.id.summaryCard);
        dailyBreakdownCard = findViewById(R.id.dailyBreakdownCard);
        actionButtons = findViewById(R.id.actionButtons);
        recyclerViewDailySummary = findViewById(R.id.recyclerViewDailySummary);
        btnSaveReport = findViewById(R.id.btnSaveReport);
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnSelectWeek.setOnClickListener(v -> showWeekPicker());
        btnSaveReport.setOnClickListener(v -> saveReport());
        btnGeneratePDF.setOnClickListener(v -> generatePDF());
    }

    private void setupRecyclerView() {
        dailySummaryAdapter = new DailySummaryAdapter();
        recyclerViewDailySummary.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDailySummary.setAdapter(dailySummaryAdapter);
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
        if (selectedWeekStart == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        Calendar weekEnd = Calendar.getInstance();
        weekEnd.setTime(selectedWeekStart.getTime());
        weekEnd.add(Calendar.DAY_OF_MONTH, 6);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDate = sdf.format(selectedWeekStart.getTime());
        String endDate = sdf.format(weekEnd.getTime());
        
        List<Transaction> filteredTransactions = new ArrayList<>();
        
        // Filter Deposits
        for (Deposit d : allDeposits) {
            if (d.getDate() != null && d.getDate().compareTo(startDate) >= 0 && d.getDate().compareTo(endDate) <= 0) {
                Transaction t = new Transaction();
                t.setDate(d.getDate());
                t.setAmount(d.getAmount());
                t.setCustomerName(d.getCustomerName() != null ? d.getCustomerName() : "Unknown");
                t.setCustomerId(d.getCustomerId());
                t.setAccountNumber(d.getAccountNumber() != null ? d.getAccountNumber() : "N/A");
                t.setType("deposit");
                t.setOnlineMobileNumber(d.getOnlineMobileNumber());
                filteredTransactions.add(t);
            }
        }
        
        // Filter Withdrawals
        for (Withdraw w : allWithdrawals) {
            if (w.getWithdrawDate() != null && w.getWithdrawDate().compareTo(startDate) >= 0 && w.getWithdrawDate().compareTo(endDate) <= 0) {
                Transaction t = new Transaction();
                t.setDate(w.getWithdrawDate());
                t.setAmount(w.getAmount());
                t.setCustomerName(w.getCustomerName() != null ? w.getCustomerName() : "Unknown");
                t.setCustomerId(String.valueOf(w.getCustomerId()));
                t.setAccountNumber(w.getAccountNumber() != null ? w.getAccountNumber() : "N/A");
                t.setType("withdrawal");
                t.setOnlineMobileNumber(w.getOnlineMobileNumber());
                filteredTransactions.add(t);
            }
        }
        
        generateWeeklyReport(filteredTransactions, startDate, endDate);
        progressBar.setVisibility(View.GONE);
    }

    private void showWeekPicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    
                    selectedWeekStart = Calendar.getInstance();
                    selectedWeekStart.setTime(selected.getTime());
                    int dayOfWeek = selectedWeekStart.get(Calendar.DAY_OF_WEEK);
                    int daysToSubtract = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
                    selectedWeekStart.add(Calendar.DAY_OF_MONTH, -daysToSubtract);
                    
                    processData();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        
        datePickerDialog.setTitle("Select any day in the week");
        datePickerDialog.show();
    }

    private void generateWeeklyReport(List<Transaction> transactions, String startDate, String endDate) {
        java.util.Map<String, ReportData.DailySummary> dailySummaryMap = new java.util.HashMap<>();
        double totalDeposits = 0;
        double totalWithdrawals = 0;
        int totalEntries = 0;
        
        for (Transaction t : transactions) {
            String date = t.getDate();
            double amount = t.getAmount();
            boolean isDeposit = t.isDeposit();
            
            totalEntries++;
            if (isDeposit) totalDeposits += amount;
            else totalWithdrawals += amount;
            
            if (!dailySummaryMap.containsKey(date)) {
                dailySummaryMap.put(date, new ReportData.DailySummary(date, 0.0, 0));
            }
            
            ReportData.DailySummary summary = dailySummaryMap.get(date);
            if (isDeposit) summary.setTotalAmount(summary.getTotalAmount() + amount);
            else summary.setTotalAmount(summary.getTotalAmount() - amount);
            summary.setEntryCount(summary.getEntryCount() + 1);
        }
        
        List<ReportData.DailySummary> dailySummaries = new ArrayList<>(dailySummaryMap.values());
        java.util.Collections.sort(dailySummaries, (s1, s2) -> s2.getDate().compareTo(s1.getDate()));
        
        String weekPeriod = startDate + " to " + endDate;
        currentWeeklyReport = new ReportData.WeeklyReport(weekPeriod, totalDeposits - totalWithdrawals, totalEntries, dailySummaries);
        
        displayWeeklyReport();
    }

    private void displayWeeklyReport() {
        if (currentWeeklyReport == null) return;
        tvWeekPeriod.setText("Week: " + currentWeeklyReport.getWeekPeriod());
        tvTotalCollection.setText(String.format(Locale.getDefault(), "₹%.2f", currentWeeklyReport.getTotalCollection()));
        tvTotalEntries.setText(String.valueOf(currentWeeklyReport.getTotalEntries()));
        dailySummaryAdapter.setDailySummaries(currentWeeklyReport.getDailySummaries());
        summaryCard.setVisibility(View.VISIBLE);
        dailyBreakdownCard.setVisibility(View.VISIBLE);
        actionButtons.setVisibility(View.VISIBLE);
    }

    private void saveReport() {
        if (currentWeeklyReport == null) return;
        com.example.routewisecollection.utils.ReportManager reportManager = new com.example.routewisecollection.utils.ReportManager(this);
        String filename = reportManager.saveWeeklyReportToFile(currentWeeklyReport);
        if (filename != null) Toast.makeText(this, "Report saved: " + filename, Toast.LENGTH_SHORT).show();
    }

    private void generatePDF() {
        if (!checkPermissions()) return;
        if (currentWeeklyReport == null) {
            Toast.makeText(this, "No report data available", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress();
        new android.os.Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            String companyName = prefs.getString(Constants.PREF_COMPANY_NAME, Constants.DEFAULT_COMPANY_NAME);
            String companyPhone = prefs.getString(Constants.PREF_COMPANY_PHONE, "");
            
            LoginManager loginManager = new LoginManager(this);
            String pdfPath = PDFGenerator.generateWeeklyReportPDF(
                this, currentWeeklyReport, tvWeekPeriod.getText().toString(), 
                loginManager.getAgentName(), loginManager.getAgentMobile(), companyName, companyPhone
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
}

