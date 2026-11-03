package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * Entité Room représentant un règlement / acompte échelonné d'un client (table `reglements`).
 * Le montant versé est stocké en DZD via BigDecimal avec la date et le mode de paiement
 * ('ESPECES', 'CHEQUE', 'VIREMENT', 'AUTRE').
 */
@Entity(
    tableName = "reglements",
    foreignKeys = [
        ForeignKey(
            entity = Vente::class,
            parentColumns = ["id"],
            childColumns = ["vente_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["vente_id"], name = "idx_reglements_vente"),
        Index(value = ["client_id", "date_reglement"], name = "idx_reglements_client_date"),
        Index(value = ["date_reglement"], name = "idx_reglements_date")
    ]
)
data class Reglement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "vente_id")
    val venteId: Long,
    @ColumnInfo(name = "client_id")
    val clientId: Long,
    @ColumnInfo(name = "date_reglement")
    val dateReglement: Long = System.currentTimeMillis(),
    val montant: BigDecimal,
    @ColumnInfo(name = "mode_paiement")
    val modePaiement: String, // "ESPECES", "CHEQUE", "VIREMENT", "AUTRE"
    val observation: String = ""
)
