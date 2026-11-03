# MycoConserve Pro — Suppression Complète des Données & Refonte Ergonomique Compacte

Mise à jour fonctionnelle et ergonomique de **MycoConserve Pro** permettant de supprimer individuellement tout enregistrement (client, vente, format, lot/mouvement, règlement) avec réajustement automatique du stock, de vider intégralement les données de test en un clic pour démarrer en production réelle, et de compacter tous les en-têtes et boutons `+` afin de maximiser l'espace utile à l'écran.

## User Review & Critical Decisions

> [!IMPORTANT]
> Ce plan intègre vos trois décisions confirmées pour nettoyer les données de démonstration et aérer l'interface mobile.

- **Décision Confirmée 1 — Suppression Individuelle + Réinitialisation Globale** : Ajout d'une action de suppression sur chaque carte (**Client**, **Vente**, **Format de conserve**, **Lot / Mouvement de stock**, **Règlement**) ET d'un bouton global **« Vider les données (Réinitialiser) »** avec boîte de confirmation et blocage définitif du re-remplissage automatique des données de test.
- **Décision Confirmée 2 — Recalcul Transactionnel Automatique du Stock** :
  - *Suppression d'une vente* : restitue automatiquement au stock les quantités de chaque format facturé dans cette vente.
  - *Suppression d'un lot de production ou mouvement de casse* : annule l'impact de ce mouvement sur le `stock_actuel` du format concerné.
  - *Suppression d'un règlement* : recalcule immédiatement le reste à payer et le statut (`IMPAYE`, `PARTIEL`, `PAYE`) de la vente associée.
- **Décision Confirmée 3 — En-têtes Ultra-Compacts sur 1 Ligne & Petits Boutons `+`** :
  - Remplacement des grands blocs d'en-tête et des gros boutons par une barre supérieure fine sur **une seule ligne** associant le titre, le résumé chiffré en DZD et un petit bouton compact `+`.
  - Sur l'écran **Formats & Stock**, le bandeau de valorisation est condensé en une barre fine intégrée, libérant plus de **78 % de l'écran** pour la liste des formats et des mouvements.

---

## 1. Overview & Core Concept

- **Ce que fait cette mise à jour** :
  1. Permet à l'artisan de purger en un geste toutes les données de démonstration (ou de supprimer au cas par cas une erreur de saisie sur un client, une vente, un acompte ou un lot) tout en maintenant une cohérence comptable et de stock irréprochable.
  2. Corrige la densité visuelle sur smartphone : les en-têtes, barres de recherche, résumés de stock et boutons d'ajout (`+`) sont affinés pour éviter que le haut de l'écran n'occupe la moitié de la surface d'affichage.
- **Valeur Ajoutée Clé** :
  - **Passage immédiat en conditions réelles** : Une fois les données de test purgées, l'application enregistre que la base a été initialisée et ne recrée plus jamais les données fictives au redémarrage.
  - **Gain d'espace vertical (+35 % à +45 % de liste visible)** : L'utilisateur visualise immédiatement 3 à 4 cartes de formats ou de ventes supplémentaires sans avoir à défiler.

---

## 2. User Experience & Visual Design

### 2.1 Parcours de Suppression (Individuelle & Globale)

1. **Bouton Global « Vider les données » (Accessible depuis le Dashboard et l'écran Stock)** :
   - Une icône discrète **« Purger / Réinitialiser »** dans l'en-tête ouvre une boîte de confirmation sécurisée proposant :
     - **Option A — Vider toutes les données (Ventes, Clients, Lots & Formats)** : remet la base à zéro pour repartir sur une base 100 % vierge.
     - **Option B — Supprimer uniquement l'historique (Ventes, Règlements & Lots) en conservant le catalogue des Formats**.
2. **Suppression d'une Vente (Écran Ventes & Fiche Client)** :
   - Dans chaque carte de vente, une icône corbeille (`Supprimer la vente`) ouvre une confirmation indiquant : *« Supprimer la vente #X et restituer les quantités vendues au stock ? »*.
   - À la validation, les quantités sont réintégrées dans `formats_produits.stock_actuel`, les lignes et règlements associés sont supprimés en cascade, et le solde du client est mis à jour en direct.
3. **Suppression d'un Client (Écran Clients & Fiche Client)** :
   - Bouton corbeille sur chaque fiche client avec confirmation : supprime le client ainsi que ses ventes et règlements associés (en restituant le stock des ventes supprimées).
4. **Suppression d'un Format ou d'un Lot / Mouvement (Écran Stock)** :
   - *Sur une carte Format* : icône corbeille à côté du crayon d'édition permettant de supprimer/archiver le format.
   - *Sur une ligne du Journal des Mouvements (Lot de production ou Casse)* : icône corbeille permettant d'annuler le lot ou l'ajustement et de recalculer instantanément le `stock_actuel` du format.

### 2.2 Refonte Ergonomique Ultra-Compacte des Écrans

- **En-tête d'écran sur 1 seule ligne (`44.dp` de hauteur)** :
  - À gauche : Titre de la section + badge compact du compteur ou de la valeur totale (ex. `Stock • 482 500 DA`).
  - À droite : Petit bouton compact (`34.dp` de hauteur, icône `+` de `16.dp` et libellé court `+ Format`, `+ Vente`, `+ Client`).
- **Écran Formats & Stock optimisé** :
  - Suppression du grand bloc de valorisation sur deux étages : les indicateurs (*Valeur du stock*, *Unités totales*, *Alertes*) sont regroupés dans une **bande horizontale fine de `36.dp`** juste au-dessus des onglets compacts (`38.dp`).
  - Les boutons d'action de chaque carte de format (`+ Lot`, `Casse`, `Modifier`, `Supprimer`) sont logés sur une ligne compacte de `32.dp` de hauteur.
- **Écrans Ventes & Clients optimisés** :
  - Champ de recherche affiné et puces de filtres (`FilterChips`) à marges réduites pour afficher d'emblée un maximum de ventes et de clients.
  - Bannière du Dashboard condensée avec bouton d'action rapide pour purger les données de démonstration.

---

## 3. Key Product Decisions & Trade-Offs

- **Décision 1 : Transactions ACID de Suppression avec Réversibilité du Stock**
  - *Approche choisie* : La suppression d'une vente ou d'un mouvement de stock s'exécute dans une `@Transaction` Room qui lit d'abord les quantités concernées, réajuste `formats_produits.stock_actuel`, puis supprime l'enregistrement.
  - *Pourquoi* : Évite tout écart de stock lorsqu'une vente de test ou une erreur de saisie de lot est supprimée.
- **Décision 2 : Persistance de l'état « Données de test purgées » (`SharedPreferences`)**
  - *Approche choisie* : Dès le premier peuplement initial ou dès que l'utilisateur clique sur « Vider les données », un drapeau local `seed_completed = true` est enregistré.
  - *Pourquoi* : Sans ce drapeau, le test `if (dao.countFormats() == 0)` réinjecterait automatiquement les données fictives au lancement suivant après une purge totale.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### 4.1 Diagramme des Flux de Suppression Transactionnelle & UI Compacte

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                 INTERFACE JETPACK COMPOSE ULTRA-COMPACTE                     │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │ Barre d'En-tête Fine (1 ligne, 44.dp) : Titre + Résumé DZD + Petit [+] │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐  ┌────────────┐  │
│  │   Dashboard    │  │     Ventes     │  │    Clients     │  │   Stock    │  │
│  │ • Purge Globale│  │ • Suppr. Vente │  │ • Suppr. Client│  │ • Suppr.Fmt│  │
│  │ • Bandeau Fin  │  │ • Suppr. Acompt│  │ • Suppr. Fact. │  │ • Suppr.Lot│  │
│  └───────┬────────┘  └───────┬────────┘  └───────┬────────┘  └─────┬──────┘  │
└──────────┼───────────────────┼───────────────────┼─────────────────┼─────────┘
           │                   │                   │                 │
           ▼                   ▼                   ▼                 ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                  TRANSACTIONS ACID DE SUPPRESSION (Room DAO)                 │
│  • supprimerVenteEtRestaurerStockTransaction(venteId)                        │
│      1. Lit lignes_vente(venteId) -> Ajoute +quantite à formats_produits     │
│      2. Supprime mouvements_stock liés, reglements et vente                  │
│  • supprimerMouvementStockTransaction(mouvementId)                           │
│      1. Lit quantite_delta -> Applique (-quantite_delta) à stock_actuel      │
│      2. Supprime la ligne mouvements_stock                                   │
│  • supprimerClientCompletTransaction(clientId)                               │
│      1. Supprime chaque vente du client (avec restauration du stock)         │
│      2. Supprime la fiche client                                             │
│  • viderToutesLesDonneesTransaction(conserverFormats)                        │
│      1. Vide reglements, lignes_vente, ventes, mouvements_stock, clients     │
│      2. Vide formats_produits (ou remet stock_actuel = 0 si conservés)       │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Nouvelles Requêtes SQL & Opérations DAO

```sql
-- Suppression unitaire d'un règlement (suivie du recalcul du statut de la vente)
DELETE FROM reglements WHERE id = :reglementId;

-- Suppression des mouvements de stock liés à une vente annulée
DELETE FROM mouvements_stock WHERE vente_id = :venteId;

-- Suppression d'une vente (les lignes_vente et reglements sont purgés via ON DELETE CASCADE)
DELETE FROM ventes WHERE id = :venteId;

-- Suppression d'un client
DELETE FROM clients WHERE id = :clientId;

-- Suppression d'un format (ou désactivation si encore utilisé, ou purge directe)
DELETE FROM formats_produits WHERE id = :formatId;

-- Purge globale de toutes les données de test
DELETE FROM reglements;
DELETE FROM lignes_vente;
DELETE FROM ventes;
DELETE FROM mouvements_stock;
DELETE FROM clients;
DELETE FROM formats_produits;
```

### 4.3 Feuille de Route d'Exécution

1. **Étape 1 — Couche DAO, Repository & Prévention du Re-Seeding** :
   - Ajout des méthodes transactionnelles `supprimerVenteEtRestaurerStockTransaction`, `supprimerReglementTransaction`, `supprimerClientCompletTransaction`, `supprimerFormatTransaction`, `supprimerMouvementStockTransaction` et `viderToutesLesDonneesTransaction`.
   - Enregistrement du drapeau `initial_seed_done` dans les préférences locales de `AppDatabase` pour que la suppression totale laisse la base définitivement propre.
2. **Étape 2 — Refonte Compacte des En-têtes & Boutons `+` (`CommonComponents`, `StockScreen`, `VentesScreen`, `ClientsScreen`, `DashboardScreen`)** :
   - Création d'un composant `CompactScreenHeader` sur une seule ligne avec petit bouton `+` (`height = 34.dp`).
   - Réduction de la hauteur du bandeau de valorisation et des cartes dans `StockScreen`, `VentesScreen`, `ClientsScreen` et `DashboardScreen`.
3. **Étape 3 — Intégration des Boutons de Suppression & Dialogues de Confirmation** :
   - Ajout des boutons corbeille (`Icons.Default.DeleteOutline`) avec confirmation sur les cartes de ventes, les lignes d'acomptes, les fiches clients, les formats de conserves et les lots/mouvements de stock, ainsi que le bouton de réinitialisation globale.
4. **Étape 4 — Vérification & Compilation (`compile_applet`)** :
   - Compilation complète et mise à jour automatique du workflow APK.
