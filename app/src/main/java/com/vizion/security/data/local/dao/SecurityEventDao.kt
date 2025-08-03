// Chemin: app/src/main/java/com/vizion/security/data/local/dao/SecurityEventDao.kt

package com.vizion.security.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.vizion.security.data.local.entity.SecurityEventEntity

/**
 * DAO pour les opérations sur les événements de sécurité
 *
 * Interface Room qui définit toutes les opérations de base de données
 * pour les événements de sécurité, avec support pour :
 * - Opérations CRUD complètes
 * - Requêtes analytiques avancées
 * - Synchronisation Wazuh
 * - Observateurs temps réel avec Flow
 * - Requêtes ML et corrélation
 */
@Dao
interface SecurityEventDao {

    // === OPÉRATIONS CRUD DE BASE ===

    @Query("SELECT * FROM security_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<SecurityEventEntity>>

    @Query("SELECT * FROM security_events WHERE severity = :severity ORDER BY timestamp DESC")
    fun getEventsBySeverity(severity: String): Flow<List<SecurityEventEntity>>

    @Query("SELECT * FROM security_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getEventsAfter(since: Long): Flow<List<SecurityEventEntity>>

    @Insert
    suspend fun insertEvent(event: SecurityEventEntity)

    @Insert
    suspend fun insertEvents(events: List<SecurityEventEntity>)

    @Update
    suspend fun updateEvent(event: SecurityEventEntity)

    @Delete
    suspend fun deleteEvent(event: SecurityEventEntity)

    @Query("DELETE FROM security_events WHERE timestamp < :cutoffDate")
    suspend fun deleteOldEvents(cutoffDate: Long): Int

    // === REQUÊTES ANALYTIQUES ===

    @Query("SELECT COUNT(*) FROM security_events WHERE severity = :severity")
    suspend fun getEventCountBySeverity(severity: String): Int

    @Query("SELECT COUNT(*) FROM security_events WHERE timestamp >= :since")
    suspend fun getEventCountSince(since: Long): Int

    @Query("SELECT AVG(risk_score) FROM security_events WHERE timestamp >= :since")
    suspend fun getAverageRiskScore(since: Long): Double

    @Query("SELECT COUNT(*) FROM security_events WHERE is_synced = 1")
    suspend fun getSyncedEventsCount(): Int

    @Query("SELECT COUNT(*) FROM security_events WHERE is_synced = 0")
    suspend fun getUnsyncedEventsCount(): Int

    // === GESTION SYNCHRONISATION WAZUH ===

    @Query("SELECT * FROM security_events WHERE is_synced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsyncedEvents(limit: Int = 100): List<SecurityEventEntity>

    @Query("UPDATE security_events SET is_synced = 1, sync_timestamp = :syncTime, wazuh_agent_id = :agentId WHERE id = :eventId")
    suspend fun markAsSynced(eventId: String, syncTime: Long, agentId: String?)

    @Query("UPDATE security_events SET is_synced = 1, sync_timestamp = :syncTime WHERE id IN (:eventIds)")
    suspend fun markMultipleAsSynced(eventIds: List<String>, syncTime: Long)

    // === REQUÊTES TEMPS RÉEL ===

    @Query("SELECT * FROM security_events WHERE severity IN ('CRITICAL', 'ERROR') AND is_resolved = 0 ORDER BY timestamp DESC")
    fun getActiveThreats(): Flow<List<SecurityEventEntity>>

    @Query("SELECT * FROM security_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 50): Flow<List<SecurityEventEntity>>

    @Query("SELECT COUNT(*) FROM security_events WHERE severity = 'CRITICAL' AND is_resolved = 0")
    fun getActiveCriticalCount(): Flow<Int>

    // === ML ET CORRÉLATION ===

    @Query("SELECT * FROM security_events WHERE correlation_id = :correlationId ORDER BY timestamp ASC")
    suspend fun getCorrelatedEvents(correlationId: String): List<SecurityEventEntity>

    @Query("SELECT * FROM security_events WHERE confidence_score < :threshold ORDER BY timestamp DESC")
    suspend fun getLowConfidenceEvents(threshold: Float = 0.7f): List<SecurityEventEntity>

    @Query("SELECT * FROM security_events WHERE parent_event_id = :parentId ORDER BY timestamp ASC")
    suspend fun getChildEvents(parentId: String): List<SecurityEventEntity>

    // === STATISTIQUES AVANCÉES ===

    @Query("""
        SELECT 
            severity,
            COUNT(*) as count,
            AVG(risk_score) as avg_risk,
            MAX(timestamp) as last_occurrence
        FROM security_events 
        WHERE timestamp >= :since 
        GROUP BY severity
    """)
    suspend fun getSecurityStatistics(since: Long): List<SecurityStatistics>

    @Query("""
        SELECT 
            event_type,
            COUNT(*) as frequency,
            AVG(confidence_score) as avg_confidence
        FROM security_events 
        WHERE timestamp >= :since 
        GROUP BY event_type 
        ORDER BY frequency DESC
    """)
    suspend fun getEventTypeFrequency(since: Long): List<EventTypeStats>

    @Query("""
        SELECT 
            source,
            COUNT(*) as event_count,
            COUNT(CASE WHEN severity = 'CRITICAL' THEN 1 END) as critical_count
        FROM security_events 
        WHERE timestamp >= :since 
        GROUP BY source
    """)
    suspend fun getSourceStatistics(since: Long): List<SourceStats>

    // === NETTOYAGE ET MAINTENANCE ===

    @Query("SELECT COUNT(*) FROM security_events")
    suspend fun getTotalEventCount(): Int

    @Query("DELETE FROM security_events WHERE is_resolved = 1 AND resolution_timestamp < :cutoffDate")
    suspend fun cleanupResolvedEvents(cutoffDate: Long): Int

    @Query("SELECT * FROM security_events WHERE hash IS NOT NULL GROUP BY hash HAVING COUNT(*) > 1")
    suspend fun findDuplicateEvents(): List<SecurityEventEntity>

    @Query("DELETE FROM security_events WHERE id NOT IN (SELECT MIN(id) FROM security_events WHERE hash = :hash)")
    suspend fun removeDuplicatesByHash(hash: String)
}

/**
 * Classes de données pour les requêtes statistiques
 */
data class SecurityStatistics(
    val severity: String,
    val count: Int,
    val avg_risk: Double,
    val last_occurrence: Long
)

data class EventTypeStats(
    val event_type: String,
    val frequency: Int,
    val avg_confidence: Double
)

data class SourceStats(
    val source: String,
    val event_count: Int,
    val critical_count: Int
)