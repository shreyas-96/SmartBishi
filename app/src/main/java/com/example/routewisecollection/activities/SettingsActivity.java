package com.example.routewisecollection.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.routewisecollection.R;
import com.example.routewisecollection.utils.Constants;

public class SettingsActivity extends AppCompatActivity {

    private EditText etCompanyName, etCompanyAddress, etCompanyPhone;
    private CheckBox cbSmsEnabled, cbPrintEnabled;
    private RadioGroup radioGroupTheme;
    private Button btnSaveSettings;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();
        loadSettings();
        setupClickListeners();

        // Animate header and setting cards for a modern feel
        View header = findViewById(R.id.headerSettings);
        View cardCompany = findViewById(R.id.cardCompany);
        View cardDefaults = findViewById(R.id.cardDefaults);
        View cardFeatures = findViewById(R.id.cardFeatures);
        View cardTheme = findViewById(R.id.cardTheme);
        View btnSave = findViewById(R.id.btnSaveSettings);

        animateIn(header, 0);
        animateIn(cardCompany, 80);
        animateIn(cardDefaults, 140);
        animateIn(cardFeatures, 200);
        animateIn(cardTheme, 260);
        animateIn(btnSave, 320);
    }

    private void initializeViews() {
        etCompanyName = findViewById(R.id.etCompanyName);
        etCompanyAddress = findViewById(R.id.etCompanyAddress);
        etCompanyPhone = findViewById(R.id.etCompanyPhone);
        cbSmsEnabled = findViewById(R.id.cbSmsEnabled);
        cbPrintEnabled = findViewById(R.id.cbPrintEnabled);
        radioGroupTheme = findViewById(R.id.radioGroupTheme);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
    }

    private void loadSettings() {
        etCompanyName.setText(sharedPreferences.getString(Constants.PREF_COMPANY_NAME, Constants.DEFAULT_COMPANY_NAME));
        etCompanyAddress.setText(sharedPreferences.getString(Constants.PREF_COMPANY_ADDRESS, ""));
        etCompanyPhone.setText(sharedPreferences.getString(Constants.PREF_COMPANY_PHONE, ""));
        cbSmsEnabled.setChecked(sharedPreferences.getBoolean(Constants.PREF_SMS_ENABLED, false));
        cbPrintEnabled.setChecked(sharedPreferences.getBoolean(Constants.PREF_PRINT_ENABLED, false));
        
        // Load theme preference
        int themeMode = sharedPreferences.getInt(Constants.PREF_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (themeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            radioGroupTheme.check(R.id.radioLightMode);
        } else if (themeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            radioGroupTheme.check(R.id.radioDarkMode);
        } else {
            radioGroupTheme.check(R.id.radioSystemMode);
        }
    }

    private void setupClickListeners() {
        btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void animateIn(View v, long delay) {
        if (v == null) return;
        v.setAlpha(0f);
        v.setTranslationY(24f);
        v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(320)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void saveSettings() {
        String companyName = etCompanyName.getText().toString().trim();
        String companyAddress = etCompanyAddress.getText().toString().trim();
        String companyPhone = etCompanyPhone.getText().toString().trim();


        if (companyName.isEmpty()) {
            Toast.makeText(this, "BhishiGroup name is required", Toast.LENGTH_SHORT).show();
            return;
        }


        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREF_COMPANY_NAME, companyName);
        editor.putString(Constants.PREF_COMPANY_ADDRESS, companyAddress);
        editor.putString(Constants.PREF_COMPANY_PHONE, companyPhone);
        editor.putBoolean(Constants.PREF_SMS_ENABLED, cbSmsEnabled.isChecked());
        editor.putBoolean(Constants.PREF_PRINT_ENABLED, cbPrintEnabled.isChecked());
        
        // Save theme preference
        int selectedTheme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        int checkedId = radioGroupTheme.getCheckedRadioButtonId();
        if (checkedId == R.id.radioLightMode) {
            selectedTheme = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (checkedId == R.id.radioDarkMode) {
            selectedTheme = AppCompatDelegate.MODE_NIGHT_YES;
        }
        
        editor.putInt(Constants.PREF_THEME_MODE, selectedTheme);
        editor.apply();
        
        // Apply theme immediately to entire app
        AppCompatDelegate.setDefaultNightMode(selectedTheme);

        Toast.makeText(this, "Settings saved! Theme applied.", Toast.LENGTH_SHORT).show();
        
        // Recreate activity to apply theme immediately
        recreate();
    }
}
