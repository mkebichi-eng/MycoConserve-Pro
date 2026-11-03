package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ClientWithBalanceRow
import com.example.data.local.FormatSalesBreakdownRow
import com.example.ui.components.CanneryHeaderBanner
import com.example.ui.components.CustomDateRangeDialog
import com.example.ui.components.ReglementDialog
import com.example.ui.theme.ChanterelleGoldSecondary
import com.example.ui.theme.ForestUmberPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPartialAmber
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.viewmodel.ComparativeChartBar
import com.example.ui.viewmodel.DashboardUiState
import com.example.util.Formatters
import com.example.util.PeriodFilter
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onSelectPeriod: (PeriodFilter) -> Unit,
    onSetCustomDateRange: (Long, Long) -> Unit,
    onQuickCollectDebtor: (clientId: Long, montant: Double, modePaiement: String, observation: String) -> Unit,
    onNavigateToClient: (Long) -> Unit,
    onPurgeAllData: (conserverFormats: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var showPurgeDialog by remember { mutableStateOf(false) }
    var debtorForPayment by remember { mutableStateOf<ClientWithBalanceRow?>(null) }

    if (showPurgeDialog) {
        com.example.ui.components.PurgeAllDataDialog(
            onDismiss = { showPurgeDialog = false },
            onPurgeEverything = {
                com.example.data.AppDatabase.markSeedDismissed(context)
                onPurgeAllData(false)
                showPurgeDialog = false
            },
            onPurgeHistoryKeepFormats = {
                com.example.data.AppDatabase.markSeedDismissed(context)
                onPurgeAllData(true)
                showPurgeDialog = false
            }
        )
    }

    if (showDateRangeDialog) {
        CustomDateRangeDialog(
            initialStartMillis = uiState.customStartMillis ?: uiState.periodStartMillis,
            initialEndMillis = uiState.customEndMillis ?: uiState.periodEndMillis,
            onDismiss = { showDateRangeDialog = false },
            onConfirmRange = { start, end ->
                onSetCustomDateRange(start, end)
                showDateRangeDialog = false
            }
        )
    }

    debtorForPayment?.let { debtor ->
        ReglementDialog(
            titre = "Encaisser créance client",
            sousTitreClient = debtor.nom,
            resteAPayerMax = debtor.soldeDebiteur,
            onDismiss = { debtorForPayment = null },
            onConfirm = { montant, mode, obs ->
                onQuickCollectDebtor(debtor.id, montant, mode, obs)
                debtorForPayment = null
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Compact Header Banner with "Vider test" action
        item {
            CanneryHeaderBanner(
                title = "MycoConserve Pro",
                subtitle = "Pilotage financier, stock & créances (DZD)",
                onOpenPurgeDialog = { showPurgeDialog = true }
            )
        }

        // 2. Period Selector FilterChips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PeriodFilter.entries.forEach { period ->
                        val isSelected = uiState.selectedPeriod == period
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (period == PeriodFilter.PERSONNALISE) {
                                    showDateRangeDialog = true
                                } else {
                                    onSelectPeriod(period)
                                }
                            },
                            label = {
                                Text(
                                    text = period.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = if (period == PeriodFilter.PERSONNALISE) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("period_chip_${period.name.lowercase()}")
                        )
                    }
                }

                Text(
                    text = "Période active : ${Formatters.formatDateShort(uiState.periodStartMillis)} → ${Formatters.formatDateShort(uiState.periodEndMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 3. 4 Reactive KPI Cards (2x2 Grid)
        item {
            val tauxRecouvrement = if (uiState.caFacture > 0.01) {
                ((uiState.tresorerieEncaissee / uiState.caFacture) * 100.0).roundToInt().coerceIn(0, 999)
            } else 100

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiFinanceCard(
                        title = "CA Total Facturé",
                        amountDzd = Formatters.formatDzd(uiState.caFacture),
                        subtitle = "Ventes sur la période",
                        icon = Icons.Default.PointOfSale,
                        accentColor = ForestUmberPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_ca_facture")
                    )
                    KpiFinanceCard(
                        title = "Trésorerie Encaissée",
                        amountDzd = Formatters.formatDzd(uiState.tresorerieEncaissee),
                        subtitle = "Ratio encaissé : $tauxRecouvrement%",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = StatusPaidGreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_tresorerie_encaissee")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiFinanceCard(
                        title = "Créances Générées",
                        amountDzd = Formatters.formatDzd(uiState.creancesPeriode),
                        subtitle = "Global dû : ${Formatters.formatDzd(uiState.creancesGlobales)}",
                        icon = Icons.Default.WarningAmber,
                        accentColor = StatusUnpaidRed,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_creances_clients")
                    )
                    KpiFinanceCard(
                        title = "Valeur du Stock",
                        amountDzd = Formatters.formatDzd(uiState.valorisationStock.valeurStockTotal),
                        subtitle = "${uiState.valorisationStock.totalUnitesStock} u. • ${uiState.valorisationStock.nbFormatsAlerte} en alerte",
                        icon = Icons.Default.Inventory2,
                        accentColor = ChanterelleGoldSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_valeur_stock")
                    )
                }
            }
        }

        // 4. Comparative Chart: Ventes Facturées vs Encaissements Réels
        item {
            ComparativeSalesCashChartCard(bars = uiState.comparativeBars)
        }

        // 5. Breakdown by Preserve Format (Rentabilité & Volume)
        item {
            FormatBreakdownCard(formats = uiState.repartitionFormats)
        }

        // 6. Quick Action Block: Top Clients Débiteurs
        item {
            TopDebtorsCard(
                debtors = uiState.topDebiteurs,
                onCallClient = { phone -> Formatters.dialClient(context, phone) },
                onShareStatement = { debtor ->
                    Formatters.shareClientStatement(
                        context = context,
                        clientNom = debtor.nom,
                        telephone = debtor.telephone,
                        totalCommande = debtor.totalCommande,
                        totalPaye = debtor.totalPaye,
                        soldeDebiteur = debtor.soldeDebiteur
                    )
                },
                onCollectDebt = { debtor -> debtorForPayment = debtor },
                onOpenClientProfile = onNavigateToClient
            )
        }
    }
}

@Composable
private fun KpiFinanceCard(
    title: String,
    amountDzd: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = amountDzd,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ComparativeSalesCashChartCard(
    bars: List<ComparativeChartBar>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Facturé vs Encaissé Réel",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Comparaison temporelle des flux en DZD",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = ForestUmberPrimary, label = "Ventes facturées")
                LegendDot(color = StatusPaidGreen, label = "Encaissements réels")
            }

            if (bars.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune opération enregistrée sur cette période.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxVal = max(
                    1.0,
                    bars.maxOf { max(it.ventesFacturees, it.tresorerieEncaissee) }
                )
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

                Column {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp)
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        for (i in 0..3) {
                            val y = size.height * (i / 3f)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (i < 3) dashEffect else null
                            )
                        }

                        val groupCount = bars.size
                        val slotWidth = size.width / groupCount.toFloat()
                        val barWidth = (slotWidth * 0.28f).coerceAtMost(24.dp.toPx())
                        val gap = barWidth * 0.22f

                        bars.forEachIndexed { index, item ->
                            val centerX = slotWidth * index + slotWidth / 2f
                            val hFacture = ((item.ventesFacturees / maxVal).toFloat() * (size.height - 8.dp.toPx()))
                                .coerceAtLeast(if (item.ventesFacturees > 0) 4.dp.toPx() else 0f)
                            val hEncaisse = ((item.tresorerieEncaissee / maxVal).toFloat() * (size.height - 8.dp.toPx()))
                                .coerceAtLeast(if (item.tresorerieEncaissee > 0) 4.dp.toPx() else 0f)

                            // Facturé bar (left)
                            if (hFacture > 0f) {
                                drawRoundRect(
                                    color = ForestUmberPrimary,
                                    topLeft = Offset(centerX - barWidth - gap / 2f, size.height - hFacture),
                                    size = Size(barWidth, hFacture),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }
                            // Encaissé bar (right)
                            if (hEncaisse > 0f) {
                                drawRoundRect(
                                    color = StatusPaidGreen,
                                    topLeft = Offset(centerX + gap / 2f, size.height - hEncaisse),
                                    size = Size(barWidth, hEncaisse),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        bars.forEach { bar ->
                            Text(
                                text = bar.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FormatBreakdownCard(
    formats: List<FormatSalesBreakdownRow>
) {
    val totalRevenue = formats.sumOf { it.chiffreAffairesFormat }.coerceAtLeast(1.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = "Performance par Format de Conserve",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Répartition du chiffre d'affaires et des quantités écoulées",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (formats.isEmpty()) {
                Text(
                    text = "Aucune vente enregistrée pour ces formats sur la période.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                formats.forEachIndexed { idx, row ->
                    val share = (row.chiffreAffairesFormat / totalRevenue).toFloat().coerceIn(0f, 1f)
                    val barColor = when (idx % 3) {
                        0 -> ForestUmberPrimary
                        1 -> ChanterelleGoldSecondary
                        else -> StatusPaidGreen
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    text = row.nomFormat,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = row.contenance,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = Formatters.formatDzd(row.chiffreAffairesFormat),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        LinearProgressIndicator(
                            progress = { share },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${row.quantiteTotaleVendue} unités vendues",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(share * 100).roundToInt()}% du CA",
                                style = MaterialTheme.typography.labelSmall,
                                color = barColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopDebtorsCard(
    debtors: List<ClientWithBalanceRow>,
    onCallClient: (String) -> Unit,
    onShareStatement: (ClientWithBalanceRow) -> Unit,
    onCollectDebt: (ClientWithBalanceRow) -> Unit,
    onOpenClientProfile: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Top Clients Débiteurs",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Créances prioritaires • Appel & relance WhatsApp en 1 clic",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = StatusPartialAmber.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${debtors.size} à relancer",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusPartialAmber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (debtors.isEmpty()) {
                Text(
                    text = "Excellent ! Aucun client ne présente d'impayé actuellement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusPaidGreen,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                debtors.forEachIndexed { index, debtor ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                    Surface(
                        onClick = { onOpenClientProfile(debtor.id) },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = debtor.nom,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Reste dû : ${Formatters.formatDzd(debtor.soldeDebiteur)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = StatusUnpaidRed
                                )
                                debtor.plusAncienneImpayee?.let { oldest ->
                                    Text(
                                        text = "Depuis le ${Formatters.formatDateShort(oldest)} • ${debtor.telephone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalIconButton(
                                    onClick = { onCollectDebt(debtor) },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("debtor_collect_${debtor.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = "Encaisser un acompte",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = { onShareStatement(debtor) },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("debtor_share_${debtor.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Envoyer relevé WhatsApp ou SMS",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = { onCallClient(debtor.telephone) },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("debtor_call_${debtor.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Appeler le client",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
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
