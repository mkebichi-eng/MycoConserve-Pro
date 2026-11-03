# MycoConserve Pro — Gestion Commerciale, Stock & Analyse Financière de Conserverie Artisanale

Application Android 100 % locale, autonome et hors-ligne dédiée au pilotage quotidien d'une conserverie artisanale de champignons : gestion libre des formats de conserves, traçabilité des lots de production et des pertes, ventes à crédit avec règlements échelonnés en Dinar Algérien (DZD), et tableau de bord financier analytique.

## User Review & Critical Decisions

> [!IMPORTANT]
> Ce plan intègre vos choix confirmés lors de la phase de clarification initiale afin de garantir une exécution native, fluide et immédiatement opérationnelle dans l'environnement Android.

- **Décision Confirmée 1 — Stack Native Android (Kotlin, Jetpack Compose & SQLite Room)** : L'application et son architecture sont conçues nativement avec Kotlin, Jetpack Compose (Material 3) et Room SQLite (avec support complet des transactions ACID `@Transaction`, requêtes SQL brutes optimisées, indexation et pagination `LIMIT / OFFSET`).
- **Décision Confirmée 2 — Devise Principale en Dinar Algérien (DZD)** : Tous les montants unitaires, totaux de facturation, acomptes, créances et indicateurs de trésorerie sont formatés en Dinar Algérien (`DA` / `DZD`, avec séparateurs de milliers lisibles et police tabulaire).
- **Décision Confirmée 3 — Gestion Complète des Flux de Stock** : Le module de stock prend en charge à la fois les entrées de **lots de production** (mise en conserve), la **décrémentation automatique** lors des ventes validées, et les **ajustements manuels motivés** (casse, perte, échantillon, correction d'inventaire).
- **Décision Confirmée 4 — Actions Rapides de Recouvrement Client** : Les fiches clients et le bloc « Top Clients Débiteurs » intègrent le déclenchement d'appel téléphonique direct (`ACTION_DIAL`) ainsi que le partage instantané d'un relevé de compte ou rappel d'échéance formaté via **WhatsApp ou SMS**.

---

## 1. Overview & Core Concept

- **Ce que fait l'application** : **MycoConserve Pro** remplace les carnets papier et tableurs dispersés par un outil de gestion tout-en-un fonctionnant sans aucune connexion Internet. L'artisan crée ses propres formats de conditionnement (Bocal 150 g, Bocal 350 g, Seau 1 kg, Bidon 2 kg, Bidon 2,5 kg…), enregistre ses fournées de stérilisation, saisit ses commandes clients en quelques secondes avec déduction immédiate du stock, suit chaque acompte versé en espèces, chèque ou virement, et visualise en temps réel son chiffre d'affaires facturé face à sa trésorerie réellement encaissée.
- **Public Cible / Persona** : Producteurs, artisans conserveurs de champignons et responsables commerciaux opérant en atelier, en entrepôt, sur les marchés ou en tournée de livraison auprès de restaurateurs, épiceries fines et grossistes, souvent dans des zones à connectivité limitée.
- **Valeur Ajoutée Clé** :
  1. **Fiabilité comptable et transactionnelle (ACID)** : Aucune vente ne peut être enregistrée sans mise à jour atomique du stock et du solde client ; les prix unitaires sont figés au moment de la vente (`prix_unitaire_fige`) pour préserver l'intégrité de l'historique en cas de changement de tarif ultérieur.
  2. **Maîtrise du risque client** : Visibilité immédiate sur le reste à recouvrer par vente et par client, avec relance WhatsApp/SMS et appel en un geste.
  3. **Performance hors-ligne durable** : Grâce aux index composites SQLite et à la pagination par curseur/offset, les listes et agrégations restent instantanées même après des dizaines de milliers de lignes de vente et de règlements.

---

## 2. User Experience & Visual Design

### Parcours Utilisateurs Clés (Key User Flows)

1. **Pilotage sur le Dashboard Financier (Onglet 1 — Dashboard)** :
   - L'utilisateur sélectionne en un clic la période d'analyse via une rangée de `FilterChips` horizontaux : *Aujourd'hui* | *Cette semaine* | *Ce mois* | *Cette année* | *Plage personnalisée* (ouvre un sélecteur de plage calendaire `DateRangePicker`).
   - Les **4 cartes KPI** se mettent à jour instantanément :
     - **CA Facturé** (montant total des ventes de la période en DZD).
     - **Trésorerie Encaissée** (somme réelle des règlements perçus sur la période en DZD + taux de recouvrement en %).
     - **Créances Générées** (reste à recouvrer sur les ventes de la période + encours global).
     - **Valeur Marchande du Stock** (valorisation temps réel $\sum (\text{stock\_actuel} \times \text{prix\_unitaire})$ et nombre total d'unités disponibles).
   - **Graphiques & Analyses** :
     - *Graphique comparatif Barres/Courbes* : Ventes facturées vs Encaissements réels découpés par jour (ou par mois sur la vue annuelle).
     - *Graphique de répartition par format* : Barres horizontales pondérées et anneau de répartition montrant quels formats (ex. Seau 1 kg vs Bidon 2,5 kg) génèrent le plus de chiffre d'affaires et de volume.
   - **Bloc d'action rapide « Top Clients Débiteurs »** : Affiche les clients ayant le solde débiteur le plus élevé avec leur ancienneté de dette, un bouton **Appeler** direct et un bouton **Relance WhatsApp/SMS** pré-rempli avec le détail du solde dû.

2. **Gestion de l'Historique des Ventes, Filtres & Encaissements Rapides (Onglet 2 — Ventes)** :
   - En-tête doté d'une **barre de recherche instantanée par nom de client** et de **filtres rapides par statut** : *Tous* | *Soldés* (`PAYE`) | *Partiels* (`PARTIEL`) | *Impayés* (`IMPAYE`).
   - Liste paginée fluide (*Infinite Scroll* par lots de 25 ventes via `LIMIT` & `OFFSET`) triée antéchronologiquement (`ORDER BY date_vente DESC`) avec **en-têtes collants de regroupement temporel** (*Aujourd'hui*, *Cette semaine*, *Ce mois*, *Plus ancien*).
   - **Cartes de vente pliables/dépliables** :
     - *État replié* : Nom du client, date/heure, badge coloré de statut, montant total en DZD, et indicateur saillant du **Reste dû**.
     - *État déplié* : Liste détaillée des formats vendus (quantité $\times$ prix figé = sous-total), historique chronologique des acomptes déjà versés (date, montant, mode de paiement), et bouton d'action primaire **« + Enregistrer un acompte »** (masqué si la vente est déjà soldée).
   - **Création d'une Nouvelle Vente (Bouton flottant `+ Nouvelle Vente`)** :
     - Sélection d'un client existant ou création rapide à la volée.
     - Sélection multi-formats avec contrôle de stock en direct (alerte visuelle si la quantité demandée dépasse le stock disponible).
     - Option d'encaissement immédiat au comptant (Total, Partiel avec saisie du montant versé et du mode de paiement : *Espèces*, *Chèque*, *Virement*, ou *À crédit 0 DA*).
     - Validation atomique : crée la vente, insère les lignes, enregistre l'acompte initial éventuel et décrémente les stocks de chaque format dans une seule transaction SQLite.

3. **Fiches Clients & Suivi Rigoureux des Règlements Échelonnés (Onglet 3 — Clients)** :
   - Annuaire des clients avec recherche rapide, filtre « Débiteurs uniquement » et indicateur visuel de solde sur chaque ligne.
   - **Fiche Client Détaillée** :
     - Bandeau financier en direct : **Total Commandé (DZD)** | **Total Payé (DZD)** | **Solde Débiteur Restant Dû (DZD)**.
     - Boutons d'action rapide : **Appeler**, **Envoyer Relevé WhatsApp/SMS**, et **Nouvel Encaissement Global / Sur Vente**.
     - Double vue par onglets internes : *Ventes du client* (avec reste à payer par facture) et *Journal des Règlements* (tous les versements consignés avec date, montant, mode de paiement et observation).

4. **Gestion Dynamique des Formats & Mouvements de Stock (Onglet 4 — Stock)** :
   - **Catalogue libre des formats** : Création et édition à tout moment de formats personnalisés (*Nom du format*, *Contenance ex: 150g / 1kg / 2.5kg*, *Prix unitaire forfaitaire en DZD*, *Stock initial*, *Seuil d'alerte stock bas*).
   - **Indicateurs visuels d'alerte** : Jauge de niveau de stock et badge d'alerte ambre/rouge dès que `stock_actuel <= seuil_alerte`.
   - **Actions rapides sur chaque format** :
     - **« + Lot de Production »** : Ajoute des unités issues d'une session de mise en conserve (avec note de lot/fournée et date).
     - **« Ajustement / Casse »** : Déduit ou corrige le stock suite à un bris de bocal, contrôle qualité ou inventaire physique, avec journalisation complète du motif.
   - **Journal de traçabilité des mouvements** : Historique consultable de toutes les entrées de production, sorties sur vente et pertes.

### Identité Visuelle & Thème (Visual Identity & Theme)

- **Direction Esthétique** : *Terroir Artisanal & Précision Comptable* — Une esthétique chaleureuse inspirée de la myciculture et des conserveries artisanales (tons boisés, terre d'ombre, chanterelle dorée et parchemin crème), associée à la lisibilité chirurgicale d'un outil financier professionnel.
- **Palette de Couleurs (Tokens Material 3)** :
  - `Background` / `Surface` : Crème Parchemin doux (`#FAF7F2`) en mode clair / Truffe Noire profonde (`#141210`) en mode sombre.
  - `Primary` : Brun Forêt / Cèpe profond (`#5C3A21` clair / `#E0B084` sombre) pour les actions structurantes et la navigation active.
  - `Secondary` : Jaune Chanterelle / Ambre doré (`#C87D20`) pour les mises en avant, les lots de production et la valorisation du stock.
  - `Tertiary` / `Success` : Vert Mousse Sauvage (`#2E6F40`) pour la trésorerie encaissée et le statut `PAYE`.
  - `Warning` : Ocre Cuivré (`#D97706`) pour les paiements `PARTIEL` et les seuils de stock bas.
  - `Error` : Terracotta Brique (`#B91C1C`) pour les statuts `IMPAYE`, les créances urgentes et les ruptures de stock.
- **Typographie & Hiérarchie (Polices Google Fonts locales via `font-util`)** :
  - *Titres & En-têtes (`Display` / `Headline`)* : **Fraunces** (ou **Playfair Display**) pour évoquer le caractère authentique d'une maison de conserve artisanale.
  - *Corps & Interface (`Body` / `Label`)* : **Plus Jakarta Sans** pour une lisibilité irréprochable sur mobile.
  - *Chiffres Financiers, Prix DZD & Quantités (`Monospace`)* : **JetBrains Mono** (chiffres tabulaires alignés au pixel près dans les cartes KPI, factures et soldes clients).
- **Composition & Composants** :
  - Barre de navigation inférieure Material 3 (`NavigationBar`) à 4 onglets respectant strictement les marges système (`WindowInsets.navigationBars`).
  - Cartes à élévation tonale subtile avec bordures fines (`1.dp`) et transitions fluides d'ouverture/fermeture (`animateContentSize()`).
  - Graphiques financiers dessinés sur mesure via Jetpack Compose `Canvas` (barres comparatives à coins arrondis, légendes interactives et courbes lissées sans dépendance lourde).

---

## 3. Key Product Decisions & Trade-Offs

- **Décision 1 : Transactions SQLite Atomiques (`@Transaction`) pour Ventes, Règlements et Stocks**
  - *Approche choisie* : Toute validation de vente ou d'acompte s'exécute au sein d'une transaction ACID unique côté DAO Room.
  - *Pourquoi* : Garantit qu'en cas de fermeture inopinée de l'application ou de batterie faible au milieu d'une saisie, la base ne se retrouve jamais avec une vente créée sans décrémentation du stock, ni avec un règlement enregistré sans mise à jour du statut (`IMPAYE` $\rightarrow$ `PARTIEL` $\rightarrow$ `PAYE`).
  - *Alternative écartée* : Appels séquentiels indépendants depuis l'interface, trop vulnérables aux incohérences de stock.

- **Décision 2 : Prix Unitaire Figé (`prix_unitaire_fige`) et Calcul Dynamique des Soldes**
  - *Approche choisie* : Lorsqu'un format est ajouté à une vente, son tarif courant est copié dans `lignes_vente.prix_unitaire_fige`. De même, le solde d'un client et le reste à payer d'une facture sont calculés par agrégation SQL indexée (`montant_total - COALESCE(SUM(reglements.montant), 0)`).
  - *Pourquoi* : Si l'artisan augmente le prix du « Bidon 2,5 kg » de 2 800 DA à 3 100 DA le mois suivant, toutes les factures passées et les soldes clients historiques restent strictement intacts au centime près.
  - *Alternative écartée* : Jointure directe sur le prix actuel du catalogue ou stockage de compteurs de dette redondants non synchronisés.

- **Décision 3 : Pagination SQLite (`LIMIT` / `OFFSET`) combinée aux Flux Réactifs (`Flow`)**
  - *Approche choisie* : L'historique des ventes charge les données par tranches paginées (ex. 25 entrées par page) tout en appliquant les filtres SQL (`client`, `statut`) directement au niveau du moteur SQLite via des index dédiés.
  - *Pourquoi* : Évite de charger des milliers d'objets en mémoire vive et maintient un défilement à 60/120 Hz sur tous les smartphones Android, même après plusieurs années d'activité.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### 4.1 Architecture Globale & Flux de Données (Clean MVVM)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        COUCHE PRÉSENTATION (UI Compose)                      │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐  ┌────────────┐  │
│  │   Dashboard    │  │     Ventes     │  │    Clients     │  │   Stock    │  │
│  │  • Chips Dates │  │  • Recherche   │  │  • Fiche Solde │  │  • Formats │  │
│  │  • 4 KPI Cards │  │  • Filtres     │  │  • Échéancier  │  │  • Lots    │  │
│  │  • Graphiques  │  │  • InfiniteScr │  │  • Appel / WA  │  │  • Casse   │  │
│  │  • Top Débit.  │  │  • Cartes Dépl.│  │  • Acomptes    │  │  • Alertes │  │
│  └───────┬────────┘  └───────┬────────┘  └───────┬────────┘  └─────┬──────┘  │
└──────────┼───────────────────┼───────────────────┼─────────────────┼─────────┘
           │ StateFlow         │ StateFlow         │ StateFlow       │ StateFlow
           ▼                   ▼                   ▼                 ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                     COUCHE ÉTAT & LOGIQUE MÉTIER (ViewModels)                │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐  ┌────────────┐  │
│  │ DashboardVM    │  │ VentesVM       │  │ ClientsVM      │  │ StockVM    │  │
│  │ • Périodes     │  │ • Pagination   │  │ • Calcul Solde │  │ • Seuils   │  │
│  │ • Agrégations  │  │ • Panier Vente │  │ • Export Texte │  │ • Sessions │  │
│  └───────┬────────┘  └───────┬────────┘  └───────┬────────┘  └─────┬──────┘  │
└──────────┴───────────────────┴─────────┬─────────┴─────────────────┴─────────┘
                                         │ Appels Suspend & Flows
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                        COUCHE DOMAINE & REPOSITORY                           │
│                     ConserverieRepository (Source Unique)                    │
│   • Validation métier (contrôle stock disponible, recalcul automatique       │
│     du statut IMPAYE / PARTIEL / PAYE après chaque règlement)                │
└────────────────────────────────────────┬─────────────────────────────────────┘
                                         │ Transactions ACID (@Transaction)
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                   COUCHE PERSISTANCE LOCALE (SQLite / Room)                  │
│  ┌──────────────┐ ┌──────────────────┐ ┌──────────────┐ ┌─────────────────┐  │
│  │  ClientDao   │ │ FormatProduitDao │ │   VenteDao   │ │  ReglementDao   │  │
│  └──────┬───────┘ └────────┬─────────┘ └──────┬───────┘ └────────┬────────┘  │
│         └──────────────────┴─────────┬────────┴──────────────────┘           │
│                                      ▼                                       │
│   [ Tables SQLite : clients | formats_produits | mouvements_stock |          │
│                     ventes  | lignes_vente     | reglements ]                │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

### 4.2 Schéma Relationnel SQLite Exhaustif (DDL & Index Stratégiques)

Le schéma respecte intégralement les 5 tables métier attendues (`clients`, `formats_produits`, `ventes`, `lignes_vente`, `reglements`), enrichies d'une table `mouvements_stock` pour assurer la traçabilité complète des lots de production et des ajustements de casse confirmés.

```sql
-- 1. Table des Clients
CREATE TABLE IF NOT EXISTS clients (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    nom TEXT NOT NULL,
    telephone TEXT NOT NULL DEFAULT '',
    adresse TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    date_creation INTEGER NOT NULL
);

-- 2. Table des Formats de Produits (Dynamiques)
CREATE TABLE IF NOT EXISTS formats_produits (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    nom_format TEXT NOT NULL,           -- ex: "Bocal", "Seau", "Bidon"
    contenance TEXT NOT NULL,           -- ex: "150g", "1kg", "2kg", "2.5kg"
    prix_unitaire REAL NOT NULL,        -- Prix forfaitaire en DZD (DA)
    stock_actuel INTEGER NOT NULL DEFAULT 0,
    seuil_alerte INTEGER NOT NULL DEFAULT 10,
    actif INTEGER NOT NULL DEFAULT 1
);

-- 3. Table des Ventes (En-têtes de commandes/factures)
CREATE TABLE IF NOT EXISTS ventes (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    client_id INTEGER NOT NULL,
    date_vente INTEGER NOT NULL,        -- Timestamp Epoch (ms) pour tri et filtrage rapide
    montant_total REAL NOT NULL,        -- Total en DZD
    statut TEXT NOT NULL,               -- 'IMPAYE', 'PARTIEL', 'PAYE'
    notes TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT
);

-- 4. Table des Lignes de Vente (Détail des formats vendus avec prix figé)
CREATE TABLE IF NOT EXISTS lignes_vente (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    vente_id INTEGER NOT NULL,
    format_id INTEGER NOT NULL,
    quantite INTEGER NOT NULL,
    prix_unitaire_fige REAL NOT NULL,   -- Tarif du format au moment exact de la vente
    sous_total REAL NOT NULL,           -- quantite * prix_unitaire_fige
    FOREIGN KEY (vente_id) REFERENCES ventes(id) ON DELETE CASCADE,
    FOREIGN KEY (format_id) REFERENCES formats_produits(id) ON DELETE RESTRICT
);

-- 5. Table des Règlements (Acomptes et paiements échelonnés)
CREATE TABLE IF NOT EXISTS reglements (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    vente_id INTEGER NOT NULL,
    client_id INTEGER NOT NULL,
    date_reglement INTEGER NOT NULL,    -- Timestamp Epoch (ms)
    montant REAL NOT NULL,              -- Montant versé en DZD
    mode_paiement TEXT NOT NULL,        -- 'ESPECES', 'CHEQUE', 'VIREMENT', 'AUTRE'
    observation TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (vente_id) REFERENCES ventes(id) ON DELETE CASCADE,
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT
);

-- 6. Table des Mouvements de Stock (Lots de production, ventes, pertes/casse)
CREATE TABLE IF NOT EXISTS mouvements_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    format_id INTEGER NOT NULL,
    type_mouvement TEXT NOT NULL,       -- 'PRODUCTION', 'VENTE', 'PERTE_CASSE', 'AJUSTEMENT'
    quantite_delta INTEGER NOT NULL,    -- Positif (+) pour entrée, Négatif (-) pour sortie
    stock_apres INTEGER NOT NULL,
    date_mouvement INTEGER NOT NULL,
    motif TEXT NOT NULL DEFAULT '',     -- ex: "Lot Stérilisation #14", "Vente #108", "Bocal brisé"
    vente_id INTEGER,
    FOREIGN KEY (format_id) REFERENCES formats_produits(id) ON DELETE CASCADE
);
```

#### Index Stratégiques pour la Performance (Milliers d'Opérations)

```sql
-- Accélère le tri antéchronologique et le filtrage par période du Dashboard et de l'historique
CREATE INDEX IF NOT EXISTS idx_ventes_date_desc ON ventes(date_vente DESC);

-- Accélère le filtrage combiné par statut (Tous, Soldés, Partiels, Impayés) et date
CREATE INDEX IF NOT EXISTS idx_ventes_statut_date ON ventes(statut, date_vente DESC);

-- Accélère le chargement de la fiche client et le calcul des soldes
CREATE INDEX IF NOT EXISTS idx_ventes_client ON ventes(client_id, date_vente DESC);
CREATE INDEX IF NOT EXISTS idx_reglements_vente ON reglements(vente_id);
CREATE INDEX IF NOT EXISTS idx_reglements_client_date ON reglements(client_id, date_reglement DESC);
CREATE INDEX IF NOT EXISTS idx_reglements_date ON reglements(date_reglement DESC);

-- Accélère le dépliage des cartes de vente et l'analyse des ventes par format
CREATE INDEX IF NOT EXISTS idx_lignes_vente_vente ON lignes_vente(vente_id);
CREATE INDEX IF NOT EXISTS idx_lignes_vente_format ON lignes_vente(format_id);

-- Accélère la recherche de client par nom
CREATE INDEX IF NOT EXISTS idx_clients_nom ON clients(nom COLLATE NOCASE);
```

---

### 4.3 Requêtes SQL d'Agrégation Temporelle, Pagination & Analyses

#### A. KPIs Réactifs du Dashboard sur la Période `[startMillis, endMillis]`

```sql
-- 1. Chiffre d'Affaires Total Facturé sur la période
SELECT COALESCE(SUM(montant_total), 0.0) AS ca_facture
FROM ventes
WHERE date_vente BETWEEN :startMillis AND :endMillis;

-- 2. Trésorerie Réelle Encaissée sur la période (tous règlements perçus sur la plage)
SELECT COALESCE(SUM(montant), 0.0) AS tresorerie_encaissee
FROM reglements
WHERE date_reglement BETWEEN :startMillis AND :endMillis;

-- 3. Créances Clients Générées sur la période (Reste à recouvrer sur les ventes de la période)
SELECT COALESCE(SUM(v.montant_total - COALESCE(r_sum.total_paye, 0.0)), 0.0) AS creances_periode
FROM ventes v
LEFT JOIN (
    SELECT vente_id, SUM(montant) AS total_paye
    FROM reglements
    GROUP BY vente_id
) r_sum ON r_sum.vente_id = v.id
WHERE v.date_vente BETWEEN :startMillis AND :endMillis
  AND (v.montant_total - COALESCE(r_sum.total_paye, 0.0)) > 0.001;

-- 4. Valeur Marchande Totale du Stock Actuel de Conserves
SELECT COALESCE(SUM(stock_actuel * prix_unitaire), 0.0) AS valeur_stock_total,
       COALESCE(SUM(stock_actuel), 0) AS total_unites_stock
FROM formats_produits
WHERE actif = 1;
```

#### B. Graphique Comparatif : Ventes Facturées vs Encaissements Réels (Regroupement par Jour ou par Mois)

```sql
-- Agrégation journalière (ou mensuelle via '%Y-%m') des ventes facturées
SELECT strftime('%Y-%m-%d', date_vente / 1000, 'unixepoch', 'localtime') AS periode_label,
       COALESCE(SUM(montant_total), 0.0) AS total_facture
FROM ventes
WHERE date_vente BETWEEN :startMillis AND :endMillis
GROUP BY periode_label
ORDER BY periode_label ASC;

-- Agrégation journalière (ou mensuelle via '%Y-%m') des encaissements réels
SELECT strftime('%Y-%m-%d', date_reglement / 1000, 'unixepoch', 'localtime') AS periode_label,
       COALESCE(SUM(montant), 0.0) AS total_encaisse
FROM reglements
WHERE date_reglement BETWEEN :startMillis AND :endMillis
GROUP BY periode_label
ORDER BY periode_label ASC;
```

#### C. Répartition des Ventes par Format de Conserve (Rentabilité & Vélocité)

```sql
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
ORDER BY chiffre_affaires_format DESC;
```

#### D. Bloc Action Rapide « Top Clients Débiteurs »

```sql
SELECT c.id AS client_id,
       c.nom,
       c.telephone,
       COALESCE(v_tot.total_commande, 0.0) AS total_commande,
       COALESCE(r_tot.total_paye, 0.0) AS total_paye,
       (COALESCE(v_tot.total_commande, 0.0) - COALESCE(r_tot.total_paye, 0.0)) AS solde_debiteur,
       v_tot.plus_ancienne_vente_impayee
FROM clients c
INNER JOIN (
    SELECT client_id,
           SUM(montant_total) AS total_commande,
           MIN(CASE WHEN statut != 'PAYE' THEN date_vente ELSE NULL END) AS plus_ancienne_vente_impayee
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
LIMIT 10;
```

#### E. Historique des Ventes Paginé (`LIMIT` & `OFFSET`) avec Recherche & Filtre Statut

```sql
SELECT v.id,
       v.client_id,
       c.nom AS client_nom,
       c.telephone AS client_telephone,
       v.date_vente,
       v.montant_total,
       v.statut,
       COALESCE(SUM(r.montant), 0.0) AS montant_paye,
       (v.montant_total - COALESCE(SUM(r.montant), 0.0)) AS reste_a_payer
FROM ventes v
INNER JOIN clients c ON c.id = v.client_id
LEFT JOIN reglements r ON r.vente_id = v.id
WHERE (:statutFiltre = 'TOUS' OR v.statut = :statutFiltre)
  AND (:rechercheClient = '' OR c.nom LIKE '%' || :rechercheClient || '%')
GROUP BY v.id
ORDER BY v.date_vente DESC
LIMIT :limit OFFSET :offset;
```

---

### 4.4 Mapping des Interactions & Transactions Métier

1. **Validation Atomique d'une Vente (`creerVenteTransactionnelle`)** :
   - Vérifie que chaque format sélectionné possède un `stock_actuel >= quantite` (ou affiche un avertissement explicite).
   - Calcule `montant_total = SUM(quantite * prix_unitaire_fige)`.
   - Détermine automatiquement le statut initial :
     - `PAYE` si `acompte_initial >= montant_total`
     - `PARTIEL` si `0 < acompte_initial < montant_total`
     - `IMPAYE` si `acompte_initial == 0`
   - Insère la ligne dans `ventes`, insère toutes les `lignes_vente`, insère le `reglement` initial si `acompte_initial > 0`, décrémente `formats_produits.stock_actuel` pour chaque format et écrit une trace dans `mouvements_stock` (`type_mouvement = 'VENTE'`).

2. **Enregistrement d'un Nouvel Acompte (`ajouterReglementTransactionnel`)** :
   - Accessible en un clic depuis une **carte de vente dépliée** ou depuis la **fiche client**.
   - Vérifie que le montant saisi est supérieur à `0` et ne dépasse pas le `reste_a_payer` de la vente ciblée (ou ventile automatiquement l'encaissement sur les factures les plus anciennes du client lors d'un versement global).
   - Insère l'enregistrement dans `reglements` avec `date_reglement`, `montant`, `mode_paiement` (*Espèces*, *Chèque*, *Virement*) et `observation`.
   - Recalcule la somme totale payée sur la vente : met à jour `ventes.statut` en `'PAYE'` si le reste dû tombe à `0 DA`, sinon `'PARTIEL'`.

3. **Regroupement Temporel & Défilement Infini (Infinite Scroll)** :
   - Le `VentesViewModel` maintient l'index de page courant (`pageSize = 25`). Lorsque l'utilisateur approche de la fin de la `LazyColumn`, la page suivante est chargée de manière asynchrone via `LIMIT 25 OFFSET (page * 25)` sans bloquer l'interface.
   - Les ventes chargées sont regroupées dynamiquement sous 4 sections temporelles : **Aujourd'hui**, **Cette semaine**, **Ce mois**, et **Plus ancien**.

---

### 4.5 Feuille de Route Chronologique d'Implémentation (Roadmap par Jalons)

- **Jalon 1 — Fondations SQLite / Room, Indexation & Jeu de Données Artisanal Initial** :
  - Mise en place des entités relationnelles (`clients`, `formats_produits`, `ventes`, `lignes_vente`, `reglements`, `mouvements_stock`), des index SQL et des DAOs avec transactions ACID.
  - Pré-chargement automatique au premier lancement (si la base est vide) de formats réalistes (*Bocal 150g*, *Bocal 350g*, *Seau 1kg*, *Bidon 2kg*, *Bidon 2.5kg*), de clients restaurateurs/grossistes, de lots de production et de ventes avec règlements échelonnés en DZD pour permettre un test immédiat du Dashboard et des filtres.

- **Jalon 2 — Identité Visuelle « Terroir Artisanal », Typographie Locale & Navigation Principale** :
  - Installation locale des polices Google Fonts (`Fraunces`, `Plus Jakarta Sans`, `JetBrains Mono`) et configuration du thème Material 3.
  - Construction du `Scaffold` adaptatif avec barre d'onglets (`Dashboard`, `Ventes`, `Clients`, `Stock`) et formatage monétaire DZD.

- **Jalon 3 — Module Stock Dynamique, Lots de Production & Casse** :
  - Écran de création/modification libre des formats de conserves (prix forfaitaire en DZD, contenance, seuil d'alerte).
  - Modales rapides d'entrée de **Lot de Production** et d'**Ajustement / Casse** avec journal complet des mouvements de stock.

- **Jalon 4 — Module Ventes Paginé, Cartes Dépliables & Fiches Clients avec Règlements Échelonnés** :
  - Écran d'historique des ventes avec recherche client, filtres par statut, regroupement temporel (*Aujourd'hui*, *Cette semaine*, *Ce mois*, *Plus ancien*), pagination `LIMIT / OFFSET` et cartes dépliables permettant de saisir un acompte en direct.
  - Assistant de création de nouvelle vente avec décrémentation immédiate du stock et encaissement initial optionnel.
  - Fiche client détaillée avec balance financière temps réel, appel téléphonique direct et partage de relevé via WhatsApp/SMS.

- **Jalon 5 — Dashboard Financier Réactif, Graphiques Canvas & Vérification Qualité** :
  - Sélecteur de période rapide par `FilterChips` + sélecteur de plage personnalisée (`DateRangePicker`).
  - Cartes KPI réactives, graphique comparatif *Ventes facturées vs Encaissements réels*, répartition par format de conserve et bloc *Top Clients Débiteurs*.
  - Compilation et validation complète de l'application (`compile_applet`).
