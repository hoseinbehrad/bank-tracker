package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FinancialExpenseRed
import com.example.ui.theme.FinancialIncomeGreen
import com.example.util.CurrencyType
import com.example.util.CurrencyUtils

@Composable
fun FinancialSummaryCards(
    incomeRials: Long,
    expenseRials: Long,
    netRials: Long,
    currency: CurrencyType,
    usePersianDigits: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_summary_cards"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Expense Card
        SummaryMetricItem(
            modifier = Modifier.weight(1f),
            title = "برداشت‌ها (هزینه)",
            amountText = CurrencyUtils.formatAmount(
                amountRials = expenseRials,
                currency = currency,
                usePersianDigits = usePersianDigits,
                includeUnit = true
            ),
            icon = Icons.Default.ArrowDownward,
            iconTint = FinancialExpenseRed,
            accentColor = FinancialExpenseRed.copy(alpha = 0.12f)
        )

        // Income Card
        SummaryMetricItem(
            modifier = Modifier.weight(1f),
            title = "واریزها (درآمد)",
            amountText = CurrencyUtils.formatAmount(
                amountRials = incomeRials,
                currency = currency,
                usePersianDigits = usePersianDigits,
                includeUnit = true
            ),
            icon = Icons.Default.ArrowUpward,
            iconTint = FinancialIncomeGreen,
            accentColor = FinancialIncomeGreen.copy(alpha = 0.12f)
        )
    }
}

@Composable
private fun SummaryMetricItem(
    title: String,
    amountText: String,
    icon: ImageVector,
    iconTint: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
            )
        }
    }
}
