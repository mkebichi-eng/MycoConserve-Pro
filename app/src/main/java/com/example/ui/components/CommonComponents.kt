package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPaidGreenBg
import com.example.ui.theme.StatusPartialAmber
import com.example.ui.theme.StatusPartialAmberBg
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.theme.StatusUnpaidRedBg
import com.example.util.Formatters
import com.example.util.ModePaiement
import kotlin.math.roundToLong

@Composable
fun CanneryHeaderBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4A2C16),
            Color(0xFF6B3F1D),
            Color(0xFF8C5323)
        )
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Subtle decorative preserve jars & mushroom caps on the right
            Canvas(
                modifier = Modifier
                    .size(96.dp, 68.dp)
                    .align(Alignment.CenterEnd)
            ) {
                // Jar 1 silhouette
                drawRoundRect(
                    color = Color(0x26FAF3E8),
                    topLeft = Offset(size.width * 0.12f, size.height * 0.22f),
                    size = Size(size.width * 0.34f, size.height * 0.72f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = Color(0x55E09F3E),
                    topLeft = Offset(size.width * 0.16f, size.height * 0.10f),
                    size = Size(size.width * 0.26f, size.height * 0.12f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Jar 2 silhouette
                drawRoundRect(
                    color = Color(0x33FAF3E8),
                    topLeft = Offset(size.width * 0.54f, size.height * 0.14f),
                    size = Size(size.width * 0.40f, size.height * 0.80f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    color = Color(0x77E09F3E),
                    topLeft = Offset(size.width * 0.59f, size.height * 0.02f),
                    size = Size(size.width * 0.30f, size.height * 0.12f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            Column(modifier = Modifier.fillMaxWidth(0.84f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0x33FAF3E8),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFFADCC0),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "100% HORS-LIGNE • SQLITE ACID",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFADCC0),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFFFFFCF7)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEAD8C3)
                )
            }
        }
    }
}

@Composable
fun StatutVenteBadge(
    statut: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label, icon) = when (statut.uppercase()) {
        "PAYE" -> Quadruple(StatusPaidGreenBg, StatusPaidGreen, "Soldé", Icons.Default.CheckCircle)
        "PARTIEL" -> Quadruple(StatusPartialAmberBg, StatusPartialAmber, "Partiel", Icons.Default.Schedule)
        else -> Quadruple(StatusUnpaidRedBg, StatusUnpaidRed, "Impayé", Icons.Default.ErrorOutline)
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReglementDialog(
    titre: String,
    sousTitreClient: String,
    resteAPayerMax: Double,
    onDismiss: () -> Unit,
    onConfirm: (montant: Double, modePaiement: String, observation: String) -> Unit
) {
    var montantText by remember(resteAPayerMax) {
        mutableStateOf(
            if (resteAPayerMax > 0) resteAPayerMax.roundToLong().toString() else ""
        )
    }
    var selectedMode by remember { mutableStateOf(ModePaiement.ESPECES) }
    var observation by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = titre,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = sousTitreClient,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reste à recouvrer :",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = Formatters.formatDzd(resteAPayerMax),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Quick percentage buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            montantText = (resteAPayerMax * 0.5).roundToLong().toString()
                            errorMsg = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("50%", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = {
                            montantText = resteAPayerMax.roundToLong().toString()
                            errorMsg = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tout solder (100%)", style = MaterialTheme.typography.labelMedium)
                    }
                }

                OutlinedTextField(
                    value = montantText,
                    onValueChange = {
                        montantText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                        errorMsg = null
                    },
                    label = { Text("Montant versé (DA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { msg -> { Text(msg) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_montant_reglement")
                )

                Text(
                    text = "Mode de paiement",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModePaiement.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = observation,
                    onValueChange = { observation = it },
                    label = { Text("Observation (ex: Chèque n°, reçu par...)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_observation_reglement")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = montantText.replace(',', '.').toDoubleOrNull()
                    when {
                        parsed == null || parsed <= 0.0 -> {
                            errorMsg = "Veuillez saisir un montant valide supérieur à 0 DA"
                        }
                        parsed > resteAPayerMax + 0.5 -> {
                            errorMsg = "Le montant dépasse le reste dû (${Formatters.formatDzd(resteAPayerMax)})"
                        }
                        else -> {
                            onConfirm(parsed, selectedMode.code, observation.trim())
                        }
                    }
                },
                modifier = Modifier.testTag("confirm_reglement_button")
            ) {
                Text("Valider l'encaissement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangeDialog(
    initialStartMillis: Long?,
    initialEndMillis: Long?,
    onDismiss: () -> Unit,
    onConfirmRange: (Long, Long) -> Unit
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMillis,
        initialSelectedEndDateMillis = initialEndMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val start = pickerState.selectedStartDateMillis ?: System.currentTimeMillis()
                    val end = pickerState.selectedEndDateMillis ?: start
                    onConfirmRange(start, end)
                }
            ) {
                Text("Appliquer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    ) {
        DateRangePicker(
            state = pickerState,
            title = {
                Text(
                    text = "Sélectionner une période d'analyse",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
        )
    }
}
