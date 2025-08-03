package com.vizion.security.data.repository

import android.util.Log
import com.vizion.security.data.local.dao.WazuhLogDao
import com.vizion.security.data.local.entity.WazuhLogEntity
import com.vizion.security.data.remote.WazuhApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository Wazuh utilisant l'API REST pour la gestion d'agents
 */
@Singleton
class WazuhRepository @Inject constructor(
    private val wazuhLogDao: WazuhLogDao,
    private val wazuhApiClient: WazuhApiClient
) {

    companion object {
        private const val TAG = "WazuhRepository"
    }

    /**
     * Récupère les logs récents depuis la base de données locale
     */
    fun getRecentLogs(): Flow<List<WazuhLogEntity>> = wazuhLogDao.getRecentLogs(50)

    /**
     * Établit une connexion avec Wazuh (enregistrement automatique + UDP)
     */
    suspend fun connectToWazuh(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to Wazuh via API...")
            val connected = wazuhApiClient.connect()

            if (connected) {
                Log.i(TAG, "Successfully connected to Wazuh")
                addLogToDatabase("INFO", "Connected to Wazuh via API", "WazuhRepository")
            } else {
                Log.e(TAG, "Failed to connect to Wazuh")
                addLogToDatabase("ERROR", "Failed to connect to Wazuh", "WazuhRepository")
            }

            connected
        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}", e)
            addLogToDatabase("ERROR", "Connection error: ${e.message}", "WazuhRepository")
            false
        }
    }

    /**
     * Envoie un heartbeat
     */
    suspend fun sendHeartbeat(): Boolean = withContext(Dispatchers.IO) {
        try {
            val success = wazuhApiClient.sendHeartbeat()

            if (success) {
                addLogToDatabase("INFO", "Heartbeat sent successfully", "WazuhAgent")
            } else {
                Log.w(TAG, "Heartbeat failed")
                addLogToDatabase("WARN", "Heartbeat failed", "WazuhAgent")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Heartbeat error: ${e.message}", e)
            addLogToDatabase("ERROR", "Heartbeat error: ${e.message}", "WazuhAgent")
            false
        }
    }

    /**
     * Ajoute un log - sauvegarde locale + envoi via API client
     */
    suspend fun addLog(level: String, message: String, location: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Sauvegarde locale systématique
            addLogToDatabase(level, message, location)

            // Envoi via API client
            val sent = wazuhApiClient.sendLog(level, message, location)

            if (sent) {
                markLogAsSent(level, message, location)
            }

            sent
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send log: ${e.message}", e)
            addLogToDatabase("ERROR", "Failed to send log: ${e.message}", "WazuhRepository")
            false
        }
    }

    /**
     * Synchronise les logs non envoyés
     */
    suspend fun syncUnsentLogs(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Syncing unsent logs...")

            val unsentLogs = wazuhLogDao.getUnsentLogs()
            val sentIds = mutableListOf<Long>()

            for (log in unsentLogs) {
                val sent = wazuhApiClient.sendLog(log.level, log.message, log.source)
                if (sent) {
                    sentIds.add(log.id)
                }
            }

            if (sentIds.isNotEmpty()) {
                wazuhLogDao.markLogsAsSent(sentIds)
                Log.d(TAG, "Marked ${sentIds.size} logs as sent")
            }

            addLogToDatabase("INFO", "Synced ${sentIds.size} unsent logs", "WazuhRepository")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync logs: ${e.message}", e)
            addLogToDatabase("ERROR", "Failed to sync logs: ${e.message}", "WazuhRepository")
            false
        }
    }

    /**
     * Vérifie l'état de connexion
     */
    fun isConnected(): Boolean = wazuhApiClient.isConnected()

    /**
     * Statistiques de connexion
     */
    fun getConnectionStats(): Map<String, Any> = wazuhApiClient.getConnectionStats()

    /**
     * Ferme la connexion
     */
    fun disconnect() {
        wazuhApiClient.disconnect()
    }

    /**
     * Compte les logs depuis une date
     */
    suspend fun getLogCountSince(date: Date): Int = wazuhLogDao.getLogCountSince(date)

    /**
     * Purge les anciens logs
     */
    suspend fun purgeOldLogs(): Int {
        val cutoffDate = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }.time
        return wazuhLogDao.deleteOldLogs(cutoffDate)
    }

    /**
     * Enregistre un log dans la base locale
     */
    private suspend fun addLogToDatabase(level: String, message: String, source: String) {
        try {
            val log = WazuhLogEntity(
                timestamp = Date(),
                level = level,
                message = message,
                source = source,
                isSent = false
            )
            wazuhLogDao.insertLog(log)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save log to database: ${e.message}", e)
        }
    }

    /**
     * Marque un log comme envoyé
     */
    private suspend fun markLogAsSent(level: String, message: String, source: String) {
        try {
            val unsentLogs = wazuhLogDao.getUnsentLogs()
            val matchingLog = unsentLogs.find {
                it.level == level && it.message == message && it.source == source
            }
            matchingLog?.let {
                wazuhLogDao.updateLog(it.copy(isSent = true))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark log as sent: ${e.message}", e)
        }
    }
}