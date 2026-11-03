package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClientWithBalanceRow
import com.example.data.local.FormatProduitEntity
import com.example.data.local.LigneVenteDetailRow
import com.example.data.local.PanierItemDraft
import com.example.data.local.ReglementEntity
import com.example.data.local.VenteResumeRow
import com.example.data.repository.ConserverieRepository
import com.example.util.StatutVenteFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpandedVenteDetails(
    val lignes: List<LigneVenteDetailRow> = emptyList(),
    val reglements: List<ReglementEntity> = emptyList(),
    val isLoading: Boolean = false
)

data class VentesUiState(
    val rechercheClient: String = "",
    val statutFiltre: StatutVenteFilter = StatutVenteFilter.TOUS,
    val ventesChargees: List<VenteResumeRow> = emptyList(),
    val totalMatchingCount: Int = 0,
    val isLoadingPage: Boolean = false,
    val canLoadMore: Boolean = false,
    val expandedVenteIds: Set<Long> = emptySet(),
    val detailsParVente: Map<Long, ExpandedVenteDetails> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
class VentesViewModel(
    private val repository: ConserverieRepository
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    private val _uiState = MutableStateFlow(VentesUiState())
    val uiState: StateFlow<VentesUiState> = _uiState.asStateFlow()

    val clientsDisponibles: StateFlow<List<ClientWithBalanceRow>> = repository.clientsWithBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val formatsDisponibles: StateFlow<List<FormatProduitEntity>> = repository.formatsActifs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentLimit = PAGE_SIZE

    init {
        // Observe any database change or filter change to refresh paginated window
        viewModelScope.launch {
            combine(
                repository.modificationPulse,
                _uiState
                    .flatMapLatest { state ->
                        repository.observeTotalMatchingVentesCount(
                            statutFiltre = state.statutFiltre.code,
                            rechercheClient = state.rechercheClient.trim()
                        )
                    }
            ) { _, totalCount -> totalCount }
                .collect { totalCount ->
                    rechargerJusquaLimiteCourante(totalCount)
                }
        }
    }

    fun updateRechercheClient(query: String) {
        currentLimit = PAGE_SIZE
        _uiState.update { it.copy(rechercheClient = query) }
        rafraichirDepuisDebut()
    }

    fun updateStatutFiltre(filtre: StatutVenteFilter) {
        currentLimit = PAGE_SIZE
        _uiState.update { it.copy(statutFiltre = filtre) }
        rafraichirDepuisDebut()
    }

    private fun rafraichirDepuisDebut() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoadingPage = true) }
            val rows = repository.chargerPageVentes(
                statutFiltre = state.statutFiltre.code,
                rechercheClient = state.rechercheClient.trim(),
                limit = currentLimit,
                offset = 0
            )
            _uiState.update {
                it.copy(
                    ventesChargees = rows,
                    isLoadingPage = false,
                    canLoadMore = rows.size < it.totalMatchingCount
                )
            }
        }
    }

    private suspend fun rechargerJusquaLimiteCourante(totalCount: Int) {
        val state = _uiState.value
        val rows = repository.chargerPageVentes(
            statutFiltre = state.statutFiltre.code,
            rechercheClient = state.rechercheClient.trim(),
            limit = currentLimit,
            offset = 0
        )
        // Also refresh details of currently expanded cards
        val updatedDetails = mutableMapOf<Long, ExpandedVenteDetails>()
        for (venteId in state.expandedVenteIds) {
            val (lignes, reglements) = repository.chargerDetailsCarteVente(venteId)
            updatedDetails[venteId] = ExpandedVenteDetails(
                lignes = lignes,
                reglements = reglements,
                isLoading = false
            )
        }
        _uiState.update {
            it.copy(
                ventesChargees = rows,
                totalMatchingCount = totalCount,
                isLoadingPage = false,
                canLoadMore = rows.size < totalCount,
                detailsParVente = it.detailsParVente + updatedDetails
            )
        }
    }

    fun chargerPageSuivante() {
        val state = _uiState.value
        if (state.isLoadingPage || !state.canLoadMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPage = true) }
            val offset = state.ventesChargees.size
            val nouvellesLignes = repository.chargerPageVentes(
                statutFiltre = state.statutFiltre.code,
                rechercheClient = state.rechercheClient.trim(),
                limit = PAGE_SIZE,
                offset = offset
            )
            currentLimit = offset + nouvellesLignes.size
            _uiState.update { current ->
                val merged = (current.ventesChargees + nouvellesLignes).distinctBy { it.id }
                current.copy(
                    ventesChargees = merged,
                    isLoadingPage = false,
                    canLoadMore = merged.size < current.totalMatchingCount
                )
            }
        }
    }

    fun toggleExpansionCarteVente(venteId: Long) {
        val currentExpanded = _uiState.value.expandedVenteIds
        if (currentExpanded.contains(venteId)) {
            _uiState.update { it.copy(expandedVenteIds = currentExpanded - venteId) }
        } else {
            _uiState.update {
                it.copy(
                    expandedVenteIds = currentExpanded + venteId,
                    detailsParVente = it.detailsParVente + (venteId to ExpandedVenteDetails(isLoading = true))
                )
            }
            viewModelScope.launch {
                val (lignes, reglements) = repository.chargerDetailsCarteVente(venteId)
                _uiState.update {
                    it.copy(
                        detailsParVente = it.detailsParVente + (
                            venteId to ExpandedVenteDetails(
                                lignes = lignes,
                                reglements = reglements,
                                isLoading = false
                            )
                        )
                    )
                }
            }
        }
    }

    fun enregistrerAcompte(
        venteId: Long,
        montant: Double,
        modePaiement: String,
        observation: String
    ) {
        viewModelScope.launch {
            repository.enregistrerAcompteSurVente(
                venteId = venteId,
                montant = montant,
                modePaiement = modePaiement,
                observation = observation
            )
        }
    }

    fun supprimerVente(venteId: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    expandedVenteIds = it.expandedVenteIds - venteId,
                    detailsParVente = it.detailsParVente - venteId
                )
            }
            repository.supprimerVente(venteId)
        }
    }

    fun supprimerReglement(reglementId: Long) {
        viewModelScope.launch {
            repository.supprimerReglement(reglementId)
        }
    }

    fun creerNouvelleVente(
        clientId: Long,
        panier: List<PanierItemDraft>,
        acompteInitial: Double,
        modePaiement: String,
        observationAcompte: String,
        notesVente: String,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val newId = repository.validerNouvelleVente(
                clientId = clientId,
                panier = panier,
                acompteInitial = acompteInitial,
                modePaiement = modePaiement,
                observationAcompte = observationAcompte,
                notesVente = notesVente
            )
            onSuccess(newId)
        }
    }

    fun creerClientRapide(
        nom: String,
        telephone: String,
        adresse: String,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val clientId = repository.sauvegarderClient(
                nom = nom,
                telephone = telephone,
                adresse = adresse,
                notes = ""
            )
            onCreated(clientId)
        }
    }
}
