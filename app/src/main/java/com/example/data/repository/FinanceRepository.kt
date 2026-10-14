package com.example.data.repository

import com.example.data.db.BankCardDao
import com.example.data.db.CategoryDao
import com.example.data.db.TransactionDao
import com.example.data.model.BankCardEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.sms.ParsedBankSms
import kotlinx.coroutines.flow.Flow

class FinanceRepository(
    private val dao: TransactionDao,
    private val cardDao: BankCardDao,
    private val categoryDao: CategoryDao
) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAll()
    val distinctAccounts: Flow<List<String>> = dao.getDistinctAccounts()
    val distinctMonthSheets: Flow<List<String>> = dao.getDistinctMonthSheets()
    val allCards: Flow<List<BankCardEntity>> = cardDao.getAllCards()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getTransactionsByTimeRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> {
        return dao.getTransactionsByTimeRange(startTime, endTime)
    }

    fun getTransactionsByAccount(accountNumber: String): Flow<List<TransactionEntity>> {
        return dao.getTransactionsByAccount(accountNumber)
    }

    fun getTransactionsByMonth(monthSheet: String): Flow<List<TransactionEntity>> {
        return dao.getTransactionsByMonth(monthSheet)
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return dao.getById(id)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = dao.insert(transaction)
        // If transaction has balance, update card balance
        if (transaction.balanceRials > 0 && transaction.accountNumber.isNotBlank()) {
            updateCardBalance(transaction.accountNumber, transaction.bankName, transaction.balanceRials)
        }
        return id
    }

    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long> {
        val ids = dao.insertAll(transactions)
        for (tx in transactions) {
            if (tx.balanceRials > 0 && tx.accountNumber.isNotBlank()) {
                updateCardBalance(tx.accountNumber, tx.bankName, tx.balanceRials)
            }
        }
        return ids
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        dao.update(transaction)
    }

    suspend fun updateTransactionCategory(id: Long, categoryName: String) {
        val tx = dao.getById(id)
        if (tx != null) {
            dao.update(tx.copy(category = categoryName))
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        dao.delete(transaction)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    suspend fun getAllTransactionsDirect(): List<TransactionEntity> {
        return dao.getAllDirect()
    }

    suspend fun isDuplicate(amountRials: Long, type: String, dateString: String, accountNumber: String): Boolean {
        return dao.countMatching(amountRials, type, dateString, accountNumber) > 0
    }

    // Card operations
    suspend fun insertCard(card: BankCardEntity): Long {
        return cardDao.insert(card)
    }

    suspend fun insertAllCards(cards: List<BankCardEntity>): List<Long> {
        return cardDao.insertAll(cards)
    }

    suspend fun updateCard(card: BankCardEntity) {
        cardDao.update(card)
    }

    suspend fun deleteCard(card: BankCardEntity) {
        cardDao.delete(card)
    }

    suspend fun deleteCardById(id: Long) {
        cardDao.deleteById(id)
    }

    suspend fun deleteAllCards() {
        cardDao.deleteAll()
    }

    suspend fun getAllCardsDirect(): List<BankCardEntity> {
        return cardDao.getAllCardsDirect()
    }

    suspend fun updateCardBalance(accountNumber: String, bankName: String, balanceRials: Long) {
        val existing = cardDao.getByAccountNumber(accountNumber)
        if (existing != null) {
            cardDao.updateBalance(accountNumber, balanceRials)
        } else {
            val color = when {
                bankName.contains("ملت") -> 0xFFDC2626L // Mellat Red
                bankName.contains("صادرات") -> 0xFF475569L // Saderat Slate
                bankName.contains("ملی") -> 0xFF0284C7L // Melli Sky
                bankName.contains("سامان") -> 0xFF0D9488L // Saman Teal
                bankName.contains("پاسارگاد") -> 0xFFD97706L // Pasargad Gold
                bankName.contains("بلو") || bankName.contains("blu") -> 0xFF2563EBL // Blu Blue
                else -> 0xFF475569L // Slate
            }
            cardDao.insert(
                BankCardEntity(
                    accountNumber = accountNumber,
                    bankName = bankName,
                    cardTitle = bankName,
                    cardColorHex = color,
                    balanceRials = balanceRials
                )
            )
        }
    }

    // Category operations
    suspend fun ensureDefaultCategories() {
        val count = categoryDao.count()
        if (count == 0) {
            val defaults = listOf(
                CategoryEntity(name = "سوپرمارکت", colorHex = 0xFF2563EB, isDefault = true),
                CategoryEntity(name = "غذا", colorHex = 0xFFF59E0B, isDefault = true),
                CategoryEntity(name = "حمل و نقل", colorHex = 0xFF10B981, isDefault = true),
                CategoryEntity(name = "قبوض، شارژ و اینترنت", colorHex = 0xFF8B5CF6, isDefault = true),
                CategoryEntity(name = "حقوق و واریز", colorHex = 0xFF059669, isDefault = true),
                CategoryEntity(name = "خیریه", colorHex = 0xFF0891B2, isDefault = true),
                CategoryEntity(name = "داروخانه", colorHex = 0xFFEF4444, isDefault = true)
            )
            categoryDao.insertAll(defaults)
        }
    }

    suspend fun insertCategory(name: String, colorHex: Long): Long {
        return categoryDao.insert(CategoryEntity(name = name.trim(), colorHex = colorHex))
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.delete(category)
    }

    suspend fun deleteCategoryByName(name: String) {
        categoryDao.deleteByName(name)
    }

    suspend fun getAllCategoriesDirect(): List<CategoryEntity> {
        return categoryDao.getAllCategoriesDirect()
    }

    suspend fun saveParsedSms(
        parsed: ParsedBankSms,
        category: String? = null,
        monthSheet: String = "شهریور"
    ): Long {
        val cat = category ?: parsed.suggestedCategory.titleFa
        val entity = TransactionEntity(
            accountNumber = parsed.accountNumber.ifEmpty { "ناشناخته" },
            bankName = parsed.bankName,
            type = parsed.type,
            amountRials = parsed.amountRials,
            balanceRials = parsed.balanceRials,
            category = cat,
            rawSms = parsed.rawSms,
            sender = parsed.sender,
            dateString = parsed.dateString,
            timestamp = System.currentTimeMillis(),
            monthSheet = monthSheet
        )
        return insertTransaction(entity)
    }
}
