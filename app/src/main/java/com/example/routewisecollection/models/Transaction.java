package com.example.routewisecollection.models;

public class Transaction {
    public enum Type {
        DEPOSIT, WITHDRAW
    }

    private String transactionId;
    private String customerId;
    private String accountNumber;
    private String customerName;
    private double amount;
    private String date;
    private long timestamp;
    private String receiptNumber;
    private String remarks;
    private String mode;
    private Type type;
    private String time;
    private String typeDisplay;
    private String onlineMobileNumber;


    public Transaction() {}

    // Constructor for Deposit
    public static Transaction fromDeposit(Deposit deposit) {
        Transaction transaction = new Transaction();
        transaction.transactionId = deposit.getTransactionId();
        transaction.customerId = deposit.getCustomerId();
        transaction.accountNumber = deposit.getAccountNumber();
        transaction.customerName = deposit.getCustomerName();
        transaction.amount = deposit.getAmount();
        transaction.date = deposit.getDate();
        transaction.timestamp = deposit.getTimestamp();
        transaction.receiptNumber = deposit.getReceiptNumber();
        transaction.remarks = deposit.getRemarks();
        transaction.mode = deposit.getMode();
        transaction.onlineMobileNumber = deposit.getOnlineMobileNumber();
        transaction.type = Type.DEPOSIT;
        return transaction;
    }

    // Constructor for Withdraw
    public static Transaction fromWithdraw(Withdraw withdraw) {
        Transaction transaction = new Transaction();
        transaction.transactionId = withdraw.getTransactionId() != null ? withdraw.getTransactionId() : String.valueOf(withdraw.getId());
        transaction.customerId = withdraw.getCustomerId();
        transaction.accountNumber = withdraw.getAccountNumber();
        transaction.customerName = withdraw.getCustomerName();
        transaction.amount = withdraw.getAmount();
        transaction.date = withdraw.getWithdrawDate();
        transaction.timestamp = withdraw.getTimestamp();
        transaction.receiptNumber = withdraw.getReceiptNumber();
        transaction.remarks = withdraw.getNotes();
        transaction.mode = withdraw.getWithdrawMethod();
        transaction.onlineMobileNumber = withdraw.getOnlineMobileNumber();
        transaction.type = Type.WITHDRAW;
        return transaction;
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    
    public void setType(String typeString) {
        if ("deposit".equals(typeString)) {
            this.type = Type.DEPOSIT;
        } else if ("withdrawal".equals(typeString)) {
            this.type = Type.WITHDRAW;
        }
    }
    
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    
    public String getTypeDisplay() { return typeDisplay; }
    public void setTypeDisplay(String typeDisplay) { this.typeDisplay = typeDisplay; }

    public String getOnlineMobileNumber() { return onlineMobileNumber; }
    public void setOnlineMobileNumber(String onlineMobileNumber) { this.onlineMobileNumber = onlineMobileNumber; }



    // Helper methods
    public boolean isDeposit() { return type == Type.DEPOSIT; }
    public boolean isWithdraw() { return type == Type.WITHDRAW; }
    
    public double getNetAmount() {
        return isDeposit() ? amount : -amount;
    }
    
    public String getAmountDisplay() {
        return isDeposit() ? "+" + String.format("%.2f", amount) : "-" + String.format("%.2f", amount);
    }

    // Backward compatibility method to get time from timestamp
    public String getTimeFromTimestamp() {
        if (timestamp > 0) {
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
            return timeFormat.format(new java.util.Date(timestamp));
        }
        return "00:00:00";
    }
}
