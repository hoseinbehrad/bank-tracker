package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TransactionCategory
import com.example.sms.BankSmsParser
import com.example.util.CurrencyType
import com.example.util.CurrencyUtils

@Composable
fun SmsSimulatorDialog(
    currency: CurrencyType,
    usePersianDigits: Boolean,
    onDismiss: () -> Unit,
    onImportSms: (String, String, TransactionCategory, String) -> Unit
) {
    // Default pre-filled with the user's exact SMS example
    val defaultSample = """
حساب1234567890
برداشت10,850,000
مانده98,765,432
05/05/05-12:57
    """.trimIndent()

    var smsText by remember { mutableStateOf(defaultSample) }
    var senderText by remember { mutableStateOf("20000") }
    var noteText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TransactionCategory?>(null) }

    val parsedState by remember(smsText, senderText) {
        derivedStateOf {
            BankSmsParser.parse(smsText, senderText)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("sms_simulator_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "آزمایش و ثبت پیامک بانکی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Bank SMS Tester & Parser",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            smsText = defaultSample
                            senderText = "20000"
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("نمونه ملت", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            smsText = """
حساب1234567890
واریز150,000,000
مانده248,765,432
05/05/01-10:00
                            """.trimIndent()
                            senderText = "20000"
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("نمونه واریز", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            smsText = """
برداشت2,500,000
خرید اسنپ فود
مانده12,000,000
05/05/06-20:15
                            """.trimIndent()
                            senderText = "20000"
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("نمونه خرید", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sender field
                OutlinedTextField(
                    value = senderText,
                    onValueChange = { senderText = it },
                    label = { Text("شماره یا نام فرستنده (Sender)") },
                    placeholder = { Text("20000 / Bank Mellat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // SMS input area
                OutlinedTextField(
                    value = smsText,
                    onValueChange = { smsText = it },
                    label = { Text("متن پیامک ارسالی از بانک (SMS Body)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("sms_input_field"),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Live Parsing Preview Card
                val isParsedValid = parsedState.isValid
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isParsedValid) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        }
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isParsedValid) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isParsedValid) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isParsedValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isParsedValid) "تحلیل هوشمند موفقیت‌آمیز بود" else "متن پیامک هنوز خوانده نشده است",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isParsedValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }

                        if (isParsedValid) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ParsedInfoRow("بانک شناسایی‌شده:", parsedState.bankName)
                            ParsedInfoRow("شماره حساب:", parsedState.accountNumber.ifEmpty { "تشخیص داده نشد" })
                            ParsedInfoRow("نوع عملیات:", if (parsedState.type == "EXPENSE") "برداشت (هزینه)" else "واریز (درآمد)")

                            val amountStr = CurrencyUtils.formatAmount(
                                amountRials = parsedState.amountRials,
                                currency = currency,
                                usePersianDigits = usePersianDigits,
                                includeUnit = true
                            )
                            ParsedInfoRow("مبلغ تراکنش:", amountStr)

                            if (parsedState.balanceRials > 0) {
                                val balStr = CurrencyUtils.formatAmount(
                                    amountRials = parsedState.balanceRials,
                                    currency = currency,
                                    usePersianDigits = usePersianDigits,
                                    includeUnit = true
                                )
                                ParsedInfoRow("مانده پس از تراکنش:", balStr)
                            }

                            if (parsedState.dateString.isNotEmpty()) {
                                ParsedInfoRow("تاریخ و زمان:", parsedState.dateString)
                            }
                            ParsedInfoRow("دسته‌بندی هوشمند:", parsedState.suggestedCategory.titleFa)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("یادداشت اختیاری") },
                    placeholder = { Text("مثلاً: خرید روغن و خواروبار") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action button
                Button(
                    onClick = {
                        val chosenCat = selectedCategory ?: parsedState.suggestedCategory
                        onImportSms(smsText, senderText, chosenCat, noteText)
                        onDismiss()
                    },
                    enabled = isParsedValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_sms_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ذخیره در تراکنش‌های مالی",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ParsedInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
