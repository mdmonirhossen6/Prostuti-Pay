package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.models.PaymentConfig
import com.example.models.PaymentEvent

@Database(
    entities = [PaymentEvent::class, PaymentConfig::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun paymentEventDao(): PaymentEventDao
    abstract fun paymentConfigDao(): PaymentConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Extended fields for payment_events
                db.execSQL("ALTER TABLE payment_events ADD COLUMN expectedAmount REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE payment_events ADD COLUMN tolerance REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE payment_events ADD COLUMN minAcceptedAmount REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE payment_events ADD COLUMN maxAcceptedAmount REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE payment_events ADD COLUMN amountDifference REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE payment_events ADD COLUMN parserVersion TEXT NOT NULL DEFAULT '1.2'")
                db.execSQL("ALTER TABLE payment_events ADD COLUMN parseStatus TEXT NOT NULL DEFAULT 'parsed'")
                db.execSQL("ALTER TABLE payment_events ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'pending'")
                db.execSQL("ALTER TABLE payment_events ADD COLUMN backendStatus TEXT NOT NULL DEFAULT 'pending'")
                db.execSQL("ALTER TABLE payment_events ADD COLUMN backendMessage TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE payment_events ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE payment_events ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE payment_events ADD COLUMN auditLogJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_events_receivedAt ON payment_events(receivedAt)")

                // Extended fields for payment_config
                db.execSQL("ALTER TABLE payment_config ADD COLUMN toleranceEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE payment_config ADD COLUMN globalTolerance REAL NOT NULL DEFAULT 10.0")
                db.execSQL("ALTER TABLE payment_config ADD COLUMN minTolerance REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE payment_config ADD COLUMN maxTolerance REAL NOT NULL DEFAULT 100.0")
                db.execSQL("ALTER TABLE payment_config ADD COLUMN perPlanToleranceJson TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE payment_config ADD COLUMN perMethodToleranceJson TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE payment_config ADD COLUMN deviceName TEXT NOT NULL DEFAULT 'Dedicated Payment Phone 01'")
                db.execSQL("ALTER TABLE payment_config ADD COLUMN deviceId TEXT NOT NULL DEFAULT 'DEV-PROSTUTI-01'")
                db.execSQL("ALTER TABLE payment_config ADD COLUMN environment TEXT NOT NULL DEFAULT 'production'")
                db.execSQL("ALTER TABLE payment_config ADD COLUMN requestTimeoutSeconds INTEGER NOT NULL DEFAULT 15")
                db.execSQL("ALTER TABLE payment_config ADD COLUMN retryMaxAttempts INTEGER NOT NULL DEFAULT 9")
                db.execSQL("ALTER TABLE payment_config ADD COLUMN firebaseProjectId TEXT NOT NULL DEFAULT 'prostuti-app'")
                db.execSQL("ALTER TABLE payment_config ADD COLUMN setupWizardCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE payment_config ADD COLUMN setupCurrentStep INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prostuti_payment_listener.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
