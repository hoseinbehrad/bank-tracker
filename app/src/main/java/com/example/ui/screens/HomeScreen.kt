package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.BankCardEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.AddCategoryDialog
import com.example.ui.components.BankCardView
import com.example.ui.components.CategoryAnalyticsView
import com.example.ui.components.CategorySelectionPopupDialog
import com.example.ui.components.FinancialSummaryCards
import com.example.ui.components.ManageCategoriesDialog
import com.example.ui.components.TransactionItemView
import com.example.ui.dialogs.AddEditCardDialog
import com.example.ui.dialogs.CategoryPickerSheet
import com.example.ui.dialogs.DataBackupDialog
import com.example.ui.dialogs.ManualTransactionDialog
import com.example.ui.dialogs.NewMonthSheetDialog
import com.example.ui.dialogs.SmsDetailDialog
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.TimeFilter
import com.example.util.CurrencyType
import com.example.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FinanceViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Permission state check
    var hasSmsPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsPermissions = permissions[Manifest.permission.RECEIVE_SMS] == true
        if (hasSmsPermissions) {
            Toast.makeText(context, "دسترسی پیامک فعال شد. پیامک‌های بانکی خودکار خوانده می‌شوند.", Toast.LENGTH_LONG).show()
        }
    }

    // Modal dialog states
    var showManualAdd by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<BankCardEntity?>(null) }
    var showNewMonthDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var selectedTransactionForCategory by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedTransactionForDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Transactions, 1 = Analytics
    var showSearchBar by remember { mutableStateOf(false) }

    // Toast feedback for inbox scanning with deduplication
    LaunchedEffect(uiState.scanResult) {
        uiState.scanResult?.let { result ->
            val newCount = result.newTransactions.size
            val dupCount = result.duplicateCount
            if (newCount > 0) {
                val msg = if (dupCount > 0) {
                    "$newCount پیامک جدید ثبت شد ($dupCount پیامک تکراری نادیده گرفته شد)"
                } else {
                    "$newCount پیامک بانکی با موفقیت ثبت شد"
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            } else {
                val msg = if (dupCount > 0) {
                    "پیامک جدیدی یافت نشد ($dupCount پیامک تکراری قبلاً در برنامه ثبت شده بود)"
                } else {
                    "پیامک بانکی جدیدی یافت نشد"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            viewModel.dismissScanResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "حسابدار بانکی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Active Month Sheet Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = uiState.activeMonthSheet,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Backup & Restore Dialog Trigger
                    IconButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier.testTag("backup_data_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "پشتیبان‌گیری و بازیابی",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Scan SMS button placed up beside search and 123
                    Surface(
                        onClick = {
                            if (!hasSmsPermissions) {
                                val perms = mutableListOf(
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.READ_CONTACTS
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(perms.toTypedArray())
                            } else {
                                viewModel.scanInbox(context)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .testTag("top_scan_sms_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isScanningInbox) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = "اسکن پیامک",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isScanningInbox) "اسکن..." else "اسکن پیامک",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Persian / English digits toggle (123)
                    IconButton(
                        onClick = { viewModel.toggleDigitFormat() },
                        modifier = Modifier.testTag("digits_switch_icon")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = if (uiState.usePersianDigits) "۱۲۳" else "123",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Toggle search
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(
                            imageVector = if (showSearchBar) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = "جستجو"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // Manual add FAB
            FloatingActionButton(
                onClick = { showManualAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("manual_add_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن دستی")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // SMS Permission Banner if not granted
            if (!hasSmsPermissions) {
                item {
                    Surface(
                        onClick = {
                            val perms = mutableListOf(
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_SMS
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sms_permission_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "فعال‌سازی خواندن خودکار پیامک بانک",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "برای خواندن و ثبت خودکار پیامک‌های بانکی بدون تکرار، اینجا بزنید",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            } else {
                // Active status pill with Scan Inbox option
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "دریافت خودکار پیامک فعال است",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Scan Inbox Button (with deduplication)
                            Surface(
                                onClick = { viewModel.scanInbox(context) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("scan_inbox_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (uiState.isScanningInbox) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (uiState.isScanningInbox) "در حال بررسی..." else "اسکن پیامک‌ها",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar when open
            if (showSearchBar) {
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("جستجو در یادداشت‌ها، مبالغ، دسته‌بندی‌ها...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_text_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            // SECTION 1: Monthly Balance Sheet Header & Actions
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "دوره مالی (Balance Sheet):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Start New Month Button
                            Surface(
                                onClick = { showNewMonthDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.testTag("start_new_month_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "شروع ماه جدید",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Month Sheet Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uiState.availableMonths.forEach { month ->
                                val isSelected = uiState.selectedMonthFilter == month
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectMonthFilter(month) },
                                    label = {
                                        Text(
                                            text = if (month == uiState.activeMonthSheet) "$month (جاری)" else month,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            // "All Months" option
                            FilterChip(
                                selected = uiState.selectedMonthFilter == null,
                                onClick = { viewModel.selectMonthFilter(null) },
                                label = {
                                    Text(
                                        text = "همه دوره‌ها",
                                        fontSize = 11.sp,
                                        fontWeight = if (uiState.selectedMonthFilter == null) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // SECTION 2: Multi-Card Selector & Overall Tab
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "انتخاب کارت / حساب:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Add Card Button
                        Surface(
                            onClick = {
                                cardToEdit = null
                                showAddCardDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.testTag("add_card_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "افزودن کارت",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal Card Tabs: Overall + Individual Cards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Overall Tab Chip
                        val isOverallSelected = uiState.isOverallView
                        Surface(
                            onClick = { viewModel.selectOverall() },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isOverallSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isOverallSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.testTag("overall_card_tab")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (isOverallSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "همه کارت‌ها (Overall)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOverallSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val overallFormatted = CurrencyUtils.formatAmount(
                                        amountRials = uiState.overallBalanceRials,
                                        currency = uiState.currency,
                                        usePersianDigits = uiState.usePersianDigits,
                                        includeUnit = true
                                    )
                                    Text(
                                        text = overallFormatted,
                                        fontSize = 10.sp,
                                        color = if (isOverallSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // Individual Cards
                        uiState.cards.forEach { card ->
                            val isCardSelected = uiState.selectedCard?.id == card.id
                            val cardColor = Color(card.cardColorHex)
                            Surface(
                                onClick = { viewModel.selectCard(card) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isCardSelected) cardColor else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isCardSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.testTag("card_tab_${card.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isCardSelected) Color.White else cardColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = card.cardTitle.ifBlank { card.bankName },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCardSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val cardBalFormatted = CurrencyUtils.formatAmount(
                                            amountRials = card.balanceRials,
                                            currency = uiState.currency,
                                            usePersianDigits = uiState.usePersianDigits,
                                            includeUnit = true
                                        )
                                        Text(
                                            text = cardBalFormatted,
                                            fontSize = 10.sp,
                                            color = if (isCardSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: The Main Bank Card View (Without fake card number)
            item {
                val currentCard = uiState.selectedCard
                BankCardView(
                    isOverall = uiState.isOverallView,
                    accountNumber = currentCard?.accountNumber ?: "",
                    bankName = currentCard?.bankName ?: "بانک ملت",
                    cardTitle = currentCard?.cardTitle ?: "همه کارت‌ها",
                    cardCount = uiState.cards.size,
                    cardColorHex = currentCard?.cardColorHex ?: 0xFFDC2626L,
                    balanceRials = uiState.totalBalanceRials,
                    currency = uiState.currency,
                    usePersianDigits = uiState.usePersianDigits,
                    onToggleCurrency = { viewModel.toggleCurrency() },
                    onEditCard = if (currentCard != null) {
                        {
                            cardToEdit = currentCard
                            showAddCardDialog = true
                        }
                    } else null
                )
            }

            // Financial Summary Cards (Income vs Expense)
            item {
                FinancialSummaryCards(
                    incomeRials = uiState.totalIncomeRials,
                    expenseRials = uiState.totalExpenseRials,
                    netRials = uiState.netBalanceChangeRials,
                    currency = uiState.currency,
                    usePersianDigits = uiState.usePersianDigits
                )
            }

            // Navigation Tabs (Transactions vs Analytics)
            item {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("main_tabs")
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تراکنش‌ها", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تحلیل و نمودار", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                }
            }

            // Content depending on Active Tab
            if (activeTab == 1) {
                // Analytics Tab
                item {
                    CategoryAnalyticsView(
                        categoryStats = uiState.categoryStats,
                        totalExpenseRials = uiState.totalExpenseRials,
                        currency = uiState.currency,
                        usePersianDigits = uiState.usePersianDigits,
                        onOpenAddCategory = { showAddCategoryDialog = true },
                        onOpenManageCategories = { showManageCategoriesDialog = true }
                    )
                }
            } else {
                // Transactions Tab
                if (uiState.transactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "تراکنشی در این دوره یافت نشد",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "با دریافت پیامک جدید یا دکمه «آزمایش پیامک»، تراکنش‌ها ثبت خواهند شد",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = uiState.transactions,
                        key = { it.id }
                    ) { transaction ->
                        TransactionItemView(
                            transaction = transaction,
                            currency = uiState.currency,
                            usePersianDigits = uiState.usePersianDigits,
                            onChangeCategory = {
                                selectedTransactionForCategory = transaction
                            },
                            onViewSms = {
                                selectedTransactionForDetail = transaction
                            },
                            onDelete = {
                                viewModel.deleteTransaction(transaction.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Category Picker Bottom Sheet
    selectedTransactionForCategory?.let { tx ->
        CategoryPickerSheet(
            transaction = tx,
            categories = uiState.categories,
            sheetState = sheetState,
            onDismiss = { selectedTransactionForCategory = null },
            onCategorySelected = { newCat, newNote ->
                viewModel.updateTransactionCategory(tx, newCat)
                if (newNote != tx.note) {
                    viewModel.updateTransactionNote(tx, newNote)
                }
            },
            onOpenAddCategory = {
                showAddCategoryDialog = true
            }
        )
    }

    // Raw SMS Detail Dialog
    selectedTransactionForDetail?.let { tx ->
        SmsDetailDialog(
            transaction = tx,
            currency = uiState.currency,
            usePersianDigits = uiState.usePersianDigits,
            onDismiss = { selectedTransactionForDetail = null }
        )
    }

    // Manual Transaction Dialog
    if (showManualAdd) {
        ManualTransactionDialog(
            currency = uiState.currency,
            usePersianDigits = uiState.usePersianDigits,
            onDismiss = { showManualAdd = false },
            onSave = { type, amountRials, category, note, bank ->
                val acc = uiState.selectedCard?.accountNumber ?: "نقدی / کارت"
                viewModel.addManualTransaction(
                    accountNumber = acc,
                    bankName = bank,
                    type = type,
                    amountRials = amountRials,
                    categoryName = category,
                    note = note
                )
                Toast.makeText(context, "تراکنش ثبت شد", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add / Edit Card Dialog
    if (showAddCardDialog) {
        AddEditCardDialog(
            existingCard = cardToEdit,
            currency = uiState.currency,
            usePersianDigits = uiState.usePersianDigits,
            onDismiss = {
                showAddCardDialog = false
                cardToEdit = null
            },
            onSaveCard = { card ->
                viewModel.saveCard(card)
                Toast.makeText(context, "کارت بانکی ذخیره شد", Toast.LENGTH_SHORT).show()
            },
            onDeleteCard = { card ->
                viewModel.deleteCard(card)
                Toast.makeText(context, "کارت حذف شد", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // New Month Balance Sheet Dialog
    if (showNewMonthDialog) {
        NewMonthSheetDialog(
            currentMonth = uiState.activeMonthSheet,
            onDismiss = { showNewMonthDialog = false },
            onConfirmStartNewMonth = { newMonthName ->
                viewModel.startNewMonthSheet(newMonthName)
                Toast.makeText(context, "دوره مالی $newMonthName با موجودی فعلی آغاز شد", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Backup & Restore Dialog (Export / Import)
    if (showBackupDialog) {
        DataBackupDialog(
            cards = uiState.cards,
            transactions = uiState.allTransactions,
            activeMonth = uiState.activeMonthSheet,
            usePersianDigits = uiState.usePersianDigits,
            onDismiss = { showBackupDialog = false },
            onRestoreData = { payload, replaceAll ->
                viewModel.restoreBackup(payload, replaceAll)
            }
        )
    }

    // Category Selection Popup Dialog on SMS arrival or notification tap
    uiState.pendingCategorizationTx?.let { tx ->
        CategorySelectionPopupDialog(
            transaction = tx,
            categories = uiState.categories,
            onSelectCategory = { chosenCategory ->
                viewModel.updateTransactionCategory(tx, chosenCategory)
                viewModel.dismissPendingCategorization()
                Toast.makeText(context, "دسته‌بندی «$chosenCategory» ثبت شد", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                viewModel.dismissPendingCategorization()
            },
            onOpenAddCategory = {
                showAddCategoryDialog = true
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onAddCategory = { name, colorHex ->
                viewModel.addCategory(name, colorHex)
                Toast.makeText(context, "دسته‌بندی «$name» افزوده شد", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAddCategoryDialog = false }
        )
    }

    // Manage / Remove Categories Dialog
    if (showManageCategoriesDialog) {
        ManageCategoriesDialog(
            categories = uiState.categories,
            onRemoveCategory = { cat ->
                viewModel.removeCategory(cat)
                Toast.makeText(context, "دسته‌بندی «${cat.name}» حذف شد", Toast.LENGTH_SHORT).show()
            },
            onOpenAdd = {
                showAddCategoryDialog = true
            },
            onDismiss = { showManageCategoriesDialog = false }
        )
    }
}
