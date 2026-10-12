@file:OptIn(ExperimentalMaterial3Api::class)

package com.phabian.mpesa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import com.phabian.mpesa.data.RawSmsEntity
import com.phabian.mpesa.data.ZidiiTransactionType
import com.phabian.mpesa.sms.HistoricalSmsImporter
import com.phabian.mpesa.data.TransactionRepository
import com.phabian.mpesa.data.TransactionEntity
import com.phabian.mpesa.viewmodel.AnalyticsViewModel
import com.phabian.mpesa.data.ZidiiAnalytics
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

private val AnalyticsBlue = Color(0xFF037EF3)
private val PageBackground = Color(0xFF0B0F14)
private val DarkText = Color(0xFFFFFFFF)
private val SecondaryText = Color(0xFF9AA8BA)
private val CardSurface = Color(0xFF151C25)
private val CardElevated = Color(0xFF111B29)
private val IncomeGreen = Color(0xFF37D67A)
private val WithdrawalBlue = Color(0xFF4DA3FF)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val importer: HistoricalSmsImporter
) : ViewModel() {

    val transactions = repository.observeTransactions()
    val zidiiTransactions = repository.observeZidiiTransactions()

    val zidiiSummary = zidiiTransactions

    .map { rows ->
        ZidiiAnalytics.summarize(
            ZidiiAnalytics.classify(rows)
        )
    }

    fun importHistoricalSms() {
        viewModelScope.launch {
            importer.importAll()
        }
    }

    suspend fun getOriginalSms(
        rawMessageId: Long
    ): RawSmsEntity? {
        return repository.getOriginalSms(rawMessageId)
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            
            MaterialTheme(
                colorScheme = androidx.compose.material3.darkColorScheme(
                    primary = AnalyticsBlue,
                    onPrimary = Color.White,
                    background = PageBackground,
                    onBackground = Color.White,
                    surface = CardSurface,
                    onSurface = Color.White,
                    secondary = WithdrawalBlue,
                    onSecondary = Color.White
                )
            ) {
                MpesaAnalyticsApp()
            }
        }
    }
}

@Composable
fun MpesaAnalyticsApp(
    vm: MainViewModel = hiltViewModel()
) {
    val transactions by vm.transactions.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedTransaction by remember {
        mutableStateOf<TransactionEntity?>(null)
    }

    if (selectedTransaction != null) {
        TransactionDetailsScreen(
            transaction = selectedTransaction!!,
            onBack = {
                selectedTransaction = null
            },
            viewModel = vm
        )
        return
    }

    Scaffold(
        containerColor = PageBackground,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = CardSurface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = "Transactions"
                        )
                    },
                    label = { Text("Transactions") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = "Analytics"
                        )
                    },
                    label = { Text("Analytics") }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = "Investments"
                        )
                    },
                    label = { Text("Investments") }
                )
            }
        }
    ) { padding ->

        when (selectedTab) {

            0 -> DashboardScreen(
                transactions = transactions,
                onTransactionClick = {
                    selectedTransaction = it
                },
                onImport = vm::importHistoricalSms,
                modifier = Modifier.padding(padding)
            )

            1 -> TransactionsScreen(
                transactions = transactions,
                onTransactionClick = {
                    selectedTransaction = it
                },
                modifier = Modifier.padding(padding)
            )

            2 -> AnalyticsScreen()

            3 -> InvestmentsScreen(
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
fun DashboardScreen(
    transactions: List<TransactionEntity>,
    onTransactionClick: (TransactionEntity) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestBalance = transactions
        .filter { it.balanceAfter != null }
        .maxByOrNull { it.timestamp }
        ?.balanceAfter

    val expenditure = transactions
        .filter {
            val type = it.transactionType.name
            type == "SEND_MONEY" ||
            type == "BUY_GOODS" ||
            type == "PAYBILL" ||
            type == "WITHDRAW" ||
            type == "AIRTIME_PURCHASE" ||
            type == "CHARGE"
        }
        .sumOf { it.amount }

    val recentTransactions = transactions
        .sortedByDescending { it.timestamp }
        .take(5)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Phabians Analytics",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Text(
                text = "Your financial overview",
                color = SecondaryText,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            BalanceCard(latestBalance)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Transactions",
                    value = formatNumber(transactions.size.toLong()),
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Expenditure",
                    value = formatCurrency(expenditure),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionTitle("Cash Flow")
        }

        item {
            CashFlowCard(transactions)
        }

        item {
            SectionTitle("Recent Transactions")
        }

        items(
            items = recentTransactions,
            key = { it.id }
        ) { transaction ->

            TransactionCard(
                transaction = transaction,
                onClick = {
                    onTransactionClick(transaction)
                }
            )
        }

        item {
            Button(
                onClick = onImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("Refresh M-Pesa transactions")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Composable
fun BalanceCard(balance: Long?) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {

            Text(
                text = "CURRENT M-PESA BALANCE",
                color = Color(0xFF737373),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = balance?.let {
                    formatCurrency(it)
                } ?: "Not available",
                color = Color(0xFF037EF3),
                fontSize = 31.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Based on latest recorded transaction",
                color = Color(0xFF737373),
                fontSize = 12.sp
            )
        }
    }
}


@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        color = DarkText,
        modifier = Modifier.padding(
            top = 8.dp,
            bottom = 2.dp
        )
    )
}

@Composable
fun CashFlowCard(
    transactions: List<TransactionEntity>
) {
    val received = transactions
        .filter {
            it.transactionType.name == "RECEIVE_MONEY" ||
            it.transactionType.name == "DEPOSIT" ||
            it.transactionType.name == "LOAN_DISBURSEMENT"
        }
        .sumOf { it.amount }

    val spent = transactions
        .filter {
            it.transactionType.name == "SEND_MONEY" ||
            it.transactionType.name == "BUY_GOODS" ||
            it.transactionType.name == "PAYBILL" ||
            it.transactionType.name == "WITHDRAW" ||
            it.transactionType.name == "AIRTIME_PURCHASE"
        }
        .sumOf { it.amount }

    val net = received - spent

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardSurface
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = "All recorded transactions",
                color = SecondaryText,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                CashFlowMetric(
                    title = "Received",
                    value = formatCurrency(received)
                )

                CashFlowMetric(
                    title = "Spent",
                    value = formatCurrency(spent)
                )

                CashFlowMetric(
                    title = "Net",
                    value = formatCurrency(net)
                )
            }
        }
    }
}

@Composable
fun CashFlowMetric(
    title: String,
    value: String
) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            color = SecondaryText
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = WithdrawalBlue
        )
    }
}

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier.fillMaxSize()
    ) {

        Text(
            text = "Transactions",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "${transactions.size} recorded transactions",
            color = SecondaryText,
            fontSize = 13.sp,
            modifier = Modifier.padding(
                horizontal = 16.dp
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(
                items = transactions.sortedByDescending { it.timestamp },
                key = { it.id }
            ) { transaction ->

                TransactionCard(
                    transaction = transaction,
                    onClick = {
                        onTransactionClick(transaction)
                    }
                )
            }
        }
    }
}

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {

    val type = transaction.transactionType.name

    val incoming =
        type == "RECEIVE_MONEY" ||
        type == "DEPOSIT" ||
        type == "LOAN_DISBURSEMENT"

    val displayName =
        transaction.counterparty?.takeIf { it.isNotBlank() }
            ?: transaction.phoneNumber?.takeIf { it.isNotBlank() }
            ?: readableType(type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardSurface
        )
    ) {

        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (incoming) {
                            AnalyticsBlue.copy(alpha = 0.12f)
                        } else {
                            WithdrawalBlue.copy(alpha = 0.12f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector =
                        if (incoming) {
                            Icons.Default.ArrowDownward
                        } else {
                            Icons.Default.ArrowUpward
                        },
                    contentDescription = null,
                    tint = if (incoming) {
                        AnalyticsBlue
                    } else {
                        DarkText
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = readableType(type),
                    color = SecondaryText,
                    fontSize = 12.sp
                )

                Text(
                    text = formatDateTime(transaction.timestamp),
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {

                Text(
                    text = formatCurrency(transaction.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Text(
                    text = if (incoming) "IN" else "OUT",
                    color = if (incoming) {
                        AnalyticsBlue
                    } else {
                        SecondaryText
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TransactionDetailsScreen(
    transaction: TransactionEntity,
    onBack: () -> Unit,
    viewModel: MainViewModel
) {

    var originalSms by remember {
        mutableStateOf<RawSmsEntity?>(null)
    }

    var originalSmsLoaded by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(transaction.rawMessageId) {
        originalSmsLoaded = false

        originalSms = transaction.rawMessageId?.let { rawId ->
            viewModel.getOriginalSms(rawId)
        }

        originalSmsLoaded = true
    }
    val type = transaction.transactionType.name

    Scaffold(
        containerColor = PageBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("Transaction Details")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PageBackground
                )
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(22.dp)
                    ) {

                        Text(
                            text = readableType(type),
                            color = Color(0xFF737373),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = formatCurrency(transaction.amount),
                            color = Color(0xFF037EF3),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = formatDateTime(transaction.timestamp),
                            color = Color(0xFF737373),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            item {
                DetailSection(
                    title = "Transaction Information"
                ) {

                    DetailRow(
                        "Transaction code",
                        transaction.transactionCode ?: "Not available"
                    )

                    DetailRow(
                        "Type",
                        readableType(type)
                    )

                    DetailRow(
                        "Counterparty",
                        transaction.counterparty ?: "Not available"
                    )

                    DetailRow(
                        "Phone number",
                        transaction.phoneNumber ?: "Not available"
                    )

                    DetailRow(
                        "Date",
                        formatDate(transaction.timestamp)
                    )

                    DetailRow(
                        "Time",
                        formatTime(transaction.timestamp)
                    )
                }
            }

            item {
                DetailSection(
                    title = "M-Pesa Information"
                ) {

                    DetailRow(
                        "Amount",
                        formatCurrency(transaction.amount)
                    )

                    DetailRow(
                        "Balance after",
                        transaction.balanceAfter?.let {
                            formatCurrency(it)
                        } ?: "Not available"
                    )

                    DetailRow(
                        "Category",
                        transaction.category ?: "Not categorised"
                    )

                    DetailRow(
                        "Parser confidence",
                        "${(transaction.confidence * 100).toInt()}%"
                    )
                }
            }

            item {
                DetailSection(title = "Original SMS") {

                    when {
                        !originalSmsLoaded -> {
                            Text(
                                text = "Loading original SMS...",
                                color = SecondaryText,
                                fontSize = 13.sp
                            )
                        }

                        originalSms == null -> {
                            Text(
                                text = "Original SMS is unavailable for this transaction.",
                                color = SecondaryText,
                                fontSize = 13.sp
                            )
                        }

                        else -> {
                            originalSms?.let { sms ->

                                Text(
                                    text = "Sender",
                                    color = SecondaryText,
                                    fontSize = 12.sp
                                )

                                Text(
                                    text = sms.sender,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )

                                Text(
                                    text = "Received",
                                    color = SecondaryText,
                                    fontSize = 12.sp
                                )

                                Text(
                                    text = formatDateTime(sms.receivedAt),
                                    color = Color.White,
                                    fontSize = 14.sp
                                )

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )

                                Text(
                                    text = "Message",
                                    color = SecondaryText,
                                    fontSize = 12.sp
                                )

                                Spacer(
                                    modifier = Modifier.height(6.dp)
                                )

                                Text(
                                    text = sms.body,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardSurface
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = label,
            color = SecondaryText,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            color = DarkText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(

                onClick = {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = selectedPeriod.start
                        add(Calendar.MONTH, -1)
                    }

                    viewModel.selectMonth(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH)
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Previous month"
                )
            }

            Text(
                text = selectedPeriod.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = selectedPeriod.start
                        add(Calendar.MONTH, 1)
                    }

                    viewModel.selectMonth(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "Next month"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Analytics summary
        summary?.let { analytics ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                AnalyticsCard(
                    title = "Income",
                    value = formatCurrency(analytics.income),
                    modifier = Modifier.weight(1f)
                )

                AnalyticsCard(
                    title = "Expenditure",
                    value = formatCurrency(analytics.income),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                AnalyticsCard(
                    title = "Net Cash Flow",
                    value = formatCurrency(analytics.income),
                    modifier = Modifier.weight(1f)
                )

                AnalyticsCard(
                    title = "Transactions",
                    value = analytics.transactionCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                AnalyticsCard(
                    title = "Opening Balance",
                    value = analytics.openingBalance?.let {
                        formatCurrency(it)
                    } ?: "N/A",
                    modifier = Modifier.weight(1f)
                )

                AnalyticsCard(
                    title = "Closing Balance",
                    value = analytics.closingBalance?.let {
                        formatCurrency(it)
                    } ?: "N/A",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Transactions
        Text(
            text = "Transactions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (transactions.isEmpty()) {

            Text(
                text = "No transactions for this period.",
                style = MaterialTheme.typography.bodyMedium
            )

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                items(
                    items = transactions,
                    key = { it.id }
                ) { transaction ->

                    TransactionRow(
                        transaction = transaction
                    )
                }
            }
        }
    }
}


@Composable
fun AnalyticsCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TransactionRow(
    transaction: TransactionEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = transaction.counterparty
                        ?: transaction.transactionType.name,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = readableType(transaction.transactionType.name),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = formatCurrency(transaction.amount),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AnalyticsMetricCard(
    title: String,
    value: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardSurface
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = title,
                color = SecondaryText,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun InvestmentsScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val rows by viewModel.zidiiTransactions.collectAsState(
        initial = emptyList()
    )

    val transactions = remember(rows) {
        ZidiiAnalytics.classify(rows)
    }

    val summary = remember(transactions) {
        ZidiiAnalytics.summarize(transactions)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F14)),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Investments",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Text(
                text = "Your Zidii Money Market Fund activity",
                color = Color.LightGray,
                fontSize = 14.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111B29)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Net contributions",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )

                    Text(
                        text = ZidiiAnalytics.formatCurrency(
                            summary.netContributions
                        ),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Total invested minus total withdrawn",
                        color = Color(0xFF9AA8BA),
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ZidiiMetricCard(
                    title = "Total invested",
                    value = ZidiiAnalytics.formatCurrency(
                        summary.totalInvested
                    ),
                    modifier = Modifier.weight(1f),
                    valueColor = Color(0xFF37D67A)
                )

                ZidiiMetricCard(
                    title = "Total withdrawn",
                    value = ZidiiAnalytics.formatCurrency(
                        summary.totalWithdrawn
                    ),
                    modifier = Modifier.weight(1f),
                    valueColor = Color(0xFF4DA3FF)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ZidiiMetricCard(
                    title = "Deposits",
                    value = summary.depositCount.toString(),
                    modifier = Modifier.weight(1f),
                    valueColor = Color.White
                )

                ZidiiMetricCard(
                    title = "Withdrawals",
                    value = summary.withdrawalCount.toString(),
                    modifier = Modifier.weight(1f),
                    valueColor = Color.White
                )
            }
        }

        item {
            Text(
                text = "Zidii transaction history",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (transactions.isEmpty()) {
            item {
                Text(
                    text = "No matching Zidii transactions found yet.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }
        } else {
            items(
                items = transactions,
                key = { transaction ->
                    transaction.transactionCode
                }
            ) { transaction ->

                val formattedDate = remember(
                    transaction.timestamp
                ) {
                    java.text.SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        java.util.Locale.UK
                    ).format(
                        java.util.Date(transaction.timestamp)
                    )
                }

                val isDeposit =
                    transaction.type == ZidiiTransactionType.DEPOSIT

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF151C25)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isDeposit) {
                                    "Zidii deposit"
                                } else {
                                    "Zidii withdrawal"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = transaction.transactionCode,
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )

                            Text(
                                text = formattedDate,
                                color = Color(0xFF9AA8BA),
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = (if (isDeposit) "+" else "−") +
                                ZidiiAnalytics.formatCurrency(
                                    transaction.amount
                                ).removePrefix("KES "),
                            color = if (isDeposit) {
                                Color(0xFF37D67A)
                            } else {
                                Color(0xFF4DA3FF)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ZidiiMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF151C25)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = Color.LightGray,
                fontSize = 12.sp
            )

            Text(
                text = value,
                color = valueColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatCurrency(amount: Long): String {
    val negative = amount < 0
    val absolute = kotlin.math.abs(amount)
    val wholeShillings = absolute / 100
    val cents = absolute % 100

    val formattedWhole = NumberFormat
        .getNumberInstance(Locale("en", "KE"))
        .format(wholeShillings)

    val sign = if (negative) "-" else ""

    return "KES $sign$formattedWhole.${cents.toString().padStart(2, '0')}"
}

private fun formatNumber(value: Long): String {
    return NumberFormat
        .getNumberInstance(Locale.US)
        .format(value)
}

private fun formatDateTime(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy • h:mm a",
        Locale.UK
    ).format(Date(timestamp))
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMMM yyyy",
        Locale.UK
    ).format(Date(timestamp))
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat(
        "h:mm a",
        Locale.UK
    ).format(Date(timestamp))
}

private fun readableType(type: String): String {
    return type
        .lowercase()
        .split("_")
        .joinToString(" ") {
            it.replaceFirstChar { char ->
                char.uppercase()
            }
        }
}