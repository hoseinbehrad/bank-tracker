package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC, id DESC")
    fun getTransactionsByTimeRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountNumber = :accountNumber ORDER BY timestamp DESC, id DESC")
    fun getTransactionsByAccount(accountNumber: String): Flow<List<TransactionEntity>>

    @Query("SELECT DISTINCT accountNumber FROM transactions WHERE accountNumber != ''")
    fun getDistinctAccounts(): Flow<List<String>>

    @Query("SELECT * FROM transactions WHERE monthSheet = :monthSheet ORDER BY timestamp DESC, id DESC")
    fun getTransactionsByMonth(monthSheet: String): Flow<List<TransactionEntity>>

    @Query("SELECT DISTINCT monthSheet FROM transactions WHERE monthSheet != '' ORDER BY id DESC")
    fun getDistinctMonthSheets(): Flow<List<String>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllDirect(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE amountRials = :amountRials AND type = :type AND accountNumber = :accountNumber AND (dateString = :dateString OR (dateString = '' AND :dateString = ''))")
    suspend fun countMatching(amountRials: Long, type: String, dateString: String, accountNumber: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
