package com.example.data.repository

import com.example.data.BigDecimalConverters
import com.example.data.local.ClientEntity
import com.example.data.local.ClientWithBalanceRow
import com.example.data.local.ConserverieDao
import com.example.data.local.FormatProduitEntity
import com.example.data.local.FormatSalesBreakdownRow
import com.example.data.local.LigneVenteDetailRow
import com.example.data.local.MouvementStockWithFormatRow
import com.example.data.local.PanierItemDraft
import com.example.data.local.PeriodAggregationPoint
import com.example.data.local.ReglementEntity
import com.example.data.local.StockValorisationSummary
import com.example.data.local.VenteResumeRow
import kotlinx.coroutines.flow.Flow

class ConserverieRepository(private val dao: ConserverieDao) {

    // ─── FORMATS & STOCK ─────────────────────────────────────────────────────
    val formatsActifs: Flow<List<FormatProduitEntity>> = dao.observeFormatsActifs()
    val valorisationStock: Flow<StockValorisationSummary> = dao.observeValorisationStock()
    val recentMouvementsStock: Flow<List<MouvementStockWithFormatRow>> = dao.observeRecentMouvementsStock(80)

    suspend fun sauvegarderFormat(
        id: Long = 0,
        nomFormat: String,
        contenance: String,
        prixUnitaire: Double,
        stockInitial: Int,
        seuilAlerte: Int
    ): Long {
        return if (id == 0L) {
            val newId = dao.insertFormat(
                FormatProduitEntity(
                    nomFormat = nomFormat.trim(),
                    contenance = contenance.trim(),
                    prixUnitaire = BigDecimalConverters.fromStringToBigDecimal(prixUnitaire.toString()),
                    stockActuel = stockInitial,
                    seuilAlerte = seuilAlerte,
                    actif = 1
                )
            )
            if (stockInitial > 0) {
                dao.enregistrerMouvementStockTransaction(
                    formatId = newId,
                    typeMouvement = "PRODUCTION",
                    quantiteDelta = 0,
                    motif = "Stock initial à la création ($stockInitial u.)"
                )
            }
            newId
        } else {
            val existing = dao.getFormatById(id) ?: return 0L
            dao.updateFormat(
                existing.copy(
                    nomFormat = nomFormat.trim(),
                    contenance = contenance.trim(),
                    prixUnitaire = BigDecimalConverters.fromStringToBigDecimal(prixUnitaire.toString()),
                    seuilAlerte = seuilAlerte
                )
            )
            id
        }
    }

    suspend fun supprimerFormat(formatId: Long) {
        dao.supprimerFormatTransaction(formatId)
    }

    suspend fun ajouterLotProduction(formatId: Long, quantiteProduite: Int, motif: String) {
        if (quantiteProduite <= 0) return
        dao.enregistrerMouvementStockTransaction(
            formatId = formatId,
            typeMouvement = "PRODUCTION",
            quantiteDelta = quantiteProduite,
            motif = motif.ifBlank { "Lot de production (+$quantiteProduite u.)" }
        )
    }

    suspend fun enregistrerAjustementOuCasse(
        formatId: Long,
        quantiteDelta: Int,
        estCasseOuPerte: Boolean,
        motif: String
    ) {
        if (quantiteDelta == 0) return
        val type = if (estCasseOuPerte) "PERTE_CASSE" else "AJUSTEMENT"
        dao.enregistrerMouvementStockTransaction(
            formatId = formatId,
            typeMouvement = type,
            quantiteDelta = quantiteDelta,
            motif = motif.ifBlank {
                if (estCasseOuPerte) "Perte / Casse déclarée" else "Ajustement d'inventaire"
            }
        )
    }

    suspend fun supprimerMouvementStock(mouvementId: Long) {
        dao.supprimerMouvementStockTransaction(mouvementId)
    }

    // ─── CLIENTS ─────────────────────────────────────────────────────────────
    val clientsWithBalances: Flow<List<ClientWithBalanceRow>> = dao.observeClientsWithBalances()

    fun observeClientBalance(clientId: Long): Flow<ClientWithBalanceRow?> =
        dao.observeClientBalanceById(clientId)

    fun observeVentesClient(clientId: Long): Flow<List<VenteResumeRow>> =
        dao.observeVentesPourClient(clientId)

    fun observeReglementsClient(clientId: Long): Flow<List<ReglementEntity>> =
        dao.observeReglementsPourClient(clientId)

    suspend fun sauvegarderClient(
        id: Long = 0,
        nom: String,
        telephone: String,
        adresse: String,
        notes: String
    ): Long {
        return if (id == 0L) {
            dao.insertClient(
                ClientEntity(
                    nom = nom.trim(),
                    telephone = telephone.trim(),
                    adresse = adresse.trim(),
                    notes = notes.trim()
                )
            )
        } else {
            dao.updateClient(
                ClientEntity(
                    id = id,
                    nom = nom.trim(),
                    telephone = telephone.trim(),
                    adresse = adresse.trim(),
                    notes = notes.trim()
                )
            )
            id
        }
    }

    suspend fun supprimerClient(clientId: Long) {
        dao.supprimerClientCompletTransaction(clientId)
    }

    // ─── VENTES & RÈGLEMENTS ÉCHELONNÉS ──────────────────────────────────────
    val modificationPulse: Flow<Double> = dao.observeVentesAndReglementsModificationPulse()

    fun observeTotalMatchingVentesCount(statutFiltre: String, rechercheClient: String): Flow<Int> =
        dao.observeTotalMatchingVentesCount(statutFiltre, rechercheClient)

    suspend fun chargerPageVentes(
        statutFiltre: String,
        rechercheClient: String,
        limit: Int,
        offset: Int
    ): List<VenteResumeRow> = dao.getVentesPaginated(statutFiltre, rechercheClient, limit, offset)

    suspend fun chargerDetailsCarteVente(venteId: Long): Pair<List<LigneVenteDetailRow>, List<ReglementEntity>> {
        val lignes = dao.getLignesDetailPourVente(venteId)
        val reglements = dao.getReglementsPourVente(venteId)
        return lignes to reglements
    }

    suspend fun validerNouvelleVente(
        clientId: Long,
        panier: List<PanierItemDraft>,
        acompteInitial: Double,
        modePaiement: String,
        observationAcompte: String,
        notesVente: String
    ): Long {
        return dao.creerVenteCompleteTransaction(
            clientId = clientId,
            panier = panier,
            acompteInitial = acompteInitial,
            modePaiementAcompte = modePaiement,
            observationAcompte = observationAcompte,
            notesVente = notesVente
        )
    }

    suspend fun supprimerVente(venteId: Long) {
        dao.supprimerVenteEtRestaurerStockTransaction(venteId)
    }

    suspend fun enregistrerAcompteSurVente(
        venteId: Long,
        montant: Double,
        modePaiement: String,
        observation: String
    ) {
        dao.enregistrerReglementTransaction(
            venteId = venteId,
            montantVerse = montant,
            modePaiement = modePaiement,
            observation = observation
        )
    }

    suspend fun supprimerReglement(reglementId: Long) {
        dao.supprimerReglementTransaction(reglementId)
    }

    suspend fun enregistrerReglementGlobalClient(
        clientId: Long,
        montant: Double,
        modePaiement: String,
        observation: String
    ) {
        dao.enregistrerReglementGlobalClientTransaction(
            clientId = clientId,
            montantGlobal = montant,
            modePaiement = modePaiement,
            observation = observation
        )
    }

    // ─── PURGE GLOBALE ───────────────────────────────────────────────────────
    suspend fun viderToutesLesDonnees(conserverFormats: Boolean) {
        dao.viderToutesLesDonneesTransaction(conserverFormats)
    }

    // ─── DASHBOARD FINANCIER ─────────────────────────────────────────────────
    fun observeCaFacture(startMillis: Long, endMillis: Long): Flow<Double> =
        dao.observeCaFacturePeriode(startMillis, endMillis)

    fun observeTresorerieEncaissee(startMillis: Long, endMillis: Long): Flow<Double> =
        dao.observeTresorerieEncaisseePeriode(startMillis, endMillis)

    fun observeCreancesPeriode(startMillis: Long, endMillis: Long): Flow<Double> =
        dao.observeCreancesPeriode(startMillis, endMillis)

    val creancesGlobalesTotales: Flow<Double> = dao.observeCreancesGlobalesTotales()

    fun observeVentesParJour(startMillis: Long, endMillis: Long): Flow<List<PeriodAggregationPoint>> =
        dao.observeVentesParJour(startMillis, endMillis)

    fun observeEncaissementsParJour(startMillis: Long, endMillis: Long): Flow<List<PeriodAggregationPoint>> =
        dao.observeEncaissementsParJour(startMillis, endMillis)

    fun observeVentesParMois(startMillis: Long, endMillis: Long): Flow<List<PeriodAggregationPoint>> =
        dao.observeVentesParMois(startMillis, endMillis)

    fun observeEncaissementsParMois(startMillis: Long, endMillis: Long): Flow<List<PeriodAggregationPoint>> =
        dao.observeEncaissementsParMois(startMillis, endMillis)

    fun observeRepartitionParFormat(startMillis: Long, endMillis: Long): Flow<List<FormatSalesBreakdownRow>> =
        dao.observeRepartitionVentesParFormat(startMillis, endMillis)

    fun observeTopClientsDebiteurs(limit: Int = 6): Flow<List<ClientWithBalanceRow>> =
        dao.observeTopClientsDebiteurs(limit)
}
