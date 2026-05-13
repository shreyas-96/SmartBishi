package com.example.routewisecollection.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.net.Uri;
import android.content.Intent;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.routewisecollection.R;
import com.example.routewisecollection.models.Customer;

import java.util.ArrayList;
import java.util.List;

public class HomeCustomerAdapter extends RecyclerView.Adapter<HomeCustomerAdapter.ViewHolder> {

    private List<Customer> customers = new ArrayList<>();
    private List<Customer> customersFiltered = new ArrayList<>();
    private final Context context;
    private OnCustomerClickListener listener;

    public interface OnCustomerClickListener {
        void onCustomerClick(Customer customer);
    }

    public HomeCustomerAdapter(Context context) {
        this.context = context;
    }

    public void setOnCustomerClickListener(OnCustomerClickListener listener) {
        this.listener = listener;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
        this.customersFiltered = new ArrayList<>(customers);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        customersFiltered.clear();
        if (query.isEmpty()) {
            customersFiltered.addAll(customers);
        } else {
            String searchQuery = query.toLowerCase().trim();
            for (Customer customer : customers) {
                if (customer.getName().toLowerCase().contains(searchQuery) ||
                    customer.getPhoneNumber().contains(searchQuery) ||
                    (customer.getAccountNumber() != null && customer.getAccountNumber().toLowerCase().contains(searchQuery))) {
                    customersFiltered.add(customer);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_customer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Customer customer = customersFiltered.get(position);
        
        String initial = customer.getName() != null && !customer.getName().isEmpty() 
                ? customer.getName().substring(0, 1).toUpperCase() 
                : "?";
        holder.tvInitial.setText(initial);
        
        holder.tvName.setText(customer.getName());
        holder.tvPhone.setText(customer.getPhoneNumber());
        holder.tvAddress.setText(customer.getAddress() != null ? customer.getAddress() : "No address");

        // Make the linear10 container clickable to call the customer with confirmation
        holder.linearCall.setOnClickListener(v -> {
            if (customer.getPhoneNumber() != null && !customer.getPhoneNumber().isEmpty()) {
                new AlertDialog.Builder(context)
                        .setTitle("📞 Call Customer")
                        .setMessage("Do you want to call " + customer.getName() + "?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(Uri.parse("tel:" + customer.getPhoneNumber()));
                            context.startActivity(intent);
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCustomerClick(customer);
            }
        });
    }

    @Override
    public int getItemCount() {
        return customersFiltered.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvAddress, tvInitial;
        LinearLayout linearCall;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCustomerName);
            tvPhone = itemView.findViewById(R.id.tvCustomerPhone);
            tvAddress = itemView.findViewById(R.id.tvCustomerAddress);
            tvInitial = itemView.findViewById(R.id.tvCustomerInitial);
            linearCall = itemView.findViewById(R.id.linear10);
        }
    }
}
