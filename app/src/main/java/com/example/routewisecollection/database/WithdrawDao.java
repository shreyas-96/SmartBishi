package com.example.routewisecollection.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.routewisecollection.models.Withdraw;

import java.util.List;

@Dao
public interface WithdrawDao {

    // ➤ Insert new withdrawal
    @Insert
    long insert(Withdraw withdraw);

    // ➤ Update existing withdrawal
    @Update
    void update(Withdraw withdraw);

    // ➤ Delete a withdrawal
    @Delete
    void delete(Withdraw withdraw);

    // ➤ Get withdrawal by its ID
    @Query("SELECT * FROM withdrawals WHERE id = :withdrawId")
    LiveData<Withdraw> getWithdrawById(int withdrawId);

    @Query("SELECT * FROM withdrawals WHERE customerId = :customerId ORDER BY withdrawDate DESC, withdrawTime DESC")
    LiveData<List<Withdraw>> getWithdrawsByCustomer(String customerId);

    // ➤ Get all withdrawals for a specific date
    @Query("SELECT * FROM withdrawals WHERE withdrawDate = :date ORDER BY withdrawTime DESC")
    LiveData<List<Withdraw>> getWithdrawsByDate(String date);

    // ➤ Get withdrawals within a date range
    @Query("SELECT * FROM withdrawals WHERE withdrawDate BETWEEN :startDate AND :endDate ORDER BY withdrawDate DESC, withdrawTime DESC")
    LiveData<List<Withdraw>> getWithdrawsByDateRange(String startDate, String endDate);

    // ➤ Get all withdrawals (latest first)
    @Query("SELECT * FROM withdrawals ORDER BY withdrawDate DESC, withdrawTime DESC")
    LiveData<List<Withdraw>> getAllWithdraws();

    @Query("SELECT SUM(amount) FROM withdrawals WHERE customerId = :customerId")
    LiveData<Double> getTotalWithdrawsByCustomer(String customerId);

    // ➤ Get total withdrawals for a specific date
    @Query("SELECT SUM(amount) FROM withdrawals WHERE withdrawDate = :date")
    LiveData<Double> getTotalWithdrawsByDate(String date);

    // ➤ Get total withdrawals within a date range
    @Query("SELECT SUM(amount) FROM withdrawals WHERE withdrawDate BETWEEN :startDate AND :endDate")
    LiveData<Double> getTotalWithdrawsByDateRange(String startDate, String endDate);

    // ➤ Find withdrawal by receipt number
    @Query("SELECT * FROM withdrawals WHERE receiptNumber = :receiptNumber")
    LiveData<Withdraw> getWithdrawByReceiptNumber(String receiptNumber);

    @Query("SELECT COUNT(*) FROM withdrawals WHERE customerId = :customerId")
    LiveData<Integer> getWithdrawCountByCustomer(String customerId);

    // ➤ Get count of withdrawals for a specific date
    @Query("SELECT COUNT(*) FROM withdrawals WHERE withdrawDate = :date")
    LiveData<Integer> getWithdrawCountByDate(String date);

    // ➤ Get all unsynced withdrawals (for offline sync)
    @Query("SELECT * FROM withdrawals WHERE isSynced = 0 ORDER BY withdrawDate ASC, withdrawTime ASC")
    List<Withdraw> getUnsyncedWithdraws();

    // ➤ Mark withdrawal as synced
    @Query("UPDATE withdrawals SET isSynced = 1 WHERE id = :withdrawId")
    void markAsSynced(int withdrawId);

    @Query("DELETE FROM withdrawals WHERE transactionId = :transactionId")
    void deleteByTransactionId(String transactionId);
}
