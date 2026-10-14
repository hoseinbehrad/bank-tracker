package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_cards")
data class BankCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountNumber: String,
    val bankName: String,
    val cardTitle: String,
    val cardColorHex: Long = 0xFFDC2626L, // Default Mellat Red
    val balanceRials: Long = 0L,
    val senderContact: String = "", // Contact name or shortcode (e.g. بانک ملت, بانک صادرات, 20000)
    val contactPhone: String = ""   // Exact phone/shortcode number
)
