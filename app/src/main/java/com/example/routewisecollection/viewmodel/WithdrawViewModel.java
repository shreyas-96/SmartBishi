package com.example.routewisecollection.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.routewisecollection.database.AppDatabase;
import com.example.routewisecollection.database.WithdrawDao;
import com.example.routewisecollection.models.Withdraw;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WithdrawViewModel extends AndroidViewModel {

    private final WithdrawDao withdrawDao;
    private final ExecutorService executorService;

    public WithdrawViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        withdrawDao = database.withdrawDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    // Withdraw operations
    public void insertWithdraw(Withdraw withdraw, OnInsertListener listener) {
        executorService.execute(() -> {
            try {
                long id = withdrawDao.insert(withdraw);
                if (listener != null) {
                    listener.onSuccess(id);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onFailure(e.getMessage());
                }
            }
        });
    }

    public void updateWithdraw(Withdraw withdraw) {
        executorService.execute(() -> withdrawDao.update(withdraw));
    }

    public void deleteWithdraw(Withdraw withdraw) {
        executorService.execute(() -> withdrawDao.delete(withdraw));
    }

    public LiveData<Withdraw> getWithdrawById(int withdrawId) {
        return withdrawDao.getWithdrawById(withdrawId);
    }

    public LiveData<List<Withdraw>> getWithdrawsByCustomer(String customerId) {
        return withdrawDao.getWithdrawsByCustomer(customerId);
    }

    public LiveData<List<Withdraw>> getWithdrawsByDate(String date) {
        return withdrawDao.getWithdrawsByDate(date);
    }

    public LiveData<List<Withdraw>> getWithdrawsByDateRange(String startDate, String endDate) {
        return withdrawDao.getWithdrawsByDateRange(startDate, endDate);
    }

    public LiveData<List<Withdraw>> getAllWithdraws() {
        return withdrawDao.getAllWithdraws();
    }

    public LiveData<Double> getTotalWithdrawsByCustomer(String customerId) {
        return withdrawDao.getTotalWithdrawsByCustomer(customerId);
    }

    public LiveData<Double> getTotalWithdrawsByDate(String date) {
        return withdrawDao.getTotalWithdrawsByDate(date);
    }

    public LiveData<Double> getTotalWithdrawsByDateRange(String startDate, String endDate) {
        return withdrawDao.getTotalWithdrawsByDateRange(startDate, endDate);
    }

    public LiveData<Withdraw> getWithdrawByReceiptNumber(String receiptNumber) {
        return withdrawDao.getWithdrawByReceiptNumber(receiptNumber);
    }

    public LiveData<Integer> getWithdrawCountByCustomer(String customerId) {
        return withdrawDao.getWithdrawCountByCustomer(customerId);
    }

    public LiveData<Integer> getWithdrawCountByDate(String date) {
        return withdrawDao.getWithdrawCountByDate(date);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }

    // Callback interface
    public interface OnInsertListener {
        void onSuccess(long id);
        void onFailure(String error);
    }
}
