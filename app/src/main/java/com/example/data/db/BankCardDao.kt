package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BankCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankCardDao {
    @Query("SELECT * FROM bank_cards ORDER BY id ASC")
    fun getAllCards(): Flow<List<BankCardEntity>>

    @Query("SELECT * FROM bank_cards ORDER BY id ASC")
    suspend fun getAllCardsDirect(): List<BankCardEntity>

    @Query("SELECT * FROM bank_cards WHERE accountNumber = :accountNumber LIMIT 1")
    suspend fun getByAccountNumber(accountNumber: String): BankCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: BankCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<BankCardEntity>): List<Long>

    @Update
    suspend fun update(card: BankCardEntity)

    @Delete
    suspend fun delete(card: BankCardEntity)

    @Query("DELETE FROM bank_cards WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE bank_cards SET balanceRials = :balanceRials WHERE accountNumber = :accountNumber")
    suspend fun updateBalance(accountNumber: String, balanceRials: Long)

    @Query("DELETE FROM bank_cards")
    suspend fun deleteAll()
}
