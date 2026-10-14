package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountNumber: String,
    val bankName: String,
    val type: String, // "EXPENSE" or "INCOME"
    val amountRials: Long,
    val balanceRials: Long = 0L,
    val category: String = TransactionCategory.OTHER.name,
    val note: String = "",
    val rawSms: String = "",
    val sender: String = "",
    val dateString: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val monthSheet: String = "مرداد"
) {
    val isIncome: Boolean
        get() = type.equals("INCOME", ignoreCase = true)

    val categoryEnum: TransactionCategory
        get() = TransactionCategory.fromName(category)
}
