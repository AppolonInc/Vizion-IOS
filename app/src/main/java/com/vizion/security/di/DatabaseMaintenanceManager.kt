package com.vizion.security.di

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.vizion.security.data.local.database.VizionSecurityDatabase
import com.vizion.security.data.local.database.DatabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire de maintenance de la base de données
 * 
 * Cette classe centralise toutes les opérations de maintenance de la base de données :
 * - Nettoyage périodique des anciennes données
 * - Optimisation des performances
 * - Compactage et réindexation
 * - Statistiques d'utilisation
 * - Sauvegarde et restauration
 */
@Singleton
class DatabaseMaintenanceManager @Inject constructor(
    private val database: VizionSecurityDatabase,
    private val context: Context
) {
    
    companion object {
        private const val TAG = "DatabaseMaintenanceManager"
        private const val PREFS_NAME = "database_maintenance"
        private const val PREF_LAST_MAINTENANCE = "last_maintenance"
        private const val PREF_MAINTENANCE_COUNT = "maintenance_count"
        private const val MAINTENANCE_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 heures
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Vérifie si une maintenance est nécessaire et l'effectue si besoin
     */
    suspend fun performMaintenanceIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val lastMaintenance = prefs.getLong(PREF_LAST_MAINTENANCE, 0)
            val currentTime = System.currentTimeMillis()
            
            if (currentTime - lastMaintenance > MAINTENANCE_INTERVAL_MS) {
                Log.i(TAG, "Performing scheduled database maintenance...")
                performMaintenance()
                
                prefs.edit {
                    putLong(PREF_LAST_MAINTENANCE, currentTime)
                    putInt(PREF_MAINTENANCE_COUNT, prefs.getInt(PREF_MAINTENANCE_COUNT, 0) + 1)
                }
                
                Log.i(TAG, "Database maintenance completed successfully")
            } else {
                Log.d(TAG, "Database maintenance not needed yet")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during maintenance check: ${e.message}", e)
        }
    }
    
    /**
     * Force une maintenance immédiate
     */
    suspend fun forceMaintenance() = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Forcing database maintenance...")
            performMaintenance()
            
            prefs.edit {
                putLong(PREF_LAST_MAINTENANCE, System.currentTimeMillis())
                putInt(PREF_MAINTENANCE_COUNT, prefs.getInt(PREF_MAINTENANCE_COUNT, 0) + 1)
            }
            
            Log.i(TAG, "Forced database maintenance completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during forced maintenance: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Effectue les opérations de maintenance
     */
    private suspend fun performMaintenance() {
        try {
            // Exécuter les requêtes de maintenance
            DatabaseConfig.MAINTENANCE_QUERIES.forEach { query ->
                try {
                    database.openHelper.writableDatabase.execSQL(query)
                    Log.d(TAG, "Executed maintenance query: $query")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to execute maintenance query '$query': ${e.message}")
                }
            }
            
            // Nettoyer les anciennes données
            cleanupOldData()
            
            // Optimiser les index
            optimizeIndexes()
            
            Log.i(TAG, "Database maintenance operations completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during maintenance operations: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Nettoie les anciennes données
     */
    private suspend fun cleanupOldData() {
        try {
            val cutoffTime = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000) // 90 jours
            
            // Nettoyer les anciens événements de sécurité
            val deletedEvents = database.securityEventDao().deleteOldEvents(cutoffTime)
            Log.d(TAG, "Cleaned up $deletedEvents old security events")
            
            // Nettoyer les anciens logs Wazuh
            val deletedLogs = database.wazuhLogDao().deleteOldLogs(java.util.Date(cutoffTime))
            Log.d(TAG, "Cleaned up $deletedLogs old Wazuh logs")
            
        } catch (e: Exception) {
            Log.w(TAG, "Error during data cleanup: ${e.message}", e)
        }
    }
    
    /**
     * Optimise les index de la base de données
     */
    private suspend fun optimizeIndexes() {
        try {
            DatabaseConfig.CUSTOM_INDEXES.forEach { indexQuery ->
                try {
                    database.openHelper.writableDatabase.execSQL(indexQuery)
                    Log.d(TAG, "Optimized index: $indexQuery")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to optimize index: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error during index optimization: ${e.message}", e)
        }
    }
    
    /**
     * Obtient les statistiques de la base de données
     */
    suspend fun getDatabaseStats(): DatabaseStats = withContext(Dispatchers.IO) {
        return@withContext try {
            val totalEvents = database.securityEventDao().getTotalEventCount()
            val syncedEvents = database.securityEventDao().getSyncedEventsCount()
            val unsyncedEvents = database.securityEventDao().getUnsyncedEventsCount()
            
            val databaseFile = context.getDatabasePath("vizion_security_database")
            val databaseSize = if (databaseFile.exists()) databaseFile.length() else 0L
            
            val lastMaintenance = prefs.getLong(PREF_LAST_MAINTENANCE, 0)
            
            DatabaseStats(
                totalEvents = totalEvents,
                syncedEvents = syncedEvents,
                unsyncedEvents = unsyncedEvents,
                databaseSize = databaseSize,
                lastMaintenance = lastMaintenance
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting database stats: ${e.message}", e)
            DatabaseStats()
        }
    }
    
    /**
     * Sauvegarde la base de données
     */
    suspend fun backupDatabase(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "database_backup_$timestamp.db")
            
            val originalDb = context.getDatabasePath("vizion_security_database")
            if (originalDb.exists()) {
                originalDb.copyTo(backupFile, overwrite = true)
                Log.i(TAG, "Database backed up to: ${backupFile.absolutePath}")
                Result.success(backupFile.absolutePath)
            } else {
                Result.failure(Exception("Original database file not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up database: ${e.message}", e)
            Result.failure(e)
        }
    }
}

/**
 * Classe de données pour les statistiques de la base de données
 */
data class DatabaseStats(
    val totalEvents: Int = 0,
    val syncedEvents: Int = 0,
    val unsyncedEvents: Int = 0,
    val databaseSize: Long = 0,
    val lastMaintenance: Long = 0
) {
    val databaseSizeFormatted: String
        get() = when {
            databaseSize < 1024 -> "${databaseSize}B"
            databaseSize < 1024 * 1024 -> "${"%.1f".format(databaseSize / 1024f)}KB"
            databaseSize < 1024 * 1024 * 1024 -> "${"%.1f".format(databaseSize / (1024f * 1024f))}MB"
            else -> "${"%.1f".format(databaseSize / (1024f * 1024f * 1024f))}GB"
        }
    
    val syncPercentage: Float
        get() = if (totalEvents > 0) (syncedEvents * 100f) / totalEvents else 100f
}