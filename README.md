# Shop Agent

Application Android (Kotlin pur, build Docker reproductible) pour la **gestion et la vente d'une boutique ou d'un magasin**.

## Fonctionnalités
- **Catalogue produits** : ajout, prix, gestion du stock (Room).
- **Panier / Caisse** : ajout d'articles, calcul du total, validation de la vente (décrémente le stock).
- **Historique des ventes** : liste des transactions (montant, date, articles).
- **Logique Python** (Chaquopy) : generation de tickets, detection de rupture de stock.

## Architecture
- Kotlin + Jetpack Compose (UI)
- Room (persistance produits/ventes)
- Chaquopy (bridge Python pour la logique metier)
- Build reproductible via Docker (Android SDK 34, Gradle 8.0.2)

## Build
L'APK signe est genere par le workflow GitHub Actions (Docker) et publie l'artifact `shop-agent-release-apk`.

## Difference avec tailor-agent
Shop Agent est dedie a la **vente au detail** (catalogue, stock, caisse, tickets).
Tailor Agent est dedie a la **couture** (modeles, plans de coupe, prix par element).

---
Cree par Betsa Agent (inspire de Betsaleel, Exode 31:3).
