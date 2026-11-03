package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * Entité Room représentant une vente / facture client (table `ventes`).
 * Le montant total facturé est géré en DZD via BigDecimal et le statut suit
 * l'avancement des règlements ('IMPAYE', 'PARTIEL', 'PAYE').
 */
@Entity(
    tableName = "ventes",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["date_vente"], name = "idx_ventes_date_desc"),
        Index(value = ["statut", "date_vente"], name = "idx_ventes_statut_date"),
        Index(value = ["client_id", "date_vente"], name = "idx_ventes_client")
    ]
)
data class Vente(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "client_id")
    val clientId: Long,
    @ColumnInfo(name = "date_vente")
    val dateVente: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "montant_total")
    val montantTotal: BigDecimal,
    val statut: String, // "IMPAYE", "PARTIEL", "PAYE"
    val notes: String = ""
)
