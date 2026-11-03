package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.ConserverieRepository
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NouvelleVenteSheet
import com.example.ui.screens.StockScreen
import com.example.ui.screens.VentesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModelFactory
import com.example.ui.viewmodel.ClientsViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.StockViewModel
import com.example.ui.viewmodel.VentesViewModel
import kotlinx.coroutines.launch

enum class AppTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    VENTES("ventes", "Ventes", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
    CLIENTS("clients", "Clients", Icons.Filled.Groups, Icons.Outlined.Groups),
    STOCK("stock", "Stock", Icons.Filled.Inventory2, Icons.Outlined.Inventory2)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = ConserverieRepository(database.conserverieDao())
        val viewModelFactory = AppViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                MycoConserveApp(viewModelFactory = viewModelFactory)
            }
        }
    }
}

@Composable
fun MycoConserveApp(
    viewModelFactory: AppViewModelFactory
) {
    val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory)
    val ventesViewModel: VentesViewModel = viewModel(factory = viewModelFactory)
    val clientsViewModel: ClientsViewModel = viewModel(factory = viewModelFactory)
    val stockViewModel: StockViewModel = viewModel(factory = viewModelFactory)

    val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val ventesState by ventesViewModel.uiState.collectAsStateWithLifecycle()
    val clientsDisponibles by ventesViewModel.clientsDisponibles.collectAsStateWithLifecycle()
    val formatsDisponibles by ventesViewModel.formatsDisponibles.collectAsStateWithLifecycle()

    val filteredClients by clientsViewModel.filteredClients.collectAsStateWithLifecycle()
    val clientSearchQuery by clientsViewModel.searchQuery.collectAsStateWithLifecycle()
    val onlyDebtors by clientsViewModel.onlyDebtorsFilter.collectAsStateWithLifecycle()
    val selectedClientDetail by clientsViewModel.selectedClientDetail.collectAsStateWithLifecycle()

    val formatsStock by stockViewModel.formats.collectAsStateWithLifecycle()
    val valorisationStock by stockViewModel.valorisation.collectAsStateWithLifecycle()
    val mouvementsStock by stockViewModel.mouvementsRecents.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(AppTab.DASHBOARD) }
    var showNouvelleVenteSheet by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle system Back button when on secondary tabs
    BackHandler(enabled = currentTab != AppTab.DASHBOARD && !showNouvelleVenteSheet) {
        currentTab = AppTab.DASHBOARD
    }

    if (showNouvelleVenteSheet) {
        NouvelleVenteSheet(
            clients = clientsDisponibles,
            formats = formatsDisponibles,
            onClose = { showNouvelleVenteSheet = false },
            onCreateQuickClient = { nom, tel, adr, onCreated ->
                ventesViewModel.creerClientRapide(nom, tel, adr, onCreated)
            },
            onValidateSale = { clientId, panier, acompte, mode, obsAcompte, notes ->
                ventesViewModel.creerNouvelleVente(
                    clientId = clientId,
                    panier = panier,
                    acompteInitial = acompte,
                    modePaiement = mode,
                    observationAcompte = obsAcompte,
                    notesVente = notes
                ) { newVenteId ->
                    showNouvelleVenteSheet = false
                    currentTab = AppTab.VENTES
                    scope.launch {
                        snackbarHostState.showSnackbar("Vente #$newVenteId validée • Stock mis à jour")
                    }
                }
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    val alertCount = if (tab == AppTab.STOCK) valorisationStock.nbFormatsAlerte else 0

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (tab != AppTab.CLIENTS) {
                                clientsViewModel.selectClient(null)
                            }
                            currentTab = tab
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (alertCount > 0) {
                                        Badge { Text(alertCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            AppTab.DASHBOARD -> DashboardScreen(
                uiState = dashboardState,
                onSelectPeriod = dashboardViewModel::selectPeriod,
                onSetCustomDateRange = dashboardViewModel::setCustomDateRange,
                onQuickCollectDebtor = { clientId, montant, mode, obs ->
                    dashboardViewModel.enregistrerReglementRapideDebiteur(clientId, montant, mode, obs)
                    scope.launch {
                        snackbarHostState.showSnackbar("Règlement enregistré avec succès")
                    }
                },
                onNavigateToClient = { clientId ->
                    clientsViewModel.selectClient(clientId)
                    currentTab = AppTab.CLIENTS
                },
                modifier = Modifier.padding(innerPadding)
            )

            AppTab.VENTES -> VentesScreen(
                uiState = ventesState,
                onSearchChange = ventesViewModel::updateRechercheClient,
                onFilterChange = ventesViewModel::updateStatutFiltre,
                onToggleExpandVente = ventesViewModel::toggleExpansionCarteVente,
                onLoadNextPage = ventesViewModel::chargerPageSuivante,
                onRecordInstallment = { venteId, montant, mode, obs ->
                    ventesViewModel.enregistrerAcompte(venteId, montant, mode, obs)
                    scope.launch {
                        snackbarHostState.showSnackbar("Nouvel acompte enregistré sur la vente #$venteId")
                    }
                },
                onOpenNewSaleSheet = { showNouvelleVenteSheet = true },
                modifier = Modifier.padding(innerPadding)
            )

            AppTab.CLIENTS -> ClientsScreen(
                clients = filteredClients,
                searchQuery = clientSearchQuery,
                onlyDebtors = onlyDebtors,
                selectedClientDetail = selectedClientDetail,
                onSearchQueryChange = clientsViewModel::setSearchQuery,
                onToggleOnlyDebtors = clientsViewModel::toggleOnlyDebtors,
                onSelectClient = clientsViewModel::selectClient,
                onSaveClient = { id, nom, tel, adr, notes ->
                    clientsViewModel.sauvegarderClient(id, nom, tel, adr, notes)
                    scope.launch {
                        snackbarHostState.showSnackbar("Fiche client enregistrée")
                    }
                },
                onRecordPaymentOnSale = { venteId, montant, mode, obs ->
                    clientsViewModel.enregistrerAcompteSurFacture(venteId, montant, mode, obs)
                    scope.launch {
                        snackbarHostState.showSnackbar("Acompte enregistré sur la facture #$venteId")
                    }
                },
                onRecordGlobalClientPayment = { clientId, montant, mode, obs ->
                    clientsViewModel.enregistrerReglementGlobal(clientId, montant, mode, obs)
                    scope.launch {
                        snackbarHostState.showSnackbar("Versement global ventilé sur les factures")
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )

            AppTab.STOCK -> StockScreen(
                formats = formatsStock,
                valorisation = valorisationStock,
                mouvementsRecents = mouvementsStock,
                onSaveFormat = { id, nom, contenance, prix, stockInit, seuil ->
                    stockViewModel.sauvegarderFormat(id, nom, contenance, prix, stockInit, seuil)
                    scope.launch {
                        snackbarHostState.showSnackbar("Format de conserve enregistré")
                    }
                },
                onAddProductionBatch = { formatId, qty, motif ->
                    stockViewModel.enregistrerLotProduction(formatId, qty, motif)
                    scope.launch {
                        snackbarHostState.showSnackbar("+$qty unités ajoutées au stock (Production)")
                    }
                },
                onRecordAdjustmentOrLoss = { formatId, delta, estCasse, motif ->
                    stockViewModel.enregistrerAjustementOuCasse(formatId, delta, estCasse, motif)
                    scope.launch {
                        snackbarHostState.showSnackbar("Mouvement de stock enregistré")
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
