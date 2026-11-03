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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FormatProduitEntity
import com.example.data.local.MouvementStockWithFormatRow
import com.example.data.local.StockValorisationSummary
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
    onAddProductionBatch: (formatId: Long, quantite: Int, motif: String) -> Unit,
    onRecordAdjustmentOrLoss: (formatId: Long, quantiteDelta: Int, estCasseOuPerte: Boolean, motif: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var formatToEdit by remember { mutableStateOf<FormatProduitEntity?>(null) }
    var formatForProduction by remember { mutableStateOf<FormatProduitEntity?>(null) }
    var formatForAdjustment by remember { mutableStateOf<FormatProduitEntity?>(null) }

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
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Header + Create Format CTA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Formats & Stock Conserves",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Catalogue libre, lots de production & suivi de casse",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = {
                    formatToEdit = null
                    showFormatDialog = true
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_nouveau_format")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nouveau Format")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stock Valuation Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Valeur Marchande Totale du Stock",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Formatters.formatDzd(valorisation.valeurStockTotal),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${valorisation.totalUnitesStock} unités en stock",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (valorisation.nbFormatsAlerte > 0) {
                        Text(
                            text = "${valorisation.nbFormatsAlerte} format(s) sous le seuil",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusUnpaidRed,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Tous les stocks sont optimaux",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusPaidGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Formats (${formats.size})") }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Journal des Mouvements") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedSubTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("formats_stock_list"),
                contentPadding = PaddingValues(bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(formats, key = { it.id }) { format ->
                    FormatStockCard(
                        format = format,
                        onEdit = {
                            formatToEdit = format
                            showFormatDialog = true
                        },
                        onAddProduction = { formatForProduction = format },
                        onAdjustOrLoss = { formatForAdjustment = format }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("mouvements_stock_list"),
                contentPadding = PaddingValues(bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mouvementsRecents, key = { it.id }) { mv ->
                    MouvementStockRowCard(mv = mv)
                }
            }
        }
    }
}

@Composable
private fun FormatStockCard(
    format: FormatProduitEntity,
    onEdit: () -> Unit,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isLowStock) 1.5.dp else 1.dp,
            color = if (isLowStock) StatusUnpaidRed.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = format.nomFormat,
                            style = MaterialTheme.typography.titleMedium,
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Prix forfaitaire : ${Formatters.formatDzd(format.prixUnitaire)} • Valeur : ${Formatters.formatDzd(valeurLigne)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifier ce format",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Stock Level & Alert Threshold Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isLowStock) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Alerte stock bas",
                                tint = StatusUnpaidRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "En stock : ${format.stockActuel} unités",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isLowStock) StatusUnpaidRed else StatusPaidGreen
                        )
                    }
                    Text(
                        text = "Seuil d'alerte : ${format.seuilAlerte} u.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LinearProgressIndicator(
                    progress = { gaugeRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (isLowStock) StatusUnpaidRed else StatusPaidGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Quick Stock Movement Actions: Production Batch + Adjustment/Loss
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onAddProduction,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("btn_lot_production_${format.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Factory,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Lot Production", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onAdjustOrLoss,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_ajustement_casse_${format.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Casse / Ajust.", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun MouvementStockRowCard(mv: MouvementStockWithFormatRow) {
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (mv.typeMouvement == "PRODUCTION") Icons.Default.Inventory2 else Icons.Default.History,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column {
                    Text(
                        text = "${mv.nomFormat} (${mv.contenance})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${Formatters.formatDateTime(mv.dateMouvement)} • ${mv.motif}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isPositive) "+${mv.quantiteDelta} u." else "${mv.quantiteDelta} u.",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isPositive) StatusPaidGreen else StatusUnpaidRed
                )
                Text(
                    text = "Stock: ${mv.stockApres}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    contenancesSuggestions.forEach { preset ->
                        FilterChip(
                            selected = contenance.equals(preset, ignoreCase = true),
                            onClick = { contenance = preset },
                            label = { Text(preset) }
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
