package com.example.routewisecollection.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.routewisecollection.R;
import com.example.routewisecollection.models.Transaction;

import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> transactions;
    private Context context;
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction, int position);
        void onDeleteClick(Transaction transaction, int position);
    }

    public TransactionAdapter(List<Transaction> transactions, Context context) {
        this.transactions = transactions;
        this.context = context;
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        
        // Set basic info
        holder.tvCustomerName.setText(transaction.getCustomerName());
        holder.tvAccountNumber.setText("A/C: " + transaction.getAccountNumber());
        holder.tvDateTime.setText(transaction.getDate() + " " + transaction.getTime());
        
        // Show receipt number and mode
        String receiptInfo = "Receipt: " + transaction.getReceiptNumber();
        if (transaction.getMode() != null && !transaction.getMode().isEmpty()) {
            receiptInfo += " (" + transaction.getMode().toUpperCase() + ")";
            
            // Append online mobile number if applicable
            if ("online".equalsIgnoreCase(transaction.getMode()) && 
                transaction.getOnlineMobileNumber() != null && 
                !transaction.getOnlineMobileNumber().isEmpty()) {
                receiptInfo += " | Mob: " + transaction.getOnlineMobileNumber();
            }
        }
        holder.tvReceiptNumber.setText(receiptInfo);
        
        // Set transaction type and styling
        if (transaction.isDeposit()) {
            holder.tvTransactionType.setText("DEPOSIT");
            holder.tvTransactionType.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            holder.tvAmount.setText("+" + String.format(Locale.getDefault(), "₹%.2f", transaction.getAmount()));
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            holder.ivTransactionIcon.setImageResource(R.drawable.ic_arrow_downward); // Deposit icon
            holder.ivTransactionIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            
            // Hide interest and total amount fields since they're no longer in the new structure
            holder.tvInterest.setVisibility(View.GONE);
            holder.tvTotalAmount.setVisibility(View.GONE);
        } else {
            holder.tvTransactionType.setText("WITHDRAWAL");
            holder.tvTransactionType.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            holder.tvAmount.setText("-" + String.format(Locale.getDefault(), "₹%.2f", transaction.getAmount()));
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            holder.ivTransactionIcon.setImageResource(R.drawable.ic_arrow_upward); // Withdrawal icon
            holder.ivTransactionIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            
            // Hide interest fields for withdrawals
            holder.tvInterest.setVisibility(View.GONE);
            holder.tvTotalAmount.setVisibility(View.GONE);
        }
        
        // Set remarks (previously notes)
        if (transaction.getRemarks() != null && !transaction.getRemarks().isEmpty()) {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText("Remarks: " + transaction.getRemarks());
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransactionClick(transaction, position);
            }
        });
        
        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(transaction, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardTransaction;
        ImageView ivTransactionIcon, ivDelete;
        TextView tvTransactionType, tvCustomerName, tvAccountNumber, tvAmount, 
                 tvDateTime, tvReceiptNumber, tvNotes, tvInterest, tvTotalAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTransaction = itemView.findViewById(R.id.cardTransaction);
            ivTransactionIcon = itemView.findViewById(R.id.ivTransactionIcon);
            ivDelete = itemView.findViewById(R.id.ivDelete);
            tvTransactionType = itemView.findViewById(R.id.tvTransactionType);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvAccountNumber = itemView.findViewById(R.id.tvAccountNumber);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvReceiptNumber = itemView.findViewById(R.id.tvReceiptNumber);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            tvInterest = itemView.findViewById(R.id.tvInterest);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
        }
    }
}
