package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entité Room représentant un client de la conserverie (table `clients`).
 */
@Entity(
    tableName = "clients",
    indices = [Index(value = ["nom"], name = "idx_clients_nom")]
)
data class Client(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nom: String,
    val telephone: String = "",
    val adresse: String = "",
    val notes: String = "",
    @ColumnInfo(name = "date_creation")
    val dateCreation: Long = System.currentTimeMillis()
)
