package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ClientWithBalanceRow
import com.example.data.local.FormatProduitEntity
import com.example.data.local.PanierItemDraft
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPartialAmber
import com.example.ui.theme.StatusUnpaidRed
import com.example.util.Formatters
import com.example.util.ModePaiement
import kotlin.math.max
import kotlin.math.roundToLong

private enum class TypeEncaissementInitial(val label: String) {
    SOLDE_COMPTANT("Comptant (100%)"),
    ACOMPTE_PARTIEL("Acompte partiel"),
    CREDIT_INTEGRAL("À crédit (0 DA)")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NouvelleVenteSheet(
    clients: List<ClientWithBalanceRow>,
    formats: List<FormatProduitEntity>,
    onClose: () -> Unit,
    onCreateQuickClient: (nom: String, telephone: String, adresse: String, onCreated: (Long) -> Unit) -> Unit,
    onValidateSale: (
        clientId: Long,
        panier: List<PanierItemDraft>,
        acompteInitial: Double,
        modePaiement: String,
        observationAcompte: String,
        notesVente: String
    ) -> Unit
) {
    BackHandler(onBack = onClose)

    var selectedClientId by remember(clients) {
        mutableStateOf(clients.firstOrNull()?.id ?: 0L)
    }
    val quantitesParFormat = remember { mutableStateMapOf<Long, Int>() }
    var typeEncaissement by remember { mutableStateOf(TypeEncaissementInitial.SOLDE_COMPTANT) }
    var acomptePartielText by remember { mutableStateOf("") }
    var modePaiement by remember { mutableStateOf(ModePaiement.ESPECES) }
    var observationAcompte by remember { mutableStateOf("") }
    var notesVente by remember { mutableStateOf("") }
    var showNewClientDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val montantTotal = formats.sumOf { f ->
        val q = quantitesParFormat[f.id] ?: 0
        q * f.prixUnitaire.toDouble()
    }

    val acompteEffectif = when (typeEncaissement) {
        TypeEncaissementInitial.SOLDE_COMPTANT -> montantTotal
        TypeEncaissementInitial.CREDIT_INTEGRAL -> 0.0
        TypeEncaissementInitial.ACOMPTE_PARTIEL ->
            acomptePartielText.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, montantTotal) ?: 0.0
    }

    val resteAPayer = max(0.0, montantTotal - acompteEffectif)

    if (showNewClientDialog) {
        QuickClientCreateDialog(
            onDismiss = { showNewClientDialog = false },
            onConfirm = { nom, tel, adr ->
                onCreateQuickClient(nom, tel, adr) { newId ->
                    selectedClientId = newId
                    showNewClientDialog = false
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Nouvelle Vente de Conserves",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Décrémentation automatique et immédiate du stock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    validationError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total à facturer : ${Formatters.formatDzd(montantTotal)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Versé : ${Formatters.formatDzd(acompteEffectif)} • Reste : ${Formatters.formatDzd(resteAPayer)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (resteAPayer > 0.01) StatusPartialAmber else StatusPaidGreen
                            )
                        }

                        Button(
                            onClick = {
                                val panier = formats.mapNotNull { f ->
                                    val q = quantitesParFormat[f.id] ?: 0
                                    if (q > 0) PanierItemDraft(f, q) else null
                                }
                                when {
                                    selectedClientId == 0L -> {
                                        validationError = "Veuillez sélectionner ou créer un client."
                                    }
                                    panier.isEmpty() -> {
                                        validationError = "Ajoutez au moins un format de conserve au panier."
                                    }
                                    else -> {
                                        onValidateSale(
                                            selectedClientId,
                                            panier,
                                            acompteEffectif,
                                            modePaiement.code,
                                            observationAcompte.trim(),
                                            notesVente.trim()
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_valider_commande_finale")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCartCheckout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Valider la Vente")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Sélection du Client
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. Client destinataire",
                                style = MaterialTheme.typography.titleMedium
                            )
                            OutlinedButton(
                                onClick = { showNewClientDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Nouveau client", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            clients.forEach { client ->
                                val selected = client.id == selectedClientId
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedClientId = client.id
                                        validationError = null
                                    },
                                    label = {
                                        Text(
                                            text = client.nom,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // 2. Sélection des Formats & Quantités
            item {
                Text(
                    text = "2. Formats de conserves & quantités",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(formats, key = { it.id }) { format ->
                val qty = quantitesParFormat[format.id] ?: 0
                val exceedsStock = qty > format.stockActuel

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (qty > 0) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        width = if (qty > 0) 1.5.dp else 1.dp,
                        color = if (exceedsStock) {
                            StatusUnpaidRed
                        } else if (qty > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = format.nomFormat,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = format.contenance,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Prix unitaire figé : ${Formatters.formatDzd(format.prixUnitaire)} • En stock : ${format.stockActuel} u.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (format.stockActuel <= format.seuilAlerte) StatusPartialAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledIconButton(
                                    onClick = {
                                        if (qty > 0) {
                                            quantitesParFormat[format.id] = qty - 1
                                        }
                                    },
                                    enabled = qty > 0,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Diminuer",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = qty.toString(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.width(28.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                FilledIconButton(
                                    onClick = {
                                        quantitesParFormat[format.id] = qty + 1
                                        validationError = null
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("add_qty_format_${format.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Augmenter",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (qty > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (exceedsStock) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = StatusUnpaidRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Attention : dépasse le stock disponible (${format.stockActuel})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusUnpaidRed
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Stock restant après vente : ${format.stockActuel - qty} u.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusPaidGreen
                                    )
                                }
                                Text(
                                    text = "Sous-total : ${Formatters.formatDzd(qty * format.prixUnitaire.toDouble())}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. Modalités de Règlement Initial
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "3. Modalité de règlement à la commande",
                            style = MaterialTheme.typography.titleMedium
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TypeEncaissementInitial.entries.forEach { option ->
                                FilterChip(
                                    selected = typeEncaissement == option,
                                    onClick = {
                                        typeEncaissement = option
                                        if (option == TypeEncaissementInitial.ACOMPTE_PARTIEL && acomptePartielText.isBlank()) {
                                            acomptePartielText = (montantTotal * 0.5).roundToLong().toString()
                                        }
                                    },
                                    label = { Text(option.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }

                        if (typeEncaissement == TypeEncaissementInitial.ACOMPTE_PARTIEL) {
                            OutlinedTextField(
                                value = acomptePartielText,
                                onValueChange = {
                                    acomptePartielText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                                },
                                label = { Text("Montant de l'acompte initial (DA)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (typeEncaissement != TypeEncaissementInitial.CREDIT_INTEGRAL) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            Text(
                                text = "Mode de paiement reçu",
                                style = MaterialTheme.typography.labelLarge
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ModePaiement.entries.forEach { m ->
                                    FilterChip(
                                        selected = modePaiement == m,
                                        onClick = { modePaiement = m },
                                        label = { Text(m.label) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = observationAcompte,
                                onValueChange = { observationAcompte = it },
                                label = { Text("Référence règlement (ex: Chèque n°, Espèces...)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = notesVente,
                            onValueChange = { notesVente = it },
                            label = { Text("Note de livraison / observation commande") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickClientCreateDialog(
    onDismiss: () -> Unit,
    onConfirm: (nom: String, telephone: String, adresse: String) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var nomError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau Client Rapide") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = {
                        nom = it
                        nomError = false
                    },
                    label = { Text("Nom du client / établissement *") },
                    isError = nomError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = telephone,
                    onValueChange = { telephone = it },
                    label = { Text("Téléphone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = adresse,
                    onValueChange = { adresse = it },
                    label = { Text("Wilaya / Adresse") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nom.isBlank()) {
                        nomError = true
                    } else {
                        onConfirm(nom.trim(), telephone.trim(), adresse.trim())
                    }
                }
            ) {
                Text("Créer et sélectionner")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
