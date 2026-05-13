package com.example.routewisecollection.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.routewisecollection.database.AppDatabase;
import com.example.routewisecollection.database.CustomerDao;
import com.example.routewisecollection.database.DepositDao;
import com.example.routewisecollection.database.RouteDao;
import com.example.routewisecollection.database.WithdrawDao;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DatabaseSyncManager ensures proper synchronization between Firebase and local app data
 * When data is deleted from Firebase, it automatically reflects in the app
 * When data is deleted from app, it automatically deletes from Firebase
 */
public class DatabaseSyncManager {
    
    private static final String TAG = "DatabaseSyncManager";
    private Context context;
    private String agentMobile;
    private CustomerDao customerDao;
    private DepositDao depositDao;
    private WithdrawDao withdrawDao;
    private RouteDao routeDao;
    private ExecutorService executorService;
    
    public DatabaseSyncManager(Context context, String agentMobile) {
        this.context = context;
        this.agentMobile = agentMobile;
        AppDatabase db = AppDatabase.getInstance(context);
        this.customerDao = db.customerDao();
        this.depositDao = db.depositDao();
        this.withdrawDao = db.withdrawDao();
        this.routeDao = db.routeDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Delete customer from Firebase and sync with app
     */
    public void deleteCustomer(String customerPhone, OnSyncCompleteListener listener) {
        DatabaseReference customerRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentMobile)
                .child("customers")
                .child(customerPhone);
        
        customerRef.removeValue()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Customer deleted from Firebase: " + customerPhone);
                    // Also delete from local Room database
                    executorService.execute(() -> {
                        customerDao.deleteByPhone(customerPhone);
                        if (listener != null) {
                            listener.onSuccess("Customer deleted successfully");
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete customer: " + e.getMessage());
                    if (listener != null) {
                        listener.onFailure("Failed to delete customer: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Delete transaction (Deposit) from Firebase and sync with app
     */
    public void deleteTransaction(String customerPhone, String transactionId, OnSyncCompleteListener listener) {
        DatabaseReference transactionRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentMobile)
                .child("transactions")
                .child(customerPhone)
                .child(transactionId);
        
        transactionRef.removeValue()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Transaction deleted from Firebase: " + transactionId);
                    // Also delete from local Room database
                    executorService.execute(() -> {
                        depositDao.deleteByTransactionId(transactionId);
                        if (listener != null) {
                            listener.onSuccess("Transaction deleted successfully");
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete transaction: " + e.getMessage());
                    if (listener != null) {
                        listener.onFailure("Failed to delete transaction: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Delete withdrawal from Firebase and sync with app
     */
    public void deleteWithdrawal(String customerPhone, String transactionId, OnSyncCompleteListener listener) {
        // Note: We use the transactionId we stored in the Withdraw model
        DatabaseReference withdrawalRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentMobile)
                .child("transactions") // Adjusting to the common transactions node for consistency
                .child(customerPhone)
                .child(transactionId);
        
        withdrawalRef.removeValue()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Withdrawal deleted from Firebase: " + transactionId);
                    // Also delete from local Room database
                    executorService.execute(() -> {
                        withdrawDao.deleteByTransactionId(transactionId);
                        if (listener != null) {
                            listener.onSuccess("Withdrawal deleted successfully");
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete withdrawal: " + e.getMessage());
                    if (listener != null) {
                        listener.onFailure("Failed to delete withdrawal: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Setup real-time listeners for automatic sync
     * This ensures that any changes in Firebase are immediately reflected in the app
     */
    public void setupRealTimeSync() {
        // Customer sync listener
        DatabaseReference customersRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentMobile)
                .child("customers");
        
        customersRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                String customerPhone = snapshot.getKey();
                if (customerPhone != null) {
                    executorService.execute(() -> {
                        // We use the unique combination of phone and agent check if needed, 
                        // but here we just delete from local DB.
                        customerDao.deleteByPhone(customerPhone);
                        Log.d(TAG, "Customer deleted from local DB due to Firebase sync: " + customerPhone);
                    });
                }
            }
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Customer sync listener cancelled: " + error.getMessage());
            }
        });
        
        // Transaction sync listener
        DatabaseReference transactionsRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentMobile)
                .child("transactions");
        
        transactionsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                // Nested listener for each customer's transactions
                setupTransactionChildListener(snapshot.getRef());
            }
            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                // If a customer's whole transaction list is removed
                String customerPhone = snapshot.getKey();
                if (customerPhone != null) {
                    executorService.execute(() -> {
                        // This would need a way to delete all transactions for a customer
                        // For now we assume individual transaction deletion is more common
                    });
                }
            }
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void setupTransactionChildListener(DatabaseReference ref) {
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                String transactionId = snapshot.getKey();
                if (transactionId != null) {
                    executorService.execute(() -> {
                        // Try deleting from both (one will exist)
                        depositDao.deleteByTransactionId(transactionId);
                        withdrawDao.deleteByTransactionId(transactionId);
                        Log.d(TAG, "Local transaction deleted due to Firebase sync: " + transactionId);
                    });
                }
            }
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
    
    /**
     * Restore all data from Firebase to local database
     * Useful for fresh installs or cleaning data
     */
    public void restoreDataFromFirebase(OnSyncCompleteListener listener) {
        Log.d(TAG, "Starting data restore from Firebase...");
        
        // 1. Restore Customers
        DatabaseReference customersRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentMobile)
                .child("customers");
                
        customersRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                executorService.execute(() -> {
                    try {
                        int count = 0;
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            com.example.routewisecollection.models.Customer c = snap.getValue(com.example.routewisecollection.models.Customer.class);
                            if (c != null) {
                                c.setAgentId(agentMobile); // Assign current agent ID
                                customerDao.insert(c);
                                count++;
                            }
                        }
                        Log.d(TAG, "Restored " + count + " customers");
                    } catch (Exception e) {
                        Log.e(TAG, "Error restoring customers: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
                Log.e(TAG, "Customer restore cancelled: " + error.getMessage());
            }
        });

        // 1.5. Restore Routes
        DatabaseReference routesRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentMobile)
                .child("agentInfo")
                .child("routes");

        routesRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                executorService.execute(() -> {
                    try {
                        int count = 0;
                        for (DataSnapshot snap : snapshot.getChildren()) {
                           String routeName = snap.child("name").getValue(String.class);
                           if (routeName != null) {
                               com.example.routewisecollection.models.Route r = new com.example.routewisecollection.models.Route();
                               r.setRouteName(routeName);
                               r.setFirebaseKey(snap.getKey());
                               r.setActive(true);
                               routeDao.insert(r);
                               count++;
                           }
                        }
                        Log.d(TAG, "Restored " + count + " routes");
                    } catch (Exception e) {
                        Log.e(TAG, "Error restoring routes: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
                Log.e(TAG, "Routes restore cancelled: " + error.getMessage());
            }
        });

        // 2. Restore Transactions (Deposits & Withdrawals)
        DatabaseReference transactionsRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentMobile)
                .child("transactions");

        transactionsRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                executorService.execute(() -> {
                    try {
                        int depositsCount = 0;
                        int withdrawalsCount = 0;

                        for (DataSnapshot customerSnaps : snapshot.getChildren()) {
                            for (DataSnapshot txnSnap : customerSnaps.getChildren()) {
                                String type = txnSnap.child("type").getValue(String.class);
                                
                                if ("deposit".equals(type)) {
                                    com.example.routewisecollection.models.Deposit d = new com.example.routewisecollection.models.Deposit();
                                    d.setTransactionId(txnSnap.child("transactionId").getValue(String.class) != null ? txnSnap.child("transactionId").getValue(String.class) : txnSnap.getKey());
                                    
                                    Double amount = txnSnap.child("amount").getValue(Double.class);
                                    d.setAmount(amount != null ? amount : 0.0);
                                    
                                    d.setAccountNumber(txnSnap.child("accountNumber").getValue(String.class));
                                    d.setCreatedAt(txnSnap.child("createdAt").getValue(String.class));
                                    d.setCustomerId(txnSnap.child("customerId").getValue(String.class));
                                    d.setCustomerName(txnSnap.child("customerName").getValue(String.class));
                                    d.setDate(txnSnap.child("date").getValue(String.class));
                                    d.setMode(txnSnap.child("paymentMethod").getValue(String.class));
                                    d.setReceiptNumber(txnSnap.child("receiptNumber").getValue(String.class));
                                    d.setRemarks(txnSnap.child("remarks").getValue(String.class));
                                    
                                    Long ts = txnSnap.child("timestamp").getValue(Long.class);
                                    d.setTimestamp(ts != null ? ts : 0L);
                                    
                                    d.setType(type);
                                    d.setOnlineMobileNumber(txnSnap.child("onlineMobileNumber").getValue(String.class));
                                    d.setSynced(true); // From Firebase, so it is synced
                                    
                                    depositDao.insert(d);
                                    depositsCount++;
                                    
                                } else if ("withdrawal".equals(type)) {
                                    com.example.routewisecollection.models.Withdraw w = new com.example.routewisecollection.models.Withdraw();
                                    w.setTransactionId(txnSnap.child("transactionId").getValue(String.class) != null ? txnSnap.child("transactionId").getValue(String.class) : txnSnap.getKey());
                                    
                                    Double amount = txnSnap.child("amount").getValue(Double.class);
                                    w.setAmount(amount != null ? amount : 0.0);
                                    
                                    w.setAccountNumber(txnSnap.child("accountNumber").getValue(String.class));
                                    // Withdraw model specific fields mapping
                                    // Note: Withdraw model might differ slightly, checking common fields
                                    w.setCustomerId(txnSnap.child("customerId").getValue(String.class));
                                    w.setCustomerName(txnSnap.child("customerName").getValue(String.class));
                                    w.setWithdrawDate(txnSnap.child("date").getValue(String.class));
                                    w.setWithdrawTime(txnSnap.child("time").getValue(String.class));
                                    w.setWithdrawMethod(txnSnap.child("paymentMethod").getValue(String.class));
                                    w.setReceiptNumber(txnSnap.child("receiptNumber").getValue(String.class));
                                    w.setNotes(txnSnap.child("remarks").getValue(String.class));
                                    
                                    Long ts = txnSnap.child("timestamp").getValue(Long.class);
                                    w.setTimestamp(ts != null ? ts : 0L);
                                    
                                    w.setOnlineMobileNumber(txnSnap.child("onlineMobileNumber").getValue(String.class));
                                    w.setSynced(true);
                                    
                                    withdrawDao.insert(w);
                                    withdrawalsCount++;
                                }
                            }
                        }
                        
                        Log.d(TAG, "Restored " + depositsCount + " deposits and " + withdrawalsCount + " withdrawals");
                        if (listener != null) {
                            listener.onSuccess("Data restored successfully from Server");
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error restoring transactions: " + e.getMessage());
                        if (listener != null) {
                            listener.onFailure("Restoration incomplete: " + e.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
                Log.e(TAG, "Transaction restore cancelled: " + error.getMessage());
                if (listener != null) {
                    listener.onFailure("Cancelled: " + error.getMessage());
                }
            }
        });
    }

    /**
     * Interface for sync completion callbacks
     */
    public interface OnSyncCompleteListener {
        void onSuccess(String message);
        void onFailure(String error);
    }
}