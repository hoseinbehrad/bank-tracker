package com.example.util

import com.example.data.model.BankCardEntity
import com.example.data.model.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val appVersion: String = "1.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val activeMonth: String,
    val cards: List<BankCardEntity>,
    val transactions: List<TransactionEntity>
)

object BackupManager {

    fun exportToJson(
        cards: List<BankCardEntity>,
        transactions: List<TransactionEntity>,
        activeMonth: String
    ): String {
        val root = JSONObject()
        root.put("app", "BankTracker")
        root.put("version", "1.0")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("activeMonth", activeMonth)

        // Cards array
        val cardsArray = JSONArray()
        for (card in cards) {
            val cObj = JSONObject()
            cObj.put("accountNumber", card.accountNumber)
            cObj.put("bankName", card.bankName)
            cObj.put("cardTitle", card.cardTitle)
            cObj.put("cardColorHex", card.cardColorHex)
            cObj.put("balanceRials", card.balanceRials)
            cObj.put("senderContact", card.senderContact)
            cObj.put("contactPhone", card.contactPhone)
            cardsArray.put(cObj)
        }
        root.put("cards", cardsArray)

        // Transactions array
        val txArray = JSONArray()
        for (tx in transactions) {
            val tObj = JSONObject()
            tObj.put("accountNumber", tx.accountNumber)
            tObj.put("bankName", tx.bankName)
            tObj.put("type", tx.type)
            tObj.put("amountRials", tx.amountRials)
            tObj.put("balanceRials", tx.balanceRials)
            tObj.put("category", tx.category)
            tObj.put("note", tx.note)
            tObj.put("rawSms", tx.rawSms)
            tObj.put("sender", tx.sender)
            tObj.put("dateString", tx.dateString)
            tObj.put("timestamp", tx.timestamp)
            tObj.put("monthSheet", tx.monthSheet)
            txArray.put(tObj)
        }
        root.put("transactions", txArray)

        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)
        val app = root.optString("app", "")
        val activeMonth = root.optString("activeMonth", "مرداد")

        val cardsList = mutableListOf<BankCardEntity>()
        val cardsArray = root.optJSONArray("cards")
        if (cardsArray != null) {
            for (i in 0 until cardsArray.length()) {
                val cObj = cardsArray.getJSONObject(i)
                cardsList.add(
                    BankCardEntity(
                        accountNumber = cObj.optString("accountNumber", ""),
                        bankName = cObj.optString("bankName", "بانک من"),
                        cardTitle = cObj.optString("cardTitle", "کارت بانکی"),
                        cardColorHex = cObj.optLong("cardColorHex", 0xFFDC2626L),
                        balanceRials = cObj.optLong("balanceRials", 0L),
                        senderContact = cObj.optString("senderContact", ""),
                        contactPhone = cObj.optString("contactPhone", "")
                    )
                )
            }
        }

        val txList = mutableListOf<TransactionEntity>()
        val txArray = root.optJSONArray("transactions")
        if (txArray != null) {
            for (i in 0 until txArray.length()) {
                val tObj = txArray.getJSONObject(i)
                txList.add(
                    TransactionEntity(
                        accountNumber = tObj.optString("accountNumber", ""),
                        bankName = tObj.optString("bankName", "بانک"),
                        type = tObj.optString("type", "EXPENSE"),
                        amountRials = tObj.optLong("amountRials", 0L),
                        balanceRials = tObj.optLong("balanceRials", 0L),
                        category = tObj.optString("category", "OTHER"),
                        note = tObj.optString("note", ""),
                        rawSms = tObj.optString("rawSms", ""),
                        sender = tObj.optString("sender", ""),
                        dateString = tObj.optString("dateString", ""),
                        timestamp = tObj.optLong("timestamp", System.currentTimeMillis()),
                        monthSheet = tObj.optString("monthSheet", activeMonth)
                    )
                )
            }
        }

        return BackupPayload(
            activeMonth = activeMonth,
            cards = cardsList,
            transactions = txList
        )
    }
}
