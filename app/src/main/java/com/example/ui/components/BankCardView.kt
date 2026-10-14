package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.CurrencyType
import com.example.util.CurrencyUtils

@Composable
fun BankCardView(
    isOverall: Boolean,
    accountNumber: String,
    bankName: String,
    cardTitle: String,
    cardCount: Int,
    cardColorHex: Long,
    balanceRials: Long,
    currency: CurrencyType,
    usePersianDigits: Boolean,
    onToggleCurrency: () -> Unit,
    onEditCard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val gradientColors = if (isOverall) {
        listOf(
            Color(0xFF1E3A8A), // Deep Royal Blue
            Color(0xFF3B82F6), // Vibrant Blue
            Color(0xFF0F172A)  // Deep Slate
        )
    } else {
        val baseColor = Color(cardColorHex)
        listOf(
            baseColor,
            baseColor.copy(alpha = 0.85f),
            Color(0xFF1E293B) // Slate dark blend
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bank_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = gradientColors))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Row: Bank branding / Overall banner + contactless icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isOverall) Icons.Default.Layers else Icons.Default.AccountBalanceWallet,
                                contentDescription = "Bank Logo",
                                tint = Color(0xFFFDE047),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isOverall) "موجودی کل (همه کارت‌ها)" else bankName.ifEmpty { "بانک من" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isOverall) {
                                    val countText = if (usePersianDigits) CurrencyUtils.toPersianDigits("$cardCount") else "$cardCount"
                                    "مجموع $countText کارت بانکی فعال"
                                } else {
                                    cardTitle.ifEmpty { "کارت بانکی" }
                                },
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isOverall && onEditCard != null) {
                            IconButton(
                                onClick = onEditCard,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "ویرایش کارت",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Icon(
                            imageVector = Icons.Default.Contactless,
                            contentDescription = "Contactless",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // EMV Chip & Currency Switcher Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chip graphic
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFD4AF37)) // Gold
                            .border(1.dp, Color(0xFFFDE047), RoundedCornerShape(6.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(width = 20.dp, height = 24.dp)
                                .border(1.dp, Color(0xFF996515), RoundedCornerShape(2.dp))
                        )
                    }

                    // Currency Switcher Button on Card
                    Surface(
                        onClick = onToggleCurrency,
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("currency_toggle_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = "تغییر واحد پول",
                                tint = Color(0xFFFDE047),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currency == CurrencyType.TOMAN) "واحد: تومان" else "واحد: ریال",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Account identification row (NO fake card number)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOverall) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "نمای تجمیعی کلیه حساب‌های بانکی",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else if (accountNumber.isNotBlank() && accountNumber != "ناشناخته") {
                        val accDisplay = if (usePersianDigits) CurrencyUtils.toPersianDigits(accountNumber) else accountNumber
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شماره حساب: $accDisplay",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom row: Balance
                Column {
                    Text(
                        text = if (isOverall) "مجموع موجودی کل حساب‌ها (Overall Balance)" else "موجودی این حساب (Account Balance)",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    AnimatedContent(
                        targetState = Pair(balanceRials, currency),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "balance_anim"
                    ) { (amount, curr) ->
                        val balanceText = CurrencyUtils.formatAmount(
                            amountRials = amount,
                            currency = curr,
                            usePersianDigits = usePersianDigits,
                            includeUnit = true
                        )
                        Text(
                            text = balanceText,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
