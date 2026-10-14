package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TransactionCategory(
    val titleFa: String,
    val titleEn: String,
    val iconName: String,
    val colorHex: Long
) {
    SUPERMARKET("سوپرمارکت", "Supermarket", "ShoppingCart", 0xFF2563EB), // Blue
    FOOD("غذا", "Food", "Fastfood", 0xFFF59E0B), // Amber
    TRANSPORT("حمل و نقل", "Transportation", "DirectionsCar", 0xFF10B981), // Emerald
    BILLS("قبوض، شارژ و اینترنت", "Bills & Internet", "Receipt", 0xFF8B5CF6), // Purple
    INCOME("حقوق و واریز", "Salary & Income", "Paid", 0xFF059669), // Green
    CHARITY("خیریه", "Charity", "Favorite", 0xFF0891B2), // Cyan
    PHARMACY("داروخانه", "Pharmacy", "LocalPharmacy", 0xFFEF4444), // Red
    OTHER("سایر", "Other", "MoreHoriz", 0xFF64748B); // Slate

    val color: Color
        get() = Color(colorHex)

    val icon: ImageVector
        get() = when (this) {
            SUPERMARKET -> Icons.Default.ShoppingCart
            FOOD -> Icons.Default.Fastfood
            TRANSPORT -> Icons.Default.DirectionsCar
            BILLS -> Icons.Default.Receipt
            INCOME -> Icons.Default.Paid
            CHARITY -> Icons.Default.Favorite
            PHARMACY -> Icons.Default.LocalPharmacy
            OTHER -> Icons.Default.MoreHoriz
        }

    companion object {
        val DEFAULT_CATEGORIES = listOf(
            SUPERMARKET, FOOD, TRANSPORT, BILLS, INCOME, CHARITY, PHARMACY
        )

        fun fromName(name: String?): TransactionCategory {
            if (name == null) return OTHER
            val clean = name.trim()
            return entries.firstOrNull { 
                it.name.equals(clean, ignoreCase = true) ||
                it.titleFa == clean ||
                it.titleFa.contains(clean) ||
                clean.contains(it.titleFa) ||
                it.titleEn.equals(clean, ignoreCase = true)
            } ?: when {
                clean.contains("خرید") || clean.contains("فروشگاه") || clean.contains("SHOPPING") -> SUPERMARKET
                clean.contains("خوراک") || clean.contains("کافه") -> FOOD
                clean.contains("درمان") || clean.contains("HEALTH") -> PHARMACY
                clean.contains("قبض") -> BILLS
                clean.contains("واریز") || clean.contains("حقوق") -> INCOME
                clean.contains("خیریه") -> CHARITY
                clean.contains("حمل") -> TRANSPORT
                else -> OTHER
            }
        }

        fun getDisplayName(name: String?): String {
            if (name.isNullOrBlank()) return "سایر"
            // If already Persian title, return it
            entries.firstOrNull { it.titleFa == name }?.let { return it.titleFa }
            // If enum name or matching
            return fromName(name).titleFa
        }

        fun getColor(name: String?): Color {
            if (name.isNullOrBlank()) return Color(0xFF64748B)
            return fromName(name).color
        }

        fun suggestCategory(text: String, isIncome: Boolean): TransactionCategory {
            if (isIncome) return INCOME
            val lower = text.lowercase()
            return when {
                lower.contains("داروخانه") || lower.contains("دارو") || lower.contains("بیمارستان") ||
                        lower.contains("پزشک") || lower.contains("مطب") || lower.contains("دندانپزشک") ||
                        lower.contains("آزمایشگاه") || lower.contains("کلینیک") -> PHARMACY

                lower.contains("خیریه") || lower.contains("محک") || lower.contains("احسان") ||
                        lower.contains("صدقه") || lower.contains("فطریه") || lower.contains("نیکی") ||
                        lower.contains("وقف") -> CHARITY

                lower.contains("اسنپ") || lower.contains("تپسی") || lower.contains("بنزین") ||
                        lower.contains("جایگاه") || lower.contains("کرایه") || lower.contains("مترو") ||
                        lower.contains("تاکسی") || lower.contains("سوخت") -> TRANSPORT

                lower.contains("کافه") || lower.contains("رستوران") || lower.contains("فست فود") ||
                        lower.contains("پیتزا") || lower.contains("ساندویچ") || lower.contains("نانوایی") ||
                        lower.contains("غذا") || lower.contains("اسنپ فود") -> FOOD

                lower.contains("قبض") || lower.contains("شارژ") || lower.contains("همراه اول") ||
                        lower.contains("ایرانسل") || lower.contains("برق") || lower.contains("گاز") ||
                        lower.contains("آب") || lower.contains("اینترنت") || lower.contains("مخابرات") ||
                        lower.contains("کارمزد") || lower.contains("كارمزد") || lower.contains("سرویس پیامک") ||
                        lower.contains("سرويس پيامك") -> BILLS

                lower.contains("سوپر") || lower.contains("هایپر") || lower.contains("کوروش") ||
                        lower.contains("فروشگاه") || lower.contains("میوه") || lower.contains("لبنیات") ||
                        lower.contains("پروتئین") || lower.contains("تره بار") || lower.contains("خرید") ||
                        lower.contains("پایانه") || lower.contains("پايانه") -> SUPERMARKET

                else -> SUPERMARKET
            }
        }
    }
}
