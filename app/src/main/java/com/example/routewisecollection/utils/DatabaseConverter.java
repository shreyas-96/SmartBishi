package com.example.routewisecollection.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DatabaseConverter {
    
    private static final String TAG = "DatabaseConverter";
    
    /**
     * Convert existing database to new structure
     * Call this method once to convert all your data
     */
    public static void convertToNewStructure(String agentMobile) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        DatabaseReference oldTransactionsRef = database.child("agents").child(agentMobile).child("transactions");
        
        oldTransactionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Starting conversion for agent: " + agentMobile);
                
                for (DataSnapshot customerSnapshot : snapshot.getChildren()) {
                    String customerPhone = customerSnapshot.getKey();
                    
                    for (DataSnapshot transactionSnapshot : customerSnapshot.getChildren()) {
                        Map<String, Object> oldData = (Map<String, Object>) transactionSnapshot.getValue();
                        
                        if (oldData != null) {
                            // Convert old structure to new structure
                            Map<String, Object> newData = convertSingleRecord(oldData, customerPhone);
                            
                            if (newData != null) {
                                // Replace old data with new structure
                                transactionSnapshot.getRef().setValue(newData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Converted: " + transactionSnapshot.getKey());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to convert: " + e.getMessage());
                                    });
                            }
                        }
                    }
                }
                
                Log.d(TAG, "Conversion completed for agent: " + agentMobile);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Conversion failed: " + error.getMessage());
            }
        });
    }
    
    /**
     * Convert single record from old to new structure
     */
    private static Map<String, Object> convertSingleRecord(Map<String, Object> oldData, String customerPhone) {
        try {
            Map<String, Object> newData = new HashMap<>();
            
            // Map old fields to new structure
            newData.put("accountNumber", oldData.get("accountNumber") != null ? oldData.get("accountNumber") : "");
            newData.put("amount", oldData.get("amount") != null ? oldData.get("amount") : 0);
            
            // Generate createdAt from current time or existing time
            long timestamp = System.currentTimeMillis();
            if (oldData.get("time") != null) {
                // Try to use existing time if available
                String oldTime = (String) oldData.get("time");
                String oldDate = (String) oldData.get("date");
                // For simplicity, using current timestamp
                // You can implement proper date/time parsing if needed
            }
            
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            newData.put("createdAt", isoFormat.format(new Date(timestamp)));
            
            // Use phone as customerId
            newData.put("customerId", customerPhone);
            newData.put("customerName", oldData.get("customerName") != null ? oldData.get("customerName") : "");
            newData.put("date", oldData.get("date") != null ? oldData.get("date") : "");
            
            // Convert mode from old paymentMethod
            String oldPaymentMethod = (String) oldData.get("paymentMethod");
            String newMode = "cash"; // default
            if (oldPaymentMethod != null && !oldPaymentMethod.isEmpty()) {
                if (oldPaymentMethod.toLowerCase().contains("online") || 
                    oldPaymentMethod.toLowerCase().contains("digital") ||
                    oldPaymentMethod.toLowerCase().contains("upi")) {
                    newMode = "online";
                }
            }
            newData.put("mode", newMode);
            
            newData.put("receiptNumber", oldData.get("receiptNumber") != null ? oldData.get("receiptNumber") : "");
            
            // Convert notes to remarks
            newData.put("remarks", oldData.get("notes") != null ? oldData.get("notes") : "");
            
            newData.put("timestamp", timestamp);
            
            // Generate transactionId
            String transactionId = "TXN" + timestamp + "_" + customerPhone.substring(Math.max(0, customerPhone.length() - 4));
            newData.put("transactionId", transactionId);
            
            newData.put("type", "deposit");
            
            return newData;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting record: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create sample data with new structure for testing
     */
    public static void createSampleNewStructureData(String agentMobile) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        DatabaseReference transactionsRef = database.child("agents").child(agentMobile).child("transactions");
        
        // Sample data
        String customerPhone = "9876543210";
        String customerName = "Prachi Patil";
        String accountNumber = "ACC001";
        double amount = 2000;
        String mode = "cash";
        
        long timestamp = System.currentTimeMillis();
        String transactionId = "TXN" + timestamp + "_3210";
        
        Map<String, Object> sampleData = new HashMap<>();
        sampleData.put("accountNumber", accountNumber);
        sampleData.put("amount", amount);
        
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        sampleData.put("createdAt", isoFormat.format(new Date(timestamp)));
        
        sampleData.put("customerId", customerPhone);
        sampleData.put("customerName", customerName);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sampleData.put("date", dateFormat.format(new Date()));
        
        sampleData.put("mode", mode);
        sampleData.put("receiptNumber", "RCP" + timestamp);
        sampleData.put("remarks", "");
        sampleData.put("timestamp", timestamp);
        sampleData.put("transactionId", transactionId);
        sampleData.put("type", "deposit");
        
        // Save sample data
        transactionsRef.child(customerPhone).child(transactionId).setValue(sampleData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Sample data created with new structure");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to create sample data: " + e.getMessage());
            });
    }
}