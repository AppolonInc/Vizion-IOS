// Chemin: app/src/main/java/com/vizion/security/data/repository/SecurityRepository.kt

package com.vizion.security.data.repository

import kotlinx.coroutines.flow.Flow
import com.vizion.security.data.local.entity.SecurityEventEntity
import com.vizion.security.data.local.dao.SecurityStatistics
import com.vizion.security.data.local.dao.EventTypeStats
import com.vizion.security.data.local.dao.SourceStats

/**
 * Interface du repository de sécurité
 *
 * Cette interface définit toutes les opérations disponibles pour
 * la gestion des événements de sécurité, statistiques et synchronisation.
 *
 * Architecture :
 * - Repository pattern pour abstraction des sources de données
 * - Support Flow pour les observateurs temps réel
 * - Opérations synchrones et asynchrones
 * - Gestion cache et synchronisation Wazuh
 * - Analytics et ML intégrés
 */
interface SecurityRepository {

    // === GESTION DES ÉVÉNEMENTS ===

    /**
     * Obtient tous les événements de sécurité
     */
    fun getAllEvents(): Flow<List<SecurityEventEntity>>

    /**
     * Obtient les événements par niveau de sévérité
     */
    fun getEventsBySeverity(severity: String): Flow<List<SecurityEventEntity>>

    /**
     * Obtient les événements récents (limite configurable)
     */
    fun getRecentEvents(limit: Int = 50): Flow<List<SecurityEventEntity>>

    /**
     * Obtient les menaces actives (non résolues et critiques)
     */
    fun getActiveThreats(): Flow<List<SecurityEventEntity>>

    /**
     * Obtient le nombre de menaces critiques actives
     */
    fun getActiveCriticalCount(): Flow<Int>

    /**
     * Insère un nouvel événement de sécurité
     */
    suspend fun insertSecurityEvent(event: SecurityEventEntity)

    /**
     * Insère plusieurs événements en lot
     */
    suspend fun insertSecurityEvents(events: List<SecurityEventEntity>)

    /**
     * Met à jour un événement existant
     */
    suspend fun updateSecurityEvent(event: SecurityEventEntity)

    /**
     * Résout un événement de sécurité
     */
    suspend fun resolveSecurityEvent(eventId: String, resolvedBy: String = "user")

    /**
     * Supprime les anciens événements
     */
    suspend fun cleanupOldEvents(daysToKeep: Int = 30): Int

    // === STATISTIQUES ET ANALYTICS ===

    /**
     * Obtient les statistiques de sécurité pour une période
     */
    suspend fun getSecurityStatistics(sinceDays: Int = 7): List<SecurityStatistics>

    /**
     * Obtient la fréquence des types d'événements
     */
    suspend fun getEventTypeFrequency(sinceDays: Int = 7): List<EventTypeStats>

    /**
     * Obtient les statistiques par source
     */
    suspend fun getSourceStatistics(sinceDays: Int = 7): List<SourceStats>

    /**
     * Obtient le nombre total d'événements
     */
    suspend fun getTotalEventCount(): Int

    /**
     * Obtient le nombre d'événements par sévérité
     */
    suspend fun getEventCountBySeverity(severity: String): Int

    /**
     * Obtient le score de risque moyen
     */
    suspend fun getAverageRiskScore(sinceDays: Int = 7): Double

    /**
     * Obtient le nombre d'événements récents
     */
    suspend fun getRecentEventCount(): Int

    // === SYNCHRONISATION WAZUH ===

    /**
     * Obtient les événements non synchronisés
     */
    suspend fun getUnsyncedEvents(limit: Int = 100): List<SecurityEventEntity>

    /**
     * Marque un événement comme synchronisé
     */
    suspend fun markEventAsSynced(eventId: String, wazuhAgentId: String?)

    /**
     * Marque plusieurs événements comme synchronisés
     */
    suspend fun markEventsAsSynced(eventIds: List<String>)

    /**
     * Obtient le nombre d'événements synchronisés
     */
    suspend fun getSyncedEventsCount(): Int

    /**
     * Obtient le nombre d'événements non synchronisés
     */
    suspend fun getUnsyncedEventsCount(): Int

    /**
     * Synchronise tous les événements en attente avec Wazuh
     */
    suspend fun syncPendingEventsWithWazuh(): Result<Int>

    // === ML ET CORRÉLATION ===

    /**
     * Trouve les événements corrélés
     */
    suspend fun getCorrelatedEvents(correlationId: String): List<SecurityEventEntity>

    /**
     * Trouve les événements avec faible score de confiance
     */
    suspend fun getLowConfidenceEvents(threshold: Float = 0.7f): List<SecurityEventEntity>

    /**
     * Obtient les événements enfants d'un événement parent
     */
    suspend fun getChildEvents(parentEventId: String): List<SecurityEventEntity>

    /**
     * Analyse et classifie un nouvel événement avec ML
     */
    suspend fun classifyAndEnrichEvent(event: SecurityEventEntity): SecurityEventEntity

    /**
     * Détecte les anomalies comportementales
     */
    suspend fun detectAnomalies(): List<SecurityEventEntity>

    // === EXPORT ET RAPPORTS ===

    /**
     * Exporte les événements au format JSON
     */
    suspend fun exportEventsToJson(sinceDays: Int = 30): Result<String>

    /**
     * Exporte les événements au format CSV
     */
    suspend fun exportEventsToCSV(sinceDays: Int = 30): Result<String>

    /**
     * Génère un rapport de sécurité
     */
    suspend fun generateSecurityReport(sinceDays: Int = 7): String

    // === MAINTENANCE ===

    /**
     * Optimise la base de données
     */
    suspend fun optimizeDatabase(): Result<String>

    /**
     * Supprime les doublons d'événements
     */
    suspend fun removeDuplicateEvents(): Int

    /**
     * Nettoie les événements résolus anciens
     */
    suspend fun cleanupResolvedEvents(daysToKeep: Int = 90): Int
}