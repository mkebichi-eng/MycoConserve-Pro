package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClientWithBalanceRow
import com.example.data.local.FormatSalesBreakdownRow
import com.example.data.local.StockValorisationSummary
import com.example.data.repository.ConserverieRepository
import com.example.util.Formatters
import com.example.util.PeriodFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ComparativeChartBar(
    val label: String,
    val ventesFacturees: Double,
    val tresorerieEncaissee: Double
)

data class DashboardUiState(
    val selectedPeriod: PeriodFilter = PeriodFilter.CE_MOIS,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val periodStartMillis: Long = 0L,
    val periodEndMillis: Long = 0L,
    val caFacture: Double = 0.0,
    val tresorerieEncaissee: Double = 0.0,
    val creancesPeriode: Double = 0.0,
    val creancesGlobales: Double = 0.0,
    val valorisationStock: StockValorisationSummary = StockValorisationSummary(0.0, 0, 0),
    val comparativeBars: List<ComparativeChartBar> = emptyList(),
    val repartitionFormats: List<FormatSalesBreakdownRow> = emptyList(),
    val topDebiteurs: List<ClientWithBalanceRow> = emptyList()
)

private data class PeriodSelection(
    val filter: PeriodFilter = PeriodFilter.CE_MOIS,
    val customStart: Long? = null,
    val customEnd: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: ConserverieRepository
) : ViewModel() {

    private val periodSelection = MutableStateFlow(PeriodSelection())

    val uiState: StateFlow<DashboardUiState> = periodSelection
        .flatMapLatest { selection ->
            val (startMs, endMs) = Formatters.computePeriodBounds(
                filter = selection.filter,
                customStartMillis = selection.customStart,
                customEndMillis = selection.customEnd
            )
            val groupMonthly = selection.filter == PeriodFilter.CETTE_ANNEE ||
                (endMs - startMs) > 62L * 86_400_000L

            val ventesPointsFlow = if (groupMonthly) {
                repository.observeVentesParMois(startMs, endMs)
            } else {
                repository.observeVentesParJour(startMs, endMs)
            }
            val encaissementsPointsFlow = if (groupMonthly) {
                repository.observeEncaissementsParMois(startMs, endMs)
            } else {
                repository.observeEncaissementsParJour(startMs, endMs)
            }

            val kpiFlow = combine(
                repository.observeCaFacture(startMs, endMs),
                repository.observeTresorerieEncaissee(startMs, endMs),
                repository.observeCreancesPeriode(startMs, endMs),
                repository.creancesGlobalesTotales,
                repository.valorisationStock
            ) { ca, encaisse, creancesPer, creancesGlob, stockVal ->
                KpiBundle(ca, encaisse, creancesPer, creancesGlob, stockVal)
            }

            val analyticsFlow = combine(
                ventesPointsFlow,
                encaissementsPointsFlow,
                repository.observeRepartitionParFormat(startMs, endMs),
                repository.observeTopClientsDebiteurs(6)
            ) { ventesPts, encaissePts, formatsList, debiteurs ->
                val allKeys = (ventesPts.map { it.periodeLabel } + encaissePts.map { it.periodeLabel })
                    .distinct()
                    .sorted()
                val ventesMap = ventesPts.associate { it.periodeLabel to it.montantTotal }
                val encaisseMap = encaissePts.associate { it.periodeLabel to it.montantTotal }

                val bars = allKeys.takeLast(10).map { key ->
                    ComparativeChartBar(
                        label = Formatters.formatDayMonth(key),
                        ventesFacturees = ventesMap[key] ?: 0.0,
                        tresorerieEncaissee = encaisseMap[key] ?: 0.0
                    )
                }
                AnalyticsBundle(bars, formatsList, debiteurs)
            }

            combine(kpiFlow, analyticsFlow) { kpi, analytics ->
                DashboardUiState(
                    selectedPeriod = selection.filter,
                    customStartMillis = selection.customStart,
                    customEndMillis = selection.customEnd,
                    periodStartMillis = startMs,
                    periodEndMillis = endMs,
                    caFacture = kpi.ca,
                    tresorerieEncaissee = kpi.encaisse,
                    creancesPeriode = kpi.creancesPeriode,
                    creancesGlobales = kpi.creancesGlobales,
                    valorisationStock = kpi.stockVal,
                    comparativeBars = analytics.bars,
                    repartitionFormats = analytics.formats,
                    topDebiteurs = analytics.debiteurs
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState()
        )

    fun selectPeriod(filter: PeriodFilter) {
        periodSelection.value = periodSelection.value.copy(filter = filter)
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        periodSelection.value = PeriodSelection(
            filter = PeriodFilter.PERSONNALISE,
            customStart = startMillis,
            customEnd = endMillis
        )
    }

    fun enregistrerReglementRapideDebiteur(
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

    private data class KpiBundle(
        val ca: Double,
        val encaisse: Double,
        val creancesPeriode: Double,
        val creancesGlobales: Double,
        val stockVal: StockValorisationSummary
    )

    private data class AnalyticsBundle(
        val bars: List<ComparativeChartBar>,
        val formats: List<FormatSalesBreakdownRow>,
        val debiteurs: List<ClientWithBalanceRow>
    )
}
