package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.models.PaymentEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentEventDao {

    @Query("SELECT * FROM payment_events ORDER BY receivedAt DESC")
    fun getAllEvents(): Flow<List<PaymentEvent>>

    @Query("SELECT * FROM payment_events ORDER BY receivedAt DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 100): Flow<List<PaymentEvent>>

    @Query("SELECT * FROM payment_events WHERE status IN ('received', 'parsed') ORDER BY receivedAt ASC")
    suspend fun getPendingSyncEvents(): List<PaymentEvent>

    @Query("SELECT * FROM payment_events WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getByFingerprint(fingerprint: String): PaymentEvent?

    @Query("SELECT * FROM payment_events WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getByTransactionId(transactionId: String): PaymentEvent?

    @Query("SELECT * FROM payment_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PaymentEvent?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: PaymentEvent): Long

    @Update
    suspend fun update(event: PaymentEvent)

    @Query("DELETE FROM payment_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM payment_events")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM payment_events WHERE status = :status")
    fun getCountByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM payment_events")
    fun getTotalCount(): Flow<Int>
}
