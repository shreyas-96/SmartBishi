package com.example.routewisecollection.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.routewisecollection.R;
import com.example.routewisecollection.activities.CustomerInfoActivity;
import com.example.routewisecollection.activities.RouteListActivity;
import com.example.routewisecollection.adapters.HomeCustomerAdapter;
import com.example.routewisecollection.utils.LoginManager;
import com.example.routewisecollection.viewmodel.DepositViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvAgentName, tvTotalCustomers, tvTodayCollection, tvActiveRoute;
    private CardView cardViewCustomers, cardRoutes;
    private EditText etSearch;
    private RecyclerView rvCustomerList;
    private HomeCustomerAdapter homeCustomerAdapter;
    private LoginManager loginManager;
    private DepositViewModel depositViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        loginManager = new LoginManager(requireContext());
        depositViewModel = new ViewModelProvider(this).get(DepositViewModel.class);

        tvAgentName = view.findViewById(R.id.tvAgentName);
        tvTotalCustomers = view.findViewById(R.id.tvTotalCustomers);
        tvTodayCollection = view.findViewById(R.id.tvTodayCollection);
        tvActiveRoute = view.findViewById(R.id.tvActiveRoute);
        cardViewCustomers = view.findViewById(R.id.cardViewCustomers);
        cardRoutes = view.findViewById(R.id.cardRoutes);
        rvCustomerList = view.findViewById(R.id.rvCustomerList);
        etSearch = view.findViewById(R.id.etSearch);

        setupRecyclerView();
        setupUI();
        setupClickListeners();
        setupSearch();
        observeStats();

        return view;
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (homeCustomerAdapter != null) {
                    homeCustomerAdapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        homeCustomerAdapter = new HomeCustomerAdapter(requireContext());
        rvCustomerList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCustomerList.setAdapter(homeCustomerAdapter);

        homeCustomerAdapter.setOnCustomerClickListener(customer -> {
            // Optional: Define behavior when a customer in the list is clicked
            // For now, it could open the same CustomerInfoActivity or a detail view
        });
    }

    private void setupUI() {
        tvAgentName.setText("Welcome, " + loginManager.getAgentName());
    }

    private void observeStats() {
        // Observe Customer Count - Filtered by Agent
        String agentId = loginManager.getAgentMobile();
        depositViewModel.getAllCustomers(agentId).observe(getViewLifecycleOwner(), customers -> {
            if (customers != null) {
                tvTotalCustomers.setText(String.valueOf(customers.size()));
                homeCustomerAdapter.setCustomers(customers);
            } else {
                tvTotalCustomers.setText("0");
            }
        });

        // Observe Today's Collection
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        depositViewModel.getTotalCollectionByDate(todayDate).observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                tvTodayCollection.setText("₹ " + String.format(Locale.getDefault(), "%.0f", total));
            } else {
                tvTodayCollection.setText("₹ 0");
            }
        });

        // Observe Routes count
        depositViewModel.getAllRoutes().observe(getViewLifecycleOwner(), routes -> {
            if (routes != null) {
                tvActiveRoute.setText(String.valueOf(routes.size()));
            } else {
                tvActiveRoute.setText("0");
            }
        });
    }

    private void setupClickListeners() {
        cardViewCustomers.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CustomerInfoActivity.class);
            intent.putExtra("agentId", loginManager.getAgentId());
            startActivity(intent);
        });

        cardRoutes.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RouteListActivity.class);
            startActivity(intent);
        });
    }
}
