package com.example.routewisecollection.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.routewisecollection.R;
import com.example.routewisecollection.models.CustomerReportModel;

import java.util.List;
import java.util.Locale;

public class CustomerReportAdapter extends RecyclerView.Adapter<CustomerReportAdapter.ViewHolder> {

    private final List<CustomerReportModel> customers;
    private final Context context;

    public CustomerReportAdapter(List<CustomerReportModel> customers, Context context){
        this.customers = customers;
        this.context = context;
    }

    @NonNull
    @Override
    public CustomerReportAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_customer_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerReportAdapter.ViewHolder holder, int position) {
        CustomerReportModel customer = customers.get(position);
        
        holder.tvCustomerName.setText(customer.getName());
        holder.tvMobile.setText(customer.getPhoneNumber());
        holder.tvAddress.setText(customer.getAddress());
        holder.tvTotalEntries.setText(String.format(Locale.getDefault(), "%d entries", customer.getTotalEntries()));
        holder.tvDateRange.setText(customer.getDateRange());
        
        // Display account balance with color coding
        double balance = customer.getTotalAmount();
        String balanceText = String.format(Locale.getDefault(), "₹%.2f", Math.abs(balance));
        
        if (balance > 0) {
            // Positive balance - customer has credit (green)
            holder.tvTotalAmount.setText("+" + balanceText);
            holder.tvTotalAmount.setTextColor(ContextCompat.getColor(context, R.color.success));
            holder.tvTotalAmount.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.success_light));
        } else if (balance < 0) {
            // Negative balance - customer owes money (red)
            holder.tvTotalAmount.setText("-" + balanceText);
            holder.tvTotalAmount.setTextColor(ContextCompat.getColor(context, R.color.error));
            holder.tvTotalAmount.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.error_light));
        } else {
            // Zero balance (neutral)
            holder.tvTotalAmount.setText("₹0.00");
            holder.tvTotalAmount.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.tvTotalAmount.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.background_light));
        }
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardCustomerReport;
        TextView tvCustomerName, tvMobile, tvTotalAmount, tvAddress, tvTotalEntries, tvDateRange;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCustomerReport = itemView.findViewById(R.id.cardCustomerReport);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvMobile = itemView.findViewById(R.id.tvMobile);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvTotalEntries = itemView.findViewById(R.id.tvTotalEntries);
            tvDateRange = itemView.findViewById(R.id.tvDateRange);
        }
    }
}