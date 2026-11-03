package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.local.FormatProduitEntity
import com.example.data.local.MouvementStockWithFormatRow
import com.example.data.local.StockValorisationSummary
import com.example.ui.components.CompactScreenHeader
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.PurgeAllDataDialog
import com.example.ui.theme.ChanterelleGoldSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPartialAmber
import com.example.ui.theme.StatusUnpaidRed
import com.example.util.Formatters
import kotlin.math.roundToLong

@Composable
fun StockScreen(
    formats: List<FormatProduitEntity>,
    valorisation: StockValorisationSummary,
    mouvementsRecents: List<MouvementStockWithFormatRow>,
    onSaveFormat: (
        id: Long,
        nomFormat: String,
        contenance: String,
        prixUnitaire: Double,
        stockInitial: Int,
        seuilAlerte: Int
    ) -> Unit,
    onDeleteFormat: (Long) -> Unit,
    onAddProductionBatch: (formatId: Long, quantite: Int, motif: String) -> Unit,
    onRecordAdjustmentOrLoss: (formatId: Long, quantiteDelta: Int, estCasseOuPerte: Boolean, motif: String) -> Unit,
    onDeleteMouvementStock: (Long) -> Unit,
    onPurgeAllData: (conserverFormats: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var formatToEdit by remember { mutableStateOf<FormatProduitEntity?>(null) }
    var formatForProduction by remember { mutableStateOf<FormatProduitEntity?>(null) }
    var formatForAdjustment by remember { mutableStateOf<FormatProduitEntity?>(null) }
    var formatToDelete by remember { mutableStateOf<FormatProduitEntity?>(null) }
    var mouvementToDelete by remember { mutableStateOf<MouvementStockWithFormatRow?>(null) }
    var showPurgeDialog by remember { mutableStateOf(false) }

    if (showPurgeDialog) {
        PurgeAllDataDialog(
            onDismiss = { showPurgeDialog = false },
            onPurgeEverything = {
                AppDatabase.markSeedDismissed(context)
                onPurgeAllData(false)
                showPurgeDialog = false
            },
            onPurgeHistoryKeepFormats = {
                AppDatabase.markSeedDismissed(context)
                onPurgeAllData(true)
                showPurgeDialog = false
            }
        )
    }

    formatToDelete?.let { fmt ->
        ConfirmDeleteDialog(
            titre = "Supprimer le format ?",
            message = "Voulez-vous supprimer le format « ${fmt.nomFormat} (${fmt.contenance}) » du catalogue ?",
            onDismiss = { formatToDelete = null },
            onConfirm = {
                onDeleteFormat(fmt.id)
                formatToDelete = null
            }
        )
    }

    mouvementToDelete?.let { mv ->
        val actionStockInfo = if (mv.quantiteDelta >= 0) {
            "Le stock de « ${mv.nomFormat} » sera réduit de ${mv.quantiteDelta} u."
        } else {
            "Le stock de « ${mv.nomFormat} » sera augmenté de ${-mv.quantiteDelta} u."
        }
        ConfirmDeleteDialog(
            titre = "Supprimer ce lot / mouvement ?",
            message = "Motif : ${mv.motif}\n$actionStockInfo",
            onDismiss = { mouvementToDelete = null },
            onConfirm = {
                onDeleteMouvementStock(mv.id)
                mouvementToDelete = null
            }
        )
    }

    if (showFormatDialog) {
        FormatProduitDialog(
            initialFormat = formatToEdit,
            onDismiss = {
                showFormatDialog = false
                formatToEdit = null
            },
            onConfirm = { id, nom, contenance, prix, stockInit, seuil ->
                onSaveFormat(id, nom, contenance, prix, stockInit, seuil)
                showFormatDialog = false
                formatToEdit = null
            }
        )
    }

    formatForProduction?.let { fmt ->
        ProductionBatchDialog(
            format = fmt,
            onDismiss = { formatForProduction = null },
            onConfirm = { qty, motif ->
                onAddProductionBatch(fmt.id, qty, motif)
                formatForProduction = null
            }
        )
    }

    formatForAdjustment?.let { fmt ->
        StockAdjustmentDialog(
            format = fmt,
            onDismiss = { formatForAdjustment = null },
            onConfirm = { delta, estCasse, motif ->
                onRecordAdjustmentOrLoss(fmt.id, delta, estCasse, motif)
                formatForAdjustment = null
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // 1. En-tête ultra-compact sur 1 seule ligne
        CompactScreenHeader(
            title = "Stock",
            subtitleBadge = "${Formatters.formatDzd(valorisation.valeurStockTotal)} • ${valorisation.totalUnitesStock} u.",
            subtitleColor = MaterialTheme.colorScheme.primary,
            addButtonLabel = "Format",
            addButtonTestTag = "btn_nouveau_format",
            onAddClick = {
                formatToEdit = null
                showFormatDialog = true
            },
            onPurgeAllClick = { showPurgeDialog = true }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Sélecteur d'onglets compact sur 1 ligne (Formats vs Lots/Mouvements) + indicateur d'alerte
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    label = {
                        Text(
                            text = "Formats (${formats.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    label = {
                        Text(
                            text = "Lots & Mouvements (${mouvementsRecents.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            if (valorisation.nbFormatsAlerte > 0) {
                Surface(
                    color = StatusUnpaidRed.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${valorisation.nbFormatsAlerte} alerte(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusUnpaidRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Liste occupant plus de 80% de l'écran
        if (selectedSubTab == 0) {
            if (formats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun format en stock. Touchez « + Format » en haut à droite.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("formats_stock_list"),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(formats, key = { it.id }) { format ->
                        FormatStockCard(
                            format = format,
                            onEdit = {
                                formatToEdit = format
                                showFormatDialog = true
                            },
                            onDelete = { formatToDelete = format },
                            onAddProduction = { formatForProduction = format },
                            onAdjustOrLoss = { formatForAdjustment = format }
                        )
                    }
                }
            }
        } else {
            if (mouvementsRecents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun lot ou mouvement de stock enregistré.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("mouvements_stock_list"),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(mouvementsRecents, key = { it.id }) { mv ->
                        MouvementStockRowCard(
                            mv = mv,
                            onDelete = { mouvementToDelete = mv }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatStockCard(
    format: FormatProduitEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddProduction: () -> Unit,
    onAdjustOrLoss: () -> Unit
) {
    val isLowStock = format.estEnAlerteStock
    val valeurLigne = format.valeurMarchandeStockDzd
    val gaugeRatio = (format.stockActuel.toFloat() / (format.seuilAlerte * 3f).coerceAtLeast(1f))
        .coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("format_card_${format.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isLowStock) 1.2.dp else 1.dp,
            color = if (isLowStock) StatusUnpaidRed.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = format.nomFormat,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = format.contenance,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier ce format",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_delete_format_${format.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Supprimer ce format",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PU: ${Formatters.formatDzd(format.prixUnitaire)} • Val: ${Formatters.formatDzd(valeurLigne)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isLowStock) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Alerte stock bas",
                            tint = StatusUnpaidRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "${format.stockActuel} u. (seuil ${format.seuilAlerte})",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isLowStock) StatusUnpaidRed else StatusPaidGreen
                    )
                }
            }

            LinearProgressIndicator(
                progress = { gaugeRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (isLowStock) StatusUnpaidRed else StatusPaidGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Compact Action Buttons Row (32.dp height)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onAddProduction,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(32.dp)
                        .testTag("btn_lot_production_${format.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Factory,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Lot Production", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onAdjustOrLoss,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .testTag("btn_ajustement_casse_${format.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Casse / Ajust.", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun MouvementStockRowCard(
    mv: MouvementStockWithFormatRow,
    onDelete: () -> Unit
) {
    val isPositive = mv.quantiteDelta >= 0
    val badgeColor = when (mv.typeMouvement) {
        "PRODUCTION" -> StatusPaidGreen
        "VENTE" -> ChanterelleGoldSecondary
        "PERTE_CASSE" -> StatusUnpaidRed
        else -> StatusPartialAmber
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (mv.typeMouvement == "PRODUCTION") Icons.Default.Inventory2 else Icons.Default.History,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Column {
                    Text(
                        text = "${mv.nomFormat} (${mv.contenance})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${Formatters.formatDateShort(mv.dateMouvement)} • ${mv.motif}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isPositive) "+${mv.quantiteDelta} u." else "${mv.quantiteDelta} u.",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isPositive) StatusPaidGreen else StatusUnpaidRed
                    )
                    Text(
                        text = "Stock: ${mv.stockApres}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("btn_delete_mouvement_${mv.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Supprimer ce lot ou mouvement",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormatProduitDialog(
    initialFormat: FormatProduitEntity?,
    onDismiss: () -> Unit,
    onConfirm: (
        id: Long,
        nomFormat: String,
        contenance: String,
        prixUnitaire: Double,
        stockInitial: Int,
        seuilAlerte: Int
    ) -> Unit
) {
    var nomFormat by remember(initialFormat) { mutableStateOf(initialFormat?.nomFormat ?: "") }
    var contenance by remember(initialFormat) { mutableStateOf(initialFormat?.contenance ?: "1kg") }
    var prixText by remember(initialFormat) {
        mutableStateOf(initialFormat?.prixUnitaire?.toDouble()?.roundToLong()?.toString() ?: "")
    }
    var stockInitText by remember(initialFormat) {
        mutableStateOf(initialFormat?.stockActuel?.toString() ?: "50")
    }
    var seuilText by remember(initialFormat) {
        mutableStateOf(initialFormat?.seuilAlerte?.toString() ?: "15")
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val contenancesSuggestions = listOf("150g", "350g", "500g", "1kg", "2kg", "2.5kg", "5kg")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialFormat == null) "Créer un Format de Conserve" else "Modifier le Format")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nomFormat,
                    onValueChange = {
                        nomFormat = it
                        errorMsg = null
                    },
                    label = { Text("Désignation (ex: Bocal Cèpes, Bidon Pro) *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_nom_format")
                )

                Text(
                    text = "Contenance / Conditionnement",
                    style = MaterialTheme.typography.labelMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    contenancesSuggestions.forEach { preset ->
                        FilterChip(
                            selected = contenance.equals(preset, ignoreCase = true),
                            onClick = { contenance = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = contenance,
                    onValueChange = { contenance = it },
                    label = { Text("Contenance personnalisée (ex: 150g, 2.5kg) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = prixText,
                    onValueChange = {
                        prixText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                        errorMsg = null
                    },
                    label = { Text("Prix de vente forfaitaire unitaire (DA) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_prix_unitaire_format")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (initialFormat == null) {
                        OutlinedTextField(
                            value = stockInitText,
                            onValueChange = { stockInitText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Stock initial") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = seuilText,
                        onValueChange = { seuilText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Seuil alerte") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                errorMsg?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prix = prixText.replace(',', '.').toDoubleOrNull()
                    when {
                        nomFormat.isBlank() || contenance.isBlank() -> {
                            errorMsg = "Veuillez renseigner le nom et la contenance du format."
                        }
                        prix == null || prix <= 0.0 -> {
                            errorMsg = "Veuillez saisir un prix unitaire valide en DA."
                        }
                        else -> {
                            onConfirm(
                                initialFormat?.id ?: 0L,
                                nomFormat.trim(),
                                contenance.trim(),
                                prix,
                                stockInitText.toIntOrNull() ?: 0,
                                seuilText.toIntOrNull() ?: 10
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("btn_save_format")
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun ProductionBatchDialog(
    format: FormatProduitEntity,
    onDismiss: () -> Unit,
    onConfirm: (quantite: Int, motif: String) -> Unit
) {
    var quantiteText by remember { mutableStateOf("30") }
    var motif by remember { mutableStateOf("Lot Stérilisation du jour") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Entrée de Lot de Production") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${format.nomFormat} (${format.contenance}) • Stock actuel : ${format.stockActuel} u.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = quantiteText,
                    onValueChange = { quantiteText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Quantité produite à ajouter (+)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_quantite_production")
                )
                OutlinedTextField(
                    value = motif,
                    onValueChange = { motif = it },
                    label = { Text("N° de lot / Session de mise en conserve") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = quantiteText.toIntOrNull() ?: 0
                    if (q > 0) {
                        onConfirm(q, motif.trim())
                    }
                },
                modifier = Modifier.testTag("btn_confirm_production")
            ) {
                Text("Ajouter au Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun StockAdjustmentDialog(
    format: FormatProduitEntity,
    onDismiss: () -> Unit,
    onConfirm: (quantiteDelta: Int, estCasseOuPerte: Boolean, motif: String) -> Unit
) {
    var estCasse by remember { mutableStateOf(true) }
    var quantiteText by remember { mutableStateOf("1") }
    var motif by remember { mutableStateOf("Casse bocal / contrôle qualité") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustement de Stock / Casse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${format.nomFormat} (${format.contenance}) • Stock actuel : ${format.stockActuel} u.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = estCasse,
                        onClick = {
                            estCasse = true
                            motif = "Casse bocal / perte stérilisation"
                        },
                        label = { Text("Sortie Casse / Perte (-)") }
                    )
                    FilterChip(
                        selected = !estCasse,
                        onClick = {
                            estCasse = false
                            motif = "Correction inventaire (+)"
                        },
                        label = { Text("Ajout Inventaire (+)") }
                    )
                }
                OutlinedTextField(
                    value = quantiteText,
                    onValueChange = { quantiteText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Nombre d'unités") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = motif,
                    onValueChange = { motif = it },
                    label = { Text("Motif de l'ajustement") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = quantiteText.toIntOrNull() ?: 0
                    if (q > 0) {
                        val delta = if (estCasse) -q else q
                        onConfirm(delta, estCasse, motif.trim())
                    }
                }
            ) {
                Text("Valider l'ajustement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
