package com.example.routewisecollection.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.routewisecollection.R;
import com.example.routewisecollection.activities.DailyReportActivity;
import com.example.routewisecollection.activities.MonthlyReportActivity;
import com.example.routewisecollection.activities.CustomerReportActivity;
import com.example.routewisecollection.activities.WithdrawReportActivity;

public class ReportsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        CardView cardDaily = view.findViewById(R.id.cardDailyReport);
        CardView cardMonthly = view.findViewById(R.id.cardMonthlyReport);
        CardView cardWithdraw = view.findViewById(R.id.cardWithdrawReport);
        CardView cardCustomer = view.findViewById(R.id.cardCustomerReport);

        cardDaily.setOnClickListener(v -> startActivity(new Intent(getActivity(), DailyReportActivity.class)));
        cardMonthly.setOnClickListener(v -> startActivity(new Intent(getActivity(), MonthlyReportActivity.class)));
        cardWithdraw.setOnClickListener(v -> startActivity(new Intent(getActivity(), WithdrawReportActivity.class)));
        cardCustomer.setOnClickListener(v -> startActivity(new Intent(getActivity(), CustomerReportActivity.class)));

        return view;
    }
}
