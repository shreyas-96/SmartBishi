package com.example.routewisecollection.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SmsUtils {
    
    /**
     * Check if SMS permission is granted
     * @param context Application context
     * @return true if permission granted, false otherwise
     */
    public static boolean isSmsPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Request SMS permission
     * @param activity Activity instance
     */
    public static void requestSmsPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, 
                new String[]{Manifest.permission.SEND_SMS}, 
                Constants.PERMISSION_SEND_SMS);
    }
    
    /**
     * Send SMS to customer
     * @param context Application context
     * @param phoneNumber Customer phone number
     * @param message SMS message
     * @return true if SMS sent successfully, false otherwise
     */
    public static boolean sendSms(Context context, String phoneNumber, String message) {
        if (!isSmsPermissionGranted(context)) {
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> 
                    Toast.makeText(context, "SMS permission not granted", Toast.LENGTH_SHORT).show()
                );
            }
            return false;
        }
        
        try {
            SmsManager smsManager = SmsManager.getDefault();
            
            // If message is longer than 160 characters, split it
            if (message.length() > 160) {
                java.util.ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            }
            
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> 
                    Toast.makeText(context, "SMS sent successfully to " + phoneNumber, Toast.LENGTH_SHORT).show()
                );
            }
            return true;
        } catch (Exception e) {
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> 
                    Toast.makeText(context, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Generate deposit receipt SMS message
     * @param customerName Customer name
     * @param amount Deposit amount
     * @param receiptNumber Receipt number
     * @param companyName Company name
     * @return SMS message
     */
    public static String generateDepositSms(String customerName, double amount, 
                                           String receiptNumber, String companyName) {
        return String.format("Dear %s,\n\nYour deposit of Rs. %.2f has been received.\n" +
                        "Receipt No: %s\n\nThank you!\n%s", 
                customerName, amount, receiptNumber, companyName);
    }
    
    /**
     * Generate payment reminder SMS
     * @param customerName Customer name
     * @param dueAmount Due amount
     * @param companyName Company name
     * @return SMS message
     */
    public static String generateReminderSms(String customerName, double dueAmount, String companyName) {
        return String.format("Dear %s,\n\nThis is a reminder for your Total payment of Rs. %.2f.\n" +
                        "Please make the payment at your earliest convenience.\n\nThank you!\n%s", 
                customerName, dueAmount, companyName);
    }
}
