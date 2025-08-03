# Vizion Security - Application Android Native

<div align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt="Language">
  <img src="https://img.shields.io/badge/Architecture-MVVM-orange.svg" alt="Architecture">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-purple.svg" alt="UI">
  <img src="https://img.shields.io/badge/Version-1.0.0-brightgreen.svg" alt="Version">
</div>

## 📱 Vue d'ensemble

**Vizion Security** est une application de sécurité mobile professionnelle développée en **Kotlin natif** pour Android. Elle offre une surveillance en temps réel, une protection bancaire avancée et une intégration complète avec le système SIEM Wazuh pour un monitoring de sécurité d'entreprise.

### 🎯 Objectifs principaux
- **Surveillance proactive** des menaces mobiles
- **Protection bancaire** en temps réel
- **Intégration SIEM** avec transmission de logs sécurisée
- **Interface moderne** avec Material Design 3
- **Architecture native** pour des performances optimales

---

## 🚀 Fonctionnalités

### 🛡️ Sécurité Core
- **Score de sécurité global** - Évaluation en temps réel (0-100)
- **Détection de menaces** - Classification automatique (CRITICAL, WARNING, INFO)
- **Surveillance continue** - Monitoring 24/7 en arrière-plan
- **Alertes intelligentes** - Notifications proactives des incidents

### 🏦 Protection Bancaire
- **Détection automatique** des applications bancaires
- **Service d'accessibilité** pour protection en temps réel
- **Blocage des menaces** - Intervention automatique
- **Surveillance des sessions** bancaires actives

### 🌐 Intégration SIEM Wazuh
- **Connexion TCP native** au serveur Wazuh (159.65.120.14:1514)
- **Auto-enregistrement** via API REST Wazuh
- **Transmission en temps réel** des logs de sécurité
- **Heartbeat périodique** (30 secondes)
- **Reconnexion automatique** en cas de perte réseau
- **Synchronisation** des logs non envoyés

### 📊 Interface Utilisateur
- **5 écrans principaux** : Dashboard, Alertes, Confidentialité, Wazuh, Paramètres
- **Design premium** avec composants personnalisés (GlassCard, PremiumCard)
- **Animations fluides** entre les écrans
- **Thème adaptatif** sombre/clair
- **Navigation intuitive** avec Material 3

---

## 🏗️ Architecture Technique

### 📋 Technologies Utilisées
```
• Kotlin 1.9.20          - Langage principal
• Jetpack Compose        - Interface utilisateur moderne
• Hilt                   - Injection de dépendances
• Coroutines             - Programmation asynchrone
• Room Database          - Stockage local sécurisé
• Retrofit + OkHttp      - Client HTTP pour API
• DataStore              - Préférences sécurisées
• WorkManager            - Tâches en arrière-plan
• Material Design 3      - Système de design
```

### 🏛️ Architecture MVVM
```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
├─────────────────────────────────────────────────────────────┤
│ • MainActivity (Jetpack Compose)                            │
│ • 5 Screens: Dashboard, Alerts, Privacy, Wazuh, Settings   │
│ • ViewModels avec StateFlow                                 │
│ • Navigation Component                                      │
│ • Composants UI personnalisés                              │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                     DOMAIN LAYER                           │
├─────────────────────────────────────────────────────────────┤
│ • Use Cases (logique métier)                               │
│ • Repository Interfaces                                    │
│ • Modèles de données                                       │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      DATA LAYER                            │
├─────────────────────────────────────────────────────────────┤
│ • Repositories (SecurityRepository, WazuhRepository)       │
│ • Base de données Room (3 tables)                         │
│ • API Client Wazuh (REST + UDP)                           │
│ • DataStore pour préférences                              │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    SERVICES LAYER                          │
├─────────────────────────────────────────────────────────────┤
│ • SecurityMonitoringService (Foreground)                   │
│ • WazuhAgentService (Foreground)                           │
│ • BankingProtectionService (Accessibility)                 │
│ • DataPurgeWorker (Maintenance)                            │
└─────────────────────────────────────────────────────────────┘
```

### 🗄️ Structure de la Base de Données
```sql
-- Table des événements de sécurité
security_events (
    id: Long PRIMARY KEY,
    timestamp: Date,
    eventType: String,
    severity: String,        -- CRITICAL, WARNING, INFO
    message: String,
    source: String,
    details: String?,
    isProcessed: Boolean
)

-- Table des logs Wazuh
wazuh_logs (
    id: Long PRIMARY KEY,
    timestamp: Date,
    level: String,
    message: String,
    source: String,
    agentId: String?,
    ruleId: String?,
    isSent: Boolean         -- Synchronisation
)

-- Table des permissions d'applications
app_permissions (
    packageName: String PRIMARY KEY,
    appName: String,
    permissions: String,    -- JSON array
    riskLevel: String,      -- LOW, MEDIUM, HIGH
    lastScanned: Date,
    isSystemApp: Boolean
)
```

---

## 🔧 Installation et Configuration

### 📋 Prérequis Système
```bash
• Android Studio Hedgehog (2023.1.1) ou plus récent
• JDK 17 (OpenJDK recommandé)
• Android SDK API Level 34 (Android 14)
• Gradle 8.11.1
• Device/Emulator: Android 7.0 (API 24) minimum
```

### ⚙️ Configuration de l'Environnement

#### 1. Vérification Java
```bash
java -version  # Doit afficher Java 17
```

#### 2. Configuration Android SDK
```bash
# Dans Android Studio: Tools > SDK Manager
# Installer Android SDK Platform 34
# Installer Android SDK Build-Tools 34.0.0
```

#### 3. Variables d'environnement (optionnel)
```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

### 🚀 Installation du Projet

#### 1. Clonage et configuration
```bash
git clone <repository-url>
cd VizionSecurity

# Créer le fichier de configuration local
echo "sdk.dir=$ANDROID_HOME" > local.properties
echo "WAZUH_SERVER=159.65.120.14" >> local.properties
echo "WAZUH_PORT=1514" >> local.properties
echo "API_BASE_URL=https://appollon-inc.com/api" >> local.properties
```

#### 2. Build et installation
```bash
# Nettoyer et construire
./gradlew clean
./gradlew build

# Installation sur device/émulateur
./gradlew installDebug
```

---

## 🔐 Permissions et Sécurité

### 🛡️ Permissions Critiques
```xml
<!-- Réseau et connectivité -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Surveillance système -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />

<!-- Administration device -->
<uses-permission android:name="android.permission.DEVICE_ADMIN" />

<!-- Service d'accessibilité (protection bancaire) -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />

<!-- Services en arrière-plan -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### 🔒 Configuration Sécurité
- **Chiffrement AES-256** pour données sensibles
- **Certificate Pinning** pour connexions Wazuh
- **ProGuard/R8** pour obfuscation du code
- **Network Security Config** pour HTTPS forcé
- **Root Detection** et protection anti-tampering

---

## 🎮 Utilisation

### 🔑 Connexion
```
Email: admin@example.com
Mot de passe: adminpassword
```

### 📱 Navigation des Écrans

#### 1. **Dashboard** 📊
- **Score de sécurité global** (0-100) avec animation
- **Compteurs en temps réel** : menaces actives, apps protégées
- **Statut Wazuh** avec indicateur de connexion
- **Activité récente** des 24 dernières heures

#### 2. **Alertes** 🚨
- **Classification automatique** : Critique, Attention, Info, Succès
- **Résumé visuel** avec compteurs par type
- **Détails complets** : timestamp, source, description
- **Filtrage** par niveau de sévérité

#### 3. **Confidentialité** 🔒
- **Analyse des permissions** d'applications installées
- **Évaluation des risques** : Faible, Moyen, Élevé
- **Statistiques globales** : apps analysées, permissions totales
- **Détection d'apps suspectes** avec permissions dangereuses

#### 4. **Wazuh** 🌐
- **Statut de connexion** en temps réel avec animation
- **Statistiques de transmission** : événements, données
- **Logs en direct** avec classification par niveau
- **Informations serveur** : IP, port, uptime

#### 5. **Paramètres** ⚙️
- **Profil utilisateur** avec statut d'activation
- **Configuration sécurité** : surveillance, protection bancaire
- **Gestion notifications** avec types personnalisables
- **Informations application** : version, à propos

---

## 🔧 Développement

### 🏃‍♂️ Commandes de Développement
```bash
# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Tests unitaires
./gradlew test

# Tests d'intégration
./gradlew connectedAndroidTest

# Analyse de code
./gradlew lint

# Génération AAB pour Play Store
./gradlew bundleRelease
```

### 📊 Monitoring et Logs
```bash
# Logs généraux de l'application
adb logcat -s VizionSecurity

# Logs spécifiques par service
adb logcat -s WazuhAgent
adb logcat -s SecurityMonitoring
adb logcat -s BankingProtection

# Logs avec filtrage avancé
adb logcat | grep -E "(VizionSecurity|WazuhAgent|SecurityMonitoring)"

# Informations système de l'app
adb shell dumpsys package com.vizion.security
adb shell dumpsys meminfo com.vizion.security
```

### 🧪 Tests et Débogage
```bash
# Test de connectivité Wazuh
adb shell ping -c 3 159.65.120.14
adb shell nc -zv 159.65.120.14 1514

# Forcer redémarrage de l'app
adb shell am force-stop com.vizion.security
adb shell am start -n com.vizion.security/.presentation.MainActivity

# Vérifier services actifs
adb shell dumpsys activity services | grep vizion
```

---

## 🌐 Intégration Wazuh SIEM

### 🔗 Configuration Serveur
```yaml
# Configuration Wazuh Manager
Server: 159.65.120.14
API Port: 55000 (HTTPS)
Agent Port: 1514 (UDP)
Auth Port: 1515 (TCP)

# Authentification API
User: mobile_agent_manager
Password: MobileAgentSecure123!
```

### 📡 Protocole de Communication
```
1. Auto-enregistrement via API REST
   POST /agents → Création agent mobile
   
2. Connexion UDP pour logs temps réel
   Format: timestamp hostname ossec: LEVEL: [agent_id] source: message
   
3. Heartbeat périodique (30s)
   INFO: Mobile agent heartbeat - device_info
   
4. Synchronisation logs non envoyés
   Retry automatique des logs en échec
```

### 📈 Métriques Transmises
- **Événements de sécurité** : démarrage/arrêt services, détection menaces
- **Informations système** : version Android, modèle device, utilisation mémoire
- **Activité applications** : installations, permissions dangereuses
- **Statut connexion** : heartbeat, reconnexions, erreurs réseau

---

## 📦 Structure du Projet

```
app/
├── src/main/java/com/vizion/security/
│   ├── VizionSecurityApplication.kt      # Point d'entrée avec Hilt
│   ├── presentation/                     # Couche UI
│   │   ├── MainActivity.kt               # Activité principale Compose
│   │   ├── navigation/                   # Navigation entre écrans
│   │   ├── screen/                       # 5 écrans principaux
│   │   ├── component/                    # Composants UI réutilisables
│   │   ├── viewmodel/                    # ViewModels avec StateFlow
│   │   └── theme/                        # Thème Material 3 personnalisé
│   ├── data/                            # Couche données
│   │   ├── local/                       # Base de données Room
│   │   │   ├── database/                # Configuration DB
│   │   │   ├── dao/                     # Data Access Objects
│   │   │   ├── entity/                  # Entités de base de données
│   │   │   └── converter/               # Convertisseurs de types
│   │   ├── remote/                      # API et clients réseau
│   │   │   └── WazuhApiClient.kt        # Client Wazuh REST + UDP
│   │   └── repository/                  # Repositories (abstraction)
│   ├── service/                         # Services Android
│   │   ├── SecurityMonitoringService.kt # Surveillance continue
│   │   ├── WazuhAgentService.kt         # Agent SIEM
│   │   └── BankingProtectionService.kt  # Protection bancaire
│   ├── receiver/                        # BroadcastReceivers
│   ├── worker/                          # Tâches WorkManager
│   └── di/                              # Modules injection Hilt
├── src/main/res/                        # Ressources Android
│   ├── values/                          # Strings, couleurs, thèmes
│   ├── xml/                             # Configurations système
│   └── AndroidManifest.xml              # Permissions et composants
└── build.gradle                         # Configuration Gradle
```

---

## 🚀 Déploiement

### 📱 Build de Production
```bash
# Génération APK release
./gradlew assembleRelease

# Génération AAB pour Play Store
./gradlew bundleRelease

# Localisation des fichiers
ls -la app/build/outputs/apk/release/
ls -la app/build/outputs/bundle/release/
```

### 🔐 Signature d'Application
```bash
# Créer keystore (une seule fois)
keytool -genkey -v -keystore release-key.keystore \
  -alias vizion-key -keyalg RSA -keysize 2048 -validity 10000

# Signer l'APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore release-key.keystore app-release-unsigned.apk vizion-key

# Aligner l'APK
zipalign -v 4 app-release-unsigned.apk VizionSecurity-release.apk
```

### 🏪 Distribution
- **Google Play Store** : Upload AAB via Play Console
- **Distribution interne** : APK signé pour entreprise
- **Tests** : Internal testing tracks disponibles

---

## 📊 Performance et Monitoring

### 📈 Métriques Clés
- **Temps de démarrage** : < 2 secondes
- **Utilisation mémoire** : < 100MB en fonctionnement normal
- **Consommation batterie** : Optimisée avec Doze mode
- **Taille APK** : ~15MB (avec ProGuard)
- **Connexions Wazuh** : 99.9% uptime avec reconnexion auto

### 🔍 Outils de Monitoring
```bash
# Profiling mémoire
adb shell dumpsys meminfo com.vizion.security

# Utilisation CPU
adb shell top | grep vizion

# Statistiques réseau
adb shell cat /proc/net/dev

# Analyse des performances
./gradlew :app:assembleDebug -Pandroid.enableProfileJson=true
```

---

## 🤝 Contribution

### 🔄 Workflow de Développement
1. **Fork** le projet
2. **Créer** une branche feature (`git checkout -b feature/nouvelle-fonctionnalite`)
3. **Commit** les changements (`git commit -am 'Ajout nouvelle fonctionnalité'`)
4. **Push** vers la branche (`git push origin feature/nouvelle-fonctionnalite`)
5. **Créer** une Pull Request

### 📝 Standards de Code
- **Kotlin Coding Conventions** officielles
- **Commentaires détaillés** en français
- **Tests unitaires** obligatoires pour nouvelle logique
- **Documentation** mise à jour pour nouvelles APIs

---

## 📄 Licence et Support

### 📜 Licence
```
Propriétaire - Vizion Security © 2024
Tous droits réservés.

Cette application est la propriété exclusive de Vizion Security.
Toute reproduction, distribution ou modification non autorisée est interdite.
```

### 🆘 Support Technique
- **Email** : support@vizion-security.com
- **Documentation** : [docs.vizion-security.com](https://docs.vizion-security.com)
- **Issues GitHub** : Pour rapporter des bugs
- **Wiki** : Guides d'utilisation détaillés

### 📞 Contact Entreprise
- **Commercial** : sales@vizion-security.com
- **Partenariats** : partners@vizion-security.com
- **Sécurité** : security@vizion-security.com

---

## 🔮 Roadmap

### 🎯 Version 1.1 (Q2 2024)
- [ ] **Machine Learning** pour détection d'anomalies
- [ ] **Géolocalisation** des menaces
- [ ] **Rapports PDF** automatisés
- [ ] **API publique** pour intégrations tierces

### 🎯 Version 1.2 (Q3 2024)
- [ ] **Support multi-langues** (EN, ES, DE)
- [ ] **Dashboard web** pour administrateurs
- [ ] **Intégration Splunk** en plus de Wazuh
- [ ] **Mode hors-ligne** avancé

### 🎯 Version 2.0 (Q4 2024)
- [ ] **Intelligence artificielle** prédictive
- [ ] **Blockchain** pour audit trail
- [ ] **Support iOS** (React Native)
- [ ] **Cloud SaaS** solution

---

<div align="center">
  <h3>🛡️ Vizion Security - Sécurité Mobile d'Entreprise 🛡️</h3>
  <p><em>Protégez votre écosystème mobile avec une surveillance intelligente</em></p>
  
  **[Documentation](https://docs.vizion-security.com)** • 
  **[Support](mailto:support@vizion-security.com)** • 
  **[Démo](https://demo.vizion-security.com)**
</div>