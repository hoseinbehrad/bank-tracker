package com.example.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.TransactionEntity
import com.example.util.CurrencyType
import com.example.util.CurrencyUtils
import com.example.util.PersianCalendarUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // Group messages by sender in case of multi-part SMS
        val sender = messages[0].originatingAddress ?: "Bank"
        val bodyBuilder = StringBuilder()
        for (sms in messages) {
            bodyBuilder.append(sms.messageBody)
        }
        val fullBody = bodyBuilder.toString()

        if (!BankSmsParser.isBankSms(sender, fullBody)) {
            return
        }

        // Asynchronously save to Room Database
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val configuredCards = db.bankCardDao().getAllCardsDirect()

                val parsed = BankSmsParser.parse(fullBody, sender, configuredCards)
                if (!parsed.isValid) {
                    return@launch
                }

                // Filter by user configured bank cards only
                if (!SmsInboxScanner.matchesAnyConfiguredCard(context, configuredCards, sender, fullBody, parsed)) {
                    return@launch
                }

                val matchedCard = configuredCards.firstOrNull { card ->
                    (card.accountNumber.isNotBlank() && (fullBody.contains(card.accountNumber) || parsed.accountNumber == card.accountNumber)) ||
                    (card.bankName.contains("صادرات") && parsed.bankName.contains("صادرات")) ||
                    (card.bankName.contains("ملت") && parsed.bankName.contains("ملت"))
                }

                val finalBankName = matchedCard?.bankName ?: parsed.bankName
                val acc = if (parsed.accountNumber.isNotBlank() && parsed.accountNumber != "ناشناخته") {
                    parsed.accountNumber
                } else {
                    matchedCard?.accountNumber ?: "ناشناخته"
                }

                // Deduplication check
                val isDup = db.transactionDao().countMatching(
                    amountRials = parsed.amountRials,
                    type = parsed.type,
                    dateString = parsed.dateString,
                    accountNumber = acc
                ) > 0

                if (isDup) {
                    return@launch
                }

                val sheet = if (parsed.dateString.isNotBlank()) {
                    PersianCalendarUtils.extractMonthFromDateString(parsed.dateString)
                } else {
                    "شهریور"
                }

                val entity = TransactionEntity(
                    accountNumber = acc,
                    bankName = finalBankName,
                    type = parsed.type,
                    amountRials = parsed.amountRials,
                    balanceRials = parsed.balanceRials,
                    category = parsed.suggestedCategory.titleFa,
                    rawSms = parsed.rawSms,
                    sender = parsed.sender,
                    dateString = parsed.dateString,
                    timestamp = System.currentTimeMillis(),
                    monthSheet = sheet
                )
                val txId = db.transactionDao().insert(entity)

                // Update card balance
                if (parsed.balanceRials > 0 && acc.isNotBlank()) {
                    val existingCard = db.bankCardDao().getByAccountNumber(acc)
                    if (existingCard != null) {
                        db.bankCardDao().updateBalance(acc, parsed.balanceRials)
                    }
                }

                // Show rich local notification with action buttons and tap popup
                showTransactionNotification(context, parsed, entity.copy(id = txId), txId)

                // Notify live UI
                _incomingTransactionEvent.emit(entity.copy(id = txId))
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showTransactionNotification(
        context: Context,
        parsed: ParsedBankSms,
        tx: TransactionEntity,
        txId: Long
    ) {
        val channelId = "bank_transactions_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "پیامک‌های بانکی (Bank Transactions)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان خودکار تراکنش‌های بانکی ثبت شده و تعیین دسته‌بندی"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val actionText = if (parsed.type == "EXPENSE") "برداشت" else "واریز"
        val formattedAmount = CurrencyUtils.formatAmount(
            parsed.amountRials,
            CurrencyType.TOMAN,
            usePersianDigits = true
        )
        val formattedBalance = if (parsed.balanceRials > 0) {
            " | مانده: " + CurrencyUtils.formatAmount(
                parsed.balanceRials,
                CurrencyType.TOMAN,
                usePersianDigits = true
            )
        } else ""

        val title = "${tx.bankName}: $actionText $formattedAmount تومان"
        val content = "حساب ${tx.accountNumber}$formattedBalance — لمس برای تغییر دسته‌بندی"

        val notifId = (System.currentTimeMillis() % 100000).toInt()

        // Tap on notification opens MainActivity and triggers Category Popup
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_CATEGORY_POPUP
            putExtra(EXTRA_TRANSACTION_ID, txId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        // Add Quick Action Buttons in notification bar
        val quickCategories = listOf("سوپرمارکت", "غذا", "حمل و نقل", "قبوض، شارژ و اینترنت")
        quickCategories.forEachIndexed { index, catName ->
            val actionIntent = Intent(context, CategoryNotificationReceiver::class.java).apply {
                putExtra(CategoryNotificationReceiver.EXTRA_TRANSACTION_ID, txId)
                putExtra(CategoryNotificationReceiver.EXTRA_CATEGORY_NAME, catName)
                putExtra(CategoryNotificationReceiver.EXTRA_NOTIFICATION_ID, notifId)
            }
            val actionPendingIntent = PendingIntent.getBroadcast(
                context,
                notifId * 10 + index,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, catName, actionPendingIntent)
        }

        try {
            notificationManager.notify(notifId, builder.build())
        } catch (_: SecurityException) {
            // Notification permission not granted yet, ignore safely
        }
    }

    companion object {
        const val ACTION_OPEN_CATEGORY_POPUP = "com.example.ACTION_OPEN_CATEGORY_POPUP"
        const val EXTRA_TRANSACTION_ID = "extra_tx_id"

        private val _incomingTransactionEvent = MutableSharedFlow<TransactionEntity>(extraBufferCapacity = 1)
        val incomingTransactionEvent = _incomingTransactionEvent.asSharedFlow()
    }
}
