package com.example.routewisecollection.activities;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.routewisecollection.utils.Constants;

/**
 * Base Activity class that all activities should extend
 * Ensures theme is properly applied and edge-to-edge display works on all devices
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate()
        applyTheme();
        
        // Enable edge-to-edge display
        enableEdgeToEdge();
        
        super.onCreate(savedInstanceState);
    }

    /**
     * Apply saved theme preference
     */
    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        int themeMode = prefs.getInt(Constants.PREF_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    /**
     * Enable edge-to-edge display for all devices (notch, punch-hole, etc.)
     */
    private void enableEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            // Android 10 and below
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        }
        
        // Make status bar and navigation bar transparent
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
    }

    /**
     * Apply window insets to a view (call this for root layout)
     */
    protected void applyWindowInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            
            v.setPadding(
                v.getPaddingLeft(),
                topInset,
                v.getPaddingRight(),
                bottomInset
            );
            
            return insets;
        });
    }

    /**
     * Get current theme mode
     */
    protected int getCurrentThemeMode() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(Constants.PREF_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}
