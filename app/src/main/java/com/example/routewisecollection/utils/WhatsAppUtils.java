package com.example.routewisecollection.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Utility class for sending WhatsApp messages via webhook API
 */
public class WhatsAppUtils {
    
    private static final String TAG = "WhatsAppUtils";
    private static final String WEBHOOK_BASE_URL = "https://webhook.whatapi.in/webhook/69213b981b9845c02d533ccb";
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final OkHttpClient httpClient = new OkHttpClient();

    /**
     * Send deposit confirmation message via WhatsApp
     * 
     * @param context Application context
     * @param phoneNumber Customer phone number (with country code)
     * @param customerName Customer name
     * @param depositAmount Deposit amount
     * @param accountNumber Account number
     * @param totalAmount Total amount (balance after deposit)
     * @param agentName Agent/Company name
     */
    public static void sendDepositMessage(Context context, String phoneNumber, String customerName,
                                         double depositAmount, String accountNumber,
                                         double totalAmount, String agentName) {

        executorService.execute(() -> {
            try {
                // Format the message with all required variables
                @SuppressLint("DefaultLocale") String message = String.format(
                    "bhishi,%s,%.2f,credit,%s,%.2f,%s",
                    customerName,
                    depositAmount,
                    accountNumber,
                    totalAmount,
                    agentName
                );
                
                // URL encode the message
                String encodedMessage = URLEncoder.encode(message, "UTF-8");
                
                // Build the complete URL
                String url = String.format("%s?number=%s&message=%s", 
                    WEBHOOK_BASE_URL, 
                    phoneNumber, 
                    encodedMessage);
                
                // Make the HTTP request
                Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
                
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    Log.d(TAG, "WhatsApp deposit message sent successfully to " + phoneNumber);
                } else {
                    Log.e(TAG, "Failed to send WhatsApp deposit message. Response code: " + response.code());
                }
                
                response.close();
                
            } catch (IOException e) {
                Log.e(TAG, "Error sending WhatsApp deposit message: " + e.getMessage(), e);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error sending WhatsApp deposit message: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Send withdrawal/credit confirmation message via WhatsApp
     * 
     * @param context Application context
     * @param phoneNumber Customer phone number (with country code)
     * @param customerName Customer name
     * @param withdrawalAmount Withdrawal amount (after penalty)
     * @param accountNumber Account number
     * @param totalAmount Total amount (balance after withdrawal)
     * @param agentName Agent/Company name
     */
    public static void sendWithdrawalMessage(Context context, String phoneNumber, String customerName, 
                                            double withdrawalAmount, String accountNumber, 
                                            double totalAmount, String agentName) {
        
        executorService.execute(() -> {
            try {
                // Format the message with all required variables
                @SuppressLint("DefaultLocale") String message = String.format(
                    "bhishi,%s,%.2f,withdraw,%s,%.2f,%s",
                    customerName,
                    withdrawalAmount,
                    accountNumber,
                    totalAmount,
                    agentName
                );
                
                // URL encode the message
                String encodedMessage = URLEncoder.encode(message, "UTF-8");
                
                // Build the complete URL
                String url = String.format("%s?number=%s&message=%s", 
                    WEBHOOK_BASE_URL, 
                    phoneNumber, 
                    encodedMessage);
                
                // Make the HTTP request
                Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
                
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    Log.d(TAG, "WhatsApp withdrawal message sent successfully to " + phoneNumber);
                } else {
                    Log.e(TAG, "Failed to send WhatsApp withdrawal message. Response code: " + response.code());
                }
                
                response.close();
                
            } catch (IOException e) {
                Log.e(TAG, "Error sending WhatsApp withdrawal message: " + e.getMessage(), e);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error sending WhatsApp withdrawal message: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Send a custom WhatsApp message
     * 
     * @param context Application context
     * @param phoneNumber Customer phone number (with country code)
     * @param message Custom message to send
     * @param callback Optional callback for success/failure
     */
    public static void sendCustomMessage(Context context, String phoneNumber, String message, 
                                        final WhatsAppCallback callback) {
        
        executorService.execute(() -> {
            try {
                // URL encode the message
                String encodedMessage = URLEncoder.encode(message, "UTF-8");
                
                // Build the complete URL
                String url = String.format("%s?number=%s&message=%s", 
                    WEBHOOK_BASE_URL, 
                    phoneNumber, 
                    encodedMessage);
                
                // Make the HTTP request
                Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
                
                Response response = httpClient.newCall(request).execute();
                
                final boolean success = response.isSuccessful();
                final int responseCode = response.code();
                
                response.close();
                
                // Callback on main thread
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (success) {
                            callback.onSuccess();
                        } else {
                            callback.onFailure("HTTP error code: " + responseCode);
                        }
                    });
                }
                
                if (success) {
                    Log.d(TAG, "WhatsApp custom message sent successfully to " + phoneNumber);
                } else {
                    Log.e(TAG, "Failed to send WhatsApp custom message. Response code: " + responseCode);
                }
                
            } catch (IOException e) {
                Log.e(TAG, "Error sending WhatsApp custom message: " + e.getMessage(), e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        callback.onFailure("Network error: " + e.getMessage())
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error sending WhatsApp custom message: " + e.getMessage(), e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        callback.onFailure("Error: " + e.getMessage())
                    );
                }
            }
        });
    }
    
    /**
     * Callback interface for WhatsApp message sending
     */
    public interface WhatsAppCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
