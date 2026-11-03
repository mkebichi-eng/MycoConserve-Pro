package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ClientWithBalanceRow
import com.example.data.local.ReglementEntity
import com.example.data.local.VenteResumeRow
import com.example.ui.components.CompactScreenHeader
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.ReglementDialog
import com.example.ui.components.StatutVenteBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusUnpaidRed
import com.example.ui.viewmodel.ClientDetailSheetState
import com.example.util.Formatters
import com.example.util.ModePaiement

@Composable
fun ClientsScreen(
    clients: List<ClientWithBalanceRow>,
    searchQuery: String,
    onlyDebtors: Boolean,
    selectedClientDetail: ClientDetailSheetState?,
    onSearchQueryChange: (String) -> Unit,
    onToggleOnlyDebtors: (Boolean) -> Unit,
    onSelectClient: (Long?) -> Unit,
    onSaveClient: (id: Long, nom: String, telephone: String, adresse: String, notes: String) -> Unit,
    onDeleteClient: (Long) -> Unit,
    onDeleteClientSale: (Long) -> Unit,
    onDeleteClientReglement: (Long) -> Unit,
    onRecordPaymentOnSale: (venteId: Long, montant: Double, modePaiement: String, observation: String) -> Unit,
    onRecordGlobalClientPayment: (clientId: Long, montant: Double, modePaiement: String, observation: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditClientDialog by remember { mutableStateOf(false) }
    var clientToEdit by remember { mutableStateOf<ClientWithBalanceRow?>(null) }
    var clientToDelete by remember { mutableStateOf<ClientWithBalanceRow?>(null) }

    clientToDelete?.let { target ->
        ConfirmDeleteDialog(
            titre = "Supprimer le client ?",
            message = "Voulez-vous supprimer « ${target.nom} » ainsi que ses commandes (${target.nbVentes}) ? Les stocks des commandes supprimées seront automatiquement restitués.",
            onDismiss = { clientToDelete = null },
            onConfirm = {
                onDeleteClient(target.id)
                clientToDelete = null
            }
        )
    }

    if (showEditClientDialog) {
        ClientFormDialog(
            initialClient = clientToEdit,
            onDismiss = {
                showEditClientDialog = false
                clientToEdit = null
            },
            onConfirm = { id, nom, tel, adr, notes ->
                onSaveClient(id, nom, tel, adr, notes)
                showEditClientDialog = false
                clientToEdit = null
            }
        )
    }

    if (selectedClientDetail != null) {
        ClientDetailLedgerView(
            detailState = selectedClientDetail,
            onBack = { onSelectClient(null) },
            onEditClient = {
                clientToEdit = selectedClientDetail.client
                showEditClientDialog = true
            },
            onDeleteClient = { clientToDelete = selectedClientDetail.client },
            onDeleteSale = onDeleteClientSale,
            onDeleteReglement = onDeleteClientReglement,
            onRecordPaymentOnSale = onRecordPaymentOnSale,
            onRecordGlobalPayment = onRecordGlobalClientPayment
        )
        return
    }

    val totalCreancesClients = remember(clients) {
        clients.sumOf { it.soldeDebiteur }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // 1. En-tête ultra-compact sur 1 ligne avec petit bouton "+ Client"
        CompactScreenHeader(
            title = "Clients",
            subtitleBadge = "Dû: ${Formatters.formatDzd(totalCreancesClients)}",
            subtitleColor = if (totalCreancesClients > 0.01) StatusUnpaidRed else StatusPaidGreen,
            addButtonLabel = "Client",
            addButtonTestTag = "btn_nouveau_client",
            onAddClick = {
                clientToEdit = null
                showEditClientDialog = true
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Barre de recherche compacte
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Rechercher par nom, tél, wilaya…", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer", modifier = Modifier.size(16.dp))
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_clients_input")
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !onlyDebtors,
                onClick = { onToggleOnlyDebtors(false) },
                label = { Text("Tous (${clients.size})", style = MaterialTheme.typography.labelMedium) }
            )
            FilterChip(
                selected = onlyDebtors,
                onClick = { onToggleOnlyDebtors(true) },
                label = { Text("Débiteurs uniquement", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                },
                modifier = Modifier.testTag("chip_only_debtors")
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (clients.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun client enregistré.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("clients_list"),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clients, key = { it.id }) { client ->
                    ClientDirectoryCard(
                        client = client,
                        onClick = { onSelectClient(client.id) },
                        onDelete = { clientToDelete = client },
                        onCall = { Formatters.dialClient(context, client.telephone) },
                        onShareStatement = {
                            Formatters.shareClientStatement(
                                context = context,
                                clientNom = client.nom,
                                telephone = client.telephone,
                                totalCommande = client.totalCommande,
                                totalPaye = client.totalPaye,
                                soldeDebiteur = client.soldeDebiteur
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientDirectoryCard(
    client: ClientWithBalanceRow,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onShareStatement: () -> Unit
) {
    val hasDebt = client.soldeDebiteur > 0.01

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("client_card_${client.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (hasDebt) StatusUnpaidRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = client.nom.take(2).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = client.nom,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${client.telephone.ifBlank { "Sans tél." }} • ${client.nbVentes} cmd.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onShareStatement,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Envoyer relevé WhatsApp ou SMS",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onCall,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Appeler le client",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_delete_client_${client.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Supprimer le client",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Commandé",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatDzd(client.totalCommande),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Payé",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatDzd(client.totalPaye),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = StatusPaidGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Solde Dû",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatDzd(client.soldeDebiteur),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (hasDebt) StatusUnpaidRed else StatusPaidGreen
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDetailLedgerView(
    detailState: ClientDetailSheetState,
    onBack: () -> Unit,
    onEditClient: () -> Unit,
    onDeleteClient: () -> Unit,
    onDeleteSale: (Long) -> Unit,
    onDeleteReglement: (Long) -> Unit,
    onRecordPaymentOnSale: (venteId: Long, montant: Double, modePaiement: String, observation: String) -> Unit,
    onRecordGlobalPayment: (clientId: Long, montant: Double, modePaiement: String, observation: String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val client = detailState.client
    var selectedTab by remember { mutableIntStateOf(0) }
    var targetSaleForPayment by remember { mutableStateOf<VenteResumeRow?>(null) }
    var saleToDelete by remember { mutableStateOf<VenteResumeRow?>(null) }
    var reglementToDelete by remember { mutableStateOf<ReglementEntity?>(null) }
    var showGlobalPaymentDialog by remember { mutableStateOf(false) }

    targetSaleForPayment?.let { sale ->
        ReglementDialog(
            titre = "Acompte sur Facture #${sale.id}",
            sousTitreClient = client.nom,
            resteAPayerMax = sale.resteAPayer,
            onDismiss = { targetSaleForPayment = null },
            onConfirm = { montant, mode, obs ->
                onRecordPaymentOnSale(sale.id, montant, mode, obs)
                targetSaleForPayment = null
            }
        )
    }

    saleToDelete?.let { sale ->
        ConfirmDeleteDialog(
            titre = "Supprimer la facture #${sale.id} ?",
            message = "Le montant (${Formatters.formatDzd(sale.montantTotal)}) sera retiré du solde client et les quantités seront restituées au stock.",
            onDismiss = { saleToDelete = null },
            onConfirm = {
                onDeleteSale(sale.id)
                saleToDelete = null
            }
        )
    }

    reglementToDelete?.let { reg ->
        ConfirmDeleteDialog(
            titre = "Supprimer ce règlement ?",
            message = "Montant : ${Formatters.formatDzd(reg.montant)}\nLe solde du client et le statut de la facture #${reg.venteId} seront recalculés.",
            onDismiss = { reglementToDelete = null },
            onConfirm = {
                onDeleteReglement(reg.id)
                reglementToDelete = null
            }
        )
    }

    if (showGlobalPaymentDialog) {
        ReglementDialog(
            titre = "Versement Global Client",
            sousTitreClient = "${client.nom} (imputé sur les plus anciennes factures)",
            resteAPayerMax = client.soldeDebiteur,
            onDismiss = { showGlobalPaymentDialog = false },
            onConfirm = { montant, mode, obs ->
                onRecordGlobalPayment(client.id, montant, mode, obs)
                showGlobalPaymentDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = client.nom,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Fiche Client & Échéancier",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditClient) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier la fiche client"
                        )
                    }
                    IconButton(onClick = onDeleteClient) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Supprimer ce client",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (client.adresse.isNotBlank() || client.telephone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = listOfNotNull(
                                    client.telephone.takeIf { it.isNotBlank() },
                                    client.adresse.takeIf { it.isNotBlank() }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BalanceMetricColumn(
                            label = "Total Commandé",
                            value = Formatters.formatDzd(client.totalCommande),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        BalanceMetricColumn(
                            label = "Total Payé",
                            value = Formatters.formatDzd(client.totalPaye),
                            color = StatusPaidGreen,
                            alignment = Alignment.CenterHorizontally
                        )
                        BalanceMetricColumn(
                            label = "Reste Dû",
                            value = Formatters.formatDzd(client.soldeDebiteur),
                            color = if (client.soldeDebiteur > 0.01) StatusUnpaidRed else StatusPaidGreen,
                            alignment = Alignment.End
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { Formatters.dialClient(context, client.telephone) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Appeler", style = MaterialTheme.typography.labelSmall)
                        }
                        FilledTonalButton(
                            onClick = {
                                Formatters.shareClientStatement(
                                    context = context,
                                    clientNom = client.nom,
                                    telephone = client.telephone,
                                    totalCommande = client.totalCommande,
                                    totalPaye = client.totalPaye,
                                    soldeDebiteur = client.soldeDebiteur
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(32.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Relevé WA/SMS", style = MaterialTheme.typography.labelSmall)
                        }
                        if (client.soldeDebiteur > 0.01) {
                            Button(
                                onClick = { showGlobalPaymentDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(32.dp)
                                    .testTag("btn_reglement_global_client")
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Versement", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Commandes (${detailState.ventes.size})", style = MaterialTheme.typography.labelMedium) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Règlements (${detailState.reglements.size})", style = MaterialTheme.typography.labelMedium) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(detailState.ventes, key = { it.id }) { vente ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Facture #${vente.id} • ${Formatters.formatDateShort(vente.dateVente)}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        StatutVenteBadge(statut = vente.statut)
                                        IconButton(
                                            onClick = { saleToDelete = vente },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Supprimer la facture",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Total : ${Formatters.formatDzd(vente.montantTotal)}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Reste : ${Formatters.formatDzd(vente.resteAPayer)}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (vente.resteAPayer > 0.01) StatusUnpaidRed else StatusPaidGreen
                                    )
                                }
                                if (vente.resteAPayer > 0.01) {
                                    OutlinedButton(
                                        onClick = { targetSaleForPayment = vente },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Acompte sur cette facture", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(detailState.reglements, key = { it.id }) { reg ->
                        val modeLabel = ModePaiement.fromCode(reg.modePaiement).label
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${Formatters.formatDateTime(reg.dateReglement)} • $modeLabel",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Facture #${reg.venteId}" +
                                            if (reg.observation.isNotBlank()) " — ${reg.observation}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "+${Formatters.formatDzd(reg.montant)}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = StatusPaidGreen
                                    )
                                    IconButton(
                                        onClick = { reglementToDelete = reg },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Supprimer cet acompte",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(15.dp)
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

@Composable
private fun BalanceMetricColumn(
    label: String,
    value: String,
    color: Color,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = color
        )
    }
}

@Composable
private fun ClientFormDialog(
    initialClient: ClientWithBalanceRow?,
    onDismiss: () -> Unit,
    onConfirm: (id: Long, nom: String, telephone: String, adresse: String, notes: String) -> Unit
) {
    var nom by remember(initialClient) { mutableStateOf(initialClient?.nom ?: "") }
    var telephone by remember(initialClient) { mutableStateOf(initialClient?.telephone ?: "") }
    var adresse by remember(initialClient) { mutableStateOf(initialClient?.adresse ?: "") }
    var notes by remember(initialClient) { mutableStateOf(initialClient?.notes ?: "") }
    var nomError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialClient == null) "Nouveau Client" else "Modifier Fiche Client")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = {
                        nom = it
                        nomError = false
                    },
                    label = { Text("Nom / Raison sociale *") },
                    isError = nomError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_client_nom")
                )
                OutlinedTextField(
                    value = telephone,
                    onValueChange = { telephone = it },
                    label = { Text("Numéro de téléphone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_client_telephone")
                )
                OutlinedTextField(
                    value = adresse,
                    onValueChange = { adresse = it },
                    label = { Text("Wilaya / Adresse de livraison") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observations commerciales") },
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
                        onConfirm(
                            initialClient?.id ?: 0L,
                            nom.trim(),
                            telephone.trim(),
                            adresse.trim(),
                            notes.trim()
                        )
                    }
                },
                modifier = Modifier.testTag("btn_save_client")
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
