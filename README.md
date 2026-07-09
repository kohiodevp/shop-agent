# Tailor Agent — Application Android de production

Application Android pour un **tailleur professionnel** : gestion des clients,
fiches de mesures, suivi des commandes (En attente → Coupe → Couture →
Essayage → Livré), calcul du métrage de tissu par type de vêtement, gradation
des tailles, et export PDF/JSON.

## Caractéristiques

- **Langage** : Kotlin (JVM 17), moteur métier natif (`TailorEngine.kt`)
- **UI** : Jetpack Compose 1.4.3 + Material3 1.1.0
- **Stockage** : SQLite (`TailorDb`, SQLiteOpenHelper)
- **Build** : Gradle 8.0.2 / AGP 8.0.2 / Kotlin 1.8.10
- **Cible** : `compileSdk 34`, `minSdk 24` (Android 7.0+), `targetSdk 34`
- **Build reproductible** : entièrement conteneurisé via Docker
  (`eclipse-temurin:17-jdk` + Android SDK 34)

## Construction de l'APK (en local, via Docker)

Prérequis : Docker installé.

```bash
./build_apk.sh
# -> app-release.apk dans output/
```

Le script construit l'image (install SDK + Gradle), compile `assembleRelease`,
puis extrait l'APK signé (`release.keystore`, alias `tailor`) vers
`output/app-release.apk`.

## Construction via GitHub Actions

Chaque push sur `main` (ou `workflow_dispatch`) déclenche le workflow
`.github/workflows/build-apk.yml` qui rebuild l'APK dans Docker et publie
l'artefact `tailor-agent-release-apk` (téléchargeable depuis l'onglet Actions).

## Installation

```bash
adb install output/app-release.apk
```

## Note sur le moteur Python (Chaquopy)

Le moteur métier était initialement conçu en Python (`app/src/main/python/
tailor_logic.py`, conservé comme référence). Chaquopy 12.0.0 (seule version
runtime publiée sur le Maven officiel) casse l'enregistrement de l'extension
`android{}` avec les Gradle/AGP/JDK modernes. Le moteur a donc été réécrit en
Kotlin pur (`TailorEngine.kt`) — logique strictement équivalente, sans
dépendance Python runtime.

## Sécurité

- `release.keystore` est **exclu du dépôt** (`.gitignore`).
- L'APK signé n'est pas versionné ; il est produit par le build.
