package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FormatProduitEntity
import com.example.data.local.MouvementStockWithFormatRow
import com.example.data.local.StockValorisationSummary
import com.example.data.repository.ConserverieRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StockViewModel(
    private val repository: ConserverieRepository
) : ViewModel() {

    val formats: StateFlow<List<FormatProduitEntity>> = repository.formatsActifs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val valorisation: StateFlow<StockValorisationSummary> = repository.valorisationStock
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StockValorisationSummary(0.0, 0, 0)
        )

    val mouvementsRecents: StateFlow<List<MouvementStockWithFormatRow>> = repository.recentMouvementsStock
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sauvegarderFormat(
        id: Long = 0,
        nomFormat: String,
        contenance: String,
        prixUnitaire: Double,
        stockInitial: Int,
        seuilAlerte: Int
    ) {
        viewModelScope.launch {
            repository.sauvegarderFormat(
                id = id,
                nomFormat = nomFormat,
                contenance = contenance,
                prixUnitaire = prixUnitaire,
                stockInitial = stockInitial,
                seuilAlerte = seuilAlerte
            )
        }
    }

    fun enregistrerLotProduction(formatId: Long, quantite: Int, motif: String) {
        viewModelScope.launch {
            repository.ajouterLotProduction(
                formatId = formatId,
                quantiteProduite = quantite,
                motif = motif
            )
        }
    }

    fun enregistrerAjustementOuCasse(
        formatId: Long,
        quantiteDelta: Int,
        estCasseOuPerte: Boolean,
        motif: String
    ) {
        viewModelScope.launch {
            repository.enregistrerAjustementOuCasse(
                formatId = formatId,
                quantiteDelta = quantiteDelta,
                estCasseOuPerte = estCasseOuPerte,
                motif = motif
            )
        }
    }

    fun supprimerFormat(formatId: Long) {
        viewModelScope.launch {
            repository.supprimerFormat(formatId)
        }
    }

    fun supprimerMouvementStock(mouvementId: Long) {
        viewModelScope.launch {
            repository.supprimerMouvementStock(mouvementId)
        }
    }

    fun viderToutesLesDonnees(conserverFormats: Boolean) {
        viewModelScope.launch {
            repository.viderToutesLesDonnees(conserverFormats)
        }
    }
}
