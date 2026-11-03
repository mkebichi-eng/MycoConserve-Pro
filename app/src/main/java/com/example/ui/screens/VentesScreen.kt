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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
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
import com.example.data.local.ReglementEntity
import com.example.data.local.VenteResumeRow
import com.example.ui.components.CompactScreenHeader
import com.example.ui.components.ConfirmDeleteDialog
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
    onDeleteVente: (Long) -> Unit,
    onDeleteReglement: (Long) -> Unit,
    onOpenNewSaleSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var venteForReglement by remember { mutableStateOf<VenteResumeRow?>(null) }
    var venteToDelete by remember { mutableStateOf<VenteResumeRow?>(null) }
    var reglementToDelete by remember { mutableStateOf<ReglementEntity?>(null) }

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

    venteToDelete?.let { target ->
        ConfirmDeleteDialog(
            titre = "Supprimer la vente #${target.id} ?",
            message = "Client : ${target.clientNom} (${Formatters.formatDzd(target.montantTotal)})\nLes quantités facturées seront automatiquement restituées au stock.",
            onDismiss = { venteToDelete = null },
            onConfirm = {
                onDeleteVente(target.id)
                venteToDelete = null
            }
        )
    }

    reglementToDelete?.let { reg ->
        ConfirmDeleteDialog(
            titre = "Supprimer cet acompte ?",
            message = "Montant : ${Formatters.formatDzd(reg.montant)}\nLe reste à payer de la vente #${reg.venteId} sera automatiquement recalculé.",
            onDismiss = { reglementToDelete = null },
            onConfirm = {
                onDeleteReglement(reg.id)
                reglementToDelete = null
            }
        )
    }

    val groupedSales = remember(uiState.ventesChargees) {
        val now = System.currentTimeMillis()
        uiState.ventesChargees.groupBy { Formatters.getTemporalGroupLabel(it.dateVente, now) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // 1. En-tête ultra-compact sur 1 ligne avec petit bouton "+ Vente"
        CompactScreenHeader(
            title = "Ventes",
            subtitleBadge = "${uiState.totalMatchingCount} cmd.",
            addButtonLabel = "Vente",
            addButtonTestTag = "btn_nouvelle_vente",
            onAddClick = onOpenNewSaleSheet
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Barre de recherche compacte
        OutlinedTextField(
            value = uiState.rechercheClient,
            onValueChange = onSearchChange,
            placeholder = { Text("Rechercher un client…", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = if (uiState.rechercheClient.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Effacer",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_ventes_client_input")
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Filtres rapides par statut (Tous, Soldés, Partiels, Impayés)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatutVenteFilter.entries.forEach { filtre ->
                val selected = uiState.statutFiltre == filtre
                FilterChip(
                    selected = selected,
                    onClick = { onFilterChange(filtre) },
                    label = {
                        Text(
                            text = filtre.label,
                            style = MaterialTheme.typography.labelMedium,
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

        Spacer(modifier = Modifier.height(4.dp))

        if (uiState.ventesChargees.isEmpty() && !uiState.isLoadingPage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Aucune vente enregistrée.",
                        style = MaterialTheme.typography.bodyMedium,
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
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            onDeleteVente = { venteToDelete = vente },
                            onDeleteReglement = { reg -> reglementToDelete = reg },
                            onCallClient = { Formatters.dialClient(context, vente.clientTelephone) }
                        )
                    }
                }

                if (uiState.isLoadingPage) {
                    item(key = "loading_more_footer") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
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
            .padding(top = 4.dp, bottom = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$count vente(s)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExpandableVenteCard(
    vente: VenteResumeRow,
    isExpanded: Boolean,
    details: ExpandedVenteDetails?,
    onToggleExpand: () -> Unit,
    onOpenAddPayment: () -> Unit,
    onDeleteVente: () -> Unit,
    onDeleteReglement: (ReglementEntity) -> Unit,
    onCallClient: () -> Unit
) {
    Card(
        onClick = onToggleExpand,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("vente_card_${vente.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isExpanded) 1.4.dp else 1.dp,
            color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vente.clientNom,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "#${vente.id} • ${Formatters.formatDateTime(vente.dateVente)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatutVenteBadge(statut = vente.statut)
                    IconButton(
                        onClick = onDeleteVente,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_delete_vente_${vente.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Supprimer cette vente",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Replier" else "Déplier",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Financial Summary Row
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Facturé",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatDzd(vente.montantTotal),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Réglé",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatDzd(vente.montantPaye),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
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
                            fontSize = 13.sp,
                            color = if (vente.resteAPayer > 0.01) StatusUnpaidRed else StatusPaidGreen
                        )
                    }
                }
            }

            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                if (details == null || details.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                } else {
                    Text(
                        text = "Formats facturés",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    details.lignes.forEach { ligne ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ligne.quantite} × ${ligne.nomFormat} (${ligne.contenance})",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${Formatters.formatDzd(ligne.prixUnitaireFige)} → ${Formatters.formatDzd(ligne.sousTotal)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Text(
                        text = "Versements enregistrés (${details.reglements.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (details.reglements.isEmpty()) {
                        Text(
                            text = "Aucun versement consigné (crédit intégral).",
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
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${Formatters.formatDateShort(reg.dateReglement)} • $modeLabel",
                                        style = MaterialTheme.typography.labelSmall,
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "+${Formatters.formatDzd(reg.montant)}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = StatusPaidGreen
                                    )
                                    IconButton(
                                        onClick = { onDeleteReglement(reg) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Supprimer cet acompte",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCallClient,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Appeler", style = MaterialTheme.typography.labelSmall)
                        }

                        if (vente.resteAPayer > 0.01) {
                            Button(
                                onClick = onOpenAddPayment,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(32.dp)
                                    .testTag("btn_add_acompte_vente_${vente.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Acompte", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
