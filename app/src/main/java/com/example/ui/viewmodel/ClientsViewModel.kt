package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClientWithBalanceRow
import com.example.data.local.ReglementEntity
import com.example.data.local.VenteResumeRow
import com.example.data.repository.ConserverieRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClientDetailSheetState(
    val client: ClientWithBalanceRow,
    val ventes: List<VenteResumeRow> = emptyList(),
    val reglements: List<ReglementEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ClientsViewModel(
    private val repository: ConserverieRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val onlyDebtorsFilter = MutableStateFlow(false)
    private val selectedClientId = MutableStateFlow<Long?>(null)

    val filteredClients: StateFlow<List<ClientWithBalanceRow>> = combine(
        repository.clientsWithBalances,
        searchQuery,
        onlyDebtorsFilter
    ) { allClients, query, onlyDebtors ->
        val trimmed = query.trim()
        allClients.filter { c ->
            val matchesQuery = trimmed.isEmpty() ||
                c.nom.contains(trimmed, ignoreCase = true) ||
                c.telephone.contains(trimmed, ignoreCase = true) ||
                c.adresse.contains(trimmed, ignoreCase = true)
            val matchesDebt = !onlyDebtors || c.soldeDebiteur > 0.01
            matchesQuery && matchesDebt
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val selectedClientDetail: StateFlow<ClientDetailSheetState?> = selectedClientId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                combine(
                    repository.observeClientBalance(id),
                    repository.observeVentesClient(id),
                    repository.observeReglementsClient(id)
                ) { balance, ventes, reglements ->
                    balance?.let {
                        ClientDetailSheetState(
                            client = it,
                            ventes = ventes,
                            reglements = reglements
                        )
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleOnlyDebtors(onlyDebtors: Boolean) {
        onlyDebtorsFilter.value = onlyDebtors
    }

    fun selectClient(clientId: Long?) {
        selectedClientId.value = clientId
    }

    fun sauvegarderClient(
        id: Long = 0,
        nom: String,
        telephone: String,
        adresse: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.sauvegarderClient(
                id = id,
                nom = nom,
                telephone = telephone,
                adresse = adresse,
                notes = notes
            )
        }
    }

    fun enregistrerAcompteSurFacture(
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

    fun enregistrerReglementGlobal(
        clientId: Long,
        montant: Double,
        modePaiement: String,
        observation: String
    ) {
        viewModelScope.launch {
            repository.enregistrerReglementGlobalClient(
                clientId = clientId,
                montant = montant,
                modePaiement = modePaiement,
                observation = observation
            )
        }
    }

    fun supprimerClient(clientId: Long) {
        viewModelScope.launch {
            if (selectedClientId.value == clientId) {
                selectedClientId.value = null
            }
            repository.supprimerClient(clientId)
        }
    }

    fun supprimerFactureClient(venteId: Long) {
        viewModelScope.launch {
            repository.supprimerVente(venteId)
        }
    }

    fun supprimerReglementClient(reglementId: Long) {
        viewModelScope.launch {
            repository.supprimerReglement(reglementId)
        }
    }
}
