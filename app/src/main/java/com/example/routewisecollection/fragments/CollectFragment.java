package com.example.routewisecollection.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.routewisecollection.R;
import com.example.routewisecollection.activities.WithdrawActivity;
import com.example.routewisecollection.models.Customer;
import com.example.routewisecollection.models.Deposit;
import com.example.routewisecollection.utils.LoginManager;
import com.example.routewisecollection.utils.SmsUtils;
import com.example.routewisecollection.utils.WhatsAppUtils;
import com.example.routewisecollection.viewmodel.DepositViewModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CollectFragment extends Fragment {

    private Spinner spinnerCustomer;
    private EditText etDepositAmount, etSearchCustomer, etOnlineMobileNumber;
    private TextView tvCustomerDetails;
    private View btnSaveDeposit;
    private RadioGroup radioGroupDepositType;
    private com.google.android.material.textfield.TextInputLayout tilOnlineMobileNumber;
    private CardView cardWithdrawOption;

    private List<Customer> customerList = new ArrayList<>();
    private List<Customer> displayedCustomers = new ArrayList<>();
    private ArrayAdapter<String> customerNamesAdapter;
    private Customer selectedCustomer;

    private DatabaseReference customersRef, transactionsRef;
    private String agentMobile, agentName;
    private DepositViewModel depositViewModel;
    private LoginManager loginManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collect, container, false);

        initializeViews(view);
        setupLogic();
        fetchCustomers();

        return view;
    }

    private void initializeViews(View view) {
        spinnerCustomer = view.findViewById(R.id.spinnerCustomer);
        etSearchCustomer = view.findViewById(R.id.etSearchCustomer);
        etDepositAmount = view.findViewById(R.id.etDepositAmount);
        tvCustomerDetails = view.findViewById(R.id.tvCustomerDetails);
        btnSaveDeposit = view.findViewById(R.id.btnSaveDeposit);
        radioGroupDepositType = view.findViewById(R.id.radioGroupDepositType);
        tilOnlineMobileNumber = view.findViewById(R.id.tilOnlineMobileNumber);
        etOnlineMobileNumber = view.findViewById(R.id.etOnlineMobileNumber);
        cardWithdrawOption = view.findViewById(R.id.cardWithdrawOption);

        loginManager = new LoginManager(requireContext());
        agentMobile = loginManager.getAgentMobile();
        agentName = loginManager.getAgentName();

        depositViewModel = new ViewModelProvider(this).get(DepositViewModel.class);

        customersRef = FirebaseDatabase.getInstance().getReference("agents").child(agentMobile).child("customers");
        transactionsRef = FirebaseDatabase.getInstance().getReference("agents").child(agentMobile).child("transactions");
    }

    private void setupLogic() {
        radioGroupDepositType.setOnCheckedChangeListener((group, checkedId) -> {
            tilOnlineMobileNumber.setVisibility(checkedId == R.id.radioOnline ? View.VISIBLE : View.GONE);
            if (checkedId != R.id.radioOnline) etOnlineMobileNumber.setText("");
        });

        cardWithdrawOption.setOnClickListener(v -> startActivity(new Intent(getActivity(), WithdrawActivity.class)));

        etSearchCustomer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateSpinner(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        spinnerCustomer.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < displayedCustomers.size()) {
                    selectedCustomer = displayedCustomers.get(position);
                    displayCustomerDetails();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnSaveDeposit.setOnClickListener(v -> saveDeposit());
    }

    private void fetchCustomers() {
        customersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                customerList.clear();
                List<String> names = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Customer c = ds.getValue(Customer.class);
                    if (c != null) {
                        customerList.add(c);
                        names.add(c.getName() + " - " + c.getAccountNumber());
                    }
                }
                customerNamesAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
                customerNamesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCustomer.setAdapter(customerNamesAdapter);
                updateSpinner("");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateSpinner(String query) {
        if (customerList.isEmpty()) return;
        String q = query.trim().toLowerCase();
        List<Customer> sorted = new ArrayList<>(customerList);
        if (!q.isEmpty()) {
            Collections.sort(sorted, (a, b) -> Integer.compare(score(b, q), score(a, q)));
        }
        displayedCustomers = sorted;
        List<String> names = new ArrayList<>();
        for (Customer c : sorted) names.add(c.getName() + " - " + c.getAccountNumber());
        
        if (customerNamesAdapter != null) {
            customerNamesAdapter.clear();
            customerNamesAdapter.addAll(names);
            customerNamesAdapter.notifyDataSetChanged();
        }
        if (!sorted.isEmpty()) {
            spinnerCustomer.setSelection(0);
            selectedCustomer = sorted.get(0);
            displayCustomerDetails();
        }
    }

    private int score(Customer c, String q) {
        String n = c.getName().toLowerCase();
        String a = c.getAccountNumber().toLowerCase();
        if (n.startsWith(q)) return 100;
        if (n.contains(q)) return 60;
        if (a.contains(q)) return 50;
        return 0;
    }

    private void displayCustomerDetails() {
        if (selectedCustomer != null) {
            tvCustomerDetails.setText("Name: " + selectedCustomer.getName() + "\nAcc: " + selectedCustomer.getAccountNumber() + "\nPh: " + selectedCustomer.getPhoneNumber());
        }
    }

    private void saveDeposit() {
        String amountStr = etDepositAmount.getText().toString().trim();
        if (amountStr.isEmpty() || selectedCustomer == null) {
            Toast.makeText(getContext(), "Please enter amount and select customer", Toast.LENGTH_SHORT).show();
            return;
        }

        final String mode = radioGroupDepositType.getCheckedRadioButtonId() == R.id.radioOnline ? "online" : "cash";
        String onlineNum = mode.equals("online") ? etOnlineMobileNumber.getText().toString().trim() : "";
        if (mode.equals("online") && onlineNum.isEmpty()) {
            tilOnlineMobileNumber.setError("Enter mobile number");
            return;
        }

        double amount = Double.parseDouble(amountStr);
        long timestamp = System.currentTimeMillis();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String txnId = transactionsRef.child(selectedCustomer.getPhoneNumber()).push().getKey();
        if (txnId == null) txnId = "TXN" + timestamp;

        Deposit deposit = new Deposit(selectedCustomer.getAccountNumber(), amount, 
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date()),
            selectedCustomer.getPhoneNumber(), selectedCustomer.getName(), date, mode, "RCP" + timestamp, "", timestamp, txnId, "deposit", onlineNum);

        boolean isOnline = com.example.routewisecollection.utils.NetworkUtils.isNetworkAvailable(requireContext());
        if (isOnline) {
            transactionsRef.child(selectedCustomer.getPhoneNumber()).child(txnId).setValue(deposit)
                .addOnSuccessListener(aVoid -> {
                    deposit.setSynced(true);
                    saveToLocal(deposit);
                })
                .addOnFailureListener(e -> {
                    deposit.setSynced(false);
                    saveToLocal(deposit);
                });
        } else {
            deposit.setSynced(false);
            saveToLocal(deposit);
        }
    }

    private void saveToLocal(Deposit deposit) {
        depositViewModel.insertDeposit(deposit, new DepositViewModel.OnInsertListener() {
            @Override
            public void onSuccess(long id) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Transaction Saved Successfully", Toast.LENGTH_SHORT).show();
                        etDepositAmount.setText("");
                        // Send notifications
                        sendNotifications(selectedCustomer, deposit.getAmount());
                    });
                }
            }
            @Override public void onFailure(String error) {}
        });
    }

    private void sendNotifications(Customer customer, double amount) {
        // SMS and WhatsApp logic
        if (SmsUtils.isSmsPermissionGranted(requireContext())) {
            String msg = "Payment of Rs. " + amount + " received for Account: " + customer.getAccountNumber() + ". Thank you!";
            SmsUtils.sendSms(requireContext(), customer.getPhoneNumber(), msg);
        }
        WhatsAppUtils.sendDepositMessage(requireContext(), customer.getPhoneNumber(), customer.getName(), amount, customer.getAccountNumber(), amount, agentName);
    }
}
