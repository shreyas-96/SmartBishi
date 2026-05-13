package com.example.routewisecollection.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "withdrawals")
public class Withdraw {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "transactionId")
    private String transactionId; // Firebase transaction key

    @ColumnInfo(name = "customerId")
    private String customerId;

    @ColumnInfo(name = "accountNumber")
    private String accountNumber;

    @ColumnInfo(name = "amount")
    private double amount;

    @ColumnInfo(name = "withdrawDate")
    private String withdrawDate;

    @ColumnInfo(name = "withdrawTime")
    private String withdrawTime;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "receiptNumber")
    private String receiptNumber;

    @ColumnInfo(name = "notes")
    private String notes;

    @ColumnInfo(name = "customerName")
    private String customerName;

    @ColumnInfo(name = "withdrawMethod")
    private String withdrawMethod;

    @ColumnInfo(name = "onlineMobileNumber")
    private String onlineMobileNumber;

    @ColumnInfo(name = "isSynced")
    private boolean isSynced; // Flag to track if synced to Firebase

    // Default constructor for Room
    public Withdraw() {}

    // Full constructor
    public Withdraw(String customerId, String accountNumber, double amount, String withdrawDate,
                   String withdrawTime, String receiptNumber, String notes, 
                   String customerName, String withdrawMethod, String onlineMobileNumber) {
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.withdrawDate = withdrawDate;
        this.withdrawTime = withdrawTime;
        this.receiptNumber = receiptNumber;
        this.notes = notes;
        this.customerName = customerName;
        this.withdrawMethod = withdrawMethod;
        this.onlineMobileNumber = onlineMobileNumber;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCustomerId() {
        return customerId;
    }
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

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

    public String getWithdrawDate() {
        return withdrawDate;
    }
    public void setWithdrawDate(String withdrawDate) {
        this.withdrawDate = withdrawDate;
    }

    public String getWithdrawTime() {
        return withdrawTime;
    }
    public void setWithdrawTime(String withdrawTime) {
        this.withdrawTime = withdrawTime;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }
    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCustomerName() {
        return customerName;
    }
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getWithdrawMethod() {
        return withdrawMethod;
    }
    public void setWithdrawMethod(String withdrawMethod) {
        this.withdrawMethod = withdrawMethod;
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
