package com.vizion.security.presentation.model

// Chemin: app/src/main/java/com/vizion/security/presentation/model/SecurityUIModels.kt

import java.text.SimpleDateFormat
import java.util.*

/**
 * Modèles de données pour l'interface utilisateur de sécurité
 *
 * Ces classes définissent les structures de données utilisées
 * par les composables Compose pour afficher les informations de sécurité.
 *
 * Architecture :
 * - Séparation des modèles UI et des entités de base de données
 * - Classes de données immutables pour la sécurité des threads
 * - Extensions pour la conversion depuis les entités
 * - Formatage et présentation des données optimisés pour l'UI
 */

/**
 * Représentation UI d'un événement de sécurité
 *
 * Cette classe adapte les données de SecurityEventEntity
 * pour l'affichage dans l'interface utilisateur.
 */
data class SecurityEventUI(
    val id: String,
    val eventType: String,
    val severity: String,
    val message: String,
    val source: String,
    val timestamp: String,
    val riskScore: Int,
    val isResolved: Boolean,
    val recommendedAction: String?,
    val category: String? = null,
    val confidenceScore: Float = 0.0f
) {
    /**
     * Couleur associée au niveau de sévérité
     */
    val severityColor: Long
        get() = when (severity) {
            "CRITICAL" -> 0xFFF44336
            "ERROR" -> 0xFFFF9800
            "WARNING" -> 0xFFFFC107
            else -> 0xFF4CAF50
        }

    /**
     * Icône représentant le type d'événement
     */
    val eventIcon: String
        get() = when {
            eventType.contains("MALWARE", ignoreCase = true) -> "🦠"
            eventType.contains("ROOT", ignoreCase = true) -> "🔓"
            eventType.contains("INTRUSION", ignoreCase = true) -> "🚨"
            eventType.contains("NETWORK", ignoreCase = true) -> "🌐"
            eventType.contains("PERMISSION", ignoreCase = true) -> "🔒"
            eventType.contains("SYSTEM", ignoreCase = true) -> "⚙️"
            eventType.contains("SCAN", ignoreCase = true) -> "🔍"
            else -> "📋"
        }

    /**
     * Description courte pour affichage rapide
     */
    val shortDescription: String
        get() = if (message.length > 100) {
            "${message.take(97)}..."
        } else {
            message
        }
}

/**
 * État global de l'interface utilisateur de sécurité
 */
data class SecurityUiState(
    val isLoading: Boolean = false,
    val isServiceRunning: Boolean = false,
    val isScanning: Boolean = false,
    val isExporting: Boolean = false,
    val isSyncing: Boolean = false,

    // Données de sécurité
    val threatLevel: String = "NORMAL",
    val activeThreats: Int = 0,
    val totalEvents: Int = 0,
    val criticalEvents: Int = 0,
    val warningEvents: Int = 0,
    val infoEvents: Int = 0,
    val averageRiskScore: Double = 0.0,

    // Événements
    val recentEvents: List<SecurityEventUI> = emptyList(),
    val selectedEvent: SecurityEventUI? = null,

    // Synchronisation
    val syncedEvents: Int = 0,
    val unsyncedEvents: Int = 0,
    val isWazuhConnected: Boolean = false,
    val wazuhConnectionInfo: String = "",

    // Timestamps
    val lastCheckTime: String = "",
    val lastScanTime: Long = 0,
    val lastUpdated: Long = 0,

    // Messages
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    /**
     * Niveau de risque global calculé
     */
    val riskLevel: String
        get() = when {
            threatLevel == "CRITICAL" || criticalEvents > 5 -> "ÉLEVÉ"
            threatLevel == "WARNING" || criticalEvents > 0 -> "MODÉRÉ"
            warningEvents > 10 -> "FAIBLE"
            else -> "MINIMAL"
        }

    /**
     * Pourcentage d'événements synchronisés
     */
    val syncPercentage: Float
        get() = if (totalEvents > 0) {
            (syncedEvents * 100f) / totalEvents
        } else {
            100f
        }

    /**
     * Status de connexion Wazuh formaté
     */
    val wazuhStatus: String
        get() = if (isWazuhConnected) "Connecté" else "Déconnecté"
}

/**
 * État des permissions de l'application
 */
data class PermissionState(
    val grantedPermissions: List<String> = emptyList(),
    val deniedPermissions: List<String> = emptyList(),
    val lastPermissionCheck: Long = 0
) {
    val hasAllRequiredPermissions: Boolean
        get() = deniedPermissions.isEmpty()

    val permissionSummary: String
        get() = "${grantedPermissions.size} accordées, ${deniedPermissions.size} refusées"

    /**
     * Pourcentage de permissions accordées
     */
    val permissionPercentage: Float
        get() {
            val total = grantedPermissions.size + deniedPermissions.size
            return if (total > 0) {
                (grantedPermissions.size * 100f) / total
            } else {
                100f
            }
        }
}

/**
 * Statistiques de sécurité pour affichage dashboard
 */
data class SecurityStats(
    val totalEvents: Int,
    val criticalEvents: Int,
    val warningEvents: Int,
    val infoEvents: Int,
    val averageRiskScore: Double,
    val syncedEvents: Int,
    val unsyncedEvents: Int
) {
    val criticalPercentage: Float
        get() = if (totalEvents > 0) (criticalEvents * 100f) / totalEvents else 0f

    val syncPercentage: Float
        get() = if (totalEvents > 0) (syncedEvents * 100f) / totalEvents else 100f

    val riskLevelText: String
        get() = when {
            criticalEvents > 10 -> "CRITIQUE"
            criticalEvents > 5 -> "ÉLEVÉ"
            criticalEvents > 0 -> "MODÉRÉ"
            warningEvents > 20 -> "FAIBLE"
            else -> "MINIMAL"
        }
}

/**
 * Modèle pour l'affichage des métriques de performance
 */
data class PerformanceMetrics(
    val cpuUsage: Float = 0f,
    val memoryUsage: Float = 0f,
    val batteryLevel: Int = 100,
    val networkLatency: Long = 0,
    val databaseSize: Long = 0,
    val lastUpdateTime: Long = System.currentTimeMillis()
) {
    val cpuUsageText: String
        get() = "${"%.1f".format(cpuUsage)}%"

    val memoryUsageText: String
        get() = "${"%.1f".format(memoryUsage)}%"

    val networkLatencyText: String
        get() = "${networkLatency}ms"

    val databaseSizeText: String
        get() = when {
            databaseSize < 1024 -> "${databaseSize}B"
            databaseSize < 1024 * 1024 -> "${"%.1f".format(databaseSize / 1024f)}KB"
            else -> "${"%.1f".format(databaseSize / (1024f * 1024f))}MB"
        }
}

/**
 * Configuration pour l'affichage des graphiques
 */
data class ChartConfig(
    val showGrid: Boolean = true,
    val animationEnabled: Boolean = true,
    val timeRange: TimeRange = TimeRange.LAST_24_HOURS,
    val chartType: ChartType = ChartType.LINE
) {
    enum class TimeRange(val displayName: String, val hours: Int) {
        LAST_HOUR("Dernière heure", 1),
        LAST_6_HOURS("6 dernières heures", 6),
        LAST_24_HOURS("24 dernières heures", 24),
        LAST_WEEK("7 derniers jours", 168),
        LAST_MONTH("30 derniers jours", 720)
    }

    enum class ChartType(val displayName: String) {
        LINE("Courbe"),
        BAR("Barres"),
        PIE("Secteurs"),
        AREA("Aire")
    }
}

/**
 * Filtre pour les événements de sécurité
 */
data class SecurityEventFilter(
    val severities: Set<String> = setOf("CRITICAL", "ERROR", "WARNING", "INFO"),
    val eventTypes: Set<String> = emptySet(),
    val sources: Set<String> = emptySet(),
    val dateRange: DateRange? = null,
    val onlyUnresolved: Boolean = false,
    val minimumRiskScore: Int = 0
) {
    /**
     * Vérifie si un événement correspond au filtre
     */
    fun matches(event: SecurityEventUI): Boolean {
        if (!severities.contains(event.severity)) return false
        if (eventTypes.isNotEmpty() && !eventTypes.contains(event.eventType)) return false
        if (sources.isNotEmpty() && !sources.contains(event.source)) return false
        if (onlyUnresolved && event.isResolved) return false
        if (event.riskScore < minimumRiskScore) return false

        // TODO: Vérifier dateRange si nécessaire

        return true
    }

    val isActive: Boolean
        get() = severities.size < 4 || eventTypes.isNotEmpty() || sources.isNotEmpty() ||
                dateRange != null || onlyUnresolved || minimumRiskScore > 0
}

/**
 * Plage de dates pour les filtres
 */
data class DateRange(
    val startDate: Date,
    val endDate: Date
) {
    val durationDays: Int
        get() = ((endDate.time - startDate.time) / (24 * 60 * 60 * 1000)).toInt()

    val displayText: String
        get() {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return "${format.format(startDate)} - ${format.format(endDate)}"
        }
}