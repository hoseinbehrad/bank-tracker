package com.example.ui.dialogs

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankCardEntity
import com.example.util.CurrencyType
import com.example.util.CurrencyUtils

val CARD_COLOR_PRESETS = listOf(
    Pair("ملت قرمز", 0xFFDC2626L),
    Pair("صادرات تیره", 0xFF475569L),
    Pair("سامان فیروزه‌ای", 0xFF0D9488L),
    Pair("ملی آبی", 0xFF0284C7L),
    Pair("بلوبانک", 0xFF2563EBL),
    Pair("پاسارگاد طلایی", 0xFFD97706L),
    Pair("سپه سرمه‌ای", 0xFF1E3A8AL),
    Pair("سبز مالی", 0xFF16A34AL)
)

val BANK_NAME_PRESETS = listOf(
    "بانک ملت", "بانک صادرات", "بانک ملی", "بانک سامان",
    "بلو بانک", "بانک پاسارگاد", "بانک تجارت", "بانک سپه"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditCardDialog(
    existingCard: BankCardEntity? = null,
    currency: CurrencyType,
    usePersianDigits: Boolean,
    onDismiss: () -> Unit,
    onSaveCard: (card: BankCardEntity) -> Unit,
    onDeleteCard: ((card: BankCardEntity) -> Unit)? = null
) {
    val context = LocalContext.current

    var bankName by remember { mutableStateOf(existingCard?.bankName ?: "بانک ملت") }
    var cardTitle by remember { mutableStateOf(existingCard?.cardTitle ?: "کارت من") }
    var accountNumber by remember { mutableStateOf(existingCard?.accountNumber ?: "") }
    var senderContact by remember { mutableStateOf(existingCard?.senderContact ?: "") }
    var contactPhone by remember { mutableStateOf(existingCard?.contactPhone ?: "") }
    var balanceInput by remember {
        val initialBal = existingCard?.balanceRials ?: 0L
        val formatted = if (initialBal > 0) {
            val converted = if (currency == CurrencyType.TOMAN) initialBal / 10 else initialBal
            converted.toString()
        } else ""
        mutableStateOf(formatted)
    }
    var selectedColor by remember {
        mutableLongStateOf(
            existingCard?.cardColorHex ?: when {
                bankName.contains("صادرات") -> 0xFF475569L
                bankName.contains("ملت") -> 0xFFDC2626L
                else -> 0xFF0D9488L
            }
        )
    }

    // Contact Picker Launcher for picking bank sender contact
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        contactUri?.let { uri ->
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val idCol = c.getColumnIndex(ContactsContract.Contacts._ID)
                        val nameCol = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val contactId = if (idCol >= 0) c.getString(idCol) else null
                        val contactName = if (nameCol >= 0) c.getString(nameCol) else ""
                        if (contactName.isNotBlank()) {
                            senderContact = contactName
                            if (cardTitle.isBlank() || cardTitle == "کارت من" || cardTitle == "کارت اصلی") {
                                cardTitle = "$contactName من"
                            }
                            if (bankName.isBlank() || bankName == "بانک ملت") {
                                bankName = contactName
                            }
                        }

                        contactId?.let { id ->
                            val phoneCursor = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(id),
                                null
                            )
                            phoneCursor?.use { pc ->
                                if (pc.moveToFirst()) {
                                    val numberCol = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numberCol >= 0) {
                                        val number = pc.getString(numberCol) ?: ""
                                        contactPhone = number
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Handled gracefully
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (existingCard != null) "ویرایش کارت / بانک" else "افزودن بانک و کارت جدید",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                if (existingCard != null && onDeleteCard != null) {
                    IconButton(
                        onClick = {
                            onDeleteCard(existingCard)
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف کارت",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Bank Contact Picker Action
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "مخاطب پیامک بانک:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (senderContact.isNotBlank() || contactPhone.isNotBlank()) {
                                        "${senderContact.ifEmpty { "فرستنده" }} (${contactPhone.ifEmpty { "شماره ثبت شده" }})"
                                    } else {
                                        "هنوز مخاطبی انتخاب نشده است (اختیاری)"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { contactPickerLauncher.launch(null) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("pick_bank_contact_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("انتخاب از مخاطبین", fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "در اسکن پیامک‌ها، تنها پیامک‌های فرستنده‌های این کارت خوانده می‌شوند و سایر پیامک‌ها رد می‌شوند.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                // Bank Name selector chips
                Text(
                    text = "نام بانک صادرکننده:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BANK_NAME_PRESETS.forEach { preset ->
                        val isSelected = bankName == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                bankName = preset
                                if (cardTitle.isBlank() || cardTitle == "کارت من" || cardTitle.endsWith(" من")) {
                                    cardTitle = "$preset من"
                                }
                                if (preset == "بانک صادرات") {
                                    selectedColor = 0xFF475569L
                                    if (contactPhone.isBlank()) contactPhone = "B.Saderat"
                                } else if (preset == "بانک ملت") {
                                    selectedColor = 0xFFDC2626L
                                    if (contactPhone.isBlank()) contactPhone = "20000"
                                }
                            }
                        ) {
                            Text(
                                text = preset,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Account Number / Identifier (matches SMS)
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it.trim() },
                    label = { Text("شماره حساب (مطابق با پیامک بانک)") },
                    placeholder = { Text("مثال: 1234567890 یا 54321") },
                    supportingText = { Text("شماره‌ای که در متن پیامک‌های بانک قید می‌شود") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_account_number_input")
                )

                // Sender identifier / Phone (optional manual edit)
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it.trim() },
                    label = { Text("شماره / سرشماره فرستنده پیامک (اختیاری)") },
                    placeholder = { Text("مثال: 20000 یا B.Saderat") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Card Custom Title
                OutlinedTextField(
                    value = cardTitle,
                    onValueChange = { cardTitle = it },
                    label = { Text("عنوان دلخواه کارت") },
                    placeholder = { Text("مثال: حساب ملت، صادرات، حقوق") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Balance (Explicitly Optional)
                val unitName = if (currency == CurrencyType.TOMAN) "تومان" else "ریال"
                OutlinedTextField(
                    value = balanceInput,
                    onValueChange = { balanceInput = it },
                    label = { Text("موجودی اولیه (اختیاری - $unitName)") },
                    placeholder = { Text("اختیاری (خودکار از آخرین پیامک خوانده می‌شود)") },
                    supportingText = { Text("نیازی به وارد کردن نیست؛ موجودی به‌صورت خودکار از آخرین پیامک بانک خوانده و به‌روزرسانی می‌شود.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Card Color Theme selector
                Text(
                    text = "رنگ تم کارت:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CARD_COLOR_PRESETS.take(6).forEach { (_, colorHex) ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawParsedAmount = CurrencyUtils.parseAmount(balanceInput)
                    val balanceRials = if (currency == CurrencyType.TOMAN) rawParsedAmount * 10 else rawParsedAmount
                    val finalAcc = accountNumber.ifBlank {
                        if (bankName.contains("صادرات")) "54321" else "1234567890"
                    }
                    val finalCard = (existingCard ?: BankCardEntity(
                        accountNumber = finalAcc,
                        bankName = bankName,
                        cardTitle = cardTitle.ifBlank { bankName },
                        cardColorHex = selectedColor,
                        balanceRials = balanceRials,
                        senderContact = senderContact,
                        contactPhone = contactPhone
                    )).copy(
                        accountNumber = finalAcc,
                        bankName = bankName,
                        cardTitle = cardTitle.ifBlank { bankName },
                        cardColorHex = selectedColor,
                        balanceRials = balanceRials,
                        senderContact = senderContact,
                        contactPhone = contactPhone
                    )
                    onSaveCard(finalCard)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_card_button")
            ) {
                Text("ذخیره کارت", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
