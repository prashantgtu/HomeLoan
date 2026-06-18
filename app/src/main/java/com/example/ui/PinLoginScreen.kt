package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.data.PinManager

@Composable
fun PinLoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val pinManager = remember { PinManager(context) }
    
    val isSetupRequired = remember { !pinManager.isPinSet() }
    
    var pinInput by remember { mutableStateOf("") }
    var tempPin by remember { mutableStateOf("") }
    var isConfirmStage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val maxPinLength = 4

    val titleText = when {
        isSetupRequired && !isConfirmStage -> "Create 4-Digit PIN"
        isSetupRequired && isConfirmStage -> "Confirm 4-Digit PIN"
        else -> "Enter 4-Digit PIN"
    }

    val subtitleText = when {
        isSetupRequired && !isConfirmStage -> "Set a PIN to secure your loan details offline."
        isSetupRequired && isConfirmStage -> "Re-enter your PIN to confirm."
        else -> "Secure offline access to Home Loan Pro."
    }

    val handleDigitClick: (Char) -> Unit = { digit ->
        if (pinInput.length < maxPinLength) {
            errorMessage = ""
            val newInput = pinInput + digit
            pinInput = newInput
            
            if (newInput.length == maxPinLength) {
                if (isSetupRequired) {
                    if (!isConfirmStage) {
                        tempPin = newInput
                        isConfirmStage = true
                        pinInput = ""
                    } else {
                        if (newInput == tempPin) {
                            pinManager.savePin(newInput)
                            onLoginSuccess()
                        } else {
                            errorMessage = "PINs do not match. Start over."
                            tempPin = ""
                            isConfirmStage = false
                            pinInput = ""
                        }
                    }
                } else {
                    if (pinManager.verifyPin(newInput)) {
                        onLoginSuccess()
                    } else {
                        errorMessage = "Incorrect PIN. Try again."
                        pinInput = ""
                    }
                }
            }
        }
    }

    val handleDeleteClick: () -> Unit = {
        if (pinInput.isNotEmpty()) {
            pinInput = pinInput.dropLast(1)
        }
    }

    val onClearClick: () -> Unit = {
        pinInput = ""
        errorMessage = ""
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Pin Status Indicators and Error Messages
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                // PIN dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until maxPinLength) {
                        val isFilled = i < pinInput.length
                        val color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        val size = if (isFilled) 18.dp else 14.dp
                        Box(
                            modifier = Modifier
                                .size(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(size)
                                    .background(color, shape = CircleShape)
                            )
                        }
                    }
                }

                // Error Message text
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    // Invisible spacer to maintain layout height
                    Text(
                        text = "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                    )
                }
            }

            // Keypad Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val keys = listOf(
                    listOf('1', '2', '3'),
                    listOf('4', '5', '6'),
                    listOf('7', '8', '9'),
                    listOf('C', '0', 'D')
                )

                for (row in keys) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        for (key in row) {
                            OutlinedButton(
                                onClick = {
                                    when (key) {
                                        'C' -> onClearClick()
                                        'D' -> handleDeleteClick()
                                        else -> handleDigitClick(key)
                                    }
                                },
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                when (key) {
                                    'C' -> {
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    'D' -> {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = key.toString(),
                                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
