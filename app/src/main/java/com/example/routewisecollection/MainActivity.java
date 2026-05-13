package com.example.routewisecollection;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.routewisecollection.activities.LoginActivity;
import com.example.routewisecollection.fragments.CollectFragment;
import com.example.routewisecollection.fragments.HomeFragment;
import com.example.routewisecollection.fragments.ProfileFragment;
import com.example.routewisecollection.fragments.ReportsFragment;
import com.example.routewisecollection.utils.LoginManager;
import com.example.routewisecollection.utils.DatabaseSyncManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginManager = new LoginManager(this);

        if (!loginManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        // Trigger restore if database was wiped for version 10
        checkMigrationSync();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        setupBottomNavigation();
        setupDatabaseSync();
        syncOfflineData();
        setupBackNavigation();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (bottomNavigationView.getSelectedItemId() != R.id.nav_home) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                } else {
                    showExitDialog();
                }
            }
        });
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                int id = item.getItemId();
                
                if (id == R.id.nav_home) {
                    fragment = new HomeFragment();
                } else if (id == R.id.nav_deposit) {
                    fragment = new CollectFragment();
                } else if (id == R.id.nav_reports) {
                    fragment = new ReportsFragment();
                } else if (id == R.id.nav_settings) {
                    fragment = new ProfileFragment();
                }

                if (fragment != null) {
                    loadFragment(fragment);
                    return true;
                }
                return false;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void setupDatabaseSync() {
        DatabaseSyncManager syncManager = new DatabaseSyncManager(this, loginManager.getAgentMobile());
        syncManager.setupRealTimeSync();
    }

    private void syncOfflineData() {
        if (!com.example.routewisecollection.utils.NetworkUtils.isNetworkAvailable(this)) {
            return;
        }
        com.example.routewisecollection.services.SyncService syncService = 
            new com.example.routewisecollection.services.SyncService(this);
        syncService.syncAll(new com.example.routewisecollection.services.SyncService.SyncCallback() {
            @Override
            public void onSyncComplete() {}
            @Override
            public void onSyncFailed(String error) {}
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void checkMigrationSync() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isSynced = prefs.getBoolean("migration_v10_synced", false);
        
        if (!isSynced) {
            String mobile = loginManager.getAgentMobile();
            if (mobile != null && !mobile.isEmpty()) {
                DatabaseSyncManager syncManager = new DatabaseSyncManager(this, mobile);
                syncManager.restoreDataFromFirebase(new DatabaseSyncManager.OnSyncCompleteListener() {
                    @Override
                    public void onSuccess(String message) {
                        prefs.edit().putBoolean("migration_v10_synced", true).apply();
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Data Restored Successfully", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onFailure(String error) {
                        // We'll try again next time if it fails
                    }
                });
            }
        }
    }

    private void applyTheme() {
        android.content.SharedPreferences prefs = getSharedPreferences(
            com.example.routewisecollection.utils.Constants.PREFS_NAME, 
            MODE_PRIVATE
        );
        int themeMode = prefs.getInt(
            com.example.routewisecollection.utils.Constants.PREF_THEME_MODE, 
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }
}