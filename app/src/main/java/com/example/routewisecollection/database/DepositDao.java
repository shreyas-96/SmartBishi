package com.example.routewisecollection.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.routewisecollection.models.Deposit;

import java.util.List;

@Dao
public interface DepositDao {

    // ➤ Insert new deposit
    @Insert
    long insert(Deposit deposit);

    // ➤ Update existing deposit
    @Update
    void update(Deposit deposit);

    // ➤ Delete a deposit
    @Delete
    void delete(Deposit deposit);

    // ➤ Get deposit by its transaction ID
    @Query("SELECT * FROM deposits WHERE transactionId = :transactionId")
    LiveData<Deposit> getDepositById(String transactionId);

    // ➤ Get all deposits by customer (latest first)
    @Query("SELECT * FROM deposits WHERE customerId = :customerId ORDER BY timestamp DESC")
    LiveData<List<Deposit>> getDepositsByCustomer(String customerId);

    // ➤ Get all deposits for a specific date
    @Query("SELECT * FROM deposits WHERE date = :date ORDER BY timestamp DESC")
    LiveData<List<Deposit>> getDepositsByDate(String date);

    // ➤ Get deposits within a date range
    @Query("SELECT * FROM deposits WHERE date BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    LiveData<List<Deposit>> getDepositsByDateRange(String startDate, String endDate);

    // ➤ Get all deposits (latest first)
    @Query("SELECT * FROM deposits ORDER BY timestamp DESC")
    LiveData<List<Deposit>> getAllDeposits();

    // ➤ Get total amount deposited by a specific customer
    @Query("SELECT SUM(amount) FROM deposits WHERE customerId = :customerId")
    LiveData<Double> getTotalDepositsByCustomer(String customerId);

    // ➤ Get total collection for a specific date
    @Query("SELECT SUM(amount) FROM deposits WHERE date = :date")
    LiveData<Double> getTotalCollectionByDate(String date);

    // ➤ Get total collection within a date range
    @Query("SELECT SUM(amount) FROM deposits WHERE date BETWEEN :startDate AND :endDate")
    LiveData<Double> getTotalCollectionByDateRange(String startDate, String endDate);

    // ➤ Find deposit by receipt number
    @Query("SELECT * FROM deposits WHERE receiptNumber = :receiptNumber")
    LiveData<Deposit> getDepositByReceiptNumber(String receiptNumber);

    // ➤ Get deposits by mode (cash/online)
    @Query("SELECT * FROM deposits WHERE mode = :mode ORDER BY timestamp DESC")
    LiveData<List<Deposit>> getDepositsByMode(String mode);

    // ➤ Get deposits by type (deposit/withdraw)
    @Query("SELECT * FROM deposits WHERE type = :type ORDER BY timestamp DESC")
    LiveData<List<Deposit>> getDepositsByType(String type);

    // ➤ Get total amount by mode for a specific date
    @Query("SELECT SUM(amount) FROM deposits WHERE mode = :mode AND date = :date")
    LiveData<Double> getTotalAmountByModeAndDate(String mode, String date);

    // ➤ Get count of deposits by customer
    @Query("SELECT COUNT(*) FROM deposits WHERE customerId = :customerId")
    LiveData<Integer> getDepositCountByCustomer(String customerId);

    // ➤ Get all unsynced deposits (for offline sync)
    @Query("SELECT * FROM deposits WHERE isSynced = 0 ORDER BY timestamp ASC")
    List<Deposit> getUnsyncedDeposits();

    // ➤ Mark deposit as synced
    @Query("UPDATE deposits SET isSynced = 1 WHERE transactionId = :transactionId")
    void markAsSynced(String transactionId);

    @Query("DELETE FROM deposits WHERE transactionId = :transactionId")
    void deleteByTransactionId(String transactionId);
}
