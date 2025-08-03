// Chemin: app/src/main/java/com/vizion/security/data/local/entity/SecurityEventEntity.kt

package com.vizion.security.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entité Room pour les événements de sécurité
 *
 * Cette entité stocke tous les événements de sécurité détectés par l'application
 * et permet leur synchronisation avec le serveur Wazuh.
 *
 * Architecture :
 * - Stockage local avec Room Database
 * - Index sur timestamp pour performances des requêtes
 * - Métadonnées enrichies pour analyse ML
 * - Support géolocalisation pour traçabilité
 * - État de synchronisation Wazuh
 */
@Entity(
    tableName = "security_events",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["severity"]),
        Index(value = ["event_type"]),
        Index(value = ["is_synced"])
    ]
)
data class SecurityEventEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "severity")
    val severity: String, // INFO, WARNING, ERROR, CRITICAL

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "source")
    val source: String, // Module qui a généré l'événement

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "risk_score")
    val riskScore: Int = 0,

    @ColumnInfo(name = "is_resolved")
    val isResolved: Boolean = false,

    @ColumnInfo(name = "recommended_action")
    val recommendedAction: String? = null,

    // Champs pour synchronisation Wazuh
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "wazuh_agent_id")
    val wazuhAgentId: String? = null,

    @ColumnInfo(name = "wazuh_rule_id")
    val wazuhRuleId: Int? = null,

    @ColumnInfo(name = "sync_timestamp")
    val syncTimestamp: Long? = null,

    // Métadonnées pour ML
    @ColumnInfo(name = "device_info")
    val deviceInfo: String? = null,

    @ColumnInfo(name = "app_version")
    val appVersion: String? = null,

    @ColumnInfo(name = "os_version")
    val osVersion: String? = null,

    @ColumnInfo(name = "network_info")
    val networkInfo: String? = null,

    // Géolocalisation (optionnelle)
    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,

    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,

    @ColumnInfo(name = "location_accuracy")
    val locationAccuracy: Float? = null,

    // ML et corrélation
    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float = 1.0f,

    @ColumnInfo(name = "correlation_id")
    val correlationId: String? = null,

    @ColumnInfo(name = "parent_event_id")
    val parentEventId: String? = null,

    // Actions et résolution
    @ColumnInfo(name = "action_taken")
    val actionTaken: String? = null,

    @ColumnInfo(name = "resolution_timestamp")
    val resolutionTimestamp: Long? = null,

    @ColumnInfo(name = "resolved_by")
    val resolvedBy: String? = null, // user, system, auto

    // Métadonnées techniques
    @ColumnInfo(name = "raw_data")
    val rawData: String? = null, // JSON avec données brutes

    @ColumnInfo(name = "hash")
    val hash: String? = null, // Hash pour déduplication

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)