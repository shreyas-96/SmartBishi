package com.example.routewisecollection.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "deposits")
public class Deposit {

    @ColumnInfo(name = "accountNumber")
    private String accountNumber;

    @ColumnInfo(name = "amount")
    private double amount;

    @ColumnInfo(name = "createdAt")
    private String createdAt;

    @ColumnInfo(name = "customerId")
    private String customerId;

    @ColumnInfo(name = "customerName")
    private String customerName;

    @ColumnInfo(name = "date")
    private String date;

    @ColumnInfo(name = "mode")
    private String mode;

    @ColumnInfo(name = "receiptNumber")
    private String receiptNumber;

    @ColumnInfo(name = "remarks")
    private String remarks;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "transactionId")
    private String transactionId;

    @ColumnInfo(name = "type")
    private String type;

    @ColumnInfo(name = "onlineMobileNumber")
    private String onlineMobileNumber;

    @ColumnInfo(name = "isSynced")
    private boolean isSynced; // Flag to track if synced to Firebase

    // Default constructor for Room
    public Deposit() {}
    
    public Deposit(boolean isSynced) {
        this.isSynced = isSynced;
    }

    // Full constructor for new structure
    public Deposit(String accountNumber, double amount, String createdAt, String customerId,
                   String customerName, String date, String mode, String receiptNumber,
                   String remarks, long timestamp, @NonNull String transactionId, String type, String onlineMobileNumber) {
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.createdAt = createdAt;
        this.customerId = customerId;
        this.customerName = customerName;
        this.date = date;
        this.mode = mode;
        this.receiptNumber = receiptNumber;
        this.remarks = remarks;
        this.timestamp = timestamp;
        this.transactionId = transactionId;
        this.type = type;
        this.onlineMobileNumber = onlineMobileNumber;
    }

    // Getters and Setters for new structure
    public String getAccountNumber() {
        return accountNumber;
    }
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCustomerId() {
        return customerId;
    }
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }

    public String getMode() {
        return mode;
    }
    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }
    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getRemarks() {
        return remarks;
    }
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @NonNull
    public String getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(@NonNull String transactionId) {
        this.transactionId = transactionId;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getOnlineMobileNumber() {
        return onlineMobileNumber;
    }

    public void setOnlineMobileNumber(String onlineMobileNumber) {
        this.onlineMobileNumber = onlineMobileNumber;
    }

    public boolean isSynced() {
        return isSynced;
    }
    public void setSynced(boolean synced) {
        this.isSynced = synced;
    }
}
