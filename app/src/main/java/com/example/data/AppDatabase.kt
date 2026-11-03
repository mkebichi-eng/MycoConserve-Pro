package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.ConserverieDao
import com.example.data.local.MouvementStockEntity
import com.example.data.local.PanierItemDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Base de données SQLite locale Room (`AppDatabase`) regroupant les entités
 * Client, FormatProduit, Vente, LigneVente, Reglement et MouvementStockEntity.
 */
@Database(
    entities = [
        Client::class,
        FormatProduit::class,
        Vente::class,
        LigneVente::class,
        Reglement::class,
        MouvementStockEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(BigDecimalConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conserverieDao(): ConserverieDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val PREFS_NAME = "mycoconserve_db_prefs"
        private const val KEY_SEED_DONE = "initial_seed_done"

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appCtx = context.applicationContext
                val instance = Room.databaseBuilder(
                    appCtx,
                    AppDatabase::class.java,
                    "mycoconserve_artisan.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                CoroutineScope(Dispatchers.IO).launch {
                    val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    if (!prefs.getBoolean(KEY_SEED_DONE, false)) {
                        seedDatabaseIfEmpty(instance.conserverieDao())
                        prefs.edit().putBoolean(KEY_SEED_DONE, true).apply()
                    }
                }
                instance
            }
        }

        fun markSeedDismissed(context: Context) {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SEED_DONE, true)
                .apply()
        }

        suspend fun seedDatabaseIfEmpty(dao: ConserverieDao) {
            if (dao.countFormats() > 0) return

            val now = System.currentTimeMillis()
            val hourMs = 3_600_000L
            val dayMs = 86_400_000L

            // 1. Formats de conserves dynamiques (Tarifs en DZD / DA via BigDecimal)
            val fBocal150 = FormatProduit(
                nomFormat = "Bocal Cèpes Marinés",
                contenance = "150g",
                prixUnitaire = 750.toDzdBigDecimal(),
                stockActuel = 120,
                seuilAlerte = 25
            )
            val fBocal350 = FormatProduit(
                nomFormat = "Bocal Girolles Forestières",
                contenance = "350g",
                prixUnitaire = 1450.toDzdBigDecimal(),
                stockActuel = 85,
                seuilAlerte = 20
            )
            val fSeau1kgParis = FormatProduit(
                nomFormat = "Seau Champignons Entiers",
                contenance = "1kg",
                prixUnitaire = 1950.toDzdBigDecimal(),
                stockActuel = 64,
                seuilAlerte = 15
            )
            val fSeau1kgPleurotes = FormatProduit(
                nomFormat = "Seau Pleurotes Grillés",
                contenance = "1kg",
                prixUnitaire = 2200.toDzdBigDecimal(),
                stockActuel = 14,
                seuilAlerte = 15
            )
            val fBidon2kg = FormatProduit(
                nomFormat = "Bidon Pro Émincés",
                contenance = "2kg",
                prixUnitaire = 3600.toDzdBigDecimal(),
                stockActuel = 48,
                seuilAlerte = 12
            )
            val fBidon25kg = FormatProduit(
                nomFormat = "Bidon Restauration Cèpes",
                contenance = "2.5kg",
                prixUnitaire = 4800.toDzdBigDecimal(),
                stockActuel = 9,
                seuilAlerte = 12
            )

            val idBocal150 = dao.insertFormat(fBocal150)
            val idBocal350 = dao.insertFormat(fBocal350)
            val idSeau1kgParis = dao.insertFormat(fSeau1kgParis)
            val idSeau1kgPleurotes = dao.insertFormat(fSeau1kgPleurotes)
            val idBidon2kg = dao.insertFormat(fBidon2kg)
            val idBidon25kg = dao.insertFormat(fBidon25kg)

            val savedBocal150 = fBocal150.copy(id = idBocal150)
            val savedBocal350 = fBocal350.copy(id = idBocal350)
            val savedSeau1kgParis = fSeau1kgParis.copy(id = idSeau1kgParis)
            val savedSeau1kgPleurotes = fSeau1kgPleurotes.copy(id = idSeau1kgPleurotes)
            val savedBidon2kg = fBidon2kg.copy(id = idBidon2kg)
            val savedBidon25kg = fBidon25kg.copy(id = idBidon25kg)

            // Mouvements de stock initiaux
            dao.insertMouvementStock(
                MouvementStockEntity(
                    formatId = idBocal150,
                    typeMouvement = "PRODUCTION",
                    quantiteDelta = 150,
                    stockApres = 150,
                    dateMouvement = now - 28 * dayMs,
                    motif = "Lot Autoclave #101 — Récolte Chréa"
                )
            )
            dao.insertMouvementStock(
                MouvementStockEntity(
                    formatId = idSeau1kgParis,
                    typeMouvement = "PRODUCTION",
                    quantiteDelta = 95,
                    stockApres = 95,
                    dateMouvement = now - 21 * dayMs,
                    motif = "Lot Stérilisation #104 — Saumure douce"
                )
            )
            dao.insertMouvementStock(
                MouvementStockEntity(
                    formatId = idBidon25kg,
                    typeMouvement = "PRODUCTION",
                    quantiteDelta = 32,
                    stockApres = 32,
                    dateMouvement = now - 15 * dayMs,
                    motif = "Lot Restauration #108"
                )
            )
            dao.insertMouvementStock(
                MouvementStockEntity(
                    formatId = idBocal350,
                    typeMouvement = "PERTE_CASSE",
                    quantiteDelta = -3,
                    stockApres = 85,
                    dateMouvement = now - 4 * dayMs,
                    motif = "Casse thermique sortie autoclave"
                )
            )

            // 2. Clients de la conserverie
            val idClientAtlas = dao.insertClient(
                Client(
                    nom = "Restaurant El Djenina & Grill",
                    telephone = "0550 42 18 90",
                    adresse = "Rue Didouche Mourad, Alger Centre",
                    notes = "Livraison chaque samedi matin, privilégie les bidons 2kg et 2.5kg.",
                    dateCreation = now - 60 * dayMs
                )
            )
            val idClientGourmet = dao.insertClient(
                Client(
                    nom = "Épicerie Fine Terroir Kabyle",
                    telephone = "0661 88 34 12",
                    adresse = "Boulevard Krim Belkacem, Tizi Ouzou",
                    notes = "Commande régulièrement les bocaux 150g et 350g.",
                    dateCreation = now - 55 * dayMs
                )
            )
            val idClientSorex = dao.insertClient(
                Client(
                    nom = "Grossiste Alimentaire Numidia",
                    telephone = "0770 19 65 43",
                    adresse = "Zone Industrielle Palma, Constantine",
                    notes = "Règlement par chèque ou virement en 2 à 3 échéances.",
                    dateCreation = now - 45 * dayMs
                )
            )
            val idClientTrattoria = dao.insertClient(
                Client(
                    nom = "Pizzeria Trattoria Santa Cruz",
                    telephone = "0558 91 04 27",
                    adresse = "Front de Mer, Oran",
                    notes = "Forte demande en Seaux 1kg et Bidons 2kg.",
                    dateCreation = now - 40 * dayMs
                )
            )
            val idClientCasbah = dao.insertClient(
                Client(
                    nom = "Hôtel & Traiteur Les Cèdres",
                    telephone = "0662 54 71 09",
                    adresse = "Route Nationale 9, Béjaïa",
                    notes = "Commandes événementielles, paiement échelonné.",
                    dateCreation = now - 35 * dayMs
                )
            )

            // 3. Ventes historiques et récentes avec règlements échelonnés
            val v1Date = now - 24 * dayMs
            val v1Id = dao.creerVenteCompleteTransaction(
                clientId = idClientSorex,
                panier = listOf(
                    PanierItemDraft(savedBidon2kg, 10),
                    PanierItemDraft(savedSeau1kgParis, 15)
                ),
                acompteInitial = 35000.0,
                modePaiementAcompte = "CHEQUE",
                observationAcompte = "Chèque BNA n°448120 à la livraison",
                notesVente = "Commande mensuelle grossiste",
                dateVente = v1Date
            )
            dao.enregistrerReglementTransaction(
                venteId = v1Id,
                montantVerse = 30250.0,
                modePaiement = "VIREMENT",
                observation = "Solde facture #1 par virement bancaire",
                dateReglement = now - 14 * dayMs
            )

            val v2Date = now - 18 * dayMs
            val v2Id = dao.creerVenteCompleteTransaction(
                clientId = idClientCasbah,
                panier = listOf(
                    PanierItemDraft(savedBidon25kg, 8),
                    PanierItemDraft(savedBocal350, 12)
                ),
                acompteInitial = 20000.0,
                modePaiementAcompte = "ESPECES",
                observationAcompte = "1er versement espèces à réception",
                notesVente = "Banquet réception mariage",
                dateVente = v2Date
            )
            dao.enregistrerReglementTransaction(
                venteId = v2Id,
                montantVerse = 15000.0,
                modePaiement = "ESPECES",
                observation = "2ème acompte espèces",
                dateReglement = now - 8 * dayMs
            )

            val v3Date = now - 5 * dayMs
            val v3Id = dao.creerVenteCompleteTransaction(
                clientId = idClientSorex,
                panier = listOf(
                    PanierItemDraft(savedBidon2kg, 12),
                    PanierItemDraft(savedSeau1kgPleurotes, 10),
                    PanierItemDraft(savedBocal150, 20)
                ),
                acompteInitial = 30000.0,
                modePaiementAcompte = "VIREMENT",
                observationAcompte = "Avance 1/3 par virement",
                notesVente = "Réapprovisionnement régional Est",
                dateVente = v3Date
            )
            dao.enregistrerReglementTransaction(
                venteId = v3Id,
                montantVerse = 20000.0,
                modePaiement = "ESPECES",
                observation = "2ème versement chauffeur",
                dateReglement = now - 2 * dayMs
            )

            dao.creerVenteCompleteTransaction(
                clientId = idClientGourmet,
                panier = listOf(
                    PanierItemDraft(savedBocal150, 24),
                    PanierItemDraft(savedBocal350, 16)
                ),
                acompteInitial = 41200.0,
                modePaiementAcompte = "ESPECES",
                observationAcompte = "Règlement intégral comptant",
                notesVente = "Coffrets dégustation terroir",
                dateVente = now - 3 * dayMs
            )

            dao.creerVenteCompleteTransaction(
                clientId = idClientTrattoria,
                panier = listOf(
                    PanierItemDraft(savedSeau1kgParis, 14),
                    PanierItemDraft(savedBidon2kg, 6)
                ),
                acompteInitial = 0.0,
                modePaiementAcompte = "ESPECES",
                observationAcompte = "",
                notesVente = "À régler fin de semaine",
                dateVente = now - 2 * dayMs
            )

            dao.creerVenteCompleteTransaction(
                clientId = idClientAtlas,
                panier = listOf(
                    PanierItemDraft(savedBidon25kg, 6),
                    PanierItemDraft(savedSeau1kgPleurotes, 8)
                ),
                acompteInitial = 25000.0,
                modePaiementAcompte = "ESPECES",
                observationAcompte = "Acompte espèces livraison matin",
                notesVente = "Service week-end",
                dateVente = now - 4 * hourMs
            )

            dao.creerVenteCompleteTransaction(
                clientId = idClientGourmet,
                panier = listOf(
                    PanierItemDraft(savedBocal150, 12),
                    PanierItemDraft(savedBocal350, 6)
                ),
                acompteInitial = 17700.0,
                modePaiementAcompte = "CHEQUE",
                observationAcompte = "Chèque CPA remis en main propre",
                notesVente = "Complément vitrine",
                dateVente = now - 1 * hourMs
            )
        }
    }
}
