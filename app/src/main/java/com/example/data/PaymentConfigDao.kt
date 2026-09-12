package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.models.PaymentConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentConfigDao {

    @Query("SELECT * FROM payment_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<PaymentConfig?>

    @Query("SELECT * FROM payment_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): PaymentConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: PaymentConfig)
}
