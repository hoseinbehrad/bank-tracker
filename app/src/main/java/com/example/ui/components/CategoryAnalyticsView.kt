package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionCategory
import com.example.ui.viewmodel.CategoryExpenseStat
import com.example.util.CurrencyType
import com.example.util.CurrencyUtils

@Composable
fun CategoryAnalyticsView(
    categoryStats: List<CategoryExpenseStat>,
    totalExpenseRials: Long,
    currency: CurrencyType,
    usePersianDigits: Boolean,
    onOpenAddCategory: () -> Unit = {},
    onOpenManageCategories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_analytics_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
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
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Analytics",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "تحلیل هزینه‌ها بر اساس دسته‌بندی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "دسته‌بندی‌های مصارف بانکی",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                val totalFormatted = CurrencyUtils.formatAmount(
                    amountRials = totalExpenseRials,
                    currency = currency,
                    usePersianDigits = usePersianDigits,
                    includeUnit = true
                )
                Text(
                    text = totalFormatted,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action buttons: Add Category & Manage/Remove Categories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenAddCategory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("افزودن دسته‌بندی", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenManageCategories,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف و ویرایش", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (categoryStats.isEmpty() || totalExpenseRials == 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هنوز هزینه‌ای در این بازه زمانی ثبت نشده است",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                // Multi-segment distribution bar
                MultiSegmentBar(categoryStats = categoryStats)

                Spacer(modifier = Modifier.height(18.dp))

                // List of categories
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    categoryStats.forEach { stat ->
                        CategoryStatRow(
                            stat = stat,
                            currency = currency,
                            usePersianDigits = usePersianDigits
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiSegmentBar(
    categoryStats: List<CategoryExpenseStat>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.LightGray.copy(alpha = 0.3f))
    ) {
        categoryStats.forEach { stat ->
            if (stat.percentage > 0) {
                Box(
                    modifier = Modifier
                        .weight(stat.percentage.coerceAtLeast(0.1f))
                        .height(10.dp)
                        .background(Color(stat.colorHex))
                )
            }
        }
    }
}

@Composable
private fun CategoryStatRow(
    stat: CategoryExpenseStat,
    currency: CurrencyType,
    usePersianDigits: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = stat.percentage / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "category_progress"
    )
    val catColor = Color(stat.colorHex)
    val catEnum = TransactionCategory.fromName(stat.categoryName)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = catEnum.icon,
                        contentDescription = stat.categoryName,
                        tint = catColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = stat.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    val countText = if (usePersianDigits) {
                        "${CurrencyUtils.toPersianDigits(stat.count.toString())} تراکنش"
                    } else {
                        "${stat.count} tx"
                    }
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val amountText = CurrencyUtils.formatAmount(
                    amountRials = stat.totalRials,
                    currency = currency,
                    usePersianDigits = usePersianDigits,
                    includeUnit = true
                )
                Text(
                    text = amountText,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )

                val percentNumber = String.format(java.util.Locale.US, "%.1f%%", stat.percentage)
                val percentDisplay = if (usePersianDigits) {
                    CurrencyUtils.toPersianDigits(percentNumber)
                } else {
                    percentNumber
                }
                Text(
                    text = percentDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = catColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = catColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
