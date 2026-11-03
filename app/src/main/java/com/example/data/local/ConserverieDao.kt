package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.Client
import com.example.data.FormatProduit
import com.example.data.LigneVente
import com.example.data.Reglement
import com.example.data.Vente
import kotlinx.coroutines.flow.Flow
import kotlin.math.max
import kotlin.math.min

@Dao
interface ConserverieDao {

    // ─── 1. FORMATS DE PRODUITS & STOCK ──────────────────────────────────────

    @Query("SELECT * FROM formats_produits WHERE actif = 1 ORDER BY nom_format ASC, prix_unitaire ASC")
    fun observeFormatsActifs(): Flow<List<FormatProduitEntity>>

    @Query("SELECT * FROM formats_produits WHERE id = :formatId LIMIT 1")
    suspend fun getFormatById(formatId: Long): FormatProduitEntity?

    @Query("SELECT COUNT(*) FROM formats_produits")
    suspend fun countFormats(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormat(format: FormatProduitEntity): Long

    @Update
    suspend fun updateFormat(format: FormatProduitEntity)

    @Query("UPDATE formats_produits SET stock_actuel = :nouveauStock WHERE id = :formatId")
    suspend fun updateStockFormat(formatId: Long, nouveauStock: Int)

    @Query("SELECT COUNT(*) FROM lignes_vente WHERE format_id = :formatId")
    suspend fun countLignesVenteParFormat(formatId: Long): Int

    @Query("DELETE FROM formats_produits WHERE id = :formatId")
    suspend fun deleteFormatById(formatId: Long)

    @Query("UPDATE formats_produits SET actif = 0, stock_actuel = 0 WHERE id = :formatId")
    suspend fun desactiverFormatById(formatId: Long)

    @Transaction
    suspend fun supprimerFormatTransaction(formatId: Long) {
        val nbUsages = countLignesVenteParFormat(formatId)
        if (nbUsages == 0) {
            deleteFormatById(formatId)
        } else {
            desactiverFormatById(formatId)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMouvementStock(mouvement: MouvementStockEntity): Long

    @Query("SELECT * FROM mouvements_stock WHERE id = :mouvementId LIMIT 1")
    suspend fun getMouvementStockById(mouvementId: Long): MouvementStockEntity?

    @Query("DELETE FROM mouvements_stock WHERE id = :mouvementId")
    suspend fun deleteMouvementStockById(mouvementId: Long)

    @Query("DELETE FROM mouvements_stock WHERE vente_id = :venteId")
    suspend fun deleteMouvementsStockParVenteId(venteId: Long)

    @Query(
        """
        SELECT m.id,
               m.format_id,
               f.nom_format,
               f.contenance,
               m.type_mouvement,
               m.quantite_delta,
               m.stock_apres,
               m.date_mouvement,
               m.motif
        FROM mouvements_stock m
        INNER JOIN formats_produits f ON f.id = m.format_id
        ORDER BY m.date_mouvement DESC
        LIMIT :limit
        """
    )
    fun observeRecentMouvementsStock(limit: Int = 80): Flow<List<MouvementStockWithFormatRow>>

    @Transaction
    suspend fun enregistrerMouvementStockTransaction(
        formatId: Long,
        typeMouvement: String,
        quantiteDelta: Int,
        motif: String,
        dateMouvement: Long = System.currentTimeMillis()
    ) {
        val format = getFormatById(formatId) ?: return
        val nouveauStock = max(0, format.stockActuel + quantiteDelta)
        updateStockFormat(formatId, nouveauStock)
        insertMouvementStock(
            MouvementStockEntity(
                formatId = formatId,
                typeMouvement = typeMouvement,
                quantiteDelta = quantiteDelta,
                stockApres = nouveauStock,
                dateMouvement = dateMouvement,
                motif = motif
            )
        )
    }

    /**
     * Supprime un lot de production ou mouvement de stock et recalcule automatiquement
     * le stock du format concerné en inversant le delta du mouvement supprimé.
     */
    @Transaction
    suspend fun supprimerMouvementStockTransaction(mouvementId: Long) {
        val mv = getMouvementStockById(mouvementId) ?: return
        val format = getFormatById(mv.formatId)
        if (format != null) {
            val stockCorrige = max(0, format.stockActuel - mv.quantiteDelta)
            updateStockFormat(format.id, stockCorrige)
        }
        deleteMouvementStockById(mouvementId)
    }

    // ─── 2. CLIENTS & SOLDES FINANCIERS EN DIRECT ────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Query("DELETE FROM clients WHERE id = :clientId")
    suspend fun deleteClientById(clientId: Long)

    @Query("SELECT id FROM ventes WHERE client_id = :clientId")
    suspend fun getVenteIdsPourClient(clientId: Long): List<Long>

    /**
     * Supprime un client ainsi que toutes ses ventes et règlements, en restituant
     * automatiquement au stock les quantités des ventes supprimées.
     */
    @Transaction
    suspend fun supprimerClientCompletTransaction(clientId: Long) {
        val venteIds = getVenteIdsPourClient(clientId)
        for (vId in venteIds) {
            supprimerVenteEtRestaurerStockTransaction(vId)
        }
        deleteClientById(clientId)
    }

    @Query(
        """
        SELECT c.id,
               c.nom,
               c.telephone,
               c.adresse,
               c.notes,
               COALESCE(v_tot.total_commande, 0.0) AS total_commande,
               COALESCE(r_tot.total_paye, 0.0) AS total_paye,
               MAX(0.0, COALESCE(v_tot.total_commande, 0.0) - COALESCE(r_tot.total_paye, 0.0)) AS solde_debiteur,
               COALESCE(v_tot.nb_ventes, 0) AS nb_ventes,
               v_tot.plus_ancienne_impayee AS plus_ancienne_impayee
        FROM clients c
        LEFT JOIN (
            SELECT client_id,
                   SUM(montant_total) AS total_commande,
                   COUNT(id) AS nb_ventes,
                   MIN(CASE WHEN statut != 'PAYE' THEN date_vente ELSE NULL END) AS plus_ancienne_impayee
            FROM ventes
            GROUP BY client_id
        ) v_tot ON v_tot.client_id = c.id
        LEFT JOIN (
            SELECT client_id,
                   SUM(montant) AS total_paye
            FROM reglements
            GROUP BY client_id
        ) r_tot ON r_tot.client_id = c.id
        ORDER BY solde_debiteur DESC, c.nom COLLATE NOCASE ASC
        """
    )
    fun observeClientsWithBalances(): Flow<List<ClientWithBalanceRow>>

    @Query(
        """
        SELECT c.id,
               c.nom,
               c.telephone,
               c.adresse,
               c.notes,
               COALESCE(v_tot.total_commande, 0.0) AS total_commande,
               COALESCE(r_tot.total_paye, 0.0) AS total_paye,
               MAX(0.0, COALESCE(v_tot.total_commande, 0.0) - COALESCE(r_tot.total_paye, 0.0)) AS solde_debiteur,
               COALESCE(v_tot.nb_ventes, 0) AS nb_ventes,
               v_tot.plus_ancienne_impayee AS plus_ancienne_impayee
        FROM clients c
        LEFT JOIN (
            SELECT client_id,
                   SUM(montant_total) AS total_commande,
                   COUNT(id) AS nb_ventes,
                   MIN(CASE WHEN statut != 'PAYE' THEN date_vente ELSE NULL END) AS plus_ancienne_impayee
            FROM ventes
            GROUP BY client_id
        ) v_tot ON v_tot.client_id = c.id
        LEFT JOIN (
            SELECT client_id,
                   SUM(montant) AS total_paye
            FROM reglements
            GROUP BY client_id
        ) r_tot ON r_tot.client_id = c.id
        WHERE c.id = :clientId
        LIMIT 1
        """
    )
    fun observeClientBalanceById(clientId: Long): Flow<ClientWithBalanceRow?>

    // ─── 3. VENTES, PAGINATION, CARTES DÉPLIABLES & RÈGLEMENTS ───────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVente(vente: VenteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLignesVente(lignes: List<LigneVenteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReglement(reglement: ReglementEntity): Long

    @Query("SELECT * FROM ventes WHERE id = :venteId LIMIT 1")
    suspend fun getVenteById(venteId: Long): VenteEntity?

    @Query("DELETE FROM ventes WHERE id = :venteId")
    suspend fun deleteVenteById(venteId: Long)

    @Query("SELECT * FROM lignes_vente WHERE vente_id = :venteId")
    suspend fun getLignesBrutesPourVente(venteId: Long): List<LigneVente>

    @Query("SELECT * FROM reglements WHERE id = :reglementId LIMIT 1")
    suspend fun getReglementById(reglementId: Long): Reglement?

    @Query("DELETE FROM reglements WHERE id = :reglementId")
    suspend fun deleteReglementById(reglementId: Long)

    @Query("UPDATE ventes SET statut = :nouveauStatut WHERE id = :venteId")
    suspend fun updateStatutVente(venteId: Long, nouveauStatut: String)

    @Query("SELECT COALESCE(SUM(montant), 0.0) FROM reglements WHERE vente_id = :venteId")
    suspend fun getTotalPayePourVente(venteId: Long): Double

    /**
     * Supprime une vente et restitue automatiquement les quantités vendues au stock
     * de chaque format concerné.
     */
    @Transaction
    suspend fun supprimerVenteEtRestaurerStockTransaction(venteId: Long) {
        val lignes = getLignesBrutesPourVente(venteId)
        for (ligne in lignes) {
            val format = getFormatById(ligne.formatId)
            if (format != null) {
                updateStockFormat(format.id, format.stockActuel + ligne.quantite)
            }
        }
        deleteMouvementsStockParVenteId(venteId)
        deleteVenteById(venteId) // Supprime aussi lignes_vente et reglements via ON DELETE CASCADE
    }

    /**
     * Supprime un acompte / règlement et met à jour le statut de la vente correspondante.
     */
    @Transaction
    suspend fun supprimerReglementTransaction(reglementId: Long) {
        val reglement = getReglementById(reglementId) ?: return
        deleteReglementById(reglementId)

        val vente = getVenteById(reglement.venteId) ?: return
        val totalPayeRestant = getTotalPayePourVente(vente.id).toDzdBigDecimal()
        val tolerance = java.math.BigDecimal("0.01")
        val nouveauStatut = when {
            vente.montantTotal.subtract(totalPayeRestant) <= tolerance -> "PAYE"
            totalPayeRestant > tolerance -> "PARTIEL"
            else -> "IMPAYE"
        }
        updateStatutVente(vente.id, nouveauStatut)
    }

    @Query(
        """
        SELECT v.id,
               v.client_id,
               c.nom AS client_nom,
               c.telephone AS client_telephone,
               v.date_vente,
               v.montant_total,
               v.statut,
               v.notes,
               COALESCE(SUM(r.montant), 0.0) AS montant_paye,
               MAX(0.0, v.montant_total - COALESCE(SUM(r.montant), 0.0)) AS reste_a_payer
        FROM ventes v
        INNER JOIN clients c ON c.id = v.client_id
        LEFT JOIN reglements r ON r.vente_id = v.id
        WHERE (:statutFiltre = 'TOUS' OR v.statut = :statutFiltre)
          AND (:rechercheClient = '' OR c.nom LIKE '%' || :rechercheClient || '%')
        GROUP BY v.id
        ORDER BY v.date_vente DESC, v.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getVentesPaginated(
        statutFiltre: String,
        rechercheClient: String,
        limit: Int,
        offset: Int
    ): List<VenteResumeRow>

    @Query(
        """
        SELECT COUNT(*)
        FROM ventes v
        INNER JOIN clients c ON c.id = v.client_id
        WHERE (:statutFiltre = 'TOUS' OR v.statut = :statutFiltre)
          AND (:rechercheClient = '' OR c.nom LIKE '%' || :rechercheClient || '%')
        """
    )
    fun observeTotalMatchingVentesCount(
        statutFiltre: String,
        rechercheClient: String
    ): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(montant_total), 0.0)
               + COUNT(*) * 0.0001
               + COALESCE((SELECT SUM(montant) + COUNT(*) * 0.0001 FROM reglements), 0.0)
        FROM ventes
        """
    )
    fun observeVentesAndReglementsModificationPulse(): Flow<Double>

    @Query(
        """
        SELECT v.id,
               v.client_id,
               c.nom AS client_nom,
               c.telephone AS client_telephone,
               v.date_vente,
               v.montant_total,
               v.statut,
               v.notes,
               COALESCE(SUM(r.montant), 0.0) AS montant_paye,
               MAX(0.0, v.montant_total - COALESCE(SUM(r.montant), 0.0)) AS reste_a_payer
        FROM ventes v
        INNER JOIN clients c ON c.id = v.client_id
        LEFT JOIN reglements r ON r.vente_id = v.id
        WHERE v.client_id = :clientId
        GROUP BY v.id
        ORDER BY v.date_vente DESC, v.id DESC
        """
    )
    fun observeVentesPourClient(clientId: Long): Flow<List<VenteResumeRow>>

    @Query(
        """
        SELECT lv.id,
               lv.vente_id,
               lv.format_id,
               f.nom_format,
               f.contenance,
               lv.quantite,
               lv.prix_unitaire_fige,
               lv.sous_total
        FROM lignes_vente lv
        INNER JOIN formats_produits f ON f.id = lv.format_id
        WHERE lv.vente_id = :venteId
        ORDER BY lv.id ASC
        """
    )
    suspend fun getLignesDetailPourVente(venteId: Long): List<LigneVenteDetailRow>

    @Query(
        """
        SELECT *
        FROM reglements
        WHERE vente_id = :venteId
        ORDER BY date_reglement DESC, id DESC
        """
    )
    suspend fun getReglementsPourVente(venteId: Long): List<ReglementEntity>

    @Query(
        """
        SELECT *
        FROM reglements
        WHERE client_id = :clientId
        ORDER BY date_reglement DESC, id DESC
        """
    )
    fun observeReglementsPourClient(clientId: Long): Flow<List<ReglementEntity>>

    @Transaction
    suspend fun creerVenteCompleteTransaction(
        clientId: Long,
        panier: List<PanierItemDraft>,
        acompteInitial: Double,
        modePaiementAcompte: String,
        observationAcompte: String,
        notesVente: String,
        dateVente: Long = System.currentTimeMillis()
    ): Long {
        val montantTotalBd = panier.fold(java.math.BigDecimal.ZERO) { acc, item ->
            val ligneTotal = item.format.prixUnitaire.multiply(java.math.BigDecimal.valueOf(item.quantite.toLong()))
            acc.add(ligneTotal)
        }.setScale(2, java.math.RoundingMode.HALF_UP)

        val acompteBd = acompteInitial.toDzdBigDecimal()
            .max(java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP))
            .min(montantTotalBd)

        val tolerance = java.math.BigDecimal("0.01")
        val statutInitial = when {
            montantTotalBd.subtract(acompteBd) <= tolerance -> "PAYE"
            acompteBd > tolerance -> "PARTIEL"
            else -> "IMPAYE"
        }

        val venteId = insertVente(
            Vente(
                clientId = clientId,
                dateVente = dateVente,
                montantTotal = montantTotalBd,
                statut = statutInitial,
                notes = notesVente
            )
        )

        val lignesEntities = panier.map { item ->
            val sousTotalBd = item.format.prixUnitaire
                .multiply(java.math.BigDecimal.valueOf(item.quantite.toLong()))
                .setScale(2, java.math.RoundingMode.HALF_UP)
            LigneVente(
                venteId = venteId,
                formatId = item.format.id,
                quantite = item.quantite,
                prixUnitaireFige = item.format.prixUnitaire,
                sousTotal = sousTotalBd
            )
        }
        insertLignesVente(lignesEntities)

        for (item in panier) {
            val freshFormat = getFormatById(item.format.id) ?: item.format
            val nouveauStock = max(0, freshFormat.stockActuel - item.quantite)
            updateStockFormat(freshFormat.id, nouveauStock)
            insertMouvementStock(
                MouvementStockEntity(
                    formatId = freshFormat.id,
                    typeMouvement = "VENTE",
                    quantiteDelta = -item.quantite,
                    stockApres = nouveauStock,
                    dateMouvement = dateVente,
                    motif = "Vente #$venteId (${item.quantite} u.)",
                    venteId = venteId
                )
            )
        }

        if (acompteBd > tolerance) {
            insertReglement(
                Reglement(
                    venteId = venteId,
                    clientId = clientId,
                    dateReglement = dateVente,
                    montant = acompteBd,
                    modePaiement = modePaiementAcompte,
                    observation = observationAcompte.ifBlank { "Acompte initial à la commande" }
                )
            )
        }

        return venteId
    }

    @Transaction
    suspend fun enregistrerReglementTransaction(
        venteId: Long,
        montantVerse: Double,
        modePaiement: String,
        observation: String,
        dateReglement: Long = System.currentTimeMillis()
    ) {
        val vente = getVenteById(venteId) ?: return
        val dejaPayeBd = getTotalPayePourVente(venteId).toDzdBigDecimal()
        val resteActuelBd = vente.montantTotal.subtract(dejaPayeBd).max(java.math.BigDecimal.ZERO)
        val montantEffectifBd = montantVerse.toDzdBigDecimal()
            .max(java.math.BigDecimal.ZERO)
            .min(resteActuelBd)
            .setScale(2, java.math.RoundingMode.HALF_UP)

        val tolerance = java.math.BigDecimal("0.01")
        if (montantEffectifBd <= tolerance) return

        insertReglement(
            Reglement(
                venteId = vente.id,
                clientId = vente.clientId,
                dateReglement = dateReglement,
                montant = montantEffectifBd,
                modePaiement = modePaiement,
                observation = observation
            )
        )

        val nouveauTotalPayeBd = dejaPayeBd.add(montantEffectifBd)
        val nouveauStatut = when {
            vente.montantTotal.subtract(nouveauTotalPayeBd) <= tolerance -> "PAYE"
            nouveauTotalPayeBd > tolerance -> "PARTIEL"
            else -> "IMPAYE"
        }
        updateStatutVente(vente.id, nouveauStatut)
    }

    @Query(
        """
        SELECT * FROM ventes
        WHERE client_id = :clientId AND statut != 'PAYE'
        ORDER BY date_vente ASC, id ASC
        """
    )
    suspend fun getVentesNonSoldeesPourClientAsc(clientId: Long): List<VenteEntity>

    @Transaction
    suspend fun enregistrerReglementGlobalClientTransaction(
        clientId: Long,
        montantGlobal: Double,
        modePaiement: String,
        observation: String,
        dateReglement: Long = System.currentTimeMillis()
    ) {
        var budgetRestant = montantGlobal
        val ventesImpayees = getVentesNonSoldeesPourClientAsc(clientId)
        for (vente in ventesImpayees) {
            if (budgetRestant <= 0.01) break
            val dejaPaye = getTotalPayePourVente(vente.id)
            val resteSurVente = max(0.0, vente.montantTotal.toDouble() - dejaPaye)
            if (resteSurVente > 0.01) {
                val aAffecter = min(budgetRestant, resteSurVente)
                enregistrerReglementTransaction(
                    venteId = vente.id,
                    montantVerse = aAffecter,
                    modePaiement = modePaiement,
                    observation = observation.ifBlank { "Règlement sur solde client" },
                    dateReglement = dateReglement
                )
                budgetRestant -= aAffecter
            }
        }
    }

    // ─── 4. PURGE GLOBALE DES DONNÉES DE TEST ────────────────────────────────

    @Query("DELETE FROM reglements")
    suspend fun clearAllReglements()

    @Query("DELETE FROM lignes_vente")
    suspend fun clearAllLignesVente()

    @Query("DELETE FROM ventes")
    suspend fun clearAllVentes()

    @Query("DELETE FROM mouvements_stock")
    suspend fun clearAllMouvementsStock()

    @Query("DELETE FROM clients")
    suspend fun clearAllClients()

    @Query("DELETE FROM formats_produits")
    suspend fun clearAllFormats()

    @Query("UPDATE formats_produits SET stock_actuel = 0")
    suspend fun resetAllFormatsStockToZero()

    @Transaction
    suspend fun viderToutesLesDonneesTransaction(conserverFormats: Boolean) {
        clearAllReglements()
        clearAllLignesVente()
        clearAllVentes()
        clearAllMouvementsStock()
        clearAllClients()
        if (conserverFormats) {
            resetAllFormatsStockToZero()
        } else {
            clearAllFormats()
        }
    }

    // ─── 5. REQUÊTES D'AGRÉGATION TEMPORELLE DU DASHBOARD FINANCIER ──────────

    @Query(
        """
        SELECT COALESCE(SUM(montant_total), 0.0)
        FROM ventes
        WHERE date_vente BETWEEN :startMillis AND :endMillis
        """
    )
    fun observeCaFacturePeriode(startMillis: Long, endMillis: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(montant), 0.0)
        FROM reglements
        WHERE date_reglement BETWEEN :startMillis AND :endMillis
        """
    )
    fun observeTresorerieEncaisseePeriode(startMillis: Long, endMillis: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(MAX(0.0, v.montant_total - COALESCE(r_sum.total_paye, 0.0))), 0.0)
        FROM ventes v
        LEFT JOIN (
            SELECT vente_id, SUM(montant) AS total_paye
            FROM reglements
            GROUP BY vente_id
        ) r_sum ON r_sum.vente_id = v.id
        WHERE v.date_vente BETWEEN :startMillis AND :endMillis
          AND (v.montant_total - COALESCE(r_sum.total_paye, 0.0)) > 0.01
        """
    )
    fun observeCreancesPeriode(startMillis: Long, endMillis: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(MAX(0.0, v.montant_total - COALESCE(r_sum.total_paye, 0.0))), 0.0)
        FROM ventes v
        LEFT JOIN (
            SELECT vente_id, SUM(montant) AS total_paye
            FROM reglements
            GROUP BY vente_id
        ) r_sum ON r_sum.vente_id = v.id
        WHERE (v.montant_total - COALESCE(r_sum.total_paye, 0.0)) > 0.01
        """
    )
    fun observeCreancesGlobalesTotales(): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(stock_actuel * prix_unitaire), 0.0) AS valeur_stock_total,
               COALESCE(SUM(stock_actuel), 0) AS total_unites_stock,
               COALESCE(SUM(CASE WHEN stock_actuel <= seuil_alerte THEN 1 ELSE 0 END), 0) AS nb_formats_alerte
        FROM formats_produits
        WHERE actif = 1
        """
    )
    fun observeValorisationStock(): Flow<StockValorisationSummary>

    @Query(
        """
        SELECT strftime('%Y-%m-%d', date_vente / 1000, 'unixepoch', 'localtime') AS periode_label,
               COALESCE(SUM(montant_total), 0.0) AS montant_total
        FROM ventes
        WHERE date_vente BETWEEN :startMillis AND :endMillis
        GROUP BY periode_label
        ORDER BY periode_label ASC
        """
    )
    fun observeVentesParJour(startMillis: Long, endMillis: Long): Flow<List<PeriodAggregationPoint>>

    @Query(
        """
        SELECT strftime('%Y-%m-%d', date_reglement / 1000, 'unixepoch', 'localtime') AS periode_label,
               COALESCE(SUM(montant), 0.0) AS montant_total
        FROM reglements
        WHERE date_reglement BETWEEN :startMillis AND :endMillis
        GROUP BY periode_label
        ORDER BY periode_label ASC
        """
    )
    fun observeEncaissementsParJour(startMillis: Long, endMillis: Long): Flow<List<PeriodAggregationPoint>>

    @Query(
        """
        SELECT strftime('%Y-%m', date_vente / 1000, 'unixepoch', 'localtime') AS periode_label,
               COALESCE(SUM(montant_total), 0.0) AS montant_total
        FROM ventes
        WHERE date_vente BETWEEN :startMillis AND :endMillis
        GROUP BY periode_label
        ORDER BY periode_label ASC
        """
    )
    fun observeVentesParMois(startMillis: Long, endMillis: Long): Flow<List<PeriodAggregationPoint>>

    @Query(
        """
        SELECT strftime('%Y-%m', date_reglement / 1000, 'unixepoch', 'localtime') AS periode_label,
               COALESCE(SUM(montant), 0.0) AS montant_total
        FROM reglements
        WHERE date_reglement BETWEEN :startMillis AND :endMillis
        GROUP BY periode_label
        ORDER BY periode_label ASC
        """
    )
    fun observeEncaissementsParMois(startMillis: Long, endMillis: Long): Flow<List<PeriodAggregationPoint>>

    @Query(
        """
        SELECT f.id AS format_id,
               f.nom_format,
               f.contenance,
               COALESCE(SUM(lv.quantite), 0) AS quantite_totale_vendue,
               COALESCE(SUM(lv.sous_total), 0.0) AS chiffre_affaires_format
        FROM formats_produits f
        INNER JOIN lignes_vente lv ON lv.format_id = f.id
        INNER JOIN ventes v ON v.id = lv.vente_id
        WHERE v.date_vente BETWEEN :startMillis AND :endMillis
        GROUP BY f.id, f.nom_format, f.contenance
        ORDER BY chiffre_affaires_format DESC
        """
    )
    fun observeRepartitionVentesParFormat(startMillis: Long, endMillis: Long): Flow<List<FormatSalesBreakdownRow>>

    @Query(
        """
        SELECT c.id,
               c.nom,
               c.telephone,
               c.adresse,
               c.notes,
               COALESCE(v_tot.total_commande, 0.0) AS total_commande,
               COALESCE(r_tot.total_paye, 0.0) AS total_paye,
               (COALESCE(v_tot.total_commande, 0.0) - COALESCE(r_tot.total_paye, 0.0)) AS solde_debiteur,
               COALESCE(v_tot.nb_ventes, 0) AS nb_ventes,
               v_tot.plus_ancienne_impayee AS plus_ancienne_impayee
        FROM clients c
        INNER JOIN (
            SELECT client_id,
                   SUM(montant_total) AS total_commande,
                   COUNT(id) AS nb_ventes,
                   MIN(CASE WHEN statut != 'PAYE' THEN date_vente ELSE NULL END) AS plus_ancienne_impayee
            FROM ventes
            GROUP BY client_id
        ) v_tot ON v_tot.client_id = c.id
        LEFT JOIN (
            SELECT client_id,
                   SUM(montant) AS total_paye
            FROM reglements
            GROUP BY client_id
        ) r_tot ON r_tot.client_id = c.id
        WHERE (COALESCE(v_tot.total_commande, 0.0) - COALESCE(r_tot.total_paye, 0.0)) > 0.01
        ORDER BY solde_debiteur DESC
        LIMIT :limit
        """
    )
    fun observeTopClientsDebiteurs(limit: Int = 6): Flow<List<ClientWithBalanceRow>>
}
