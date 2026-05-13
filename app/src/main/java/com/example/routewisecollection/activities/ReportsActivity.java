package com.example.routewisecollection.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import com.example.routewisecollection.R;
import com.example.routewisecollection.models.Deposit;
import com.example.routewisecollection.models.Withdraw;
import com.example.routewisecollection.utils.LoginManager;
import com.example.routewisecollection.viewmodel.DepositViewModel;
import com.example.routewisecollection.viewmodel.WithdrawViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    private TextView tvTodayCollection, tvMonthlyCollection, tvTotalCustomers;
    private TextView reportIcon, headerTitle, headerSubtitle, sectionTitle;
    private CardView btnDailyReport, btnMonthlyReport, btnWithdrawReport, btnCustomerReport;
    private CardView headerCard, todayCard, monthlyCard, customerCard;
    private FloatingActionButton fabAddEntry;

    private DatabaseReference agentCustomersRef;
    private String agentMobile;
    
    private DepositViewModel depositViewModel;
    private WithdrawViewModel withdrawViewModel;
    
    private List<Deposit> allDeposits = new ArrayList<>();
    private List<Withdraw> allWithdrawals = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        initializeViews();
        setupFirebaseReferences();
        
        // Initialize ViewModels
        depositViewModel = new ViewModelProvider(this).get(DepositViewModel.class);
        withdrawViewModel = new ViewModelProvider(this).get(WithdrawViewModel.class);
        
        setupClickListeners();
        startAnimations();
        loadReports();
    }

    private void initializeViews() {
        reportIcon = findViewById(R.id.reportIcon);
        headerTitle = findViewById(R.id.headerTitle);
        headerSubtitle = findViewById(R.id.headerSubtitle);
        sectionTitle = findViewById(R.id.sectionTitle);

        tvTodayCollection = findViewById(R.id.tvTodayCollection);
        tvMonthlyCollection = findViewById(R.id.tvMonthlyCollection);
        tvTotalCustomers = findViewById(R.id.tvTotalCustomers);

        headerCard = findViewById(R.id.headerCard);
        todayCard = findViewById(R.id.todayCard);
        monthlyCard = findViewById(R.id.monthlyCard);
        customerCard = findViewById(R.id.customerCard);

        btnDailyReport = findViewById(R.id.btnDailyReport);
        btnMonthlyReport = findViewById(R.id.btnMonthlyReport);
        btnWithdrawReport = findViewById(R.id.btnWithdrawReport);
        btnCustomerReport = findViewById(R.id.btnCustomerReport);
        fabAddEntry = findViewById(R.id.fabAddEntry);
    }

    private void setupFirebaseReferences() {
        LoginManager loginManager = new LoginManager(this);
        agentMobile = loginManager.getAgentMobile();

        // Reference to customers node under the current agent
        agentCustomersRef = FirebaseDatabase.getInstance()
                .getReference("agents").child(agentMobile).child("customers");
    }

    private void setupClickListeners() {
        btnDailyReport.setOnClickListener(v ->
                startActivity(new Intent(ReportsActivity.this, DailyReportActivity.class)));
        btnMonthlyReport.setOnClickListener(v ->
                startActivity(new Intent(ReportsActivity.this, MonthlyReportActivity.class)));
        btnWithdrawReport.setOnClickListener(v ->
                startActivity(new Intent(ReportsActivity.this, WithdrawReportActivity.class)));
        btnCustomerReport.setOnClickListener(v ->
                startActivity(new Intent(ReportsActivity.this, CustomerReportActivity.class)));
        customerCard.setOnClickListener(v -> {
            Intent intent = new Intent(ReportsActivity.this, ActiveCustomersActivity.class);
            startActivity(intent);
        });


        fabAddEntry.setOnClickListener(v -> {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(ReportsActivity.this);
            builder.setTitle("Quick Actions")
                    .setItems(new CharSequence[]{"Deposit", "Withdraw"}, (dialog, which) -> {
                        if (which == 0) {
                            startActivity(new Intent(ReportsActivity.this, com.example.routewisecollection.activities.DepositActivity.class));
                        } else {
                            startActivity(new Intent(ReportsActivity.this, com.example.routewisecollection.activities.WithdrawActivity.class));
                        }
                    })
                    .show();
        });
    }

    private void startAnimations() {
        ObjectAnimator iconScaleX = ObjectAnimator.ofFloat(reportIcon, "scaleX", 0f, 1.2f, 1f);
        ObjectAnimator iconScaleY = ObjectAnimator.ofFloat(reportIcon, "scaleY", 0f, 1.2f, 1f);
        iconScaleX.setDuration(800);
        iconScaleY.setDuration(800);
        iconScaleX.setInterpolator(new BounceInterpolator());
        iconScaleY.setInterpolator(new BounceInterpolator());
        AnimatorSet iconAnimator = new AnimatorSet();
        iconAnimator.playTogether(iconScaleX, iconScaleY);
        iconAnimator.setStartDelay(200);
        iconAnimator.start();

        new Handler().postDelayed(() -> {
            ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(headerTitle, "alpha", 0f, 1f);
            ObjectAnimator subtitleAlpha = ObjectAnimator.ofFloat(headerSubtitle, "alpha", 0f, 1f);
            titleAlpha.setDuration(600);
            subtitleAlpha.setDuration(600);
            titleAlpha.start();
            subtitleAlpha.setStartDelay(200);
            subtitleAlpha.start();
        }, 500);

        animateCard(todayCard, 1000);
        animateCard(monthlyCard, 1200);
        animateCard(customerCard, 1400);

        new Handler().postDelayed(() -> {
            ObjectAnimator sectionAlpha = ObjectAnimator.ofFloat(sectionTitle, "alpha", 0f, 1f);
            sectionAlpha.setDuration(500);
            sectionAlpha.start();
        }, 1600);

        animateReportButton(btnDailyReport, 1800);
        animateReportButton(btnMonthlyReport, 2000);
        animateReportButton(btnWithdrawReport, 2200);
        animateReportButton(btnCustomerReport, 2400);
    }

    private void animateCard(View card, long delay) {
        new Handler().postDelayed(() -> {
            ObjectAnimator translateY = ObjectAnimator.ofFloat(card, "translationY", 100f, 0f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f);
            translateY.setDuration(600);
            alpha.setDuration(600);
            translateY.setInterpolator(new OvershootInterpolator());
            AnimatorSet cardAnimator = new AnimatorSet();
            cardAnimator.playTogether(translateY, alpha);
            cardAnimator.start();
        }, delay);
    }

    private void animateReportButton(View button, long delay) {
        new Handler().postDelayed(() -> {
            ObjectAnimator translateX = ObjectAnimator.ofFloat(button, "translationX", -300f, 0f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(button, "alpha", 0f, 1f);
            translateX.setDuration(700);
            alpha.setDuration(700);
            translateX.setInterpolator(new AccelerateDecelerateInterpolator());
            AnimatorSet buttonAnimator = new AnimatorSet();
            buttonAnimator.playTogether(translateX, alpha);
            buttonAnimator.start();
        }, delay);
    }

    private void loadReports() {
        // 1. Fetch Today's & Monthly summaries from Local DB (for real-time/offline support)
        depositViewModel.getAllDeposits().observe(this, deposits -> {
            allDeposits = deposits != null ? deposits : new ArrayList<>();
            calculateSummaries();
        });
        
        withdrawViewModel.getAllWithdraws().observe(this, withdrawals -> {
            allWithdrawals = withdrawals != null ? withdrawals : new ArrayList<>();
            calculateSummaries();
        });

        // Still load customer count from Firebase (or could also use local DB)
        loadTotalCustomers();
    }
    
    private void calculateSummaries() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());
        
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        String monthPrefix = String.format(Locale.getDefault(), "%04d-%02d", year, month);
        
        double todayDeposits = 0;
        double todayWithdrawals = 0;
        double monthlyDeposits = 0;
        double monthlyWithdrawals = 0;
        
        // Process Deposits
        for (Deposit d : allDeposits) {
            String date = d.getDate();
            if (date != null) {
                if (date.equals(today)) {
                    todayDeposits += d.getAmount();
                }
                if (date.startsWith(monthPrefix)) {
                    monthlyDeposits += d.getAmount();
                }
            }
        }
        
        // Process Withdrawals
        for (Withdraw w : allWithdrawals) {
            String date = w.getWithdrawDate();
            if (date != null) {
                if (date.equals(today)) {
                    todayWithdrawals += w.getAmount();
                }
                if (date.startsWith(monthPrefix)) {
                    monthlyWithdrawals += w.getAmount();
                }
            }
        }
        
        // Update UI
        double todayNet = todayDeposits - todayWithdrawals;
        double monthlyNet = monthlyDeposits - monthlyWithdrawals;
        
        tvTodayCollection.setText(String.format(Locale.getDefault(), "₹%.2f", todayNet));
        tvMonthlyCollection.setText(String.format(Locale.getDefault(), "₹%.2f", monthlyNet));
        
        animateValueUpdate(todayCard);
        animateValueUpdate(monthlyCard);
    }

    private void loadTotalCustomers() {
        // Fetch total number of active customers from Firebase
        agentCustomersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                tvTotalCustomers.setText(String.valueOf(count));
                animateValueUpdate(customerCard);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvTotalCustomers.setText("0");
            }
        });
    }

    private void animateValueUpdate(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.05f, 1f);
        scaleX.setDuration(300);
        scaleY.setDuration(300);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        AnimatorSet pulseAnim = new AnimatorSet();
        pulseAnim.playTogether(scaleX, scaleY);
        pulseAnim.start();
    }
}
