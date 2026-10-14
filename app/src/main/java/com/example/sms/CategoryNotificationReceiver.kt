package com.example.sms

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val txId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
        val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (txId <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val tx = db.transactionDao().getById(txId)
                if (tx != null) {
                    db.transactionDao().update(tx.copy(category = categoryName))
                }

                // Update notification to show confirmed category
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (notificationId != -1) {
                    val updatedNotification = NotificationCompat.Builder(context, "bank_transactions_channel")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("دسته‌بندی ثبت شد")
                        .setContentText("تراکنش در «$categoryName» ذخیره گردید.")
                        .setAutoCancel(true)
                        .setTimeoutAfter(3000)
                        .build()
                    notificationManager.notify(notificationId, updatedNotification)
                }

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "دسته‌بندی «$categoryName» ثبت شد", Toast.LENGTH_SHORT).show()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "extra_tx_id"
        const val EXTRA_CATEGORY_NAME = "extra_cat_name"
        const val EXTRA_NOTIFICATION_ID = "extra_notif_id"
    }
}
