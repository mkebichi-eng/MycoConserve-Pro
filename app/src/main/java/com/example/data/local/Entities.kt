package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.BigDecimalConverters
import com.example.data.Client
import com.example.data.FormatProduit
import com.example.data.LigneVente
import com.example.data.Reglement
import com.example.data.Vente
import java.math.BigDecimal
import java.math.RoundingMode

fun Double.toDzdBigDecimal(): BigDecimal =
    BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP)

fun Int.toDzdBigDecimal(): BigDecimal =
    BigDecimal.valueOf(this.toLong()).setScale(2, RoundingMode.HALF_UP)

// Re-export des entités du package `com.example.data` pour compatibilité avec les couches DAO / UI
typealias ClientEntity = Client
typealias FormatProduitEntity = FormatProduit
typealias VenteEntity = Vente
typealias LigneVenteEntity = LigneVente
typealias ReglementEntity = Reglement
typealias AppDatabase = com.example.data.AppDatabase

/**
 * Entité MouvementStock (table `mouvements_stock`)
 */
@Entity(
    tableName = "mouvements_stock",
    foreignKeys = [
        ForeignKey(
            entity = FormatProduit::class,
            parentColumns = ["id"],
            childColumns = ["format_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["format_id", "date_mouvement"], name = "idx_mouvements_format_date"),
        Index(value = ["date_mouvement"], name = "idx_mouvements_date")
    ]
)
data class MouvementStockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "format_id")
    val formatId: Long,
    @ColumnInfo(name = "type_mouvement")
    val typeMouvement: String, // "PRODUCTION", "VENTE", "PERTE_CASSE", "AJUSTEMENT"
    @ColumnInfo(name = "quantite_delta")
    val quantiteDelta: Int,
    @ColumnInfo(name = "stock_apres")
    val stockApres: Int,
    @ColumnInfo(name = "date_mouvement")
    val dateMouvement: Long = System.currentTimeMillis(),
    val motif: String = "",
    @ColumnInfo(name = "vente_id")
    val venteId: Long? = null
)

// Modèles de projection SQL

data class VenteResumeRow(
    val id: Long,
    @ColumnInfo(name = "client_id") val clientId: Long,
    @ColumnInfo(name = "client_nom") val clientNom: String,
    @ColumnInfo(name = "client_telephone") val clientTelephone: String,
    @ColumnInfo(name = "date_vente") val dateVente: Long,
    @ColumnInfo(name = "montant_total") val montantTotal: Double,
    val statut: String,
    val notes: String,
    @ColumnInfo(name = "montant_paye") val montantPaye: Double,
    @ColumnInfo(name = "reste_a_payer") val resteAPayer: Double
)

data class LigneVenteDetailRow(
    val id: Long,
    @ColumnInfo(name = "vente_id") val venteId: Long,
    @ColumnInfo(name = "format_id") val formatId: Long,
    @ColumnInfo(name = "nom_format") val nomFormat: String,
    val contenance: String,
    val quantite: Int,
    @ColumnInfo(name = "prix_unitaire_fige") val prixUnitaireFige: BigDecimal,
    @ColumnInfo(name = "sous_total") val sousTotal: BigDecimal
)

data class ClientWithBalanceRow(
    val id: Long,
    val nom: String,
    val telephone: String,
    val adresse: String,
    val notes: String,
    @ColumnInfo(name = "total_commande") val totalCommande: Double,
    @ColumnInfo(name = "total_paye") val totalPaye: Double,
    @ColumnInfo(name = "solde_debiteur") val soldeDebiteur: Double,
    @ColumnInfo(name = "nb_ventes") val nbVentes: Int,
    @ColumnInfo(name = "plus_ancienne_impayee") val plusAncienneImpayee: Long?
)

data class PeriodAggregationPoint(
    @ColumnInfo(name = "periode_label") val periodeLabel: String,
    @ColumnInfo(name = "montant_total") val montantTotal: Double
)

data class FormatSalesBreakdownRow(
    @ColumnInfo(name = "format_id") val formatId: Long,
    @ColumnInfo(name = "nom_format") val nomFormat: String,
    val contenance: String,
    @ColumnInfo(name = "quantite_totale_vendue") val quantiteTotaleVendue: Int,
    @ColumnInfo(name = "chiffre_affaires_format") val chiffreAffairesFormat: Double
)

data class StockValorisationSummary(
    @ColumnInfo(name = "valeur_stock_total") val valeurStockTotal: Double,
    @ColumnInfo(name = "total_unites_stock") val totalUnitesStock: Int,
    @ColumnInfo(name = "nb_formats_alerte") val nbFormatsAlerte: Int
)

data class MouvementStockWithFormatRow(
    val id: Long,
    @ColumnInfo(name = "format_id") val formatId: Long,
    @ColumnInfo(name = "nom_format") val nomFormat: String,
    val contenance: String,
    @ColumnInfo(name = "type_mouvement") val typeMouvement: String,
    @ColumnInfo(name = "quantite_delta") val quantiteDelta: Int,
    @ColumnInfo(name = "stock_apres") val stockApres: Int,
    @ColumnInfo(name = "date_mouvement") val dateMouvement: Long,
    val motif: String
)

data class PanierItemDraft(
    val format: FormatProduit,
    val quantite: Int
)
