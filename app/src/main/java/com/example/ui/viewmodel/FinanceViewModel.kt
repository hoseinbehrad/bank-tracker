package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BankCardEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.repository.FinanceRepository
import com.example.sms.BankSmsParser
import com.example.sms.BankSmsReceiver
import com.example.sms.ParsedBankSms
import com.example.sms.SmsInboxScanner
import com.example.sms.SmsScanResult
import com.example.util.BackupPayload
import com.example.util.CurrencyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TimeFilter(val titleFa: String, val titleEn: String) {
    THIS_MONTH("این دوره", "Current Period"),
    LAST_MONTH("دوره قبل", "Previous Period"),
    ALL_TIME("کل تاریخچه", "All Time")
}

data class CategoryExpenseStat(
    val categoryName: String,
    val totalRials: Long,
    val count: Int,
    val percentage: Float,
    val colorHex: Long
)

data class FinanceDashboardUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val cards: List<BankCardEntity> = emptyList(),
    val selectedCard: BankCardEntity? = null, // null indicates "Overall"
    val isOverallView: Boolean = true,
    val accounts: List<String> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val activeMonthSheet: String = "شهریور",
    val availableMonths: List<String> = listOf("شهریور", "مرداد"),
    val selectedMonthFilter: String? = "شهریور", // null means all months
    val selectedTimeFilter: TimeFilter = TimeFilter.THIS_MONTH,
    val currency: CurrencyType = CurrencyType.TOMAN,
    val usePersianDigits: Boolean = true,
    val searchQuery: String = "",
    val totalBalanceRials: Long = 0L,
    val overallBalanceRials: Long = 0L,
    val totalIncomeRials: Long = 0L,
    val totalExpenseRials: Long = 0L,
    val netBalanceChangeRials: Long = 0L,
    val categoryStats: List<CategoryExpenseStat> = emptyList(),
    val isScanningInbox: Boolean = false,
    val scanResult: SmsScanResult? = null,
    val pendingCategorizationTx: TransactionEntity? = null
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    private val _selectedCardId = MutableStateFlow<Long?>(null) // null = Overall
    private val _activeMonthSheet = MutableStateFlow("شهریور")
    private val _selectedMonthFilter = MutableStateFlow<String?>("شهریور")
    private val _selectedTimeFilter = MutableStateFlow(TimeFilter.THIS_MONTH)
    private val _currency = MutableStateFlow(CurrencyType.TOMAN)
    private val _usePersianDigits = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _isScanningInbox = MutableStateFlow(false)
    private val _scanResult = MutableStateFlow<SmsScanResult?>(null)
    private val _pendingCategorizationTx = MutableStateFlow<TransactionEntity?>(null)

    init {
        val db = AppDatabase.getInstance(application)
        repository = FinanceRepository(db.transactionDao(), db.bankCardDao(), db.categoryDao())

        // Ensure default categories and seed cards if brand new
        viewModelScope.launch {
            repository.ensureDefaultCategories()
            val existingCards = repository.getAllCardsDirect()
            val existingTx = repository.getAllTransactionsDirect()
            if (existingCards.isEmpty() && existingTx.isEmpty()) {
                seedInitialCards()
            }
        }

        // Listen to incoming SMS broadcasts from BankSmsReceiver
        viewModelScope.launch {
            BankSmsReceiver.incomingTransactionEvent.collect { incomingTx ->
                _pendingCategorizationTx.value = incomingTx
            }
        }
    }

    private data class FilterState(
        val selectedCardId: Long?,
        val activeMonth: String,
        val selectedMonth: String?,
        val searchQuery: String
    )

    private val filterStateFlow = combine(
        _selectedCardId,
        _activeMonthSheet,
        _selectedMonthFilter,
        _searchQuery
    ) { cardId, activeMonth, selectedMonth, query ->
        FilterState(cardId, activeMonth, selectedMonth, query)
    }

    private data class FormatState(
        val currency: CurrencyType,
        val usePersianDigits: Boolean,
        val isScanning: Boolean,
        val pendingTx: TransactionEntity?,
        val scanResult: SmsScanResult?
    )

    private val formatStateFlow = combine(
        _currency,
        _usePersianDigits,
        _isScanningInbox,
        _pendingCategorizationTx,
        _scanResult
    ) { curr, digits, scanning, pendingTx, scanResult ->
        FormatState(curr, digits, scanning, pendingTx, scanResult)
    }

    val uiState: StateFlow<FinanceDashboardUiState> = combine(
        repository.allTransactions,
        repository.allCards,
        repository.allCategories,
        filterStateFlow,
        formatStateFlow
    ) { transactions, cards, categories, filters, formats ->
        // Distinct months from transactions
        val txMonths = transactions.map { it.monthSheet }.filter { it.isNotBlank() }.distinct()
        val distinctMonths = (listOf(filters.activeMonth) + txMonths).distinct()

        // Find selected card
        val selectedCard = if (filters.selectedCardId != null) {
            cards.find { it.id == filters.selectedCardId }
        } else null
        val isOverall = selectedCard == null

        // Calculate live balance for each card:
        // Reads from the latest transaction with balanceRials > 0 for that card
        val cardBalances = cards.associate { card ->
            val latestTxWithBalance = transactions
                .filter { (it.accountNumber == card.accountNumber || it.bankName == card.bankName) && it.balanceRials > 0 }
                .maxByOrNull { it.timestamp }
            val balance = latestTxWithBalance?.balanceRials ?: card.balanceRials
            card.id to balance
        }

        val liveCards = cards.map { card ->
            val liveBal = cardBalances[card.id] ?: card.balanceRials
            card.copy(balanceRials = liveBal)
        }

        val liveSelectedCard = if (selectedCard != null) {
            val liveBal = cardBalances[selectedCard.id] ?: selectedCard.balanceRials
            selectedCard.copy(balanceRials = liveBal)
        } else null

        // Overall balance
        val overallBalance = liveCards.sumOf { it.balanceRials }
        val displayedBalance = if (liveSelectedCard != null) liveSelectedCard.balanceRials else overallBalance

        // Filtering transactions:
        val filteredTransactions = transactions.filter { tx ->
            val matchesCard = if (liveSelectedCard != null) {
                tx.accountNumber == liveSelectedCard.accountNumber || tx.bankName == liveSelectedCard.bankName
            } else true

            val matchesMonth = if (filters.selectedMonth != null) {
                tx.monthSheet == filters.selectedMonth
            } else true

            val matchesQuery = filters.searchQuery.isBlank() ||
                    tx.note.contains(filters.searchQuery, ignoreCase = true) ||
                    tx.bankName.contains(filters.searchQuery, ignoreCase = true) ||
                    tx.accountNumber.contains(filters.searchQuery) ||
                    tx.category.contains(filters.searchQuery, ignoreCase = true)

            matchesCard && matchesMonth && matchesQuery
        }

        // Calculate income & expense
        var incomeSum = 0L
        var expenseSum = 0L
        val categoryExpenses = mutableMapOf<String, Pair<Long, Int>>()

        for (tx in filteredTransactions) {
            if (tx.isIncome) {
                incomeSum += tx.amountRials
            } else {
                expenseSum += tx.amountRials
                val catName = TransactionCategory.getDisplayName(tx.category)
                val current = categoryExpenses[catName] ?: Pair(0L, 0)
                categoryExpenses[catName] = Pair(current.first + tx.amountRials, current.second + 1)
            }
        }

        val categoryStats = categoryExpenses.map { (catName, data) ->
            val total = data.first
            val count = data.second
            val percentage = if (expenseSum > 0) (total.toFloat() / expenseSum.toFloat()) * 100f else 0f
            val matchedCatEntity = categories.find { it.name == catName }
            val colorHex = matchedCatEntity?.colorHex ?: TransactionCategory.getColor(catName).value.toLong()
            CategoryExpenseStat(catName, total, count, percentage, colorHex)
        }.sortedByDescending { it.totalRials }

        val accounts = (liveCards.map { it.accountNumber } + transactions.map { it.accountNumber }).distinct().filter { it.isNotBlank() }

        FinanceDashboardUiState(
            transactions = filteredTransactions,
            allTransactions = transactions,
            cards = liveCards,
            selectedCard = liveSelectedCard,
            isOverallView = isOverall,
            accounts = accounts,
            categories = categories,
            activeMonthSheet = filters.activeMonth,
            availableMonths = distinctMonths,
            selectedMonthFilter = filters.selectedMonth,
            selectedTimeFilter = TimeFilter.THIS_MONTH,
            currency = formats.currency,
            usePersianDigits = formats.usePersianDigits,
            searchQuery = filters.searchQuery,
            totalBalanceRials = displayedBalance,
            overallBalanceRials = overallBalance,
            totalIncomeRials = incomeSum,
            totalExpenseRials = expenseSum,
            netBalanceChangeRials = incomeSum - expenseSum,
            categoryStats = categoryStats,
            isScanningInbox = formats.isScanning,
            scanResult = formats.scanResult,
            pendingCategorizationTx = formats.pendingTx
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceDashboardUiState()
    )

    // Card Selection
    fun selectOverall() {
        _selectedCardId.value = null
    }

    fun selectCard(card: BankCardEntity) {
        _selectedCardId.value = card.id
    }

    fun saveCard(card: BankCardEntity) {
        viewModelScope.launch {
            if (card.id == 0L) {
                repository.insertCard(card)
            } else {
                repository.updateCard(card)
            }
        }
    }

    fun deleteCard(card: BankCardEntity) {
        viewModelScope.launch {
            repository.deleteCard(card)
            if (_selectedCardId.value == card.id) {
                _selectedCardId.value = null
            }
        }
    }

    // Category Management
    fun addCategory(name: String, colorHex: Long) {
        viewModelScope.launch {
            val clean = name.trim()
            if (clean.isNotBlank()) {
                repository.insertCategory(clean, colorHex)
            }
        }
    }

    fun removeCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Pending Categorization (Notification popup & Live SMS popup)
    fun showPendingCategorizationById(txId: Long) {
        viewModelScope.launch {
            val tx = repository.getTransactionById(txId)
            if (tx != null) {
                _pendingCategorizationTx.value = tx
            }
        }
    }

    fun assignCategoryToPendingTx(categoryName: String) {
        val currentTx = _pendingCategorizationTx.value ?: return
        viewModelScope.launch {
            repository.updateTransactionCategory(currentTx.id, categoryName)
            _pendingCategorizationTx.value = null
        }
    }

    fun dismissPendingCategorization() {
        _pendingCategorizationTx.value = null
    }

    // Monthly Balance Sheets
    fun startNewMonthSheet(newMonthName: String) {
        val cleanName = newMonthName.trim()
        if (cleanName.isNotBlank()) {
            _activeMonthSheet.value = cleanName
            _selectedMonthFilter.value = cleanName
        }
    }

    fun selectMonthFilter(monthName: String?) {
        _selectedMonthFilter.value = monthName
    }

    // Preferences
    fun toggleCurrency() {
        _currency.value = if (_currency.value == CurrencyType.TOMAN) CurrencyType.RIAL else CurrencyType.TOMAN
    }

    fun toggleDigitFormat() {
        _usePersianDigits.value = !_usePersianDigits.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTimeFilter(filter: TimeFilter) {
        _selectedTimeFilter.value = filter
    }

    // Transactions
    fun updateTransactionCategory(transaction: TransactionEntity, newCategory: String) {
        viewModelScope.launch {
            repository.updateTransaction(transaction.copy(category = newCategory))
        }
    }

    fun updateTransactionNote(transaction: TransactionEntity, newNote: String) {
        viewModelScope.launch {
            repository.updateTransaction(transaction.copy(note = newNote))
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.deleteById(transactionId)
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun addManualTransaction(
        accountNumber: String,
        bankName: String,
        type: String,
        amountRials: Long,
        categoryName: String,
        note: String
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                accountNumber = accountNumber.ifEmpty { "نقدی / کارت" },
                bankName = bankName.ifEmpty { "بانک صادرات" },
                type = type,
                amountRials = amountRials,
                balanceRials = 0L,
                category = categoryName,
                note = note,
                rawSms = "ثبت دستی",
                sender = "Manual",
                dateString = "امروز",
                timestamp = System.currentTimeMillis(),
                monthSheet = _activeMonthSheet.value
            )
            repository.insertTransaction(entity)
        }
    }

    fun parseAndAddSms(
        smsText: String,
        sender: String = "20000",
        customCategory: String? = null,
        note: String = ""
    ): ParsedBankSms {
        val configuredCards = uiState.value.cards
        val parsed = BankSmsParser.parse(smsText, sender, configuredCards)
        if (parsed.isValid) {
            viewModelScope.launch {
                val acc = parsed.accountNumber.ifEmpty {
                    configuredCards.firstOrNull { it.bankName == parsed.bankName }?.accountNumber ?: "ناشناخته"
                }
                val isDup = repository.isDuplicate(
                    amountRials = parsed.amountRials,
                    type = parsed.type,
                    dateString = parsed.dateString,
                    accountNumber = acc
                )

                if (!isDup) {
                    val chosenCategory = customCategory ?: parsed.suggestedCategory.titleFa
                    val entity = TransactionEntity(
                        accountNumber = acc,
                        bankName = parsed.bankName,
                        type = parsed.type,
                        amountRials = parsed.amountRials,
                        balanceRials = parsed.balanceRials,
                        category = chosenCategory,
                        note = note,
                        rawSms = parsed.rawSms,
                        sender = parsed.sender,
                        dateString = parsed.dateString,
                        timestamp = System.currentTimeMillis(),
                        monthSheet = _activeMonthSheet.value
                    )
                    val id = repository.insertTransaction(entity)
                    // Prompt categorization popup for newly added SMS
                    _pendingCategorizationTx.value = entity.copy(id = id)
                }
            }
        }
        return parsed
    }

    // Inbox Scanner with Deduplication and card filtering
    fun scanInbox(context: Context) {
        viewModelScope.launch {
            _isScanningInbox.value = true
            val existing = repository.getAllTransactionsDirect()
            val currentCards = repository.getAllCardsDirect()
            val result = SmsInboxScanner.scanExistingBankSms(
                context = context,
                existingTransactions = existing,
                configuredCards = currentCards,
                activeMonth = _activeMonthSheet.value
            )

            if (result.newTransactions.isNotEmpty()) {
                repository.insertAll(result.newTransactions)

                // Update card balances from the latest real transaction for each card
                val allTx = repository.getAllTransactionsDirect()
                for (card in currentCards) {
                    val latestTx = allTx
                        .filter { (it.accountNumber == card.accountNumber || it.bankName == card.bankName) && it.balanceRials > 0 }
                        .maxByOrNull { it.timestamp }
                    if (latestTx != null && latestTx.balanceRials > 0) {
                        repository.updateCardBalance(card.accountNumber, card.bankName, latestTx.balanceRials)
                    }
                }
            }
            _scanResult.value = result
            _isScanningInbox.value = false
        }
    }

    fun dismissScanResult() {
        _scanResult.value = null
    }

    // Backup & Restore
    fun restoreBackup(payload: BackupPayload, replaceAll: Boolean) {
        viewModelScope.launch {
            if (replaceAll) {
                repository.deleteAll()
                repository.deleteAllCards()
            }

            if (payload.cards.isNotEmpty()) {
                repository.insertAllCards(payload.cards)
            }

            if (payload.transactions.isNotEmpty()) {
                repository.insertAll(payload.transactions)
            }

            if (payload.activeMonth.isNotBlank()) {
                _activeMonthSheet.value = payload.activeMonth
                _selectedMonthFilter.value = payload.activeMonth
            }
        }
    }

    private suspend fun seedInitialCards() {
        // Mellat Card
        val mellatCard = BankCardEntity(
            accountNumber = "1234567890",
            bankName = "بانک ملت",
            cardTitle = "ملت من",
            cardColorHex = 0xFFDC2626L, // Mellat Red
            balanceRials = 27422465L,
            senderContact = "بانک ملت",
            contactPhone = "20000"
        )

        // Saderat Card
        val saderatCard = BankCardEntity(
            accountNumber = "54321",
            bankName = "بانک صادرات",
            cardTitle = "صادرات من",
            cardColorHex = 0xFF475569L, // Saderat Slate
            balanceRials = 16092897L,
            senderContact = "بانک صادرات",
            contactPhone = "200060"
        )

        repository.insertAllCards(listOf(mellatCard, saderatCard))
    }
}
