package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.VenteResumeRow
import com.example.ui.components.ReglementDialog
import com.example.ui.components.StatutVenteBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.viewmodel.ExpandedVenteDetails
import com.example.ui.viewmodel.VentesUiState
import com.example.util.Formatters
import com.example.util.ModePaiement
import com.example.util.StatutVenteFilter

@Composable
fun VentesScreen(
    uiState: VentesUiState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (StatutVenteFilter) -> Unit,
    onToggleExpandVente: (Long) -> Unit,
    onLoadNextPage: () -> Unit,
    onRecordInstallment: (venteId: Long, montant: Double, modePaiement: String, observation: String) -> Unit,
    onOpenNewSaleSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var venteForReglement by remember { mutableStateOf<VenteResumeRow?>(null) }

    // Trigger Infinite Scroll when reaching last 3 items
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore, uiState.canLoadMore, uiState.isLoadingPage) {
        if (shouldLoadMore && uiState.canLoadMore && !uiState.isLoadingPage) {
            onLoadNextPage()
        }
    }

    venteForReglement?.let { targetVente ->
        ReglementDialog(
            titre = "Enregistrer un acompte",
            sousTitreClient = "Vente #${targetVente.id} • ${targetVente.clientNom}",
            resteAPayerMax = targetVente.resteAPayer,
            onDismiss = { venteForReglement = null },
            onConfirm = { montant, mode, obs ->
                onRecordInstallment(targetVente.id, montant, mode, obs)
                venteForReglement = null
            }
        )
    }

    // Group sales chronologically into Aujourd'hui, Cette semaine, Ce mois, Plus ancien
    val groupedSales = remember(uiState.ventesChargees) {
        val now = System.currentTimeMillis()
        uiState.ventesChargees.groupBy { Formatters.getTemporalGroupLabel(it.dateVente, now) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Bar: Title + New Sale CTA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Historique des Ventes",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "${uiState.totalMatchingCount} commande(s) • Tri antéchronologique",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onOpenNewSaleSheet,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_nouvelle_vente")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nouvelle Vente")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar by Client Name
        OutlinedTextField(
            value = uiState.rechercheClient,
            onValueChange = onSearchChange,
            placeholder = { Text("Rechercher un client par nom…") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = if (uiState.rechercheClient.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Effacer")
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_ventes_client_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Status FilterChips (Tous, Soldés, Partiels, Impayés)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatutVenteFilter.entries.forEach { filtre ->
                val selected = uiState.statutFiltre == filtre
                FilterChip(
                    selected = selected,
                    onClick = { onFilterChange(filtre) },
                    label = {
                        Text(
                            text = filtre.label,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_statut_${filtre.code.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.ventesChargees.isEmpty() && !uiState.isLoadingPage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Aucune vente ne correspond aux filtres sélectionnés.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("ventes_paginated_list"),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedSales.forEach { (groupTitle, salesInGroup) ->
                    item(key = "header_$groupTitle") {
                        TemporalSectionHeader(
                            title = groupTitle,
                            count = salesInGroup.size
                        )
                    }

                    items(
                        items = salesInGroup,
                        key = { it.id }
                    ) { vente ->
                        val isExpanded = uiState.expandedVenteIds.contains(vente.id)
                        val details = uiState.detailsParVente[vente.id]
                        ExpandableVenteCard(
                            vente = vente,
                            isExpanded = isExpanded,
                            details = details,
                            onToggleExpand = { onToggleExpandVente(vente.id) },
                            onOpenAddPayment = { venteForReglement = vente },
                            onCallClient = { Formatters.dialClient(context, vente.clientTelephone) }
                        )
                    }
                }

                if (uiState.isLoadingPage) {
                    item(key = "loading_more_footer") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemporalSectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = "$count vente(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ExpandableVenteCard(
    vente: VenteResumeRow,
    isExpanded: Boolean,
    details: ExpandedVenteDetails?,
    onToggleExpand: () -> Unit,
    onOpenAddPayment: () -> Unit,
    onCallClient: () -> Unit
) {
    Card(
        onClick = onToggleExpand,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("vente_card_${vente.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Client Name + Status Badge + Expand Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vente.clientNom,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Facture #${vente.id} • ${Formatters.formatDateTime(vente.dateVente)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatutVenteBadge(statut = vente.statut)
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Replier" else "Déplier",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Financial Summary Row (Total Facturé | Payé | Reste Dû)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Facturé",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatDzd(vente.montantTotal),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Déjà Réglé",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatDzd(vente.montantPaye),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = StatusPaidGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Reste Dû",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatDzd(vente.resteAPayer),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (vente.resteAPayer > 0.01) StatusUnpaidRed else StatusPaidGreen
                        )
                    }
                }
            }

            // Expanded Details Section
            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                if (details == null || details.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    }
                } else {
                    // 1. Lignes de Vente (Formats commandés avec prix_unitaire_fige)
                    Text(
                        text = "Formats de conserves facturés",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    details.lignes.forEach { ligne ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ligne.quantite} × ${ligne.nomFormat} (${ligne.contenance})",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${Formatters.formatDzd(ligne.prixUnitaireFige)} → ${Formatters.formatDzd(ligne.sousTotal)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. Historique des Règlements Échelonnés sur cette vente
                    Text(
                        text = "Versements & Acomptes enregistrés (${details.reglements.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (details.reglements.isEmpty()) {
                        Text(
                            text = "Aucun versement consigné (vente à crédit intégral).",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusUnpaidRed
                        )
                    } else {
                        details.reglements.forEach { reg ->
                            val modeLabel = ModePaiement.fromCode(reg.modePaiement).label
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${Formatters.formatDateShort(reg.dateReglement)} • $modeLabel",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (reg.observation.isNotBlank()) {
                                        Text(
                                            text = reg.observation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "+ ${Formatters.formatDzd(reg.montant)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = StatusPaidGreen
                                )
                            }
                        }
                    }

                    // 3. Action Buttons inside Expanded Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCallClient,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Appeler")
                        }

                        if (vente.resteAPayer > 0.01) {
                            Button(
                                onClick = onOpenAddPayment,
                                modifier = Modifier
                                    .weight(1.6f)
                                    .testTag("btn_add_acompte_vente_${vente.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enregistrer un acompte")
                            }
                        }
                    }
                }
            }
        }
    }
}
