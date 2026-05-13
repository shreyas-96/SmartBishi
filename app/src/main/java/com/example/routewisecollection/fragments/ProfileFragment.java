package com.example.routewisecollection.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.routewisecollection.R;
import com.example.routewisecollection.activities.LoginActivity;
import com.example.routewisecollection.activities.SettingsActivity;
import com.example.routewisecollection.utils.LoginManager;
import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment {

    private LoginManager loginManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        loginManager = new LoginManager(requireContext());

        TextView tvName = view.findViewById(R.id.tvProfileName);
        TextView tvMobile = view.findViewById(R.id.tvProfileMobile);
        TextView tvInitials = view.findViewById(R.id.tvInitials);
        MaterialButton btnSettings = view.findViewById(R.id.btnAppSettings);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        String name = loginManager.getAgentName();
        tvName.setText(name);
        tvMobile.setText(loginManager.getAgentMobile());
        if (name != null && !name.isEmpty()) {
            tvInitials.setText(name.substring(0, 1).toUpperCase());
        }

        btnSettings.setOnClickListener(v -> startActivity(new Intent(getActivity(), SettingsActivity.class)));

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        return view;
    }

    private void showLogoutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    loginManager.logout();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) getActivity().finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
