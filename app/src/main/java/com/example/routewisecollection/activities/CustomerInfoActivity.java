package com.example.routewisecollection.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.routewisecollection.R;
import com.example.routewisecollection.adapters.CustomerAdapter;
import com.example.routewisecollection.models.Customer;
import com.example.routewisecollection.utils.Constants;
import com.example.routewisecollection.utils.LoginManager;
import com.example.routewisecollection.utils.DatabaseSyncManager;
import com.example.routewisecollection.viewmodel.DepositViewModel;
import com.example.routewisecollection.viewmodel.WithdrawViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerInfoActivity extends AppCompatActivity implements CustomerAdapter.OnCustomerActionListener {

    private RecyclerView recyclerViewCustomers;
    private CustomerAdapter customerAdapter;
    private FloatingActionButton fabAddCustomer;
    private EditText etSearchCustomer;
    private TextView tvCustomerCount;
    private LinearLayout layoutEmptyState;

    // Filter buttons
    private Button btnFilterAll, btnFilterSangli, btnFilterKolhapur, btnFilterSatara;
    private Button currentActiveFilter;

    // Route mapping
    private final Map<Integer, String> routeMap = new HashMap<>();
    private List<Customer> allCustomers = new ArrayList<>();
    private String agentMobile; // Current logged-in agent mobile

    // Firebase reference
    private DatabaseReference customersRef;
    
    // ViewModels for local data (Offline support)
    private DepositViewModel depositViewModel;
    private WithdrawViewModel withdrawViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_info);

        initializeViews();
        setupRecyclerView();
        
        // Initialize ViewModels
        depositViewModel = new ViewModelProvider(this).get(DepositViewModel.class);
        withdrawViewModel = new ViewModelProvider(this).get(WithdrawViewModel.class);
        
        setupClickListeners();
        setupSearchFilter();

        // Get current logged-in agent
        LoginManager loginManager = new LoginManager(this);
        agentMobile = loginManager.getAgentMobile();

        // Initialize Firebase reference for customers of current agent
        customersRef = FirebaseDatabase.getInstance()
                .getReference("agents")
                .child(agentMobile)
                .child("customers");

        // Fetch customers from Firebase
        fetchCustomersFromFirebase();
    }

    private void initializeViews() {
        recyclerViewCustomers = findViewById(R.id.recyclerViewCustomers);
        fabAddCustomer = findViewById(R.id.fabAddCustomer);
        etSearchCustomer = findViewById(R.id.etSearchCustomer);
        tvCustomerCount = findViewById(R.id.tvCustomerCount);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterSangli = findViewById(R.id.btnFilterSangli);
        btnFilterKolhapur = findViewById(R.id.btnFilterKolhapur);
        btnFilterSatara = findViewById(R.id.btnFilterSatara);

        currentActiveFilter = btnFilterAll;
    }

    private void setupRecyclerView() {
        customerAdapter = new CustomerAdapter(this);
        customerAdapter.setOnCustomerActionListener(this);
        recyclerViewCustomers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCustomers.setAdapter(customerAdapter);
    }

    private void setupClickListeners() {
        fabAddCustomer.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCustomerActivity.class);
            startActivity(intent);
        });

        btnFilterAll.setOnClickListener(v -> setActiveFilter(btnFilterAll, -1));
        btnFilterSangli.setOnClickListener(v -> setActiveFilter(btnFilterSangli, getRouteIdByName("Sangli")));
        btnFilterKolhapur.setOnClickListener(v -> setActiveFilter(btnFilterKolhapur, getRouteIdByName("Kolhapur")));
        btnFilterSatara.setOnClickListener(v -> setActiveFilter(btnFilterSatara, getRouteIdByName("Satara")));
    }

    private void setupSearchFilter() {
        etSearchCustomer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* no-op */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                customerAdapter.filterBySearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { /* no-op */ }
        });
    }

    private void setActiveFilter(Button selectedButton, int routeId) {
        resetFilterButtons();
        selectedButton.setBackgroundResource(R.drawable.button_gradient);
        selectedButton.setTextColor(getResources().getColor(R.color.white));
        currentActiveFilter = selectedButton;
        customerAdapter.filterByRoute(routeId);
    }

    private void resetFilterButtons() {
        Button[] buttons = {btnFilterAll, btnFilterSangli, btnFilterKolhapur, btnFilterSatara};
        for (Button button : buttons) {
            button.setBackgroundResource(R.drawable.button_rounded);
            button.setTextColor(getResources().getColor(R.color.text_primary));
        }
    }

    private int getRouteIdByName(String routeName) {
        for (Map.Entry<Integer, String> entry : routeMap.entrySet()) {
            if (entry.getValue().equals(routeName)) return entry.getKey();
        }
        return -1;
    }

    @SuppressLint("SetTextI18n")
    private void updateCustomerCount(int count) {
        tvCustomerCount.setText("Total Customers: " + count);
    }

    // Firebase fetch and update RecyclerView
    private void fetchCustomersFromFirebase() {
        customersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                allCustomers.clear();
                for (DataSnapshot customerSnapshot : snapshot.getChildren()) {
                    Customer customer = customerSnapshot.getValue(Customer.class);
                    if (customer != null) {
                        allCustomers.add(customer);
                    }
                }
                customerAdapter.setCustomers(allCustomers);
                updateCustomerCount(allCustomers.size());

                if (allCustomers.isEmpty()) {
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    recyclerViewCustomers.setVisibility(View.GONE);
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    recyclerViewCustomers.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(CustomerInfoActivity.this, "Failed to load customers: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditCustomer(Customer customer) {
        Intent intent = new Intent(this, AddCustomerActivity.class);
        intent.putExtra("CUSTOMER_PHONE", customer.getPhoneNumber());
        intent.putExtra("CUSTOMER_NAME", customer.getName());
        intent.putExtra("CUSTOMER_ADDRESS", customer.getAddress());
        intent.putExtra("CUSTOMER_ACCOUNT", customer.getAccountNumber());
        intent.putExtra("CUSTOMER_ROUTE_ID", customer.getRouteId());
        intent.putExtra("IS_EDIT_MODE", true);
        startActivity(intent);
    }

    @Override
    public void onDeleteCustomer(Customer customer) {
        DatabaseSyncManager syncManager = new DatabaseSyncManager(this, agentMobile);
        syncManager.deleteCustomer(customer.getPhoneNumber(), new DatabaseSyncManager.OnSyncCompleteListener() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(CustomerInfoActivity.this, "Customer deleted from both App & Database!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(CustomerInfoActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onCustomerClick(Customer customer) {
        Intent intent = new Intent(this, DepositActivity.class);
        intent.putExtra(Constants.EXTRA_CUSTOMER_ID, customer.getId());
        intent.putExtra("CUSTOMER_PHONE", customer.getPhoneNumber());
        intent.putExtra("CUSTOMER_NAME", customer.getName());
        startActivity(intent);
    }

    @Override
    public String getRouteName(int routeId) {
        return routeMap.getOrDefault(routeId, "Unknown Route");
    }
    
    @Override
    public void loadCustomerBalance(Customer customer, CustomerAdapter.BalanceCallback callback) {
        // Switch to local DB for balance calculation to support offline entries
        
        // Use phoneNumber as the consistent linking ID for both Deposits and Withdrawals
        String customerId = customer.getPhoneNumber();
        
        depositViewModel.getTotalDepositsByCustomer(customerId).observe(this, deposits -> {
            double depositAmount = deposits != null ? deposits : 0;
            
            withdrawViewModel.getTotalWithdrawsByCustomer(customerId).observe(this, withdrawals -> {
                double withdrawalAmount = withdrawals != null ? withdrawals : 0;
                double netBalance = depositAmount - withdrawalAmount;
                
                callback.onBalanceLoaded(depositAmount, withdrawalAmount, netBalance);
            });
        });
    }

    public Button getCurrentActiveFilter() {
        return currentActiveFilter;
    }

    public void setCurrentActiveFilter(Button currentActiveFilter) {
        this.currentActiveFilter = currentActiveFilter;
    }
}

