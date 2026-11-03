package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Entité Room représentant un format dynamique de conserve (table `formats_produits`).
 * Exemples : Bocal 150g, Bocal 350g, Seau 1kg, Bidon 2kg, Bidon 2.5kg.
 * Le prix forfaitaire unitaire est stocké avec précision financière exacte en DZD via BigDecimal.
 */
@Entity(
    tableName = "formats_produits",
    indices = [Index(value = ["actif"], name = "idx_formats_actif")]
)
data class FormatProduit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "nom_format")
    val nomFormat: String,
    val contenance: String,
    @ColumnInfo(name = "prix_unitaire")
    val prixUnitaire: BigDecimal,
    @ColumnInfo(name = "stock_actuel")
    val stockActuel: Int = 0,
    @ColumnInfo(name = "seuil_alerte")
    val seuilAlerte: Int = 10,
    val actif: Int = 1
) {
    val valeurMarchandeStockDzd: BigDecimal
        get() = prixUnitaire
            .multiply(BigDecimal.valueOf(stockActuel.toLong()))
            .setScale(2, RoundingMode.HALF_UP)

    val estEnAlerteStock: Boolean
        get() = stockActuel <= seuilAlerte
}
