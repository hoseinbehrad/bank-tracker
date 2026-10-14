package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import com.example.sms.BankSmsReceiver
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleCategoryPopupIntent(intent)
        setContent {
            MyApplicationTheme {
                Surface {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCategoryPopupIntent(intent)
    }

    private fun handleCategoryPopupIntent(intent: Intent?) {
        if (intent?.action == BankSmsReceiver.ACTION_OPEN_CATEGORY_POPUP) {
            val txId = intent.getLongExtra(BankSmsReceiver.EXTRA_TRANSACTION_ID, -1L)
            if (txId > 0) {
                viewModel.showPendingCategorizationById(txId)
            }
        }
    }
}
