package com.example.routewisecollection.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.routewisecollection.database.AppDatabase;
import com.example.routewisecollection.database.DepositDao;
import com.example.routewisecollection.database.WithdrawDao;
import com.example.routewisecollection.models.Deposit;
import com.example.routewisecollection.models.Withdraw;
import com.example.routewisecollection.utils.LoginManager;
import com.example.routewisecollection.utils.NetworkUtils;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service to sync local database with Firebase when internet is available
 */
public class SyncService {

    private static final String TAG = "SyncService";
    private final Context context;
    private final ExecutorService executorService;
    private final DepositDao depositDao;
    private final WithdrawDao withdrawDao;
    private final String agentMobile;
    private final DatabaseReference transactionsRef;

    public SyncService(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        
        AppDatabase database = AppDatabase.getInstance(context);
        this.depositDao = database.depositDao();
        this.withdrawDao = database.withdrawDao();
        
        LoginManager loginManager = new LoginManager(context);
        this.agentMobile = loginManager.getAgentMobile();
        
        this.transactionsRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentMobile)
                .child("transactions");
    }

    /**
     * Sync all unsynced data to Firebase
     */
    public void syncAll(SyncCallback callback) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "No network available. Skipping sync.");
            if (callback != null) {
                callback.onSyncFailed("No network available");
            }
            return;
        }

        executorService.execute(() -> {
            try {
                // Sync deposits
                syncDeposits();
                
                // Sync withdrawals
                syncWithdrawals();
                
                if (callback != null) {
                    callback.onSyncComplete();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during sync: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onSyncFailed(e.getMessage());
                }
            }
        });
    }

    /**
     * Sync unsynced deposits to Firebase
     */
    private void syncDeposits() {
        List<Deposit> unsyncedDeposits = depositDao.getUnsyncedDeposits();
        
        if (unsyncedDeposits == null || unsyncedDeposits.isEmpty()) {
            Log.d(TAG, "No unsynced deposits to sync");
            return;
        }

        Log.d(TAG, "Syncing " + unsyncedDeposits.size() + " deposits to Firebase");

        for (Deposit deposit : unsyncedDeposits) {
            try {
                // Create deposit data map
                Map<String, Object> depositData = new HashMap<>();
                depositData.put("accountNumber", deposit.getAccountNumber() != null ? deposit.getAccountNumber() : "");
                depositData.put("amount", deposit.getAmount());
                depositData.put("createdAt", deposit.getCreatedAt() != null ? deposit.getCreatedAt() : "");
                depositData.put("customerId", deposit.getCustomerId() != null ? deposit.getCustomerId() : "");
                depositData.put("customerName", deposit.getCustomerName() != null ? deposit.getCustomerName() : "");
                depositData.put("date", deposit.getDate() != null ? deposit.getDate() : "");
                depositData.put("paymentMethod", deposit.getMode() != null ? deposit.getMode() : "cash");
                depositData.put("receiptNumber", deposit.getReceiptNumber() != null ? deposit.getReceiptNumber() : "");
                depositData.put("remarks", deposit.getRemarks() != null ? deposit.getRemarks() : "");
                depositData.put("timestamp", deposit.getTimestamp());
                depositData.put("transactionId", deposit.getTransactionId());
                depositData.put("type", "deposit");

                // Get the specific transaction path
                DatabaseReference depositRef = transactionsRef
                        .child(deposit.getCustomerId())
                        .child(deposit.getTransactionId());

                // Save to Firebase synchronously (blocking call)
                depositRef.setValue(depositData).addOnSuccessListener(aVoid -> {
                    // Mark as synced in local database
                    executorService.execute(() -> {
                        depositDao.markAsSynced(deposit.getTransactionId());
                        Log.d(TAG, "Deposit synced: " + deposit.getTransactionId());
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync deposit: " + deposit.getTransactionId(), e);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error syncing deposit: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Sync unsynced withdrawals to Firebase
     */
    private void syncWithdrawals() {
        List<Withdraw> unsyncedWithdraws = withdrawDao.getUnsyncedWithdraws();
        
        if (unsyncedWithdraws == null || unsyncedWithdraws.isEmpty()) {
            Log.d(TAG, "No unsynced withdrawals to sync");
            return;
        }

        Log.d(TAG, "Syncing " + unsyncedWithdraws.size() + " withdrawals to Firebase");

        for (Withdraw withdraw : unsyncedWithdraws) {
            try {
                // Generate transactionId for withdraw (since it doesn't have one)
                String transactionId = "WDRW" + System.currentTimeMillis() + "_" + withdraw.getId();
                
                // Calculate timestamp
                long timestamp = System.currentTimeMillis();
                
                // Create ISO format createdAt
                String createdAt = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 
                    java.util.Locale.getDefault()
                ).format(new java.util.Date(timestamp));

                // Create withdrawal data map
                Map<String, Object> withdrawalData = new HashMap<>();
                withdrawalData.put("accountNumber", withdraw.getAccountNumber() != null ? withdraw.getAccountNumber() : "");
                withdrawalData.put("amount", withdraw.getAmount());
                withdrawalData.put("createdAt", createdAt);
                withdrawalData.put("customerId", withdraw.getCustomerId()); // Already a string (phone number)
                withdrawalData.put("customerName", withdraw.getCustomerName() != null ? withdraw.getCustomerName() : "");
                withdrawalData.put("date", withdraw.getWithdrawDate() != null ? withdraw.getWithdrawDate() : "");
                withdrawalData.put("time", withdraw.getWithdrawTime() != null ? withdraw.getWithdrawTime() : "");
                withdrawalData.put("paymentMethod", withdraw.getWithdrawMethod() != null ? withdraw.getWithdrawMethod() : "CASH");
                withdrawalData.put("receiptNumber", withdraw.getReceiptNumber() != null ? withdraw.getReceiptNumber() : "");
                withdrawalData.put("remarks", withdraw.getNotes() != null ? withdraw.getNotes() : "");
                withdrawalData.put("timestamp", timestamp);
                withdrawalData.put("transactionId", transactionId);
                withdrawalData.put("type", "withdrawal");

                // Note: We're assuming customerId in Withdraw model is the customer phone number
                // If it's not, you may need to look up the customer to get their phone number
                // For now, we'll use the customerId directly
                
                DatabaseReference withdrawRef = transactionsRef
                        .child(withdraw.getCustomerId())
                        .child(transactionId);

                // Save to Firebase
                withdrawRef.setValue(withdrawalData).addOnSuccessListener(aVoid -> {
                    // Mark as synced in local database
                    executorService.execute(() -> {
                        withdrawDao.markAsSynced(withdraw.getId());
                        Log.d(TAG, "Withdrawal synced: " + withdraw.getId());
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync withdrawal: " + withdraw.getId(), e);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error syncing withdrawal: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Callback interface for sync operations
     */
    public interface SyncCallback {
        void onSyncComplete();
        void onSyncFailed(String error);
    }
}
