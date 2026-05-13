package com.example.routewisecollection;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.routewisecollection.utils.Constants;

/**
 * Application class to initialize theme globally
 * This ensures theme is applied before any activity starts
 */
public class SmartBishiApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Apply saved theme globally
        applyTheme();
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        int themeMode = prefs.getInt(Constants.PREF_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        // Apply theme globally for entire app
        AppCompatDelegate.setDefaultNightMode(themeMode);
        
        android.util.Log.d("SmartBishiApp", "Theme applied: " + getThemeName(themeMode));
    }

    private String getThemeName(int themeMode) {
        switch (themeMode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                return "Light Mode";
            case AppCompatDelegate.MODE_NIGHT_YES:
                return "Dark Mode";
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                return "Follow System";
            default:
                return "Unknown";
        }
    }
}
