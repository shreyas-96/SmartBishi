package com.example.routewisecollection.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.routewisecollection.services.SyncService;
import com.example.routewisecollection.utils.NetworkUtils;

/**
 * BroadcastReceiver to listen for network connectivity changes
 * and trigger sync when internet becomes available
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkChangeReceiver";
    private static boolean wasOffline = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) {
            return;
        }

        // Check if network is now available
        boolean isOnline = NetworkUtils.isNetworkAvailable(context);
        
        Log.d(TAG, "Network connectivity changed. Online: " + isOnline + ", Was offline: " + wasOffline);

        // If we just came back online after being offline, trigger sync
        if (isOnline && wasOffline) {
            Log.i(TAG, "✅ Internet connection restored! Starting automatic sync...");
            
            // Small delay to ensure connection is stable
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                triggerSync(context);
            }, 2000); // 2 second delay
            
            wasOffline = false;
        } else if (!isOnline) {
            Log.w(TAG, "❌ Internet connection lost. Going offline...");
            wasOffline = true;
        }
    }
    
    /**
     * Trigger sync service to upload unsynced data
     */
    private void triggerSync(Context context) {
        try {
            SyncService syncService = new SyncService(context);
            syncService.syncAll(new SyncService.SyncCallback() {
                @Override
                public void onSyncComplete() {
                    Log.i(TAG, "✅ Automatic sync completed successfully!");
                    
                    // Show toast notification on UI thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(context, "✅ Offline data synced to server", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onSyncFailed(String error) {
                    Log.e(TAG, "❌ Automatic sync failed: " + error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error triggering sync: " + e.getMessage(), e);
        }
    }
}
