package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * Entité Room représentant une ligne de vente (table `lignes_vente`).
 * Conserve `prixUnitaireFige` et `sousTotal` en BigDecimal (DZD) afin de figer
 * le tarif unitaire appliqué au moment précis de la vente.
 */
@Entity(
    tableName = "lignes_vente",
    foreignKeys = [
        ForeignKey(
            entity = Vente::class,
            parentColumns = ["id"],
            childColumns = ["vente_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FormatProduit::class,
            parentColumns = ["id"],
            childColumns = ["format_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["vente_id"], name = "idx_lignes_vente_vente"),
        Index(value = ["format_id"], name = "idx_lignes_vente_format")
    ]
)
data class LigneVente(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "vente_id")
    val venteId: Long,
    @ColumnInfo(name = "format_id")
    val formatId: Long,
    val quantite: Int,
    @ColumnInfo(name = "prix_unitaire_fige")
    val prixUnitaireFige: BigDecimal,
    @ColumnInfo(name = "sous_total")
    val sousTotal: BigDecimal
)
