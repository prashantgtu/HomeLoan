package com.example

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.text.NumberFormat
import java.util.Locale
import java.util.Calendar
import android.text.format.DateFormat
import com.example.data.AppDatabase
import com.example.data.LoanRepository
import com.example.ui.PinLoginScreen

fun formatMonthYear(startDateMs: Long, monthOffset: Int): String {
    val cal = Calendar.getInstance().apply { timeInMillis = startDateMs }
    cal.add(Calendar.MONTH, monthOffset - 1)
    return DateFormat.format("MMM yyyy", cal).toString()
}

fun getCurrentEmiInfo(summary: com.example.LoanSummary, startDateMs: Long): Pair<String, Double> {
    val now = Calendar.getInstance()
    val start = Calendar.getInstance().apply { timeInMillis = startDateMs }
    val diffYear = now.get(Calendar.YEAR) - start.get(Calendar.YEAR)
    val diffMonth = diffYear * 12 + now.get(Calendar.MONTH) - start.get(Calendar.MONTH)
    val monthIndex = diffMonth + 1
    if (monthIndex <= 0 || summary.schedule.isEmpty()) return formatMonthYear(startDateMs, 1) to summary.expectedEmi
    if (monthIndex > summary.schedule.size) return formatMonthYear(startDateMs, summary.schedule.size) to 0.0
    return formatMonthYear(startDateMs, monthIndex) to summary.schedule[monthIndex - 1].emi
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartDateSelector(startDateMs: Long, onDateSelected: (Long) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    
    if (showDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateMs)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    OutlinedButton(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Loan Start Date: ${formatMonthYear(startDateMs, 1)}", color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val repository = remember { LoanRepository(AppDatabase.getDatabase(context).loanDao()) }
    val viewModel: LoanViewModel = viewModel(factory = LoanViewModelFactory(repository))
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "pin_lock") {
        composable("pin_lock") {
            PinLoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("pin_lock") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(viewModel, { navController.navigate("schedule") })
        }
        composable("schedule") {
            ScheduleScreen(viewModel, { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: LoanViewModel, onNavigateSchedule: () -> Unit) {
    val input by viewModel.inputFlow.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val prepayments by viewModel.prepayments.collectAsState()
    val rateChanges by viewModel.rateChanges.collectAsState()
    val savedLoans by viewModel.savedLoans.collectAsState()

    var showPrepaymentDialog by remember { mutableStateOf(false) }
    var showRateChangeDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Home Loan Pro", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text("SCENARIO: STANDARD HOME PURCHASE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefault() }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "New loan scenario")
                    }
                    IconButton(onClick = { showLoadDialog = true }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Folder, contentDescription = "Load saved loan")
                    }
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Save, contentDescription = "Save current loan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            if (summary != null) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateSchedule,
                    icon = { Icon(androidx.compose.material.icons.Icons.Default.FileDownload, contentDescription = "Schedule") },
                    text = { Text("Amortization") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            InputCard(input, viewModel::updateInput)

            Spacer(modifier = Modifier.height(16.dp))

            SummaryCard(summary, input)

            Spacer(modifier = Modifier.height(16.dp))

            if (summary != null && summary!!.schedule.isNotEmpty()) {
                ChartCard(summary!!)
                Spacer(modifier = Modifier.height(16.dp))
            }

            EventsSection(
                input = input,
                prepayments = prepayments,
                rateChanges = rateChanges,
                onAddPrepayment = { showPrepaymentDialog = true },
                onAddRateChange = { showRateChangeDialog = true },
                onRemovePrepayment = viewModel::removePrepayment,
                onRemoveRateChange = viewModel::removeRateChange
            )
            Spacer(modifier = Modifier.height(64.dp))
        }
    }

    if (showPrepaymentDialog) {
        PrepaymentDialog(
            onDismiss = { showPrepaymentDialog = false },
            onConfirm = { month, amount, strategy ->
                viewModel.addPrepayment(month, amount, strategy)
                showPrepaymentDialog = false
            }
        )
    }

    if (showRateChangeDialog) {
        RateChangeDialog(
            onDismiss = { showRateChangeDialog = false },
            onConfirm = { month, rate ->
                viewModel.addRateChange(month, rate)
                showRateChangeDialog = false
            }
        )
    }

    if (showSaveDialog) {
        SaveLoanDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.saveCurrentLoan(name)
                showSaveDialog = false
            }
        )
    }

    if (showLoadDialog) {
        LoadLoanDialog(
            loans = savedLoans,
            onDismiss = { showLoadDialog = false },
            onLoad = { loan ->
                viewModel.loadLoan(loan)
                showLoadDialog = false
            },
            onDelete = { id -> viewModel.deleteLoan(id) }
        )
    }
}

@Composable
fun InputCard(input: LoanInput, onUpdate: ((LoanInput) -> LoanInput) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Loan Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = if (input.principal == 0.0) "" else input.principal.toString(),
                onValueChange = { s -> onUpdate { it.copy(principal = s.toDoubleOrNull() ?: 0.0) } },
                label = { Text("Principal Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("principal_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = if (input.annualInterestRate == 0.0) "" else input.annualInterestRate.toString(),
                    onValueChange = { s -> onUpdate { it.copy(annualInterestRate = s.toDoubleOrNull() ?: 0.0) } },
                    label = { Text("Interest Rate (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("rate_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = if (input.tenureMonths == 0) "" else input.tenureMonths.toString(),
                    onValueChange = { s -> onUpdate { it.copy(tenureMonths = s.toIntOrNull() ?: 0) } },
                    label = { Text("Tenure (Months)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("tenure_input"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = if (input.extraMonthlyPayment == 0.0) "" else input.extraMonthlyPayment.toString(),
                onValueChange = { s -> onUpdate { it.copy(extraMonthlyPayment = s.toDoubleOrNull() ?: 0.0) } },
                label = { Text("Extra Monthly Payment") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("extra_monthly_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Prepayment Strategy", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = input.strategy == RepaymentStrategy.REDUCE_TENURE,
                    onClick = { onUpdate { it.copy(strategy = RepaymentStrategy.REDUCE_TENURE) } }
                )
                Text("Reduce Tenure")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = input.strategy == RepaymentStrategy.REDUCE_EMI,
                    onClick = { onUpdate { it.copy(strategy = RepaymentStrategy.REDUCE_EMI) } }
                )
                Text("Reduce EMI")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            StartDateSelector(startDateMs = input.startDateMs, onDateSelected = { s -> onUpdate { it.copy(startDateMs = s) } })
        }
    }
}

@Composable
fun SummaryCard(summary: LoanSummary?, input: LoanInput) {
    if (summary == null) return
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 0
    val (currentDateStr, currentEmi) = getCurrentEmiInfo(summary, input.startDateMs)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Current EMI ($currentDateStr)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(format.format(currentEmi) + " / mo", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.Top)
                ) {
                    Text("ACTIVE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(12.dp)
                ) {
                    Text("TOTAL INTEREST", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(format.format(summary.totalInterest), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                }
                Column(
                    modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(12.dp)
                ) {
                    Text("TOTAL PAYMENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(format.format(summary.totalPayment), style = MaterialTheme.typography.titleMedium)
                }
            }
            if (summary.savingsInterest > 0 || summary.savingsTenureMonths > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                     shape = RoundedCornerShape(20.dp),
                     elevation = CardDefaults.cardElevation(0.dp)
                ) {
                     Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                         Text("Prepayment Impact", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                         Spacer(modifier = Modifier.height(8.dp))
                         Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                             Column {
                                 Text("INTEREST SAVED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                                 Text(format.format(summary.savingsInterest), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                             }
                             Column(horizontalAlignment = Alignment.End) {
                                 Text("TENURE REDUCED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                                 Text("${summary.savingsTenureMonths} Months", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                             }
                         }
                     }
                }
            }
        }
    }
}

@Composable
fun ChartCard(summary: LoanSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().height(250.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Text("Balance Trend", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            
            val maxBalance = summary.schedule.maxOfOrNull { it.openBalance } ?: 1.0
            val months = summary.schedule.size
            
            if (months > 0) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    val pathInfo = Path()
                    var first = true
                    summary.schedule.forEachIndexed { index, row ->
                        val x = (index.toFloat() / months.toFloat()) * w
                        val y = h - ((row.closeBalance / maxBalance).toFloat() * h)
                        if (first) {
                            pathInfo.moveTo(x, y)
                            first = false
                        } else {
                            pathInfo.lineTo(x, y)
                        }
                    }
                    
                    drawPath(
                        pathInfo,
                        color = Color(0xFF1976D2),
                        style = Stroke(width = 4f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Box(modifier = Modifier.size(12.dp).align(Alignment.CenterVertically)) {
                        Canvas(modifier = Modifier.fillMaxSize()) { drawRect(Color(0xFF1976D2)) }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remaining Balance", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun EventsSection(
    input: LoanInput,
    prepayments: List<Prepayment>,
    rateChanges: List<RateChange>,
    onAddPrepayment: () -> Unit,
    onAddRateChange: () -> Unit,
    onRemovePrepayment: (Prepayment) -> Unit,
    onRemoveRateChange: (RateChange) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Lump-sum Prepayments", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onAddPrepayment, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Prepayment")
                }
            }
            if (prepayments.isEmpty()) {
                Text("No prepayments added.", style = MaterialTheme.typography.bodySmall)
            } else {
                val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                format.maximumFractionDigits = 0
                prepayments.forEach { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${formatMonthYear(input.startDateMs, p.month)}: ${format.format(p.amount)} (${if (p.strategy == RepaymentStrategy.REDUCE_TENURE) "Reduce Tenure" else "Reduce EMI"})", style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { onRemovePrepayment(p) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Interest Rate Revisions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onAddRateChange, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Rate Change")
                }
            }
            if (rateChanges.isEmpty()) {
                Text("No rate changes added.", style = MaterialTheme.typography.bodySmall)
            } else {
                rateChanges.forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${formatMonthYear(input.startDateMs, r.month)}: ${r.newInterestRate}%")
                        IconButton(onClick = { onRemoveRateChange(r) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrepaymentDialog(onDismiss: () -> Unit, onConfirm: (Int, Double, RepaymentStrategy) -> Unit) {
    var monthStr by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var strategy by remember { mutableStateOf(RepaymentStrategy.REDUCE_TENURE) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Prepayment") },
        text = {
            Column {
                OutlinedTextField(
                    value = monthStr,
                    onValueChange = { monthStr = it },
                    label = { Text("Month (e.g. 12)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Strategy for this payment", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = strategy == RepaymentStrategy.REDUCE_TENURE,
                        onClick = { strategy = RepaymentStrategy.REDUCE_TENURE }
                    )
                    Text("Reduce Tenure", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = strategy == RepaymentStrategy.REDUCE_EMI,
                        onClick = { strategy = RepaymentStrategy.REDUCE_EMI }
                    )
                    Text("Reduce EMI", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val m = monthStr.toIntOrNull()
                    val a = amountStr.toDoubleOrNull()
                    if (m != null && a != null) {
                        onConfirm(m, a, strategy)
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RateChangeDialog(onDismiss: () -> Unit, onConfirm: (Int, Double) -> Unit) {
    var monthStr by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Rate Change") },
        text = {
            Column {
                OutlinedTextField(
                    value = monthStr,
                    onValueChange = { monthStr = it },
                    label = { Text("Month") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text("New Interest Rate (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val m = monthStr.toIntOrNull()
                    val r = rateStr.toDoubleOrNull()
                    if (m != null && r != null) {
                        onConfirm(m, r)
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SaveLoanDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var nameStr by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Loan Scenario") },
        text = {
            OutlinedTextField(
                value = nameStr,
                onValueChange = { nameStr = it },
                label = { Text("Scenario Name") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (nameStr.isNotBlank()) onSave(nameStr) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LoadLoanDialog(
    loans: List<com.example.data.LoanEntity>,
    onDismiss: () -> Unit,
    onLoad: (com.example.data.LoanEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Load Saved Loan") },
        text = {
            if (loans.isEmpty()) {
                Text("No saved loans found.")
            } else {
                LazyColumn {
                    items(loans) { loan ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f).clickable { onLoad(loan) }.padding(8.dp)) {
                                Text(loan.name, style = MaterialTheme.typography.titleMedium)
                                Text("Principal: ${NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }.format(loan.principal)}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onDelete(loan.id) }) {
                                Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: LoanViewModel, onBack: () -> Unit) {
    val summary by viewModel.summary.collectAsState()
    val input by viewModel.inputFlow.collectAsState()
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Amortization Schedule") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { summary?.let { exportScheduleToCsv(context, it) } }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (summary == null || summary!!.schedule.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No data to display")
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Header row
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp).horizontalScroll(rememberScrollState())) {
                    Text("Date", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelMedium)
                    Text("EMI", modifier = Modifier.width(70.dp), style = MaterialTheme.typography.labelMedium)
                    Text("Principal", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelMedium)
                    Text("Interest", modifier = Modifier.width(70.dp), style = MaterialTheme.typography.labelMedium)
                    Text("Balance", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.labelMedium)
                    Text("Prepay", modifier = Modifier.width(70.dp), style = MaterialTheme.typography.labelMedium)
                }
                HorizontalDivider()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(summary!!.schedule) { row ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp).horizontalScroll(rememberScrollState())) {
                            Text(formatMonthYear(input.startDateMs, row.month), modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall)
                            Text("%.1f".format(row.emi), modifier = Modifier.width(70.dp), style = MaterialTheme.typography.bodySmall)
                            Text("%.1f".format(row.principalPaid), modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall)
                            Text("%.1f".format(row.interestPaid), modifier = Modifier.width(70.dp), style = MaterialTheme.typography.bodySmall)
                            Text("%.1f".format(row.closeBalance), modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                            Text("%.1f".format(row.prepayment), modifier = Modifier.width(70.dp), style = MaterialTheme.typography.bodySmall, color = if (row.prepayment > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
