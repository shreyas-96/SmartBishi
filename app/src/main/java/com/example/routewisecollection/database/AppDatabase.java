package com.example.routewisecollection.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.routewisecollection.models.Customer;
import com.example.routewisecollection.models.Deposit;
import com.example.routewisecollection.models.Route;
import com.example.routewisecollection.models.Withdraw;

@Database(
        entities = {Customer.class, Deposit.class, Route.class, Withdraw.class},
        version = 10, // Updated for agent_id in Customers
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    // DAO getters
    public abstract CustomerDao customerDao();
    public abstract DepositDao depositDao();
    public abstract RouteDao routeDao();
    public abstract WithdrawDao withdrawDao();

    // -------------------------
    // Migration 3 → 4
    // -------------------------
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE deposits RENAME TO deposits_old;");
            database.execSQL(
                    "CREATE TABLE deposits (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                            "customer_id INTEGER NOT NULL," +
                            "deposit_date TEXT," +
                            "deposit_time TEXT," +
                            "amount REAL," +
                            "interest_amount REAL," +
                            "total_amount REAL," +
                            "payment_method TEXT," +
                            "receipt_number TEXT," +
                            "notes TEXT," +
                            "customer_name TEXT" +
                            ");"
            );
            database.execSQL(
                    "INSERT INTO deposits (id, customer_id, deposit_date, deposit_time, amount, interest_amount, total_amount, payment_method, receipt_number, notes, customer_name) " +
                            "SELECT id, customerId, depositDate, depositTime, amount, interestAmount, totalAmount, paymentMethod, receiptNumber, notes, customerName " +
                            "FROM deposits_old;"
            );
            database.execSQL("DROP TABLE deposits_old;");
        }
    };

    // -------------------------
    // Migration 4 → 5
    // -------------------------
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE withdrawals (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                            "customerId INTEGER NOT NULL," +
                            "accountNumber TEXT," +
                            "amount REAL NOT NULL," +
                            "withdrawDate TEXT," +
                            "withdrawTime TEXT," +
                            "receiptNumber TEXT," +
                            "notes TEXT," +
                            "customerName TEXT," +
                            "withdrawMethod TEXT" +
                            ");"
            );
        }
    };

    // -------------------------
    // Migration 5 → 6
    // -------------------------
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // No schema changes
        }
    };

    // -------------------------
    // Migration 6 → 7
    // -------------------------
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE deposits ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE withdrawals ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1");
        }
    };

    // -------------------------
    // Migration 7 → 8 (Change Withdrawals.customerId INTEGER to TEXT)
    // -------------------------
    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE withdrawals RENAME TO withdrawals_old;");
            database.execSQL(
                    "CREATE TABLE withdrawals (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                            "customerId TEXT," +
                            "accountNumber TEXT," +
                            "amount REAL NOT NULL," +
                            "withdrawDate TEXT," +
                            "withdrawTime TEXT," +
                            "receiptNumber TEXT," +
                            "notes TEXT," +
                            "customerName TEXT," +
                            "withdrawMethod TEXT," +
                            "isSynced INTEGER NOT NULL DEFAULT 1" +
                            ");"
            );
            database.execSQL(
                    "INSERT INTO withdrawals (id, customerId, accountNumber, amount, withdrawDate, withdrawTime, receiptNumber, notes, customerName, withdrawMethod, isSynced) " +
                            "SELECT id, CAST(customerId AS TEXT), accountNumber, amount, withdrawDate, withdrawTime, receiptNumber, notes, customerName, withdrawMethod, isSynced " +
                            "FROM withdrawals_old;"
            );
            database.execSQL("DROP TABLE withdrawals_old;");
        }
    };

    // -------------------------
    // Migration 8 → 9 (Add transactionId and timestamp to Withdrawals)
    // -------------------------
    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE withdrawals ADD COLUMN transactionId TEXT");
            database.execSQL("ALTER TABLE withdrawals ADD COLUMN timestamp INTEGER NOT NULL DEFAULT 0");
        }
    };

    // -------------------------
    // Singleton instance
    // -------------------------
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "routewise_collection_db"
                            )
                            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
